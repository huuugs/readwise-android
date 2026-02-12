package com.readwise.core.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 阅读位置
 */
@Parcelize
data class ReadPosition(
    val chapterIndex: Int = 0,
    val pageIndex: Int? = null, // PDF 页码
    val paragraphIndex: Int = 0,
    val charOffset: Int = 0,
    val progress: Float = 0f, // 0.0 - 1.0
    val updateTime: Long = System.currentTimeMillis()
) : Parcelable {
    companion object {
        fun initial() = ReadPosition()
    }
}
