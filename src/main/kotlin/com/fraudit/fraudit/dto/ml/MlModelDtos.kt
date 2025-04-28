package com.fraudit.fraudit.dto.ml

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime
import java.util.UUID

data class MlModelRequest(
    val modelName: String,
    val modelType: String = "RANDOM_FOREST",
    val modelVersion: String,
    val featureList: String,
    val performanceMetrics: String,
    val isActive: Boolean = false,
    val modelPath: String
)

data class MlModelResponse(
    val id: Long,
    val modelName: String,
    val modelType: String,
    val modelVersion: String,
    val featureList: String,
    val performanceMetrics: String,
    val trainedDate: OffsetDateTime,
    val isActive: Boolean,
    val modelPath: String,
    val createdById: UUID?,
    val createdByUsername: String?
)

data class MlModelSummaryResponse(
    val id: Long,
    val modelName: String,
    val modelVersion: String,
    val modelType: String,
    val isActive: Boolean,
    val trainedDate: OffsetDateTime
)


/**
 * Request DTO for training a new ML model
 */
data class MlTrainingRequest(
    @field:NotBlank(message = "Model name is required")
    @field:Size(min = 3, max = 100, message = "Model name must be between 3 and 100 characters")
    val modelName: String,

    @field:NotBlank(message = "Model version is required")
    @field:Size(min = 1, max = 50, message = "Model version must be between 1 and 50 characters")
    val modelVersion: String,

    @field:NotEmpty(message = "At least one training statement ID is required")
    val trainingStatementIds: List<Long>
)

/**
 * Response DTO for model training results
 */
data class MlTrainingResponse(
    val modelId: Long,
    val modelName: String,
    val modelVersion: String,
    val trainedDate: OffsetDateTime,
    val accuracy: Double?,
    val precision: Double?,
    val recall: Double?,
    val f1Score: Double?,
    val auc: Double?,
    val trainingDataSize: Int
)

/**
 * Response DTO for model evaluation results
 */
data class MlModelEvaluationResponse(
    val modelId: Long,
    val modelName: String,
    val testDataSize: Int,
    val accuracy: Double,
    val precision: Double,
    val recall: Double,
    val f1Score: Double,
    val auc: Double,
    val confusionMatrix: Map<String, Int>,
    val evaluationDate: OffsetDateTime
)

/**
 * Request DTO for feature importance analysis
 */
data class FeatureImportanceRequest(
    val modelId: Long,
    val topN: Int = 10
)

/**
 * Response DTO for feature importance analysis
 */
data class FeatureImportanceResponse(
    val modelId: Long,
    val modelName: String,
    val features: List<FeatureImportance>
)

/**
 * DTO for feature importance details
 */
data class FeatureImportance(
    val featureName: String,
    val importance: Double,
    val description: String
)

/**
 * Response DTO for batch prediction results
 */
data class BatchPredictionResponse(
    val modelId: Long,
    val statementCount: Int,
    val highRiskCount: Int,
    val mediumRiskCount: Int,
    val lowRiskCount: Int,
    val predictions: List<StatementPrediction>,
    val predictionDate: OffsetDateTime
)

/**
 * DTO for individual statement prediction in batch process
 */
data class StatementPrediction(
    val statementId: Long,
    val companyName: String,
    val fiscalYear: Int,
    val fraudProbability: Double,
    val riskLevel: String,
    val topIndicators: List<String>
)


/**
 * Request DTO for batch feature generation
 */
data class GenerateFeaturesRequest(
    @field:NotEmpty(message = "At least one statement ID is required")
    @field:Size(max = 5000, message = "Maximum of 5000 statement IDs allowed per batch")
    val statementIds: List<Long>
)

/**
 * Response DTO for batch feature generation
 */
data class GenerateFeaturesResponse(
    val totalStatements: Int,
    val processedCount: Int,
    val successCount: Int,
    val failureCount: Int,
    val errors: Map<Long, String>
)

/**
 * Response DTO for feature details
 */
data class FeatureDetailsResponse(
    val featuresId: Long,
    val statementId: Long,
    val featureCount: Int,
    val createdAt: java.time.OffsetDateTime
)