package com.fraudit.fraudit.controller

import com.fraudit.fraudit.dto.common.ApiResponse
import com.fraudit.fraudit.dto.ml.GenerateFeaturesRequest
import com.fraudit.fraudit.dto.ml.GenerateFeaturesResponse
import com.fraudit.fraudit.service.impl.MlFeatureService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/ml/features")
@PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
class MlFeaturesController(
    private val mlFeatureService: MlFeatureService
) {
    private val logger = LoggerFactory.getLogger(MlFeaturesController::class.java)

    @PostMapping("/generate")
    fun generateFeatures(
        @RequestParam statementId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        try {
            val userId = UUID.fromString(userDetails.username)
            logger.info("Generating ML features for statement ID: $statementId")

            val features = mlFeatureService.generateFeaturesForStatement(statementId, userId)

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "ML features generated successfully",
                    data = mapOf(
                        "statementId" to statementId,
                        "featuresId" to features.id!!,
                        "featureCount" to features.featureSet.count { it == ':' }
                    )
                )
            )
        } catch (e: Exception) {
            logger.error("Error generating ML features for statement ID $statementId: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error generating ML features",
                    errors = listOf(e.message ?: "An unexpected error occurred")
                )
            )
        }
    }

    @PostMapping("/generate-batch")
    fun generateFeaturesBatch(
        @Valid @RequestBody request: GenerateFeaturesRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<GenerateFeaturesResponse>> {
        try {
            val userId = UUID.fromString(userDetails.username)
            logger.info("Generating ML features for ${request.statementIds.size} statements")

            val results = mlFeatureService.generateFeaturesForStatements(request.statementIds, userId)

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Batch ML feature generation completed",
                    data = GenerateFeaturesResponse(
                        totalStatements = request.statementIds.size,
                        processedCount = results.processedCount,
                        successCount = results.successCount,
                        failureCount = results.failureCount,
                        errors = results.errors
                    )
                )
            )
        } catch (e: Exception) {
            logger.error("Error in batch ML feature generation: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error in batch ML feature generation",
                    errors = listOf(e.message ?: "An unexpected error occurred")
                )
            )
        }
    }

    @GetMapping("/check-batch")
    fun checkFeatureAvailability(
        @RequestParam statementIds: List<Long>,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        try {
            val results = mlFeatureService.checkFeaturesExistence(statementIds)

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Feature availability check completed",
                    data = mapOf(
                        "totalChecked" to statementIds.size,
                        "featuresExist" to results.count { it.value },
                        "featuresMissing" to results.count { !it.value },
                        "statementStatus" to results
                    )
                )
            )
        } catch (e: Exception) {
            logger.error("Error checking feature availability: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error checking feature availability",
                    errors = listOf(e.message ?: "An unexpected error occurred")
                )
            )
        }
    }
}