package com.readwise.core.repository

import com.readwise.core.database.dao.BookmarkDao
import com.readwise.core.database.entity.BookmarkEntity
import com.readwise.core.model.BookmarkType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 书签仓储
 */
@Singleton
class BookmarkRepository @Inject constructor(
    private val bookmarkDao: BookmarkDao
) {
    fun getBookmarksByBookId(bookId: String): Flow<List<Bookmark>> {
        return bookmarkDao.getBookmarksByBookId(bookId).map { entities ->
            entities.map { it.toBookmark() }
        }
    }

    fun getBookmarksByType(bookId: String, type: BookmarkType): Flow<List<Bookmark>> {
        return bookmarkDao.getBookmarksByType(bookId, type).map { entities ->
            entities.map { it.toBookmark() }
        }
    }

    suspend fun addBookmark(bookmark: Bookmark): String {
        val entity = bookmark.toEntity()
        bookmarkDao.insertBookmark(entity)
        return entity.id
    }

    suspend fun updateBookmark(bookmark: Bookmark) {
        bookmarkDao.updateBookmark(bookmark.toEntity())
    }

    suspend fun deleteBookmark(bookmarkId: String) {
        bookmarkDao.deleteBookmarkById(bookmarkId)
    }

    suspend fun deleteBookmarksByBookId(bookId: String) {
        bookmarkDao.deleteBookmarksByBookId(bookId)
    }

    suspend fun getBookmarkCount(bookId: String): Int {
        return bookmarkDao.getBookmarkCount(bookId)
    }
}

/**
 * 书签领域模型
 */
data class Bookmark(
    val id: String = "",
    val bookId: String,
    val type: BookmarkType,
    val chapterIndex: Int = 0,
    val pageIndex: Int? = null,
    val startOffset: Int,
    val endOffset: Int,
    val selectedText: String? = null,
    val color: Int? = null,
    val note: String? = null,
    val createTime: Long = System.currentTimeMillis()
)

private fun Bookmark.toEntity(): BookmarkEntity {
    return BookmarkEntity(
        id = id.takeIf { it.isNotEmpty() } ?: java.util.UUID.randomUUID().toString(),
        bookId = bookId,
        type = type,
        chapterIndex = chapterIndex,
        pageIndex = pageIndex,
        startOffset = startOffset,
        endOffset = endOffset,
        selectedText = selectedText,
        color = color,
        note = note,
        createTime = createTime
    )
}

private fun BookmarkEntity.toBookmark(): Bookmark {
    return Bookmark(
        id = id,
        bookId = bookId,
        type = type,
        chapterIndex = chapterIndex,
        pageIndex = pageIndex,
        startOffset = startOffset,
        endOffset = endOffset,
        selectedText = selectedText,
        color = color,
        note = note,
        createTime = createTime
    )
}
