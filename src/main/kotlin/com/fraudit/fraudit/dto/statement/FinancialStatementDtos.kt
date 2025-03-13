package com.fraudit.fraudit.dto.statement

import com.fraudit.fraudit.domain.enum.StatementStatus
import com.fraudit.fraudit.domain.enum.StatementType
import java.time.OffsetDateTime
import java.util.UUID

data class FinancialStatementRequest(
    val fiscalYearId: Long,
    val statementType: StatementType,
    val period: String?
)

data class FinancialStatementResponse(
    val id: Long,
    val fiscalYearId: Long,
    val companyId: Long,
    val companyName: String,
    val year: Int,
    val statementType: StatementType,
    val period: String?,
    val status: StatementStatus,
    val uploadDate: OffsetDateTime,
    val hasFinancialData: Boolean,
    val hasRiskAssessment: Boolean,
    val uploadedByUserId: UUID,
    val uploadedByUsername: String
)

data class FinancialStatementSummaryResponse(
    val id: Long,
    val companyName: String,
    val year: Int,
    val statementType: StatementType,
    val status: StatementStatus,
    val uploadDate: OffsetDateTime
)

data class StatementStatusUpdateRequest(
    val status: StatementStatus
)
