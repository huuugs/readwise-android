package com.readwise.core.model

/**
 * 书籍格式枚举
 */
enum class BookFormat(val displayName: String, @JvmName("extension") val fileExtension: String) {
    PDF("Portable Document Format", "pdf"),
    EPUB("EPUB", "epub"),
    MOBI("MOBI", "mobi"),
    AZW3("AZW3", "azw3"),
    TXT("TXT", "txt"),
    DOCX("DOCX", "docx"),
    HTML("HTML", "html"),
    CHM("CHM", "chm"),
    CBZ("CBZ", "cbz"),
    CBR("CBR", "cbr"),
    FB2("FB2", "fb2"),
    DJVU("DJVU", "djvu"),
    RTF("RTF", "rtf"),
    UNKNOWN("未知", "");

    companion object {
        fun fromExtension(extension: String): BookFormat {
            return values().find { it.fileExtension.equals(extension, ignoreCase = true) } ?: UNKNOWN
        }

        fun fromMimeType(mimeType: String): BookFormat {
            return when (mimeType.lowercase()) {
                "application/pdf" -> PDF
                "application/epub+zip" -> EPUB
                "application/x-mobipocket-ebook" -> MOBI
                "text/plain" -> TXT
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> DOCX
                "text/html" -> HTML
                "application/x-chm" -> CHM
                "application/vnd.comicbook+zip" -> CBZ
                "application/x-cbr" -> CBR
                "application/x-fictionbook" -> FB2
                "image/vnd.djvu" -> DJVU
                "application/rtf" -> RTF
                else -> UNKNOWN
            }
        }
    }
}
