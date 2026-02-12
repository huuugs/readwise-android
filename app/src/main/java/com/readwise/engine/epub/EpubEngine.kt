package com.readwise.engine.epub

import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow

/**
 * EPUB 引擎接口
 */
interface EpubEngine {
    /**
     * 打开 EPUB 文档
     */
    suspend fun openDocument(path: String): EpubDocument

    /**
     * 获取章节内容
     */
    suspend fun getChapter(chapterIndex: Int): EpubChapter

    /**
     * 获取资源（图片、字体等）
     */
    suspend fun getResource(href: String): ByteArray?

    /**
     * 获取封面
     */
    suspend fun getCover(): Bitmap?

    /**
     * 获取目录
     */
    suspend fun getTableOfContents(): List<EpubTocItem>

    /**
     * 搜索文本
     */
    suspend fun search(query: String): Flow<EpubSearchResult>

    /**
     * 获取章节数量
     */
    fun getChapterCount(): Int

    /**
     * 获取文档信息
     */
    fun getDocumentInfo(): EpubDocumentInfo?

    /**
     * 关闭文档
     */
    fun close()

    /**
     * 是否已打开
     */
    fun isOpened(): Boolean
}

/**
 * EPUB 文档接口
 */
interface EpubDocument {
    /**
     * 获取章节数量
     */
    fun getChapterCount(): Int

    /**
     * 获取章节列表
     */
    fun getChapters(): List<EpubChapter>

    /**
     * 获取元数据
     */
    fun getMetadata(): EpubDocumentInfo?

    /**
     * 检查章节是否存在
     */
    fun isChapterValid(chapterIndex: Int): Boolean
}

/**
 * EPUB 章节内容
 */
data class EpubChapter(
    val index: Int,
    val title: String,
    val href: String,
    val content: String = "", // HTML 内容
    val resources: List<EpubResource> = emptyList()
) {
    val plainText: String
        get() = content.replace(Regex("<[^>]+>"), "")
}

/**
 * EPUB 资源（图片、样式表等）
 */
data class EpubResource(
    val href: String,
    val type: String, // image/jpeg, text/css, etc.
    val data: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EpubResource
        return href == other.href
    }

    override fun hashCode(): Int {
        return href.hashCode()
    }
}

/**
 * EPUB 目录项
 */
data class EpubTocItem(
    val title: String,
    val href: String,
    val chapterIndex: Int,
    val level: Int = 0,
    val children: List<EpubTocItem> = emptyList()
)

/**
 * EPUB 搜索结果
 */
data class EpubSearchResult(
    val chapterIndex: Int,
    val chapterTitle: String,
    val snippet: String,
    val position: Int
)

/**
 * EPUB 文档信息
 */
data class EpubDocumentInfo(
    val path: String,
    val title: String,
    val author: String? = null,
    val description: String? = null,
    val language: String? = null,
    val publisher: String? = null,
    val identifier: String? = null,
    val chapterCount: Int = 0
)

/**
 * EPUB 排版配置
 */
data class EpubLayoutConfig(
    val fontSize: Int = 18, // sp
    val lineHeight: Float = 1.6f,
    val paragraphSpacing: Int = 16, // dp
    val marginHorizontal: Int = 16, // dp
    val marginVertical: Int = 16, // dp
    val textColor: Int = 0xFF000000.toInt(), // ARGB
    val backgroundColor: Int = 0xFFFFFFFF.toInt(), // ARGB
    val fontFamily: String? = null, // 字体族名称
    val textAlign: EpubTextAlign = EpubTextAlign.JUSTIFY,
    val columnCount: Int = 1, // 单栏或双栏
    val verticalScroll: Boolean = false // 翻页或滚动
)

/**
 * 文本对齐方式
 */
enum class EpubTextAlign {
    LEFT,
    CENTER,
    RIGHT,
    JUSTIFY
}
