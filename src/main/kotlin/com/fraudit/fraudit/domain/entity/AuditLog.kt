package com.fraudit.fraudit.domain.entity

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Entity class for tracking system actions for audit purposes
 */
@Entity
@Table(name = "audit_log")
data class AuditLog(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_log_sequence")
    @SequenceGenerator(name = "audit_log_sequence", sequenceName = "audit_log_log_id_seq", allocationSize = 1)
    @Column(name = "log_id")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User? = null,

    @Column(name = "action", nullable = false)
    val action: String,

    @Column(name = "entity_type", nullable = false)
    val entityType: String,

    @Column(name = "entity_id", nullable = false)
    val entityId: String,

    @Column(name = "details")
    val details: String? = null,

    @Column(name = "ip_address")
    val ipAddress: String? = null,

    @Column(name = "timestamp", updatable = false)
    val timestamp: OffsetDateTime = OffsetDateTime.now()
)