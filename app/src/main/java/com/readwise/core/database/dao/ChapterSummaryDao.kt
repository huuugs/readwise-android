package com.readwise.core.database.dao

import androidx.room.*
import com.readwise.core.database.entity.ChapterSummaryEntity
import kotlinx.coroutines.flow.Flow

/**
 * 章节总结数据访问对象
 */
@Dao
interface ChapterSummaryDao {
    @Query("SELECT * FROM chapter_summaries WHERE bookId = :bookId ORDER BY chapterIndex")
    fun getBookSummaries(bookId: String): Flow<List<ChapterSummaryEntity>>

    @Query("SELECT * FROM chapter_summaries WHERE bookId = :bookId AND chapterIndex = :chapterIndex")
    suspend fun getChapterSummary(bookId: String, chapterIndex: Int): ChapterSummaryEntity?

    @Query("SELECT * FROM chapter_summaries WHERE id = :id")
    suspend fun getSummaryById(id: String): ChapterSummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(summary: ChapterSummaryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(summaries: List<ChapterSummaryEntity>)

    @Update
    suspend fun update(summary: ChapterSummaryEntity)

    @Delete
    suspend fun delete(summary: ChapterSummaryEntity)

    @Query("DELETE FROM chapter_summaries WHERE bookId = :bookId")
    suspend fun deleteByBookId(bookId: String)

    @Query("DELETE FROM chapter_summaries WHERE bookId = :bookId AND chapterIndex = :chapterIndex")
    suspend fun deleteChapterSummary(bookId: String, chapterIndex: Int)

    @Query("SELECT COUNT(*) FROM chapter_summaries WHERE bookId = :bookId")
    suspend fun getSummaryCount(bookId: String): Int
}
