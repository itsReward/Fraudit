package com.fraudit.fraudit.repository

import com.fraudit.fraudit.domain.entity.AuditLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface AuditLogRepository : JpaRepository<AuditLog, Long> {
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
    fun findByTimestampBetween(startTime: OffsetDateTime, endTime: OffsetDateTime): List<AuditLog>

    /**
     * Find audit logs by user ID within a date range
     */
    @Query("SELECT a FROM AuditLog a WHERE a.user.id = :userId AND a.timestamp BETWEEN :startTime AND :endTime")
    fun findByUserIdAndTimestampBetween(userId: UUID, startTime: OffsetDateTime, endTime: OffsetDateTime): List<AuditLog>

    /**
     * Find the most recent audit logs
     */
    fun findTop10ByOrderByTimestampDesc(): List<AuditLog>

    /**
     * Count audit logs by action
     */
    @Query("SELECT a.action, COUNT(a) FROM AuditLog a GROUP BY a.action")
    fun countByAction(): List<Array<Any>>

    /**
     * Find audit logs related to a specific entity
     */
    @Query("SELECT a FROM AuditLog a WHERE a.entityType = :entityType ORDER BY a.timestamp DESC")
    fun findByEntityTypeOrderByTimestampDesc(entityType: String, pageable: Pageable): Page<AuditLog>

    /**
     * Find audit logs by user ID and entity type
     */
    @Query("SELECT a FROM AuditLog a WHERE a.user.id = :userId AND a.entityType = :entityType")
    fun findByUserIdAndEntityType(userId: UUID, entityType: String): List<AuditLog>
}