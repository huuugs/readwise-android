package com.readwise.core.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 章节
 */
@Parcelize
data class Chapter(
    val index: Int,
    val title: String,
    val startOffset: Int = 0,
    val endOffset: Int = 0,
    val level: Int = 1 // 目录层级
) : Parcelable
