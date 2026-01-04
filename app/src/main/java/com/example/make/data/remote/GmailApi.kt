package com.example.make.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.*

// Basic Gmail API models
@Serializable
data class GmailMessageListResponse(
    val messages: List<GmailMessageSummary> = emptyList(),
    val resultSizeEstimate: Int
)

@Serializable
data class GmailMessageSummary(
    val id: String,
    val threadId: String
)

@Serializable
data class GmailMessageDetail(
    val id: String,
    val threadId: String,
    val snippet: String,
    val payload: GmailPayload,
    val internalDate: String? = null
)

@Serializable
data class GmailPayload(
    val headers: List<GmailHeader>,
    val body: GmailBody? = null,
    val parts: List<GmailPart>? = null, // For multipart messages
    val mimeType: String? = null
)

@Serializable
data class GmailPart(
    val mimeType: String,
    val body: GmailBody? = null,
    val parts: List<GmailPart>? = null, // Nested parts
    val headers: List<GmailHeader>? = null
)

@Serializable
data class GmailHeader(
    val name: String,
    val value: String
)

@Serializable
data class GmailBody(
    val data: String? = null, // Base64url encoded
    val size: Int? = null
)

// For sending messages
@Serializable
data class GmailSendRequest(
    val raw: String // Base64url encoded RFC 2822 formatted message
)

@Serializable
data class GmailSendResponse(
    val id: String,
    val threadId: String,
    val labelIds: List<String>? = null
)

interface GmailApi {
    @GET("gmail/v1/users/me/messages")
    suspend fun listMessages(
        @Header("Authorization") token: String,
        @Query("maxResults") maxResults: Int = 10,
        @Query("q") query: String? = null
    ): GmailMessageListResponse

    @GET("gmail/v1/users/me/messages/{id}")
    suspend fun getMessage(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Query("format") format: String = "full" // full, metadata, minimal
    ): GmailMessageDetail

    @POST("gmail/v1/users/me/messages/send")
    suspend fun sendMessage(
        @Header("Authorization") token: String,
        @Body request: GmailSendRequest
    ): GmailSendResponse
}

// Helper class for email body extraction
object GmailBodyExtractor {
    /**
     * Extract plain text or HTML body from Gmail message
     */
    fun extractBody(payload: GmailPayload): EmailBody {
        var plainText: String? = null
        var htmlText: String? = null
        
        // Check if body is directly in payload
        if (payload.body?.data != null) {
            val decoded = decodeBase64Url(payload.body.data)
            when {
                payload.mimeType?.contains("text/plain") == true -> plainText = decoded
                payload.mimeType?.contains("text/html") == true -> htmlText = decoded
            }
        }
        
        // Check multipart
        payload.parts?.let { parts ->
            extractFromParts(parts).let { body ->
                if (plainText == null) plainText = body.plainText
                if (htmlText == null) htmlText = body.htmlText
            }
        }
        
        return EmailBody(
            plainText = plainText,
            htmlText = htmlText
        )
    }
    
    private fun extractFromParts(parts: List<GmailPart>): EmailBody {
        var plainText: String? = null
        var htmlText: String? = null
        
        for (part in parts) {
            when {
                part.mimeType == "text/plain" && part.body?.data != null -> {
                    plainText = decodeBase64Url(part.body.data)
                }
                part.mimeType == "text/html" && part.body?.data != null -> {
                    htmlText = decodeBase64Url(part.body.data)
                }
                part.mimeType?.startsWith("multipart/") == true && part.parts != null -> {
                    val nested = extractFromParts(part.parts)
                    if (plainText == null) plainText = nested.plainText
                    if (htmlText == null) htmlText = nested.htmlText
                }
            }
        }
        
        return EmailBody(plainText, htmlText)
    }
    
    private fun decodeBase64Url(data: String): String {
        return try {
            // Gmail uses base64url encoding (RFC 4648)
            val normalized = data.replace('-', '+').replace('_', '/')
            val padded = when (normalized.length % 4) {
                2 -> normalized + "=="
                3 -> normalized + "="
                else -> normalized
            }
            String(android.util.Base64.decode(padded, android.util.Base64.DEFAULT))
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * Extract specific header value
     */
    fun getHeader(payload: GmailPayload, headerName: String): String? {
        return payload.headers.find { 
            it.name.equals(headerName, ignoreCase = true) 
        }?.value
    }
}

data class EmailBody(
    val plainText: String?,
    val htmlText: String?
) {
    fun getDisplayText(): String {
        return plainText ?: htmlText?.let { stripHtml(it) } ?: ""
    }
    
    private fun stripHtml(html: String): String {
        return html
            .replace(Regex("<[^>]*>"), "") // Remove HTML tags
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .trim()
    }
}

// Helper for composing reply messages
object GmailComposer {
    /**
     * Create RFC 2822 formatted email message
     */
    fun composeReply(
        to: String,
        subject: String,
        body: String,
        inReplyTo: String? = null,
        references: String? = null,
        threadId: String? = null
    ): String {
        val message = buildString {
            appendLine("To: $to")
            appendLine("Subject: $subject")
            appendLine("Content-Type: text/plain; charset=utf-8")
            
            if (inReplyTo != null) {
                appendLine("In-Reply-To: $inReplyTo")
            }
            if (references != null) {
                appendLine("References: $references")
            }
            if (threadId != null) {
                appendLine("Thread-Id: $threadId")
            }
            
            appendLine() // Empty line between headers and body
            append(body)
        }
        
        return encodeBase64Url(message.toByteArray())
    }
    
    private fun encodeBase64Url(data: ByteArray): String {
        return android.util.Base64.encodeToString(
            data, 
            android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
        )
    }
}
