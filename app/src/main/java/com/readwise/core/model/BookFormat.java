package com.readwise.core.model;

/**
 * 书籍格式
 */
public class BookFormat {
    private final String fileExtension;

    BookFormat(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public String getDisplayName() {
        if (this instanceof PDF) return "Portable Document Format";
        if (this instanceof EPUB) return "EPUB";
        if (this instanceof MOBI) return "MOBI";
        if (this instanceof AZW3) return "AZW3";
        if (this instanceof TXT) return "TXT";
        if (this instanceof DOCX) return "DOCX";
        if (this instanceof HTML) return "HTML";
        if (this instanceof CHM) return "CHM";
        if (this instanceof CBZ) return "CBZ";
        if (this instanceof CBR) return "CBR";
        if (this instanceof FB2) return "FB2";
        if (this instanceof DJVU) return "DJVU";
        if (this instanceof RTF) return "RTF";
        return "未知";
    }

    public static final BookFormat PDF = new BookFormat("pdf") {};
    public static final BookFormat EPUB = new BookFormat("epub") {};
    public static final BookFormat MOBI = new BookFormat("mobi") {};
    public static final BookFormat AZW3 = new BookFormat("azw3") {};
    public static final BookFormat TXT = new BookFormat("txt") {};
    public static final BookFormat DOCX = new BookFormat("docx") {};
    public static final BookFormat HTML = new BookFormat("html") {};
    public static final BookFormat CHM = new BookFormat("chm") {};
    public static final BookFormat CBZ = new BookFormat("cbz") {};
    public static final BookFormat CBR = new BookFormat("cbr") {};
    public static final BookFormat FB2 = new BookFormat("fb2") {};
    public static final BookFormat DJVU = new BookFormat("djvu") {};
    public static final BookFormat RTF = new BookFormat("rtf") {};
    public static final BookFormat UNKNOWN = new BookFormat("") {};

    public static BookFormat fromExtension(String extension) {
        for (BookFormat format : new BookFormat[]{PDF, EPUB, MOBI, AZW3, TXT, DOCX, HTML, CHM, CBZ, CBR, FB2, DJVU, RTF}) {
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
