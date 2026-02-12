package com.readwise.core.database.dao

import androidx.room.*
import com.readwise.core.database.entity.BookEntity
import kotlinx.coroutines.flow.Flow

/**
 * 书籍数据访问对象
 */
@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY lastReadTime DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books ORDER BY lastReadTime DESC LIMIT :limit")
    fun getRecentBooks(limit: Int = 20): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE category = :category ORDER BY lastReadTime DESC")
    fun getBooksByCategory(category: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun getBookById(bookId: String): BookEntity?

    @Query("SELECT * FROM books WHERE id = :bookId")
    fun getBookByIdFlow(bookId: String): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%'")
    suspend fun searchBooks(query: String): List<BookEntity>

    @Query("SELECT * FROM books WHERE category = :category AND (title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%')")
    fun searchBooksInCategory(category: String, query: String): Flow<List<BookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Update
    suspend fun updateBook(book: BookEntity)

    @Delete
    suspend fun deleteBook(book: BookEntity)

    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun deleteBookById(bookId: String)

    @Query("DELETE FROM books")
    suspend fun deleteAllBooks()

    @Query("UPDATE books SET readingProgress = :progress, lastReadPosition = :position, lastReadTime = :time WHERE id = :bookId")
    suspend fun updateReadingProgress(bookId: String, progress: Float, position: String?, time: Long)

    @Query("UPDATE books SET category = :category WHERE id = :bookId")
    suspend fun updateCategory(bookId: String, category: String)

    @Query("UPDATE books SET rating = :rating WHERE id = :bookId")
    suspend fun updateRating(bookId: String, rating: Int)

    @Query("UPDATE books SET notes = :notes WHERE id = :bookId")
    suspend fun updateNotes(bookId: String, notes: String?)

    @Query("SELECT DISTINCT category FROM books ORDER BY category")
    fun getAllCategories(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM books")
    suspend fun getBookCount(): Int

    @Query("SELECT SUM(fileSize) FROM books")
    suspend fun getTotalSize(): Long?
}
