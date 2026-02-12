package com.readwise.core.di

import android.content.Context
import androidx.room.Room
import com.readwise.core.database.AppDatabase
import com.readwise.core.database.dao.*
import com.readwise.core.network.ApiService
import com.readwise.core.network.interceptor.HttpLoggingInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * 应用模块 - 提供全局依赖
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // 开发阶段使用，生产环境应实现 Migration
            .build()
    }

    @Provides
    fun provideBookDao(database: AppDatabase): BookDao = database.bookDao()

    @Provides
    fun provideBookmarkDao(database: AppDatabase): BookmarkDao = database.bookmarkDao()

    @Provides
    fun provideReadingPositionDao(database: AppDatabase): ReadingPositionDao = database.readingPositionDao()

    @Provides
    fun provideDictionaryHistoryDao(database: AppDatabase): DictionaryHistoryDao = database.dictionaryHistoryDao()

    @Provides
    fun provideVocabularyDao(database: AppDatabase): VocabularyDao = database.vocabularyDao()

    @Provides
    fun provideXRayDataDao(database: AppDatabase): XRayDataDao = database.xRayDataDao()

    @Provides
    fun provideAIConfigDao(database: AppDatabase): AIConfigDao = database.aiConfigDao()

    @Provides
    fun provideChapterSummaryDao(database: AppDatabase): ChapterSummaryDao = database.chapterSummaryDao()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor())
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.example.com/") // 占位 URL
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    fun provideApiService(retrofit: Retrofit): ApiService = retrofit.create(ApiService::class.java)
}
