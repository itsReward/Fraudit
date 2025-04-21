package com.fraudit.fraudit.controller

import com.fraudit.fraudit.domain.entity.AltmanZScore
import com.fraudit.fraudit.domain.entity.BeneishMScore
import com.fraudit.fraudit.domain.entity.FinancialRatios
import com.fraudit.fraudit.domain.entity.PiotroskiFScore
import com.fraudit.fraudit.domain.entity.User
import com.fraudit.fraudit.dto.assessment.*
import com.fraudit.fraudit.dto.common.ApiResponse
import com.fraudit.fraudit.dto.ratios.*
import com.fraudit.fraudit.service.AuditLogService
import com.fraudit.fraudit.service.FinancialAnalysisService
import com.fraudit.fraudit.service.FinancialStatementService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/financial-analysis")
class FinancialAnalysisController(
    private val financialAnalysisService: FinancialAnalysisService,
    private val financialStatementService: FinancialStatementService,
    private val auditLogService: AuditLogService
) {
    private val logger = LoggerFactory.getLogger(FinancialAnalysisController::class.java)

    @GetMapping("/ratios/statement/{statementId}")
    fun getFinancialRatiosByStatementId(@PathVariable statementId: Long): ResponseEntity<ApiResponse<FinancialRatiosResponse>> {
        try {
            // Verify statement exists
            val statement = financialStatementService.findById(statementId)

            // Get the financial ratios
            val ratios = financialAnalysisService.getFinancialRatios(statementId)
                ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse(
                        success = false,
                        message = "Financial ratios not found for statement id: $statementId",
                        errors = listOf("Financial ratios not found")
                    )
                )

            // Map entity to DTO
            val company = statement.fiscalYear.company
            val response = FinancialRatiosResponse(
                id = ratios.id!!,
                statementId = statementId,
                companyId = company.id!!,
                companyName = company.name,
                year = statement.fiscalYear.year,
                currentRatio = ratios.currentRatio,
                quickRatio = ratios.quickRatio,
                cashRatio = ratios.cashRatio,
                grossMargin = ratios.grossMargin,
                operatingMargin = ratios.operatingMargin,
                netProfitMargin = ratios.netProfitMargin,
                returnOnAssets = ratios.returnOnAssets,
                returnOnEquity = ratios.returnOnEquity,
                assetTurnover = ratios.assetTurnover,
                inventoryTurnover = ratios.inventoryTurnover,
                accountsReceivableTurnover = ratios.accountsReceivableTurnover,
                daysSalesOutstanding = ratios.daysSalesOutstanding,
                debtToEquity = ratios.debtToEquity,
                debtRatio = ratios.debtRatio,
                interestCoverage = ratios.interestCoverage,
                priceToEarnings = ratios.priceToEarnings,
                priceToBook = ratios.priceToBook,
                accrualRatio = ratios.accrualRatio,
                earningsQuality = ratios.earningsQuality,
                calculatedAt = ratios.calculatedAt
            )

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Financial ratios retrieved successfully",
                    data = response
                )
            )
        } catch (e: Exception) {
            logger.error("Error retrieving financial ratios: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error retrieving financial ratios",
                    errors = listOf(e.message ?: "An unexpected error occurred")
                )
            )
        }
    }

    @GetMapping("/z-score/statement/{statementId}")
    fun getAltmanZScoreByStatementId(@PathVariable statementId: Long): ResponseEntity<ApiResponse<AltmanZScoreResponse>> {
        try {
            // Verify statement exists
            val statement = financialStatementService.findById(statementId)

            // Get the Z-Score
            val zScore = financialAnalysisService.getAltmanZScore(statementId)
                ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse(
                        success = false,
                        message = "Altman Z-Score not found for statement id: $statementId",
                        errors = listOf("Altman Z-Score not found")
                    )
                )

            // Map entity to DTO
            val company = statement.fiscalYear.company
            val response = AltmanZScoreResponse(
                id = zScore.id!!,
                statementId = statementId,
                companyId = company.id!!,
                companyName = company.name,
                year = statement.fiscalYear.year,
                workingCapitalToTotalAssets = zScore.workingCapitalToTotalAssets,
                retainedEarningsToTotalAssets = zScore.retainedEarningsToTotalAssets,
                ebitToTotalAssets = zScore.ebitToTotalAssets,
                marketValueEquityToBookValueDebt = zScore.marketValueEquityToBookValueDebt,
                salesToTotalAssets = zScore.salesToTotalAssets,
                zScore = zScore.zScore,
                riskCategory = zScore.riskCategory,
                calculatedAt = zScore.calculatedAt
            )

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Altman Z-Score retrieved successfully",
                    data = response
                )
            )
        } catch (e: Exception) {
            logger.error("Error retrieving Altman Z-Score: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error retrieving Altman Z-Score",
                    errors = listOf(e.message ?: "An unexpected error occurred")
                )
            )
        }
    }

    @GetMapping("/m-score/statement/{statementId}")
    fun getBeneishMScoreByStatementId(@PathVariable statementId: Long): ResponseEntity<ApiResponse<BeneishMScoreResponse>> {
        try {
            // Verify statement exists
            val statement = financialStatementService.findById(statementId)

            // Get the M-Score
            val mScore = financialAnalysisService.getBeneishMScore(statementId)
                ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse(
                        success = false,
                        message = "Beneish M-Score not found for statement id: $statementId",
                        errors = listOf("Beneish M-Score not found")
                    )
                )

            // Map entity to DTO
            val company = statement.fiscalYear.company
            val response = BeneishMScoreResponse(
                id = mScore.id!!,
                statementId = statementId,
                companyId = company.id!!,
                companyName = company.name,
                year = statement.fiscalYear.year,
                daysSalesReceivablesIndex = mScore.daysSalesReceivablesIndex,
                grossMarginIndex = mScore.grossMarginIndex,
                assetQualityIndex = mScore.assetQualityIndex,
                salesGrowthIndex = mScore.salesGrowthIndex,
                depreciationIndex = mScore.depreciationIndex,
                sgAdminExpensesIndex = mScore.sgAdminExpensesIndex,
                leverageIndex = mScore.leverageIndex,
                totalAccrualsToTotalAssets = mScore.totalAccrualsToTotalAssets,
                mScore = mScore.mScore,
                manipulationProbability = mScore.manipulationProbability,
                calculatedAt = mScore.calculatedAt
            )

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Beneish M-Score retrieved successfully",
                    data = response
                )
            )
        } catch (e: Exception) {
            logger.error("Error retrieving Beneish M-Score: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error retrieving Beneish M-Score",
                    errors = listOf(e.message ?: "An unexpected error occurred")
                )
            )
        }
    }

    @GetMapping("/f-score/statement/{statementId}")
    fun getPiotroskiFScoreByStatementId(@PathVariable statementId: Long): ResponseEntity<ApiResponse<PiotroskiFScoreResponse>> {
        try {
            // Verify statement exists
            val statement = financialStatementService.findById(statementId)

            // Get the F-Score
            val fScore = financialAnalysisService.getPiotroskiFScore(statementId)
                ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse(
                        success = false,
                        message = "Piotroski F-Score not found for statement id: $statementId",
                        errors = listOf("Piotroski F-Score not found")
                    )
                )

            // Map entity to DTO
            val company = statement.fiscalYear.company
            val response = PiotroskiFScoreResponse(
                id = fScore.id!!,
                statementId = statementId,
                companyId = company.id!!,
                companyName = company.name,
                year = statement.fiscalYear.year,
                positiveNetIncome = fScore.positiveNetIncome,
                positiveOperatingCashFlow = fScore.positiveOperatingCashFlow,
                cashFlowGreaterThanNetIncome = fScore.cashFlowGreaterThanNetIncome,
                improvingRoa = fScore.improvingRoa,
                decreasingLeverage = fScore.decreasingLeverage,
                improvingCurrentRatio = fScore.improvingCurrentRatio,
                noNewShares = fScore.noNewShares,
                improvingGrossMargin = fScore.improvingGrossMargin,
                improvingAssetTurnover = fScore.improvingAssetTurnover,
                fScore = fScore.fScore,
                financialStrength = fScore.financialStrength,
                calculatedAt = fScore.calculatedAt
            )

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Piotroski F-Score retrieved successfully",
                    data = response
                )
            )
        } catch (e: Exception) {
            logger.error("Error retrieving Piotroski F-Score: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error retrieving Piotroski F-Score",
                    errors = listOf(e.message ?: "An unexpected error occurred")
                )
            )
        }
    }

    @PostMapping("/calculate/{statementId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    fun calculateAllScores(
        @PathVariable statementId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Void>> {
        try {
            val userId = UUID.fromString(userDetails.username)

            // Verify statement exists
            val statement = financialStatementService.findById(statementId)

            // Check permissions - admin or statement owner
            if (statement.user.id != userId && !userDetails.authorities.any { it.authority == "ROLE_ADMIN" }) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiResponse(
                        success = false,
                        message = "You don't have permission to analyze this statement",
                        errors = listOf("Insufficient permissions")
                    )
                )
            }

            // Verify financial data exists
            if (statement.financialData == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse(
                        success = false,
                        message = "Financial data must be entered before analysis can be performed",
                        errors = listOf("Missing financial data")
                    )
                )
            }

            // Perform all calculations
            financialAnalysisService.calculateAllScoresAndRatios(statementId, userId)

            // Log the calculation event
            auditLogService.logEvent(
                userId = userId,
                action = "CALCULATE_ALL",
                entityType = "FINANCIAL_ANALYSIS",
                entityId = statementId.toString(),
                details = "Performed complete financial analysis for statement id: $statementId"
            )

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Financial analysis completed successfully"
                )
            )
        } catch (e: Exception) {
            logger.error("Error calculating financial scores: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error calculating financial scores",
                    errors = listOf(e.message ?: "An unexpected error occurred")
                )
            )
        }
    }

    @PostMapping("/calculate-ratios/{statementId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    fun calculateFinancialRatios(
        @PathVariable statementId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<FinancialRatiosResponse>> {
        try {
            val userId = UUID.fromString(userDetails.username)

            // Verify statement exists and check permissions
            val statement = financialStatementService.findById(statementId)
            if (statement.user.id != userId && !userDetails.authorities.any { it.authority == "ROLE_ADMIN" }) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiResponse(
                        success = false,
                        message = "You don't have permission to analyze this statement",
                        errors = listOf("Insufficient permissions")
                    )
                )
            }

            // Calculate financial ratios
            val ratios = financialAnalysisService.calculateFinancialRatios(statementId, userId)

            // Map entity to DTO
            val company = statement.fiscalYear.company
            val response = FinancialRatiosResponse(
                id = ratios.id!!,
                statementId = statementId,
                companyId = company.id!!,
                companyName = company.name,
                year = statement.fiscalYear.year,
                currentRatio = ratios.currentRatio,
                quickRatio = ratios.quickRatio,
                cashRatio = ratios.cashRatio,
                grossMargin = ratios.grossMargin,
                operatingMargin = ratios.operatingMargin,
                netProfitMargin = ratios.netProfitMargin,
                returnOnAssets = ratios.returnOnAssets,
                returnOnEquity = ratios.returnOnEquity,
                assetTurnover = ratios.assetTurnover,
                inventoryTurnover = ratios.inventoryTurnover,
                accountsReceivableTurnover = ratios.accountsReceivableTurnover,
                daysSalesOutstanding = ratios.daysSalesOutstanding,
                debtToEquity = ratios.debtToEquity,
                debtRatio = ratios.debtRatio,
                interestCoverage = ratios.interestCoverage,
                priceToEarnings = ratios.priceToEarnings,
                priceToBook = ratios.priceToBook,
                accrualRatio = ratios.accrualRatio,
                earningsQuality = ratios.earningsQuality,
                calculatedAt = ratios.calculatedAt
            )

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Financial ratios calculated successfully",
                    data = response
                )
            )
        } catch (e: Exception) {
            logger.error("Error calculating financial ratios: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error calculating financial ratios",
                    errors = listOf(e.message ?: "An unexpected error occurred")
                )
            )
        }
    }

    @PostMapping("/calculate-z-score/{statementId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    fun calculateAltmanZScore(
        @PathVariable statementId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<AltmanZScoreResponse>> {
        try {
            val userId = UUID.fromString(userDetails.username)

            // Verify statement exists and check permissions
            val statement = financialStatementService.findById(statementId)
            if (statement.user.id != userId && !userDetails.authorities.any { it.authority == "ROLE_ADMIN" }) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiResponse(
                        success = false,
                        message = "You don't have permission to analyze this statement",
                        errors = listOf("Insufficient permissions")
                    )
                )
            }

            // Calculate Altman Z-Score
            val zScore = financialAnalysisService.calculateAltmanZScore(statementId, userId)

            // Map entity to DTO
            val company = statement.fiscalYear.company
            val response = AltmanZScoreResponse(
                id = zScore.id!!,
                statementId = statementId,
                companyId = company.id!!,
                companyName = company.name,
                year = statement.fiscalYear.year,
                workingCapitalToTotalAssets = zScore.workingCapitalToTotalAssets,
                retainedEarningsToTotalAssets = zScore.retainedEarningsToTotalAssets,
                ebitToTotalAssets = zScore.ebitToTotalAssets,
                marketValueEquityToBookValueDebt = zScore.marketValueEquityToBookValueDebt,
                salesToTotalAssets = zScore.salesToTotalAssets,
                zScore = zScore.zScore,
                riskCategory = zScore.riskCategory,
                calculatedAt = zScore.calculatedAt
            )

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Altman Z-Score calculated successfully",
                    data = response
                )
            )
        } catch (e: Exception) {
            logger.error("Error calculating Altman Z-Score: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error calculating Altman Z-Score",
                    errors = listOf(e.message ?: "An unexpected error occurred")
                )
            )
        }
    }

    @PostMapping("/calculate-m-score/{statementId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    fun calculateBeneishMScore(
        @PathVariable statementId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<BeneishMScoreResponse>> {
        try {
            val userId = UUID.fromString(userDetails.username)

            // Verify statement exists and check permissions
            val statement = financialStatementService.findById(statementId)
            if (statement.user.id != userId && !userDetails.authorities.any { it.authority == "ROLE_ADMIN" }) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiResponse(
                        success = false,
                        message = "You don't have permission to analyze this statement",
                        errors = listOf("Insufficient permissions")
                    )
                )
            }

            // Calculate Beneish M-Score
            val mScore = financialAnalysisService.calculateBeneishMScore(statementId, userId)

            // Map entity to DTO
            val company = statement.fiscalYear.company
            val response = BeneishMScoreResponse(
                id = mScore.id!!,
                statementId = statementId,
                companyId = company.id!!,
                companyName = company.name,
                year = statement.fiscalYear.year,
                daysSalesReceivablesIndex = mScore.daysSalesReceivablesIndex,
                grossMarginIndex = mScore.grossMarginIndex,
                assetQualityIndex = mScore.assetQualityIndex,
                salesGrowthIndex = mScore.salesGrowthIndex,
                depreciationIndex = mScore.depreciationIndex,
                sgAdminExpensesIndex = mScore.sgAdminExpensesIndex,
                leverageIndex = mScore.leverageIndex,
                totalAccrualsToTotalAssets = mScore.totalAccrualsToTotalAssets,
                mScore = mScore.mScore,
                manipulationProbability = mScore.manipulationProbability,
                calculatedAt = mScore.calculatedAt
            )

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Beneish M-Score calculated successfully",
                    data = response
                )
            )
        } catch (e: Exception) {
            logger.error("Error calculating Beneish M-Score: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error calculating Beneish M-Score",
                    errors = listOf(e.message ?: "An unexpected error occurred")
                )
            )
        }
    }

    @PostMapping("/calculate-f-score/{statementId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    fun calculatePiotroskiFScore(
        @PathVariable statementId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<PiotroskiFScoreResponse>> {
        try {
            val userId = UUID.fromString(userDetails.username)

            // Verify statement exists and check permissions
            val statement = financialStatementService.findById(statementId)
            if (statement.user.id != userId && !userDetails.authorities.any { it.authority == "ROLE_ADMIN" }) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiResponse(
                        success = false,
                        message = "You don't have permission to analyze this statement",
                        errors = listOf("Insufficient permissions")
                    )
                )
            }

            // Calculate Piotroski F-Score
            val fScore = financialAnalysisService.calculatePiotroskiFScore(statementId, userId)

            // Map entity to DTO
            val company = statement.fiscalYear.company
            val response = PiotroskiFScoreResponse(
                id = fScore.id!!,
                statementId = statementId,
                companyId = company.id!!,
                companyName = company.name,
                year = statement.fiscalYear.year,
                positiveNetIncome = fScore.positiveNetIncome,
                positiveOperatingCashFlow = fScore.positiveOperatingCashFlow,
                cashFlowGreaterThanNetIncome = fScore.cashFlowGreaterThanNetIncome,
                improvingRoa = fScore.improvingRoa,
                decreasingLeverage = fScore.decreasingLeverage,
                improvingCurrentRatio = fScore.improvingCurrentRatio,
                noNewShares = fScore.noNewShares,
                improvingGrossMargin = fScore.improvingGrossMargin,
                improvingAssetTurnover = fScore.improvingAssetTurnover,
                fScore = fScore.fScore,
                financialStrength = fScore.financialStrength,
                calculatedAt = fScore.calculatedAt
            )

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Piotroski F-Score calculated successfully",
                    data = response
                )
            )
        } catch (e: Exception) {
            logger.error("Error calculating Piotroski F-Score: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error calculating Piotroski F-Score",
                    errors = listOf(e.message ?: "An unexpected error occurred")
                )
            )
        }
    }

    @PostMapping("/prepare-ml-features/{statementId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    fun prepareMlFeatures(
        @PathVariable statementId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        try {
            val userId = UUID.fromString(userDetails.username)

            // Verify statement exists and check permissions
            val statement = financialStatementService.findById(statementId)
            if (statement.user.id != userId && !userDetails.authorities.any { it.authority == "ROLE_ADMIN" }) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiResponse(
                        success = false,
                        message = "You don't have permission to analyze this statement",
                        errors = listOf("Insufficient permissions")
                    )
                )
            }

            // Prepare ML features
            val mlFeatures = financialAnalysisService.prepareMlFeatures(statementId, userId)

            // Create simplified response with feature metadata
            val response = mapOf(
                "id" to mlFeatures.id!!,
                "statementId" to statementId,
                "featureCount" to try {
                    org.json.JSONObject(mlFeatures.featureSet).length()
                } catch (e: Exception) {
                    0
                },
                "createdAt" to mlFeatures.createdAt
            )

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "ML features prepared successfully",
                    data = response
                )
            )
        } catch (e: Exception) {
            logger.error("Error preparing ML features: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error preparing ML features",
                    errors = listOf(e.message ?: "An unexpected error occurred")
                )
            )
        }
    }

    private fun getFinancialRatios(statementId: Long): FinancialRatios? {
        return try {
            // Get the financial ratios from the repository
            val statement = financialStatementService.findById(statementId)
            statement.financialRatios
        } catch (e: Exception) {
            null
        }
    }

    private fun getAltmanZScore(statementId: Long): AltmanZScore? {
        return try {
            // Get the Altman Z-Score from the repository
            val statement = financialStatementService.findById(statementId)
            statement.altmanZScore
        } catch (e: Exception) {
            null
        }
    }

    private fun getBeneishMScore(statementId: Long): BeneishMScore? {
        return try {
            // Get the Beneish M-Score from the repository
            val statement = financialStatementService.findById(statementId)
            statement.beneishMScore
        } catch (e: Exception) {
            null
        }
    }

    private fun getPiotroskiFScore(statementId: Long): PiotroskiFScore? {
        return try {
            // Get the Piotroski F-Score from the repository
            val statement = financialStatementService.findById(statementId)
            statement.piotroskiFScore
        } catch (e: Exception) {
            null
        }
    }
}