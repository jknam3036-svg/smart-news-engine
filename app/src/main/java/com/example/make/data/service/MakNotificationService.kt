package com.example.make.data.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.make.data.model.IMMessage
import com.example.make.data.model.MessengerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class MakNotificationService : NotificationListenerService() {

    companion object {
        // Shared flow to broadcast messages to UI (실제 데이터는 Room DB에 저장됨)
        val incomingMessages = MutableSharedFlow<IMMessage>(extraBufferCapacity = 64)
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return

        val packageName = sbn.packageName
        
        // Filter for specific messenger/email apps
        val sourcePackage = when (packageName) {
            "com.kakao.talk" -> "KAKAOTALK"
            "jp.naver.line.android" -> "LINE"
            "com.slack" -> "SLACK"
            "org.telegram.messenger" -> "TELEGRAM"
            "com.google.android.gm" -> "GMAIL"
            "com.samsung.android.email.provider" -> "SAMSUNG_EMAIL"
            else -> return // Ignore other apps
        }

        val extras = sbn.notification.extras
        
        // Extract Title (Sender) and Text (Message)
        val title = extras.getString(Notification.EXTRA_TITLE) ?: "Unknown"
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        
        // Skip if empty or system messages
        if (text.isBlank()) return

        // Save to Room DB
        val app = applicationContext as? com.example.make.MakeApplication ?: return
        val dao = app.database.intelligenceDao()

        // Determine type
        val targetSourceType = if (sourcePackage == "GMAIL" || sourcePackage == "SAMSUNG_EMAIL") {
            com.example.make.data.model.SourceType.EMAIL
        } else {
            com.example.make.data.model.SourceType.MESSENGER
        }

        scope.launch {
            // Use notification key as unique ID to prevent duplicates
            val uniqueId = "noti_${sbn.key}"
            
            // Check if this notification was already processed
            val existing = dao.getItemById(uniqueId)
            if (existing != null) {
                // Already processed, skip
                return@launch
            }
            
            val entity = com.example.make.data.local.entity.IntelligenceEntity(
                id = uniqueId,
                sourceType = targetSourceType,
                sourceId = sbn.key,
                rawContent = text,
                summary = text, // API restriction: we only get preview text
                priority = com.example.make.data.model.ItemPriority.NORMAL,
                capturedAt = sbn.postTime,
                senderOrSource = if (subText != null) "$subText ($title)" else title,
                type = sourcePackage 
            )
            dao.insertItem(entity)
        }
    }
}
