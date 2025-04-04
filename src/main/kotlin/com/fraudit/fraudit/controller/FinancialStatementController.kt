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
    }

    @GetMapping("/{id}")
    fun getStatementById(@PathVariable id: Long): ResponseEntity<ApiResponse<FinancialStatementResponse>> {
        val statement = financialStatementService.findById(id)

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Financial statement retrieved successfully",
                data = mapToFinancialStatementResponse(statement)
            )
        )
    }

    @GetMapping("/company/{companyId}")
    fun getStatementsByCompanyId(
        @PathVariable companyId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<PagedResponse<FinancialStatementSummaryResponse>>> {
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
    }

    @GetMapping("/fiscal-year/{fiscalYearId}")
    fun getStatementsByFiscalYearId(
        @PathVariable fiscalYearId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<PagedResponse<FinancialStatementSummaryResponse>>> {
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
    }

    @GetMapping("/user")
    @PreAuthorize("isAuthenticated()")
    fun getMyStatements(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<PagedResponse<FinancialStatementSummaryResponse>>> {
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
    }

    @GetMapping("/status/{status}")
    fun getStatementsByStatus(
        @PathVariable status: StatementStatus,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<PagedResponse<FinancialStatementSummaryResponse>>> {
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
    }

    @GetMapping("/type/{statementType}")
    fun getStatementsByType(
        @PathVariable statementType: StatementType,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<PagedResponse<FinancialStatementSummaryResponse>>> {
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
    }

    @GetMapping("/stock-code/{stockCode}")
    fun getStatementsByStockCode(
        @PathVariable stockCode: String
    ): ResponseEntity<ApiResponse<List<FinancialStatementSummaryResponse>>> {
        val statements = financialStatementService.findByCompanyStockCode(stockCode)

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Financial statements for stock code $stockCode retrieved successfully",
                data = statements.map { mapToFinancialStatementSummaryResponse(it) }
            )
        )
    }

    @GetMapping("/company/{companyId}/year/{year}")
    fun getStatementsByCompanyAndYear(
        @PathVariable companyId: Long,
        @PathVariable year: Int
    ): ResponseEntity<ApiResponse<List<FinancialStatementSummaryResponse>>> {
        val statements = financialStatementService.findByCompanyIdAndYear(companyId, year)

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Financial statements for company $companyId in year $year retrieved successfully",
                data = statements.map { mapToFinancialStatementSummaryResponse(it) }
            )
        )
    }

    @GetMapping("/validate")
    fun validateStatement(
        @RequestParam fiscalYearId: Long,
        @RequestParam statementType: StatementType,
        @RequestParam(required = false) period: String?
    ): ResponseEntity<ApiResponse<FinancialStatementValidationResponse>> {
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
    }

    @GetMapping("/stats")
    fun getStatementStats(): ResponseEntity<ApiResponse<FinancialStatementStatsResponse>> {
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
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    fun createStatement(
        @Valid @RequestBody statementRequest: FinancialStatementRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<FinancialStatementResponse>> {
        val userId = UUID.fromString(userDetails.username)

        // Get fiscal year
        val fiscalYear = fiscalYearService.findById(statementRequest.fiscalYearId)

        // Get user
        val user = userService.findById(userId)

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
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    fun updateStatement(
        @PathVariable id: Long,
        @Valid @RequestBody statementRequest: FinancialStatementRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<FinancialStatementResponse>> {
        val userId = UUID.fromString(userDetails.username)

        // Get existing statement
        val existingStatement = financialStatementService.findById(id)

        // Get fiscal year
        val fiscalYear = fiscalYearService.findById(statementRequest.fiscalYearId)

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
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    fun updateStatementStatus(
        @PathVariable id: Long,
        @Valid @RequestBody statusUpdateRequest: StatementStatusUpdateRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<FinancialStatementResponse>> {
        val userId = UUID.fromString(userDetails.username)

        val updatedStatement = financialStatementService.updateStatus(id, statusUpdateRequest.status, userId)

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Financial statement status updated successfully",
                data = mapToFinancialStatementResponse(updatedStatement)
            )
        )
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN') or @securityService.isOwner(#id)")
    fun deleteStatement(
        @PathVariable id: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Void>> {
        val userId = UUID.fromString(userDetails.username)

        financialStatementService.deleteStatement(id, userId)

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Financial statement deleted successfully"
            )
        )
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