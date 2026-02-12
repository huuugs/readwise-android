package com.readwise.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.readwise.core.database.converter.ListConverter
import com.readwise.core.model.BookFormat

/**
 * 书籍实体
 */
@Entity(tableName = "books")
@TypeConverters(ListConverter::class)
data class BookEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val author: String,
    val coverPath: String?,
    val filePath: String,
    val format: String, // PDF, EPUB, MOBI, TXT, etc.
    val fileSize: Long,
    val addedTime: Long = System.currentTimeMillis(),
    val lastReadTime: Long? = null,
    val lastReadPosition: String? = null, // JSON 格式的位置信息
    val readingProgress: Float = 0f, // 0.0 - 1.0
    val category: String = "默认",
    val tags: List<String> = emptyList(), // 标签列表
    val rating: Int = 0, // 0-5 星
    val notes: String? = null,
    val wordCount: Int = 0, // 总字数
    val chapterCount: Int = 0 // 章节数
)
