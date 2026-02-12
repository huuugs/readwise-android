package com.readwise.bookshelf.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readwise.core.model.Book
import com.readwise.core.repository.BookRepository
import com.readwise.core.repository.BookmarkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 书架视图模型
 */
@HiltViewModel
class BookshelfViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val bookmarkRepository: BookmarkRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookshelfUiState())
    val uiState: StateFlow<BookshelfUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.GRID)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _sortMode = MutableStateFlow(SortMode.RECENTLY_READ)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    val books: StateFlow<List<Book>> = combine(
        searchQuery,
        selectedCategory,
        sortMode
    ) { query, category, sort ->
        Triple(query, category, sort)
    }.flatMapLatest { (query, category, _) ->
        when {
            query.isNotEmpty() -> {
                // 搜索模式
                flow {
                    emit(bookRepository.searchBooks(query))
                }
            }
            category != null -> {
                // 分类筛选
                bookRepository.getBooksByCategory(category)
            }
            else -> {
                // 全部书籍
                bookRepository.getAllBooks()
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val categories: StateFlow<List<String>> = bookRepository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadBooks()
    }

    private fun loadBooks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // 数据通过 Flow 自动更新
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelect(category: String?) {
        _selectedCategory.value = category
    }

    fun onViewModeToggle() {
        _viewMode.update { mode ->
            when (mode) {
                ViewMode.GRID -> ViewMode.LIST
                ViewMode.LIST -> ViewMode.GRID
            }
        }
    }

    fun onSortModeChange(sortMode: SortMode) {
        _sortMode.value = sortMode
    }

    fun onDeleteBook(bookId: String) {
        viewModelScope.launch {
            bookRepository.deleteBook(bookId)
        }
    }

    fun onBookClick(book: Book, onNavigate: (String) -> Unit) {
        onNavigate(book.id)
    }

    fun onBookLongClick(book: Book) {
        _uiState.update { it.copy(selectedBook = book) }
    }

    fun dismissSelection() {
        _uiState.update { it.copy(selectedBook = null) }
    }
}

/**
 * 书架UI状态
 */
data class BookshelfUiState(
    val isLoading: Boolean = false,
    val selectedBook: Book? = null,
    val showDeleteDialog: Boolean = false
)

/**
 * 视图模式
 */
enum class ViewMode {
    GRID,
    LIST
}

/**
 * 排序模式
 */
enum class SortMode {
    RECENTLY_READ,    // 最近阅读
    RECENTLY_ADDED,    // 最近添加
    TITLE,            // 书名
    AUTHOR            // 作者
}
