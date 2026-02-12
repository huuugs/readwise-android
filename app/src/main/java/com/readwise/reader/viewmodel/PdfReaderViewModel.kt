package com.readwise.reader.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readwise.core.model.Book
import com.readwise.core.model.ReadPosition
import com.readwise.core.repository.BookRepository
import com.readwise.engine.pdf.PdfDocument
import com.readwise.engine.pdf.PdfDocumentInfo
import com.readwise.engine.pdf.PdfEngine
import com.readwise.engine.pdf.PdfOutlineItem
import com.readwise.engine.pdf.PdfPageSize
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * PDF 阅读器视图模型
 */
@HiltViewModel
class PdfReaderViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val pdfEngine: PdfEngine,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val bookId: String = checkNotNull(savedStateHandle["bookId"])

    private val _uiState = MutableStateFlow(PdfReaderUiState())
    val uiState: StateFlow<PdfReaderUiState> = _uiState.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val document: StateFlow<PdfDocument?> = flow {
        emit(loadDocument())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val documentInfo: StateFlow<PdfDocumentInfo?> = document.mapNotNull { it?.getMetadata() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val outline: StateFlow<List<PdfOutlineItem>> = flow {
        emit(pdfEngine.getOutline())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadBook()
        observeCurrentPage()
    }

    private suspend fun loadDocument(): PdfDocument? {
        val book = bookRepository.getBookById(bookId) ?: return null
        return pdfEngine.openDocument(book.filePath)
    }

    private fun loadBook() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val book = bookRepository.getBookById(bookId)
            if (book != null) {
                _uiState.update { state ->
                    state.copy(
                        book = book,
                        isLoading = false,
                        currentPage = book.lastReadPosition?.chapterIndex ?: 0
                    )
                }

                // 加载到保存的位置
                book.lastReadPosition?.let { position ->
                    goToPage(position.chapterIndex, false)
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Book not found") }
            }
        }
    }

    private fun observeCurrentPage() {
        viewModelScope.launch {
            currentPage.collect { page ->
                saveReadingProgress()
            }
        }
    }

    fun goToPage(pageIndex: Int, animate: Boolean = true) {
        val pageCount = _uiState.value.pageCount
        if (pageCount == null) return

        if (pageIndex in 0 until pageCount) {
            _currentPage.value = pageIndex
            _uiState.update { it.copy(currentPage = pageIndex) }
        }
    }

    fun nextPage() {
        goToPage(_currentPage.value + 1)
    }

    fun previousPage() {
        goToPage(_currentPage.value - 1)
    }

    fun jumpToOutline(item: PdfOutlineItem) {
        goToPage(item.pageIndex)
    }

    private fun saveReadingProgress() {
        viewModelScope.launch {
            val book = _uiState.value.book ?: return@launch
            val page = _currentPage.value
            val pageCount = _uiState.value.pageCount ?: return@launch
            val progress = page.toFloat() / pageCount.toFloat()

            val position = ReadPosition(
                chapterIndex = page,
                pageIndex = page,
                progress = progress
            )

            bookRepository.updateReadingProgress(
                bookId = book.id,
                progress = progress,
                position = position
            )
        }
    }

    fun toggleOutline() {
        _uiState.update { it.copy(showOutline = !it.showOutline) }
    }

    fun toggleSettings() {
        _uiState.update { it.copy(showSettings = !it.showSettings) }
    }

    fun setScale(scale: Float) {
        _uiState.update { it.copy(scale = scale.coerceIn(0.5f..3f)) }
    }

    fun setZoomMode(mode: ZoomMode) {
        _uiState.update { it.copy(zoomMode = mode) }
    }

    override fun onCleared() {
        super.onCleared()
        pdfEngine.close()
    }
}

/**
 * PDF 阅读器 UI 状态
 */
data class PdfReaderUiState(
    val isLoading: Boolean = true,
    val book: Book? = null,
    val currentPage: Int = 0,
    val pageCount: Int? = null,
    val scale: Float = 1f,
    val zoomMode: ZoomMode = ZoomMode.FIT_WIDTH,
    val showOutline: Boolean = false,
    val showSettings: Boolean = false,
    val error: String? = null
)

/**
 * 缩放模式
 */
enum class ZoomMode {
    FIT_WIDTH,     // 适应宽度
    FIT_PAGE,      // 适应页面
    FIT_HEIGHT,    // 适应高度
    CUSTOM         // 自定义缩放
}
