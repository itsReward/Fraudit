package com.fraudit.fraudit.controller

import com.fraudit.fraudit.dto.common.ApiResponse
import com.fraudit.fraudit.dto.dashboard.*
import com.fraudit.fraudit.service.DashboardService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@RestController
@RequestMapping("/api/dashboard")
class DashboardController(private val dashboardService: DashboardService) {

    @GetMapping("/fraud-risk-stats")
    fun getFraudRiskStats(): ResponseEntity<ApiResponse<FraudRiskStatsResponse>> {
        val stats = dashboardService.getFraudRiskStats()
        
        val response = FraudRiskStatsResponse(
            totalAssessments = stats["totalAssessments"] as Int,
            highRiskCount = stats["highRiskCount"] as Int,
            mediumRiskCount = stats["mediumRiskCount"] as Int,
            lowRiskCount = stats["lowRiskCount"] as Int,
            averageRiskScore = stats["averageRiskScore"] as BigDecimal,
            unresolvedAlerts = stats["unresolvedAlerts"] as Int
        )
        
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Fraud risk statistics retrieved successfully",
                data = response
            )
        )
    }

    @GetMapping("/company-risk-summary")
    fun getCompanyRiskSummary(): ResponseEntity<ApiResponse<List<CompanyRiskSummaryResponse>>> {
        val summaries = dashboardService.getCompanyRiskSummary()
        
        val response = summaries.map { summary ->
            CompanyRiskSummaryResponse(
                companyId = summary["companyId"] as Long,
                companyName = summary["companyName"] as String,
                stockCode = summary["stockCode"] as String,
                sector = summary["sector"] as String,
                riskLevel = summary["riskLevel"] as String,
                riskScore = summary["riskScore"] as BigDecimal,
                assessmentDate = summary["assessmentDate"] as OffsetDateTime
            )
        }
        
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Company risk summary retrieved successfully",
                data = response
            )
        )
    }

    @GetMapping("/fraud-indicators-distribution")
    fun getFraudIndicatorsDistribution(): ResponseEntity<ApiResponse<FraudIndicatorsDistributionResponse>> {
        val distribution = dashboardService.getFraudIndicatorsDistribution()
        
        val response = FraudIndicatorsDistributionResponse(
            zScore = distribution["zScore"] as Map<String, Int>,
            mScore = distribution["mScore"] as Map<String, Int>,
            fScore = distribution["fScore"] as Map<String, Int>,
            financialRatio = distribution["financialRatio"] as Map<String, Int>,
            mlPrediction = distribution["mlPrediction"] as Map<String, Int>
        )
        
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Fraud indicators distribution retrieved successfully",
                data = response
            )
        )
    }

    @GetMapping("/recent-risk-alerts")
    fun getRecentRiskAlerts(@RequestParam(defaultValue = "5") limit: Int): ResponseEntity<ApiResponse<List<RecentRiskAlertResponse>>> {
        val alerts = dashboardService.getRecentRiskAlerts(limit)
        
        val response = alerts.map { alert ->
            RecentRiskAlertResponse(
                alertId = alert["alertId"] as Long,
                companyName = alert["companyName"] as String,
                alertType = alert["alertType"] as String,
                severity = alert["severity"] as String,
                message = alert["message"] as String,
                createdAt = alert["createdAt"] as OffsetDateTime,
                isResolved = alert["isResolved"] as Boolean
            )
        }
        
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Recent risk alerts retrieved successfully",
                data = response
            )
        )
    }

    @GetMapping("/fraud-risk-trends")
    fun getFraudRiskTrends(@RequestParam(required = false) companyId: Long?): ResponseEntity<ApiResponse<List<FraudRiskTrendResponse>>> {
        val trends = dashboardService.getFraudRiskTrends(companyId)
        
        val response = trends.map { trend ->
            FraudRiskTrendResponse(
                period = trend["period"] as String,
                averageRiskScore = trend["averageRiskScore"] as BigDecimal,
                assessmentCount = trend["assessmentCount"] as Int,
                highRiskCount = trend["highRiskCount"] as Int,
                mediumRiskCount = trend["mediumRiskCount"] as Int,
                lowRiskCount = trend["lowRiskCount"] as Int
            )
        }
        
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Fraud risk trends retrieved successfully",
                data = response
            )
        )
    }

    @GetMapping("/user-activity")
    fun getUserActivityStats(@RequestParam(required = false) userId: UUID?): ResponseEntity<ApiResponse<UserActivityResponse>> {
        val stats = dashboardService.getUserActivityStats(userId)
        
        val response = UserActivityResponse(
            totalActions = stats["totalActions"] as Int,
            actionCounts = stats["actionCounts"] as Map<String, Int>,
            dailyActivity = stats["dailyActivity"] as Map<String, Int>,
            mostActiveUsers = (stats["mostActiveUsers"] as List<Map<String, Any>>).map { user ->
                UserActivityDetail(
                    userId = user["userId"].toString(),
                    username = user["username"] as String,
                    activityCount = user["activityCount"] as Int
                )
            }
        )
        
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "User activity statistics retrieved successfully",
                data = response
            )
        )
    }
}