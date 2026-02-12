package com.readwise.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.readwise.core.database.converter.ListConverter
import com.readwise.core.database.dao.*
import com.readwise.core.database.entity.*

/**
 * 应用数据库
 */
@Database(
    entities = [
        BookEntity::class,
        BookmarkEntity::class,
        ReadingPositionEntity::class,
        DictionaryHistoryEntity::class,
        VocabularyEntity::class,
        XRayDataEntity::class,
        AIConfigEntity::class,
        ChapterSummaryEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(ListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun readingPositionDao(): ReadingPositionDao
    abstract fun dictionaryHistoryDao(): DictionaryHistoryDao
    abstract fun vocabularyDao(): VocabularyDao
    abstract fun xRayDataDao(): XRayDataDao
    abstract fun aiConfigDao(): AIConfigDao
    abstract fun chapterSummaryDao(): ChapterSummaryDao

    companion object {
        const val DATABASE_NAME = "readwise_database"
    }
}
