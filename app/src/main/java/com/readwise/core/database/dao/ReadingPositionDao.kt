package com.readwise.core.database.dao

import androidx.room.*
import com.readwise.core.database.entity.ReadingPositionEntity
import kotlinx.coroutines.flow.Flow

/**
 * 阅读位置数据访问对象
 */
@Dao
interface ReadingPositionDao {
    @Query("SELECT * FROM reading_positions WHERE bookId = :bookId")
    suspend fun getPosition(bookId: String): ReadingPositionEntity?

    @Query("SELECT * FROM reading_positions WHERE bookId = :bookId")
    fun getPositionFlow(bookId: String): Flow<ReadingPositionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosition(position: ReadingPositionEntity)

    @Update
    suspend fun updatePosition(position: ReadingPositionEntity)

    @Query("UPDATE reading_positions SET chapterIndex = :chapterIndex, pageIndex = :pageIndex, paragraphIndex = :paragraphIndex, charOffset = :charOffset, progress = :progress, updateTime = :updateTime WHERE bookId = :bookId")
    suspend fun updatePosition(
        bookId: String,
        chapterIndex: Int,
        pageIndex: Int?,
        paragraphIndex: Int,
        charOffset: Int,
        progress: Float,
        updateTime: Long
    )

    @Query("DELETE FROM reading_positions WHERE bookId = :bookId")
    suspend fun deletePosition(bookId: String)

    @Query("SELECT * FROM reading_positions ORDER BY updateTime DESC LIMIT :limit")
    fun getRecentPositions(limit: Int = 10): Flow<List<ReadingPositionEntity>>
}
