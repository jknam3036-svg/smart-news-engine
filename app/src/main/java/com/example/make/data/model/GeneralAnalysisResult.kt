package com.example.make.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GeneralAnalysisResult(
    val summary: String,
    val strategicInsight: String? = null,
    val businessImpact: Int? = null,
    val sentimentScore: Double? = null,
    val priority: ItemPriority,
    val tags: List<String> = emptyList(),
    val suggestedAction: String? = null,
    val actionDraft: String? = null,
    val evidence: String? = null,
    val publishedAt: String? = null
)
