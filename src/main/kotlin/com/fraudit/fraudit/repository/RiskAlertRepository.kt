package com.fraudit.fraudit.repository

import com.fraudit.fraudit.domain.entity.RiskAlert
import com.fraudit.fraudit.domain.enum.AlertSeverity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface RiskAlertRepository : JpaRepository<RiskAlert, Long> {
    fun findByAssessmentId(assessmentId: Long): List<RiskAlert>
    fun findBySeverity(severity: AlertSeverity): List<RiskAlert>
    fun findByIsResolved(isResolved: Boolean): List<RiskAlert>
    fun findByResolvedById(userId: UUID): List<RiskAlert>
    fun findByAlertType(alertType: String): List<RiskAlert>
}