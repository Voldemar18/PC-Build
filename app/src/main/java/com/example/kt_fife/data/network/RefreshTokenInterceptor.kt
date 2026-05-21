package com.example.kt_fife.data.network

import android.util.Log
import com.example.kt_fife.data.local.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class RefreshTokenInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
    private val apiService: ApiService
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        var response = chain.proceed(originalRequest)

        if (response.code == 401 &&
            !originalRequest.url.toString().contains("/api/login") &&
            !originalRequest.url.toString().contains("/api/register") &&
            !originalRequest.url.toString().contains("/api/auth/refresh")) {

            synchronized(this) {
                val newResponse = tryRefreshToken(chain, originalRequest)
                if (newResponse != null) {
                    response.close()
                    return newResponse
                }
            }
        }

        return response
    }

    private fun tryRefreshToken(
        chain: Interceptor.Chain,
        originalRequest: okhttp3.Request
    ): Response? {
        return try {
            val refreshToken = tokenManager.getRefreshToken()
            if (refreshToken == null) {
                Log.e("RefreshTokenInterceptor", "No refresh token available")
                return null
            }

            Log.d("RefreshTokenInterceptor", "Attempting to refresh token")

            val response = runBlocking {
                apiService.refreshToken(RefreshTokenRequest(refreshToken))
            }

            if (response.isSuccessful && response.body() != null) {
                val newTokens = response.body()!!
                Log.d("RefreshTokenInterceptor", "Token refreshed successfully")

                tokenManager.saveTokens(newTokens.accessToken, newTokens.refreshToken)

                val newRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer ${newTokens.accessToken}")
                    .build()

                return chain.proceed(newRequest)
            } else {
                Log.e("RefreshTokenInterceptor", "Failed to refresh token: ${response.code()}")
                tokenManager.clearTokens()
                return null
            }
        } catch (e: Exception) {
            Log.e("RefreshTokenInterceptor", "Error refreshing token", e)
            tokenManager.clearTokens()
            null
        }
    }
}