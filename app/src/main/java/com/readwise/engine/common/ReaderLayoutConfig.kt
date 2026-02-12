package com.readwise.engine.common

/**
 * Unified reader layout configuration
 * Used across all format-specific readers for consistent styling
 */
data class ReaderLayoutConfig(
    val fontSize: Int = 18,
    val lineHeight: Float = 1.6f,
    val paragraphSpacing: Int = 16,
    val marginHorizontal: Int = 16,
    val marginVertical: Int = 16,
    val textColor: Int = 0xFF000000.toInt(),
    val backgroundColor: Int = 0xFFFFFFFF.toInt(),
    val fontFamily: String? = null,
    val textAlign: TextAlign = TextAlign.Start,
    val removeEmptyLines: Boolean = true
)

/**
 * Text alignment options for reader content
 */
enum class TextAlign {
    Start,
    Center,
    End
}

/**
 * Font family options
 */
enum class FontFamily {
    Serif,
    SansSerif,
    Monospace
}

/**
 * Reading theme presets
 */
enum class ReadingTheme(
    val textColor: Int,
    val backgroundColor: Int
) {
    Light(
        textColor = 0xFF000000.toInt(),
        backgroundColor = 0xFFFFFFFF.toInt()
    ),
    Dark(
        textColor = 0xFFFFFFFF.toInt(),
        backgroundColor = 0xFF000000.toInt()
    ),
    Sepia(
        textColor = 0xFF5C4B37.toInt(),
        backgroundColor = 0xFFF5E6D3.toInt()
    ),
    Night(
        textColor = 0xFFCCCCCC.toInt(),
        backgroundColor = 0xFF1A1A1A.toInt()
    )
}
