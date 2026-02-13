package com.readwise.engine.common

import kotlinx.coroutines.flow.Flow

/**
 * 统一阅读引擎接口
 * 支持多种文档格式的统一访问
 */
interface UnifiedEngine {
    /**
     * 打开文档
     * @param path 文件路径
     * @param format 书籍格式（可选，为null时自动检测）
     * @return 文档接口
     */
    suspend fun openDocument(
        path: String,
        format: BookFormatDetector.Format? = null
    ): UnifiedDocument

    /**
     * 获取章节数量
     */
    fun getChapterCount(): Int

    /**
     * 获取章节内容
     */
    suspend fun getChapter(chapterIndex: Int): UnifiedChapter

    /**
     * 获取目录
     */
    suspend fun getOutline(): List<OutlineItem>

    /**
     * 搜索文本
     */
    suspend fun search(query: String): Flow<SearchResult>

    /**
     * 获取封面
     */
    suspend fun getCover(): android.graphics.Bitmap?

    /**
     * 获取文档信息
     */
    fun getDocumentInfo(): DocumentInfo?

    /**
     * 关闭文档
     */
    fun close()

    /**
     * 是否已打开
     */
    fun isOpened(): Boolean

    /**
     * 获取当前格式
     */
    fun getFormat(): BookFormatDetector.Format
}

/**
 * 统一文档接口
 */
interface UnifiedDocument {
    /**
     * 获取章节数量
     */
    fun getChapterCount(): Int

    /**
     * 获取所有章节
     */
    fun getChapters(): List<UnifiedChapter>

    /**
     * 获取文档元数据
     */
    fun getMetadata(): DocumentInfo?

    /**
     * 检查章节是否有效
     */
    fun isChapterValid(chapterIndex: Int): Boolean
}

/**
 * 统一章节
 */
data class UnifiedChapter(
    val index: Int,
    val title: String,
    val content: String = "",
    val plainText: String = "",
    val resources: List<ChapterResource> = emptyList()
) {
    val displayText: String
        get() = plainText.takeIfNotEmpty() ?: content
}

/**
 * 章节资源（图片、样式等）
 */
data class ChapterResource(
    val href: String,
    val type: String,
    val data: ByteArray? = null
)

/**
 * 目录项
 */
data class OutlineItem(
    val title: String,
    val chapterIndex: Int,
    val level: Int = 0,
    val children: List<OutlineItem> = emptyList()
)

/**
 * 搜索结果
 */
data class SearchResult(
    val chapterIndex: Int,
    val chapterTitle: String,
    val snippet: String,
    val position: Int
)

/**
 * 文档信息
 */
data class DocumentInfo(
    val path: String,
    val title: String,
    val author: String? = null,
    val format: BookFormatDetector.Format,
    val fileSize: Long = 0,
    val chapterCount: Int = 0,
    val language: String? = null,
    val publisher: String? = null,
    val identifier: String? = null
)
