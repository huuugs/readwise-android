package com.readwise.reader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.readwise.ai.model.XRayEntity
import com.readwise.ai.model.XRayEntityType
import com.readwise.engine.common.BookFormatDetector
import com.readwise.engine.common.OutlineItem
import com.readwise.engine.common.ReaderLayoutConfig
import com.readwise.engine.common.TextAlign
import com.readwise.reader.viewmodel.UnifiedReaderViewModel
import kotlinx.coroutines.launch

/**
 * 统一阅读器界面
 * 支持多种文档格式的统一阅读
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedReaderScreen(
    viewModel: UnifiedReaderViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentChapter by viewModel.currentChapter.collectAsState()
    val chapterList by viewModel.chapterList.collectAsState()
    val tocList by viewModel.tocList.collectAsState()
    val layoutConfig by viewModel.layoutConfig.collectAsState()
    val outlineVisible by viewModel.outlineVisible.collectAsState()
    val xRayVisible by viewModel.xRayVisible.collectAsState()

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            ReaderTopBar(
                title = uiState.book?.displayTitle ?: "",
                format = uiState.format,
                onNavigateBack = onNavigateBack,
                onTocClick = { viewModel.toggleOutline() },
                onXRayClick = { viewModel.toggleXRay() },
                onSettingsClick = { viewModel.toggleSettings() }
            )
        },
        bottomBar = {
            ReaderBottomBar(
                currentChapter = currentChapter,
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
                ReaderContent(
                    chapter = currentChapter,
                    chapterList = chapterList,
                    config = layoutConfig,
                    format = uiState.format,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 目录侧边栏
            if (outlineVisible) {
                OutlineSidebar(
                    toc = tocList,
                    currentChapter = currentChapter,
                    onChapterClick = { item ->
                        scope.launch {
                            viewModel.jumpToOutline(item)
                        }
                    },
                    onDismiss = { viewModel.toggleOutline() }
                )
            }

            // X-ray 快捷面板
            if (xRayVisible) {
                XRayQuickPanel(
                    bookId = uiState.book?.id ?: "",
                    onDismiss = { viewModel.toggleXRay() },
                    onOpenFull = { /* Navigate to full X-ray */ }
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
 * 阅读内容区域
 */
@Composable
fun ReaderContent(
    chapter: Int?,
    chapterList: List<com.readwise.engine.common.UnifiedChapter>,
    config: ReaderLayoutConfig,
    format: com.readwise.core.model.BookFormat,
    modifier: Modifier = Modifier
) {
    val bgColor = Color(config.backgroundColor)
    val textColor = Color(config.textColor)

    if (chapter == null || chapterList.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text("No content")
        }
        return
    }

    val currentChapter = chapterList[chapter]

    LazyColumn(
        state = rememberLazyListState(),
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
            ChapterTitle(
                title = currentChapter.title,
                textColor = textColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }

        // 章节内容
        if (currentChapter.plainText.isNotEmpty()) {
            item {
                val paragraphs = currentChapter.plainText.split("\n\n")
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

        // PDF 特殊处理（内容在 UI 层渲染）
        if (currentChapter.content.isEmpty() &&
            uiState.format == BookFormatDetector.Format.PDF) {
            item {
                PdfPageView(
                    pageNumber = chapter + 1,
                    textColor = textColor,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * 章节标题
 */
@Composable
fun ChapterTitle(
    title: String,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = textColor,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

/**
 * 文本段落
 */
@Composable
fun Paragraph(
    text: String,
    config: ReaderLayoutConfig,
    textColor: Color
) {
    val textAlign = when (config.textAlign) {
        TextAlign.Start -> androidx.compose.ui.text.style.TextAlign.Start
        TextAlign.Center -> androidx.compose.ui.text.style.TextAlign.Center
        TextAlign.End -> androidx.compose.ui.text.style.TextAlign.End
    }

    Text(
        text = if (config.removeEmptyLines) {
            text.trim()
        } else {
            text
        },
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = config.fontSize.sp,
            lineHeight = (config.fontSize * config.lineHeight).sp,
            textAlign = textAlign
        ),
        color = textColor,
        modifier = Modifier
            .padding(bottom = config.paragraphSpacing.dp)
    )
}

/**
 * PDF 页面视图
 */
@Composable
fun PdfPageView(
    pageNumber: Int,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = textColor.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "PDF Page $pageNumber",
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
        }
    }
}

/**
 * 顶部工具栏
 */
@Composable
fun ReaderTopBar(
    title: String,
    format: BookFormat,
    onNavigateBack: () -> Unit,
    onTocClick: () -> Unit,
    onXRayClick: () -> Unit,
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
                    text = format.name,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
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
            IconButton(onClick = onXRayClick) {
                Icon(Icons.Default.PeopleOutline, contentDescription = "X-ray")
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
            IconButton(
                onClick = onPreviousChapter,
                enabled = currentChapter > 0
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
            }
            Text(
                text = "${currentChapter + 1} / $totalChapters",
                style = MaterialTheme.typography.bodyMedium
            )
            IconButton(
                onClick = onNextChapter,
                enabled = currentChapter < totalChapters - 1
            ) {
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
fun OutlineSidebar(
    toc: List<OutlineItem>,
    currentChapter: Int,
    onChapterClick: (OutlineItem) -> Unit,
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
                    OutlineItem(
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
fun OutlineItem(
    item: OutlineItem,
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
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp)
        )
    }
}

/**
 * 阅读设置面板
 */
@Composable
fun ReaderSettingsPanel(
    config: ReaderLayoutConfig,
    onConfigChange: (ReaderLayoutConfig) -> Unit,
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
                TextAlign.values().forEach { align ->
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

                Spacer(modifier = Modifier.height(24.dp))
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.CenterVertically
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

private val TextAlign.displayName: String
    get() = when (this) {
        TextAlign.Start -> "Left"
        TextAlign.Center -> "Center"
        TextAlign.End -> "Right"
    }


/**
 * X-ray Quick Panel
 * Shows top entities in book without leaving reader
 */
@Composable
fun XRayQuickPanel(
    bookId: String,
    onDismiss: () -> Unit,
    onOpenFull: () -> Unit
) {
    // TODO: Integrate with XRayViewModel to fetch actual data
    val sampleEntities = listOf(
        XRayEntity(
            id = "1",
            bookId = bookId,
            type = XRayEntityType.CHARACTER,
            name = "Elizabeth Bennet",
            description = "Protagonist, intelligent and witty",
            firstMention = 0,
            mentions = listOf(0, 1, 2, 3, 4)
        ),
        XRayEntity(
            id = "2",
            bookId = bookId,
            type = XRayEntityType.CHARACTER,
            name = "Mr. Darcy",
            description = "Wealthy aristocrat, love interest",
            firstMention = 0,
            mentions = listOf(0, 1, 2, 3)
        ),
        XRayEntity(
            id = "3",
            bookId = bookId,
            type = XRayEntityType.LOCATION,
            name = "Longbourn",
            description = "Bennet family estate",
            firstMention = 0,
            mentions = listOf(0, 1, 2)
        )
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = false
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "X-ray",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onOpenFull) {
                    Text("View Full")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sampleEntities) { entity ->
                    XRayQuickEntityCard(entity = entity)
                }
            }
        }
    }
}

/**
 * Quick entity card for bottom sheet
 */
@Composable
fun XRayQuickEntityCard(entity: XRayEntity) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable { /* TODO: Show entity detail */ }
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(entity.type.color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = entity.type.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Name
            Text(
                text = entity.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Description
            Text(
                text = entity.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}
