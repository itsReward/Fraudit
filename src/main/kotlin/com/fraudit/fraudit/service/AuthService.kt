package com.fraudit.fraudit.service

import com.fraudit.fraudit.domain.entity.User
import com.fraudit.fraudit.dto.auth.*
import java.util.*

interface AuthService {
    fun authenticate(loginRequest: LoginRequest): LoginResponse
    fun register(registerRequest: RegisterRequest): User
    fun refreshToken(tokenRefreshRequest: TokenRefreshRequest): TokenRefreshResponse
    fun logout(userId: UUID)
    fun changePassword(userId: UUID, currentPassword: String, newPassword: String): Boolean
}
