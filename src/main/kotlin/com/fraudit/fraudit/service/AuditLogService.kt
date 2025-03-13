package com.fraudit.fraudit.service

import com.fraudit.fraudit.domain.entity.AuditLog
import java.time.OffsetDateTime
import java.util.UUID

interface AuditLogService {
    fun findAll(): List<AuditLog>
    fun findById(id: Long): AuditLog
    fun findByUserId(userId: UUID): List<AuditLog>
    fun findByAction(action: String): List<AuditLog>
    fun findByEntityTypeAndEntityId(entityType: String, entityId: String): List<AuditLog>
    fun findByDateRange(startDate: OffsetDateTime, endDate: OffsetDateTime): List<AuditLog>
    fun logEvent(userId: UUID?, action: String, entityType: String, entityId: String, details: String? = null, ipAddress: String? = null): AuditLog
}