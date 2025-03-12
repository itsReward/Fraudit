package com.fraudit.fraudit.repository

import com.fraudit.fraudit.domain.entity.AuditLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface AuditLogRepository : JpaRepository<AuditLog, Long> {
    fun findByUserId(userId: UUID): List<AuditLog>
    fun findByAction(action: String): List<AuditLog>
    fun findByEntityTypeAndEntityId(entityType: String, entityId: String): List<AuditLog>
    fun findByTimestampBetween(startTime: OffsetDateTime, endTime: OffsetDateTime): List<AuditLog>
}