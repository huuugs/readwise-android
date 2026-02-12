package com.readwise.ai.repository

import com.readwise.ai.model.*
import com.readwise.ai.service.AIService
import com.readwise.core.database.dao.BookDao
import com.readwise.core.database.dao.ChapterSummaryDao
import com.readwise.core.database.dao.VocabularyDao
import com.readwise.core.database.dao.XRayDataDao
import com.readwise.core.database.entity.ChapterSummaryEntity
import com.readwise.core.database.entity.VocabularyEntity
import com.readwise.core.database.entity.XRayDataEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for AI features
 * Manages AI service interactions and data persistence
 */
@Singleton
class AIRepository @Inject constructor(
    private val aiService: AIService,
    private val xRayDataDao: XRayDataDao,
    private val chapterSummaryDao: ChapterSummaryDao,
    private val vocabularyDao: VocabularyDao,
    private val bookDao: BookDao
) {

    private val json = Json { ignoreUnknownKeys = true }

    // === Chat ===

    /**
     * Send chat message and get response
     */
    suspend fun chat(messages: List<ChatMessage>): AIResponse {
        return aiService.chat(messages)
    }

    /**
     * Stream chat response
     */
    suspend fun chatStream(
        messages: List<ChatMessage>,
        onChunk: (String) -> Unit
    ): AIResponse {
        return aiService.chatStream(messages, onChunk)
    }

    /**
     * Check if AI service is available
     */
    fun isAvailable(): Boolean = aiService.isAvailable()

    // === Chapter Summarization ===

    /**
     * Generate summary for chapter
     */
    suspend fun generateChapterSummary(
        bookId: String,
        chapterIndex: Int,
        chapterTitle: String?,
        chapterContent: String
    ): ChapterSummary {
        val summary = aiService.summarizeChapter(
            chapterTitle ?: "Chapter ${chapterIndex + 1}",
            chapterContent,
            null
        )

        // Save to database
        val entity = ChapterSummaryEntity(
            bookId = bookId,
            chapterIndex = chapterIndex,
            chapterTitle = chapterTitle,
            summary = summary.summary,
            keyPoints = json.encodeToString(summary.keyPoints),
            quotes = summary.importantQuotes?.let { json.encodeToString(it) },
            aiModel = summary.aiModel
        )
        chapterSummaryDao.insert(entity)

        return summary
    }

    /**
     * Get summary for chapter
     */
    fun getChapterSummary(bookId: String, chapterIndex: Int): Flow<ChapterSummary?> {
        return chapterSummaryDao.getSummary(bookId, chapterIndex).map { entity ->
            entity?.toChapterSummary()
        }
    }

    /**
     * Get all summaries for book
     */
    fun getBookSummaries(bookId: String): Flow<List<ChapterSummary>> {
        return chapterSummaryDao.getSummariesForBook(bookId).map { entities ->
            entities.map { it.toChapterSummary() }
        }
    }

    /**
     * Check if chapter has summary
     */
    suspend fun hasSummary(bookId: String, chapterIndex: Int): Boolean {
        return chapterSummaryDao.getSummarySync(bookId, chapterIndex) != null
    }

    /**
     * Delete chapter summary
     */
    suspend fun deleteChapterSummary(bookId: String, chapterIndex: Int) {
        chapterSummaryDao.deleteByChapter(bookId, chapterIndex)
    }

    // === X-ray Analysis ===

    /**
     * Generate X-ray analysis for book
     */
    suspend fun generateXRay(
        bookId: String,
        bookTitle: String,
        chapters: List<Pair<Int, String>>
    ): XRayData {
        val xRayData = aiService.analyzeXRay(bookTitle, chapters, null)

        // Save to database
        val entity = XRayDataEntity(
            bookId = bookId,
            data = json.encodeToString(xRayData),
            isComplete = xRayData.isComplete,
            version = 1
        )
        xRayDataDao.insert(entity)

        return xRayData
    }

    /**
     * Get X-ray data for book
     */
    fun getXRayData(bookId: String): Flow<XRayData?> {
        return xRayDataDao.getXRayData(bookId).map { entity ->
            entity?.toXRayData()
        }
    }

    /**
     * Check if X-ray data exists
     */
    suspend fun hasXRayData(bookId: String): Boolean {
        return xRayDataDao.getXRayDataSync(bookId) != null
    }

    /**
     * Delete X-ray data
     */
    suspend fun deleteXRayData(bookId: String) {
        xRayDataDao.deleteByBookId(bookId)
    }

    /**
     * Get entity by name from X-ray data
     */
    suspend fun getXRayEntity(bookId: String, entityName: String): XRayEntity? {
        val xRayData = xRayDataDao.getXRayDataSync(bookId) ?: return null
        val data = json.decodeFromString<XRayData>(xRayData.data)
        return data.entities.find { it.name == entityName }
    }

    // === Vocabulary ===

    /**
     * Look up word and save to history
     */
    suspend fun lookupVocabulary(
        word: String,
        context: String? = null
    ): VocabularyEntry {
        val entry = aiService.lookupVocabulary(word, context, null)

        // Save to history
        val entity = VocabularyEntity(
            word = entry.word,
            definition = entry.definition,
            partOfSpeech = entry.partOfSpeech,
            example = entry.example,
            context = context,
            frequency = 1,
            lookupTime = System.currentTimeMillis()
        )
        vocabularyDao.insert(entity)

        return entry
    }

    /**
     * Get vocabulary lookup history
     */
    fun getVocabularyHistory(limit: Int = 50): Flow<List<VocabularyEntry>> {
        return vocabularyDao.getRecentLookups(limit).map { entities ->
            entities.map { it.toVocabularyEntry() }
        }
    }

    /**
     * Search vocabulary history
     */
    fun searchVocabulary(query: String): Flow<List<VocabularyEntry>> {
        return vocabularyDao.searchVocabulary("%$query%").map { entities ->
            entities.map { it.toVocabularyEntry() }
        }
    }

    /**
     * Clear vocabulary history
     */
    suspend fun clearVocabularyHistory() {
        vocabularyDao.deleteAll()
    }

    // === Utility Functions ===

    /**
     * Explain a term
     */
    suspend fun explainTerm(term: String, context: String? = null): String {
        return aiService.explainTerm(term, context)
    }

    /**
     * Translate text
     */
    suspend fun translate(text: String, targetLanguage: String): String {
        return aiService.translate(text, targetLanguage)
    }

    /**
     * Batch generate summaries for multiple chapters
     */
    suspend fun generateSummariesForBook(
        bookId: String,
        chapters: List<Pair<Int, Pair<String, String>>> // (index, title, content)
    ): List<ChapterSummary> {
        return chapters.map { (index, titleAndContent) ->
            val (title, content) = titleAndContent
            generateChapterSummary(bookId, index, title, content)
        }
    }

    /**
     * Generate summaries in background
     */
    suspend fun generateSummariesInBackground(
        bookId: String,
        chapters: List<Pair<Int, Pair<String, String>>>,
        onProgress: (Int, Int) -> Unit // (current, total)
    ) {
        chapters.forEachIndexed { i, (index, titleAndContent) ->
            val (title, content) = titleAndContent
            generateChapterSummary(bookId, index, title, content)
            onProgress(i + 1, chapters.size)
        }
    }
}

// Extension functions for entity to model conversion

private fun ChapterSummaryEntity.toChapterSummary(): ChapterSummary {
    return ChapterSummary(
        bookId = bookId,
        chapterIndex = chapterIndex,
        chapterTitle = chapterTitle,
        summary = summary,
        keyPoints = json.decodeFromString(keyPoints),
        importantQuotes = quotes?.let { json.decodeFromString<List<String>>(it) },
        generatedAt = generateTime,
        aiModel = aiModel ?: "unknown"
    )
}

private fun XRayDataEntity.toXRayData(): XRayData {
    return json.decodeFromString(data)
}

private fun VocabularyEntity.toVocabularyEntry(): VocabularyEntry {
    return VocabularyEntry(
        word = word,
        definition = definition,
        partOfSpeech = partOfSpeech,
        example = example,
        context = context,
        frequency = frequency
    )
}
