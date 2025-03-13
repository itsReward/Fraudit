package com.fraudit.fraudit.service

import java.io.OutputStream
import java.util.UUID

/**
 * Service for generating and exporting reports
 */
interface ReportService {
    /**
     * Generate a fraud risk assessment report for a specific statement
     * @param statementId ID of the financial statement
     * @param outputStream Output stream to write the report to
     * @param userId ID of the user generating the report
     */
    fun generateFraudRiskReport(statementId: Long, outputStream: OutputStream, userId: UUID)

    /**
     * Generate a company overview report
     * @param companyId ID of the company
     * @param outputStream Output stream to write the report to
     * @param userId ID of the user generating the report
     */
    fun generateCompanyOverviewReport(companyId: Long, outputStream: OutputStream, userId: UUID)

    /**
     * Generate a comparative analysis report for multiple companies
     * @param companyIds List of company IDs to include in the report
     * @param outputStream Output stream to write the report to
     * @param userId ID of the user generating the report
     */
    fun generateComparativeReport(companyIds: List<Long>, outputStream: OutputStream, userId: UUID)

    /**
     * Generate a summary report of all high-risk companies
     * @param riskThreshold Minimum risk score to include in the report
     * @param outputStream Output stream to write the report to
     * @param userId ID of the user generating the report
     */
    fun generateHighRiskSummaryReport(riskThreshold: Int, outputStream: OutputStream, userId: UUID)
}