package com.fraudit.fraudit.controller

import com.fraudit.fraudit.domain.entity.User
import com.fraudit.fraudit.domain.enum.AlertSeverity
import com.fraudit.fraudit.domain.enum.RiskLevel
import com.fraudit.fraudit.dto.common.ApiResponse
import com.fraudit.fraudit.dto.common.PagedResponse
import com.fraudit.fraudit.dto.risk.*
import com.fraudit.fraudit.service.AuditLogService
import com.fraudit.fraudit.service.FinancialAnalysisService
import com.fraudit.fraudit.service.FinancialStatementService
import com.fraudit.fraudit.service.RiskAlertService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import java.util.UUID
import jakarta.validation.Valid

@RestController
@RequestMapping("/api/fraud-risk")
class FraudRiskController(
    private val financialAnalysisService: FinancialAnalysisService,
    private val financialStatementService: FinancialStatementService,
    private val riskAlertService: RiskAlertService,
    private val auditLogService: AuditLogService
) {
    private val logger = LoggerFactory.getLogger(FraudRiskController::class.java)

    @GetMapping("/assessments")
    fun getAllAssessments(
        @RequestParam(required = false) companyId: Long?,
        @RequestParam(required = false) riskLevel: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<PagedResponse<FraudRiskSummaryResponse>>> {
        try {
            val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "assessedAt"))

            // Get risk assessments with optional filtering
            val assessmentsPage = if (companyId != null && riskLevel != null) {
                try {
                    financialAnalysisService.getFraudRiskAssessmentsByCompanyAndRiskLevel(
                        companyId,
                        getRiskLevelFromString(riskLevel),
                        pageable
                    )
                } catch (e: IllegalArgumentException) {
                    logger.warn("Invalid risk level provided: $riskLevel")
                    return ResponseEntity.badRequest().body(
                        ApiResponse(
                            success = false,
                            message = "Invalid risk level",
                            errors = listOf("'$riskLevel' is not a valid risk level")
                        )
                    )
                }
            } else if (companyId != null) {
                financialAnalysisService.getFraudRiskAssessmentsByCompany(companyId, pageable)
            } else if (riskLevel != null) {
                try {
                    financialAnalysisService.getFraudRiskAssessmentsByRiskLevel(
                        getRiskLevelFromString(riskLevel),
                        pageable
                    )
                } catch (e: IllegalArgumentException) {
                    logger.warn("Invalid risk level provided: $riskLevel")
                    return ResponseEntity.badRequest().body(
                        ApiResponse(
                            success = false,
                            message = "Invalid risk level",
                            errors = listOf("'$riskLevel' is not a valid risk level")
                        )
                    )
                }
            } else {
                financialAnalysisService.getAllFraudRiskAssessments(pageable)
            }

            // Map to summary DTOs
            val summaries = assessmentsPage.map { assessment ->
                FraudRiskSummaryResponse(
                    id = assessment.id!!,
                    companyName = assessment.statement.fiscalYear.company.name,
                    year = assessment.statement.fiscalYear.year,
                    overallRiskScore = assessment.overallRiskScore,
                    riskLevel = assessment.riskLevel,
                    assessedAt = assessment.assessedAt
                )
            }

            val pagedResponse = PagedResponse(
                content = summaries.content,
                page = summaries.number,
                size = summaries.size,
                totalElements = summaries.totalElements,
                totalPages = summaries.totalPages,
                first = summaries.isFirst,
                last = summaries.isLast
            )

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Fraud risk assessments retrieved successfully",
                    data = pagedResponse
                )
            )
        } catch (e: Exception) {
            logger.error("Error retrieving fraud risk assessments: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error retrieving fraud risk assessments",
                    errors = listOf(e.message ?: "An unexpected error occurred")
                )
            )
        }
    }

    /**
     * Helper method to safely convert a string to RiskLevel enum
     * Handles null, empty strings, and case-insensitive matching
     */
    private fun getRiskLevelFromString(riskLevelStr: String?): RiskLevel {
        if (riskLevelStr.isNullOrBlank()) {
            throw IllegalArgumentException("Risk level cannot be null or empty")
        }

        // Try to match case-insensitive
        return RiskLevel.values().find {
            it.name.equals(riskLevelStr, ignoreCase = true)
        } ?: throw IllegalArgumentException("Invalid risk level: $riskLevelStr")
    }

    @GetMapping("/assessments/{id}")
    fun getAssessmentById(@PathVariable id: Long): ResponseEntity<ApiResponse<FraudRiskAssessmentResponse>> {
        try {
            val assessment = financialAnalysisService.getFraudRiskAssessmentById(id)

            // Map to detailed DTO
            val company = assessment.statement.fiscalYear.company
            val response = FraudRiskAssessmentResponse(
                id = assessment.id!!,
                statementId = assessment.statement.id!!,
                companyId = company.id!!,
                companyName = company.name,
                year = assessment.statement.fiscalYear.year,
                zScoreRisk = assessment.zScoreRisk,
                mScoreRisk = assessment.mScoreRisk,
                fScoreRisk = assessment.fScoreRisk,
                financialRatioRisk = assessment.financialRatioRisk,
                mlPredictionRisk = assessment.mlPredictionRisk,
                overallRiskScore = assessment.overallRiskScore,
                riskLevel = assessment.riskLevel,
                assessmentSummary = assessment.assessmentSummary,
                assessedAt = assessment.assessedAt,
                assessedById = assessment.assessedBy?.id,
                assessedByUsername = assessment.assessedBy?.username,
                alertCount = assessment.riskAlerts.size
            )

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Fraud risk assessment retrieved successfully",
                    data = response
                )
            )
        } catch (e: Exception) {
            logger.error("Error retrieving fraud risk assessment: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse(
                    success = false,
                    message = "Fraud risk assessment not found",
                    errors = listOf(e.message ?: "Assessment not found")
                )
            )
        }
    }

    @GetMapping("/assessments/statement/{statementId}")
    fun getAssessmentByStatementId(@PathVariable statementId: Long): ResponseEntity<ApiResponse<FraudRiskAssessmentResponse>> {
        try {
            val assessment = financialAnalysisService.getFraudRiskAssessmentByStatementId(statementId)
                ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse(
                        success = false,
                        message = "No fraud risk assessment found for statement id: $statementId",
                        errors = listOf("Assessment not found")
                    )
                )

            // Map to detailed DTO
            val company = assessment.statement.fiscalYear.company
            val response = FraudRiskAssessmentResponse(
                id = assessment.id!!,
                statementId = assessment.statement.id!!,
                companyId = company.id!!,
                companyName = company.name,
                year = assessment.statement.fiscalYear.year,
                zScoreRisk = assessment.zScoreRisk,
                mScoreRisk = assessment.mScoreRisk,
                fScoreRisk = assessment.fScoreRisk,
                financialRatioRisk = assessment.financialRatioRisk,
                mlPredictionRisk = assessment.mlPredictionRisk,
                overallRiskScore = assessment.overallRiskScore,
                riskLevel = assessment.riskLevel,
                assessmentSummary = assessment.assessmentSummary,
                assessedAt = assessment.assessedAt,
                assessedById = assessment.assessedBy?.id,
                assessedByUsername = assessment.assessedBy?.username,
                alertCount = assessment.riskAlerts.size
            )

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Fraud risk assessment retrieved successfully",
                    data = response
                )
            )
        } catch (e: Exception) {
            logger.error("Error retrieving fraud risk assessment by statement ID: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse(
                    success = false,
                    message = "Error retrieving fraud risk assessment",
                    errors = listOf(e.message ?: "An unexpected error occurred")
                )
            )
        }
    }

    @PostMapping("/assess")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    fun performRiskAssessment(
        @Valid @RequestBody request: PerformRiskAssessmentRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<FraudRiskAssessmentResponse>> {
        try {
            val userId = UUID.fromString(userDetails.username)

            // Verify statement exists and permissions
            val statement = financialStatementService.findById(request.statementId)
            if (statement.user.id != userId && !userDetails.authorities.any { it.authority == "ROLE_ADMIN" }) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiResponse(
                        success = false,
                        message = "You don't have permission to assess this statement",
                        errors = listOf("Insufficient permissions")
                    )
                )
            }

            // Check if required data and scores exist
            if (statement.financialData == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse(
                        success = false,
                        message = "Financial data must be entered before risk assessment",
                        errors = listOf("Missing financial data")
                    )
                )
            }

            if (statement.financialRatios == null || statement.altmanZScore == null ||
                statement.beneishMScore == null || statement.piotroskiFScore == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse(
                        success = false,
                        message = "Financial analysis (ratios, Z-Score, M-Score, F-Score) must be calculated before risk assessment",
                        errors = listOf("Missing analysis scores")
                    )
                )
            }

            // Perform risk assessment
            val assessment = financialAnalysisService.assessFraudRisk(request.statementId, userId)

            // Generate risk alerts based on assessment
            financialAnalysisService.generateRiskAlerts(assessment.id!!, userId)

            // Map to response DTO
            val company = statement.fiscalYear.company
            val response = FraudRiskAssessmentResponse(
                id = assessment.id!!,
                statementId = statement.id!!,
                companyId = company.id!!,
                companyName = company.name,
                year = statement.fiscalYear.year,
                zScoreRisk = assessment.zScoreRisk,
                mScoreRisk = assessment.mScoreRisk,
                fScoreRisk = assessment.fScoreRisk,
                financialRatioRisk = assessment.financialRatioRisk,
                mlPredictionRisk = assessment.mlPredictionRisk,
                overallRiskScore = assessment.overallRiskScore,
                riskLevel = assessment.riskLevel,
                assessmentSummary = assessment.assessmentSummary,
                assessedAt = assessment.assessedAt,
                assessedById = assessment.assessedBy?.id,
                assessedByUsername = assessment.assessedBy?.username,
                alertCount = assessment.riskAlerts.size
            )

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Fraud risk assessment performed successfully",
                    data = response
                )
            )
        } catch (e: Exception) {
            logger.error("Error performing fraud risk assessment: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error performing fraud risk assessment",
                    errors = listOf(e.message ?: "An unexpected error occurred")
                )
            )
        }
    }

    @GetMapping("/alerts")
    fun getAllAlerts(
        @RequestParam(required = false) assessmentId: Long?,
        @RequestParam(required = false) severity: String?,
        @RequestParam(required = false) isResolved: Boolean?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<PagedResponse<RiskAlertResponse>>> {
        try {
            val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))

            // Get risk alerts with optional filtering
            val alerts = when {
                assessmentId != null && severity != null && isResolved != null -> {
                    val alertSeverity = AlertSeverity.valueOf(severity)
                    // Custom query needed for this combination
                    // For simplicity, we're just getting by assessment ID here
                    riskAlertService.findByAssessmentId(assessmentId)
                        .filter { it.severity == alertSeverity && it.isResolved == isResolved }
                }
                assessmentId != null && severity != null -> {
                    val alertSeverity = AlertSeverity.valueOf(severity)
                    riskAlertService.findByAssessmentId(assessmentId)
                        .filter { it.severity == alertSeverity }
                }
                assessmentId != null && isResolved != null -> {
                    riskAlertService.findByAssessmentId(assessmentId)
                        .filter { it.isResolved == isResolved }
                }
                severity != null && isResolved != null -> {
                    val alertSeverity = AlertSeverity.valueOf(severity)
                    if (isResolved) {
                        riskAlertService.findResolved()
                            .filter { it.severity == alertSeverity }
                    } else {
                        riskAlertService.findUnresolved()
                            .filter { it.severity == alertSeverity }
                    }
                }
                assessmentId != null -> riskAlertService.findByAssessmentId(assessmentId)
                severity != null -> riskAlertService.findBySeverity(AlertSeverity.valueOf(severity))
                isResolved != null -> if (isResolved) riskAlertService.findResolved() else riskAlertService.findUnresolved()
                else -> riskAlertService.findAll()
            }

            // Manual pagination (since we're filtering in memory for some cases)
            val start = page * size
            val end = minOf(start + size, alerts.size)
            val pagedAlerts = if (start < alerts.size) alerts.subList(start, end) else emptyList()

            // Map to response DTOs
            val alertResponses = pagedAlerts.map { alert ->
                val company = alert.assessment.statement.fiscalYear.company
                RiskAlertResponse(
                    id = alert.id!!,
                    assessmentId = alert.assessment.id!!,
                    companyId = company.id!!,
                    companyName = company.name,
                    alertType = alert.alertType,
                    severity = alert.severity,
                    message = alert.message,
                    createdAt = alert.createdAt,
                    isResolved = alert.isResolved,
                    resolvedById = alert.resolvedBy?.id,
                    resolvedByUsername = alert.resolvedBy?.username,
                    resolvedAt = alert.resolvedAt,
                    resolutionNotes = alert.resolutionNotes
                )
            }

            val pagedResponse = PagedResponse(
                content = alertResponses,
                page = page,
                size = size,
                totalElements = alerts.size.toLong(),
                totalPages = (alerts.size + size - 1) / size, // Ceiling division
                first = page == 0,
                last = end >= alerts.size
            )

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Risk alerts retrieved successfully",
                    data = pagedResponse
                )
            )
        } catch (e: Exception) {
            logger.error("Error retrieving risk alerts: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error retrieving risk alerts",
                    errors = listOf(e.message ?: "An unexpected error occurred")
                )
            )
        }
    }

    @GetMapping("/alerts/{id}")
    fun getAlertById(@PathVariable id: Long): ResponseEntity<ApiResponse<RiskAlertResponse>> {
        try {
            val alert = riskAlertService.findById(id)

            val company = alert.assessment.statement.fiscalYear.company
            val response = RiskAlertResponse(
                id = alert.id!!,
                assessmentId = alert.assessment.id!!,
                companyId = company.id!!,
                companyName = company.name,
                alertType = alert.alertType,
                severity = alert.severity,
                message = alert.message,
                createdAt = alert.createdAt,
                isResolved = alert.isResolved,
                resolvedById = alert.resolvedBy?.id,
                resolvedByUsername = alert.resolvedBy?.username,
                resolvedAt = alert.resolvedAt,
                resolutionNotes = alert.resolutionNotes
            )

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Risk alert retrieved successfully",
                    data = response
                )
            )
        } catch (e: Exception) {
            logger.error("Error retrieving risk alert: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse(
                    success = false,
                    message = "Risk alert not found",
                    errors = listOf(e.message ?: "Alert not found")
                )
            )
        }
    }

    @PutMapping("/alerts/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'AUDITOR')")
    fun resolveAlert(
        @PathVariable id: Long,
        @Valid @RequestBody resolveRequest: ResolveAlertRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<RiskAlertResponse>> {
        try {
            val userId = UUID.fromString(userDetails.username)

            // Resolve the alert
            val resolvedAlert = riskAlertService.resolveAlert(id, userId, resolveRequest.resolutionNotes)

            val company = resolvedAlert.assessment.statement.fiscalYear.company
            val response = RiskAlertResponse(
                id = resolvedAlert.id!!,
                assessmentId = resolvedAlert.assessment.id!!,
                companyId = company.id!!,
                companyName = company.name,
                alertType = resolvedAlert.alertType,
                severity = resolvedAlert.severity,
                message = resolvedAlert.message,
                createdAt = resolvedAlert.createdAt,
                isResolved = resolvedAlert.isResolved,
                resolvedById = resolvedAlert.resolvedBy?.id,
                resolvedByUsername = resolvedAlert.resolvedBy?.username,
                resolvedAt = resolvedAlert.resolvedAt,
                resolutionNotes = resolvedAlert.resolutionNotes
            )

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Risk alert resolved successfully",
                    data = response
                )
            )
        } catch (e: Exception) {
            logger.error("Error resolving risk alert: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error resolving risk alert",
                    errors = listOf(e.message ?: "An unexpected error occurred")
                )
            )
        }
    }
}