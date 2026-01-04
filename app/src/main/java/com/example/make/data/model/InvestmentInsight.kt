package com.example.make.data.model

import com.google.firebase.Timestamp

data class InvestmentInsight(
    val id: String = "",
    val content: Content = Content(),
    val intelligence: Intelligence = Intelligence(),
    val meta_data: MetaData = MetaData()
)

data class Content(
    val original_title: String = "",
    val korean_title: String = "",
    val korean_body: String = ""
)

data class Intelligence(
    val impact_score: Int = 0,
    val market_sentiment: String = "NEUTRAL",
    val actionable_insight: String = "",
    val related_assets: List<String> = emptyList()
)

data class MetaData(
    val source_name: String = "",
    val original_url: String = "",
    val published_at: String = "",
    val analyzed_at: Timestamp? = null
)
