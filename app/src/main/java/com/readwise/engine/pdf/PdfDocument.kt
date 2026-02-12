package com.readwise.engine.pdf

/**
 * PDF 文档接口
 */
interface PdfDocument {
    /**
     * 获取页数
     */
    fun getPageCount(): Int

    /**
     * 获取页面尺寸
     */
    fun getPageSize(pageIndex: Int): PdfPageSize?

    /**
     * 获取文档元数据
     */
    fun getMetadata(): PdfDocumentInfo?

    /**
     * 检查页面是否存在
     */
    fun isPageValid(pageIndex: Int): Boolean
}
