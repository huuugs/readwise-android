package com.readwise.core.database.dao

import androidx.room.*
import com.readwise.core.database.entity.DictionaryHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * 词典查询历史数据访问对象
 */
@Dao
interface DictionaryHistoryDao {
    @Query("SELECT * FROM dictionary_history ORDER BY queryTime DESC")
    fun getAllHistory(): Flow<List<DictionaryHistoryEntity>>

    @Query("SELECT * FROM dictionary_history WHERE word = :word ORDER BY queryTime DESC LIMIT 1")
    suspend fun getHistoryByWord(word: String): DictionaryHistoryEntity?

    @Query("SELECT * FROM dictionary_history WHERE isFavorite = 1 ORDER BY queryTime DESC")
    fun getFavorites(): Flow<List<DictionaryHistoryEntity>>

    @Query("SELECT * FROM dictionary_history WHERE word LIKE '%' || :query || '%' ORDER BY queryTime DESC")
    fun searchHistory(query: String): Flow<List<DictionaryHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: DictionaryHistoryEntity)

    @Update
    suspend fun updateHistory(history: DictionaryHistoryEntity)

    @Query("UPDATE dictionary_history SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: String, isFavorite: Boolean)

    @Delete
    suspend fun deleteHistory(history: DictionaryHistoryEntity)

    @Query("DELETE FROM dictionary_history WHERE id = :id")
    suspend fun deleteHistoryById(id: String)

    @Query("DELETE FROM dictionary_history")
    suspend fun deleteAllHistory()
}
