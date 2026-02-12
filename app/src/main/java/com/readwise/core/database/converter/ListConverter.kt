package com.readwise.core.database.converter

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 列表类型转换器
 * 用于将 List<String> 存储为 JSON 字符串
 */
class ListConverter {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return try {
            Json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
