package com.readwise.core.model

import android.os.Parcelable
import com.readwise.engine.common.BookFormat
import kotlinx.parcelize.Parcelize

/**
 * 书籍领域模型
 */
@Parcelize
data class Book(
    val id: String,
    val title: String,
    val author: String,
    val coverPath: String?,
    val filePath: String,
    val format: BookFormat,
    val fileSize: Long,
    val addedTime: Long = System.currentTimeMillis(),
    val lastReadTime: Long? = null,
    val lastReadPosition: ReadPosition? = null,
    val readingProgress: Float = 0f,
    val category: String = "默认",
    val tags: List<String> = emptyList(),
    val rating: Int = 0,
    val notes: String? = null,
    val wordCount: Int = 0,
    val chapterCount: Int = 0
) : Parcelable {
    val displayTitle: String get() = title.takeIf { it.isNotEmpty() } ?: "未命名"
    val displayAuthor: String get() = author.takeIf { it.isNotEmpty() } ?: "未知作者"

    val formattedFileSize: String get() = formatFileSize(fileSize)
    val readingProgressPercent: Int get() = (readingProgress * 100).toInt()

    fun hasCover(): Boolean = coverPath != null
    fun isRead(): Boolean = readingProgress >= 1f
    fun isNew(): Boolean = lastReadTime == null

    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "${size}B"
            size < 1024 * 1024 -> "${size / 1024}KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)}MB"
            else -> "${size / (1024 * 1024 * 1024)}GB"
        }
    }
}
