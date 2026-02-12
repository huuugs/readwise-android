package com.readwise.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber

/**
 * HTTP日志拦截器
 * 用于调试网络请求
 */
class HttpLoggingInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.nanoTime()

        Timber.d("Request: ${request.method} ${request.url}")

        val response = chain.proceed(request)
        val endTime = System.nanoTime()

        Timber.d("Response: ${response.code} in ${(endTime - startTime) / 1e6}ms")

        return response
    }
}
