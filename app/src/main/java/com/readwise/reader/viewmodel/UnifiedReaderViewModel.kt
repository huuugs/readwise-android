package com.readwise.reader.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readwise.core.model.Book
import com.readwise.core.model.BookFormat
import com.readwise.core.model.ReadPosition
import com.readwise.core.repository.BookRepository
import com.readwise.engine.common.ReaderLayoutConfig
import com.readwise.engine.common.TextAlign as EngineTextAlign
import com.readwise.engine.common.UnifiedEngine
import com.readwise.engine.common.BookFormatDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 统一阅读器视图模型
 * 支持多种文档格式的统一阅读
 */
@HiltViewModel
class UnifiedReaderViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val unifiedEngine: UnifiedEngine,
    private val formatDetector: BookFormatDetector,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val bookId: String = checkNotNull(savedStateHandle["bookId"])

    private val _uiState = MutableStateFlow(UnifiedReaderUiState())
    val uiState: StateFlow<UnifiedReaderUiState> = _uiState.asStateFlow()

    private val _currentChapter = MutableStateFlow(0)
    val currentChapter: StateFlow<Int> = _currentChapter.asStateFlow()

    private val _chapterList = MutableStateFlow<List<com.readwise.engine.common.UnifiedChapter>>(emptyList())
    val chapterList: StateFlow<List<com.readwise.engine.common.UnifiedChapter>> = _chapterList.asStateFlow()

    private val _tocList = MutableStateFlow<List<com.readwise.engine.common.OutlineItem>>(emptyList())
    val tocList: StateFlow<List<com.readwise.engine.common.OutlineItem>> = _tocList.asStateFlow()

    private val _layoutConfig = MutableStateFlow(ReaderLayoutConfig())
    val layoutConfig: StateFlow<ReaderLayoutConfig> = _layoutConfig.asStateFlow()

    private val _outlineVisible = MutableStateFlow(false)
    val outlineVisible: StateFlow<Boolean> = _outlineVisible.asStateFlow()

    private val _xRayVisible = MutableStateFlow(false)
    val xRayVisible: StateFlow<Boolean> = _xRayVisible.asStateFlow()

    init {
        loadBook()
        observeCurrentChapter()
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
                        format = book.format
                    )
                }

                // 打开文档
                try {
                    unifiedEngine.openDocument(book.filePath, book.format)
                    _tocList.value = unifiedEngine.getOutline()
                    _chapterList.value = getChapterList()

                    // 加载到保存的位置
                    book.lastReadPosition?.let { position ->
                        goToChapter(position.chapterIndex, false)
                    }
                } catch (e: Exception) {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            error = e.message
                        )
                    }
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Book not found") }
            }
        }
    }

    private fun getChapterList(): List<com.readwise.engine.common.UnifiedChapter> {
        val count = unifiedEngine.getChapterCount()
        return (0 until count).map { index ->
            try {
                unifiedEngine.getChapter(index)
            } catch (e: Exception) {
                com.readwise.engine.common.UnifiedChapter(
                    index = index,
                    title = "Chapter ${index + 1}",
                    content = "Error: ${e.message}"
                )
            }
        }
    }

    private fun observeCurrentChapter() {
        viewModelScope.launch {
            currentChapter.collect { index ->
                saveReadingProgress()
            }
        }
    }

    fun goToChapter(chapterIndex: Int, animate: Boolean = true) {
        val chapterCount = unifiedEngine.getChapterCount()
        if (chapterIndex in 0 until chapterCount) {
            _currentChapter.value = chapterIndex
            _uiState.update { it.copy(currentChapterIndex = chapterIndex) }
        }
    }

    fun nextChapter() {
        val next = _currentChapter.value + 1
        if (next < unifiedEngine.getChapterCount()) {
            goToChapter(next)
        }
    }

    fun previousChapter() {
        val prev = _currentChapter.value - 1
        if (prev >= 0) {
            goToChapter(prev)
        }
    }

    fun jumpToOutline(item: com.readwise.engine.common.OutlineItem) {
        goToChapter(item.chapterIndex)
    }

    private fun saveReadingProgress() {
        viewModelScope.launch {
            val book = _uiState.value.book ?: return@launch
            val chapter = _currentChapter.value
            val chapterCount = unifiedEngine.getChapterCount()
            val progress = if (chapterCount > 0) {
                chapter.toFloat() / chapterCount.toFloat()
            } else 0f

            val position = ReadPosition(
                chapterIndex = chapter,
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
        _outlineVisible.value = !_outlineVisible.value
    }

    fun toggleXRay() {
        _xRayVisible.value = !_xRayVisible.value
    }

    fun toggleSettings() {
        _uiState.update { it.copy(showSettings = !it.showSettings) }
    }

    fun updateLayoutConfig(config: ReaderLayoutConfig) {
        _layoutConfig.value = config
    }

    fun setFontSize(size: Int) {
        val current = _layoutConfig.value
        _layoutConfig.value = current.copy(
            fontSize = size.coerceIn(12..32)
        )
    }

    fun setLineHeight(height: Float) {
        val current = _layoutConfig.value
        _layoutConfig.value = current.copy(
            lineHeight = height.coerceIn(1.0f..2.5f)
        )
    }

    fun setTextAlign(align: EngineTextAlign) {
        val current = _layoutConfig.value
        _layoutConfig.value = current.copy(textAlign = align)
    }

    override fun onCleared() {
        super.onCleared()
        unifiedEngine.close()
    }
}

/**
 * 统一阅读器 UI 状态
 */
data class UnifiedReaderUiState(
    val isLoading: Boolean = true,
    val book: Book? = null,
    val format: BookFormat = BookFormat.UNKNOWN,
    val currentChapterIndex: Int = 0,
    val chapterCount: Int? = null,
    val showOutline: Boolean = false,
    val showXRay: Boolean = false,
    val showSettings: Boolean = false,
    val error: String? = null
)
