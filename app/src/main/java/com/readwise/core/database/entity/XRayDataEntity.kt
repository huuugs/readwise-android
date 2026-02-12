package com.readwise.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * X-ray 数据实体
 * 存储AI分析的人物、地点、术语等信息
 */
@Entity(tableName = "xray_data")
data class XRayDataEntity(
    @PrimaryKey
    val bookId: String,
    val data: String, // JSON 格式的 X-ray 数据
    val generateTime: Long = System.currentTimeMillis(),
    val isComplete: Boolean = false, // 是否完整分析
    val version: Int = 1 // 数据版本号
)
