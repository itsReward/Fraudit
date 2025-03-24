package com.fraudit.fraudit.service

import com.fraudit.fraudit.domain.entity.User
import io.jsonwebtoken.Claims
import java.util.Date
import java.util.UUID

interface JwtTokenService {
    /**
     * Generate a JWT access token for a user
     */
    fun generateAccessToken(user: User): String

    /**
     * Generate a JWT refresh token for a user
     */
    fun generateRefreshToken(user: User): String

    /**
     * Validate a JWT token
     * @return true if the token is valid, false otherwise
     */
    fun validateToken(token: String): Boolean

    /**
     * Extract the user ID from a JWT token
     * @return the user ID, or null if the token is invalid
     */
    fun getUserIdFromToken(token: String): UUID?

    /**
     * Extract the expiration date from a JWT token
     * @return the expiration date, or null if the token is invalid
     */
    fun getExpirationFromToken(token: String): Date?

    /**
     * Extract all claims from a JWT token
     * @return the claims, or null if the token is invalid
     */
    fun getAllClaimsFromToken(token: String): Claims?
}