package com.example.kt_fife.data.network

data class UserRegistrationRequest(
    val username: String,
    val email: String,
    val password: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val phone: String? = null
)

data class UserLoginRequest(
    val username: String,
    val password: String
)

data class RefreshTokenRequest(
    val refreshToken: String
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Int = 0,
    val user: UserResponse? = null
)

data class UserResponse(
    val id: Long,
    val username: String,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val phone: String?,
    val avatar: String?,
    val role: String,
    val isActive: Boolean,
    val emailVerified: Boolean,
    val dateJoined: String,
    val lastActivity: String?
)