package com.example.make.data.repository

import android.content.Context
import android.database.Cursor
import android.provider.Telephony
import com.example.make.data.local.dao.IntelligenceDao
import com.example.make.data.local.entity.IntelligenceEntity
import com.example.make.data.model.ItemPriority
import com.example.make.data.model.SMSCategory
import com.example.make.data.model.SMSMessage
import com.example.make.data.model.SourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class SmsRepository {

    suspend fun syncSmsFromDevice(context: Context, dao: IntelligenceDao) {
        withContext(Dispatchers.IO) {
            val cursor: Cursor? = context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
                null,
                null,
                "${Telephony.Sms.DATE} DESC LIMIT 50"
            )

            cursor?.use {
                val idxId = it.getColumnIndex(Telephony.Sms._ID)
                val idxAddress = it.getColumnIndex(Telephony.Sms.ADDRESS)
                val idxBody = it.getColumnIndex(Telephony.Sms.BODY)
                val idxDate = it.getColumnIndex(Telephony.Sms.DATE)

                while (it.moveToNext()) {
                    val smsId = it.getString(idxId)
                    val address = it.getString(idxAddress)
                    val body = it.getString(idxBody)
                    val date = it.getLong(idxDate)

                    // Basic categorization (spam filter simulation)
                    val priority = if (body.contains("Code") || body.contains("인증")) ItemPriority.LOW else ItemPriority.NORMAL
                    
                    val entity = IntelligenceEntity(
                        id = "sms_$smsId",
                        sourceType = SourceType.SMS,
                        sourceId = smsId,
                        rawContent = body,
                        summary = body, // Raw body as summary initially
                        priority = priority,
                        capturedAt = date,
                        senderOrSource = address ?: "Unknown",
                        type = "SMS" // Can distinguish MMS later
                    )
                    dao.insertItem(entity)
                }
            }
        }
    }
}
