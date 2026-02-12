package com.readwise.reader.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readwise.core.model.Book
import com.readwise.core.model.ReadPosition
import com.readwise.core.repository.BookRepository
import com.readwise.engine.epub.EpubChapter
import com.readwise.engine.epub.EpubDocumentInfo
import com.readwise.engine.epub.EpubEngine
import com.readwise.engine.epub.EpubLayoutConfig
import com.readwise.engine.epub.EpubTocItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * EPUB 阅读器视图模型
 */
@HiltViewModel
class EpubReaderViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val epubEngine: EpubEngine,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val bookId: String = checkNotNull(savedStateHandle["bookId"])

    private val _uiState = MutableStateFlow(EpubReaderUiState())
    val uiState: StateFlow<EpubReaderUiState> = _uiState.asStateFlow()

    private val _currentChapter = MutableStateFlow(0)
    val currentChapter: StateFlow<Int> = _currentChapter.asStateFlow()

    private val _layoutConfig = MutableStateFlow(EpubLayoutConfig())
    val layoutConfig: StateFlow<EpubLayoutConfig> = _layoutConfig.asStateFlow()

    val documentInfo: StateFlow<EpubDocumentInfo?> = flow {
        emit(loadDocument())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val tableOfContents: StateFlow<List<EpubTocItem>> = flow {
        emit(epubEngine.getTableOfContents())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val currentChapterContent: StateFlow<EpubChapter?> = combine(
        _currentChapter,
        documentInfo
    ) { chapterIndex, _ ->
        chapterIndex
    }.flatMapLatest { chapterIndex ->
        flow {
            emit(loadChapter(chapterIndex))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    init {
        loadBook()
        observeChapterChanges()
    }

    private suspend fun loadDocument(): EpubDocumentInfo? {
        val book = bookRepository.getBookById(bookId) ?: return null
        epubEngine.openDocument(book.filePath)
        return epubEngine.getDocumentInfo()
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

                // 加载到保存的位置
                book.lastReadPosition?.let { position ->
                    goToChapter(position.chapterIndex, false)
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

    private suspend fun loadChapter(chapterIndex: Int): EpubChapter? {
        return if (epubEngine.isChapterValid(chapterIndex)) {
            epubEngine.getChapter(chapterIndex)
        } else null
    }

    fun goToChapter(chapterIndex: Int, animate: Boolean = true) {
        val chapterCount = epubEngine.getChapterCount()
        if (chapterIndex in 0 until chapterCount) {
            _currentChapter.value = chapterIndex
            _uiState.update { it.copy(currentChapterIndex = chapterIndex) }
        }
    }

    fun nextChapter() {
        val next = _currentChapter.value + 1
        if (next < epubEngine.getChapterCount()) {
            goToChapter(next)
        }
    }

    fun previousChapter() {
        val prev = _currentChapter.value - 1
        if (prev >= 0) {
            goToChapter(prev)
        }
    }

    fun jumpToToc(item: EpubTocItem) {
        goToChapter(item.chapterIndex)
    }

    private fun saveReadingProgress() {
        viewModelScope.launch {
            val book = _uiState.value.book ?: return@launch
            val chapter = _currentChapter.value
            val chapterCount = epubEngine.getChapterCount()
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

    fun updateLayoutConfig(config: EpubLayoutConfig) {
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

    fun setTextAlign(align: com.readwise.engine.epub.EpubTextAlign) {
        val current = _layoutConfig.value
        _layoutConfig.value = current.copy(
            textAlign = align
        )
    }

    override fun onCleared() {
        super.onCleared()
        epubEngine.close()
    }
}

/**
 * EPUB 阅读器 UI 状态
 */
data class EpubReaderUiState(
    val isLoading: Boolean = true,
    val book: Book? = null,
    val currentChapterIndex: Int = 0,
    val chapterCount: Int? = null,
    val showToc: Boolean = false,
    val showSettings: Boolean = false,
    val error: String? = null
)
