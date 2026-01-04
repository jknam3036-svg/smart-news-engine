package com.example.make.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class SourceType {
    EMAIL, NEWS, SMS, ECONOMY, MESSENGER
}

@Serializable
enum class ItemPriority {
    LOW, NORMAL, HIGH, CRITICAL
}

@Serializable
enum class ActionStatus {
    PENDING, COMPLETED, IGNORED, NONE
}

@Serializable
data class IntelligenceItem(
    val id: String,
    val sourceType: SourceType,
    val sourceId: String,
    
    // AI Analysis Data
    val summary: String,
    val strategicInsight: String? = null,
    val businessImpact: Int? = null, // 1-10 scale
    val sentimentScore: Double? = null, // -1.0 to 1.0
    val priority: ItemPriority,
    val tags: List<String>,
    
    // Automation & Action
    val suggestedAction: String? = null,
    val actionDraft: String? = null, // Pre-written email/message draft
    val actionStatus: ActionStatus = ActionStatus.NONE,
    
    // Context Intelligence
    val contextCorrelationIds: List<String> = emptyList(), // Related IDs
    val sourceUrl: String? = null,
    val evidence: String? = null, // Factual basis from the original text
    val publishedAt: String? = null, // Original article/msg creation time string
    
    val capturedAt: Long
)
