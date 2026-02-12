package com.readwise.reader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.readwise.engine.pdf.PdfOutlineItem
import com.readwise.reader.viewmodel.PdfReaderViewModel
import com.readwise.reader.viewmodel.ZoomMode
import kotlinx.coroutines.launch

/**
 * PDF 阅读器界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    viewModel: PdfReaderViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val documentInfo by viewModel.documentInfo.collectAsState()
    val outline by viewModel.outline.collectAsState()

    val scope = rememberCoroutineScope()
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    Scaffold(
        topBar = {
            ReaderTopBar(
                title = uiState.book?.displayTitle ?: "",
                currentPage = currentPage + 1,
                totalPages = uiState.pageCount ?: 0,
                onNavigateBack = onNavigateBack,
                onOutlineClick = { viewModel.toggleOutline() },
                onSettingsClick = { viewModel.toggleSettings() }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .onSizeChanged { size ->
                    containerSize = IntSize(size.width, size.height)
                }
        ) {
            if (uiState.isLoading) {
                LoadingIndicator()
            } else if (uiState.error != null) {
                ErrorView(uiState.error ?: "Unknown error")
            } else {
                PdfContent(
                    viewModel = viewModel,
                    containerWidth = containerSize.width,
                    containerHeight = containerSize.height
                )
            }

            // 目录侧边栏
            if (uiState.showOutline) {
                OutlineSidebar(
                    outline = outline,
                    currentPage = currentPage,
                    onOutlineClick = { viewModel.jumpToOutline(it) },
                    onDismiss = { viewModel.toggleOutline() }
                )
            }

            // 设置面板
            if (uiState.showSettings) {
                ReaderSettingsPanel(
                    scale = uiState.scale,
                    zoomMode = uiState.zoomMode,
                    onScaleChange = { viewModel.setScale(it) },
                    onZoomModeChange = { viewModel.setZoomMode(it) },
                    onDismiss = { viewModel.toggleSettings() }
                )
            }
        }
    }
}

/**
 * PDF 内容区域
 */
@Composable
fun PdfContent(
    viewModel: PdfReaderViewModel,
    containerWidth: Int,
    containerHeight: Int
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 渲染当前页
        PdfPageView(
            pageIndex = currentPage,
            viewModel = viewModel,
            containerWidth = containerWidth,
            containerHeight = containerHeight
        )

        // 手势控制
        GestureDetector(
            onTap = { offset ->
                handlePageTap(offset, containerWidth, viewModel)
            }
        )
    }
}

/**
 * PDF 页面视图
 */
@Composable
fun PdfPageView(
    pageIndex: Int,
    viewModel: PdfReaderViewModel,
    containerWidth: Int,
    containerHeight: Int
) {
    val uiState by viewModel.uiState.collectAsState()

    // 计算页面尺寸
    val pageWidth = when (uiState.zoomMode) {
        ZoomMode.FIT_WIDTH -> containerWidth
        ZoomMode.FIT_PAGE -> containerWidth
        ZoomMode.FIT_HEIGHT -> (containerWidth * 0.707f).toInt() // A4 ratio
        ZoomMode.CUSTOM -> (containerWidth * uiState.scale).toInt()
    }

    val pageHeight = when (uiState.zoomMode) {
        ZoomMode.FIT_WIDTH -> (containerWidth * 1.414f).toInt() // A4 ratio
        ZoomMode.FIT_PAGE -> containerHeight
        ZoomMode.FIT_HEIGHT -> containerHeight
        ZoomMode.CUSTOM -> (containerHeight * uiState.scale).toInt()
    }

    // 显示占位符
    // TODO: 实际渲染 PDF 页面
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Page ${pageIndex + 1}",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
    }
}

/**
 * 手势检测
 */
@Composable
fun GestureDetector(
    onTap: (androidx.compose.ui.geometry.Offset) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset -> onTap(offset) }
                )
            }
    )
}

/**
 * 处理页面点击
 */
private fun handlePageTap(
    offset: androidx.compose.ui.geometry.Offset,
    containerWidth: Int,
    viewModel: PdfReaderViewModel
) {
    val tapX = offset.x

    // 将屏幕分为左中右三部分
    when {
        tapX < containerWidth / 3f -> viewModel.previousPage()
        tapX > containerWidth * 2f / 3f -> viewModel.nextPage()
        else -> {
            // 中间点击 - 显示/隐藏工具栏
            // TODO: 实现工具栏显示/隐藏
        }
    }
}

/**
 * 顶部工具栏
 */
@Composable
fun ReaderTopBar(
    title: String,
    currentPage: Int,
    totalPages: Int,
    onNavigateBack: () -> Unit,
    onOutlineClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    maxLines = 1
                )
                Text(
                    text = "$currentPage / $totalPages",
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
            IconButton(onClick = onOutlineClick) {
                Icon(Icons.Default.Toc, contentDescription = "Outline")
            }
            IconButton(onClick = onSettingsClick) {
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
    outline: List<PdfOutlineItem>,
    currentPage: Int,
    onOutlineClick: (PdfOutlineItem) -> Unit,
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
                text = "Directory",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(outline) { item ->
                    OutlineItem(
                        item = item,
                        currentPage = currentPage,
                        onClick = { onOutlineClick(item) }
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
    item: com.readwise.engine.pdf.PdfOutlineItem,
    currentPage: Int,
    onClick: () -> Unit
) {
    val isSelected = item.pageIndex == currentPage

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
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
                .clickable(onClick = onClick)
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
    scale: Float,
    zoomMode: ZoomMode,
    onScaleChange: (Float) -> Unit,
    onZoomModeChange: (ZoomMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reading Settings") },
        text = {
            Column {
                Text("Zoom Mode")
                ZoomMode.values().forEach { mode ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = zoomMode == mode,
                            onClick = { onZoomModeChange(mode) }
                        )
                        Text(mode.displayName)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Scale: ${(scale * 100).toInt()}%")
                Slider(
                    value = scale,
                    onValueChange = onScaleChange,
                    valueRange = 0.5f..3f
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * 加载指示器
 */
@Composable
fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * 错误视图
 */
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

private val ZoomMode.displayName: String
    get() = when (this) {
        ZoomMode.FIT_WIDTH -> "Fit Width"
        ZoomMode.FIT_PAGE -> "Fit Page"
        ZoomMode.FIT_HEIGHT -> "Fit Height"
        ZoomMode.CUSTOM -> "Custom"
    }
