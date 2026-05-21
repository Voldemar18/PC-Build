package com.example.kt_fife.data.network

import com.example.kt_fife.data.local.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import android.util.Log
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = tokenManager.getAccessToken()

        val url = originalRequest.url.toString()
        Log.d("AuthInterceptor", "URL: $url")
        Log.d("AuthInterceptor", "Token exists: ${token != null}")

        val isAuthRequest = url.contains("/api/login") ||
                url.contains("/api/register") ||
                url.contains("/api/auth/refresh")

        val newRequest = if (token != null && token.isNotBlank() && !isAuthRequest) {
            Log.d("AuthInterceptor", "Adding token to request")
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            Log.d("AuthInterceptor", "No token added")
            originalRequest
        }

        val response = chain.proceed(newRequest)
        Log.d("AuthInterceptor", "Response code: ${response.code}")

        return response
    }
}