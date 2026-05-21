package com.example.kt_fife.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kt_fife.data.local.TokenManager
import com.example.kt_fife.data.network.*
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

sealed class AuthUiEvent {
    data class OnRegister(
        val username: String,
        val email: String,
        val password: String,
        val firstName: String? = null,
        val lastName: String? = null,
        val phone: String? = null
    ) : AuthUiEvent()

    data class OnLogin(
        val username: String,
        val password: String
    ) : AuthUiEvent()

    object OnResetError : AuthUiEvent()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun handleEvent(event: AuthUiEvent) {
        when (event) {
            is AuthUiEvent.OnRegister -> register(
                event.username,
                event.email,
                event.password,
                event.firstName,
                event.lastName,
                event.phone
            )
            is AuthUiEvent.OnLogin -> login(event.username, event.password)
            is AuthUiEvent.OnResetError -> resetError()
        }
    }

    private fun register(
        username: String,
        email: String,
        password: String,
        firstName: String?,
        lastName: String?,
        phone: String?
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)

            try {
                val request = UserRegistrationRequest(
                    username = username,
                    email = email,
                    password = password,
                    firstName = firstName,
                    lastName = lastName,
                    phone = phone
                )

                val response = apiService.register(request)

                if (response.isSuccessful && response.body() != null) {
                    val tokens = response.body()!!
                    tokenManager.saveTokens(tokens.accessToken, tokens.refreshToken)
                    _uiState.value = AuthUiState(isSuccess = true)
                } else {
                    val errorBody = response.errorBody()?.string()
                    _uiState.value = AuthUiState(
                        error = errorBody ?: response.message() ?: "Registration failed"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState(
                    error = e.message ?: "Network error occurred"
                )
            }
        }
    }

    private fun login(username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)

            try {
                val request = UserLoginRequest(username, password)
                val response = apiService.login(request)

                Log.d("AuthViewModel", "LOGIN RESPONSE")
                Log.d("AuthViewModel", "Code: ${response.code()}")
                Log.d("AuthViewModel", "Message: ${response.message()}")

                response.headers().names().forEach { name ->
                    Log.d("AuthViewModel", "Header $name: ${response.headers().get(name)}")
                }

                val rawBody = response.body()?.let {
                    val gson = Gson()
                    gson.toJson(it)
                }
                Log.d("AuthViewModel", "Raw body: $rawBody")

                if (response.isSuccessful && response.body() != null) {
                    val tokens = response.body()!!
                    Log.d("AuthViewModel", "Access token: ${tokens.accessToken}")
                    Log.d("AuthViewModel", "Refresh token: ${tokens.refreshToken}")
                    Log.d("AuthViewModel", "Token type: ${tokens.tokenType}")

                    tokenManager.saveTokens(tokens.accessToken, tokens.refreshToken)
                    _uiState.value = AuthUiState(isSuccess = true)
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("AuthViewModel", "Error body: $errorBody")
                    _uiState.value = AuthUiState(
                        error = errorBody ?: response.message() ?: "Invalid credentials"
                    )
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Exception", e)
                _uiState.value = AuthUiState(
                    error = e.message ?: "Network error occurred"
                )
            }
        }
    }

    private fun resetError() {
        _uiState.value = AuthUiState()
    }
}