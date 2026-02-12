package com.readwise.ai.service

import com.readwise.ai.model.*

/**
 * Core AI service interface
 * Provides unified access to different AI providers
 */
interface AIService {

    /**
     * Get current provider
     */
    fun getProvider(): AIProvider

    /**
     * Get current model name
     */
    fun getModel(): String

    /**
     * Simple chat completion
     * @param messages Conversation history
     * @param config Request configuration
     * @return AI response
     */
    suspend fun chat(
        messages: List<ChatMessage>,
        config: AIRequestConfig? = null
    ): AIResponse

    /**
     * Generate chapter summary
     * @param chapterTitle Chapter title
     * @param chapterContent Chapter text content
     * @param config Request configuration
     * @return Generated summary
     */
    suspend fun summarizeChapter(
        chapterTitle: String,
        chapterContent: String,
        config: AIRequestConfig? = null
    ): ChapterSummary

    /**
     * Generate X-ray entities for book
     * Analyzes characters, locations, terms, etc.
     * @param bookTitle Book title
     * @param chapters List of chapter contents
     * @param config Request configuration
     * @return X-ray analysis data
     */
    suspend fun analyzeXRay(
        bookTitle: String,
        chapters: List<Pair<Int, String>>, // (chapterIndex, content)
        config: AIRequestConfig? = null
    ): XRayData

    /**
     * Explain a concept or term
     * @param term Term to explain
     * @param context Surrounding text (optional)
     * @param config Request configuration
     * @return Explanation text
     */
    suspend fun explainTerm(
        term: String,
        context: String? = null,
        config: AIRequestConfig? = null
    ): String

    /**
     * Translate text
     * @param text Text to translate
     * @param targetLanguage Target language code
     * @param config Request configuration
     * @return Translated text
     */
    suspend fun translate(
        text: String,
        targetLanguage: String,
        config: AIRequestConfig? = null
    ): String

    /**
     * Look up vocabulary word
     * @param word Word to lookup
     * @param context Sentence where word appears
     * @param config Request configuration
     * @return Vocabulary entry with definition
     */
    suspend fun lookupVocabulary(
        word: String,
        context: String? = null,
        config: AIRequestConfig? = null
    ): VocabularyEntry

    /**
     * Stream chat response
     * @param messages Conversation history
     * @param onChunk Callback for each token chunk
     * @param config Request configuration
     * @return Complete response
     */
    suspend fun chatStream(
        messages: List<ChatMessage>,
        onChunk: (String) -> Unit,
        config: AIRequestConfig? = null
    ): AIResponse

    /**
     * Check if service is configured and available
     */
    fun isAvailable(): Boolean

    /**
     * Update service configuration
     */
    fun updateConfig(config: AIRequestConfig)
}
