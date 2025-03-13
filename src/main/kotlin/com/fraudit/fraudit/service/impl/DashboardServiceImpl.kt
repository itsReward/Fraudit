package com.fraudit.fraudit.service.impl

import com.fraudit.fraudit.domain.enum.RiskLevel
import com.fraudit.fraudit.repository.*
import com.fraudit.fraudit.service.DashboardService
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Service
class DashboardServiceImpl(
    private val fraudRiskAssessmentRepository: FraudRiskAssessmentRepository,
    private val companyRepository: CompanyRepository,
    private val riskAlertRepository: RiskAlertRepository,
    private val financialStatementRepository: FinancialStatementRepository,
    private val auditLogRepository: AuditLogRepository
) : DashboardService {

    override fun getFraudRiskStats(): Map<String, Any> {
        val allAssessments = fraudRiskAssessmentRepository.findAll()

        // Total number of assessments
        val totalAssessments = allAssessments.size

        // Count by risk level
        val highRiskCount = allAssessments.count { it.riskLevel == RiskLevel.HIGH || it.riskLevel == RiskLevel.VERY_HIGH }
        val mediumRiskCount = allAssessments.count { it.riskLevel == RiskLevel.MEDIUM }
        val lowRiskCount = allAssessments.count { it.riskLevel == RiskLevel.LOW }

        // Average risk score
        val avgRiskScore = if (allAssessments.isNotEmpty()) {
            allAssessments
                .mapNotNull { it.overallRiskScore }
                .takeIf { it.isNotEmpty() }
                ?.let { scores ->
                    scores.reduce { acc, score -> acc.add(score) }
                        .divide(BigDecimal(scores.size), 2, BigDecimal.ROUND_HALF_UP)
                } ?: BigDecimal.ZERO
        } else {
            BigDecimal.ZERO
        }

        // Number of unresolved alerts
        val unresolvedAlerts = riskAlertRepository.findByIsResolved(false).size

        return mapOf(
            "totalAssessments" to totalAssessments,
            "highRiskCount" to highRiskCount,
            "mediumRiskCount" to mediumRiskCount,
            "lowRiskCount" to lowRiskCount,
            "averageRiskScore" to avgRiskScore,
            "unresolvedAlerts" to unresolvedAlerts
        )
    }

    override fun getCompanyRiskSummary(): List<Map<String, Any>> {
        val companies = companyRepository.findAll()

        val result = companies.mapNotNull { company ->
            // Get the latest assessment for this company
            val assessments = fraudRiskAssessmentRepository.findLatestByCompanyId(company.id!!)

            if (assessments.isEmpty()) {
                null // Skip companies with no assessments
            } else {
                val latestAssessment = assessments.first()

                mapOf(
                    "companyId" to (company.id as Any),
                    "companyName" to company.name,
                    "stockCode" to company.stockCode,
                    "sector" to (company.sector ?: "Unknown"),
                    "riskLevel" to (latestAssessment.riskLevel?.name ?: "Unknown"),
                    "riskScore" to (latestAssessment.overallRiskScore ?: BigDecimal.ZERO),
                    "assessmentDate" to latestAssessment.assessedAt
                )
            }
        }

        // Create a new sorted list with explicit type
        val sortedResult = result.sortedWith(compareByDescending<Map<String, Any>> {
            it["riskScore"] as BigDecimal
        })

        // Explicitly cast the result to the expected type
        return sortedResult as List<Map<String, Any>>
    }

    override fun getFraudIndicatorsDistribution(): Map<String, Map<String, Int>> {
        val allAssessments = fraudRiskAssessmentRepository.findAll()

        // Z-Score risk distribution
        val zScoreDistribution = mapOf(
            "high" to allAssessments.count { it.zScoreRisk != null && it.zScoreRisk!! >= BigDecimal("70.0") },
            "medium" to allAssessments.count { it.zScoreRisk != null && it.zScoreRisk!! >= BigDecimal("40.0") && it.zScoreRisk!! < BigDecimal("70.0") },
            "low" to allAssessments.count { it.zScoreRisk != null && it.zScoreRisk!! < BigDecimal("40.0") }
        )

        // M-Score risk distribution
        val mScoreDistribution = mapOf(
            "high" to allAssessments.count { it.mScoreRisk != null && it.mScoreRisk!! >= BigDecimal("70.0") },
            "medium" to allAssessments.count { it.mScoreRisk != null && it.mScoreRisk!! >= BigDecimal("40.0") && it.mScoreRisk!! < BigDecimal("70.0") },
            "low" to allAssessments.count { it.mScoreRisk != null && it.mScoreRisk!! < BigDecimal("40.0") }
        )

        // F-Score risk distribution
        val fScoreDistribution = mapOf(
            "high" to allAssessments.count { it.fScoreRisk != null && it.fScoreRisk!! >= BigDecimal("70.0") },
            "medium" to allAssessments.count { it.fScoreRisk != null && it.fScoreRisk!! >= BigDecimal("40.0") && it.fScoreRisk!! < BigDecimal("70.0") },
            "low" to allAssessments.count { it.fScoreRisk != null && it.fScoreRisk!! < BigDecimal("40.0") }
        )

        // Financial ratio risk distribution
        val ratioDistribution = mapOf(
            "high" to allAssessments.count { it.financialRatioRisk != null && it.financialRatioRisk!! >= BigDecimal("70.0") },
            "medium" to allAssessments.count { it.financialRatioRisk != null && it.financialRatioRisk!! >= BigDecimal("40.0") && it.financialRatioRisk!! < BigDecimal("70.0") },
            "low" to allAssessments.count { it.financialRatioRisk != null && it.financialRatioRisk!! < BigDecimal("40.0") }
        )

        // ML prediction risk distribution
        val mlDistribution = mapOf(
            "high" to allAssessments.count { it.mlPredictionRisk != null && it.mlPredictionRisk!! >= BigDecimal("70.0") },
            "medium" to allAssessments.count { it.mlPredictionRisk != null && it.mlPredictionRisk!! >= BigDecimal("40.0") && it.mlPredictionRisk!! < BigDecimal("70.0") },
            "low" to allAssessments.count { it.mlPredictionRisk != null && it.mlPredictionRisk!! < BigDecimal("40.0") }
        )

        return mapOf(
            "zScore" to zScoreDistribution,
            "mScore" to mScoreDistribution,
            "fScore" to fScoreDistribution,
            "financialRatio" to ratioDistribution,
            "mlPrediction" to mlDistribution
        )
    }

    override fun getRecentRiskAlerts(limit: Int): List<Map<String, Any>> {
        // Get all alerts, sorted by creation date descending
        val alerts = riskAlertRepository.findAll()
            .sortedByDescending { it.createdAt }
            .take(limit)

        return alerts.map { alert ->
            val companyName = alert.assessment.statement.fiscalYear.company.name

            mapOf(
                "alertId" to (alert.id as Any), // Cast nullable Long to Any
                "companyName" to companyName,
                "alertType" to alert.alertType,
                "severity" to alert.severity.name,
                "message" to alert.message,
                "createdAt" to alert.createdAt,
                "isResolved" to alert.isResolved
            )
        }
    }

    override fun getFraudRiskTrends(companyId: Long?): List<Map<String, Any>> {
        val assessments = if (companyId != null) {
            fraudRiskAssessmentRepository.findLatestByCompanyId(companyId)
        } else {
            fraudRiskAssessmentRepository.findAll()
        }

        // Group assessments by month
        val groupedByMonth = assessments.groupBy {
            val date = it.assessedAt
            "${date.year}-${String.format("%02d", date.monthValue)}"
        }

        val result = groupedByMonth.map { (monthYear, monthAssessments) ->
            val avgRiskScore = monthAssessments
                .mapNotNull { it.overallRiskScore }
                .takeIf { it.isNotEmpty() }
                ?.let { scores ->
                    scores.reduce { acc, score -> acc.add(score) }
                        .divide(BigDecimal(scores.size), 2, BigDecimal.ROUND_HALF_UP)
                } ?: BigDecimal.ZERO

            val highRiskCount = monthAssessments.count { it.riskLevel == RiskLevel.HIGH || it.riskLevel == RiskLevel.VERY_HIGH }
            val mediumRiskCount = monthAssessments.count { it.riskLevel == RiskLevel.MEDIUM }
            val lowRiskCount = monthAssessments.count { it.riskLevel == RiskLevel.LOW }

            mapOf<String, Any>(
                "period" to monthYear,
                "averageRiskScore" to avgRiskScore,
                "assessmentCount" to monthAssessments.size,
                "highRiskCount" to highRiskCount,
                "mediumRiskCount" to mediumRiskCount,
                "lowRiskCount" to lowRiskCount
            )
        }

        // Use compareBy with explicit type for sorting and explicit return type
        val sortedResult = result.sortedWith(compareBy<Map<String, Any>> {
            it["period"] as String
        })

        return sortedResult
    }

    override fun getUserActivityStats(userId: UUID?): Map<String, Any> {
        // Get audit logs, filtered by user if specified
        val logs = if (userId != null) {
            auditLogRepository.findByUserId(userId)
        } else {
            auditLogRepository.findAll()
        }

        // Count actions by type
        val actionCounts = logs.groupBy { it.action }
            .mapValues { it.value.size }

        // Count activities by day for the last 30 days
        val now = OffsetDateTime.now()
        val thirtyDaysAgo = now.minusDays(30)

        val dailyActivity = logs
            .filter { it.timestamp.isAfter(thirtyDaysAgo) }
            .groupBy { it.timestamp.toLocalDate().toString() }
            .mapValues { it.value.size }

        // Most active users if not filtering by user
        val mostActiveUsers = if (userId == null) {
            logs.groupBy { it.user?.id }
                .mapValues { it.value.size }
                .entries
                .sortedByDescending { it.value }
                .take(5)
                .map { entry ->
                    val user = entry.key?.let { id ->
                        logs.firstOrNull { it.user?.id == id }?.user
                    }

                    mapOf(
                        "userId" to (user?.id ?: "Unknown"),
                        "username" to (user?.username ?: "Unknown"),
                        "activityCount" to entry.value
                    )
                }
        } else {
            emptyList()
        }

        return mapOf(
            "totalActions" to logs.size,
            "actionCounts" to actionCounts,
            "dailyActivity" to dailyActivity,
            "mostActiveUsers" to mostActiveUsers
        )
    }
}