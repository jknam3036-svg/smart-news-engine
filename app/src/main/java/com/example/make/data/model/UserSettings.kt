package com.example.make.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserSettings(
    val accounts: List<EmailAccountConfig> = emptyList(),
    val monitoringKeywords: List<String> = emptyList(), // e.g., "AI", "Semiconductor"
    val filterRules: List<FilterRule> = emptyList(),
    val notificationSettings: NotificationSettings = NotificationSettings()
)

@Serializable
data class EmailAccountConfig(
    val id: String,
    val email: String,
    val provider: String, // "GMAIL", "OUTLOOK"
    val isEnabled: Boolean = true
)

@Serializable
data class FilterRule(
    val id: String,
    val target: FilterTarget, // SUBJECT, BODY, SENDER
    val keyword: String,
    val action: FilterAction // MARK_AS_SPAM, MARK_AS_CRITICAL, MUTE
)

@Serializable
enum class FilterTarget {
    SUBJECT, BODY, SENDER, APP_NAME // APP_NAME for Notification Listener
}

@Serializable
enum class FilterAction {
    MARK_AS_SPAM,
    MARK_AS_CRITICAL,
    MARK_AS_IMPORTANT,
    ARCHIVE,
    DELETE,
    MUTE_NOTIFICATION
}

@Serializable
data class NotificationSettings(
    val alertOnCritical: Boolean = true,
    val alertOnImportant: Boolean = true,
    val alertOnNormal: Boolean = false,
    val muteAll: Boolean = false
)
