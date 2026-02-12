package com.readwise.ai.di

import com.readwise.ai.service.AIService
import com.readwise.ai.service.AIConfigManager
import com.readwise.ai.service.OpenAIService
import com.readwise.ai.repository.AIRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * AI service dependency injection module
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AIModule {

    @Binds
    @Singleton
    abstract fun bindAIService(impl: OpenAIService): AIService

    @Module
    companion object {

        @Provides
        @Singleton
        fun provideAIRepository(
            aiService: AIService,
            xRayDataDao: com.readwise.core.database.dao.XRayDataDao,
            chapterSummaryDao: com.readwise.core.database.dao.ChapterSummaryDao,
            vocabularyDao: com.readwise.core.database.dao.VocabularyDao,
            bookDao: com.readwise.core.database.dao.BookDao
        ): AIRepository {
            return AIRepository(
                aiService = aiService,
                xRayDataDao = xRayDataDao,
                chapterSummaryDao = chapterSummaryDao,
                vocabularyDao = vocabularyDao,
                bookDao = bookDao
            )
        }
    }
}
