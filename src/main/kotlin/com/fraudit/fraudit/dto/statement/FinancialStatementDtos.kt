package com.fraudit.fraudit.dto.statement

import com.fraudit.fraudit.domain.enum.StatementStatus
import com.fraudit.fraudit.domain.enum.StatementType
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Request DTO for creating a new financial statement
 */
data class FinancialStatementRequest(
    @field:NotNull(message = "Fiscal year ID is required")
    val fiscalYearId: Long,

    @field:NotNull(message = "Statement type is required")
    val statementType: StatementType,

    @field:Size(max = 50, message = "Period cannot exceed 50 characters")
    val period: String? = null
)

/**
 * Response DTO for a financial statement with complete details
 */
data class FinancialStatementResponse(
    val id: Long,
    val fiscalYearId: Long,
    val companyId: Long,
    val companyName: String,
    val stockCode: String,
    val year: Int,
    val statementType: StatementType,
    val period: String?,
    val status: StatementStatus,
    val uploadDate: OffsetDateTime,
    val hasFinancialData: Boolean,
    val hasRiskAssessment: Boolean,
    val hasDocuments: Boolean,
    val documentCount: Int,
    val uploadedByUserId: UUID,
    val uploadedByUsername: String
)

/**
 * Summary response DTO for a financial statement (list view)
 */
data class FinancialStatementSummaryResponse(
    val id: Long,
    val companyName: String,
    val stockCode: String,
    val year: Int,
    val statementType: StatementType,
    val period: String?,
    val status: StatementStatus,
    val uploadDate: OffsetDateTime,
    val hasFinancialData: Boolean
)

/**
 * Request DTO for updating a financial statement's status
 */
data class StatementStatusUpdateRequest(
    @field:NotNull(message = "Status is required")
    val status: StatementStatus
)

/**
 * Statistics response DTO for financial statements
 */
data class FinancialStatementStatsResponse(
    val totalStatements: Int,
    val pendingCount: Int,
    val processedCount: Int,
    val analyzedCount: Int,
    val annualCount: Int,
    val interimCount: Int,
    val quarterlyCount: Int,
    val statementsByCompany: Map<String, Int>,
    val statementsByYear: Map<Int, Int>
)

/**
 * Validation response DTO for financial statement validation
 */
data class FinancialStatementValidationResponse(
    val valid: Boolean,
    val message: String?
)