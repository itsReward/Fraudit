package com.fraudit.fraudit.dto.company

import java.time.LocalDate
import java.time.OffsetDateTime

data class CompanyRequest(
    val name: String,
    val stockCode: String,
    val sector: String?,
    val listingDate: LocalDate?,
    val description: String?
)

data class CompanyResponse(
    val id: Long,
    val name: String,
    val stockCode: String,
    val sector: String?,
    val listingDate: LocalDate?,
    val description: String?,
    val createdAt: OffsetDateTime?,
    val updatedAt: OffsetDateTime?
)

data class CompanySummaryResponse(
    val id: Long,
    val name: String,
    val stockCode: String,
    val sector: String?
)

data class CompanyRiskResponse(
    val id: Long,
    val name: String,
    val stockCode: String,
    val sector: String?,
    val riskLevel: String,
    val riskScore: Double,
    val lastAssessmentDate: OffsetDateTime
)