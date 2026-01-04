package com.example.make.data.repository

import android.util.Log
import com.example.make.data.model.EconomicIndicator
import com.example.make.data.model.IndicatorType
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * 경제지표 Repository
 * Firestore의 economic_indicators 컬렉션에서 데이터 조회
 * GitHub Actions가 15분마다 ECOS API로부터 수집한 실시간 데이터
 */
class EconomicIndicatorsRepository {
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "EconomicIndicatorsRepo"
    
    /**
     * 모든 경제지표 조회
     * @return List<EconomicIndicator> - 7개 지표 (금리 4개, 환율 3개)
     */
    suspend fun getAllIndicators(): List<EconomicIndicator> {
        return try {
            val snapshot = db.collection("economic_indicators")
                .get()
                .await()
            
            if (snapshot.isEmpty) {
                Log.w(TAG, "No indicators found in Firestore")
                return emptyList()
            }
            
            val indicators = snapshot.documents.mapNotNull { doc ->
                try {
                    val id = doc.getString("id") ?: return@mapNotNull null
                    val name = doc.getString("name") ?: return@mapNotNull null
                    val value = doc.getDouble("value") ?: 0.0
                    val changeRate = doc.getDouble("change_rate") ?: 0.0
                    val unit = doc.getString("unit") ?: ""
                    val type = doc.getString("type") ?: "unknown"
                    val source = doc.getString("source") ?: "한국은행"
                    val capturedAt = doc.getString("captured_at") ?: ""
                    
                    // 타입 매핑
                    val indicatorType = when (type) {
                        "interest_rate" -> when (name) {
                            "기준금리" -> IndicatorType.BASE_RATE
                            "CD 91일" -> IndicatorType.CD_RATE
                            else -> IndicatorType.KR_TREASURY
                        }
                        "exchange_rate" -> IndicatorType.EXCHANGE_RATE
                        else -> IndicatorType.UNKNOWN
                    }
                    
                    // 타임스탬프 파싱
                    val timestamp = try {
                        java.time.Instant.parse(capturedAt).toEpochMilli()
                    } catch (e: Exception) {
                        System.currentTimeMillis()
                    }
                    
                    EconomicIndicator(
                        id = id,
                        type = indicatorType,
                        name = name,
                        value = value,
                        unit = unit,
                        changeRate = changeRate,
                        capturedAt = timestamp,
                        source = source
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing indicator: ${e.message}")
                    null
                }
            }
            
            Log.d(TAG, "Loaded ${indicators.size} indicators from Firestore")
            indicators
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching indicators: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * 특정 지표 조회
     * @param indicatorId 지표 ID (예: "base_rate", "usd_krw")
     */
    suspend fun getIndicatorById(indicatorId: String): EconomicIndicator? {
        return try {
            val doc = db.collection("economic_indicators")
                .document(indicatorId)
                .get()
                .await()
            
            if (!doc.exists()) {
                Log.w(TAG, "Indicator not found: $indicatorId")
                return null
            }
            
            val name = doc.getString("name") ?: return null
            val value = doc.getDouble("value") ?: 0.0
            val changeRate = doc.getDouble("change_rate") ?: 0.0
            val unit = doc.getString("unit") ?: ""
            val type = doc.getString("type") ?: "unknown"
            val source = doc.getString("source") ?: "한국은행"
            val capturedAt = doc.getString("captured_at") ?: ""
            
            val indicatorType = when (type) {
                "interest_rate" -> when (name) {
                    "기준금리" -> IndicatorType.BASE_RATE
                    "CD 91일" -> IndicatorType.CD_RATE
                    else -> IndicatorType.KR_TREASURY
                }
                "exchange_rate" -> IndicatorType.EXCHANGE_RATE
                else -> IndicatorType.UNKNOWN
            }
            
            val timestamp = try {
                java.time.Instant.parse(capturedAt).toEpochMilli()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
            
            EconomicIndicator(
                id = indicatorId,
                type = indicatorType,
                name = name,
                value = value,
                unit = unit,
                changeRate = changeRate,
                capturedAt = timestamp,
                source = source
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching indicator $indicatorId: ${e.message}", e)
            null
        }
    }
}
