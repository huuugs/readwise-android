package com.readwise.engine.pdf

import android.graphics.Bitmap
import android.graphics.RectF
import kotlinx.coroutines.flow.Flow

/**
 * PDF 引擎接口
 */
interface PdfEngine {
    /**
     * 打开 PDF 文档
     */
    suspend fun openDocument(path: String): PdfDocument

    /**
     * 渲染页面
     */
    suspend fun renderPage(
        pageIndex: Int,
        width: Int,
        height: Int
    ): Bitmap?

    /**
     * 提取页面文本
     */
    suspend fun extractText(pageIndex: Int): String

    /**
     * 搜索文本
     */
    suspend fun search(query: String): Flow<PdfSearchResult>

    /**
     * 获取目录
     */
    suspend fun getOutline(): List<PdfOutlineItem>

    /**
     * 获取页面数量
     */
    fun getPageCount(): Int

    /**
     * 获取文档信息
     */
    fun getDocumentInfo(): PdfDocumentInfo?

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
 * PDF 文档信息
 */
data class PdfDocumentInfo(
    val path: String,
    val pageCount: Int,
    val title: String? = null,
    val author: String? = null,
    val subject: String? = null,
    val keywords: String? = null,
    val creator: String? = null,
    val producer: String? = null,
    val creationDate: Long? = null,
    val modificationDate: Long? = null
)

/**
 * PDF 搜索结果
 */
data class PdfSearchResult(
    val pageIndex: Int,
    val startOffset: Int,
    val endOffset: Int,
    val context: String,
    val bounds: RectF? = null
)

/**
 * PDF 目录项
 */
data class PdfOutlineItem(
    val title: String,
    val pageIndex: Int,
    val level: Int = 0,
    val children: List<PdfOutlineItem> = emptyList()
)

/**
 * PDF 页面尺寸
 */
data class PdfPageSize(
    val width: Float,
    val height: Float
) {
    val aspectRatio: Float
        get() = if (height > 0) width / height else 1f
}

/**
 * PDF 渲染配置
 */
data class PdfRenderConfig(
    val width: Int,
    val height: Int,
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotation: Int = 0 // 0, 90, 180, 270
)
