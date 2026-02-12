package com.readwise.core.database.dao

import androidx.room.*
import com.readwise.core.database.entity.XRayDataEntity
import kotlinx.coroutines.flow.Flow

/**
 * X-ray 数据访问对象
 */
@Dao
interface XRayDataDao {
    @Query("SELECT * FROM xray_data WHERE bookId = :bookId")
    suspend fun getXRayData(bookId: String): XRayDataEntity?

    @Query("SELECT * FROM xray_data WHERE bookId = :bookId")
    fun getXRayDataFlow(bookId: String): Flow<XRayDataEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(data: XRayDataEntity)

    @Update
    suspend fun update(data: XRayDataEntity)

    @Query("UPDATE xray_data SET data = :data, isComplete = :isComplete, generateTime = :generateTime WHERE bookId = :bookId")
    suspend fun updateXRayData(bookId: String, data: String, isComplete: Boolean, generateTime: Long)

    @Delete
    suspend fun delete(data: XRayDataEntity)

    @Query("DELETE FROM xray_data WHERE bookId = :bookId")
    suspend fun deleteByBookId(bookId: String)

    @Query("SELECT * FROM xray_data WHERE isComplete = 1")
    suspend fun getCompletedXRayData(): List<XRayDataEntity>
}
