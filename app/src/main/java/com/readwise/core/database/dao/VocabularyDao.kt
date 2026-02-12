package com.readwise.core.database.dao

import androidx.room.*
import com.readwise.core.database.entity.VocabularyEntity
import kotlinx.coroutines.flow.Flow

/**
 * 生词本数据访问对象
 */
@Dao
interface VocabularyDao {
    @Query("SELECT * FROM vocabulary ORDER BY addTime DESC")
    fun getAllVocabulary(): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM vocabulary WHERE word = :word LIMIT 1")
    suspend fun getVocabularyByWord(word: String): VocabularyEntity?

    @Query("SELECT * FROM vocabulary WHERE mastered = 0 ORDER BY addTime DESC")
    fun getLearningVocabulary(): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM vocabulary WHERE mastered = 1 ORDER BY addTime DESC")
    fun getMasteredVocabulary(): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM vocabulary WHERE nextReviewTime <= :time ORDER BY nextReviewTime")
    suspend fun getDueVocabulary(time: Long = System.currentTimeMillis()): List<VocabularyEntity>

    @Query("SELECT * FROM vocabulary WHERE bookId = :bookId ORDER BY addTime DESC")
    fun getVocabularyByBookId(bookId: String): Flow<List<VocabularyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVocabulary(vocabulary: VocabularyEntity)

    @Update
    suspend fun updateVocabulary(vocabulary: VocabularyEntity)

    @Query("UPDATE vocabulary SET reviewCount = reviewCount + 1, nextReviewTime = :nextReviewTime WHERE id = :id")
    suspend fun incrementReviewCount(id: String, nextReviewTime: Long?)

    @Query("UPDATE vocabulary SET mastered = :mastered WHERE id = :id")
    suspend fun updateMasteredStatus(id: String, mastered: Boolean)

    @Delete
    suspend fun deleteVocabulary(vocabulary: VocabularyEntity)

    @Query("DELETE FROM vocabulary WHERE id = :id")
    suspend fun deleteVocabularyById(id: String)

    @Query("DELETE FROM vocabulary")
    suspend fun deleteAllVocabulary()

    @Query("SELECT COUNT(*) FROM vocabulary")
    suspend fun getVocabularyCount(): Int

    @Query("SELECT COUNT(*) FROM vocabulary WHERE mastered = 0")
    suspend fun getLearningCount(): Int
}
