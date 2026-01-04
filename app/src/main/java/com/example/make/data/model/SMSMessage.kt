package com.example.make.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class SMSCategory {
    DELIVERY, PERSONAL, SPAM, UNKNOWN
}

@Serializable
data class SMSMessage(
    val id: String,
    val sender: String,
    val body: String,
    val receivedAt: Long,
    val category: SMSCategory = SMSCategory.UNKNOWN
)
