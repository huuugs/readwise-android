package com.readwise.ai.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readwise.ai.model.ChatMessage
import com.readwise.ai.model.MessageRole
import com.readwise.ai.repository.AIRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for AI Chat feature
 */
@HiltViewModel
class AIChatViewModel @Inject constructor(
    private val aiRepository: AIRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _uiState = MutableStateFlow(AIChatUiState())
    val uiState: StateFlow<AIChatUiState> = _uiState.asStateFlow()

    private val currentConversation: MutableList<ChatMessage> = mutableListOf()

    init {
        // Add system greeting
        addSystemMessage("Hello! I'm your AI reading assistant. How can I help you today?")
    }

    /**
     * Send message to AI
     */
    fun sendMessage(message: String) {
        if (message.isBlank()) return

        viewModelScope.launch {
            // Add user message
            val userMessage = ChatMessage(MessageRole.USER, message)
            addMessage(userMessage)

            _uiState.update { it.copy(isLoading = true) }

            try {
                // Get response from AI
                val response = aiRepository.chat(currentConversation.toList())

                // Add assistant response
                val assistantMessage = ChatMessage(MessageRole.ASSISTANT, response.content)
                addMessage(assistantMessage)

                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to get response"
                    )
                }
            }
        }
    }

    /**
     * Send message with streaming response
     */
    fun sendMessageStream(message: String) {
        if (message.isBlank()) return

        viewModelScope.launch {
            // Add user message
            val userMessage = ChatMessage(MessageRole.USER, message)
            addMessage(userMessage)

            _uiState.update { it.copy(isLoading = true) }

            try {
                val responseBuilder = StringBuilder()

                // Stream response
                val response = aiRepository.chatStream(
                    messages = currentConversation.toList(),
                    onChunk = { chunk ->
                        responseBuilder.append(chunk)
                        // Update streaming message in real-time
                        _uiState.update {
                            it.copy(
                                streamingContent = responseBuilder.toString()
                            )
                        }
                    }
                )

                // Replace streaming message with final response
                removeStreamingMessage()

                val assistantMessage = ChatMessage(MessageRole.ASSISTANT, response.content)
                addMessage(assistantMessage)

                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to get response"
                    )
                }
            }
        }
    }

    /**
     * Clear conversation
     */
    fun clearConversation() {
        currentConversation.clear()
        _messages.value = emptyList()
        addSystemMessage("Conversation cleared. How can I help you?")
    }

    /**
     * Delete message
     */
    fun deleteMessage(message: ChatMessage) {
        currentConversation.remove(message)
        _messages.value = currentConversation.toList()
    }

    /**
     * Retry last message
     */
    fun retryLastMessage() {
        val lastUserMessage = currentConversation
            .reversed()
            .firstOrNull { it.role == MessageRole.USER }

        if (lastUserMessage != null) {
            // Remove last assistant response if exists
            val messagesToRemove = currentConversation
                .reversed()
                .takeWhile { it.role != MessageRole.USER }
                .toList()

            messagesToRemove.forEach { currentConversation.remove(it) }
            _messages.value = currentConversation.toList()

            // Resend
            sendMessage(lastUserMessage.content)
        }
    }

    /**
     * Copy message content
     */
    fun copyMessage(message: ChatMessage) {
        // Trigger clipboard copy
        _uiState.update { it.copy(copiedMessage = message.content) }
    }

    /**
     * Clear error
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Check if AI is available
     */
    fun isAIAvailable(): Boolean {
        return aiRepository.isAvailable()
    }

    // Private helper methods

    private fun addMessage(message: ChatMessage) {
        currentConversation.add(message)
        _messages.value = currentConversation.toList()
    }

    private fun addSystemMessage(content: String) {
        val message = ChatMessage(MessageRole.SYSTEM, content)
        addMessage(message)
    }

    private fun removeStreamingMessage() {
        _uiState.update { it.copy(streamingContent = null) }
    }
}

/**
 * UI state for AI Chat
 */
data class AIChatUiState(
    val isLoading: Boolean = false,
    val streamingContent: String? = null,
    val error: String? = null,
    val copiedMessage: String? = null
)
