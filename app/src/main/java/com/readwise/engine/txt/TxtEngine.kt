package com.readwise.engine.txt

import kotlinx.coroutines.flow.Flow
import java.nio.charset.Charset

/**
 * TXT 引擎接口
 */
interface TxtEngine {
    /**
     * 打开 TXT 文档
     */
    suspend fun openDocument(path: String, charset: Charset? = null): TxtDocument

    /**
     * 获取章节内容
     */
    suspend fun getChapter(chapterIndex: Int): TxtChapter

    /**
     * 获取所有章节
     */
    suspend fun getChapters(): List<TxtChapter>

    /**
     * 搜索文本
     */
    suspend fun search(query: String): Flow<TxtSearchResult>

    /**
     * 获取章节数量
     */
    fun getChapterCount(): Int

    /**
     * 获取文档信息
     */
    fun getDocumentInfo(): TxtDocumentInfo?

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
 * TXT 文档接口
 */
interface TxtDocument {
    /**
     * 获取章节数量
     */
    fun getChapterCount(): Int

    /**
     * 获取章节列表
     */
    fun getChapters(): List<TxtChapter>

    /**
     * 获取元数据
     */
    fun getMetadata(): TxtDocumentInfo

    /**
     * 检查章节是否存在
     */
    fun isChapterValid(chapterIndex: Int): Boolean

    /**
     * 获取完整文本内容
     */
    fun getFullText(): String

    /**
     * 获取原始字节数组
     */
    fun getRawBytes(): ByteArray
}

/**
 * TXT 章节
 */
data class TxtChapter(
    val index: Int,
    val title: String,
    val startOffset: Int,     // 在原文件中的字节偏移
    val endOffset: Int,       // 在原文件中的字节偏移
    val content: String = ""
) {
    val plainText: String
        get() = content
}

/**
 * TXT 搜索结果
 */
data class TxtSearchResult(
    val chapterIndex: Int,
    val chapterTitle: String,
    val snippet: String,
    val position: Int
)

/**
 * TXT 文档信息
 */
data class TxtDocumentInfo(
    val path: String,
    val title: String,
    val fileSize: Long,
    val charset: String,
    val lineCount: Int,
    val wordCount: Int,
    val chapterCount: Int
)

/**
 * TXT 编码
 */
enum class TxtCharset(val displayName: String, val charsetName: String) {
    UTF_8("UTF-8", "UTF-8"),
    GBK("GBK", "GBK"),
    GB18030("GB18030", "GB18030"),
    BIG5("Big5", "Big5"),
    UTF_16("UTF-16", "UTF-16"),
    UTF_16BE("UTF-16BE", "UTF-16BE"),
    UNICODE("Unicode", "Unicode");

    companion object {
        fun fromCharsetName(name: String): TxtCharset? {
            return values().find { it.charsetName == name }
        }
    }
}

/**
 * TXT 排版配置
 */
data class TxtLayoutConfig(
    val fontSize: Int = 18,           // sp
    val lineHeight: Float = 1.6f,     // 行高倍数
    val paragraphSpacing: Int = 16,    // dp
    val marginHorizontal: Int = 16,    // dp
    val marginVertical: Int = 16,      // dp
    val textColor: Int = 0xFF000000.toInt(),
    val backgroundColor: Int = 0xFFFFFFFF.toInt(),
    val fontFamily: String? = null,
    val textAlign: TxtTextAlign = TxtTextAlign.JUSTIFY,
    val isVertical: Boolean = false,    // 竖排模式
    val removeEmptyLines: Boolean = true, // 移除空行
    val indentFirstLine: Boolean = true, // 首行缩进
    val trimWhitespace: Boolean = true  // 裁剪空白
)

/**
 * 文本对齐方式
 */
enum class TxtTextAlign {
    LEFT,
    CENTER,
    RIGHT,
    JUSTIFY
}

/**
 * 章节识别规则
 */
data class ChapterDetectionRule(
    val enabled: Boolean = true,
    val pattern: ChapterPattern = ChapterPattern.NUMERIC,
    val customPattern: String? = null
)

/**
 * 章节模式
 */
enum class ChapterPattern {
    NUMERIC,        // 第1章、Chapter 1
    CHINESE,        // 第一章、第一回
    ROMAN,          // Chapter I、Chapter II
    EMPTY_LINE,     // 空行分隔
    NONE            // 不分章节
}
