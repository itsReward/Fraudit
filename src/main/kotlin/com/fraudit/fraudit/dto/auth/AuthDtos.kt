package com.fraudit.fraudit.dto.auth

import com.fraudit.fraudit.domain.enum.UserRole
import java.util.UUID

data class LoginRequest(
    val username: String,
    val password: String,
    val rememberMe: Boolean = false
)

data class LoginResponse(
    val userId: UUID,
    val username: String,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val role: String,
    val token: String,
    val expiresAt: Long
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val firstName: String?,
    val lastName: String?,
    val role: UserRole = UserRole.ANALYST
)

data class PasswordChangeRequest(
    val currentPassword: String,
    val newPassword: String,
    val confirmNewPassword: String
)

data class TokenRefreshRequest(
    val refreshToken: String
)

data class TokenRefreshResponse(
    val token: String,
    val refreshToken: String,
    val expiresAt: Long
)