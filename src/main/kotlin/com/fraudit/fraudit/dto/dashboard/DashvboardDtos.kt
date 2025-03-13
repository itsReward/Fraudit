package com.fraudit.fraudit.dto.dashboard

import java.math.BigDecimal
import java.time.OffsetDateTime

data class FraudRiskStatsResponse(
    val totalAssessments: Int,
    val highRiskCount: Int,
    val mediumRiskCount: Int,
    val lowRiskCount: Int,
    val averageRiskScore: BigDecimal,
    val unresolvedAlerts: Int
)

data class CompanyRiskSummaryResponse(
    val companyId: Long,
    val companyName: String,
    val stockCode: String,
    val sector: String,
    val riskLevel: String,
    val riskScore: BigDecimal,
    val assessmentDate: OffsetDateTime
)

data class FraudIndicatorsDistributionResponse(
    val zScore: Map<String, Int>,
    val mScore: Map<String, Int>,
    val fScore: Map<String, Int>,
    val financialRatio: Map<String, Int>,
    val mlPrediction: Map<String, Int>
)

data class RecentRiskAlertResponse(
    val alertId: Long,
    val companyName: String,
    val alertType: String,
    val severity: String,
    val message: String,
    val createdAt: OffsetDateTime,
    val isResolved: Boolean
)

data class FraudRiskTrendResponse(
    val period: String,
    val averageRiskScore: BigDecimal,
    val assessmentCount: Int,
    val highRiskCount: Int,
    val mediumRiskCount: Int,
    val lowRiskCount: Int
)

data class UserActivityResponse(
    val totalActions: Int,
    val actionCounts: Map<String, Int>,
    val dailyActivity: Map<String, Int>,
    val mostActiveUsers: List<Map<String, Any>>
)