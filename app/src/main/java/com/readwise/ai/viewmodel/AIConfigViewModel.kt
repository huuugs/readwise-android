package com.readwise.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readwise.ai.model.AIProvider
import com.readwise.ai.model.AIRequestConfig
import com.readwise.ai.service.AIConfigManager
import com.readwise.core.database.entity.AIConfigEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for AI configuration management
 */
@HiltViewModel
class AIConfigViewModel @Inject constructor(
    private val configManager: AIConfigManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AIConfigUiState())
    val uiState: StateFlow<AIConfigUiState> = _uiState.asStateFlow()

    val allConfigs = configManager.getAllConfigs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadDefaultProvider()
        loadConfiguredProviders()
    }

    private fun loadDefaultProvider() {
        val defaultProvider = configManager.getDefaultProvider()
        _uiState.update { it.copy(defaultProvider = defaultProvider) }
    }

    private fun loadConfiguredProviders() {
        val configuredProviders = configManager.getConfiguredProviders()
        _uiState.update { it.copy(configuredProviders = configuredProviders) }
    }

    /**
     * Set default provider
     */
    fun setDefaultProvider(provider: AIProvider) {
        configManager.setDefaultProvider(provider)
        _uiState.update { it.copy(defaultProvider = provider) }
    }

    /**
     * Save API key for provider
     */
    fun saveApiKey(provider: AIProvider, apiKey: String) {
        viewModelScope.launch {
            configManager.saveApiKey(provider, apiKey)
            loadConfiguredProviders()
            _uiState.update { it.copy(saveSuccess = true) }
        }
    }

    /**
     * Remove API key for provider
     */
    fun removeApiKey(provider: AIProvider) {
        viewModelScope.launch {
            configManager.removeApiKey(provider)
            loadConfiguredProviders()
        }
    }

    /**
     * Set custom base URL for provider
     */
    fun setBaseUrl(provider: AIProvider, url: String?) {
        viewModelScope.launch {
            configManager.setBaseUrl(provider, url)
        }
    }

    /**
     * Save custom configuration
     */
    fun saveCustomConfig(
        name: String,
        provider: AIProvider,
        apiKey: String,
        baseUrl: String?,
        model: String,
        temperature: Float,
        maxTokens: Int,
        setAsDefault: Boolean
    ) {
        viewModelScope.launch {
            val config = AIConfigEntity(
                name = name,
                service = provider.name,
                apiKey = apiKey, // Will be encrypted
                baseUrl = baseUrl,
                model = model,
                temperature = temperature,
                maxTokens = maxTokens,
                isDefault = setAsDefault
            )

            configManager.saveConfig(config)

            if (setAsDefault) {
                configManager.setAsDefault(config)
            }

            loadConfiguredProviders()
            _uiState.update { it.copy(saveSuccess = true) }
        }
    }

    /**
     * Delete configuration
     */
    fun deleteConfig(config: AIConfigEntity) {
        viewModelScope.launch {
            configManager.deleteConfig(config)
            loadConfiguredProviders()
        }
    }

    /**
     * Set configuration as default
     */
    fun setAsDefault(config: AIConfigEntity) {
        viewModelScope.launch {
            configManager.setAsDefault(config)
            loadDefaultProvider()
        }
    }

    /**
     * Test API connection
     */
    fun testConnection(provider: AIProvider, apiKey: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTesting = true) }

            try {
                // Store API key temporarily for testing
                configManager.saveApiKey(provider, apiKey)

                val isValid = apiKey.isNotEmpty()

                _uiState.update {
                    it.copy(
                        isTesting = false,
                        testSuccess = isValid,
                        errorMessage = if (isValid) null else "Invalid API key"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isTesting = false,
                        testSuccess = false,
                        errorMessage = e.message
                    )
                }
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Clear success flag
     */
    fun clearSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    /**
     * Clear all API keys
     */
    fun clearAllKeys() {
        viewModelScope.launch {
            configManager.clearAllApiKeys()
            loadConfiguredProviders()
        }
    }
}

/**
 * UI state for AI configuration
 */
data class AIConfigUiState(
    val defaultProvider: AIProvider = AIProvider.OPENAI,
    val configuredProviders: List<AIProvider> = emptyList(),
    val isTesting: Boolean = false,
    val testSuccess: Boolean? = null,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)
