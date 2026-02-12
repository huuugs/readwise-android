package com.readwise.reader.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readwise.core.model.Book
import com.readwise.core.model.ReadPosition
import com.readwise.core.repository.BookRepository
import com.readwise.engine.txt.TxtChapter
import com.readwise.engine.txt.TxtCharset
import com.readwise.engine.txt.TxtEngine
import com.readwise.engine.txt.TxtLayoutConfig
import com.readwise.engine.txt.TxtTextAlign
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * TXT 阅读器视图模型
 */
@HiltViewModel
class TxtReaderViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val txtEngine: TxtEngine,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val bookId: String = checkNotNull(savedStateHandle["bookId"])

    private val _uiState = MutableStateFlow(TxtReaderUiState())
    val uiState: StateFlow<TxtReaderUiState> = _uiState.asStateFlow()

    private val _currentChapter = MutableStateFlow(0)
    val currentChapter: StateFlow<Int> = _currentChapter.asStateFlow()

    private val _layoutConfig = MutableStateFlow(TxtLayoutConfig())
    val layoutConfig: StateFlow<TxtLayoutConfig> = _layoutConfig.asStateFlow()

    private val _availableCharsets = MutableStateFlow(listOf(TxtCharset.UTF_8))
    val availableCharsets: StateFlow<List<TxtCharset>> = _availableCharsets.asStateFlow()

    private val _chapterList = MutableStateFlow<List<TxtChapter>>(emptyList())
    val chapterList: StateFlow<List<TxtChapter>> = _chapterList.asStateFlow()

    val documentInfo: StateFlow<com.readwise.engine.txt.TxtDocumentInfo?> = flow {
        emit(txtEngine.getDocumentInfo())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    init {
        loadBook()
        observeChapterChanges()
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
                        currentChapterIndex = book.lastReadPosition?.chapterIndex ?: 0
                    )
                }

                // 打开文档
                try {
                    txtEngine.openDocument(book.filePath)

                    // 加载章节列表
                    val chapters = txtEngine.getChapters()
                    _chapterList.value = chapters

                    // 跳转到保存的位置
                    book.lastReadPosition?.let { position ->
                        goToChapter(position.chapterIndex, false)
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Book not found") }
            }
        }
    }

    private fun observeChapterChanges() {
        viewModelScope.launch {
            _currentChapter.collect { chapterIndex ->
                saveReadingProgress()
            }
        }
    }

    fun goToChapter(chapterIndex: Int, animate: Boolean = true) {
        val chapterCount = txtEngine.getChapterCount()
        if (chapterIndex in 0 until chapterCount) {
            _currentChapter.value = chapterIndex
            _uiState.update { it.copy(currentChapterIndex = chapterIndex) }
        }
    }

    fun nextChapter() {
        val next = _currentChapter.value + 1
        if (next < txtEngine.getChapterCount()) {
            goToChapter(next)
        }
    }

    fun previousChapter() {
        val prev = _currentChapter.value - 1
        if (prev >= 0) {
            goToChapter(prev)
        }
    }

    fun jumpToChapter(chapter: TxtChapter) {
        goToChapter(chapter.index)
    }

    private fun saveReadingProgress() {
        viewModelScope.launch {
            val book = _uiState.value.book ?: return@launch
            val chapter = _currentChapter.value
            val chapterCount = txtEngine.getChapterCount()
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

    fun toggleToc() {
        _uiState.update { it.copy(showToc = !it.showToc) }
    }

    fun toggleSettings() {
        _uiState.update { it.copy(showSettings = !it.showSettings) }
    }

    fun toggleEncoding() {
        _uiState.update { it.copy(showEncodingSelector = !it.showEncodingSelector) }
    }

    fun updateLayoutConfig(config: TxtLayoutConfig) {
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

    fun setTextAlign(align: TxtTextAlign) {
        val current = _layoutConfig.value
        _layoutConfig.value = current.copy(textAlign = align)
    }

    fun toggleVerticalMode() {
        val current = _layoutConfig.value
        _layoutConfig.value = current.copy(
            isVertical = !current.isVertical
        )
    }

    fun changeCharset(charset: TxtCharset) {
        viewModelScope.launch {
            val book = _uiState.value.book ?: return@launch

            try {
                txtEngine.close()
                txtEngine.openDocument(book.filePath, charset.charset)

                // 重新加载章节
                val chapters = txtEngine.getChapters()
                _chapterList.value = chapters

                // 重新加载当前章节
                goToChapter(0)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        txtEngine.close()
    }
}

/**
 * TXT 阅读器 UI 状态
 */
data class TxtReaderUiState(
    val isLoading: Boolean = true,
    val book: Book? = null,
    val currentChapterIndex: Int = 0,
    val chapterCount: Int? = null,
    val showToc: Boolean = false,
    val showSettings: Boolean = false,
    val showEncodingSelector: Boolean = false,
    val error: String? = null
)
