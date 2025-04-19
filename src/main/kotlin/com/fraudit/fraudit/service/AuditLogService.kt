package com.fraudit.fraudit.service

import com.fraudit.fraudit.domain.entity.AuditLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.OffsetDateTime
import java.util.UUID

interface AuditLogService {
    /**
     * Find all audit logs
     */
    fun findAll(): List<AuditLog>

    /**
     * Find all audit logs with pagination
     */
    fun findAll(pageable: Pageable): Page<AuditLog>

    /**
     * Find audit log by ID
     */
    fun findById(id: Long): AuditLog

    /**
     * Find audit logs by user ID
     */
    fun findByUserId(userId: UUID): List<AuditLog>

    /**
     * Find audit logs by user ID with pagination
     */
    fun findByUserId(userId: UUID, pageable: Pageable): Page<AuditLog>

    /**
     * Find audit logs by action
     */
    fun findByAction(action: String): List<AuditLog>

    /**
     * Find audit logs by entity type and entity ID
     */
    fun findByEntityTypeAndEntityId(entityType: String, entityId: String): List<AuditLog>

    /**
     * Find audit logs within a date range
     */
    fun findByDateRange(startDate: OffsetDateTime, endDate: OffsetDateTime): List<AuditLog>

    /**
     * Find audit logs by user ID within a date range
     */
    fun findByUserIdAndDateRange(userId: UUID, startDate: OffsetDateTime, endDate: OffsetDateTime): List<AuditLog>

    /**
     * Log an event
     * @param userId User ID (can be null for system events)
     * @param action Action performed
     * @param entityType Type of entity affected
     * @param entityId ID of the entity affected
     * @param details Additional details about the event
     * @param ipAddress IP address of the user (optional)
     * @return The created audit log
     */
    fun logEvent(
        userId: UUID?,
        action: String,
        entityType: String,
        entityId: String,
        details: String? = null,
        ipAddress: String? = null
    ): AuditLog
}