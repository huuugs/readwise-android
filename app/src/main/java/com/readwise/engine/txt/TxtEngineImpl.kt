package com.readwise.engine.txt

import android.content.Context
import androidx.compose.ui.text.intl.Locale
import com.google.mediainstructiondetector.TextEncodingDetector
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.Charset
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TXT 引擎实现
 */
@Singleton
class TxtEngineImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : TxtEngine {

    private var document: TxtDocumentImpl? = null
    private var documentInfo: TxtDocumentInfo? = null

    override suspend fun openDocument(
        path: String,
        charset: Charset?
    ): TxtDocument = withContext(Dispatchers.IO) {
        close()

        val file = File(path)
        if (!file.exists()) {
            throw FileNotFoundException("TXT file not found: $path")
        }

        // 检测编码
        val detectedCharset = charset ?: detectEncoding(file)

        // 读取文件内容
        val content = file.readBytes().toString(
            Charset.forName(detectedCharset.charsetName)
        )

        // 识别章节
        val chapters = detectChapters(content)

        // 统计信息
        val lineCount = content.lines().size
        val wordCount = content.split(Regex("\\s+")).size

        document = TxtDocumentImpl(
            path = path,
            content = content,
            chapters = chapters,
            charset = detectedCharset
        )

        documentInfo = TxtDocumentInfo(
            path = path,
            title = file.nameWithoutExtension,
            fileSize = file.length(),
            charset = detectedCharset.displayName,
            lineCount = lineCount,
            wordCount = wordCount,
            chapterCount = chapters.size
        )

        document as TxtDocumentImpl
    }

    override suspend fun getChapter(chapterIndex: Int): TxtChapter = withContext(Dispatchers.IO) {
        val doc = document ?: throw IllegalStateException("Document not opened")
        if (!doc.isChapterValid(chapterIndex)) {
            throw IllegalArgumentException("Invalid chapter index: $chapterIndex")
        }

        val chapter = doc.getChapters()[chapterIndex]
        val content = loadChapterContent(chapter)

        TxtChapter(
            index = chapterIndex,
            title = chapter.title,
            startOffset = chapter.startOffset,
            endOffset = chapter.endOffset,
            content = content
        )
    }

    override suspend fun getChapters(): List<TxtChapter> = withContext(Dispatchers.IO) {
        document?.getChapters() ?: emptyList()
    }

    override suspend fun search(query: String): Flow<TxtSearchResult> = flow {
        val doc = document ?: return@flow
        val fullText = doc.getFullText()

        val lines = fullText.lines()
        lines.forEachIndexed { index, line ->
            if (line.contains(query, ignoreCase = true)) {
                val start = maxOf(0, index - 2)
                val end = minOf(lines.size, index + 3)
                val snippet = lines.subList(start, end).joinToString("\n")

                emit(TxtSearchResult(
                    chapterIndex = 0,
                    chapterTitle = "Full Text",
                    snippet = snippet,
                    position = index
                ))
            }
        }
    }

    override fun getChapterCount(): Int {
        return document?.getChapterCount() ?: 0
    }

    override fun getDocumentInfo(): TxtDocumentInfo? = documentInfo

    override fun close() {
        document = null
        documentInfo = null
    }

    override fun isOpened(): Boolean {
        return document != null
    }

    /**
     * 检测文件编码
     */
    private fun detectEncoding(file: File): TxtCharset {
        return try {
            val bytes = file.readBytes()
            val detector = TextEncodingDetector()
            val detected = detector.detect(bytes, false) ?: "UTF-8"

            when (detected.uppercase()) {
                "GB18030", "GBK" -> TxtCharset.GBK
                "BIG5" -> TxtCharset.BIG5
                "UTF-16", "UTF-16LE", "UTF-16BE" -> TxtCharset.UTF_16
                else -> TxtCharset.fromCharsetName(detected) ?: TxtCharset.UTF_8
            }
        } catch (e: Exception) {
            TxtCharset.UTF_8
        }
    }

    /**
     * 识别章节
     */
    private fun detectChapters(content: String): List<TxtChapter> {
        val lines = content.lines()
        val chapters = mutableListOf<TxtChapter>()
        var currentOffset = 0

        for ((index, line) in lines.withIndex()) {
            // 检测章节标题
            val chapterInfo = detectChapterLine(line)

            if (chapterInfo != null && chapters.isNotEmpty()) {
                // 上一章结束
                chapters[chapters.size - 1] = chapters.last().copy(
                    endOffset = currentOffset
                )
            }

            if (chapterInfo != null) {
                // 新章节开始
                chapters.add(TxtChapter(
                    index = chapters.size,
                    title = chapterInfo,
                    startOffset = currentOffset
                ))
            }

            // 更新偏移（包括换行符）
            currentOffset += line.toByteArray().size + 1
        }

        // 如果没有章节，将全文作为一章
        if (chapters.isEmpty()) {
            chapters.add(TxtChapter(
                index = 0,
                title = "Full Text",
                startOffset = 0,
                endOffset = currentOffset
            ))
        }

        return chapters
    }

    /**
     * 检测章节标题
     */
    private fun detectChapterLine(line: String): String? {
        val trimmed = line.trim()

        // 中文数字章节：第一章、第一回等
        val chinesePattern = Regex("^第[一二三四五六七八九十百千零零两]+[章节回卷集部篇]")
        if (chinesePattern.find(trimmed) != null) {
            return trimmed
        }

        // 阿拉伯数字章节：Chapter 1
        val chapterPattern = Regex("^(Chapter|Chapter|CHAPTER|CHAPTER)\\s+\\d+", RegexOption.IGNORE_CASE)
        if (chapterPattern.find(trimmed) != null) {
            return trimmed
        }

        // 纯数字章节：1. 2. 3.
        val numericPattern = Regex("^\\d+\\.")
        if (numericPattern.find(trimmed) != null) {
            return trimmed
        }

        // 罗马数字章节：I. II. III.
        val romanPattern = Regex("^[IVXLCDM]+\\.")
        if (romanPattern.find(trimmed) != null) {
            return trimmed
        }

        return null
    }

    /**
     * 加载章节内容
     */
    private fun loadChapterContent(chapter: TxtChapter): String {
        val doc = document ?: throw IllegalStateException("Document not opened")
        val bytes = doc.getRawBytes()

        val start = chapter.startOffset
        val end = chapter.endOffset

        return if (start >= 0 && end <= bytes.size) {
            String(bytes, start, end)
        } else {
            ""
        }
    }
}

/**
 * TXT 文档实现
 */
private data class TxtDocumentImpl(
    private val path: String,
    private val content: String,
    private val chapters: List<TxtChapter>,
    private val charset: TxtCharset
) : TxtDocument {

    override fun getChapterCount(): Int = chapters.size

    override fun getChapters(): List<TxtChapter> = chapters

    override fun getMetadata(): TxtDocumentInfo {
        val file = File(path)
        val lineCount = content.lines().size
        val wordCount = content.split(Regex("\\s+")).size

        return TxtDocumentInfo(
            path = path,
            title = file.nameWithoutExtension,
            fileSize = file.length(),
            charset = charset.displayName,
            lineCount = lineCount,
            wordCount = wordCount,
            chapterCount = chapters.size
        )
    }

    override fun isChapterValid(chapterIndex: Int): Boolean {
        return chapterIndex in chapters.indices
    }

    override fun getFullText(): String = content

    override fun getRawBytes(): ByteArray {
        return File(path).readBytes()
    }
}
