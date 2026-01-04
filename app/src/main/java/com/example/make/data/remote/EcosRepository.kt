package com.example.make.data.remote

import com.example.make.BuildConfig
import com.example.make.data.remote.dto.EcosWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EcosRepository {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val apiKey = BuildConfig.ECOS_API_KEY
    // ECOS Base URL: http://ecos.bok.or.kr/api/StatisticSearch/KEY/TYPE/KR/START/END/STAT_CODE/CYCLE/START_TIME/END_TIME/ITEM_CODE1
    // We use JSON type.
    private val baseUrl = "https://ecos.bok.or.kr/api/StatisticSearch"
    private val TAG = "EcosRepository"

    // STAT_CODE 목록:
    // 817Y002 - 금리 (일간): 010210000(국고채10년), 010200001(국고채3년), 010502000(CD91일), 010101000(콜금리)
    // 731Y001 - 환율 (일간): 0000001(원/달러), 0000002(원/엔100), 0000003(원/유로)
    // 722Y001 - 기준금리 (월간): 0101000
    
    companion object {
        const val STAT_INTEREST_RATE = "817Y002"  // 금리 (일간)
        const val STAT_EXCHANGE_RATE = "731Y001"  // 환율 (일간)
        const val STAT_BASE_RATE = "722Y001"      // 기준금리 (월간)
        
        // 금리 항목코드
        const val ITEM_TREASURY_10Y = "010210000"  // 국고채 10년
        const val ITEM_TREASURY_3Y = "010200001"   // 국고채 3년
        const val ITEM_CD_91D = "010502000"        // CD 91일
        const val ITEM_CALL_RATE = "010101000"     // 콜금리
        
        // 환율 항목코드
        const val ITEM_USD_KRW = "0000001"         // 원/달러
        const val ITEM_JPY_KRW = "0000002"         // 원/엔(100엔)
        const val ITEM_EUR_KRW = "0000003"         // 원/유로
        
        // 기준금리 항목코드
        const val ITEM_BASE_RATE = "0101000"       // 기준금리
    }
    
    // Legacy method for backward compatibility (금리 전용)
    suspend fun getLatestValue(itemCode: String): Pair<Double, Double>? = 
        getLatestValueWithResult(itemCode).data
    
    // 범용 메서드: 통계코드, 항목코드, 주기 지정 가능
    suspend fun getValueWithResult(
        statCode: String,
        itemCode: String,
        cycle: String = "D",
        days: Int = 10
    ): com.example.make.data.model.ApiResult<Pair<Double, Double>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            android.util.Log.e(TAG, "API Key is blank")
            return@withContext com.example.make.data.model.ApiResult(
                error = com.example.make.data.model.ApiError(
                    code = com.example.make.data.model.ErrorCode.API_KEY_INVALID,
                    message = "ECOS API 키가 설정되지 않았습니다"
                )
            )
        }
        
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.KOREA)
        val cal = Calendar.getInstance()
        val endDate = sdf.format(cal.time)
        
        // 주기에 따라 조회 기간 조정
        when (cycle) {
            "M" -> cal.add(Calendar.MONTH, -12)  // 월간: 1년
            "Q" -> cal.add(Calendar.YEAR, -2)    // 분기: 2년
            else -> cal.add(Calendar.DAY_OF_YEAR, -days)  // 일간
        }
        val startDate = sdf.format(cal.time)
        
        val url = "$baseUrl/$apiKey/json/kr/1/50/$statCode/$cycle/$startDate/$endDate/$itemCode"
        android.util.Log.d(TAG, "Request URL: $url")
        
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                
                if (body == null) {
                    return@use com.example.make.data.model.ApiResult(
                        error = com.example.make.data.model.ApiError(
                            code = com.example.make.data.model.ErrorCode.NETWORK_ERROR,
                            message = "ECOS 서버 응답이 없습니다"
                        )
                    )
                }
                
                android.util.Log.d(TAG, "getValueWithResult($statCode/$itemCode): ${body.take(300)}")
                
                if (!response.isSuccessful) {
                    return@use com.example.make.data.model.ApiResult(
                        error = com.example.make.data.model.ApiError(
                            code = com.example.make.data.model.ErrorCode.NETWORK_ERROR,
                            message = "서버 에러: ${response.code}"
                        )
                    )
                }
                
                try {
                    val wrapper = json.decodeFromString<EcosWrapper>(body)
                    
                    if (wrapper.result != null && wrapper.result.CODE != "INFO-000") {
                        val isNoData = wrapper.result.CODE == "INFO-200"
                        return@use com.example.make.data.model.ApiResult(
                            error = com.example.make.data.model.ApiError(
                                code = if (isNoData) com.example.make.data.model.ErrorCode.UNKNOWN else com.example.make.data.model.ErrorCode.API_KEY_INVALID,
                                message = "ECOS: ${wrapper.result.MESSAGE}"
                            )
                        )
                    }

                    val rows = wrapper.statisticSearch?.row
                    if (!rows.isNullOrEmpty()) {
                        val sorted = rows.sortedByDescending { it.TIME }
                        if (sorted.isNotEmpty()) {
                            val latest = sorted[0].DATA_VALUE.toDoubleOrNull() ?: 0.0
                            var change = 0.0
                            if (sorted.size >= 2) {
                                val prev = sorted[1].DATA_VALUE.toDoubleOrNull() ?: 0.0
                                change = latest - prev
                            }
                            return@use com.example.make.data.model.ApiResult(data = Pair(latest, change))
                        }
                    }
                    
                    return@use com.example.make.data.model.ApiResult(
                        error = com.example.make.data.model.ApiError(
                            code = com.example.make.data.model.ErrorCode.UNKNOWN,
                            message = "데이터가 없습니다"
                        )
                    )
                    
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Parse error", e)
                    return@use com.example.make.data.model.ApiResult(
                        error = com.example.make.data.model.ApiError(
                            code = com.example.make.data.model.ErrorCode.PARSE_ERROR,
                            message = "데이터 파싱 오류"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Exception: ", e)
            return@withContext com.example.make.data.model.ApiResult(
                error = com.example.make.data.model.ApiError(
                    code = com.example.make.data.model.ErrorCode.NETWORK_ERROR,
                    message = "네트워크 오류: ${e.message}"
                )
            )
        }
    }

    // New method with detailed result
    suspend fun getLatestValueWithResult(itemCode: String): com.example.make.data.model.ApiResult<Pair<Double, Double>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            android.util.Log.e(TAG, "API Key is blank")
            return@withContext com.example.make.data.model.ApiResult(
                error = com.example.make.data.model.ApiError(
                    code = com.example.make.data.model.ErrorCode.API_KEY_INVALID,
                    message = "ECOS API 키가 설정되지 않았습니다"
                )
            )
        }
        
        // Fetch last 10 days to ensure we find data even on holidays
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.KOREA)
        val cal = Calendar.getInstance()
        val endDate = sdf.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, -10)
        val startDate = sdf.format(cal.time)

        // URL Construction: /KEY/json/kr/1/10/817Y002/D/START/END/ITEM_CODE
        val url = "$baseUrl/$apiKey/json/kr/1/10/817Y002/D/$startDate/$endDate/$itemCode"
        
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                
                if (body == null) {
                    return@use com.example.make.data.model.ApiResult(
                        error = com.example.make.data.model.ApiError(
                            code = com.example.make.data.model.ErrorCode.NETWORK_ERROR,
                            message = "ECOS 서버 응답이 없습니다"
                        )
                    )
                }
                
                android.util.Log.d(TAG, "getLatestValue($itemCode): $body")
                
                if (!response.isSuccessful) {
                    return@use com.example.make.data.model.ApiResult(
                        error = com.example.make.data.model.ApiError(
                            code = com.example.make.data.model.ErrorCode.NETWORK_ERROR,
                            message = "서버 에러: ${response.code}"
                        )
                    )
                }
                
                try {
                    val wrapper = json.decodeFromString<EcosWrapper>(body)
                    
                    // Check for API Error Result
                    if (wrapper.result != null && wrapper.result.CODE != "INFO-000") {
                        val isNoData = wrapper.result.CODE == "INFO-200"
                        return@use com.example.make.data.model.ApiResult(
                            error = com.example.make.data.model.ApiError(
                                code = if (isNoData) com.example.make.data.model.ErrorCode.UNKNOWN else com.example.make.data.model.ErrorCode.API_KEY_INVALID,
                                message = "ECOS 오류: ${wrapper.result.MESSAGE} (${wrapper.result.CODE})"
                            )
                        )
                    }

                    val rows = wrapper.statisticSearch?.row
                    if (!rows.isNullOrEmpty()) {
                        val sorted = rows.sortedByDescending { it.TIME }
                        if (sorted.isNotEmpty()) {
                            val latest = sorted[0].DATA_VALUE.toDoubleOrNull() ?: 0.0
                            var change = 0.0
                            if (sorted.size >= 2) {
                                val prev = sorted[1].DATA_VALUE.toDoubleOrNull() ?: 0.0
                                change = latest - prev
                            }
                            return@use com.example.make.data.model.ApiResult(data = Pair(latest, change))
                        }
                    }
                    
                    return@use com.example.make.data.model.ApiResult(
                        error = com.example.make.data.model.ApiError(
                            code = com.example.make.data.model.ErrorCode.UNKNOWN,
                            message = "데이터가 없습니다"
                        )
                    )
                    
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Parse error", e)
                    return@use com.example.make.data.model.ApiResult(
                        error = com.example.make.data.model.ApiError(
                            code = com.example.make.data.model.ErrorCode.PARSE_ERROR,
                            message = "데이터 파싱 오류"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Exception: ", e)
            return@withContext com.example.make.data.model.ApiResult(
                error = com.example.make.data.model.ApiError(
                    code = com.example.make.data.model.ErrorCode.NETWORK_ERROR,
                    message = "네트워크 오류: ${e.message}"
                )
            )
        }
    }

    suspend fun getTimeSeries(itemCode: String): List<Double> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            android.util.Log.e(TAG, "getTimeSeries: API Key is blank")
            return@withContext emptyList()
        }
        
        // 항목코드로 통계코드와 주기 판별
        val (statCode, cycle) = when {
            // 환율 (731Y001)
            itemCode == ITEM_USD_KRW || itemCode == ITEM_JPY_KRW || itemCode == ITEM_EUR_KRW -> 
                Pair(STAT_EXCHANGE_RATE, "D")
            // 기준금리 (722Y001) - 월간
            itemCode == ITEM_BASE_RATE -> 
                Pair(STAT_BASE_RATE, "M")
            // 금리 (817Y002) - 기본
            else -> 
                Pair(STAT_INTEREST_RATE, "D")
        }
        
        android.util.Log.d(TAG, "getTimeSeries: itemCode=$itemCode, statCode=$statCode, cycle=$cycle")
        
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.KOREA)
        val cal = Calendar.getInstance()
        val endDate = sdf.format(cal.time)
        
        // 주기에 따라 조회 기간 설정
        when (cycle) {
            "M" -> cal.add(Calendar.YEAR, -5)  // 월간: 5년
            else -> cal.add(Calendar.YEAR, -1) // 일간: 1년
        }
        val startDate = sdf.format(cal.time)
        
        val outputSize = if (cycle == "M") 60 else 365
        val url = "$baseUrl/$apiKey/json/kr/1/$outputSize/$statCode/$cycle/$startDate/$endDate/$itemCode"
        
        android.util.Log.d(TAG, "getTimeSeries URL: $url")
        
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (body == null) {
                    android.util.Log.e(TAG, "getTimeSeries: Response body is null")
                    return@use emptyList()
                }
                
                android.util.Log.d(TAG, "getTimeSeries response (${body.length} bytes): ${body.take(500)}")
                
                val wrapper = json.decodeFromString<EcosWrapper>(body)
                
                // 에러 응답 확인
                if (wrapper.result != null) {
                    val resultCode = wrapper.result.CODE
                    val resultMsg = wrapper.result.MESSAGE
                    android.util.Log.d(TAG, "getTimeSeries result: code=$resultCode, msg=$resultMsg")
                    
                    if (resultCode != "INFO-000") {
                        android.util.Log.e(TAG, "getTimeSeries API error: $resultCode - $resultMsg")
                        return@use emptyList()
                    }
                }
                
                val rows = wrapper.statisticSearch?.row
                if (rows.isNullOrEmpty()) {
                    android.util.Log.w(TAG, "getTimeSeries: No data rows returned")
                    return@use emptyList()
                }
                
                android.util.Log.d(TAG, "getTimeSeries: Got ${rows.size} rows")
                
                val result = rows.sortedBy { it.TIME }.mapNotNull { it.DATA_VALUE.toDoubleOrNull() }
                android.util.Log.d(TAG, "getTimeSeries: Parsed ${result.size} values, first=${result.firstOrNull()}, last=${result.lastOrNull()}")
                
                return@use result
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "getTimeSeries exception: ${e.message}", e)
        }
        emptyList()
    }
}
