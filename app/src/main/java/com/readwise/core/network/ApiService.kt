package com.readwise.core.network

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 网络API服务接口
 */
interface ApiService {

    @GET("search")
    suspend fun searchBooks(@Query("q") query: String): SearchResultResponse

    // 其他 API 接口...
}

data class SearchResultResponse(
    val results: List<BookItem> = emptyList()
)

data class BookItem(
    val id: String,
    val title: String,
    val author: String,
    val coverUrl: String?
)
