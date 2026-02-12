package com.readwise.reader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.readwise.engine.txt.TxtChapter
import com.readwise.engine.txt.TxtCharset
import com.readwise.engine.txt.TxtLayoutConfig
import com.readwise.engine.txt.TxtTextAlign
import com.readwise.reader.viewmodel.TxtReaderViewModel

/**
 * TXT 阅读器界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TxtReaderScreen(
    viewModel: TxtReaderViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentChapter by viewModel.currentChapter.collectAsState()
    val layoutConfig by viewModel.layoutConfig.collectAsState()
    val chapterList by viewModel.chapterList.collectAsState()
    val scrollState = rememberLazyListState()

    Scaffold(
        topBar = {
            ReaderTopBar(
                title = uiState.book?.displayTitle ?: "",
                chapterTitle = currentChapter?.title ?: "",
                onNavigateBack = onNavigateBack,
                onTocClick = { viewModel.toggleToc() },
                onSettingsClick = { viewModel.toggleSettings() }
            )
        },
        bottomBar = {
            ReaderBottomBar(
                currentChapter = currentChapter?.index ?: 0,
                totalChapters = uiState.chapterCount ?: 0,
                onPreviousChapter = { viewModel.previousChapter() },
                onNextChapter = { viewModel.nextChapter() },
                onSettingsClick = { viewModel.toggleSettings() }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                LoadingIndicator()
            } else if (uiState.error != null) {
                ErrorView(uiState.error ?: "Unknown error")
            } else {
                ChapterContent(
                    chapter = currentChapter,
                    config = layoutConfig,
                    scrollState = scrollState,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 目录侧边栏
            if (uiState.showToc) {
                ChapterListSidebar(
                    chapters = chapterList,
                    currentChapter = currentChapter?.index ?: 0,
                    onChapterClick = { viewModel.jumpToChapter(it) },
                    onDismiss = { viewModel.toggleToc() }
                )
            }

            // 编码选择侧边栏
            if (uiState.showEncodingSelector) {
                EncodingSelectorSidebar(
                    currentCharset = uiState.book?.let {
                        TxtCharset.fromCharsetName(it.format) ?: TxtCharset.UTF_8
                    } ?: TxtCharset.UTF_8,
                    onCharsetChange = { charset ->
                        viewModel.changeCharset(charset)
                        viewModel.toggleEncoding()
                    },
                    onDismiss = { viewModel.toggleEncoding() }
                )
            }

            // 设置面板
            if (uiState.showSettings) {
                ReaderSettingsPanel(
                    config = layoutConfig,
                    onConfigChange = { viewModel.updateLayoutConfig(it) },
                    onDismiss = { viewModel.toggleSettings() }
                )
            }
        }
    }
}

/**
 * 章节内容
 */
@Composable
fun ChapterContent(
    chapter: TxtChapter?,
    config: TxtLayoutConfig,
    scrollState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    if (chapter == null) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text("No content")
        }
        return
    }

    val bgColor = Color(config.backgroundColor)
    val textColor = Color(config.textColor)

    SelectionContainer {
        LazyColumn(
            state = scrollState,
            modifier = modifier
                .background(bgColor)
                .padding(
                    start = config.marginHorizontal.dp,
                    end = config.marginHorizontal.dp,
                    top = config.marginVertical.dp,
                    bottom = config.marginVertical.dp
                )
        ) {
            // 章节标题
            if (chapter.index == 0) {
                item {
                    Text(
                        text = chapter.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = textColor,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }

            // 章节内容
            val paragraphs = chapter.content.lines()
            items(paragraphs) { paragraph ->
                if (paragraph.isNotBlank()) {
                    TextParagraph(
                        text = paragraph,
                        config = config,
                        textColor = textColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * 文本段落
 */
@Composable
fun TextParagraph(
    text: String,
    config: TxtLayoutConfig,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val textAlign = when (config.textAlign) {
        TxtTextAlign.LEFT -> TextAlign.Left
        TxtTextAlign.CENTER -> TextAlign.Center
        TxtTextAlign.RIGHT -> TextAlign.Right
        TxtTextAlign.JUSTIFY -> TextAlign.Start
    }

    val paragraphStyle = MaterialTheme.typography.bodyMedium.copy(
        fontSize = config.fontSize.sp,
        lineHeight = (config.fontSize * config.lineHeight).sp
    )

    val textStyle = if (config.isVertical) {
        paragraphStyle.copy(
            textDirection = androidx.compose.ui.unit.TextDirection.Content
        )
    } else {
        paragraphStyle
    }

    Text(
        text = if (config.removeEmptyLines) {
            text.trim()
        } else {
            text
        },
        style = textStyle,
        color = textColor,
        textAlign = textAlign,
        modifier = modifier.padding(bottom = config.paragraphSpacing.dp)
    )
}

/**
 * 章节列表侧边栏
 */
@Composable
fun ChapterListSidebar(
    chapters: List<TxtChapter>,
    currentChapter: Int,
    onChapterClick: (TxtChapter) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp)
                .padding(16.dp)
        ) {
            Text(
                text = "Chapters",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(chapters) { chapter ->
                    ChapterListItem(
                        chapter = chapter,
                        isCurrent = chapter.index == currentChapter,
                        onClick = { onChapterClick(chapter) }
                    )
                }
            }
        }
    }
}

/**
 * 章节列表项
 */
@Composable
fun ChapterListItem(
    chapter: TxtChapter,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isCurrent) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = chapter.title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 编码选择侧边栏
 */
@Composable
fun EncodingSelectorSidebar(
    currentCharset: TxtCharset,
    onCharsetChange: (TxtCharset) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Select Encoding",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(TxtCharset.values()) { charset ->
                    EncodingItem(
                        charset = charset,
                        isSelected = charset == currentCharset,
                        onClick = { onCharsetChange(charset) }
                    )
                }
            }
        }
    }
}

/**
 * 编码选项
 */
@Composable
fun EncodingItem(
    charset: TxtCharset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = charset.displayName,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * 阅读设置面板
 */
@Composable
fun ReaderSettingsPanel(
    config: TxtLayoutConfig,
    onConfigChange: (TxtLayoutConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var tempConfig by remember { mutableStateOf(config) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reading Settings") },
        text = {
            Column {
                // Font Size
                Text("Font Size: ${tempConfig.fontSize}")
                Slider(
                    value = tempConfig.fontSize.toFloat(),
                    onValueChange = {
                        tempConfig = tempConfig.copy(fontSize = it.toInt())
                        onConfigChange(tempConfig)
                    },
                    valueRange = 12f..32f,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Line Height
                Text("Line Height: ${tempConfig.lineHeight}")
                Slider(
                    value = tempConfig.lineHeight,
                    onValueChange = {
                        tempConfig = tempConfig.copy(lineHeight = it)
                        onConfigChange(tempConfig)
                    },
                    valueRange = 1.0f..2.5f,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Text Align
                Text("Text Alignment")
                TxtTextAlign.values().forEach { align ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = tempConfig.textAlign == align,
                            onClick = {
                                tempConfig = tempConfig.copy(textAlign = align)
                                onConfigChange(tempConfig)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(align.displayName)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Vertical Mode
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = tempConfig.isVertical,
                        onCheckedChange = {
                            tempConfig = tempConfig.copy(isVertical = it)
                            onConfigChange(tempConfig)
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Vertical Mode")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorView(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

private val TxtTextAlign.displayName: String
    get() = when (this) {
        TxtTextAlign.LEFT -> "Left"
        TxtTextAlign.CENTER -> "Center"
        TxtTextAlign.RIGHT -> "Right"
        TxtTextAlign.JUSTIFY -> "Justify"
    }
