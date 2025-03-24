package com.fraudit.fraudit.domain.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "refresh_tokens")
data class RefreshToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    val token: String,

    @Column(nullable = false)
    val userId: UUID,

    @Column(nullable = false)
    val expiryDate: Instant,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)