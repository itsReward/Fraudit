package com.fraudit.fraudit.dto.risk

import com.fraudit.fraudit.domain.enum.RiskLevel
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class PerformRiskAssessmentRequest(
    val statementId: Long
)

data class FraudRiskAssessmentResponse(
    val id: Long,
    val statementId: Long,
    val companyId: Long,
    val companyName: String,
    val year: Int,
    val zScoreRisk: BigDecimal?,
    val mScoreRisk: BigDecimal?,
    val fScoreRisk: BigDecimal?,
    val financialRatioRisk: BigDecimal?,
    val mlPredictionRisk: BigDecimal?,
    val overallRiskScore: BigDecimal?,
    val riskLevel: RiskLevel?,
    val assessmentSummary: String?,
    val assessedAt: OffsetDateTime,
    val assessedById: UUID?,
    val assessedByUsername: String?,
    val alertCount: Int
)

data class FraudRiskSummaryResponse(
    val id: Long,
    val companyName: String,
    val year: Int,
    val overallRiskScore: BigDecimal?,
    val riskLevel: RiskLevel?,
    val assessedAt: OffsetDateTime
)