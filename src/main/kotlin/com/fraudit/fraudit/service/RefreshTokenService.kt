package com.fraudit.fraudit.service

import java.util.UUID

interface RefreshTokenService {
    /**
     * Create a new refresh token for a user
     */
    fun createRefreshToken(userId: UUID): String

    /**
     * Verify a refresh token
     * @return the associated user ID if the token is valid, null otherwise
     */
    fun verifyRefreshToken(token: String): UUID?

    /**
     * Delete a refresh token
     */
    fun deleteRefreshToken(token: String)

    /**
     * Delete all refresh tokens for a user
     */
    fun deleteAllUserRefreshTokens(userId: UUID)
}