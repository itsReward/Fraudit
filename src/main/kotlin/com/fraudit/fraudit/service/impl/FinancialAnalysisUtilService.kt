package com.fraudit.fraudit.service.impl

import com.fraudit.fraudit.domain.enum.StatementStatus
import com.fraudit.fraudit.repository.FinancialDataRepository
import com.fraudit.fraudit.repository.FinancialStatementRepository
import com.fraudit.fraudit.service.AuditLogService
import com.fraudit.fraudit.service.FinancialAnalysisService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled

/**
 * Utility service for running financial analysis on existing data
 */
@Service
class FinancialAnalysisUtilService(
    private val financialStatementRepository: FinancialStatementRepository,
    private val financialDataRepository: FinancialDataRepository,
    private val financialAnalysisService: FinancialAnalysisService,
    private val auditLogService: AuditLogService,
    @Value("\${fraudit.analysis.auto-analyze-existing:false}") private val autoAnalyzeExisting: Boolean,
    @Value("\${fraudit.analysis.system-user-id:00000000-0000-0000-0000-000000000000}") private val systemUserId: String
) {
    private val logger = LoggerFactory.getLogger(FinancialAnalysisUtilService::class.java)

    // System user ID for automated operations
    private val systemUUID = UUID.fromString(systemUserId)

    /**
     * Run analysis on all statements that have financial data but haven't been analyzed
     * This runs on application startup if autoAnalyzeExisting is true
     */
    @PostConstruct
    @Profile("!test") // Skip in test environment
    fun analyzeExistingDataOnStartup() {
        if (autoAnalyzeExisting) {
            logger.info("Auto-analyzing existing financial data on startup...")
            val count = analyzeAllPendingStatements()
            logger.info("Completed analysis of $count statements")
        }
    }

    /**
     * Scheduled job to periodically check for and analyze statements with unanalyzed financial data
     * Runs every day at 1:00 AM
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Profile("!test") // Skip in test environment
    fun scheduledAnalysis() {
        logger.info("Running scheduled analysis of unanalyzed financial data...")
        val count = analyzeAllPendingStatements()
        logger.info("Scheduled analysis completed for $count statements")
    }

    /**
     * Analyze all financial statements that have financial data but haven't been fully analyzed
     * @return number of statements analyzed
     */
    @Transactional
    fun analyzeAllPendingStatements(): Int {
        // Find all statements that are in PROCESSED state (have financial data) but not ANALYZED
        val statements = financialStatementRepository.findByStatusWithCompany(StatementStatus.PROCESSED)

        logger.info("Found ${statements.size} statements with financial data ready for analysis")

        var successCount = 0
        var errorCount = 0

        statements.forEach { statement ->
            try {
                // Check if financial data exists
                val financialData = financialDataRepository.findByStatementId(statement.id!!)
                if (financialData != null) {
                    logger.info("Analyzing statement ID: ${statement.id} for ${statement.fiscalYear.company.name}")

                    // First remove any existing analysis data for this statement to prevent constraint violations
                    try {
                        // This should be implemented in FinancialAnalysisService
                        financialAnalysisService.deleteExistingAnalysis(statement.id!!)
                        logger.info("Cleared existing analysis data for statement ID: ${statement.id}")
                    } catch (e: Exception) {
                        logger.warn("Failed to clear existing analysis data: ${e.message}")
                        // Continue with the analysis anyway, it might work if there's no data
                    }

                    // Run the analysis pipeline with tryCatch for each component
                    try {
                        financialAnalysisService.calculateAllScoresAndRatios(statement.id!!, systemUUID)

                        // Log the automated analysis
                        auditLogService.logEvent(
                            userId = systemUUID,
                            action = "AUTO_ANALYZE",
                            entityType = "FINANCIAL_STATEMENT",
                            entityId = statement.id.toString(),
                            details = "Automatically analyzed existing financial data for ${statement.fiscalYear.company.name}"
                        )

                        successCount++
                    } catch (e: Exception) {
                        logger.error("Failed to calculate scores and ratios: ${e.message}", e)
                        errorCount++
                    }
                }
            } catch (e: Exception) {
                logger.error("Error analyzing statement ID: ${statement.id}: ${e.message}", e)
                errorCount++

                // Log the error
                auditLogService.logEvent(
                    userId = systemUUID,
                    action = "AUTO_ANALYZE_ERROR",
                    entityType = "FINANCIAL_STATEMENT",
                    entityId = statement.id.toString(),
                    details = "Error during automatic analysis: ${e.message}"
                )
            }
        }

        logger.info("Analysis complete. Successfully analyzed: $successCount, Errors: $errorCount")
        return successCount
    }

    /**
     * Manually trigger analysis of a specific statement
     * @param statementId ID of the statement to analyze
     * @param userId ID of the user triggering the analysis
     * @return true if analysis was successful, false otherwise
     */
    @Transactional
    fun analyzeStatement(statementId: Long, userId: UUID): Boolean {
        return try {
            // Check if financial data exists
            val financialData = financialDataRepository.findByStatementId(statementId)
            if (financialData == null) {
                logger.warn("Cannot analyze statement ID: $statementId - no financial data found")
                return false
            }

            // Delete any existing analysis data
            try {
                financialAnalysisService.deleteExistingAnalysis(statementId)
                logger.info("Cleared existing analysis data for statement ID: $statementId")
            } catch (e: Exception) {
                logger.warn("Failed to clear existing analysis data: ${e.message}")
                // Continue anyway
            }

            // Run the analysis pipeline
            financialAnalysisService.calculateAllScoresAndRatios(statementId, userId)

            // Log the manual analysis
            auditLogService.logEvent(
                userId = userId,
                action = "MANUAL_ANALYZE",
                entityType = "FINANCIAL_STATEMENT",
                entityId = statementId.toString(),
                details = "Manually triggered analysis of financial data"
            )

            true
        } catch (e: Exception) {
            logger.error("Error analyzing statement ID: $statementId: ${e.message}", e)

            // Log the error
            auditLogService.logEvent(
                userId = userId,
                action = "MANUAL_ANALYZE_ERROR",
                entityType = "FINANCIAL_STATEMENT",
                entityId = statementId.toString(),
                details = "Error during manual analysis: ${e.message}"
            )

            false
        }
    }


}