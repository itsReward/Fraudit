package com.fraudit.fraudit.service.impl

import com.fraudit.fraudit.domain.entity.RefreshToken
import com.fraudit.fraudit.repository.RefreshTokenRepository
import com.fraudit.fraudit.service.RefreshTokenService
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class RefreshTokenServiceImpl(
    private val refreshTokenRepository: RefreshTokenRepository,

    @Value("\${jwt.refresh-expiration:604800000}") // 7 days in milliseconds
    private val refreshTokenDurationMs: Long
) : RefreshTokenService {

    @Transactional
    override fun createRefreshToken(userId: UUID): String {
        // Generate a secure random token
        val token = UUID.randomUUID().toString()

        // Calculate expiry date
        val expiryDate = Instant.now().plusMillis(refreshTokenDurationMs)

        // Create refresh token entity
        val refreshToken = RefreshToken(
            token = token,
            userId = userId,
            expiryDate = expiryDate
        )

        // Save to database
        refreshTokenRepository.save(refreshToken)

        return token
    }

    @Transactional(readOnly = true)
    override fun verifyRefreshToken(token: String): UUID? {
        val refreshTokenOpt = refreshTokenRepository.findByToken(token)

        // Check if token exists and is not expired
        if (refreshTokenOpt.isPresent) {
            val refreshToken = refreshTokenOpt.get()

            if (refreshToken.expiryDate.isAfter(Instant.now())) {
                return refreshToken.userId
            }

            // Token is expired, delete it
            refreshTokenRepository.deleteByToken(token)
        }

        return null
    }

    @Transactional
    override fun deleteRefreshToken(token: String) {
        refreshTokenRepository.deleteByToken(token)
    }

    @Transactional
    override fun deleteAllUserRefreshTokens(userId: UUID) {
        refreshTokenRepository.deleteAllByUserId(userId)
    }

    /**
     * Scheduled task to clean up expired refresh tokens
     * Runs every day at midnight
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    fun cleanupExpiredTokens() {
        refreshTokenRepository.deleteAllByExpiryDateBefore(Instant.now())
    }
}