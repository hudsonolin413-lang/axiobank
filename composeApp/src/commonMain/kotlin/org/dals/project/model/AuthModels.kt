package org.dals.project.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String,
    val confirmPassword: String,
    val fullName: String,
    val email: String,
    val phoneNumber: String
)

@Serializable
data class AuthResponse(
    val success: Boolean,
    val message: String,
    val user: User? = null,
    val token: String? = null
)

@Serializable
enum class AuthState {
    LOGGED_OUT, LOGGED_IN, LOADING, LOCKED
}