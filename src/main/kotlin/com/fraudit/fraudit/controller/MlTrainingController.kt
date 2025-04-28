package com.fraudit.fraudit.controller

import com.fraudit.fraudit.domain.entity.MlModel
import com.fraudit.fraudit.dto.common.ApiResponse
import com.fraudit.fraudit.dto.ml.MlModelResponse
import com.fraudit.fraudit.dto.ml.MlTrainingRequest
import com.fraudit.fraudit.dto.ml.MlTrainingResponse
import com.fraudit.fraudit.service.MlModelService
import com.fraudit.fraudit.service.impl.MlServiceImpl
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/api/ml/training")
@PreAuthorize("hasRole('ADMIN')")
class MlTrainingController(
    private val mlModelService: MlModelService,
    private val mlService: MlServiceImpl
) {
    private val logger = LoggerFactory.getLogger(MlTrainingController::class.java)

    @PostMapping("/train")
    fun trainNewModel(
        @Valid @RequestBody request: MlTrainingRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<MlTrainingResponse>> {
        try {
            val userId = UUID.fromString(userDetails.username)

            // Train the new model
            val trainedModel = mlService.trainNewModel(
                modelName = request.modelName,
                modelVersion = request.modelVersion,
                trainingStatementIds = request.trainingStatementIds,
                userId = userId
            )

            // Create response with model details and performance metrics
            val performanceMetrics = org.json.JSONObject(trainedModel.performanceMetrics)

            val response = MlTrainingResponse(
                modelId = trainedModel.id!!,
                modelName = trainedModel.modelName,
                modelVersion = trainedModel.modelVersion,
                trainedDate = trainedModel.trainedDate,
                accuracy = if (performanceMetrics.isNull("accuracy")) null else performanceMetrics.optDouble("accuracy", 0.0),
                precision = if (performanceMetrics.isNull("precision")) null else performanceMetrics.optDouble("precision", 0.0),
                recall = if (performanceMetrics.isNull("recall")) null else performanceMetrics.optDouble("recall", 0.0),
                f1Score = if (performanceMetrics.isNull("f1_score")) null else performanceMetrics.optDouble("f1_score", 0.0),
                auc = if (performanceMetrics.isNull("auc")) null else performanceMetrics.optDouble("auc", 0.0),
                trainingDataSize = performanceMetrics.optInt("num_training_instances", 0)
            )

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Model trained successfully",
                    data = response
                )
            )
        } catch (e: Exception) {
            logger.error("Error training model: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error training model",
                    errors = listOf(e.message ?: "An unexpected error occurred")
                )
            )
        }
    }

    @PostMapping("/upload-training-data")
    fun uploadTrainingData(
        @RequestParam("file") file: MultipartFile,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        try {
            val userId = UUID.fromString(userDetails.username)

            // Process and save the training data
            val dataStats = mlService.processTrainingDataUpload(file, userId)

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Training data uploaded successfully",
                    data = dataStats
                )
            )
        } catch (e: Exception) {
            logger.error("Error uploading training data: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse(
                    success = false,
                    message = "Error uploading training data",
                    errors = listOf(e.message ?: "An unexpected error occurred")
                )
            )
        }
    }

    @GetMapping("/evaluate/{modelId}")
    fun evaluateModel(
        @PathVariable modelId: Long,
        @RequestParam testStatementIds: List<Long>,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        try {
            val userId = UUID.fromString(userDetails.username)

            // Evaluate the model on test data
            val evaluationResults = mlService.evaluateModel(modelId, testStatementIds, userId)

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Model evaluated successfully",
                    data = evaluationResults
                )
            )
        } catch (e: Exception) {
            logger.error("Error evaluating model: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error evaluating model",
                    errors = listOf(e.message ?: "An unexpected error occurred")
                )
            )
        }
    }

    @PostMapping("/batch-predict")
    fun batchPrediction(
        @RequestParam modelId: Long,
        @RequestParam statementIds: List<Long>,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        try {
            val userId = UUID.fromString(userDetails.username)

            // Run batch prediction
            val batchResults = mlService.batchPredict(modelId, statementIds, userId)

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Batch prediction completed successfully",
                    data = batchResults
                )
            )
        } catch (e: Exception) {
            logger.error("Error running batch prediction: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error running batch prediction",
                    errors = listOf(e.message ?: "An unexpected error occurred")
                )
            )
        }
    }
}