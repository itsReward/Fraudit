package com.fraudit.fraudit.service.impl

import com.fraudit.fraudit.domain.entity.FinancialStatement
import com.fraudit.fraudit.domain.enum.StatementStatus
import com.fraudit.fraudit.repository.FinancialDataRepository
import com.fraudit.fraudit.repository.FinancialStatementRepository
import com.fraudit.fraudit.service.AuditLogService
import com.fraudit.fraudit.service.FinancialAnalysisService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * Service for batch processing financial analysis
 * Handles large volumes of financial statements efficiently
 */
@Service
class BatchAnalysisService(
    private val financialStatementRepository: FinancialStatementRepository,
    private val financialDataRepository: FinancialDataRepository,
    private val financialAnalysisService: FinancialAnalysisService,
    private val auditLogService: AuditLogService,
    @Value("\${fraudit.analysis.batch-size:10}") private val batchSize: Int,
    @Value("\${fraudit.analysis.system-user-id:00000000-0000-0000-0000-000000000000}") private val systemUserId: String
) {
    private val logger = LoggerFactory.getLogger(BatchAnalysisService::class.java)
    private val systemUUID = UUID.fromString(systemUserId)

    /**
     * Process all financial statements in batches asynchronously
     * @return CompletableFuture with results of the batch processing
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun processAllStatementsBatch(): CompletableFuture<Map<String, Int>> {
        logger.info("Starting batch processing of all financial statements")

        val results = mutableMapOf<String, Int>()
        results["processed"] = 0
        results["errors"] = 0

        try {
            var page = 0
            var hasMorePages = true

            // Process in batches to avoid loading too many statements at once
            while (hasMorePages) {
                val pageable = PageRequest.of(page, batchSize, Sort.by("id"))
                val statements = financialStatementRepository.findByStatus(StatementStatus.PROCESSED, pageable)

                if (statements.isEmpty) {
                    hasMorePages = false
                } else {
                    // Process this batch
                    for (statement in statements) {
                        try {
                            processStatementInNewTransaction(statement)
                            results["processed"] = results["processed"]!! + 1
                            logger.info("Batch processed statement ID: ${statement.id}")
                        } catch (e: Exception) {
                            results["errors"] = results["errors"]!! + 1
                            logger.error("Error in batch processing statement ID: ${statement.id}: ${e.message}", e)

                            // Log the error (in a new transaction)
                            try {
                                logErrorInNewTransaction(
                                    systemUUID,
                                    "BATCH_ANALYZE_ERROR",
                                    "FINANCIAL_STATEMENT",
                                    statement.id.toString(),
                                    "Error during batch analysis: ${e.message}"
                                )
                            } catch (ex: Exception) {
                                logger.error("Failed to log error event: ${ex.message}", ex)
                            }
                        }
                    }

                    page++
                    hasMorePages = !statements.isLast
                }
            }

            logger.info("Completed batch processing. Processed: ${results["processed"]}, Errors: ${results["errors"]}")
        } catch (e: Exception) {
            logger.error("Error during batch processing: ${e.message}", e)
        }

        return CompletableFuture.completedFuture(results)
    }

    /**
     * Process a specific company's financial statements in batches
     * @param companyId ID of the company
     * @return Results of the batch processing
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun processCompanyStatementsBatch(companyId: Long): Map<String, Int> {
        logger.info("Starting batch processing for company ID: $companyId")

        val results = mutableMapOf<String, Int>()
        results["processed"] = 0
        results["errors"] = 0

        try {
            // Get all PROCESSED statements for this company
            val statements = financialStatementRepository.findByFiscalYearCompanyIdAndStatus(companyId, StatementStatus.PROCESSED)

            // Process each statement in its own transaction
            for (statement in statements) {
                try {
                    processStatementInNewTransaction(statement)
                    results["processed"] = results["processed"]!! + 1

                    logger.info("Processed statement ID: ${statement.id} for company ID: $companyId")
                } catch (e: Exception) {
                    results["errors"] = results["errors"]!! + 1
                    logger.error("Error processing statement ID: ${statement.id} for company ID: $companyId: ${e.message}", e)

                    // Log the error in a new transaction
                    try {
                        logErrorInNewTransaction(
                            systemUUID,
                            "COMPANY_BATCH_ANALYZE_ERROR",
                            "FINANCIAL_STATEMENT",
                            statement.id.toString(),
                            "Error during company batch analysis: ${e.message}"
                        )
                    } catch (ex: Exception) {
                        logger.error("Failed to log error event: ${ex.message}", ex)
                    }
                }
            }

            logger.info("Completed batch processing for company ID: $companyId. Processed: ${results["processed"]}, Errors: ${results["errors"]}")
        } catch (e: Exception) {
            logger.error("Error during batch processing for company ID: $companyId: ${e.message}", e)
        }

        return results
    }

    /**
     * Process a single statement in a new transaction
     * This ensures that failures in one statement don't roll back the entire batch
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun processStatementInNewTransaction(statement: FinancialStatement) {
        // Check if financial data exists
        val financialData = financialDataRepository.findByStatementId(statement.id!!)
            ?: throw IllegalStateException("No financial data found for statement ID: ${statement.id}")

        // Run the analysis
        financialAnalysisService.calculateAllScoresAndRatios(statement.id!!, systemUUID)
    }

    /**
     * Log an error in a new transaction to prevent the main transaction from rolling back
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun logErrorInNewTransaction(
        userId: UUID,
        action: String,
        entityType: String,
        entityId: String,
        details: String
    ) {
        auditLogService.logEvent(
            userId = userId,
            action = action,
            entityType = entityType,
            entityId = entityId,
            details = details
        )
    }
}