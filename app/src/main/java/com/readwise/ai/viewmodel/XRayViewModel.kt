package com.readwise.ai.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readwise.ai.model.XRayData
import com.readwise.ai.model.XRayEntity
import com.readwise.ai.model.XRayEntityType
import com.readwise.ai.repository.AIRepository
import com.readwise.core.model.Book
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for X-ray feature
 * Manages entity extraction and display
 */
@HiltViewModel
class XRayViewModel @Inject constructor(
    private val aiRepository: AIRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val bookId: String = checkNotNull(savedStateHandle["bookId"])

    private val _uiState = MutableStateFlow(XRayUiState())
    val uiState: StateFlow<XRayUiState> = _uiState.asStateFlow()

    private val _selectedEntity = MutableStateFlow<XRayEntity?>(null)
    val selectedEntity: StateFlow<XRayEntity?> = _selectedEntity.asStateFlow()

    private val _xRayData = MutableStateFlow<XRayData?>(null)
    val xRayData: StateFlow<XRayData?> = _xRayData.asStateFlow()

    val entities: StateFlow<List<XRayEntity>> = _xRayData
        .map { data ->
            data?.entities?.sortedByDescending { it.mentions.size } ?: emptyList()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadXRayData()
    }

    private fun loadXRayData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Check if X-ray data exists
            val hasData = aiRepository.hasXRayData(bookId)

            if (hasData) {
                aiRepository.getXRayData(bookId).collect { data ->
                    _xRayData.value = data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hasGenerated = true
                        )
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        hasGenerated = false
                    )
                }
            }
        }
    }

    /**
     * Generate X-ray analysis for current book
     */
    fun generateXRay(book: Book, chapters: List<Pair<Int, String>>) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGenerating = true,
                    progress = 0,
                    progressTotal = chapters.size
                )
            }

            try {
                val xRayData = aiRepository.generateXRay(
                    bookId = book.id,
                    bookTitle = book.displayTitle,
                    chapters = chapters
                )

                _xRayData.value = xRayData
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        hasGenerated = true,
                        progress = null,
                        progressTotal = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        error = e.message ?: "Failed to generate X-ray"
                    )
                }
            }
        }
    }

    /**
     * Select entity to view details
     */
    fun selectEntity(entity: XRayEntity) {
        viewModelScope.launch {
            _selectedEntity.value = entity
            _uiState.update { it.copy(showEntityDetail = true) }
        }
    }

    /**
     * Close entity detail
     */
    fun closeEntityDetail() {
        _selectedEntity.value = null
        _uiState.update { it.copy(showEntityDetail = false) }
    }

    /**
     * Filter entities by type
     */
    fun filterByType(type: XRayEntityType?) {
        viewModelScope.launch {
            val filtered = if (type == null) {
                _xRayData.value?.entities
            } else {
                _xRayData.value?.entities?.filter { it.type == type }
            }

            _uiState.update { it.copy(selectedFilter = type) }
        }
    }

    /**
     * Search entities by name
     */
    fun searchEntities(query: String) {
        viewModelScope.launch {
            val filtered = if (query.isBlank()) {
                _xRayData.value?.entities
            } else {
                _xRayData.value?.entities?.filter {
                    it.name.contains(query, ignoreCase = true)
                }
            }

            _uiState.update {
                it.copy(
                    searchQuery = query,
                    filteredEntities = filtered
                )
            }
        }
    }

    /**
     * Get related entities
     */
    fun getRelatedEntities(entity: XRayEntity): List<XRayEntity> {
        val data = _xRayData.value ?: return emptyList()

        return entity.relatedEntities.mapNotNull { relatedId ->
            data.entities.find { it.id == relatedId }
        }
    }

    /**
     * Get entity mentions in chapters
     */
    fun getEntityChapters(entity: XRayEntity): List<Int> {
        return entity.mentions.sorted()
    }

    /**
     * Delete X-ray data
     */
    fun deleteXRayData() {
        viewModelScope.launch {
            aiRepository.deleteXRayData(bookId)
            _xRayData.value = null
            _uiState.update { it.copy(hasGenerated = false) }
        }
    }

    /**
     * Retry generation
     */
    fun retryGeneration() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Clear error
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Get entity count by type
     */
    fun getEntityCountByType(): Map<XRayEntityType, Int> {
        val data = _xRayData.value ?: return emptyMap()

        return data.entities
            .groupBy { it.type }
            .mapValues { it.value.size }
            .toSortedMap()
    }
}

/**
 * UI state for X-ray
 */
data class XRayUiState(
    val isLoading: Boolean = true,
    val isGenerating: Boolean = false,
    val hasGenerated: Boolean = false,
    val showEntityDetail: Boolean = false,
    val selectedFilter: XRayEntityType? = null,
    val searchQuery: String = "",
    val filteredEntities: List<XRayEntity>? = null,
    val error: String? = null,
    val progress: Int? = null,
    val progressTotal: Int? = null
)
