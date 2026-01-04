package com.example.make.data.local.dao

import androidx.room.*
import com.example.make.data.local.entity.IntelligenceEntity
import com.example.make.data.local.entity.SmartNewsEntity
import com.example.make.data.local.entity.ItemRelationEntity
import com.example.make.data.local.entity.TagEntity
import com.example.make.data.local.entity.ItemTagCrossRef
import kotlinx.coroutines.flow.Flow

data class PopulatedItem(
    @Embedded val item: IntelligenceEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "name",
        associateBy = Junction(
            value = ItemTagCrossRef::class,
            parentColumn = "itemId",
            entityColumn = "tagName"
        )
    )
    val tags: List<TagEntity>,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ItemRelationEntity::class,
            parentColumn = "sourceItemId",
            entityColumn = "targetItemId"
        )
    )
    val relatedItems: List<IntelligenceEntity>
)

@Dao
interface IntelligenceDao {
    // 1. Basic CRUD
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: IntelligenceEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItemTagCrossRef(crossRef: ItemTagCrossRef)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRelation(relation: ItemRelationEntity)

    @Query("DELETE FROM intelligence_items WHERE id = :id")
    suspend fun deleteItem(id: String)

    @Query("DELETE FROM intelligence_items WHERE capturedAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)

    @Query("DELETE FROM intelligence_items")
    suspend fun deleteAll()

    @Query("SELECT * FROM intelligence_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: String): IntelligenceEntity?

    // 2. Complex Queries
    
    // Get One Item with its Tags and Related Items
    @Transaction
    @Query("SELECT * FROM intelligence_items WHERE id = :id")
    suspend fun getItemWithRelations(id: String): PopulatedItem?

    // Global Search: e.g., Find items related to "Project A" or specific sender
    @Transaction
    @Query("""
        SELECT * FROM intelligence_items 
        WHERE rawContent LIKE '%' || :query || '%' 
        OR summary LIKE '%' || :query || '%'
        ORDER BY capturedAt DESC
    """)
    fun searchItems(query: String): Flow<List<PopulatedItem>>

    // Get Items by Tag (e.g., "Show me all #Finance items")
    @Transaction
    @Query("""
        SELECT * FROM intelligence_items 
        INNER JOIN item_tags ON intelligence_items.id = item_tags.itemId 
        WHERE item_tags.tagName = :tag
        ORDER BY intelligence_items.capturedAt DESC
    """)
    fun getItemsByTag(tag: String): Flow<List<PopulatedItem>>

    // Context Query: Find items related to a specific Item (Trace the graph)
    // e.g., "Show me why this Alert happened" -> finds the Economic Indicator linked to it
    @Transaction
    @Query("""
        SELECT * FROM intelligence_items
        WHERE id IN (
            SELECT targetItemId FROM item_relations WHERE sourceItemId = :itemId
            UNION
            SELECT sourceItemId FROM item_relations WHERE targetItemId = :itemId
        )
    """)
    suspend fun getContextualItems(itemId: String): List<IntelligenceEntity>

    // Smart News
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSmartNews(news: List<SmartNewsEntity>)

    @Query("SELECT * FROM smart_news ORDER BY publishedAt DESC")
    suspend fun getAllSmartNews(): List<SmartNewsEntity>
    
    @Query("DELETE FROM smart_news")
    suspend fun deleteAllSmartNews()

    @Query("DELETE FROM smart_news WHERE publishedAt < :timestamp")
    suspend fun deleteSmartNewsOlderThan(timestamp: String)
}
