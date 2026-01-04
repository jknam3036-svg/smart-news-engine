package com.example.make.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.make.data.local.dao.IntelligenceDao
import com.example.make.data.local.entity.IntelligenceEntity
import com.example.make.data.local.entity.TagEntity
import com.example.make.data.local.entity.ItemTagCrossRef
import com.example.make.data.local.entity.ItemRelationEntity
import com.example.make.data.local.entity.SmartNewsEntity

@Database(
    entities = [
        IntelligenceEntity::class,
        TagEntity::class,
        ItemTagCrossRef::class,
        ItemRelationEntity::class,
        SmartNewsEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun intelligenceDao(): IntelligenceDao
}
