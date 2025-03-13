package com.fraudit.fraudit.service

import com.fraudit.fraudit.domain.entity.RiskAlert
import com.fraudit.fraudit.domain.enum.AlertSeverity
import java.util.UUID

interface RiskAlertService {
    fun findAll(): List<RiskAlert>
    fun findById(id: Long): RiskAlert
    fun findByAssessmentId(assessmentId: Long): List<RiskAlert>
    fun findBySeverity(severity: AlertSeverity): List<RiskAlert>
    fun findUnresolved(): List<RiskAlert>
    fun findResolved(): List<RiskAlert>
    fun findByAlertType(alertType: String): List<RiskAlert>
    fun createAlert(alert: RiskAlert): RiskAlert
    fun resolveAlert(id: Long, userId: UUID, resolutionNotes: String): RiskAlert
}