package com.readwise.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * AI配置实体
 */
@Entity(tableName = "ai_configs")
data class AIConfigEntity(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val service: String, // openai, claude, gemini, deepseek, custom
    val apiKey: String, // 加密存储
    val baseUrl: String? = null, // 自定义 API 地址
    val model: String,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2000,
    val isDefault: Boolean = false,
    val createTime: Long = System.currentTimeMillis()
)
