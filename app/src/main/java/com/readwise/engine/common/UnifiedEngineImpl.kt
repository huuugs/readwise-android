package com.readwise.engine.common

import android.graphics.Bitmap
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

    private var currentFormat: BookFormatDetector.Format = BookFormatDetector.Format.UNKNOWN
    private var currentDocument: Any? = null

    override suspend fun openDocument(
        path: String,
        format: BookFormatDetector.Format?
    ): UnifiedDocument {
        val file = File(path)
        if (!file.exists()) {
            throw IllegalArgumentException("File not found: $path")
        }

        // Detect or use provided format
        val detectedFormat = format ?: formatDetector.detectFormat(path)
        if (detectedFormat == BookFormatDetector.Format.UNKNOWN) {
            throw IllegalArgumentException("Unsupported format: ${file.extension}")
        }

        currentFormat = detectedFormat

        // Open with appropriate engine
        currentDocument = when (detectedFormat) {
            BookFormatDetector.Format.PDF -> pdfEngine.openDocument(path)
            BookFormatDetector.Format.EPUB -> epubEngine.openDocument(path)
            BookFormatDetector.Format.TXT,
            BookFormatDetector.Format.MOBI -> txtEngine.openDocument(path, null)
            else -> throw IllegalArgumentException("Unsupported format: $detectedFormat")
        }

        return this.toUnifiedDocument(currentDocument!!)
    }

    override fun getChapterCount(): Int {
        return when (currentFormat) {
            BookFormatDetector.Format.PDF -> pdfEngine.getPageCount()
            BookFormatDetector.Format.EPUB -> epubEngine.getChapterCount()
            BookFormatDetector.Format.TXT,
            BookFormatDetector.Format.MOBI -> txtEngine.getChapterCount()
            else -> 0
        }
    }

    override suspend fun getChapter(chapterIndex: Int): UnifiedChapter {
        return when (currentFormat) {
            BookFormatDetector.Format.PDF -> {
                val text = pdfEngine.extractText(chapterIndex)
                UnifiedChapter(
                    index = chapterIndex,
                    title = "Page ${chapterIndex + 1}",
                    content = "",
                    plainText = text
                )
            }
            BookFormatDetector.Format.EPUB -> {
                val epubChapter = epubEngine.getChapter(chapterIndex)
                epubChapter.toUnifiedChapter()
            }
            BookFormatDetector.Format.TXT,
            BookFormatDetector.Format.MOBI -> {
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
            BookFormatDetector.Format.PDF -> {
                pdfEngine.getOutline().map { it.toUnifiedOutline() }
            }
            BookFormatDetector.Format.EPUB -> {
                epubEngine.getTableOfContents().map { it.toUnifiedOutline() }
            }
            BookFormatDetector.Format.TXT,
            BookFormatDetector.Format.MOBI -> {
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
            BookFormatDetector.Format.PDF -> {
                pdfEngine.search(query).map { it.toUnifiedSearchResult() }
            }
            BookFormatDetector.Format.EPUB -> {
                epubEngine.search(query).map { it.toUnifiedSearchResult() }
            }
            BookFormatDetector.Format.TXT,
            BookFormatDetector.Format.MOBI -> {
                txtEngine.search(query).map { it.toUnifiedSearchResult() }
            }
            else -> flowOf()
        }
    }

    override suspend fun getCover(): Bitmap? {
        return when (currentFormat) {
            BookFormatDetector.Format.EPUB -> epubEngine.getCover()
            else -> null
        }
    }

    override fun getDocumentInfo(): DocumentInfo? {
        return when (currentFormat) {
            BookFormatDetector.Format.PDF -> {
                pdfEngine.getDocumentInfo()?.let { info ->
                    DocumentInfo(
                        path = info.path,
                        title = info.title ?: "Unknown",
                        author = info.author,
                        format = BookFormatDetector.Format.PDF,
                        fileSize = File(info.path).length(),
                        chapterCount = info.pageCount
                    )
                }
            }
            BookFormatDetector.Format.EPUB -> {
                epubEngine.getDocumentInfo()?.let { info ->
                    DocumentInfo(
                        path = info.path,
                        title = info.title,
                        author = info.author,
                        format = BookFormatDetector.Format.EPUB,
                        fileSize = File(info.path).length(),
                        chapterCount = info.chapterCount,
                        language = info.language,
                        publisher = info.publisher,
                        identifier = info.identifier
                    )
                }
            }
            BookFormatDetector.Format.TXT,
            BookFormatDetector.Format.MOBI -> {
                txtEngine.getDocumentInfo()?.let { info ->
                    DocumentInfo(
                        path = info.path,
                        title = info.title,
                        format = BookFormatDetector.Format.TXT,
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
            BookFormatDetector.Format.PDF -> pdfEngine.close()
            BookFormatDetector.Format.EPUB -> epubEngine.close()
            BookFormatDetector.Format.TXT,
            BookFormatDetector.Format.MOBI -> txtEngine.close()
            else -> {}
        }
        currentDocument = null
        currentFormat = BookFormatDetector.Format.UNKNOWN
    }

    override fun isOpened(): Boolean {
        return when (currentFormat) {
            BookFormatDetector.Format.PDF -> pdfEngine.isOpened()
            BookFormatDetector.Format.EPUB -> epubEngine.isOpened()
            BookFormatDetector.Format.TXT,
            BookFormatDetector.Format.MOBI -> txtEngine.isOpened()
            else -> false
        }
    }

    override fun getFormat(): BookFormatDetector.Format = currentFormat

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
                        format = BookFormatDetector.Format.EPUB,
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
                    format = BookFormatDetector.Format.TXT,
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
