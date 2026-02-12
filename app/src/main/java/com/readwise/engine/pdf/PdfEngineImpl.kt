package com.readwise.engine.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PDF 引擎实现 - 基于 Android PdfRenderer
 */
@Singleton
class PdfEngineImpl @Inject constructor() : PdfEngine {

    private var pdfRenderer: PdfRenderer? = null
    private var documentInfo: PdfDocumentInfo? = null
    private val pageCache = mutableMapOf<Int, Bitmap>()

    override suspend fun openDocument(path: String): PdfDocument = withContext(Dispatchers.IO) {
        // 关闭之前的文档
        close()

        val file = File(path)
        if (!file.exists()) {
            throw FileNotFoundException("PDF file not found: $path")
        }

        val input = file.inputStream()
        val renderer = PdfRenderer(input)
        pdfRenderer = renderer

        val pageCount = renderer.pageCount
        documentInfo = PdfDocumentInfo(
            path = path,
            pageCount = pageCount,
            title = file.nameWithoutExtension
        )

        object : PdfDocument {
            override fun getPageCount(): Int = pageCount
            override fun getPageSize(pageIndex: Int): PdfPageSize? {
                if (!isPageValid(pageIndex)) return null
                return pdfRenderer?.openPage(pageIndex)?.use { page ->
                    PdfPageSize(
                        width = page.width.toFloat(),
                        height = page.height.toFloat()
                    )
                }
            }
            override fun getMetadata(): PdfDocumentInfo? = documentInfo
            override fun isPageValid(pageIndex: Int): Boolean =
                pageIndex in 0 until pageCount
        }
    }

    override suspend fun renderPage(
        pageIndex: Int,
        width: Int,
        height: Int
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (!isPageValid(pageIndex)) return@withContext null

        // 检查缓存
        val cacheKey = "${pageIndex}_${width}_${height}"
        pageCache[cacheKey]?.let { return@withContext it }

        val renderer = pdfRenderer ?: return@withContext null

        try {
            renderer.openPage(pageIndex).use { page ->
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)

                // 计算缩放
                val pageWidth = page.width
                val pageHeight = page.height
                val scale = minOf(
                    width.toFloat() / pageWidth,
                    height.toFloat() / pageHeight
                )

                val scaledWidth = (pageWidth * scale).toInt()
                val scaledHeight = (pageHeight * scale).toInt()

                // 居中渲染
                val left = ((width - scaledWidth) / 2f).toInt()
                val top = ((height - scaledHeight) / 2f).toInt()

                val destRect = Rect(
                    left,
                    top,
                    left + scaledWidth,
                    top + scaledHeight
                )

                page.render(bitmap, destRect, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                // 缓存位图（限制缓存大小）
                if (pageCache.size > 10) {
                    pageCache.clear()
                }
                pageCache[cacheKey] = bitmap

                bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun extractText(pageIndex: Int): String = withContext(Dispatchers.IO) {
        // PdfRenderer 不支持文本提取，需要使用其他库
        // TODO: 集成 PdfiumAndroid 实现文本提取
        ""
    }

    override suspend fun search(query: String): Flow<PdfSearchResult> = flow {
        // TODO: 实现全文搜索
        // 需要先提取所有页面的文本，然后进行搜索
    }

    override suspend fun getOutline(): List<PdfOutlineItem> = withContext(Dispatchers.IO) {
        // PdfRenderer 不支持目录提取
        // TODO: 集成 PdfiumAndroid 实现目录提取
        emptyList()
    }

    override fun getPageCount(): Int {
        return pdfRenderer?.pageCount ?: 0
    }

    override fun getDocumentInfo(): PdfDocumentInfo? = documentInfo

    override fun close() {
        pdfRenderer?.close()
        pdfRenderer = null
        documentInfo = null

        // 清理缓存
        pageCache.values.forEach { it.recycle() }
        pageCache.clear()
    }

    override fun isOpened(): Boolean {
        return pdfRenderer != null
    }

    private fun isPageValid(pageIndex: Int): Boolean {
        val renderer = pdfRenderer ?: return false
        return pageIndex in 0 until renderer.pageCount
    }
}
