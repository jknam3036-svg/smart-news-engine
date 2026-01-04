package com.example.make.data.repository

import com.example.make.data.local.dao.IntelligenceDao
import com.example.make.data.local.entity.IntelligenceEntity
import com.example.make.data.local.entity.ItemTagCrossRef
import com.example.make.data.model.EmailAnalysisResult
import com.example.make.data.model.ItemPriority
import com.example.make.data.model.SourceType
import com.example.make.data.remote.GeminiDataSource
import com.example.make.data.remote.GmailApi
import kotlinx.coroutines.flow.Flow

class MailRepository(
    private val gmailApi: GmailApi,
    private val geminiDataSource: GeminiDataSource,
    private val intelligenceDao: IntelligenceDao
) {
    // 1. Fetch from Gmail
    // 2. Analyze with Gemini
    // 3. Save to Room
    suspend fun syncEmails(accessToken: String) {
        try {
            // Step 1: List Messages
            val listResponse = gmailApi.listMessages("Bearer $accessToken", maxResults = 10)
            
            listResponse.messages.forEach { msgSummary ->
                // Check if already synced
                if (intelligenceDao.getItemById(msgSummary.id) != null) return@forEach

                // Step 2: Get Details
                val detail = gmailApi.getMessage("Bearer $accessToken", msgSummary.id)
                
                // Extract Subject & Body
                val subject = detail.payload.headers.find { it.name == "Subject" }?.value ?: "(No Subject)"
                val sender = detail.payload.headers.find { it.name == "From" }?.value ?: "Unknown"
                val bodyText = com.example.make.data.remote.GmailBodyExtractor.extractBody(detail.payload).getDisplayText()
                
                // Step 3: Analyze with Gemini
                val analysis: EmailAnalysisResult? = geminiDataSource.analyzeEmail(subject, bodyText.take(1000))
                
                // Determine Priority and Tags based on AI result
                val priority = if (analysis?.isUrgent == true) ItemPriority.CRITICAL else ItemPriority.NORMAL
                val aiSummary = analysis?.summary ?: detail.snippet
                
                // Step 4: Map to Entity
                val entity = IntelligenceEntity(
                    id = detail.id,
                    sourceType = SourceType.EMAIL,
                    sourceId = detail.id,
                    rawContent = bodyText, // Store full body text
                    summary = aiSummary,
                    priority = priority,
                    capturedAt = System.currentTimeMillis(),
                    senderOrSource = sender,
                    suggestedAction = analysis?.suggestedActions?.joinToString(", "),
                    evidence = analysis?.evidence,
                    publishedAt = analysis?.publishedAt
                )
                
                // Step 5: Save
                intelligenceDao.insertItem(entity)
                
                // Save Tags
                analysis?.category?.name?.let { categoryName ->
                     intelligenceDao.insertItemTagCrossRef(ItemTagCrossRef(entity.id, categoryName))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getFullMessage(accessToken: String, messageId: String): com.example.make.data.remote.GmailMessageDetail {
        return gmailApi.getMessage("Bearer $accessToken", messageId)
    }

    suspend fun sendReply(accessToken: String, originalMessageId: String, replyContent: String): Boolean {
        return try {
            val original = gmailApi.getMessage("Bearer $accessToken", originalMessageId)
            
            val to = com.example.make.data.remote.GmailBodyExtractor.getHeader(original.payload, "Reply-To") 
                ?: com.example.make.data.remote.GmailBodyExtractor.getHeader(original.payload, "From") 
                ?: return false
            
            val subject = com.example.make.data.remote.GmailBodyExtractor.getHeader(original.payload, "Subject") ?: ""
            val replySubject = if (subject.startsWith("Re:", ignoreCase = true)) subject else "Re: $subject"
            
            val messageIdHeader = com.example.make.data.remote.GmailBodyExtractor.getHeader(original.payload, "Message-ID")
            val references = com.example.make.data.remote.GmailBodyExtractor.getHeader(original.payload, "References")
            val newReferences = if (references != null) "$references $messageIdHeader" else messageIdHeader
            
            val rawMessage = com.example.make.data.remote.GmailComposer.composeReply(
                to = to,
                subject = replySubject,
                body = replyContent,
                inReplyTo = messageIdHeader,
                references = newReferences,
                threadId = original.threadId
            )
            
            val response = gmailApi.sendMessage(
                token = "Bearer $accessToken",
                request = com.example.make.data.remote.GmailSendRequest(raw = rawMessage)
            )
            
            response.id.isNotEmpty()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getAllEmails(): Flow<List<com.example.make.data.local.dao.PopulatedItem>> {
        // Simple search for all items, realistically would filter strictly by SourceType.EMAIL
        return intelligenceDao.searchItems("") 
    }
}
