package com.readwise.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 章节总结实体
 */
@Entity(tableName = "chapter_summaries")
data class ChapterSummaryEntity(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val bookId: String,
    val chapterIndex: Int,
    val chapterTitle: String? = null,
    val summary: String, // 总结内容
    val keyPoints: String, // JSON 数组格式的关键要点
    val quotes: String? = null, // JSON 数组格式的重要引用
    val generateTime: Long = System.currentTimeMillis(),
    val aiModel: String? = null // 使用的AI模型
)
