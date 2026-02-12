package com.readwise.ai.service

import com.readwise.ai.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OpenAI API implementation
 * Supports GPT-3.5, GPT-4, and compatible APIs
 */
@Singleton
class OpenAIService @Inject constructor(
    private val configManager: AIConfigManager
) : AIService {

    private val client = OkHttpClient()
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var currentConfig: AIRequestConfig? = null

    override fun getProvider(): AIProvider = AIProvider.OPENAI

    override fun getModel(): String = currentConfig?.model ?: "gpt-3.5-turbo"

    override suspend fun chat(
        messages: List<ChatMessage>,
        config: AIRequestConfig?
    ): AIResponse {
        val effectiveConfig = config ?: getDefaultConfig()
        currentConfig = effectiveConfig

        val request = ChatCompletionRequest(
            model = effectiveConfig.model,
            messages = messages.map { ChatMessageDto(it.role.name.lowercase(), it.content) },
            temperature = effectiveConfig.temperature,
            max_tokens = effectiveConfig.maxTokens
        )

        val response = makeRequest(effectiveConfig, "chat/completions", request)
        val completion: ChatCompletionResponse = json.decodeFromString(response)

        return AIResponse(
            content = completion.choices.first().message.content,
            model = completion.model,
            tokensUsed = completion.usage?.total_tokens,
            finishReason = completion.choices.first().finish_reason
        )
    }

    override suspend fun summarizeChapter(
        chapterTitle: String,
        chapterContent: String,
        config: AIRequestConfig?
    ): ChapterSummary {
        val systemPrompt = """You are an expert literary analyst. Generate a comprehensive summary of the chapter.
Include key plot points, character development, and important themes.
Respond in JSON format with: summary (2-3 paragraphs), keyPoints (array of 5-7 bullet points), importantQuotes (array of 3-5 significant quotes with page references if available)."""

        val userPrompt = """Chapter Title: $chapterTitle

Chapter Content:
${chapterContent.take(8000)}

Please analyze this chapter and provide summary, key points, and important quotes."""

        val messages = listOf(
            ChatMessage(MessageRole.SYSTEM, systemPrompt),
            ChatMessage(MessageRole.USER, userPrompt)
        )

        val response = chat(messages, config)

        // Parse JSON response
        val summaryData = parseSummaryResponse(response.content)

        return ChapterSummary(
            bookId = "", // Will be set by caller
            chapterIndex = 0, // Will be set by caller
            chapterTitle = chapterTitle,
            summary = summaryData["summary"] ?: response.content,
            keyPoints = summaryData["keyPoints"]?.split("\n") ?: emptyList(),
            importantQuotes = summaryData["quotes"]?.split("\n"),
            aiModel = response.model
        )
    }

    override suspend fun analyzeXRay(
        bookTitle: String,
        chapters: List<Pair<Int, String>>,
        config: AIRequestConfig?
    ): XRayData {
        val entities = mutableListOf<XRayEntity>()

        // Process chapters in batches to avoid token limits
        val batchSize = 5
        chapters.chunked(batchSize).forEach { batch ->
            val batchText = batch.joinToString("\n\n") { (index, content) ->
                "Chapter ${index + 1}:\n${content.take(2000)}"
            }

            val systemPrompt = """You are an expert literary analyst. Extract entities from this book excerpt.
Identify: characters (with brief descriptions), locations, important terms, organizations.
Respond in JSON format: array of entities with: id, type (CHARACTER/LOCATION/TERM/ORGANIZATION), name, description."""

            val userPrompt = """Book: $bookTitle

Excerpt:
$batchText

Extract all significant entities from this excerpt."""

            val messages = listOf(
                ChatMessage(MessageRole.SYSTEM, systemPrompt),
                ChatMessage(MessageRole.USER, userPrompt)
            )

            val response = chat(messages, config)
            val extractedEntities = parseXRayEntities(response.content, bookTitle)
            entities.addAll(extractedEntities)
        }

        return XRayData(
            bookId = bookTitle, // Will be updated with real ID
            entities = entities.distinctBy { it.name },
            isComplete = chapters.size < 20 // Assume complete for shorter books
        )
    }

    override suspend fun explainTerm(
        term: String,
        context: String?,
        config: AIRequestConfig?
    ): String {
        val contextText = if (context != null) {
            "Context: $context\n\n"
        } else {
            ""
        }

        val prompt = """${contextText}Explain the term or concept: "$term"
Provide a clear, concise explanation suitable for a general reader.
Include any relevant background, significance, or related concepts."""

        val messages = listOf(
            ChatMessage(MessageRole.SYSTEM, "You are a knowledgeable educator."),
            ChatMessage(MessageRole.USER, prompt)
        )

        return chat(messages, config).content
    }

    override suspend fun translate(
        text: String,
        targetLanguage: String,
        config: AIRequestConfig?
    ): String {
        val prompt = """Translate the following text to $targetLanguage.
Preserve the original tone and meaning. Only provide the translation, no explanations.

Text:
$text"""

        val messages = listOf(
            ChatMessage(MessageRole.USER, prompt)
        )

        return chat(messages, config).content
    }

    override suspend fun lookupVocabulary(
        word: String,
        context: String?,
        config: AIRequestConfig?
    ): VocabularyEntry {
        val contextText = if (context != null) {
            "\nContext: $context"
        } else {
            ""
        }

        val prompt = """Provide comprehensive information for the word: "$word"$contextText

Respond in JSON format with:
- definition: clear, concise definition
- partOfSpeech: grammatical category (noun, verb, etc.)
- example: example sentence
- synonyms: array of 3-5 related words"""

        val messages = listOf(
            ChatMessage(MessageRole.SYSTEM, "You are a comprehensive dictionary."),
            ChatMessage(MessageRole.USER, prompt)
        )

        val response = chat(messages, config)
        val vocabData = parseVocabularyResponse(response.content)

        return VocabularyEntry(
            word = word,
            definition = vocabData["definition"] ?: "Definition not available",
            partOfSpeech = vocabData["partOfSpeech"],
            example = vocabData["example"],
            synonyms = vocabData["synonyms"]?.split(",")?.map { it.trim() },
            context = context
        )
    }

    override suspend fun chatStream(
        messages: List<ChatMessage>,
        onChunk: (String) -> Unit,
        config: AIRequestConfig?
    ): AIResponse {
        val effectiveConfig = config ?: getDefaultConfig()
        currentConfig = effectiveConfig

        val request = ChatCompletionRequest(
            model = effectiveConfig.model,
            messages = messages.map { ChatMessageDto(it.role.name.lowercase(), it.content) },
            temperature = effectiveConfig.temperature,
            max_tokens = effectiveConfig.maxTokens,
            stream = true
        )

        val baseUrl = effectiveConfig.provider.defaultBaseUrl ?: "https://api.openai.com/v1"
        val url = "$baseUrl/chat/completions"

        val requestBody = json.encodeToString(request)
            .toRequestBody("application/json".toMediaType())

        val apiRequest = Request.Builder()
            .url(url)
            .post(requestBody)
            .header("Authorization", "Bearer ${getApiKey()}")
            .header("Content-Type", "application/json")
            .build()

        val fullResponse = StringBuilder()
        client.newCall(apiRequest).execute().use { response ->
            response.body?.source()?.let { source ->
                while (!source.exhausted()) {
                    val line = source.readUtf8Line()
                    if (line?.startsWith("data: ") == true) {
                        val data = line.substring(6)
                        if (data == "[DONE]") break

                        try {
                            val chunk: ChatCompletionChunk = json.decodeFromString(data)
                            val content = chunk.choices.firstOrNull()?.delta?.content
                            if (content != null) {
                                fullResponse.append(content)
                                onChunk(content)
                            }
                        } catch (e: Exception) {
                            // Skip invalid JSON
                        }
                    }
                }
            }
        }

        return AIResponse(
            content = fullResponse.toString(),
            model = effectiveConfig.model
        )
    }

    override fun isAvailable(): Boolean {
        return try {
            getApiKey().isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    override fun updateConfig(config: AIRequestConfig) {
        currentConfig = config
    }

    // Private helper methods

    private fun getDefaultConfig(): AIRequestConfig {
        return AIRequestConfig(
            provider = AIProvider.OPENAI,
            model = "gpt-3.5-turbo",
            temperature = 0.7f,
            maxTokens = 2000
        )
    }

    private fun getApiKey(): String {
        return configManager.getApiKey(AIProvider.OPENAI) ?: ""
    }

    private suspend fun makeRequest(
        config: AIRequestConfig,
        endpoint: String,
        requestBody: Any
    ): String {
        val baseUrl = config.provider.defaultBaseUrl ?: "https://api.openai.com/v1"
        val url = "$baseUrl/$endpoint"

        val bodyJson = json.encodeToString(requestBody)
        val requestBody = bodyJson.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .header("Authorization", "Bearer ${getApiKey()}")
            .header("Content-Type", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("API error: ${response.code} - ${response.message}")
            }
            return response.body?.string() ?: throw IOException("Empty response")
        }
    }

    private fun parseSummaryResponse(content: String): Map<String, String> {
        // Try to extract JSON from markdown code block or plain JSON
        val jsonPattern = Regex("""```json\s*([\s\S]*?)\s*```""")
        val match = jsonPattern.find(content)

        val jsonStr = match?.groupValues?.get(1) ?: content

        return try {
            json.decodeFromString<Map<String, String>>(jsonStr)
        } catch (e: Exception) {
            mapOf("summary" to content)
        }
    }

    private fun parseXRayEntities(content: String, bookId: String): List<XRayEntity> {
        return try {
            val jsonStr = content.removePrefix("```json").removeSuffix("```").trim()
            val entities: List<Map<String, Any>> = json.decodeFromString(jsonStr)

            entities.mapIndexed { index, entity ->
                XRayEntity(
                    id = "${bookId}_${entity["name"]}_${index}",
                    bookId = bookId,
                    type = XRayEntityType.valueOf(entity["type"] as? String ?: "OTHER"),
                    name = entity["name"] as? String ?: "Unknown",
                    description = entity["description"] as? String ?: "",
                    firstMention = 0,
                    mentions = emptyList()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseVocabularyResponse(content: String): Map<String, String> {
        return try {
            val jsonStr = content.removePrefix("```json").removeSuffix("```").trim()
            json.decodeFromString(jsonStr)
        } catch (e: Exception) {
            mapOf("definition" to content)
        }
    }
}

// Data Transfer Objects for API serialization

@Serializable
private data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessageDto>,
    val temperature: Float? = null,
    val max_tokens: Int? = null,
    val stream: Boolean? = null
)

@Serializable
private data class ChatMessageDto(
    val role: String,
    val content: String
)

@Serializable
private data class ChatCompletionResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage? = null
)

@Serializable
private data class Choice(
    val index: Int,
    val message: ChatMessageDto,
    val finish_reason: String
)

@Serializable
private data class Usage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

@Serializable
private data class ChatCompletionChunk(
    val id: String,
    val choices: List<ChunkChoice>
)

@Serializable
private data class ChunkChoice(
    val delta: Delta
)

@Serializable
private data class Delta(
    val content: String? = null
)
