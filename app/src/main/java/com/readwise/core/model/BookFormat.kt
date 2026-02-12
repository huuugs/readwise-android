package com.readwise.core.model

/**
 * 书籍格式
 */
sealed class BookFormat(val fileExtension: String) {
    object PDF : BookFormat("pdf")
    object EPUB : BookFormat("epub")
    object MOBI : BookFormat("mobi")
    object AZW3 : BookFormat("azw3")
    object TXT : BookFormat("txt")
    object DOCX : BookFormat("docx")
    object HTML : BookFormat("html")
    object CHM : BookFormat("chm")
    object CBZ : BookFormat("cbz")
    object CBR : BookFormat("cbr")
    object FB2 : BookFormat("fb2")
    object DJVU : BookFormat("djvu")
    object RTF : BookFormat("rtf")
    object UNKNOWN : BookFormat("")

    val displayName: String
        get() = when (this) {
            PDF -> "Portable Document Format"
            EPUB -> "EPUB"
            MOBI -> "MOBI"
            AZW3 -> "AZW3"
            TXT -> "TXT"
            DOCX -> "DOCX"
            HTML -> "HTML"
            CHM -> "CHM"
            CBZ -> "CBZ"
            CBR -> "CBR"
            FB2 -> "FB2"
            DJVU -> "DJVU"
            RTF -> "RTF"
            UNKNOWN -> "未知"
        }

    companion object {
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
