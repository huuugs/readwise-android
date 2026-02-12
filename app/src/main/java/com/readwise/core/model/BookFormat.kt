package com.readwise.core.model

/**
 * 书籍格式枚举
 */
sealed class BookFormat(val displayName: String, val fileExtension: String) {
    object PDF : BookFormat("Portable Document Format", "pdf")
    object EPUB : BookFormat("EPUB", "epub")
    object MOBI : BookFormat("MOBI", "mobi")
    object AZW3 : BookFormat("AZW3", "azw3")
    object TXT : BookFormat("TXT", "txt")
    object DOCX : BookFormat("DOCX", "docx")
    object HTML : BookFormat("HTML", "html")
    object CHM : BookFormat("CHM", "chm")
    object CBZ : BookFormat("CBZ", "cbz")
    object CBR : BookFormat("CBR", "cbr")
    object FB2 : BookFormat("FB2", "fb2")
    object DJVU : BookFormat("DJVU", "djvu")
    object RTF : BookFormat("RTF", "rtf")
    object UNKNOWN : BookFormat("未知", "")

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
