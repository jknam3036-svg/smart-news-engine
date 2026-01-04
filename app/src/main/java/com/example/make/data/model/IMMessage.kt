package com.example.make.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class MessengerType {
    KAKAOTALK, TELEGRAM, LINE, SLACK, OTHER
}

@Serializable
data class IMMessage(
    val id: String,
    val type: MessengerType,
    val senderName: String,   // 알림 타이틀 (Sender)
    val roomName: String?,    // 단체 톡방 이름 (있을 경우)
    val content: String,      // 알림 내용
    val receivedAt: Long,
    val isGroupChat: Boolean
)
