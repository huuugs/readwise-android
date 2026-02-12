package com.readwise.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 阅读位置实体
 */
@Entity(tableName = "reading_positions")
data class ReadingPositionEntity(
    @PrimaryKey
    val bookId: String,
    val chapterIndex: Int = 0,
    val pageIndex: Int? = null, // PDF 页码
    val paragraphIndex: Int = 0,
    val charOffset: Int = 0,
    val progress: Float = 0f, // 0.0 - 1.0
    val updateTime: Long = System.currentTimeMillis()
)
