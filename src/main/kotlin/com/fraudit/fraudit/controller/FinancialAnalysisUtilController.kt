package com.fraudit.fraudit.controller

import com.fraudit.fraudit.dto.common.ApiResponse
import com.fraudit.fraudit.service.impl.FinancialAnalysisUtilService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/admin/analysis")
@PreAuthorize("hasRole('ADMIN')")
class FinancialAnalysisUtilController(
    private val financialAnalysisUtilService: FinancialAnalysisUtilService
) {
    private val logger = LoggerFactory.getLogger(FinancialAnalysisUtilController::class.java)

    @PostMapping("/analyze-all-pending")
    fun analyzeAllPendingStatements(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        try {
            logger.info("Admin-triggered analysis of all pending statements")
            val userId = UUID.fromString(userDetails.username)

            // Run analysis on all pending statements
            val count = financialAnalysisUtilService.analyzeAllPendingStatements()

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Analysis completed successfully",
                    data = mapOf(
                        "analyzedCount" to count,
                        "status" to "COMPLETED"
                    )
                )
            )
        } catch (e: Exception) {
            logger.error("Error during admin-triggered analysis: ${e.message}", e)
            return ResponseEntity.ok(
                ApiResponse(
                    success = false,
                    message = "Error during analysis",
                    errors = listOf(e.message ?: "Unknown error"),
                    data = mapOf(
                        "status" to "ERROR"
                    )
                )
            )
        }
    }

    @PostMapping("/analyze-statement/{statementId}")
    fun analyzeStatement(
        @PathVariable statementId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        try {
            logger.info("Admin-triggered analysis of statement ID: $statementId")
            val userId = UUID.fromString(userDetails.username)

            // Run analysis on the specific statement
            val success = financialAnalysisUtilService.analyzeStatement(statementId, userId)

            return ResponseEntity.ok(
                ApiResponse(
                    success = success,
                    message = if (success) "Analysis completed successfully" else "Analysis failed - see logs for details",
                    data = mapOf(
                        "statementId" to statementId,
                        "status" to if (success) "COMPLETED" else "FAILED"
                    )
                )
            )
        } catch (e: Exception) {
            logger.error("Error during analysis of statement ID $statementId: ${e.message}", e)
            return ResponseEntity.ok(
                ApiResponse(
                    success = false,
                    message = "Error during analysis",
                    errors = listOf(e.message ?: "Unknown error"),
                    data = mapOf(
                        "statementId" to statementId,
                        "status" to "ERROR"
                    )
                )
            )
        }
    }
}