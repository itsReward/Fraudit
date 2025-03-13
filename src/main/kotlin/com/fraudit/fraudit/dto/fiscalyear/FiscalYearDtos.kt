package com.fraudit.fraudit.dto.fiscalyear

import java.time.LocalDate
import java.time.OffsetDateTime

data class FiscalYearRequest(
    val companyId: Long,
    val year: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val isAudited: Boolean = false
)

data class FiscalYearResponse(
    val id: Long,
    val companyId: Long,
    val companyName: String,
    val year: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val isAudited: Boolean,
    val createdAt: OffsetDateTime?
)

data class FiscalYearUpdateRequest(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val isAudited: Boolean
)

data class FiscalYearSummaryResponse(
    val id: Long,
    val companyName: String,
    val year: Int,
    val isAudited: Boolean
)