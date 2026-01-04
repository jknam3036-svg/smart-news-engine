package com.example.make.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class IndicatorType {
    BASE_RATE,    // 기준금리
    CD_RATE,      // CD 금리
    US_TREASURY,  // 미국 국채 (10년물 등)
    KR_TREASURY,  // 한국 국고채
    EXCHANGE_RATE, // 환율
    STOCK_INDEX,   // 주가 지수
    UNKNOWN
}

@Serializable
data class EconomicIndicator(
    val id: String,
    val type: IndicatorType,
    val name: String,         // e.g. "US Treasury 10Y"
    val value: Double,        // e.g. 4.25
    val unit: String,         // e.g. "%", "KRW"
    val changeRate: Double,   // 전일 대비 등락폭 e.g. +0.05
    val capturedAt: Long,
    val source: String = "Unknown" // e.g., "FRED", "BOK", "Reuters"
)
