package com.readwise.core.repository

import com.readwise.core.database.dao.BookDao
import com.readwise.core.database.dao.ReadingPositionDao
import com.readwise.core.database.entity.BookEntity
import com.readwise.core.database.entity.ReadingPositionEntity
import com.readwise.core.model.Book
import com.readwise.core.model.ReadPosition
import com.readwise.engine.common.BookFormatDetector
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 书籍仓储
 * 负责书籍数据的管理和业务逻辑
 */
@Singleton
class BookRepository @Inject constructor(
    private val bookDao: BookDao,
    private val readingPositionDao: ReadingPositionDao,
    private val formatDetector: BookFormatDetector
) {
    /**
     * 获取所有书籍
     */
    fun getAllBooks(): Flow<List<Book>> {
        return bookDao.getAllBooks().map { entities ->
            entities.map { it.toBook(formatDetector = formatDetector) }
        }
    }

    /**
     * 获取最近阅读的书籍
     */
    fun getRecentBooks(limit: Int = 20): Flow<List<Book>> {
        return bookDao.getRecentBooks(limit).map { entities ->
            entities.map { it.toBook(formatDetector = formatDetector) }
        }
    }

    /**
     * 根据分类获取书籍
     */
    fun getBooksByCategory(category: String): Flow<List<Book>> {
        return bookDao.getBooksByCategory(category).map { entities ->
            entities.map { it.toBook(formatDetector = formatDetector) }
        }
    }

    /**
     * 根据ID获取书籍
     */
    suspend fun getBookById(bookId: String): Book? {
        val book = bookDao.getBookById(bookId) ?: return null
        val position = readingPositionDao.getPosition(bookId)
        return book.toBook(position?.toReadPosition(), formatDetector)
    }

    /**
     * 根据ID获取书籍流
     */
    fun getBookByIdFlow(bookId: String): Flow<Book?> {
        return bookDao.getBookByIdFlow(bookId).map { entity ->
            entity?.toBook(formatDetector = formatDetector)
        }
    }

    /**
     * 搜索书籍
     */
    suspend fun searchBooks(query: String): List<Book> {
        return bookDao.searchBooks(query).map { it.toBook(formatDetector = formatDetector) }
    }

    /**
     * 导入书籍
     */
    suspend fun importBook(filePath: String): Book {
        val file = File(filePath)
        val extension = file.extension
        val format = formatDetector.fromExtension(extension)

        val bookId = System.currentTimeMillis().toString()

        val entity = BookEntity(
            id = bookId,
            title = file.nameWithoutExtension,
            author = "未知作者",
            coverPath = null,
            filePath = filePath,
            format = format.name,
            fileSize = file.length(),
            addedTime = System.currentTimeMillis()
        )

        bookDao.insertBook(entity)
        return entity.toBook(formatDetector = formatDetector)
    }

    /**
     * 更新阅读进度
     */
    suspend fun updateReadingProgress(
        bookId: String,
        progress: Float,
        position: ReadPosition
    ) {
        bookDao.updateReadingProgress(
            bookId = bookId,
            progress = progress,
            position = position.toJson(),
            time = System.currentTimeMillis()
        )

        readingPositionDao.updatePosition(
            bookId = bookId,
            chapterIndex = position.chapterIndex,
            pageIndex = position.pageIndex,
            paragraphIndex = position.paragraphIndex,
            charOffset = position.charOffset,
            progress = progress,
            updateTime = System.currentTimeMillis()
        )
    }

    /**
     * 删除书籍
     */
    suspend fun deleteBook(bookId: String) {
        bookDao.deleteBookById(bookId)
        readingPositionDao.deletePosition(bookId)
    }

    /**
     * 更新分类
     */
    suspend fun updateCategory(bookId: String, category: String) {
        bookDao.updateCategory(bookId, category)
    }

    /**
     * 更新评分
     */
    suspend fun updateRating(bookId: String, rating: Int) {
        bookDao.updateRating(bookId, rating.coerceIn(0..5))
    }

    /**
     * 更新笔记
     */
    suspend fun updateNotes(bookId: String, notes: String?) {
        bookDao.updateNotes(bookId, notes)
    }

    /**
     * 获取所有分类
     */
    fun getAllCategories(): Flow<List<String>> {
        return bookDao.getAllCategories()
    }

    /**
     * 获取书籍数量
     */
    suspend fun getBookCount(): Int = bookDao.getBookCount()
}

/**
 * 扩展函数：将 Entity 转换为领域模型
 * 需要传入 formatDetector 实例
 */
private fun BookEntity.toBook(
    position: ReadPosition? = null,
    formatDetector: BookFormatDetector
): Book {
    return Book(
        id = id,
        title = title,
        author = author,
        coverPath = coverPath,
        filePath = filePath,
        format = formatDetector.fromFormatName(format),
        fileSize = fileSize,
        addedTime = addedTime,
        lastReadTime = lastReadTime,
        lastReadPosition = position,
        readingProgress = readingProgress,
        category = category,
        tags = tags,
        rating = rating,
        notes = notes,
        wordCount = wordCount,
        chapterCount = chapterCount
    )
}

/**
 * 扩展函数：将 ReadPosition 转换为 JSON 字符串
 */
private fun ReadPosition.toJson(): String {
    return kotlinx.serialization.json.Json.encodeToString(
        kotlinx.serialization.json.Json { encodeDefaults = true },
        this
    )
}

/**
 * 扩展函数：将 ReadingPositionEntity 转换为 ReadPosition
 */
private fun ReadingPositionEntity.toReadPosition(): ReadPosition {
    return ReadPosition(
        chapterIndex = chapterIndex,
        pageIndex = pageIndex,
        paragraphIndex = paragraphIndex,
        charOffset = charOffset,
        progress = progress,
        updateTime = updateTime
    )
}
