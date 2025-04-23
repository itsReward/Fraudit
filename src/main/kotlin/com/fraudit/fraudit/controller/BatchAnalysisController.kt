package com.fraudit.fraudit.controller

import com.fraudit.fraudit.dto.common.ApiResponse
import com.fraudit.fraudit.service.impl.BatchAnalysisService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import java.util.UUID
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("/api/admin/batch")
@PreAuthorize("hasRole('ADMIN')")
class BatchAnalysisController(
    private val batchAnalysisService: BatchAnalysisService
) {
    private val logger = LoggerFactory.getLogger(BatchAnalysisController::class.java)

    /**
     * Start batch processing of all financial statements with PROCESSED status
     */
    @PostMapping("/analyze-all")
    fun processBatchAll(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        val userId = UUID.fromString(userDetails.username)
        logger.info("Admin $userId starting batch analysis of all financial statements")

        // Start async batch processing
        val processingFuture = batchAnalysisService.processAllStatementsBatch()

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Batch processing started successfully",
                data = mapOf(
                    "status" to "STARTED",
                    "message" to "Batch processing is running in the background"
                )
            )
        )
    }

    /**
     * Process all financial statements for a specific company
     */
    @PostMapping("/analyze-company/{companyId}")
    fun processCompanyBatch(
        @PathVariable companyId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        try {
            val userId = UUID.fromString(userDetails.username)
            logger.info("Admin $userId starting batch analysis for company ID: $companyId")

            // Process this company's statements
            val results = batchAnalysisService.processCompanyStatementsBatch(companyId)

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Company batch processing completed",
                    data = mapOf(
                        "companyId" to companyId,
                        "processed" to results["processed"],
                        "errors" to results["errors"],
                        "status" to "COMPLETED"
                    )
                )
            ) as ResponseEntity<ApiResponse<Map<String, Any>>>
        } catch (e: Exception) {
            logger.error("Error in company batch processing: ${e.message}", e)
            return ResponseEntity.ok(
                ApiResponse(
                    success = false,
                    message = "Error in company batch processing",
                    errors = listOf(e.message ?: "Unknown error"),
                    data = mapOf(
                        "companyId" to companyId,
                        "status" to "ERROR"
                    )
                )
            )
        }
    }
}