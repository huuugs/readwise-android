package com.readwise.core.model

/**
 * 书籍格式
 */
class BookFormat private constructor(val fileExtension: String) {
    val displayName: String
        get() = when (fileExtension) {
            "pdf" -> "Portable Document Format"
            "epub" -> "EPUB"
            "mobi" -> "MOBI"
            "azw3" -> "AZW3"
            "txt" -> "TXT"
            "docx" -> "DOCX"
            "html" -> "HTML"
            "chm" -> "CHM"
            "cbz" -> "CBZ"
            "cbr" -> "CBR"
            "fb2" -> "FB2"
            "djvu" -> "DJVU"
            "rtf" -> "RTF"
            else -> "未知"
        }

    companion object {
        val PDF = BookFormat("pdf")
        val EPUB = BookFormat("epub")
        val MOBI = BookFormat("mobi")
        val AZW3 = BookFormat("azw3")
        val TXT = BookFormat("txt")
        val DOCX = BookFormat("docx")
        val HTML = BookFormat("html")
        val CHM = BookFormat("chm")
        val CBZ = BookFormat("cbz")
        val CBR = BookFormat("cbr")
        val FB2 = BookFormat("fb2")
        val DJVU = BookFormat("djvu")
        val RTF = BookFormat("rtf")
        val UNKNOWN = BookFormat("")

        fun fromExtension(extension: String): BookFormat {
            return values.find { it.fileExtension.equals(extension, ignoreCase = true) } ?: UNKNOWN
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
