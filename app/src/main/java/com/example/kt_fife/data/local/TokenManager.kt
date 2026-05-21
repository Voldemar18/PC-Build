package com.example.kt_fife.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    private val _isLoggedIn = MutableStateFlow(prefs.contains("access_token"))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    fun saveTokens(accessToken: String, refreshToken: String) {
        Log.d("TokenManager", "Saving tokens")
        Log.d("TokenManager", "Access token: ${accessToken.take(50)}...")
        prefs.edit()
            .putString("access_token", accessToken)
            .putString("refresh_token", refreshToken)
            .apply()
        _isLoggedIn.value = true
    }

    fun getAccessToken(): String? {
        val token = prefs.getString("access_token", null)
        Log.d("TokenManager", "Getting token: ${if (token != null) "exists (${token.length} chars)" else "null"}")
        return token
    }

    fun getRefreshToken(): String? = prefs.getString("refresh_token", null)

    fun clearTokens() {
        prefs.edit()
            .remove("access_token")
            .remove("refresh_token")
            .apply()
        _isLoggedIn.value = false
    }
}