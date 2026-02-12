package com.readwise.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.readwise.core.model.BookmarkType

/**
 * 书签/高亮/批注重体
 */
@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val bookId: String,
    val type: BookmarkType,
    val chapterIndex: Int = 0,
    val pageIndex: Int? = null, // PDF 页码
    val startOffset: Int, // 字符偏移量
    val endOffset: Int,
    val selectedText: String? = null, // 选中的文本
    val color: Int? = null, // 高亮颜色 (ARGB)
    val note: String? = null, // 批注内容
    val createTime: Long = System.currentTimeMillis(),
    val modifyTime: Long = System.currentTimeMillis()
)
