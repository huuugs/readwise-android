package com.readwise.core.model;

/**
 * 书籍格式
 */
public enum BookFormat {
    PDF("pdf", "Portable Document Format"),
    EPUB("epub", "EPUB"),
    MOBI("mobi", "MOBI"),
    AZW3("azw3", "AZW3"),
    TXT("txt", "TXT"),
    DOCX("docx", "DOCX"),
    HTML("html", "HTML"),
    CHM("chm", "CHM"),
    CBZ("cbz", "CBZ"),
    CBR("cbr", "CBR"),
    FB2("fb2", "FB2"),
    DJVU("djvu", "DJVU"),
    RTF("rtf", "RTF"),
    UNKNOWN("", "未知");

    private final String fileExtension;
    private final String displayName;

    BookFormat(String fileExtension, String displayName) {
        this.fileExtension = fileExtension;
        this.displayName = displayName;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static BookFormat fromExtension(String extension) {
        for (BookFormat format : values()) {
            if (format.fileExtension.equalsIgnoreCase(extension)) {
                return format;
            }
        }
        return UNKNOWN;
    }

    public static BookFormat fromMimeType(String mimeType) {
        switch (mimeType.toLowerCase()) {
            case "application/pdf": return PDF;
            case "application/epub+zip": return EPUB;
            case "application/x-mobipocket-ebook": return MOBI;
            case "text/plain": return TXT;
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document": return DOCX;
            case "text/html": return HTML;
            case "application/x-chm": return CHM;
            case "application/vnd.comicbook+zip": return CBZ;
            case "application/x-cbr": return CBR;
            case "application/x-fictionbook": return FB2;
            case "image/vnd.djvu": return DJVU;
            case "application/rtf": return RTF;
            default: return UNKNOWN;
        }
    }
}
