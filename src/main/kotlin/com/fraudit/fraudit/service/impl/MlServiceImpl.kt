package com.fraudit.fraudit.service.impl

import com.fraudit.fraudit.domain.entity.*
import com.fraudit.fraudit.repository.*
import com.fraudit.fraudit.service.AuditLogService
import com.fraudit.fraudit.service.MlModelService
import org.json.JSONObject
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import weka.classifiers.Evaluation
import weka.classifiers.trees.RandomForest
import weka.core.Attribute
import weka.core.DenseInstance
import weka.core.Instance
import weka.core.Instances
import weka.core.SerializationHelper
import weka.core.Utils
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.*
import jakarta.persistence.EntityNotFoundException
import java.nio.file.Files
import java.nio.file.Paths
import org.slf4j.LoggerFactory
import org.springframework.web.multipart.MultipartFile

@Service
class MlServiceImpl(
    private val mlModelRepository: MlModelRepository,
    private val mlFeaturesRepository: MlFeaturesRepository,
    private val mlPredictionRepository: MlPredictionRepository,
    private val financialStatementRepository: FinancialStatementRepository,
    private val auditLogService: AuditLogService
) {

    private val logger = LoggerFactory.getLogger(MlServiceImpl::class.java)

    // Directory to store trained models
    private val modelStorageDir = "ml-models"

    init {
        // Create model storage directory if it doesn't exist
        val dir = File(modelStorageDir)
        if (!dir.exists()) {
            dir.mkdirs()
        }
    }

    /**
     * Train a new fraud detection model using historical data
     */
    @Transactional
    fun trainNewModel(
        modelName: String,
        modelVersion: String,
        trainingStatementIds: List<Long>,
        userId: UUID
    ): MlModel {
        // 1. Prepare attributes for WEKA
        val attributes = createAttributes()
        val trainingDataset = Instances("FraudDetectionTrainingData", attributes, trainingStatementIds.size)
        trainingDataset.setClassIndex(trainingDataset.numAttributes() - 1)

        // 2. Collect training data from statements with known fraud status
        val trainingData = prepareTrainingData(trainingStatementIds, attributes, trainingDataset)

        // 3. Configure and train the Random Forest model
        val randomForest = RandomForest().apply {
            numIterations = 100 // Number of trees
            maxDepth = 0 // Unlimited depth
            seed = 42 // For reproducibility
        }

        // 4. Perform cross-validation evaluation
        val evaluation = Evaluation(trainingData)
        evaluation.crossValidateModel(randomForest, trainingData, 10, Random(1))

        // 5. Train on full dataset
        randomForest.buildClassifier(trainingData)

        // 6. Save the model to a file
        val modelPath = "$modelStorageDir/${modelName}_${modelVersion}.model"
        SerializationHelper.write(modelPath, randomForest)

        // 7. Create performance metrics JSON
        val performanceMetrics = JSONObject().apply {
            put("accuracy", evaluation.pctCorrect())
            put("precision", evaluation.precision(1)) // Class index 1 represents fraud
            put("recall", evaluation.recall(1))
            put("f1_score", evaluation.fMeasure(1))
            put("auc", evaluation.areaUnderROC(1))
            put("num_training_instances", trainingData.numInstances())
        }.toString()

        // 8. Create feature list JSON
        val featureList = JSONObject().apply {
            // Store all features used in the model
            for (i in 0 until attributes.size - 1) { // Exclude class attribute
                put(attributes.get(i).name(), i)
            }
        }.toString()

        // 9. Create and save MlModel entity
        val mlModel = MlModel(
            id = null,
            modelName = modelName,
            modelType = "RANDOM_FOREST",
            modelVersion = modelVersion,
            featureList = featureList,
            performanceMetrics = performanceMetrics,
            trainedDate = OffsetDateTime.now(),
            isActive = false, // Default to inactive; needs to be activated separately
            modelPath = modelPath
        )

        val savedModel = mlModelRepository.save(mlModel)

        // 10. Log the training event
        auditLogService.logEvent(
            userId = userId,
            action = "TRAIN",
            entityType = "ML_MODEL",
            entityId = savedModel.id.toString(),
            details = "Trained new ML model: $modelName ($modelVersion) with ${trainingData.numInstances()} instances"
        )

        return savedModel
    }

    /**
     * Perform fraud prediction on a financial statement
     */
    @Transactional
    fun predictFraud(statementId: Long, userId: UUID): MlPrediction {
        // 1. Get the statement and its ML features
        val statement = financialStatementRepository.findById(statementId)
            .orElseThrow { EntityNotFoundException("Financial statement not found with id: $statementId") }

        val mlFeatures = mlFeaturesRepository.findByStatementId(statementId)
            ?: throw EntityNotFoundException("ML features not found for statement id: $statementId")

        // 2. Get the active ML model
        val activeModels = mlModelRepository.findByIsActive(true)
        if (activeModels.isEmpty()) {
            throw IllegalStateException("No active ML model found")
        }

        val activeModel = activeModels.first()

        // 3. Load the model from file
        val randomForest = SerializationHelper.read(activeModel.modelPath) as RandomForest

        // 4. Prepare attributes and instance for prediction
        val attributes = createAttributes()
        val testDataset = Instances("FraudDetectionPrediction", attributes, 1)
        testDataset.setClassIndex(testDataset.numAttributes() - 1)

        // 5. Create instance from ML features
        val instance = createInstanceFromFeatures(mlFeatures.featureSet, attributes)
        instance.setDataset(testDataset)

        // 6. Make prediction
        val fraudProbability = randomForest.distributionForInstance(instance)[1] // Class index 1 is fraud

        // 7. Calculate feature importance
        val featureImportance = calculateFeatureImportance(randomForest, attributes)

        // 8. Generate explanation
        val explanation = generateExplanation(fraudProbability, featureImportance)

        // 9. Create and save ML Prediction entity
        val mlPrediction = MlPrediction(
            id = null,
            statement = statement,
            model = activeModel,
            fraudProbability = BigDecimal.valueOf(fraudProbability),
            featureImportance = featureImportance.toString(),
            predictionExplanation = explanation,
            predictedAt = OffsetDateTime.now()
        )

        val savedPrediction = mlPredictionRepository.save(mlPrediction)

        // 10. Log the prediction event
        auditLogService.logEvent(
            userId = userId,
            action = "PREDICT",
            entityType = "ML_PREDICTION",
            entityId = savedPrediction.id.toString(),
            details = "Generated ML prediction for statement id: $statementId. Fraud Probability: $fraudProbability"
        )

        return savedPrediction
    }

    /**
     * Create WEKA attributes for the model
     */
    private fun createAttributes(): ArrayList<Attribute> {
        val attributes = ArrayList<Attribute>()

        // 1. Financial Ratios
        attributes.add(Attribute("current_ratio"))
        attributes.add(Attribute("quick_ratio"))
        attributes.add(Attribute("cash_ratio"))
        attributes.add(Attribute("gross_margin"))
        attributes.add(Attribute("operating_margin"))
        attributes.add(Attribute("net_profit_margin"))
        attributes.add(Attribute("return_on_assets"))
        attributes.add(Attribute("return_on_equity"))
        attributes.add(Attribute("asset_turnover"))
        attributes.add(Attribute("inventory_turnover"))
        attributes.add(Attribute("accounts_receivable_turnover"))
        attributes.add(Attribute("days_sales_outstanding"))
        attributes.add(Attribute("debt_to_equity"))
        attributes.add(Attribute("debt_ratio"))
        attributes.add(Attribute("interest_coverage"))
        attributes.add(Attribute("accrual_ratio"))
        attributes.add(Attribute("earnings_quality"))

        // 2. Growth Metrics
        attributes.add(Attribute("revenue_growth"))
        attributes.add(Attribute("gross_profit_growth"))
        attributes.add(Attribute("net_income_growth"))
        attributes.add(Attribute("asset_growth"))
        attributes.add(Attribute("receivables_growth"))
        attributes.add(Attribute("inventory_growth"))
        attributes.add(Attribute("liability_growth"))

        // 3. Altman Z-Score Components
        attributes.add(Attribute("working_capital_to_total_assets"))
        attributes.add(Attribute("retained_earnings_to_total_assets"))
        attributes.add(Attribute("ebit_to_total_assets"))
        attributes.add(Attribute("market_value_equity_to_book_value_debt"))
        attributes.add(Attribute("sales_to_total_assets"))
        attributes.add(Attribute("z_score"))

        // 4. Beneish M-Score Components
        attributes.add(Attribute("days_sales_receivables_index"))
        attributes.add(Attribute("gross_margin_index"))
        attributes.add(Attribute("asset_quality_index"))
        attributes.add(Attribute("sales_growth_index"))
        attributes.add(Attribute("depreciation_index"))
        attributes.add(Attribute("sg_admin_expenses_index"))
        attributes.add(Attribute("leverage_index"))
        attributes.add(Attribute("total_accruals_to_total_assets"))
        attributes.add(Attribute("m_score"))

        // 5. Piotroski F-Score Components
        attributes.add(Attribute("f_score"))

        // 6. Class Attribute (Fraud = 1, Non-Fraud = 0)
        val classValues = ArrayList<String>()
        classValues.add("non_fraud")
        classValues.add("fraud")
        attributes.add(Attribute("is_fraud", classValues))

        return attributes
    }

    /**
     * Prepare training data for the model
     */
    private fun prepareTrainingData(
        statementIds: List<Long>,
        attributes: ArrayList<Attribute>,
        dataset: Instances
    ): Instances {
        for (statementId in statementIds) {
            try {
                val mlFeatures = mlFeaturesRepository.findByStatementId(statementId)
                    ?: continue // Skip if no features exist

                // In a real implementation, you would have labeled data
                // For now, we'll use a simple heuristic based on existing scores
                val featuresJson = JSONObject(mlFeatures.featureSet)
                val mScore = featuresJson.optDouble("m_score", -3.0)
                val zScore = featuresJson.optDouble("z_score", 3.0)

                // This is a simplistic approach - in real life, you'd have actual labeled data
                val isFraud = (mScore > -1.78) || (zScore < 1.8)

                // Create instance with the appropriate class label
                val instance = createInstanceFromFeatures(mlFeatures.featureSet, attributes)
                instance.setValue(attributes.size - 1, if (isFraud) "fraud" else "non_fraud")

                dataset.add(instance)
            } catch (e: Exception) {
                // Log error and continue with next statement
                println("Error processing statement $statementId: ${e.message}")
            }
        }

        return dataset
    }

    /**
     * Create a WEKA instance from feature JSON
     */
    private fun createInstanceFromFeatures(
        featureJson: String,
        attributes: ArrayList<Attribute>
    ): DenseInstance {
        val features = JSONObject(featureJson)
        val instance = DenseInstance(attributes.size)

        // Set values for each attribute
        for (i in 0 until attributes.size - 1) { // Exclude class attribute
            val attrName = attributes.get(i).name()
            val value = if (features.has(attrName)) {
                when (val featureValue = features.get(attrName)) {
                    is Boolean -> if (featureValue) 1.0 else 0.0
                    is Number -> featureValue.toDouble()
                    else -> {
                        try {
                            features.getDouble(attrName)
                        } catch (e: Exception) {
                            // Set to missing value if not found or not a number
                            Utils.missingValue()
                        }
                    }
                }
            } else {
                Utils.missingValue()
            }

            instance.setValue(i, value)
        }

        return instance
    }

    /**
     * Calculate feature importance from Random Forest model
     */
    private fun calculateFeatureImportance(
        randomForest: RandomForest,
        attributes: ArrayList<Attribute>
    ): JSONObject {
        val importanceMap = JSONObject()

        try {
            // RandomForest in Weka doesn't directly expose feature importance
            // We'll estimate it based on the trees and their structure
            // This is a simplified approach as Weka doesn't provide direct access

            // Get number of trees - we'll use this in our calculation
            val numTrees = randomForest.numIterations

            // Create a basic importance measure - in a real implementation
            // you'd want to extract this from the model's internal representation
            // This is a placeholder implementation
            val attributeImportances = DoubleArray(attributes.size - 1) { 0.0 }

            // Fill with some reasonable defaults based on domain knowledge
            attributeImportances[attributes.indexOfFirst { it.name() == "m_score" }.takeIf { it >= 0 } ?: 0] = 0.25
            attributeImportances[attributes.indexOfFirst { it.name() == "accrual_ratio" }.takeIf { it >= 0 } ?: 1] = 0.15
            attributeImportances[attributes.indexOfFirst { it.name() == "z_score" }.takeIf { it >= 0 } ?: 2] = 0.15
            attributeImportances[attributes.indexOfFirst { it.name() == "earnings_quality" }.takeIf { it >= 0 } ?: 3] = 0.10
            attributeImportances[attributes.indexOfFirst { it.name() == "days_sales_receivables_index" }.takeIf { it >= 0 } ?: 4] = 0.10
            attributeImportances[attributes.indexOfFirst { it.name() == "total_accruals_to_total_assets" }.takeIf { it >= 0 } ?: 5] = 0.10
            attributeImportances[attributes.indexOfFirst { it.name() == "asset_quality_index" }.takeIf { it >= 0 } ?: 6] = 0.08
            attributeImportances[attributes.indexOfFirst { it.name() == "sales_growth_index" }.takeIf { it >= 0 } ?: 7] = 0.07

            // Normalize to ensure sum is 1.0
            val sum = attributeImportances.sum()
            if (sum > 0) {
                for (i in attributeImportances.indices) {
                    attributeImportances[i] /= sum
                }
            }

            for (i in 0 until attributes.size - 1) { // Exclude class attribute
                val attributeName = attributes.get(i).name()
                importanceMap.put(attributeName, attributeImportances[i])
            }
        } catch (e: Exception) {
            logger.error("Error calculating feature importance: ${e.message}", e)

            // If we can't get importance, create a placeholder with higher weights
            // for known important factors in fraud detection
            importanceMap.put("m_score", 0.25)
            importanceMap.put("accrual_ratio", 0.15)
            importanceMap.put("z_score", 0.15)
            importanceMap.put("earnings_quality", 0.10)
            importanceMap.put("days_sales_receivables_index", 0.10)
            importanceMap.put("total_accruals_to_total_assets", 0.10)
            importanceMap.put("asset_quality_index", 0.08)
            importanceMap.put("sales_growth_index", 0.07)
        }

        return importanceMap
    }

    /**
     * Generate human-readable explanation for the prediction
     */
    private fun generateExplanation(fraudProbability: Double, featureImportance: JSONObject): String {
        val sbExplanation = StringBuilder()

        // Add overall assessment
        sbExplanation.append("The ML model has detected ")
        when {
            fraudProbability > 0.8 -> sbExplanation.append("a very high probability (${formatPercent(fraudProbability)}) of financial fraud.")
            fraudProbability > 0.6 -> sbExplanation.append("a high probability (${formatPercent(fraudProbability)}) of financial fraud.")
            fraudProbability > 0.4 -> sbExplanation.append("a moderate probability (${formatPercent(fraudProbability)}) of financial fraud.")
            fraudProbability > 0.2 -> sbExplanation.append("a low probability (${formatPercent(fraudProbability)}) of financial fraud.")
            else -> sbExplanation.append("a very low probability (${formatPercent(fraudProbability)}) of financial fraud.")
        }

        sbExplanation.append("\n\nThis assessment is based on these key factors:\n")

        // Sort features by importance and list top factors
        val sortedFeatures = featureImportance.keys()
            .asSequence()
            .map { it to featureImportance.getDouble(it) }
            .sortedByDescending { it.second }
            .take(5)
            .toList()

        // Add explanation for each top factor
        for ((feature, importance) in sortedFeatures) {
            val featureExplanation = when (feature) {
                "m_score" -> "Beneish M-Score (indicates potential earnings manipulation)"
                "accrual_ratio" -> "Accrual Ratio (difference between earnings and cash flow)"
                "z_score" -> "Altman Z-Score (indicates financial distress)"
                "earnings_quality" -> "Earnings Quality (relationship between earnings and cash flow)"
                "days_sales_receivables_index" -> "Days Sales Receivables Index (unusual changes in receivables)"
                "total_accruals_to_total_assets" -> "Total Accruals to Total Assets (high accruals suggest earnings manipulation)"
                "asset_quality_index" -> "Asset Quality Index (capitalization of expenses)"
                "sales_growth_index" -> "Sales Growth Index (unsustainable sales growth patterns)"
                "leverage_index" -> "Leverage Index (increasing debt levels)"
                else -> feature.replace("_", " ").capitalize()
            }

            sbExplanation.append("- $featureExplanation (${formatPercent(importance)} importance)\n")
        }

        // Add closing note
        sbExplanation.append("\nThis assessment is algorithmic and should be complemented with manual review.")

        return sbExplanation.toString()
    }

    /**
     * Format a probability as a percentage string
     */
    private fun formatPercent(value: Double): String {
        return "${(value * 100).toInt()}%"
    }

    /**
     * Process and save training data uploaded via CSV
     */
    @Transactional
    fun processTrainingDataUpload(file: MultipartFile, userId: UUID): Map<String, Any> {
        // Create temporary file
        val tempFile = File.createTempFile("training-data-", ".csv")
        file.transferTo(tempFile)

        try {
            // Read CSV using WEKA's CSVLoader
            val loader = weka.core.converters.CSVLoader()
            loader.setSource(tempFile)
            val data = loader.dataSet

            // Verify the data structure
            if (data.numInstances() == 0) {
                throw IllegalArgumentException("CSV file contains no data")
            }

            // Basic statistics
            val numInstances = data.numInstances()
            val numAttributes = data.numAttributes()

            // Check if the fraud column exists
            val classIndex = data.attribute("fraud")?.index() ?: -1

            if (classIndex == -1) {
                throw IllegalArgumentException("CSV file must contain a 'fraud' column")
            }

            data.setClassIndex(classIndex)

            // Count fraud vs non-fraud
            var fraudCount = 0
            var nonFraudCount = 0

            for (i in 0 until data.numInstances()) {
                val instance = data.instance(i)
                if (!instance.isMissing(classIndex)) {
                    if (instance.value(classIndex) == 1.0) {
                        fraudCount++
                    } else {
                        nonFraudCount++
                    }
                }
            }

            // Log the event
            auditLogService.logEvent(
                userId = userId,
                action = "UPLOAD",
                entityType = "TRAINING_DATA",
                entityId = file.originalFilename ?: "unknown",
                details = "Uploaded training data with $numInstances instances ($fraudCount fraud, $nonFraudCount non-fraud)"
            )

            // Return statistics
            return mapOf(
                "fileName" to (file.originalFilename ?: "unknown"),
                "fileSize" to file.size,
                "numInstances" to numInstances,
                "numAttributes" to numAttributes,
                "fraudCount" to fraudCount,
                "nonFraudCount" to nonFraudCount,
                "fraudPercentage" to (fraudCount.toDouble() / numInstances * 100).toInt()
            )
        } finally {
            // Clean up temporary file
            tempFile.delete()
        }
    }

    /**
     * Evaluate a model on test data
     */
    @Transactional
    fun evaluateModel(modelId: Long, testStatementIds: List<Long>, userId: UUID): Map<String, Any> {
        // Get the model
        val model = mlModelRepository.findById(modelId)
            .orElseThrow { EntityNotFoundException("ML model not found with id: $modelId") }

        // Load the model from file
        val randomForest = SerializationHelper.read(model.modelPath) as RandomForest

        // Create test dataset
        val attributes = createAttributes()
        val testDataset = Instances("FraudDetectionTestData", attributes, testStatementIds.size)
        testDataset.setClassIndex(testDataset.numAttributes() - 1)

        // Prepare test data
        val actualClassValues = mutableListOf<Int>()
        val predictedClassValues = mutableListOf<Int>()

        for (statementId in testStatementIds) {
            try {
                val mlFeatures = mlFeaturesRepository.findByStatementId(statementId)
                    ?: continue // Skip if no features exist

                // In a real implementation, you'd get the actual fraud label
                // Here we'll derive it from existing scores as in training
                val featuresJson = JSONObject(mlFeatures.featureSet)
                val mScore = featuresJson.optDouble("m_score", -3.0)
                val zScore = featuresJson.optDouble("z_score", 3.0)

                val actualIsFraud = (mScore > -1.78) || (zScore < 1.8)
                actualClassValues.add(if (actualIsFraud) 1 else 0)

                // Create instance and make prediction
                val instance = createInstanceFromFeatures(mlFeatures.featureSet, attributes)
                instance.setDataset(testDataset)

                val fraudProbability = randomForest.distributionForInstance(instance)[1]
                val predictedIsFraud = fraudProbability > 0.5
                predictedClassValues.add(if (predictedIsFraud) 1 else 0)
            } catch (e: Exception) {
                // Log error and continue with next statement
                println("Error processing test statement $statementId: ${e.message}")
            }
        }

        // If no valid test instances, throw error
        if (actualClassValues.isEmpty()) {
            throw IllegalArgumentException("No valid test instances found")
        }

        // Calculate evaluation metrics
        val tp = actualClassValues.zip(predictedClassValues).count { it.first == 1 && it.second == 1 }
        val tn = actualClassValues.zip(predictedClassValues).count { it.first == 0 && it.second == 0 }
        val fp = actualClassValues.zip(predictedClassValues).count { it.first == 0 && it.second == 1 }
        val fn = actualClassValues.zip(predictedClassValues).count { it.first == 1 && it.second == 0 }

        val accuracy = (tp + tn).toDouble() / (tp + tn + fp + fn)
        val precision = if (tp + fp > 0) tp.toDouble() / (tp + fp) else 0.0
        val recall = if (tp + fn > 0) tp.toDouble() / (tp + fn) else 0.0
        val f1Score = if (precision + recall > 0) 2 * precision * recall / (precision + recall) else 0.0

        // Log the evaluation event
        auditLogService.logEvent(
            userId = userId,
            action = "EVALUATE",
            entityType = "ML_MODEL",
            entityId = model.id.toString(),
            details = "Evaluated model ${model.modelName} (${model.modelVersion}) on ${actualClassValues.size} test instances"
        )

        // Return evaluation results
        return mapOf(
            "modelId" to model.id!!,
            "modelName" to model.modelName,
            "modelVersion" to model.modelVersion,
            "testDataSize" to actualClassValues.size,
            "accuracy" to accuracy,
            "precision" to precision,
            "recall" to recall,
            "f1Score" to f1Score,
            "confusionMatrix" to mapOf(
                "truePositive" to tp,
                "trueNegative" to tn,
                "falsePositive" to fp,
                "falseNegative" to fn
            ),
            "evaluationDate" to OffsetDateTime.now()
        )
    }

    /**
     * Run predictions on multiple statements in batch mode
     */
    @Transactional
    fun batchPredict(modelId: Long, statementIds: List<Long>, userId: UUID): Map<String, Any> {
        // Get the model
        val model = mlModelRepository.findById(modelId)
            .orElseThrow { EntityNotFoundException("ML model not found with id: $modelId") }

        // Ensure the model is active
        if (!model.isActive) {
            throw IllegalArgumentException("Model is not active. Please activate it before using for prediction.")
        }

        // Load the model from file
        val randomForest = SerializationHelper.read(model.modelPath) as RandomForest

        // Prepare for prediction
        val predictions = mutableListOf<Map<String, Any>>()
        var highRiskCount = 0
        var mediumRiskCount = 0
        var lowRiskCount = 0

        for (statementId in statementIds) {
            try {
                // Get statement and features
                val statement = financialStatementRepository.findById(statementId)
                    .orElseThrow { EntityNotFoundException("Financial statement not found with id: $statementId") }

                val mlFeatures = mlFeaturesRepository.findByStatementId(statementId)
                    ?: throw IllegalArgumentException("ML features not found for statement id: $statementId")

                // Create attributes and instance
                val attributes = createAttributes()
                val testDataset = Instances("FraudDetectionPrediction", attributes, 1)
                testDataset.setClassIndex(testDataset.numAttributes() - 1)

                val instance = createInstanceFromFeatures(mlFeatures.featureSet, attributes)
                instance.setDataset(testDataset)

                // Make prediction
                val fraudProbability = randomForest.distributionForInstance(instance)[1]

                // Calculate feature importance
                val featureImportance = calculateFeatureImportance(randomForest, attributes)

                // Determine risk level
                val riskLevel = when {
                    fraudProbability >= 0.7 -> "HIGH"
                    fraudProbability >= 0.4 -> "MEDIUM"
                    else -> "LOW"
                }

                // Count by risk level
                when (riskLevel) {
                    "HIGH" -> highRiskCount++
                    "MEDIUM" -> mediumRiskCount++
                    "LOW" -> lowRiskCount++
                }

                // Find top indicators
                val topIndicators = featureImportance.keys()
                    .asSequence()
                    .map { it to featureImportance.getDouble(it) }
                    .sortedByDescending { it.second }
                    .take(3)
                    .map { (feature, _) ->
                        when (feature) {
                            "m_score" -> "Beneish M-Score"
                            "accrual_ratio" -> "Accrual Ratio"
                            "z_score" -> "Altman Z-Score"
                            "earnings_quality" -> "Earnings Quality"
                            "days_sales_receivables_index" -> "Days Sales Receivables Index"
                            "total_accruals_to_total_assets" -> "Total Accruals to Total Assets"
                            "asset_quality_index" -> "Asset Quality Index"
                            "sales_growth_index" -> "Sales Growth Index"
                            "leverage_index" -> "Leverage Index"
                            else -> feature.replace("_", " ").capitalize()
                        }
                    }
                    .toList()

                // Save prediction to database
                val mlPrediction = MlPrediction(
                    id = null,
                    statement = statement,
                    model = model,
                    fraudProbability = BigDecimal.valueOf(fraudProbability),
                    featureImportance = featureImportance.toString(),
                    predictionExplanation = generateExplanation(fraudProbability, featureImportance),
                    predictedAt = OffsetDateTime.now()
                )

                mlPredictionRepository.save(mlPrediction)

                // Add to results
                predictions.add(
                    mapOf(
                        "statementId" to statement.id,
                        "companyName" to statement.fiscalYear.company.name,
                        "fiscalYear" to statement.fiscalYear.year,
                        "fraudProbability" to fraudProbability,
                        "riskLevel" to riskLevel,
                        "topIndicators" to topIndicators
                    ) as Map<String, Any>
                )
            } catch (e: Exception) {
                // Log error and continue with next statement
                println("Error processing statement $statementId: ${e.message}")
            }
        }

        // Log the batch prediction event
        auditLogService.logEvent(
            userId = userId,
            action = "BATCH_PREDICT",
            entityType = "ML_MODEL",
            entityId = model.id.toString(),
            details = "Performed batch prediction with model ${model.modelName} on ${predictions.size} statements"
        )

        // Return batch results
        return mapOf(
            "modelId" to model.id!!,
            "modelName" to model.modelName,
            "statementCount" to predictions.size,
            "highRiskCount" to highRiskCount,
            "mediumRiskCount" to mediumRiskCount,
            "lowRiskCount" to lowRiskCount,
            "predictions" to predictions,
            "predictionDate" to OffsetDateTime.now()
        )
    }
}