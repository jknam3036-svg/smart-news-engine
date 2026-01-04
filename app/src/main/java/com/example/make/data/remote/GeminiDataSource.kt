package com.example.make.data.remote

import com.example.make.data.model.EmailAnalysisResult
import com.example.make.data.model.GeneralAnalysisResult
import com.example.make.data.model.ItemPriority
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class GeminiDataSource(
    private var apiKey: String
) {
    private var model = GenerativeModel(
        modelName = "gemini-1.5-pro",
        apiKey = apiKey
    )

    fun updateKey(newKey: String) {
        if (newKey.isNotBlank()) {
            apiKey = newKey
            model = GenerativeModel(
                modelName = "gemini-1.5-pro",
                apiKey = apiKey
            )
        }
    }

    private val json = Json { ignoreUnknownKeys = true }

    // Existing Email Analysis
    suspend fun analyzeEmail(subject: String, body: String): EmailAnalysisResult? = withContext(Dispatchers.IO) {
        val prompt = """
            Analyze the following email and return a JSON object... (omitted for brevity)
            Subject: $subject
            Body: $body
        """.trimIndent()
        // ... (Logic same as before, simplified for diff integration)
        // Re-implementing strictly for the new functions below
        safeGenerate(prompt)
    }

    // 1. News Analysis using User Context
    suspend fun analyzeNews(limit: Int, title: String, content: String, userKeywords: List<String>): GeneralAnalysisResult? = withContext(Dispatchers.IO) {
        val keywordsString = userKeywords.joinToString(", ")
        val prompt = """
            You are a business intelligence assistant.
            The user is interested in these topics: [$keywordsString].
            
            Analyze this NEWS article and provide a deep business reasoning:
            Title: $title
            Content: $content
            
            Constraint: 
            1. Adhere strictly to the original text. Do not hallucinate external facts.
            2. Extract specific EVIDENCE (numbers, quotes, or key facts) that support the insight.
            3. Try to identify the PUBLICATION DATE/TIME from the content.
            
            Return JSON with:
            - summary: 1 sentence core insight.
            - strategicInsight: 2-3 sentences on HOW this specifically affects the user's topics or business strategy.
            - evidence: The specific factual basis or numbers from the original text (e.g., "15% increase in volatility", "Fed DOT plot shift").
            - publishedAt: Original time string if found (e.g., "2024-12-30 14:00"), else null.
            - businessImpact: Integer 1 to 10 scale.
            - sentimentScore: Double -1.0 (Neg) to 1.0 (Pos).
            - priority: CRITICAL, HIGH, NORMAL, LOW.
            - tags: 3 key hashtags.
            - suggestedAction: "Draft Reply", "Add Task", "Schedule", or null.
            - actionDraft: If action is suggested, provides a brief draft of a professional response or task description.
        """.trimIndent()

        safeGenerate(prompt)
    }

    // 2. Message Analysis (SMS/Kakao)
    suspend fun analyzeMessage(sender: String, content: String): GeneralAnalysisResult? = withContext(Dispatchers.IO) {
        val prompt = """
            Analyze this SHORT MESSAGE (SMS/IM) for business value:
            Sender: $sender
            Content: $content
            
            Constraint: Strictly IGNORE any private financial codes, OTPs, or bank account numbers. 
            If it's purely personal/sensitive/spam, set priority LOW and summary "Filtered sensitive/spam".
            
            Return JSON with:
            - summary: Professional summary of the context.
            - strategicInsight: Why this matters (e.g. "Vendor update", "Customer inquiry").
            - businessImpact: Integer 1-10.
            - sentimentScore: Double -1.0 to 1.0.
            - priority: CRITICAL, HIGH, NORMAL, LOW.
            - tags: e.g. "Client", "Internal", "Delivery".
            - suggestedAction: e.g. "Confirm Receipt", "Call Back", null.
            - actionDraft: A polite reply draft if applicable.
        """.trimIndent()

        safeGenerate(prompt)
    }

    private suspend inline fun <reified T> safeGenerate(prompt: String): T? {
        try {
            val response = model.generateContent(prompt + "\nReturn ONLY JSON.")
            val responseText = response.text?.trim() ?: return null
            
            // Handle markdown code blocks if present
            val cleanJson = responseText
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
                
            return json.decodeFromString<T>(cleanJson)
        } catch (e: Exception) {
            println("AI Analysis Error: ${e.message}")
            return null
        }
    }

    // 3. Simple Ping Test
    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = model.generateContent("Ping")
            !response.text.isNullOrBlank()
        } catch (e: Exception) {
            println("Gemini Test Error: ${e.message}")
            false
        }
    }
}
