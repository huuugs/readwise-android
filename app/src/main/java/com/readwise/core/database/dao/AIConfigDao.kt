package com.readwise.core.database.dao

import androidx.room.*
import com.readwise.core.database.entity.AIConfigEntity
import kotlinx.coroutines.flow.Flow

/**
 * AI配置数据访问对象
 */
@Dao
interface AIConfigDao {
    @Query("SELECT * FROM ai_configs ORDER BY createTime DESC")
    fun getAllConfigs(): Flow<List<AIConfigEntity>>

    @Query("SELECT * FROM ai_configs WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultConfig(): AIConfigEntity?

    @Query("SELECT * FROM ai_configs WHERE id = :id")
    suspend fun getConfigById(id: String): AIConfigEntity?

    @Query("SELECT * FROM ai_configs WHERE service = :service")
    suspend fun getConfigsByService(service: String): List<AIConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: AIConfigEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(configs: List<AIConfigEntity>)

    @Update
    suspend fun update(config: AIConfigEntity)

    @Query("UPDATE ai_configs SET isDefault = 0")
    suspend fun clearAllDefaults()

    @Query("UPDATE ai_configs SET isDefault = :isDefault WHERE id = :id")
    suspend fun setDefault(id: String, isDefault: Boolean)

    @Delete
    suspend fun delete(config: AIConfigEntity)

    @Query("DELETE FROM ai_configs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM ai_configs")
    suspend fun deleteAll()
}
