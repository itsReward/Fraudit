package com.fraudit.fraudit.dto.risk

import com.fraudit.fraudit.domain.enum.AlertSeverity
import java.time.OffsetDateTime
import java.util.UUID

data class RiskAlertResponse(
    val id: Long,
    val assessmentId: Long,
    val companyId: Long,
    val companyName: String,
    val alertType: String,
    val severity: AlertSeverity,
    val message: String,
    val createdAt: OffsetDateTime,
    val isResolved: Boolean,
    val resolvedById: UUID?,
    val resolvedByUsername: String?,
    val resolvedAt: OffsetDateTime?,
    val resolutionNotes: String?
)

data class ResolveAlertRequest(
    val resolutionNotes: String
)