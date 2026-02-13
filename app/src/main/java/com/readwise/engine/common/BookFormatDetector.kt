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
     * 支持的格式枚举
     */
    enum class Format {
        PDF,
        EPUB,
        MOBI,
        AZW3,
        TXT,
        DOCX,
        HTML,
        CHM,
        CBZ,
        CBR,
        FB2,
        DJVU,
        RTF,
        UNKNOWN
    }

    /**
     * 支持的 MIME 类型映射
     */
    private val mimeTypeMap = MimeTypeMap()

    /**
     * 从文件路径检测格式
     */
    fun detectFormat(path: String): Format {
        val file = File(path)
        if (!file.exists()) {
            return Format.UNKNOWN
        }

        // 先按扩展名检测
        val extension = file.extension.lowercase()
        val formatByExtension = when (extension) {
            "pdf" -> Format.PDF
            "epub" -> Format.EPUB
            "mobi", "azw", "azw3" -> Format.MOBI
            "txt" -> Format.TXT
            "rtf" -> Format.TXT
            "html", "htm" -> Format.TXT
            "md" -> Format.TXT
            "markdown" -> Format.TXT
            else -> Format.UNKNOWN
        }

        if (formatByExtension != Format.UNKNOWN) {
            return formatByExtension
        }

        // 尝试从 MIME 类型检测（如果文件是通过 ContentResolver 获取的）
        val mimeType = context.contentResolver.getType(Uri.parse(path))
        if (mimeType != null) {
            val formatByMime = when {
                "application/pdf" -> Format.PDF
                "application/x-mobipocket-ebook" -> Format.MOBI
                "application/epub+zip" -> Format.EPUB
                "text/plain" -> Format.TXT
                "text/html" -> Format.TXT
                "application/xhtml+xml" -> Format.TXT
                else -> null
            }
            if (formatByMime != null) {
                return formatByMime
            }
        }
        return Format.UNKNOWN
    }

    /**
     * 从 Uri 检测格式（通过 ContentResolver）
     */
    fun detectFormatFromUri(uri: Uri): Format {
        val mimeType = context.contentResolver.getType(uri)
        val extension = getFileNameExtension(context, uri)

        // 先按 MIME 类型
        mimeType?.let { mime ->
            when {
                "application/pdf" -> return Format.PDF
                "application/x-mobipocket-ebook" -> return Format.MOBI
                "application/epub+zip" -> return Format.EPUB
                "text/plain" -> return Format.TXT
            }
        }

        // 再按扩展名
        return when (extension?.lowercase()) {
            "pdf" -> Format.PDF
            "epub" -> Format.EPUB
            "mobi", "azw", "azw3" -> Format.MOBI
            "txt" -> Format.TXT
            else -> Format.UNKNOWN
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
    fun isSupportedFormat(format: Format): Boolean {
        return format != Format.UNKNOWN
    }

    /**
     * 获取格式显示名称
     */
    fun getFormatName(format: Format): String {
        return when (format) {
            Format.PDF -> "PDF Document"
            Format.EPUB -> "EPUB eBook"
            Format.MOBI -> "MOBI eBook"
            Format.TXT -> "Plain Text"
            Format.UNKNOWN -> "Unknown Format"
            else -> "Other"
        }
    }
}
