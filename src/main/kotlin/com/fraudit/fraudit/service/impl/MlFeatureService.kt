package com.fraudit.fraudit.service.impl

import com.fraudit.fraudit.domain.entity.MlFeatures
import com.fraudit.fraudit.repository.*
import com.fraudit.fraudit.service.AuditLogService
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import jakarta.persistence.EntityNotFoundException

/**
 * Service for generating and managing ML features
 */
@Service
class MlFeatureService(
    private val mlFeaturesRepository: MlFeaturesRepository,
    private val financialStatementRepository: FinancialStatementRepository,
    private val altmanZScoreRepository: AltmanZScoreRepository,
    private val beneishMScoreRepository: BeneishMScoreRepository,
    private val piotroskiFScoreRepository: PiotroskiFScoreRepository,
    private val financialRatiosRepository: FinancialRatiosRepository,
    private val auditLogService: AuditLogService
) {
    private val logger = LoggerFactory.getLogger(MlFeatureService::class.java)

    /**
     * Result class for batch operations
     */
    data class BatchResult(
        val processedCount: Int,
        val successCount: Int,
        val failureCount: Int,
        val errors: Map<Long, String>
    )

    /**
     * Generate ML features for a single statement
     */
    @Transactional
    fun generateFeaturesForStatement(statementId: Long, userId: UUID): MlFeatures {
        // First check if features already exist
        mlFeaturesRepository.findByStatementId(statementId)?.let {
            logger.info("ML features already exist for statement ID: $statementId, returning existing features")
            return it
        }

        // Get the statement
        val statement = financialStatementRepository.findById(statementId)
            .orElseThrow { EntityNotFoundException("Financial statement not found with id: $statementId") }

        // Check if required financial data is available
        val altmanZScore = altmanZScoreRepository.findByStatementId(statementId)
        val beneishMScore = beneishMScoreRepository.findByStatementId(statementId)
        val piotroskiFScore = piotroskiFScoreRepository.findByStatementId(statementId)
        val financialRatios = financialRatiosRepository.findByStatementId(statementId)

        if (altmanZScore == null || beneishMScore == null || piotroskiFScore == null || financialRatios == null) {
            throw IllegalArgumentException("Required financial metrics are missing. Please calculate all scores first for statement ID: $statementId")
        }

        // Create JSON object with all the features
        val featuresJson = JSONObject()

        // Add financial ratios
        featuresJson.put("current_ratio", financialRatios.currentRatio?.toDouble() ?: 0.0)
        featuresJson.put("quick_ratio", financialRatios.quickRatio?.toDouble() ?: 0.0)
        featuresJson.put("cash_ratio", financialRatios.cashRatio?.toDouble() ?: 0.0)
        featuresJson.put("gross_margin", financialRatios.grossMargin?.toDouble() ?: 0.0)
        featuresJson.put("operating_margin", financialRatios.operatingMargin?.toDouble() ?: 0.0)
        featuresJson.put("net_profit_margin", financialRatios.netProfitMargin?.toDouble() ?: 0.0)
        featuresJson.put("return_on_assets", financialRatios.returnOnAssets?.toDouble() ?: 0.0)
        featuresJson.put("return_on_equity", financialRatios.returnOnEquity?.toDouble() ?: 0.0)
        featuresJson.put("asset_turnover", financialRatios.assetTurnover?.toDouble() ?: 0.0)
        featuresJson.put("inventory_turnover", financialRatios.inventoryTurnover?.toDouble() ?: 0.0)
        featuresJson.put("accounts_receivable_turnover", financialRatios.accountsReceivableTurnover?.toDouble() ?: 0.0)
        featuresJson.put("days_sales_outstanding", financialRatios.daysSalesOutstanding?.toDouble() ?: 0.0)
        featuresJson.put("debt_to_equity", financialRatios.debtToEquity?.toDouble() ?: 0.0)
        featuresJson.put("debt_ratio", financialRatios.debtRatio?.toDouble() ?: 0.0)
        featuresJson.put("interest_coverage", financialRatios.interestCoverage?.toDouble() ?: 0.0)
        featuresJson.put("accrual_ratio", financialRatios.accrualRatio?.toDouble() ?: 0.0)
        featuresJson.put("earnings_quality", financialRatios.earningsQuality?.toDouble() ?: 0.0)

        // Add Z-Score components
        featuresJson.put("working_capital_to_total_assets", altmanZScore.workingCapitalToTotalAssets?.toDouble() ?: 0.0)
        featuresJson.put("retained_earnings_to_total_assets", altmanZScore.retainedEarningsToTotalAssets?.toDouble() ?: 0.0)
        featuresJson.put("ebit_to_total_assets", altmanZScore.ebitToTotalAssets?.toDouble() ?: 0.0)
        featuresJson.put("market_value_equity_to_book_value_debt", altmanZScore.marketValueEquityToBookValueDebt?.toDouble() ?: 0.0)
        featuresJson.put("sales_to_total_assets", altmanZScore.salesToTotalAssets?.toDouble() ?: 0.0)
        featuresJson.put("z_score", altmanZScore.zScore?.toDouble() ?: 0.0)

        // Add M-Score components
        featuresJson.put("days_sales_receivables_index", beneishMScore.daysSalesReceivablesIndex?.toDouble() ?: 0.0)
        featuresJson.put("gross_margin_index", beneishMScore.grossMarginIndex?.toDouble() ?: 0.0)
        featuresJson.put("asset_quality_index", beneishMScore.assetQualityIndex?.toDouble() ?: 0.0)
        featuresJson.put("sales_growth_index", beneishMScore.salesGrowthIndex?.toDouble() ?: 0.0)
        featuresJson.put("depreciation_index", beneishMScore.depreciationIndex?.toDouble() ?: 0.0)
        featuresJson.put("sg_admin_expenses_index", beneishMScore.sgAdminExpensesIndex?.toDouble() ?: 0.0)
        featuresJson.put("leverage_index", beneishMScore.leverageIndex?.toDouble() ?: 0.0)
        featuresJson.put("total_accruals_to_total_assets", beneishMScore.totalAccrualsToTotalAssets?.toDouble() ?: 0.0)
        featuresJson.put("m_score", beneishMScore.mScore?.toDouble() ?: 0.0)

        // Add F-Score components
        featuresJson.put("positive_net_income", piotroskiFScore.positiveNetIncome ?: false)
        featuresJson.put("positive_operating_cash_flow", piotroskiFScore.positiveOperatingCashFlow ?: false)
        featuresJson.put("cash_flow_greater_than_net_income", piotroskiFScore.cashFlowGreaterThanNetIncome ?: false)
        featuresJson.put("improving_roa", piotroskiFScore.improvingRoa ?: false)
        featuresJson.put("decreasing_leverage", piotroskiFScore.decreasingLeverage ?: false)
        featuresJson.put("improving_current_ratio", piotroskiFScore.improvingCurrentRatio ?: false)
        featuresJson.put("no_new_shares", piotroskiFScore.noNewShares ?: false)
        featuresJson.put("improving_gross_margin", piotroskiFScore.improvingGrossMargin ?: false)
        featuresJson.put("improving_asset_turnover", piotroskiFScore.improvingAssetTurnover ?: false)
        featuresJson.put("f_score", piotroskiFScore.fScore ?: 0)

        // For training purposes, add a fraud label (in real scenarios, this would come from actual data)
        // Here we're using a heuristic based on known fraud indicators
        val isFraud = (beneishMScore.mScore?.toDouble() ?: -3.0) > -1.78 ||
                (altmanZScore.zScore?.toDouble() ?: 3.0) < 1.8
        featuresJson.put("fraud", if (isFraud) 1 else 0)

        // Create and save ML Features entity
        val mlFeatures = MlFeatures(
            id = null,
            statement = statement,
            featureSet = featuresJson.toString(),
            createdAt = OffsetDateTime.now()
        )

        val savedFeatures = mlFeaturesRepository.save(mlFeatures)

        // Log the event
        auditLogService.logEvent(
            userId = userId,
            action = "GENERATE_FEATURES",
            entityType = "ML_FEATURES",
            entityId = savedFeatures.id.toString(),
            details = "Generated ML features for statement ID: $statementId"
        )

        return savedFeatures
    }

    /**
     * Generate ML features for multiple statements in parallel
     */
    fun generateFeaturesForStatements(statementIds: List<Long>, userId: UUID): BatchResult {
        logger.info("Starting batch feature generation for ${statementIds.size} statements")

        val errors = mutableMapOf<Long, String>()
        var processedCount = 0
        var successCount = 0
        var failureCount = 0

        // Use a thread pool to process statements in parallel
        val threadPool = Executors.newFixedThreadPool(10) // Adjust based on your server capacity

        try {
            val tasks = statementIds.map { statementId ->
                Callable {
                    try {
                        generateFeatureSafely(statementId, userId)
                        true // Success
                    } catch (e: Exception) {
                        errors[statementId] = e.message ?: "Unknown error"
                        false // Failure
                    } finally {
                        processedCount++
                    }
                }
            }

            // Execute all tasks and collect results
            val results = threadPool.invokeAll(tasks)

            // Count successes and failures
            successCount = results.count { it.get() == true }
            failureCount = results.count { it.get() == false }

        } finally {
            threadPool.shutdown()
        }

        // Log summary
        auditLogService.logEvent(
            userId = userId,
            action = "BATCH_GENERATE_FEATURES",
            entityType = "ML_FEATURES",
            entityId = "batch",
            details = "Generated ML features for $successCount/${statementIds.size} statements (${failureCount} failed)"
        )

        return BatchResult(
            processedCount = processedCount,
            successCount = successCount,
            failureCount = failureCount,
            errors = errors
        )
    }

    /**
     * Safe wrapper for feature generation with transaction isolation
     */
    @Transactional(noRollbackFor = [Exception::class])
    fun generateFeatureSafely(statementId: Long, userId: UUID): MlFeatures {
        return try {
            generateFeaturesForStatement(statementId, userId)
        } catch (e: Exception) {
            logger.error("Error generating features for statement ID $statementId: ${e.message}", e)
            throw e
        }
    }

    /**
     * Check which statements already have ML features
     */
    fun checkFeaturesExistence(statementIds: List<Long>): Map<Long, Boolean> {
        return statementIds.associateWith { statementId ->
            mlFeaturesRepository.findByStatementId(statementId) != null
        }
    }
}