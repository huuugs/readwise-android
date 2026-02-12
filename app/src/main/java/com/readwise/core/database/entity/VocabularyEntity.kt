package com.readwise.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 生词本实体
 */
@Entity(tableName = "vocabulary")
data class VocabularyEntity(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val word: String,
    val definition: String,
    val phonetic: String? = null,
    val bookId: String? = null, // 来源书籍
    val context: String? = null, // 上下文例句
    val addTime: Long = System.currentTimeMillis(),
    val reviewCount: Int = 0, // 复习次数
    val nextReviewTime: Long? = null, // 下次复习时间
    val mastered: Boolean = false // 是否已掌握
)
