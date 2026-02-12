package com.readwise.engine.epub

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * EPUB 引擎实现
 * 基于 Jsoup 解析 EPUB 内容
 */
@Singleton
class EpubEngineImpl @Inject constructor() : EpubEngine {

    private var document: EpubDocumentImpl? = null
    private var documentInfo: EpubDocumentInfo? = null

    override suspend fun openDocument(path: String): EpubDocument = withContext(Dispatchers.IO) {
        close()

        val file = File(path)
        if (!file.exists()) {
            throw FileNotFoundException("EPUB file not found: $path")
        }

        // 简化实现：假设 EPUB 已解压到目录
        // 实际应该使用 Readium Toolkit
        val epubDir = if (file.isDirectory) {
            file
        } else {
            // TODO: 解压 EPUB 到临时目录
            File(path).parentFile ?: file
        }

        // 查找 OPF 文件
        val opfFile = findOpfFile(epubDir) ?: throw IllegalStateException("OPF file not found")

        // 解析元数据
        val metadata = parseMetadata(opfFile)

        // 解析章节
        val chapters = parseChapters(epubDir, opfFile)

        document = EpubDocumentImpl(
            path = path,
            metadata = metadata,
            chapters = chapters,
            epubDir = epubDir
        )

        documentInfo = metadata
        document as EpubDocumentImpl
    }

    override suspend fun getChapter(chapterIndex: Int): EpubChapter = withContext(Dispatchers.IO) {
        val doc = document ?: throw IllegalStateException("Document not opened")
        if (!doc.isChapterValid(chapterIndex)) {
            throw IllegalArgumentException("Invalid chapter index: $chapterIndex")
        }

        val chapter = doc.getChapters()[chapterIndex]
        val content = loadChapterContent(chapter.href)

        EpubChapter(
            index = chapterIndex,
            title = chapter.title,
            href = chapter.href,
            content = content
        )
    }

    override suspend fun getResource(href: String): ByteArray? = withContext(Dispatchers.IO) {
        val doc = document ?: return@withContext null
        val resourceFile = File(doc.epubDir, href.removePrefix("/"))
        if (resourceFile.exists()) {
            resourceFile.readBytes()
        } else {
            null
        }
    }

    override suspend fun getCover(): Bitmap? = withContext(Dispatchers.IO) {
        val doc = document ?: return@withContext null
        val coverHref = findCoverHref(doc.epubDir)
        if (coverHref != null) {
            val coverData = getResource(coverHref)
            if (coverData != null) {
                BitmapFactory.decodeByteArray(coverData, 0, coverData.size)
            } else null
        } else null
    }

    override suspend fun getTableOfContents(): List<EpubTocItem> = withContext(Dispatchers.IO) {
        val doc = document ?: return@withContext emptyList()
        // TODO: 解析 NCX 文件获取目录
        doc.getChapters().mapIndexed { index, chapter ->
            EpubTocItem(
                title = chapter.title,
                href = chapter.href,
                chapterIndex = index,
                level = 0
            )
        }
    }

    override suspend fun search(query: String): Flow<EpubSearchResult> = flow {
        val doc = document ?: return@flow
        val chapters = doc.getChapters()

        chapters.forEach { chapter ->
            val content = loadChapterContent(chapter.href)
            val plainText = content.replace(Regex("<[^>]+>"), "")

            val index = plainText.indexOf(query, ignoreCase = true)
            if (index != -1) {
                val start = maxOf(0, index - 50)
                val end = minOf(plainText.length, index + query.length + 50)
                val snippet = plainText.substring(start, end)

                emit(EpubSearchResult(
                    chapterIndex = chapter.index,
                    chapterTitle = chapter.title,
                    snippet = "...$snippet...",
                    position = index
                ))
            }
        }
    }

    override fun getChapterCount(): Int {
        return document?.getChapterCount() ?: 0
    }

    override fun getDocumentInfo(): EpubDocumentInfo? = documentInfo

    override fun close() {
        document = null
        documentInfo = null
    }

    override fun isOpened(): Boolean {
        return document != null
    }

    /**
     * 查找 OPF 文件
     */
    private fun findOpfFile(epubDir: File): File? {
        val metaInfDir = File(epubDir, "META-INF")
        val containerFile = File(metaInfDir, "container.xml")
        if (!containerFile.exists()) return null

        val doc = Jsoup.parse(containerFile, "UTF-8")
        val rootFile = doc.selectFirst("rootfile full-path")?.text() ?: return null
        val oebpsDir = File(epubDir, rootFile.removeSuffix(".opf")).parentFile
        return File(oebpsDir ?: epubDir, rootFile)
    }

    /**
     * 解析元数据
     */
    private fun parseMetadata(opfFile: File): EpubDocumentInfo {
        val doc = Jsoup.parse(opfFile, "UTF-8")
        val metadata = doc.selectFirst("metadata") ?: return EpubDocumentInfo(
            path = opfFile.absolutePath,
            title = File(opfFile.parentFile?.parentFile?.parentFile ?: "").name
        )

        val title = metadata.selectFirst("dc|title")?.text()
        val author = metadata.selectFirst("dc|creator")?.text()
        val description = metadata.selectFirst("dc|description")?.text()
        val language = metadata.selectFirst("dc|language")?.text()
        val publisher = metadata.selectFirst("dc|publisher")?.text()
        val identifier = metadata.selectFirst("dc|identifier")?.text()

        return EpubDocumentInfo(
            path = opfFile.absolutePath,
            title = title ?: "Unknown",
            author = author,
            description = description,
            language = language,
            publisher = publisher,
            identifier = identifier
        )
    }

    /**
     * 解析章节列表
     */
    private fun parseChapters(epubDir: File, opfFile: File): List<EpubChapter> {
        val doc = Jsoup.parse(opfFile, "UTF-8")
        val manifest = doc.selectFirst("manifest") ?: return emptyList()
        val items = manifest.select("item")

        return items.mapIndexedNotNull { index, item ->
            val id = item.attr("id")
            val href = item.attr("href")
            val mediaType = item.attr("media-type")

            // 只包含 HTML 章节
            if (mediaType.startsWith("application/xhtml") || mediaType.startsWith("text/html")) {
                EpubChapter(
                    index = index,
                    title = item.selectFirst("dc|title")?.text() ?: "Chapter ${index + 1}",
                    href = href
                )
            } else null
        }
    }

    /**
     * 加载章节内容
     */
    private fun loadChapterContent(href: String): String {
        val doc = document ?: throw IllegalStateException("Document not opened")
        val contentFile = File(doc.epubDir, href.removePrefix("/"))
        if (!contentFile.exists()) return ""

        val content = contentFile.readText()
        // 使用 Jsoup 清理 HTML
        val jsoupDoc = Jsoup.parse(content, "UTF-8")
        return jsoupDoc.html()
    }

    /**
     * 查找封面
     */
    private fun findCoverHref(epubDir: File): String? {
        val opfFile = findOpfFile(epubDir) ?: return null
        val doc = Jsoup.parse(opfFile, "UTF-8")
        val manifest = doc.selectFirst("manifest") ?: return null

        // 查找 cover id
        val metaNode = doc.select("meta[name=cover]").first()
        if (metaNode != null) {
            val coverId = metaNode.attr("content")
            val coverItem = manifest.selectFirst("item[id=$coverId]")
            if (coverItem != null) {
                return coverItem.attr("href")
            }
        }

        return null
    }
}

/**
 * EPUB 文档实现
 */
private data class EpubDocumentImpl(
    private val path: String,
    private val metadata: EpubDocumentInfo,
    private val chapters: List<EpubChapter>,
    val epubDir: File
) : EpubDocument {
    override fun getChapterCount(): Int = chapters.size
    override fun getChapters(): List<EpubChapter> = chapters
    override fun getMetadata(): EpubDocumentInfo = metadata
    override fun isChapterValid(chapterIndex: Int): Boolean {
        return chapterIndex in chapters.indices
    }
}
