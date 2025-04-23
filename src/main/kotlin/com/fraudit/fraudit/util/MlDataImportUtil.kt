package com.fraudit.fraudit.util

import com.fraudit.fraudit.domain.entity.*
import com.fraudit.fraudit.repository.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Paths
import java.time.OffsetDateTime
import java.util.*
import org.json.JSONObject
import org.slf4j.LoggerFactory
import jakarta.persistence.EntityNotFoundException
import weka.classifiers.trees.RandomForest
import weka.core.SerializationHelper

/**
 * Utility component for importing ML data and models from files
 */
@Component
class MlDataImportUtil(
    private val mlModelRepository: MlModelRepository,
    private val mlFeaturesRepository: MlFeaturesRepository,
    private val mlPredictionRepository: MlPredictionRepository,
    private val financialStatementRepository: FinancialStatementRepository,
    private val altmanZScoreRepository: AltmanZScoreRepository,
    private val beneishMScoreRepository: BeneishMScoreRepository,
    private val piotroskiFScoreRepository: PiotroskiFScoreRepository,
    private val financialRatiosRepository: FinancialRatiosRepository,

    @Value("\${ml.model.storage.dir:ml-models}")
    private val modelStorageDir: String
) {
    private val logger = LoggerFactory.getLogger(MlDataImportUtil::class.java)

    init {
        // Create model storage directory if it doesn't exist
        val dir = File(modelStorageDir)
        if (!dir.exists()) {
            dir.mkdirs()
        }
    }

    /**
     * Import an existing model file into the system
     */
    @Transactional
    fun importExternalModel(
        modelFile: File,
        modelName: String,
        modelVersion: String,
        modelType: String = "RANDOM_FOREST",
        isActive: Boolean = false,
        userId: UUID
    ): MlModel {
        // Verify the model file exists and is readable
        if (!modelFile.exists() || !modelFile.canRead()) {
            throw IllegalArgumentException("Model file does not exist or cannot be read: ${modelFile.absolutePath}")
        }

        // Try to load the model to verify it's valid
        try {
            val model = SerializationHelper.read(modelFile.absolutePath) as? RandomForest
                ?: throw IllegalArgumentException("Model file is not a valid RandomForest model")

            // Get basic model info for performance metrics
            val numTrees = model.numIterations

            // Basic performance metrics placeholder
            // (would be better to calculate these properly with validation data)
            val performanceMetrics = JSONObject().apply {
                put("accuracy", 0.0) // Placeholder
                put("precision", 0.0) // Placeholder
                put("recall", 0.0) // Placeholder
                put("f1_score", 0.0) // Placeholder
                put("auc", 0.0) // Placeholder
                put("num_training_instances", 0) // Unknown for imported model
                put("num_trees", numTrees)
                put("imported", true)
            }.toString()

            // Create feature list from model
            val featureList = JSONObject().apply {
                // We don't know the exact features, so create generic placeholders
                // based on standard financial fraud features
                put("current_ratio", 0)
                put("quick_ratio", 1)
                put("gross_margin", 2)
                put("net_profit_margin", 3)
                put("return_on_assets", 4)
                put("return_on_equity", 5)
                put("asset_turnover", 6)
                put("debt_to_equity", 7)
                put("accrual_ratio", 8)
                put("z_score", 9)
                put("m_score", 10)
                put("f_score", 11)
            }.toString()

            // Copy model file to our model storage directory
            val destinationPath = Paths.get(modelStorageDir, "${modelName}_${modelVersion}.model")
            Files.copy(modelFile.toPath(), destinationPath)

            // Create and save the model entity
            val mlModel = MlModel(
                id = null,
                modelName = modelName,
                modelType = modelType,
                modelVersion = modelVersion,
                featureList = featureList,
                performanceMetrics = performanceMetrics,
                trainedDate = OffsetDateTime.now(),
                isActive = isActive,
                modelPath = destinationPath.toString()
            )

            return mlModelRepository.save(mlModel)

        } catch (e: Exception) {
            logger.error("Error importing model: ${e.message}", e)
            throw IllegalArgumentException("Failed to import model: ${e.message}")
        }
    }

    /**
     * Generate synthetic ML features for a statement based on existing financial metrics
     */
    @Transactional
    fun generateFeaturesFromFinancialData(statementId: Long, userId: UUID): MlFeatures {
        // Get the statement
        val statement = financialStatementRepository.findById(statementId)
            .orElseThrow { EntityNotFoundException("Financial statement not found with id: $statementId") }

        // Check if required financial data is available
        val altmanZScore = altmanZScoreRepository.findByStatementId(statementId)
        val beneishMScore = beneishMScoreRepository.findByStatementId(statementId)
        val piotroskiFScore = piotroskiFScoreRepository.findByStatementId(statementId)
        val financialRatios = financialRatiosRepository.findByStatementId(statementId)

        if (altmanZScore == null || beneishMScore == null || piotroskiFScore == null || financialRatios == null) {
            throw IllegalArgumentException("Required financial metrics are missing. Please calculate all scores first.")
        }

        // Check if ML features already exist
        mlFeaturesRepository.findByStatementId(statementId)?.let {
            logger.info("ML features already exist for statement id: $statementId. Returning existing features.")
            return it
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

        // Create and save ML Features entity
        val mlFeatures = MlFeatures(
            id = null,
            statement = statement,
            featureSet = featuresJson.toString(),
            createdAt = OffsetDateTime.now()
        )

        return mlFeaturesRepository.save(mlFeatures)
    }

    /**
     * Bulk import ML features from JSON files
     */
    @Transactional
    fun bulkImportFeatures(featuresDir: File, statementIdMapping: Map<String, Long>, userId: UUID): Map<String, Any> {
        if (!featuresDir.exists() || !featuresDir.isDirectory) {
            throw IllegalArgumentException("Features directory does not exist or is not a directory")
        }

        val jsonFiles = featuresDir.listFiles { file -> file.isFile && file.name.endsWith(".json") }
            ?: emptyArray()

        if (jsonFiles.isEmpty()) {
            throw IllegalArgumentException("No JSON files found in directory")
        }

        var successCount = 0
        var errorCount = 0
        val errors = mutableListOf<String>()

        for (file in jsonFiles) {
            try {
                // Extract identifier from filename
                val fileId = file.nameWithoutExtension.replace("features_", "")

                // Get corresponding statementId
                val statementId = statementIdMapping[fileId]
                    ?: continue  // Skip if no mapping exists

                // Get statement
                val statement = financialStatementRepository.findById(statementId)
                    .orElseThrow { EntityNotFoundException("Financial statement not found with id: $statementId") }

                // Check if features already exist
                if (mlFeaturesRepository.findByStatementId(statementId) != null) {
                    logger.info("ML features already exist for statement id: $statementId. Skipping.")
                    continue
                }

                // Read JSON file
                val featureJson = Files.readString(file.toPath())

                // Create and save ML Features entity
                val mlFeatures = MlFeatures(
                    id = null,
                    statement = statement,
                    featureSet = featureJson,
                    createdAt = OffsetDateTime.now()
                )

                mlFeaturesRepository.save(mlFeatures)
                successCount++

            } catch (e: Exception) {
                logger.error("Error importing features from file ${file.name}: ${e.message}", e)
                errorCount++
                errors.add("${file.name}: ${e.message}")
            }
        }

        return mapOf(
            "totalFiles" to jsonFiles.size,
            "successCount" to successCount,
            "errorCount" to errorCount,
            "errors" to errors
        )
    }
}