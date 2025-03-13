package com.fraudit.fraudit.service

import com.fraudit.fraudit.domain.enum.RiskLevel
import java.math.BigDecimal
import java.util.UUID

/**
 * Service for providing aggregated data for dashboard displays
 */
interface DashboardService {
    /**
     * Get summary statistics for fraud risk assessments
     * @return Map of statistics and their values
     */
    fun getFraudRiskStats(): Map<String, Any>

    /**
     * Get company-level risk summary
     * @return List of companies with their risk levels and scores
     */
    fun getCompanyRiskSummary(): List<Map<String, Any>>

    /**
     * Get fraud indicators distribution
     * @return Distribution of different fraud indicators
     */
    fun getFraudIndicatorsDistribution(): Map<String, Map<String, Int>>

    /**
     * Get recent risk alerts
     * @param limit Maximum number of alerts to return
     * @return List of recent risk alerts
     */
    fun getRecentRiskAlerts(limit: Int): List<Map<String, Any>>

    /**
     * Get fraud risk trends over time
     * @param companyId Optional company ID to filter by
     * @return Fraud risk trends over time
     */
    fun getFraudRiskTrends(companyId: Long?): List<Map<String, Any>>

    /**
     * Get user activity statistics
     * @param userId Optional user ID to filter by
     * @return User activity statistics
     */
    fun getUserActivityStats(userId: UUID?): Map<String, Any>
}