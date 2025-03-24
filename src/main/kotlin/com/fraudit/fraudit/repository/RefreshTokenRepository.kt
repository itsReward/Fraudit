package com.fraudit.fraudit.repository

import com.fraudit.fraudit.domain.entity.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.Optional
import java.util.UUID

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByToken(token: String): Optional<RefreshToken>

    fun findAllByUserId(userId: UUID): List<RefreshToken>

    fun deleteByToken(token: String)

    fun deleteAllByUserId(userId: UUID)

    fun deleteAllByExpiryDateBefore(expiryDate: Instant)
}