package com.example.make.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class EmailCategory {
    CRITICAL, PROJECT, ADMIN, NEWSLETTER, SPAM, UNKNOWN
}

@Serializable
data class EmailAnalysisResult(
    val category: EmailCategory,
    val summary: String?,
    val isUrgent: Boolean,
    val suggestedActions: List<String> = emptyList(),
    val evidence: String? = null,
    val publishedAt: String? = null
)

data class Email(
    val id: String,
    val subject: String,
    val sender: String,
    val body: String,
    val receivedAt: Long,
    val analysis: EmailAnalysisResult? = null
)
