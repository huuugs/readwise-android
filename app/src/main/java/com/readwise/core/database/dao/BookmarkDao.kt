package com.readwise.core.database.dao

import androidx.room.*
import com.readwise.core.database.entity.BookmarkEntity
import com.readwise.core.model.BookmarkType
import kotlinx.coroutines.flow.Flow

/**
 * 书签数据访问对象
 */
@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY createTime DESC")
    fun getBookmarksByBookId(bookId: String): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId AND type = :type ORDER BY createTime DESC")
    fun getBookmarksByType(bookId: String, type: BookmarkType): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE id = :id")
    suspend fun getBookmarkById(id: String): BookmarkEntity?

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId AND chapterIndex = :chapterIndex")
    suspend fun getBookmarksByChapter(bookId: String, chapterIndex: Int): List<BookmarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Update
    suspend fun updateBookmark(bookmark: BookmarkEntity)

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: String)

    @Query("DELETE FROM bookmarks WHERE bookId = :bookId")
    suspend fun deleteBookmarksByBookId(bookId: String)

    @Query("DELETE FROM bookmarks WHERE bookId = :bookId AND type = :type")
    suspend fun deleteBookmarksByType(bookId: String, type: BookmarkType)

    @Query("SELECT COUNT(*) FROM bookmarks WHERE bookId = :bookId")
    suspend fun getBookmarkCount(bookId: String): Int

    @Query("SELECT COUNT(*) FROM bookmarks")
    suspend fun getTotalBookmarkCount(): Int
}
