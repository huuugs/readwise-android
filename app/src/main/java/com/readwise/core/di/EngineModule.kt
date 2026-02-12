package com.readwise.core.di

import com.readwise.engine.common.UnifiedEngine
import com.readwise.engine.common.UnifiedEngineImpl
import com.readwise.engine.epub.EpubEngine
import com.readwise.engine.epub.EpubEngineImpl
import com.readwise.engine.pdf.PdfEngine
import com.readwise.engine.pdf.PdfEngineImpl
import com.readwise.engine.txt.TxtEngine
import com.readwise.engine.txt.TxtEngineImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 阅读引擎模块
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class EngineModule {

    @Binds
    @Singleton
    abstract fun bindPdfEngine(impl: PdfEngineImpl): PdfEngine

    @Binds
    @Singleton
    abstract fun bindEpubEngine(impl: EpubEngineImpl): EpubEngine

    @Binds
    @Singleton
    abstract fun bindTxtEngine(impl: TxtEngineImpl): TxtEngine

    @Binds
    @Singleton
    abstract fun bindUnifiedEngine(impl: UnifiedEngineImpl): UnifiedEngine
}
