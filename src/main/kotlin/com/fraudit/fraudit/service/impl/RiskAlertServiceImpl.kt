package com.fraudit.fraudit.service.impl

import com.fraudit.fraudit.domain.entity.RiskAlert
import com.fraudit.fraudit.domain.entity.User
import com.fraudit.fraudit.domain.enum.AlertSeverity
import com.fraudit.fraudit.repository.RiskAlertRepository
import com.fraudit.fraudit.repository.UserRepository
import com.fraudit.fraudit.service.AuditLogService
import com.fraudit.fraudit.service.RiskAlertService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID
import jakarta.persistence.EntityNotFoundException

@Service
class RiskAlertServiceImpl(
    private val riskAlertRepository: RiskAlertRepository,
    private val userRepository: UserRepository,
    private val auditLogService: AuditLogService
) : RiskAlertService {

    override fun findAll(): List<RiskAlert> = riskAlertRepository.findAll()

    override fun findById(id: Long): RiskAlert = riskAlertRepository.findById(id)
        .orElseThrow { EntityNotFoundException("Risk alert not found with id: $id") }

    override fun findByAssessmentId(assessmentId: Long): List<RiskAlert> =
        riskAlertRepository.findByAssessmentId(assessmentId)

    override fun findBySeverity(severity: AlertSeverity): List<RiskAlert> =
        riskAlertRepository.findBySeverity(severity)

    override fun findUnresolved(): List<RiskAlert> = riskAlertRepository.findByIsResolved(false)

    override fun findResolved(): List<RiskAlert> = riskAlertRepository.findByIsResolved(true)

    override fun findByAlertType(alertType: String): List<RiskAlert> =
        riskAlertRepository.findByAlertType(alertType)

    @Transactional
    override fun createAlert(alert: RiskAlert): RiskAlert {
        val savedAlert = riskAlertRepository.save(alert)

        auditLogService.logEvent(
            userId = null,
            action = "CREATE",
            entityType = "RISK_ALERT",
            entityId = savedAlert.id.toString(),
            details = "Created risk alert: ${alert.alertType} with severity ${alert.severity}"
        )

        return savedAlert
    }

    @Transactional
    override fun resolveAlert(id: Long, userId: UUID, resolutionNotes: String): RiskAlert {
        val alert = findById(id)

        if (alert.isResolved) {
            throw IllegalStateException("Alert is already resolved")
        }

        val user = userRepository.findById(userId)
            .orElseThrow { EntityNotFoundException("User not found with id: $userId") }

        // Update to resolved state
        val resolvedAlert = alert.copy(
            isResolved = true,
            resolvedBy = user,
            resolvedAt = OffsetDateTime.now(),
            resolutionNotes = resolutionNotes
        )

        val savedAlert = riskAlertRepository.save(resolvedAlert)

        auditLogService.logEvent(
            userId = userId,
            action = "RESOLVE",
            entityType = "RISK_ALERT",
            entityId = savedAlert.id.toString(),
            details = "Resolved risk alert: ${alert.alertType} with notes: $resolutionNotes"
        )

        return savedAlert
    }
}