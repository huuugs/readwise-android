package com.readwise.engine.common

import android.graphics.Bitmap
import com.readwise.core.model.BookFormat
import com.readwise.engine.epub.EpubChapter
import com.readwise.engine.epub.EpubEngine
import com.readwise.engine.epub.EpubSearchResult
import com.readwise.engine.epub.EpubTocItem
import com.readwise.engine.pdf.PdfDocument
import com.readwise.engine.pdf.PdfEngine
import com.readwise.engine.pdf.PdfOutlineItem
import com.readwise.engine.pdf.PdfSearchResult
import com.readwise.engine.txt.TxtChapter
import com.readwise.engine.txt.TxtDocument
import com.readwise.engine.txt.TxtEngine
import com.readwise.engine.txt.TxtSearchResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified engine implementation
 * Routes to appropriate format-specific engine based on detected format
 */
@Singleton
class UnifiedEngineImpl @Inject constructor(
    private val pdfEngine: PdfEngine,
    private val epubEngine: EpubEngine,
    private val txtEngine: TxtEngine,
    private val formatDetector: BookFormatDetector
) : UnifiedEngine {

    private var currentFormat: BookFormat = BookFormat.UNKNOWN
    private var currentDocument: Any? = null

    override suspend fun openDocument(
        path: String,
        format: BookFormat?
    ): UnifiedDocument {
        val file = File(path)
        if (!file.exists()) {
            throw IllegalArgumentException("File not found: $path")
        }

        // Detect or use provided format
        val detectedFormat = format ?: formatDetector.detectFormat(path)
        if (detectedFormat == BookFormat.UNKNOWN) {
            throw IllegalArgumentException("Unsupported format: ${file.extension}")
        }

        currentFormat = detectedFormat

        // Open with appropriate engine
        currentDocument = when (detectedFormat) {
            BookFormat.PDF -> pdfEngine.openDocument(path)
            BookFormat.EPUB -> epubEngine.openDocument(path)
            BookFormat.TXT,
            BookFormat.MOBI -> txtEngine.openDocument(path, null)
            else -> throw IllegalArgumentException("Unsupported format: $detectedFormat")
        }

        return this.toUnifiedDocument(currentDocument!!)
    }

    override fun getChapterCount(): Int {
        return when (currentFormat) {
            BookFormat.PDF -> pdfEngine.getPageCount()
            BookFormat.EPUB -> epubEngine.getChapterCount()
            BookFormat.TXT,
            BookFormat.MOBI -> txtEngine.getChapterCount()
            else -> 0
        }
    }

    override suspend fun getChapter(chapterIndex: Int): UnifiedChapter {
        return when (currentFormat) {
            BookFormat.PDF -> {
                val text = pdfEngine.extractText(chapterIndex)
                UnifiedChapter(
                    index = chapterIndex,
                    title = "Page ${chapterIndex + 1}",
                    content = "",
                    plainText = text
                )
            }
            BookFormat.EPUB -> {
                val epubChapter = epubEngine.getChapter(chapterIndex)
                epubChapter.toUnifiedChapter()
            }
            BookFormat.TXT,
            BookFormat.MOBI -> {
                val txtChapter = txtEngine.getChapter(chapterIndex)
                txtChapter.toUnifiedChapter()
            }
            else -> UnifiedChapter(
                index = chapterIndex,
                title = "Unknown",
                content = "",
                plainText = "Format not supported: $currentFormat"
            )
        }
    }

    override suspend fun getOutline(): List<OutlineItem> {
        return when (currentFormat) {
            BookFormat.PDF -> {
                pdfEngine.getOutline().map { it.toUnifiedOutline() }
            }
            BookFormat.EPUB -> {
                epubEngine.getTableOfContents().map { it.toUnifiedOutline() }
            }
            BookFormat.TXT,
            BookFormat.MOBI -> {
                // TXT doesn't have TOC, generate from chapters
                val count = txtEngine.getChapterCount()
                (0 until count).map { index ->
                    OutlineItem(
                        title = "Chapter ${index + 1}",
                        chapterIndex = index
                    )
                }
            }
            else -> emptyList()
        }
    }

    override suspend fun search(query: String): Flow<SearchResult> {
        return when (currentFormat) {
            BookFormat.PDF -> {
                pdfEngine.search(query).map { it.toUnifiedSearchResult() }
            }
            BookFormat.EPUB -> {
                epubEngine.search(query).map { it.toUnifiedSearchResult() }
            }
            BookFormat.TXT,
            BookFormat.MOBI -> {
                txtEngine.search(query).map { it.toUnifiedSearchResult() }
            }
            else -> flowOf()
        }
    }

    override suspend fun getCover(): Bitmap? {
        return when (currentFormat) {
            BookFormat.EPUB -> epubEngine.getCover()
            else -> null
        }
    }

    override fun getDocumentInfo(): DocumentInfo? {
        return when (currentFormat) {
            BookFormat.PDF -> {
                pdfEngine.getDocumentInfo()?.let { info ->
                    DocumentInfo(
                        path = info.path,
                        title = info.title ?: "Unknown",
                        author = info.author,
                        format = BookFormat.PDF,
                        fileSize = File(info.path).length(),
                        chapterCount = info.pageCount
                    )
                }
            }
            BookFormat.EPUB -> {
                epubEngine.getDocumentInfo()?.let { info ->
                    DocumentInfo(
                        path = info.path,
                        title = info.title,
                        author = info.author,
                        format = BookFormat.EPUB,
                        fileSize = File(info.path).length(),
                        chapterCount = info.chapterCount,
                        language = info.language,
                        publisher = info.publisher,
                        identifier = info.identifier
                    )
                }
            }
            BookFormat.TXT,
            BookFormat.MOBI -> {
                txtEngine.getDocumentInfo()?.let { info ->
                    DocumentInfo(
                        path = info.path,
                        title = info.title,
                        format = BookFormat.TXT,
                        fileSize = info.fileSize,
                        chapterCount = info.chapterCount
                    )
                }
            }
            else -> null
        }
    }

    override fun close() {
        when (currentFormat) {
            BookFormat.PDF -> pdfEngine.close()
            BookFormat.EPUB -> epubEngine.close()
            BookFormat.TXT,
            BookFormat.MOBI -> txtEngine.close()
            else -> {}
        }
        currentDocument = null
        currentFormat = BookFormat.UNKNOWN
    }

    override fun isOpened(): Boolean {
        return when (currentFormat) {
            BookFormat.PDF -> pdfEngine.isOpened()
            BookFormat.EPUB -> epubEngine.isOpened()
            BookFormat.TXT,
            BookFormat.MOBI -> txtEngine.isOpened()
            else -> false
        }
    }

    override fun getFormat(): BookFormat = currentFormat

    // Extension functions for converting format-specific models to unified models

    private fun toUnifiedDocument(document: Any): UnifiedDocument {
        return when (document) {
            is PdfDocument -> object : UnifiedDocument {
                override fun getChapterCount() = document.pageCount
                override fun getChapters() = emptyList<com.readwise.engine.common.UnifiedChapter>()
                override fun getMetadata() = null
                override fun isChapterValid(chapterIndex: Int) = chapterIndex in 0 until document.pageCount
            }
            is com.readwise.engine.epub.EpubDocument -> object : UnifiedDocument {
                override fun getChapterCount() = document.getChapterCount()
                override fun getChapters() = document.getChapters().map { it.toUnifiedChapter() }
                override fun getMetadata() = document.getMetadata()?.let {
                    DocumentInfo(
                        path = it.path,
                        title = it.title,
                        author = it.author,
                        format = BookFormat.EPUB,
                        chapterCount = it.chapterCount,
                        language = it.language,
                        publisher = it.publisher,
                        identifier = it.identifier
                    )
                }
                override fun isChapterValid(chapterIndex: Int) = document.isChapterValid(chapterIndex)
            }
            is TxtDocument -> object : UnifiedDocument {
                override fun getChapterCount() = document.getChapterCount()
                override fun getChapters() = document.getChapters().map { it.toUnifiedChapter() }
                override fun getMetadata() = DocumentInfo(
                    path = document.getMetadata().path,
                    title = document.getMetadata().title,
                    format = BookFormat.TXT,
                    fileSize = document.getMetadata().fileSize,
                    chapterCount = document.getMetadata().chapterCount
                )
                override fun isChapterValid(chapterIndex: Int) = document.isChapterValid(chapterIndex)
            }
            else -> throw IllegalArgumentException("Unknown document type: ${document.javaClass}")
        }
    }

    private fun EpubChapter.toUnifiedChapter() = UnifiedChapter(
        index = index,
        title = title,
        content = content,
        plainText = plainText,
        resources = resources.map { ChapterResource(it.href, it.type, it.data) }
    )

    private fun TxtChapter.toUnifiedChapter() = UnifiedChapter(
        index = index,
        title = title,
        content = content,
        plainText = content
    )

    private fun PdfOutlineItem.toUnifiedOutline() = OutlineItem(
        title = title,
        chapterIndex = pageIndex,
        level = level,
        children = children.map { it.toUnifiedOutline() }
    )

    private fun EpubTocItem.toUnifiedOutline() = OutlineItem(
        title = title,
        chapterIndex = chapterIndex,
        level = level,
        children = children.map { it.toUnifiedOutline() }
    )

    private fun PdfSearchResult.toUnifiedSearchResult() = SearchResult(
        chapterIndex = pageIndex,
        chapterTitle = "Page ${pageIndex + 1}",
        snippet = context,
        position = startOffset
    )

    private fun EpubSearchResult.toUnifiedSearchResult() = SearchResult(
        chapterIndex = chapterIndex,
        chapterTitle = chapterTitle,
        snippet = snippet,
        position = position
    )

    private fun TxtSearchResult.toUnifiedSearchResult() = SearchResult(
        chapterIndex = chapterIndex,
        chapterTitle = chapterTitle,
        snippet = snippet,
        position = position
    )
}
