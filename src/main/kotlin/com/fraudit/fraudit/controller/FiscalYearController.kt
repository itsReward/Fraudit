package com.fraudit.fraudit.controller

import com.fraudit.fraudit.domain.entity.FiscalYear
import com.fraudit.fraudit.dto.common.ApiResponse
import com.fraudit.fraudit.dto.common.PagedResponse
import com.fraudit.fraudit.dto.fiscalyear.*
import com.fraudit.fraudit.service.CompanyService
import com.fraudit.fraudit.service.FiscalYearService
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
@RequestMapping("/api/fiscal-years")
class FiscalYearController(
    private val fiscalYearService: FiscalYearService,
    private val companyService: CompanyService
) {
    private val logger = LoggerFactory.getLogger(FiscalYearController::class.java)

    @GetMapping
    fun getAllFiscalYears(
        @RequestParam(required = false) companyId: Long?,
        @RequestParam(required = false) year: Int?,
        @RequestParam(required = false) isAudited: Boolean?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "year") sortBy: String,
        @RequestParam(defaultValue = "DESC") sortDirection: String
    ): ResponseEntity<ApiResponse<PagedResponse<FiscalYearSummaryResponse>>> {
        try {
            val direction = if (sortDirection.equals("ASC", ignoreCase = true))
                Sort.Direction.ASC else Sort.Direction.DESC

            val pageable = PageRequest.of(page, size, Sort.by(direction, sortBy))

            val fiscalYearsPage = when {
                companyId != null -> fiscalYearService.findByCompanyIdPaged(companyId, pageable)
                year != null -> fiscalYearService.findByYearPaged(year, pageable)
                isAudited != null -> fiscalYearService.findByAuditStatusPaged(isAudited, pageable)
                else -> fiscalYearService.findAllPaged(pageable)
            }

            val pagedResponse = createPagedResponse(fiscalYearsPage) { fiscalYear ->
                mapToFiscalYearSummaryResponse(fiscalYear)
            }

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Fiscal years retrieved successfully",
                    data = pagedResponse
                )
            )
        } catch (e: Exception) {
            logger.error("Error retrieving fiscal years: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error retrieving fiscal years",
                    errors = listOf(e.message ?: "Unknown error occurred")
                )
            )
        }
    }

    @GetMapping("/{id}")
    fun getFiscalYearById(@PathVariable id: Long): ResponseEntity<ApiResponse<FiscalYearResponse>> {
        try {
            val fiscalYear = fiscalYearService.findById(id)

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Fiscal year retrieved successfully",
                    data = mapToFiscalYearResponse(fiscalYear)
                )
            )
        } catch (e: Exception) {
            logger.error("Error retrieving fiscal year: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse(
                    success = false,
                    message = "Fiscal year not found",
                    errors = listOf(e.message ?: "Fiscal year not found")
                )
            )
        }
    }

    @GetMapping("/company/{companyId}")
    fun getFiscalYearsByCompanyId(
        @PathVariable companyId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<PagedResponse<FiscalYearSummaryResponse>>> {
        try {
            // First verify company exists
            companyService.findById(companyId)

            val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "year"))
            val fiscalYearsPage = fiscalYearService.findByCompanyIdPaged(companyId, pageable)

            val pagedResponse = createPagedResponse(fiscalYearsPage) { fiscalYear ->
                mapToFiscalYearSummaryResponse(fiscalYear)
            }

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Fiscal years retrieved successfully",
                    data = pagedResponse
                )
            )
        } catch (e: Exception) {
            logger.error("Error retrieving fiscal years by company ID: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse(
                    success = false,
                    message = "Error retrieving fiscal years",
                    errors = listOf(e.message ?: "Company not found or error occurred")
                )
            )
        }
    }

    @GetMapping("/year/{year}")
    fun getFiscalYearsByYear(
        @PathVariable year: Int,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<PagedResponse<FiscalYearSummaryResponse>>> {
        try {
            val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "company.name"))
            val fiscalYearsPage = fiscalYearService.findByYearPaged(year, pageable)

            val pagedResponse = createPagedResponse(fiscalYearsPage) { fiscalYear ->
                mapToFiscalYearSummaryResponse(fiscalYear)
            }

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Fiscal years retrieved successfully",
                    data = pagedResponse
                )
            )
        } catch (e: Exception) {
            logger.error("Error retrieving fiscal years by year: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error retrieving fiscal years",
                    errors = listOf(e.message ?: "Error occurred while retrieving fiscal years")
                )
            )
        }
    }

    @GetMapping("/audited/{isAudited}")
    fun getFiscalYearsByAuditStatus(
        @PathVariable isAudited: Boolean,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<PagedResponse<FiscalYearSummaryResponse>>> {
        try {
            val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "year"))
            val fiscalYearsPage = fiscalYearService.findByAuditStatusPaged(isAudited, pageable)

            val pagedResponse = createPagedResponse(fiscalYearsPage) { fiscalYear ->
                mapToFiscalYearSummaryResponse(fiscalYear)
            }

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Fiscal years retrieved successfully",
                    data = pagedResponse
                )
            )
        } catch (e: Exception) {
            logger.error("Error retrieving fiscal years by audit status: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error retrieving fiscal years",
                    errors = listOf(e.message ?: "Error occurred while retrieving fiscal years")
                )
            )
        }
    }

    @GetMapping("/company/{companyId}/year/{year}")
    fun getFiscalYearByCompanyAndYear(
        @PathVariable companyId: Long,
        @PathVariable year: Int
    ): ResponseEntity<ApiResponse<FiscalYearResponse>> {
        try {
            val fiscalYear = fiscalYearService.findByCompanyIdAndYear(companyId, year)

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Fiscal year retrieved successfully",
                    data = mapToFiscalYearResponse(fiscalYear)
                )
            )
        } catch (e: Exception) {
            logger.error("Error retrieving fiscal year by company and year: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse(
                    success = false,
                    message = "Fiscal year not found",
                    errors = listOf(e.message ?: "Fiscal year not found for company and year")
                )
            )
        }
    }

    @GetMapping("/validate")
    fun validateFiscalYear(
        @RequestParam companyId: Long,
        @RequestParam year: Int
    ): ResponseEntity<ApiResponse<FiscalYearValidationResponse>> {
        try {
            // Check if a fiscal year with this company and year already exists
            val exists = fiscalYearService.findByCompanyId(companyId)
                .any { it.year == year }

            val validation = if (exists) {
                val company = companyService.findById(companyId)
                FiscalYearValidationResponse(
                    valid = false,
                    message = "Fiscal year $year already exists for company ${company.name}"
                )
            } else {
                FiscalYearValidationResponse(
                    valid = true,
                    message = null
                )
            }

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Fiscal year validation completed",
                    data = validation
                )
            )
        } catch (e: Exception) {
            logger.error("Error validating fiscal year: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Validation failed",
                    errors = listOf(e.message ?: "Error occurred during validation")
                )
            )
        }
    }

    @GetMapping("/stats")
    fun getFiscalYearStats(): ResponseEntity<ApiResponse<FiscalYearStatsResponse>> {
        try {
            val allFiscalYears = fiscalYearService.findAll()

            val totalFiscalYears = allFiscalYears.size
            val auditedCount = allFiscalYears.count { it.isAudited }
            val unauditedCount = totalFiscalYears - auditedCount

            val fiscalYearsByCompany = allFiscalYears
                .groupBy { it.company.name }
                .mapValues { it.value.size }

            val fiscalYearsByYear = allFiscalYears
                .groupBy { it.year }
                .mapValues { it.value.size }

            // Get statement counts for each fiscal year
            val yearStatementCounts = allFiscalYears.map { fiscalYear ->
                YearStatementCount(
                    year = fiscalYear.year,
                    companyName = fiscalYear.company.name,
                    statementCount = fiscalYearService.getStatementCountForFiscalYear(fiscalYear.id!!)
                )
            }

            // Sort by statement count (descending) and take top 10
            val yearsWithMostStatements = yearStatementCounts
                .sortedByDescending { it.statementCount }
                .take(10)

            val stats = FiscalYearStatsResponse(
                totalFiscalYears = totalFiscalYears,
                auditedCount = auditedCount,
                unauditedCount = unauditedCount,
                fiscalYearsByCompany = fiscalYearsByCompany,
                fiscalYearsByYear = fiscalYearsByYear,
                yearsWithMostStatements = yearsWithMostStatements
            )

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Fiscal year statistics retrieved successfully",
                    data = stats
                )
            )
        } catch (e: Exception) {
            logger.error("Error retrieving fiscal year statistics: ${e.message}", e)
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
    @PreAuthorize("hasAnyRole('ADMIN', 'REGULATOR')")
    fun createFiscalYear(
        @Valid @RequestBody fiscalYearRequest: FiscalYearRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<FiscalYearResponse>> {
        try {
            val userId = UUID.fromString(userDetails.username)

            // Get company
            val company = companyService.findById(fiscalYearRequest.companyId)

            // Check if fiscal year already exists for company and year
            val existingFiscalYears = fiscalYearService.findByCompanyId(fiscalYearRequest.companyId)
            if (existingFiscalYears.any { it.year == fiscalYearRequest.year }) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    ApiResponse(
                        success = false,
                        message = "Fiscal year ${fiscalYearRequest.year} already exists for company ${company.name}",
                        errors = listOf("Duplicate fiscal year")
                    )
                )
            }

            // Validate start date is before end date
            if (fiscalYearRequest.startDate.isAfter(fiscalYearRequest.endDate)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse(
                        success = false,
                        message = "Start date must be before end date",
                        errors = listOf("Invalid date range")
                    )
                )
            }

            // Create fiscal year entity
            val fiscalYear = FiscalYear(
                id = null,
                company = company,
                year = fiscalYearRequest.year,
                startDate = fiscalYearRequest.startDate,
                endDate = fiscalYearRequest.endDate,
                isAudited = fiscalYearRequest.isAudited
            )

            // Create fiscal year
            val createdFiscalYear = fiscalYearService.createFiscalYear(fiscalYear, userId)

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse(
                    success = true,
                    message = "Fiscal year created successfully",
                    data = mapToFiscalYearResponse(createdFiscalYear)
                )
            )
        } catch (e: Exception) {
            logger.error("Error creating fiscal year: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse(
                    success = false,
                    message = "Error creating fiscal year",
                    errors = listOf(e.message ?: "Error occurred while creating fiscal year")
                )
            )
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGULATOR')")
    fun updateFiscalYear(
        @PathVariable id: Long,
        @Valid @RequestBody fiscalYearUpdateRequest: FiscalYearUpdateRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<FiscalYearResponse>> {
        try {
            val userId = UUID.fromString(userDetails.username)

            // Get existing fiscal year
            val existingFiscalYear = fiscalYearService.findById(id)

            // Validate start date is before end date
            if (fiscalYearUpdateRequest.startDate.isAfter(fiscalYearUpdateRequest.endDate)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse(
                        success = false,
                        message = "Start date must be before end date",
                        errors = listOf("Invalid date range")
                    )
                )
            }

            // Create updated fiscal year entity
            val fiscalYear = existingFiscalYear.copy(
                startDate = fiscalYearUpdateRequest.startDate,
                endDate = fiscalYearUpdateRequest.endDate,
                isAudited = fiscalYearUpdateRequest.isAudited
            )

            // Update fiscal year
            val updatedFiscalYear = fiscalYearService.updateFiscalYear(fiscalYear, userId)

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Fiscal year updated successfully",
                    data = mapToFiscalYearResponse(updatedFiscalYear)
                )
            )
        } catch (e: Exception) {
            logger.error("Error updating fiscal year: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse(
                    success = false,
                    message = "Error updating fiscal year",
                    errors = listOf(e.message ?: "Error occurred while updating fiscal year")
                )
            )
        }
    }

    @PutMapping("/{id}/audit")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGULATOR', 'AUDITOR')")
    fun updateAuditStatus(
        @PathVariable id: Long,
        @Valid @RequestBody auditRequest: FiscalYearAuditRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<FiscalYearResponse>> {
        try {
            val userId = UUID.fromString(userDetails.username)

            val updatedFiscalYear = if (auditRequest.isAudited) {
                fiscalYearService.markAsAudited(id, userId)
            } else {
                fiscalYearService.markAsUnaudited(id, userId)
            }

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = if (auditRequest.isAudited)
                        "Fiscal year marked as audited successfully"
                    else
                        "Fiscal year marked as unaudited successfully",
                    data = mapToFiscalYearResponse(updatedFiscalYear)
                )
            )
        } catch (e: Exception) {
            logger.error("Error updating fiscal year audit status: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse(
                    success = false,
                    message = "Error updating fiscal year audit status",
                    errors = listOf(e.message ?: "Error occurred while updating audit status")
                )
            )
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteFiscalYear(
        @PathVariable id: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Void>> {
        try {
            val userId = UUID.fromString(userDetails.username)

            // Try to delete the fiscal year
            fiscalYearService.deleteFiscalYear(id, userId)

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Fiscal year deleted successfully"
                )
            )
        } catch (e: IllegalStateException) {
            // Fiscal year has associated statements
            logger.error("Cannot delete fiscal year: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiResponse(
                    success = false,
                    message = "Cannot delete fiscal year because it has associated financial statements",
                    errors = listOf(e.message ?: "Fiscal year has associated statements")
                )
            )
        } catch (e: Exception) {
            logger.error("Error deleting fiscal year: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse(
                    success = false,
                    message = "Error deleting fiscal year",
                    errors = listOf(e.message ?: "Error occurred while deleting fiscal year")
                )
            )
        }
    }

    /**
     * Helper method to map a FiscalYear entity to a FiscalYearResponse DTO
     */
    private fun mapToFiscalYearResponse(fiscalYear: FiscalYear): FiscalYearResponse {
        return FiscalYearResponse(
            id = fiscalYear.id!!,
            companyId = fiscalYear.company.id!!,
            companyName = fiscalYear.company.name,
            stockCode = fiscalYear.company.stockCode,
            year = fiscalYear.year,
            startDate = fiscalYear.startDate,
            endDate = fiscalYear.endDate,
            isAudited = fiscalYear.isAudited,
            statementCount = fiscalYearService.getStatementCountForFiscalYear(fiscalYear.id!!),
            createdAt = fiscalYear.createdAt
        )
    }

    /**
     * Helper method to map a FiscalYear entity to a FiscalYearSummaryResponse DTO
     */
    private fun mapToFiscalYearSummaryResponse(fiscalYear: FiscalYear): FiscalYearSummaryResponse {
        return FiscalYearSummaryResponse(
            id = fiscalYear.id!!,
            companyName = fiscalYear.company.name,
            stockCode = fiscalYear.company.stockCode,
            year = fiscalYear.year,
            isAudited = fiscalYear.isAudited,
            statementCount = fiscalYearService.getStatementCountForFiscalYear(fiscalYear.id!!)
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