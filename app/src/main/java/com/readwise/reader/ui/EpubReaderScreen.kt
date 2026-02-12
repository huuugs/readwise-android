package com.readwise.reader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.readwise.engine.epub.EpubChapter
import com.readwise.engine.epub.EpubLayoutConfig
import com.readwise.engine.epub.EpubTextAlign
import com.readwise.reader.viewmodel.EpubReaderViewModel

/**
 * EPUB 阅读器界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpubReaderScreen(
    viewModel: EpubReaderViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentChapter by viewModel.currentChapter.collectAsState()
    val chapterContent by viewModel.currentChapterContent.collectAsState()
    val tableOfContents by viewModel.tableOfContents.collectAsState()
    val layoutConfig by viewModel.layoutConfig.collectAsState()
    val scrollState = rememberScrollState()

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
                    chapter = chapterContent,
                    config = layoutConfig,
                    scrollState = scrollState,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 目录侧边栏
            if (uiState.showToc) {
                TableOfContentsSidebar(
                    toc = tableOfContents,
                    currentChapter = currentChapter?.index ?: 0,
                    onChapterClick = { viewModel.jumpToToc(it) },
                    onDismiss = { viewModel.toggleToc() }
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
    chapter: EpubChapter?,
    config: EpubLayoutConfig,
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
        item {
            Text(
                text = chapter.title,
                style = MaterialTheme.typography.headlineMedium,
                color = textColor,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // 章节内容
        item {
            val paragraphs = chapter.plainText.split("\n\n")
            paragraphs.forEach { paragraph ->
                if (paragraph.isNotBlank()) {
                    Paragraph(
                        text = paragraph,
                        config = config,
                        textColor = textColor
                    )
                }
            }
        }
    }
}

/**
 * 段落组件
 */
@Composable
fun Paragraph(
    text: String,
    config: EpubLayoutConfig,
    textColor: Color
) {
    val textAlign = when (config.textAlign) {
        EpubTextAlign.LEFT -> TextAlign.Left
        EpubTextAlign.CENTER -> TextAlign.Center
        EpubTextAlign.RIGHT -> TextAlign.Right
        EpubTextAlign.JUSTIFY -> TextAlign.Start
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = config.fontSize.sp,
            lineHeight = (config.fontSize * config.lineHeight).sp
        ),
        color = textColor,
        textAlign = textAlign,
        modifier = Modifier.padding(bottom = config.paragraphSpacing.dp)
    )
}

/**
 * 顶部工具栏
 */
@Composable
fun ReaderTopBar(
    title: String,
    chapterTitle: String,
    onNavigateBack: () -> Unit,
    onTocClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = chapterTitle,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = onTocClick) {
                Icon(Icons.Default.Toc, contentDescription = "TOC")
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }
    )
}

/**
 * 底部工具栏
 */
@Composable
fun ReaderBottomBar(
    currentChapter: Int,
    totalChapters: Int,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onSettingsClick: () -> Unit
) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surface,
        actions = {
            IconButton(onClick = onPreviousChapter, enabled = currentChapter > 0) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
            }

            Text(
                text = "${currentChapter + 1} / $totalChapters",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            IconButton(onClick = onNextChapter, enabled = currentChapter < totalChapters - 1) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next")
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onSettingsClick,
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }
    )
}

/**
 * 目录侧边栏
 */
@Composable
fun TableOfContentsSidebar(
    toc: List<com.readwise.engine.epub.EpubTocItem>,
    currentChapter: Int,
    onChapterClick: (com.readwise.engine.epub.EpubTocItem) -> Unit,
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
                text = "Table of Contents",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(toc) { item ->
                    TocItem(
                        item = item,
                        currentChapter = currentChapter,
                        onClick = { onChapterClick(item) }
                    )
                }
            }
        }
    }
}

/**
 * 目录项
 */
@Composable
fun TocItem(
    item: com.readwise.engine.epub.EpubTocItem,
    currentChapter: Int,
    onClick: () -> Unit
) {
    val isSelected = item.chapterIndex == currentChapter

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
        Text(
            text = item.title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * 阅读设置面板
 */
@Composable
fun ReaderSettingsPanel(
    config: EpubLayoutConfig,
    onConfigChange: (EpubLayoutConfig) -> Unit,
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
                EpubTextAlign.values().forEach { align ->
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

private val EpubTextAlign.displayName: String
    get() = when (this) {
        EpubTextAlign.LEFT -> "Left"
        EpubTextAlign.CENTER -> "Center"
        EpubTextAlign.RIGHT -> "Right"
        EpubTextAlign.JUSTIFY -> "Justify"
    }
