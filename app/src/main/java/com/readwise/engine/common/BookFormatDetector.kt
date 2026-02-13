package com.readwise.engine.common

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import javax.inject.Inject

/**
 * 书籍格式检测器
 * 根据文件扩展名或 MIME 类型检测书籍格式
 */
@Singleton
class BookFormatDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * 支持的 MIME 类型映射
     */
    private val mimeTypeMap = MimeTypeMap()

    /**
     * 从文件路径检测格式
     */
    fun detectFormat(path: String): BookFormat {
        val file = File(path)
        if (!file.exists()) {
            return BookFormat.UNKNOWN
        }

        // 先按扩展名检测
        val extension = file.extension.lowercase()
        val formatByExtension = when (extension) {
            "pdf" -> BookFormat.PDF
            "epub" -> BookFormat.EPUB
            "mobi", "azw", "azw3" -> BookFormat.MOBI
            "txt" -> BookFormat.TXT
            "rtf" -> BookFormat.TXT
            "html", "htm" -> BookFormat.TXT
            "md" -> BookFormat.TXT
            "markdown" -> BookFormat.TXT
            else -> BookFormat.UNKNOWN
        }

        if (formatByExtension != BookFormat.UNKNOWN) {
            return formatByExtension
        }

        // 尝试从 MIME 类型检测（如果文件是通过 ContentResolver 获取的）
        val mimeType = context.contentResolver.getType(Uri.parse(path))
        if (mimeType != null) {
            val formatByMime = when {
                "application/pdf" -> BookFormat.PDF
                "application/x-mobipocket-ebook" -> BookFormat.MOBI
                "application/epub+zip" -> BookFormat.EPUB
                "text/plain" -> BookFormat.TXT
                "text/html" -> BookFormat.TXT
                "application/xhtml+xml" -> BookFormat.TXT
                else -> null
            }
            if (formatByMime != null) {
                return formatByMime
            }
        }
        return BookFormat.UNKNOWN
    }

    /**
     * 从 Uri 检测格式（通过 ContentResolver）
     */
    fun detectFormatFromUri(uri: Uri): BookFormat {
        val mimeType = context.contentResolver.getType(uri)
        val extension = getFileNameExtension(context, uri)

        // 先按 MIME 类型
        mimeType?.let { mime ->
            when {
                "application/pdf" -> return BookFormat.PDF
                "application/x-mobipocket-ebook" -> return BookFormat.MOBI
                "application/epub+zip" -> return BookFormat.EPUB
                "text/plain" -> return BookFormat.TXT
            }
        }

        // 再按扩展名
        return when (extension?.lowercase()) {
            "pdf" -> BookFormat.PDF
            "epub" -> BookFormat.EPUB
            "mobi", "azw", "azw3" -> BookFormat.MOBI
            "txt" -> BookFormat.TXT
            else -> BookFormat.UNKNOWN
        }
    }

    /**
     * 获取文件扩展名
     */
    private fun getFileNameExtension(context: Context, uri: Uri): String? {
        val fileName = context.contentResolver.query(uri, null, null)
        val nameIndex = fileName?.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (nameIndex != null && nameIndex >= 0) {
            val name = fileName.getString(nameIndex)
            val dotIndex = name.lastIndexOf('.')
            return if (dotIndex != -1) name.substring(dotIndex + 1) else null
        }
        return null
    }

    /**
     * 检查是否为支持的格式
     */
    fun isSupportedFormat(format: BookFormat): Boolean {
        return format != BookFormat.UNKNOWN
    }

    /**
     * 获取格式显示名称
     */
    fun getFormatName(format: BookFormat): String {
        return when (format) {
            BookFormat.PDF -> "PDF Document"
            BookFormat.EPUB -> "EPUB eBook"
            BookFormat.MOBI -> "MOBI eBook"
            BookFormat.TXT -> "Plain Text"
            BookFormat.UNKNOWN -> "Unknown Format"
            else -> "Other"
        }
    }

    /**
     * 获取文件扩展名
     */
    fun fromExtension(extension: String): BookFormat {
        return when (extension.lowercase()) {
            "pdf" -> BookFormat.PDF
            "epub" -> BookFormat.EPUB
            "mobi" -> BookFormat.MOBI
            "azw3" -> BookFormat.AZW3
            "txt" -> BookFormat.TXT
            "docx" -> BookFormat.DOCX
            "html" -> BookFormat.HTML
            "chm" -> BookFormat.CHM
            "cbz" -> BookFormat.CBZ
            "cbr" -> BookFormat.CBR
            "fb2" -> BookFormat.FB2
            "djvu" -> BookFormat.DJVU
            "rtf" -> BookFormat.RTF
            else -> BookFormat.UNKNOWN
        }
    }
}
