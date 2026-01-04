package com.example.make.data.service

import com.example.make.data.local.dao.IntelligenceDao
import com.example.make.data.local.entity.ItemRelationEntity
import com.example.make.data.model.*
import com.example.make.data.remote.GeminiDataSource
import kotlinx.coroutines.flow.first

class AiIntelligenceService(
    private val geminiDataSource: GeminiDataSource,
    private val intelligenceDao: IntelligenceDao
) {
    /**
     * Hyper-Cognitive Analysis:
     * 1. Analyze the single item deeply.
     * 2. Find contextual correlations with other stored items.
     * 3. Update the item with the synthesized intelligence.
     */
    suspend fun processIntelligence(
        itemId: String,
        rawContent: String,
        sourceType: SourceType,
        userKeywords: List<String>
    ): GeneralAnalysisResult? {
        // Step 1: Deep Analysis of the item itself
        val result = when (sourceType) {
            SourceType.NEWS, SourceType.ECONOMY -> 
                geminiDataSource.analyzeNews(5, "Source: $sourceType", rawContent, userKeywords)
            SourceType.SMS, SourceType.MESSENGER -> 
                geminiDataSource.analyzeMessage("Source: $sourceType", rawContent)
            SourceType.EMAIL -> 
                geminiDataSource.analyzeEmail("Email Analysis", rawContent)?.let {
                    GeneralAnalysisResult(
                        summary = it.summary ?: "No summary",
                        priority = when(it.category) {
                            EmailCategory.CRITICAL -> ItemPriority.CRITICAL
                            EmailCategory.PROJECT -> ItemPriority.HIGH
                            else -> ItemPriority.NORMAL
                        },
                        tags = listOf(it.category.name),
                        suggestedAction = it.suggestedActions.firstOrNull(),
                        evidence = it.evidence,
                        publishedAt = it.publishedAt
                    )
                }
            else -> null
        } ?: return null

        // Step 2: Contextual Correlation (Phase 2 core)
        // Find recent items with similar tags
        if (result.tags.isNotEmpty()) {
            val primaryTag = result.tags.first()
            val recentRelated = intelligenceDao.searchItems(primaryTag).first().take(3)
            
            if (recentRelated.isNotEmpty()) {
                val contextString = recentRelated.joinToString("\n") { 
                    "[${it.item.sourceType}] ${it.item.summary}" 
                }
                
                // Ask Gemini if they are related
                // We'll update GeminiDataSource to support synthesis later, 
                // for now we just link them in the DB
                recentRelated.forEach { related ->
                    intelligenceDao.insertRelation(
                        ItemRelationEntity(
                            sourceItemId = itemId,
                            targetItemId = related.item.id,
                            relationType = "CONTEXT"
                        )
                    )
                }
            }
        }

        return result
    }
}
