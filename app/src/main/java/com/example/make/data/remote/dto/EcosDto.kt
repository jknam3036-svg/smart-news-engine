package com.example.make.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class EcosWrapper(
    @SerialName("StatisticSearch") val statisticSearch: EcosStatisticSearch? = null,
    @SerialName("RESULT") val result: EcosResult? = null
)

@Serializable
data class EcosStatisticSearch(
    val list_total_count: Int,
    val row: List<EcosItem>
)

@Serializable
data class EcosItem(
    val STAT_CODE: String,
    val STAT_NAME: String,
    val ITEM_CODE1: String,
    val ITEM_NAME1: String,
    val ITEM_CODE2: String?,
    val ITEM_NAME2: String?,
    val ITEM_CODE3: String?,
    val ITEM_NAME3: String?,
    val ITEM_CODE4: String?,
    val ITEM_NAME4: String?,
    val UNIT_NAME: String?,
    val TIME: String,
    val DATA_VALUE: String
)

@Serializable
data class EcosResult(
    val CODE: String,
    val MESSAGE: String
)
