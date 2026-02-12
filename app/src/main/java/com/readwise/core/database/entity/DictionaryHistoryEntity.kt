package com.readwise.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 词典查询历史
 */
@Entity(tableName = "dictionary_history")
data class DictionaryHistoryEntity(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val word: String,
    val result: String, // JSON 格式的查询结果
    val queryTime: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
