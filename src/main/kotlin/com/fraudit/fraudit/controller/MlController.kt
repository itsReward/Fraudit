package com.fraudit.fraudit.controller

import com.fraudit.fraudit.dto.common.ApiResponse
import com.fraudit.fraudit.dto.ml.*
import com.fraudit.fraudit.service.AuditLogService
import com.fraudit.fraudit.service.FinancialAnalysisService
import com.fraudit.fraudit.service.FinancialStatementService
import com.fraudit.fraudit.service.MlModelService
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.util.UUID
import jakarta.validation.Valid

@RestController
@RequestMapping("/api/ml")
class MlController(
    private val mlModelService: MlModelService,
    private val financialStatementService: FinancialStatementService,
    private val financialAnalysisService: FinancialAnalysisService,
    private val auditLogService: AuditLogService
) {
    private val logger = LoggerFactory.getLogger(MlController::class.java)

    @GetMapping("/models")
    fun getAllModels(@RequestParam(required = false) isActive: Boolean?): ResponseEntity<ApiResponse<List<MlModelSummaryResponse>>> {
        try {
            val models = if (isActive != null) {
                // Use the correct method findActiveModels() or create a dedicated filter
                if (isActive) {
                    mlModelService.findActiveModels()
                } else {
                    // If looking for inactive models, get all and filter
                    mlModelService.findAll().filter { !it.isActive }
                }
            } else {
                mlModelService.findAll()
            }

            val modelResponses = models.map { model ->
                MlModelSummaryResponse(
                    id = model.id!!,
                    modelName = model.modelName,
                    modelVersion = model.modelVersion,
                    modelType = model.modelType,
                    isActive = model.isActive,
                    trainedDate = model.trainedDate
                )
            }

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "ML models retrieved successfully",
                    data = modelResponses
                )
            )
        } catch (e: Exception) {
            logger.error("Error retrieving ML models: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error retrieving ML models",
                    errors = listOf(e.message ?: "An unexpected error occurred")
                )
            )
        }
    }

    @GetMapping("/models/{id}")
    fun getModelById(@PathVariable id: Long): ResponseEntity<ApiResponse<MlModelResponse>> {
        try {
            val model = mlModelService.findById(id)

            val modelResponse = MlModelResponse(
                id = model.id!!,
                modelName = model.modelName,
                modelType = model.modelType,
                modelVersion = model.modelVersion,
                featureList = model.featureList,
                performanceMetrics = model.performanceMetrics,
                trainedDate = model.trainedDate,
                isActive = model.isActive,
                modelPath = model.modelPath,
                createdById = model.createdBy?.id,
                createdByUsername = model.createdBy?.username
            )

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "ML model retrieved successfully",
                    data = modelResponse
                )
            )
        } catch (e: Exception) {
            logger.error("Error retrieving ML model: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse(
                    success = false,
                    message = "ML model not found",
                    errors = listOf(e.message ?: "Model not found")
                )
            )
        }
    }

    @PostMapping("/models")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    fun createModel(
        @Valid @RequestBody mlModelRequest: MlModelRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<MlModelResponse>> {
        try {
            val userId = UUID.fromString(userDetails.username)

            // Create model
            val model = com.fraudit.fraudit.domain.entity.MlModel(
                id = null,
                modelName = mlModelRequest.modelName,
                modelType = mlModelRequest.modelType,
                modelVersion = mlModelRequest.modelVersion,
                featureList = mlModelRequest.featureList,
                performanceMetrics = mlModelRequest.performanceMetrics,
                isActive = mlModelRequest.isActive,
                modelPath = mlModelRequest.modelPath
            )

            val createdModel = mlModelService.createModel(model, userId)

            val modelResponse = MlModelResponse(
                id = createdModel.id!!,
                modelName = createdModel.modelName,
                modelType = createdModel.modelType,
                modelVersion = createdModel.modelVersion,
                featureList = createdModel.featureList,
                performanceMetrics = createdModel.performanceMetrics,
                trainedDate = createdModel.trainedDate,
                isActive = createdModel.isActive,
                modelPath = createdModel.modelPath,
                createdById = createdModel.createdBy?.id,
                createdByUsername = createdModel.createdBy?.username
            )

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse(
                    success = true,
                    message = "ML model created successfully",
                    data = modelResponse
                )
            )
        } catch (e: Exception) {
            logger.error("Error creating ML model: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse(
                    success = false,
                    message = "Error creating ML model",
                    errors = listOf(e.message ?: "An unexpected error occurred")
                )
            )
        }
    }

    @PutMapping("/models/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    fun updateModel(
        @PathVariable id: Long,
        @Valid @RequestBody mlModelRequest: MlModelRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<MlModelResponse>> {
        try {
            val userId = UUID.fromString(userDetails.username)

            // Get existing model
            val existingModel = mlModelService.findById(id)

            // Update model properties
            val model = existingModel.copy(
                modelName = mlModelRequest.modelName,
                modelType = mlModelRequest.modelType,
                modelVersion = mlModelRequest.modelVersion,
                featureList = mlModelRequest.featureList,
                performanceMetrics = mlModelRequest.performanceMetrics,
                isActive = mlModelRequest.isActive,
                modelPath = mlModelRequest.modelPath
            )

            val updatedModel = mlModelService.updateModel(model, userId)

            val modelResponse = MlModelResponse(
                id = updatedModel.id!!,
                modelName = updatedModel.modelName,
                modelType = updatedModel.modelType,
                modelVersion = updatedModel.modelVersion,
                featureList = updatedModel.featureList,
                performanceMetrics = updatedModel.performanceMetrics,
                trainedDate = updatedModel.trainedDate,
                isActive = updatedModel.isActive,
                modelPath = updatedModel.modelPath,
                createdById = updatedModel.createdBy?.id,
                createdByUsername = updatedModel.createdBy?.username
            )

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "ML model updated successfully",
                    data = modelResponse
                )
            )
        } catch (e: Exception) {
            logger.error("Error updating ML model: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse(
                    success = false,
                    message = "Error updating ML model",
                    errors = listOf(e.message ?: "An unexpected error occurred")
                )
            )
        }
    }

    @PutMapping("/models/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    fun activateModel(
        @PathVariable id: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<MlModelResponse>> {
        try {
            val userId = UUID.fromString(userDetails.username)

            val activatedModel = mlModelService.activateModel(id, userId)

            val modelResponse = MlModelResponse(
                id = activatedModel.id!!,
                modelName = activatedModel.modelName,
                modelType = activatedModel.modelType,
                modelVersion = activatedModel.modelVersion,
                featureList = activatedModel.featureList,
                performanceMetrics = activatedModel.performanceMetrics,
                trainedDate = activatedModel.trainedDate,
                isActive = activatedModel.isActive,
                modelPath = activatedModel.modelPath,
                createdById = activatedModel.createdBy?.id,
                createdByUsername = activatedModel.createdBy?.username
            )

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "ML model activated successfully",
                    data = modelResponse
                )
            )
        } catch (e: Exception) {
            logger.error("Error activating ML model: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse(
                    success = false,
                    message = "Error activating ML model",
                    errors = listOf(e.message ?: "An unexpected error occurred")
                )
            )
        }
    }

    @PutMapping("/models/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    fun deactivateModel(
        @PathVariable id: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<MlModelResponse>> {
        try {
            val userId = UUID.fromString(userDetails.username)

            val deactivatedModel = mlModelService.deactivateModel(id, userId)

            val modelResponse = MlModelResponse(
                id = deactivatedModel.id!!,
                modelName = deactivatedModel.modelName,
                modelType = deactivatedModel.modelType,
                modelVersion = deactivatedModel.modelVersion,
                featureList = deactivatedModel.featureList,
                performanceMetrics = deactivatedModel.performanceMetrics,
                trainedDate = deactivatedModel.trainedDate,
                isActive = deactivatedModel.isActive,
                modelPath = deactivatedModel.modelPath,
                createdById = deactivatedModel.createdBy?.id,
                createdByUsername = deactivatedModel.createdBy?.username
            )

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "ML model deactivated successfully",
                    data = modelResponse
                )
            )
        } catch (e: Exception) {
            logger.error("Error deactivating ML model: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse(
                    success = false,
                    message = "Error deactivating ML model",
                    errors = listOf(e.message ?: "An unexpected error occurred")
                )
            )
        }
    }

    @DeleteMapping("/models/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteModel(
        @PathVariable id: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Void>> {
        try {
            val userId = UUID.fromString(userDetails.username)

            mlModelService.deleteModel(id, userId)

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "ML model deleted successfully"
                )
            )
        } catch (e: Exception) {
            logger.error("Error deleting ML model: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse(
                    success = false,
                    message = "Error deleting ML model",
                    errors = listOf(e.message ?: "An unexpected error occurred")
                )
            )
        }
    }

    @GetMapping("/predictions/statement/{statementId}")
    fun getPredictionsByStatementId(@PathVariable statementId: Long): ResponseEntity<ApiResponse<List<MlPredictionResponse>>> {
        try {
            // First verify statement exists
            val statement = financialStatementService.findById(statementId)

            // Get all predictions for the statement
            val predictions = statement.mlPredictions.toList()

            if (predictions.isEmpty()) {
                return ResponseEntity.ok(
                    ApiResponse(
                        success = true,
                        message = "No ML predictions found for statement id: $statementId",
                        data = emptyList()
                    )
                )
            }

            val predictionResponses = predictions.map { prediction ->
                // Parse feature importance from JSON string
                val featureImportance = try {
                    val jsonObject = JSONObject(prediction.featureImportance ?: "{}")
                    val map = mutableMapOf<String, Double>()
                    for (key in jsonObject.keys()) {
                        map[key] = jsonObject.getDouble(key)
                    }
                    map
                } catch (e: Exception) {
                    null
                }

                MlPredictionResponse(
                    id = prediction.id!!,
                    statementId = prediction.statement.id!!,
                    companyId = prediction.statement.fiscalYear.company.id!!,
                    companyName = prediction.statement.fiscalYear.company.name,
                    year = prediction.statement.fiscalYear.year,
                    modelId = prediction.model.id!!,
                    modelName = prediction.model.modelName,
                    modelVersion = prediction.model.modelVersion,
                    fraudProbability = prediction.fraudProbability,
                    featureImportance = featureImportance,
                    predictionExplanation = prediction.predictionExplanation,
                    predictedAt = prediction.predictedAt
                )
            }

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "ML predictions retrieved successfully",
                    data = predictionResponses
                )
            )
        } catch (e: Exception) {
            logger.error("Error retrieving ML predictions: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse(
                    success = false,
                    message = "Error retrieving ML predictions",
                    errors = listOf(e.message ?: "Statement not found or error occurred")
                )
            )
        }
    }

    @PostMapping("/predict/{statementId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    fun performPrediction(
        @PathVariable statementId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<MlPredictionResponse>> {
        try {
            val userId = UUID.fromString(userDetails.username)

            // Verify statement exists and check permissions
            val statement = financialStatementService.findById(statementId)
            if (statement.user.id != userId && !userDetails.authorities.any { it.authority == "ROLE_ADMIN" }) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiResponse(
                        success = false,
                        message = "You don't have permission to run predictions on this statement",
                        errors = listOf("Insufficient permissions")
                    )
                )
            }

            // Verify financial data and ML features exist
            if (statement.financialData == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse(
                        success = false,
                        message = "Financial data must be entered before running ML predictions",
                        errors = listOf("Missing financial data")
                    )
                )
            }

            if (statement.mlFeatures == null) {
                // Prepare ML features if they don't exist
                financialAnalysisService.prepareMlFeatures(statementId, userId)
            }

            // Check if there are active models
            val activeModels = mlModelService.findActiveModels()
            if (activeModels.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse(
                        success = false,
                        message = "No active ML models available for prediction",
                        errors = listOf("No active models")
                    )
                )
            }

            // Perform ML prediction
            val prediction = financialAnalysisService.performMlPrediction(statementId, userId)

            // Parse feature importance from JSON string
            val featureImportance = try {
                val jsonObject = JSONObject(prediction.featureImportance ?: "{}")
                val map = mutableMapOf<String, Double>()
                for (key in jsonObject.keys()) {
                    map[key] = jsonObject.getDouble(key)
                }
                map
            } catch (e: Exception) {
                null
            }

            val predictionResponse = MlPredictionResponse(
                id = prediction.id!!,
                statementId = prediction.statement.id!!,
                companyId = prediction.statement.fiscalYear.company.id!!,
                companyName = prediction.statement.fiscalYear.company.name,
                year = prediction.statement.fiscalYear.year,
                modelId = prediction.model.id!!,
                modelName = prediction.model.modelName,
                modelVersion = prediction.model.modelVersion,
                fraudProbability = prediction.fraudProbability,
                featureImportance = featureImportance,
                predictionExplanation = prediction.predictionExplanation,
                predictedAt = prediction.predictedAt
            )

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "ML prediction performed successfully",
                    data = predictionResponse
                )
            )
        } catch (e: Exception) {
            logger.error("Error performing ML prediction: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error performing ML prediction",
                    errors = listOf(e.message ?: "An unexpected error occurred")
                )
            )
        }
    }
}