package com.fraudit.fraudit.controller

import com.fraudit.fraudit.domain.entity.FinancialStatement
import com.fraudit.fraudit.domain.entity.User
import com.fraudit.fraudit.domain.enum.StatementStatus
import com.fraudit.fraudit.domain.enum.StatementType
import com.fraudit.fraudit.dto.common.ApiResponse
import com.fraudit.fraudit.dto.common.PagedResponse
import com.fraudit.fraudit.dto.statement.*
import com.fraudit.fraudit.service.CompanyService
import com.fraudit.fraudit.service.FinancialStatementService
import com.fraudit.fraudit.service.FiscalYearService
import com.fraudit.fraudit.service.UserService
import jakarta.validation.Valid
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

@RestController
@RequestMapping("/api/financial-statements")
class FinancialStatementController(
    private val financialStatementService: FinancialStatementService,
    private val fiscalYearService: FiscalYearService,
    private val userService: UserService,
    private val companyService: CompanyService
) {
    private val logger = LoggerFactory.getLogger(FinancialStatementController::class.java)

    @GetMapping
    fun getAllStatements(
        @RequestParam(required = false) companyId: Long?,
        @RequestParam(required = false) fiscalYearId: Long?,
        @RequestParam(required = false) statementType: StatementType?,
        @RequestParam(required = false) status: StatementStatus?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "uploadDate") sortBy: String,
        @RequestParam(defaultValue = "DESC") sortDirection: String
    ): ResponseEntity<ApiResponse<PagedResponse<FinancialStatementSummaryResponse>>> {
        try {
            val direction = if (sortDirection.equals("ASC", ignoreCase = true))
                Sort.Direction.ASC else Sort.Direction.DESC

            val pageable = PageRequest.of(page, size, Sort.by(direction, sortBy))

            val statementsPage = when {
                companyId != null -> financialStatementService.findByCompanyIdPaged(companyId, pageable)
                fiscalYearId != null -> financialStatementService.findByFiscalYearIdPaged(fiscalYearId, pageable)
                statementType != null -> financialStatementService.findByStatementTypePaged(statementType, pageable)
                status != null -> financialStatementService.findByStatusPaged(status, pageable)
                else -> financialStatementService.findAllPaged(pageable)
            }

            val pagedResponse = createPagedResponse(statementsPage) { statement ->
                mapToFinancialStatementSummaryResponse(statement)
            }

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Financial statements retrieved successfully",
                    data = pagedResponse
                )
            )
        } catch (e: Exception) {
            logger.error("Error retrieving financial statements: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error retrieving financial statements",
                    errors = listOf(e.message ?: "Unknown error occurred")
                )
            )
        }
    }

    @GetMapping("/{id}")
    fun getStatementById(@PathVariable id: Long): ResponseEntity<ApiResponse<FinancialStatementResponse>> {
        try {
            val statement = financialStatementService.findById(id)

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Financial statement retrieved successfully",
                    data = mapToFinancialStatementResponse(statement)
                )
            )
        } catch (e: Exception) {
            logger.error("Error retrieving financial statement: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse(
                    success = false,
                    message = "Financial statement not found",
                    errors = listOf(e.message ?: "Financial statement not found")
                )
            )
        }
    }

    @GetMapping("/company/{companyId}")
    fun getStatementsByCompanyId(
        @PathVariable companyId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<PagedResponse<FinancialStatementSummaryResponse>>> {
        try {
            // First verify company exists
            companyService.findById(companyId)

            val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fiscalYear.year"))
            val statementsPage = financialStatementService.findByCompanyIdPaged(companyId, pageable)

            val pagedResponse = createPagedResponse(statementsPage) { statement ->
                mapToFinancialStatementSummaryResponse(statement)
            }

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Financial statements retrieved successfully",
                    data = pagedResponse
                )
            )
        } catch (e: Exception) {
            logger.error("Error retrieving financial statements by company ID: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse(
                    success = false,
                    message = "Error retrieving financial statements",
                    errors = listOf(e.message ?: "Company not found or error occurred")
                )
            )
        }
    }

    @GetMapping("/fiscal-year/{fiscalYearId}")
    fun getStatementsByFiscalYearId(
        @PathVariable fiscalYearId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<PagedResponse<FinancialStatementSummaryResponse>>> {
        try {
            // First verify fiscal year exists
            fiscalYearService.findById(fiscalYearId)

            val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "uploadDate"))
            val statementsPage = financialStatementService.findByFiscalYearIdPaged(fiscalYearId, pageable)

            val pagedResponse = createPagedResponse(statementsPage) { statement ->
                mapToFinancialStatementSummaryResponse(statement)
            }

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Financial statements retrieved successfully",
                    data = pagedResponse
                )
            )
        } catch (e: Exception) {
            logger.error("Error retrieving financial statements by fiscal year ID: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse(
                    success = false,
                    message = "Error retrieving financial statements",
                    errors = listOf(e.message ?: "Fiscal year not found or error occurred")
                )
            )
        }
    }

    @GetMapping("/user")
    @PreAuthorize("isAuthenticated()")
    fun getMyStatements(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<PagedResponse<FinancialStatementSummaryResponse>>> {
        try {
            val userId = UUID.fromString(userDetails.username)
            val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "uploadDate"))

            val statementsPage = financialStatementService.findByUserIdPaged(userId, pageable)

            val pagedResponse = createPagedResponse(statementsPage) { statement ->
                mapToFinancialStatementSummaryResponse(statement)
            }

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Your financial statements retrieved successfully",
                    data = pagedResponse
                )
            )
        } catch (e: Exception) {
            logger.error("Error retrieving user's financial statements: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error retrieving financial statements",
                    errors = listOf(e.message ?: "Error occurred while retrieving your statements")
                )
            )
        }
    }

    @GetMapping("/status/{status}")
    fun getStatementsByStatus(
        @PathVariable status: StatementStatus,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<PagedResponse<FinancialStatementSummaryResponse>>> {
        try {
            val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "uploadDate"))
            val statementsPage = financialStatementService.findByStatusPaged(status, pageable)

            val pagedResponse = createPagedResponse(statementsPage) { statement ->
                mapToFinancialStatementSummaryResponse(statement)
            }

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Financial statements with status $status retrieved successfully",
                    data = pagedResponse
                )
            )
        } catch (e: Exception) {
            logger.error("Error retrieving financial statements by status: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error retrieving financial statements",
                    errors = listOf(e.message ?: "Error occurred while retrieving statements")
                )
            )
        }
    }

    @GetMapping("/type/{statementType}")
    fun getStatementsByType(
        @PathVariable statementType: StatementType,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<PagedResponse<FinancialStatementSummaryResponse>>> {
        try {
            val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "uploadDate"))
            val statementsPage = financialStatementService.findByStatementTypePaged(statementType, pageable)

            val pagedResponse = createPagedResponse(statementsPage) { statement ->
                mapToFinancialStatementSummaryResponse(statement)
            }

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Financial statements of type $statementType retrieved successfully",
                    data = pagedResponse
                )
            )
        } catch (e: Exception) {
            logger.error("Error retrieving financial statements by type: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error retrieving financial statements",
                    errors = listOf(e.message ?: "Error occurred while retrieving statements")
                )
            )
        }
    }

    @GetMapping("/stock-code/{stockCode}")
    fun getStatementsByStockCode(
        @PathVariable stockCode: String
    ): ResponseEntity<ApiResponse<List<FinancialStatementSummaryResponse>>> {
        try {
            // First verify company with stock code exists
            companyService.findByStockCode(stockCode)

            val statements = financialStatementService.findByCompanyStockCode(stockCode)

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Financial statements for stock code $stockCode retrieved successfully",
                    data = statements.map { mapToFinancialStatementSummaryResponse(it) }
                )
            )
        } catch (e: Exception) {
            logger.error("Error retrieving financial statements by stock code: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse(
                    success = false,
                    message = "Error retrieving financial statements",
                    errors = listOf(e.message ?: "Company with stock code not found or error occurred")
                )
            )
        }
    }

    @GetMapping("/company/{companyId}/year/{year}")
    fun getStatementsByCompanyAndYear(
        @PathVariable companyId: Long,
        @PathVariable year: Int
    ): ResponseEntity<ApiResponse<List<FinancialStatementSummaryResponse>>> {
        try {
            // First verify company exists
            companyService.findById(companyId)

            val statements = financialStatementService.findByCompanyIdAndYear(companyId, year)

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Financial statements for company $companyId in year $year retrieved successfully",
                    data = statements.map { mapToFinancialStatementSummaryResponse(it) }
                )
            )
        } catch (e: Exception) {
            logger.error("Error retrieving financial statements by company and year: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse(
                    success = false,
                    message = "Error retrieving financial statements",
                    errors = listOf(e.message ?: "Company not found or error occurred")
                )
            )
        }
    }

    @GetMapping("/validate")
    fun validateStatement(
        @RequestParam fiscalYearId: Long,
        @RequestParam statementType: StatementType,
        @RequestParam(required = false) period: String?
    ): ResponseEntity<ApiResponse<FinancialStatementValidationResponse>> {
        try {
            // First verify fiscal year exists
            fiscalYearService.findById(fiscalYearId)

            // Check if a statement with this fiscal year, type, and period already exists
            val existingStatements = financialStatementService.findByFiscalYearIdAndStatementType(
                fiscalYearId, statementType)

            val isDuplicate = existingStatements.isNotEmpty() &&
                    (period == null || existingStatements.any { it.period == period })

            val validation = if (isDuplicate) {
                val fiscalYear = fiscalYearService.findById(fiscalYearId)
                FinancialStatementValidationResponse(
                    valid = false,
                    message = "A ${statementType.name} statement" +
                            (if (period != null) " for period $period" else "") +
                            " already exists for fiscal year ${fiscalYear.year}"
                )
            } else {
                FinancialStatementValidationResponse(
                    valid = true,
                    message = null
                )
            }

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Statement validation completed",
                    data = validation
                )
            )
        } catch (e: Exception) {
            logger.error("Error validating financial statement: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse(
                    success = false,
                    message = "Validation failed",
                    errors = listOf(e.message ?: "Fiscal year not found or error occurred")
                )
            )
        }
    }

    @GetMapping("/stats")
    fun getStatementStats(): ResponseEntity<ApiResponse<FinancialStatementStatsResponse>> {
        try {
            val allStatements = financialStatementService.findAll()

            val totalStatements = allStatements.size
            val pendingCount = allStatements.count { it.status == StatementStatus.PENDING }
            val processedCount = allStatements.count { it.status == StatementStatus.PROCESSED }
            val analyzedCount = allStatements.count { it.status == StatementStatus.ANALYZED }

            val annualCount = allStatements.count { it.statementType == StatementType.ANNUAL }
            val interimCount = allStatements.count { it.statementType == StatementType.INTERIM }
            val quarterlyCount = allStatements.count { it.statementType == StatementType.QUARTERLY }

            val statementsByCompany = allStatements
                .groupBy { it.fiscalYear.company.name }
                .mapValues { it.value.size }

            val statementsByYear = allStatements
                .groupBy { it.fiscalYear.year }
                .mapValues { it.value.size }

            val stats = FinancialStatementStatsResponse(
                totalStatements = totalStatements,
                pendingCount = pendingCount,
                processedCount = processedCount,
                analyzedCount = analyzedCount,
                annualCount = annualCount,
                interimCount = interimCount,
                quarterlyCount = quarterlyCount,
                statementsByCompany = statementsByCompany,
                statementsByYear = statementsByYear
            )

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Financial statement statistics retrieved successfully",
                    data = stats
                )
            )
        } catch (e: Exception) {
            logger.error("Error retrieving financial statement statistics: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error retrieving statistics",
                    errors = listOf(e.message ?: "Error occurred while retrieving statistics")
                )
            )
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    fun createStatement(
        @Valid @RequestBody statementRequest: FinancialStatementRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<FinancialStatementResponse>> {
        try {
            val userId = UUID.fromString(userDetails.username)

            // Get fiscal year
            val fiscalYear = fiscalYearService.findById(statementRequest.fiscalYearId)

            // Get user
            val user = userService.findById(userId)

            // Validate no duplicate statement exists
            val existingStatements = financialStatementService.findByFiscalYearIdAndStatementType(
                statementRequest.fiscalYearId, statementRequest.statementType)

            if (existingStatements.isNotEmpty() &&
                (statementRequest.period == null || existingStatements.any { it.period == statementRequest.period })) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    ApiResponse(
                        success = false,
                        message = "A ${statementRequest.statementType.name} statement" +
                                (if (statementRequest.period != null) " for period ${statementRequest.period}" else "") +
                                " already exists for fiscal year ${fiscalYear.year}",
                        errors = listOf("Duplicate statement")
                    )
                )
            }

            // Create statement entity
            val statement = FinancialStatement(
                id = null,
                fiscalYear = fiscalYear,
                user = user,
                statementType = statementRequest.statementType,
                period = statementRequest.period,
                uploadDate = java.time.OffsetDateTime.now(),
                status = StatementStatus.PENDING
            )

            // Create statement
            val createdStatement = financialStatementService.createStatement(statement, userId)

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse(
                    success = true,
                    message = "Financial statement created successfully",
                    data = mapToFinancialStatementResponse(createdStatement)
                )
            )
        } catch (e: Exception) {
            logger.error("Error creating financial statement: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse(
                    success = false,
                    message = "Error creating financial statement",
                    errors = listOf(e.message ?: "Error occurred while creating statement")
                )
            )
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    fun updateStatement(
        @PathVariable id: Long,
        @Valid @RequestBody statementRequest: FinancialStatementRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<FinancialStatementResponse>> {
        try {
            val userId = UUID.fromString(userDetails.username)

            // Get existing statement
            val existingStatement = financialStatementService.findById(id)

            // Verify user has permission (admin or owner)
            if (existingStatement.user.id != userId && !userDetails.authorities.any { it.authority == "ROLE_ADMIN" }) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiResponse(
                        success = false,
                        message = "You don't have permission to update this statement",
                        errors = listOf("Insufficient permissions")
                    )
                )
            }

            // Get fiscal year
            val fiscalYear = fiscalYearService.findById(statementRequest.fiscalYearId)

            // Validate no duplicate statement exists (except this one)
            if (existingStatement.fiscalYear.id != statementRequest.fiscalYearId ||
                existingStatement.statementType != statementRequest.statementType ||
                existingStatement.period != statementRequest.period) {

                val existingStatements = financialStatementService.findByFiscalYearIdAndStatementType(
                    statementRequest.fiscalYearId, statementRequest.statementType)
                    .filter { it.id != id }

                if (existingStatements.isNotEmpty() &&
                    (statementRequest.period == null || existingStatements.any { it.period == statementRequest.period })) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(
                        ApiResponse(
                            success = false,
                            message = "A ${statementRequest.statementType.name} statement" +
                                    (if (statementRequest.period != null) " for period ${statementRequest.period}" else "") +
                                    " already exists for fiscal year ${fiscalYear.year}",
                            errors = listOf("Duplicate statement")
                        )
                    )
                }
            }

            // Create updated statement entity
            val statement = existingStatement.copy(
                fiscalYear = fiscalYear,
                statementType = statementRequest.statementType,
                period = statementRequest.period
            )

            // Update statement
            val updatedStatement = financialStatementService.updateStatement(statement, userId)

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Financial statement updated successfully",
                    data = mapToFinancialStatementResponse(updatedStatement)
                )
            )
        } catch (e: Exception) {
            logger.error("Error updating financial statement: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse(
                    success = false,
                    message = "Error updating financial statement",
                    errors = listOf(e.message ?: "Error occurred while updating statement")
                )
            )
        }
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    fun updateStatementStatus(
        @PathVariable id: Long,
        @Valid @RequestBody statusUpdateRequest: StatementStatusUpdateRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<FinancialStatementResponse>> {
        try {
            val userId = UUID.fromString(userDetails.username)

            // Get existing statement to check permissions
            val existingStatement = financialStatementService.findById(id)

            // Verify user has permission (admin or owner)
            if (existingStatement.user.id != userId && !userDetails.authorities.any { it.authority == "ROLE_ADMIN" }) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiResponse(
                        success = false,
                        message = "You don't have permission to update this statement",
                        errors = listOf("Insufficient permissions")
                    )
                )
            }

            // Update status
            val updatedStatement = financialStatementService.updateStatus(id, statusUpdateRequest.status, userId)

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Financial statement status updated successfully",
                    data = mapToFinancialStatementResponse(updatedStatement)
                )
            )
        } catch (e: Exception) {
            logger.error("Error updating financial statement status: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse(
                    success = false,
                    message = "Error updating financial statement status",
                    errors = listOf(e.message ?: "Error occurred while updating status")
                )
            )
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN') or @securityService.isOwner(#id)")
    fun deleteStatement(
        @PathVariable id: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Void>> {
        try {
            val userId = UUID.fromString(userDetails.username)

            // Attempt to delete the statement
            financialStatementService.deleteStatement(id, userId)

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Financial statement deleted successfully"
                )
            )
        } catch (e: IllegalStateException) {
            // Statement has associated records
            logger.error("Cannot delete financial statement: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiResponse(
                    success = false,
                    message = "Cannot delete financial statement because it has associated records",
                    errors = listOf(e.message ?: "Statement has associated records")
                )
            )
        } catch (e: Exception) {
            logger.error("Error deleting financial statement: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse(
                    success = false,
                    message = "Error deleting financial statement",
                    errors = listOf(e.message ?: "Error occurred while deleting statement")
                )
            )
        }
    }

    /**
     * Helper method to map a FinancialStatement entity to a FinancialStatementResponse DTO
     */
    private fun mapToFinancialStatementResponse(statement: FinancialStatement): FinancialStatementResponse {
        val company = statement.fiscalYear.company

        return FinancialStatementResponse(
            id = statement.id!!,
            fiscalYearId = statement.fiscalYear.id!!,
            companyId = company.id!!,
            companyName = company.name,
            stockCode = company.stockCode,
            year = statement.fiscalYear.year,
            statementType = statement.statementType,
            period = statement.period,
            status = statement.status,
            uploadDate = statement.uploadDate,
            hasFinancialData = statement.financialData != null,
            hasRiskAssessment = statement.fraudRiskAssessment != null,
            hasDocuments = statement.documents.isNotEmpty(),
            documentCount = statement.documents.size,
            uploadedByUserId = statement.user.id,
            uploadedByUsername = statement.user.username
        )
    }

    /**
     * Helper method to map a FinancialStatement entity to a FinancialStatementSummaryResponse DTO
     */
    private fun mapToFinancialStatementSummaryResponse(statement: FinancialStatement): FinancialStatementSummaryResponse {
        val company = statement.fiscalYear.company

        return FinancialStatementSummaryResponse(
            id = statement.id!!,
            companyName = company.name,
            stockCode = company.stockCode,
            year = statement.fiscalYear.year,
            statementType = statement.statementType,
            period = statement.period,
            status = statement.status,
            uploadDate = statement.uploadDate,
            hasFinancialData = statement.financialData != null
        )
    }

    /**
     * Helper method to create a paged response
     */
    private fun <T, R> createPagedResponse(page: Page<T>, mapper: (T) -> R): PagedResponse<R> {
        return PagedResponse(
            content = page.content.map(mapper),
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            first = page.isFirst,
            last = page.isLast
        )
    }
}