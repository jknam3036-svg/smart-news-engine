package com.example.make.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TwelveDataQuoteResponse(
    val symbol: String,
    val name: String,
    val exchange: String,
    val mic_code: String?,
    val currency: String?,
    val datetime: String,
    val timestamp: Long,
    val open: String,
    val high: String,
    val low: String,
    val close: String,
    val volume: String?,
    val previous_close: String?,
    val change: String?,
    @SerialName("percent_change") val percentChange: String?,
    val average_volume: String?,
    val is_market_open: Boolean?
)

@Serializable
data class TwelveDataPriceResponse(
    val price: String
)

@Serializable
data class TwelveDataTimeSeriesResponse(
    val meta: TimeSeriesMeta? = null,
    val values: List<TimeSeriesValue>? = null,
    val status: String? = null
)

@Serializable
data class TimeSeriesMeta(
    val symbol: String? = null,
    val interval: String? = null,
    val currency: String? = null,
    val exchange_timezone: String? = null,
    val exchange: String? = null,
    val type: String? = null
)

@Serializable
data class TimeSeriesValue(
    val datetime: String? = null,
    val open: String? = null,
    val high: String? = null,
    val low: String? = null,
    val close: String? = null,
    val volume: String? = null
)
