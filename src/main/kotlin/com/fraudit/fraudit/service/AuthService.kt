package com.fraudit.fraudit.service

import com.fraudit.fraudit.domain.entity.User
import com.fraudit.fraudit.dto.auth.*
import java.util.*

interface AuthService {
    /**
     * Authenticate a user with username/email and password
     * @param loginRequest The login request containing credentials
     * @return Login response with user details and access token
     */
    fun authenticate(loginRequest: LoginRequest): LoginResponse

    /**
     * Register a new user
     * @param registerRequest The registration details
     * @return The created user
     */
    fun register(registerRequest: RegisterRequest): User

    /**
     * Refresh an authentication token using a refresh token
     * @param tokenRefreshRequest The refresh request containing the refresh token
     * @return New access and refresh tokens
     */
    fun refreshToken(tokenRefreshRequest: TokenRefreshRequest): TokenRefreshResponse

    /**
     * Log out a user by invalidating their refresh tokens
     * @param userId The ID of the user to log out
     */
    fun logout(userId: UUID)

    /**
     * Change a user's password
     * @param userId The ID of the user
     * @param currentPassword The current password
     * @param newPassword The new password
     * @return true if password was changed successfully, false otherwise
     */
    fun changePassword(userId: UUID, currentPassword: String, newPassword: String): Boolean

    /**
     * Validate an authentication token
     * @param token The JWT token to validate
     * @return true if the token is valid, false otherwise
     */
    fun validateToken(token: String): Boolean
}