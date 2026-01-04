package com.example.make.data.remote

import com.example.make.data.remote.dto.TwelveDataQuoteResponse
import com.example.make.data.model.ApiResult
import com.example.make.data.model.ApiError
import com.example.make.data.model.ErrorCode
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import com.example.make.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class TwelveDataRepository {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private val apiKey = BuildConfig.TWELVE_DATA_API_KEY
    private val baseUrl = "https://api.twelvedata.com"
    private val TAG = "TwelveDataRepo"

    // Legacy method for backward compatibility
    suspend fun getQuote(symbol: String): TwelveDataQuoteResponse? = 
        getQuoteWithResult(symbol).data

    // New method with detailed result
    suspend fun getQuoteWithResult(symbol: String): ApiResult<TwelveDataQuoteResponse> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            android.util.Log.e(TAG, "API Key is blank")
            return@withContext ApiResult(
                error = ApiError(
                    code = ErrorCode.API_KEY_INVALID,
                    message = "API 키가 설정되지 않았습니다"
                )
            )
        }
        
        val url = "$baseUrl/quote?symbol=$symbol&apikey=$apiKey"
        val request = Request.Builder().url(url).build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                
                if (body == null) {
                    android.util.Log.e(TAG, "Empty response body for $symbol")
                    return@use ApiResult(
                        error = ApiError(
                            code = ErrorCode.NETWORK_ERROR,
                            message = "서버 응답이 비어있습니다"
                        )
                    )
                }
                
                android.util.Log.d(TAG, "getQuote($symbol): ${body.take(200)}...")
                
                // Check HTTP status
                if (!response.isSuccessful) {
                    android.util.Log.e(TAG, "HTTP Error: ${response.code}")
                    val errorCode = when (response.code) {
                        429 -> ErrorCode.RATE_LIMIT_EXCEEDED
                        401, 403 -> ErrorCode.API_KEY_INVALID
                        404 -> ErrorCode.INVALID_SYMBOL
                        else -> ErrorCode.NETWORK_ERROR
                    }
                    return@use ApiResult(
                        error = ApiError(
                            code = errorCode,
                            message = when (errorCode) {
                                ErrorCode.RATE_LIMIT_EXCEEDED -> "API 호출 한도를 초과했습니다"
                                ErrorCode.API_KEY_INVALID -> "API 키가 유효하지 않습니다"
                                ErrorCode.INVALID_SYMBOL -> "심볼을 찾을 수 없습니다: $symbol"
                                else -> "서버 오류 (${response.code})"
                            },
                            details = body
                        )
                    )
                }
                
                // Check for API error in body
                if (body.contains("\"code\":") && body.contains("\"message\":")) {
                    android.util.Log.e(TAG, "API Error in body: $body")
                    val errorCode = when {
                        body.contains("limit", ignoreCase = true) -> ErrorCode.RATE_LIMIT_EXCEEDED
                        body.contains("invalid", ignoreCase = true) -> ErrorCode.INVALID_SYMBOL
                        else -> ErrorCode.UNKNOWN
                    }
                    return@use ApiResult(
                        error = ApiError(
                            code = errorCode,
                            message = when (errorCode) {
                                ErrorCode.RATE_LIMIT_EXCEEDED -> "일일 API 호출 한도에 도달했습니다"
                                ErrorCode.INVALID_SYMBOL -> "유효하지 않은 심볼: $symbol"
                                else -> "API 오류가 발생했습니다"
                            },
                            details = body
                        )
                    )
                }
                
                // Try to parse response
                try {
                    val data = json.decodeFromString<TwelveDataQuoteResponse>(body)
                    ApiResult(data = data)
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Parse error: ", e)
                    ApiResult(
                        error = ApiError(
                            code = ErrorCode.PARSE_ERROR,
                            message = "데이터 파싱 실패",
                            details = e.message
                        )
                    )
                }
            }
        } catch (e: UnknownHostException) {
            android.util.Log.e(TAG, "Network error: ", e)
            ApiResult(
                error = ApiError(
                    code = ErrorCode.NETWORK_ERROR,
                    message = "인터넷 연결을 확인해주세요",
                    details = e.message
                )
            )
        } catch (e: SocketTimeoutException) {
            android.util.Log.e(TAG, "Timeout: ", e)
            ApiResult(
                error = ApiError(
                    code = ErrorCode.NETWORK_ERROR,
                    message = "서버 응답 시간 초과",
                    details = e.message
                )
            )
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Exception: ", e)
            ApiResult(
                error = ApiError(
                    code = ErrorCode.UNKNOWN,
                    message = "알 수 없는 오류가 발생했습니다",
                    details = e.message
                )
            )
        }
    }

    // Legacy method for backward compatibility
    suspend fun getTimeSeries(symbol: String, interval: String): com.example.make.data.remote.dto.TwelveDataTimeSeriesResponse? = 
        getTimeSeriesWithResult(symbol, interval).data

    // New method with detailed result
    suspend fun getTimeSeriesWithResult(symbol: String, interval: String): ApiResult<com.example.make.data.remote.dto.TwelveDataTimeSeriesResponse> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            android.util.Log.e(TAG, "API Key is blank")
            return@withContext ApiResult(
                error = ApiError(
                    code = ErrorCode.API_KEY_INVALID,
                    message = "API 키가 설정되지 않았습니다"
                )
            )
        }
        
        val url = "$baseUrl/time_series?symbol=$symbol&interval=$interval&outputsize=30&apikey=$apiKey"
        val request = Request.Builder().url(url).build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                
                if (body == null) {
                    android.util.Log.e(TAG, "Empty response body for $symbol")
                    return@use ApiResult(
                        error = ApiError(
                            code = ErrorCode.NETWORK_ERROR,
                            message = "서버 응답이 비어있습니다"
                        )
                    )
                }
                
                android.util.Log.d(TAG, "getTimeSeries($symbol, $interval): ${body.take(200)}...")
                
                // Check HTTP status
                if (!response.isSuccessful) {
                    android.util.Log.e(TAG, "HTTP Error: ${response.code}")
                    val errorCode = when (response.code) {
                        429 -> ErrorCode.RATE_LIMIT_EXCEEDED
                        401, 403 -> ErrorCode.API_KEY_INVALID
                        404 -> ErrorCode.INVALID_SYMBOL
                        else -> ErrorCode.NETWORK_ERROR
                    }
                    return@use ApiResult(
                        error = ApiError(
                            code = errorCode,
                            message = when (errorCode) {
                                ErrorCode.RATE_LIMIT_EXCEEDED -> "API 호출 한도를 초과했습니다"
                                ErrorCode.API_KEY_INVALID -> "API 키가 유효하지 않습니다"
                                ErrorCode.INVALID_SYMBOL -> "심볼을 찾을 수 없습니다: $symbol"
                                else -> "서버 오류 (${response.code})"
                            },
                            details = body
                        )
                    )
                }
                
                // Check for API error in body
                if (body.contains("\"code\":") && body.contains("\"message\":")) {
                    android.util.Log.e(TAG, "API Error in body: $body")
                    val errorCode = when {
                        body.contains("limit", ignoreCase = true) -> ErrorCode.RATE_LIMIT_EXCEEDED
                        body.contains("invalid", ignoreCase = true) -> ErrorCode.INVALID_SYMBOL
                        else -> ErrorCode.UNKNOWN
                    }
                    return@use ApiResult(
                        error = ApiError(
                            code = errorCode,
                            message = when (errorCode) {
                                ErrorCode.RATE_LIMIT_EXCEEDED -> "일일 API 호출 한도에 도달했습니다"
                                ErrorCode.INVALID_SYMBOL -> "유효하지 않은 심볼: $symbol"
                                else -> "API 오류가 발생했습니다"
                            },
                            details = body
                        )
                    )
                }
                
                // Try to parse response
                try {
                    val data = json.decodeFromString<com.example.make.data.remote.dto.TwelveDataTimeSeriesResponse>(body)
                    ApiResult(data = data)
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Parse error: ", e)
                    ApiResult(
                        error = ApiError(
                            code = ErrorCode.PARSE_ERROR,
                            message = "데이터 파싱 실패",
                            details = e.message
                        )
                    )
                }
            }
        } catch (e: UnknownHostException) {
            android.util.Log.e(TAG, "Network error: ", e)
            ApiResult(
                error = ApiError(
                    code = ErrorCode.NETWORK_ERROR,
                    message = "인터넷 연결을 확인해주세요",
                    details = e.message
                )
            )
        } catch (e: SocketTimeoutException) {
            android.util.Log.e(TAG, "Timeout: ", e)
            ApiResult(
                error = ApiError(
                    code = ErrorCode.NETWORK_ERROR,
                    message = "서버 응답 시간 초과",
                    details = e.message
                )
            )
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Exception: ", e)
            ApiResult(
                error = ApiError(
                    code = ErrorCode.UNKNOWN,
                    message = "알 수 없는 오류가 발생했습니다",
                    details = e.message
                )
            )
        }
    }
}
