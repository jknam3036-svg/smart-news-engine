package com.example.make.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.make.data.model.SourceType
import com.example.make.data.model.ItemPriority
import com.example.make.data.model.ActionStatus

@Entity(tableName = "intelligence_items")
data class IntelligenceEntity(
    @PrimaryKey val id: String,
    val sourceType: SourceType, // EMAIL, NEWS, SMS...
    val sourceId: String,       // Original ID from source (e.g., Email Message-ID)
    
    // Core Content
    val rawContent: String,     // Full text content
    val summary: String?,       // AI Generated Summary
    val strategicInsight: String? = null,
    val businessImpact: Int? = null,
    val sentimentScore: Double? = null,
    
    // Metadata
    val priority: ItemPriority,
    val capturedAt: Long,
    val senderOrSource: String, // Sender Name or News Provider
    
    // Action State
    val actionStatus: ActionStatus = ActionStatus.NONE,
    val suggestedAction: String? = null,
    val actionDraft: String? = null,
    
    // Extensions
    val type: String? = null, // e.g. "KAKAOTALK", "SLACK"
    val sourceUrl: String? = null,
    val evidence: String? = null,
    val publishedAt: String? = null,
    val correlationContext: String? = null // JSON or CSV of related IDs
)

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val name: String, // e.g., "ProjectA", "Urgent", "Finance"
    val category: String = "GENERAL" // "PROJECT", "TOPIC", "PERSON"
)

@Entity(tableName = "item_tags", primaryKeys = ["itemId", "tagName"])
data class ItemTagCrossRef(
    val itemId: String,
    val tagName: String
)

@Entity(tableName = "item_relations", primaryKeys = ["sourceItemId", "targetItemId"])
data class ItemRelationEntity(
    val sourceItemId: String,
    val targetItemId: String,
    val relationType: String // "CAUSED_BY", "REFERENCE", "CONTEXT", "ATTACHMENT"
)

@Entity(tableName = "smart_news")
data class SmartNewsEntity(
    @PrimaryKey val id: String,
    val source: String,
    val sourceType: String,
    val title: String,
    val summary: String,
    val fullContent: String,
    val aiInsight: String,
    val aiKeyPointsJson: String,
    val sentiment: String,
    val impactScore: Int,
    val tagsJson: String,
    val styleTagsJson: String,
    val publishedAt: String,
    val sourceUrl: String,
    val authorHandle: String? = null,
    val authorBio: String? = null,
    val authorStyle: String? = null,
    val priority: String
)
