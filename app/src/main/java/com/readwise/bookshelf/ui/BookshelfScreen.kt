package com.readwise.bookshelf.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.readwise.bookshelf.viewmodel.SortMode
import com.readwise.bookshelf.viewmodel.ViewMode
import com.readwise.core.model.Book
import com.readwise.bookshelf.viewmodel.BookshelfViewModel

/**
 * 书架界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookshelfScreen(
    viewModel: BookshelfViewModel = hiltViewModel(),
    onBookClick: (String) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToDiscovery: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val books by viewModel.books.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("书架") },
                actions = {
                    // 搜索
                    IconButton(onClick = { /* TODO: 打开搜索 */ }) {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    }
                    // 切换视图
                    IconButton(onClick = { viewModel.onViewModeToggle() }) {
                        Icon(
                            when (viewMode) {
                                ViewMode.GRID -> Icons.Default.ViewList
                                ViewMode.LIST -> Icons.Default.GridView
                            },
                            contentDescription = "切换视图"
                        )
                    }
                    // 排序
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "排序")
                    }
                    // 分类
                    IconButton(onClick = { showCategoryMenu = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "筛选")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: 导入书籍 */ }
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加书籍")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (viewMode) {
                ViewMode.GRID -> GridView(
                    books = books,
                    onBookClick = { viewModel.onBookClick(it, onBookClick) },
                    onBookLongClick = { viewModel.onBookLongClick(it) }
                )
                ViewMode.LIST -> ListView(
                    books = books,
                    onBookClick = { viewModel.onBookClick(it, onBookClick) },
                    onBookLongClick = { viewModel.onBookLongClick(it) }
                )
            }

            // 空状态
            if (books.isEmpty() && !uiState.isLoading) {
                EmptyState(
                    onImportClick = { /* TODO */ }
                )
            }
        }
    }

    // 排序菜单
    DropdownMenu(
        expanded = showSortMenu,
        onDismissRequest = { showSortMenu = false }
    ) {
        SortMode.values().forEach { mode ->
            DropdownMenuItem(
                text = { Text(mode.displayName) },
                onClick = {
                    viewModel.onSortModeChange(mode)
                    showSortMenu = false
                },
                trailingIcon = {
                    if (sortMode == mode) {
                        Icon(Icons.Default.Check, contentDescription = null)
                    }
                }
            )
        }
    }

    // 分类菜单
    DropdownMenu(
        expanded = showCategoryMenu,
        onDismissRequest = { showCategoryMenu = false }
    ) {
        DropdownMenuItem(
            text = { Text("全部") },
            onClick = {
                viewModel.onCategorySelect(null)
                showCategoryMenu = false
            },
            trailingIcon = {
                if (selectedCategory == null) {
                    Icon(Icons.Default.Check, contentDescription = null)
                }
            }
        )
        categories.forEach { category ->
            DropdownMenuItem(
                text = { Text(category) },
                onClick = {
                    viewModel.onCategorySelect(category)
                    showCategoryMenu = false
                },
                trailingIcon = {
                    if (selectedCategory == category) {
                        Icon(Icons.Default.Check, contentDescription = null)
                    }
                }
            )
        }
    }
}

/**
 * 网格视图
 */
@Composable
private fun GridView(
    books: List<Book>,
    onBookClick: (Book) -> Unit,
    onBookLongClick: (Book) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(books) { book ->
            BookGridItem(
                book = book,
                onClick = { onBookClick(book) },
                onLongClick = { onBookLongClick(book) }
            )
        }
    }
}

/**
 * 列表视图
 */
@Composable
private fun ListView(
    books: List<Book>,
    onBookClick: (Book) -> Unit,
    onBookLongClick: (Book) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(books) { book ->
            BookListItem(
                book = book,
                onClick = { onBookClick(book) },
                onLongClick = { onBookLongClick(book) }
            )
        }
    }
}

/**
 * 书籍网格项
 */
@Composable
private fun BookGridItem(
    book: Book,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
            .clickable(onClick = onClick)
    ) {
        Box {
            // 封面
            if (book.hasCover()) {
                AsyncImage(
                    model = book.coverPath,
                    contentDescription = book.displayTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // 默认封面
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = book.displayTitle.first().toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // 阅读进度
            if (book.readingProgress > 0 && book.readingProgress < 1f) {
                LinearProgressIndicator(
                    progress = book.readingProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * 书籍列表项
 */
@Composable
private fun BookListItem(
    book: Book,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // 封面
            Box(
                modifier = Modifier
                    .width(50.dp)
                    .height(70.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (book.hasCover()) {
                    AsyncImage(
                        model = book.coverPath,
                        contentDescription = book.displayTitle,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = book.displayTitle.first().toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 信息
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = book.displayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = book.displayAuthor,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = book.formattedFileSize,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (book.readingProgress > 0) {
                        LinearProgressIndicator(
                            progress = book.readingProgress,
                            modifier = Modifier.width(60.dp).height(4.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * 空状态
 */
@Composable
private fun EmptyState(onImportClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.MenuBook,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "书架是空的",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "点击 + 号导入书籍",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onImportClick) {
            Text("导入书籍")
        }
    }
}

private val SortMode.displayName: String
    get() = when (this) {
        SortMode.RECENTLY_READ -> "最近阅读"
        SortMode.RECENTLY_ADDED -> "最近添加"
        SortMode.TITLE -> "书名"
        SortMode.AUTHOR -> "作者"
    }
