package com.fraudit.fraudit.service.impl

import com.fraudit.fraudit.domain.entity.AuditLog
import com.fraudit.fraudit.domain.entity.User
import com.fraudit.fraudit.repository.AuditLogRepository
import com.fraudit.fraudit.repository.UserRepository
import com.fraudit.fraudit.service.AuditLogService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID
import jakarta.persistence.EntityNotFoundException

@Service
class AuditLogServiceImpl(
    private val auditLogRepository: AuditLogRepository,
    private val userRepository: UserRepository
) : AuditLogService {

    override fun findAll(): List<AuditLog> = auditLogRepository.findAll()

    override fun findById(id: Long): AuditLog = auditLogRepository.findById(id)
        .orElseThrow { EntityNotFoundException("Audit log entry not found with id: $id") }

    override fun findByUserId(userId: UUID): List<AuditLog> = auditLogRepository.findByUserId(userId)

    override fun findByAction(action: String): List<AuditLog> = auditLogRepository.findByAction(action)

    override fun findByEntityTypeAndEntityId(entityType: String, entityId: String): List<AuditLog> =
        auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId)

    override fun findByDateRange(startDate: OffsetDateTime, endDate: OffsetDateTime): List<AuditLog> =
        auditLogRepository.findByTimestampBetween(startDate, endDate)

    @Transactional
    override fun logEvent(userId: UUID?, action: String, entityType: String, entityId: String, details: String?, ipAddress: String?): AuditLog {
        val user: User? = userId?.let {
            userRepository.findById(it).orElse(null)
        }

        val auditLog = AuditLog(
            id = null,
            user = user,
            action = action,
            entityType = entityType,
            entityId = entityId,
            details = details,
            ipAddress = ipAddress,
            timestamp = OffsetDateTime.now()
        )

        return auditLogRepository.save(auditLog)
    }
}
