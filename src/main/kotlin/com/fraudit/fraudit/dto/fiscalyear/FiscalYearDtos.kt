package com.fraudit.fraudit.dto.fiscalyear

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * Request DTO for creating a new fiscal year
 */
data class FiscalYearRequest(
    @field:NotNull(message = "Company ID is required")
    val companyId: Long,

    @field:NotNull(message = "Year is required")
    @field:Min(value = 1900, message = "Year must be at least 1900")
    @field:Max(value = 2100, message = "Year must be at most 2100")
    val year: Int,

    @field:NotNull(message = "Start date is required")
    val startDate: LocalDate,

    @field:NotNull(message = "End date is required")
    val endDate: LocalDate,

    val isAudited: Boolean = false
)

/**
 * Response DTO for a fiscal year with complete details
 */
data class FiscalYearResponse(
    val id: Long,
    val companyId: Long,
    val companyName: String,
    val stockCode: String,
    val year: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val isAudited: Boolean,
    val statementCount: Long,
    val createdAt: OffsetDateTime?
)

/**
 * Request DTO for updating a fiscal year
 */
data class FiscalYearUpdateRequest(
    @field:NotNull(message = "Start date is required")
    val startDate: LocalDate,

    @field:NotNull(message = "End date is required")
    val endDate: LocalDate,

    val isAudited: Boolean
)

/**
 * Summary response DTO for a fiscal year (list view)
 */
data class FiscalYearSummaryResponse(
    val id: Long,
    val companyName: String,
    val stockCode: String,
    val year: Int,
    val isAudited: Boolean,
    val statementCount: Long
)

/**
 * Request DTO for auditing a fiscal year
 */
data class FiscalYearAuditRequest(
    @field:NotNull(message = "Audit status is required")
    val isAudited: Boolean
)

/**
 * Statistics response DTO for fiscal years
 */
data class FiscalYearStatsResponse(
    val totalFiscalYears: Int,
    val auditedCount: Int,
    val unauditedCount: Int,
    val fiscalYearsByCompany: Map<String, Int>,
    val fiscalYearsByYear: Map<Int, Int>,
    val yearsWithMostStatements: List<YearStatementCount>
)

/**
 * Helper DTO for year and statement count
 */
data class YearStatementCount(
    val year: Int,
    val companyName: String,
    val statementCount: Long
)

/**
 * Validation response DTO for fiscal year validation
 */
data class FiscalYearValidationResponse(
    val valid: Boolean,
    val message: String?
)