package com.fraudit.fraudit.dto.assessment

import com.fraudit.fraudit.domain.enum.RiskCategory
import java.math.BigDecimal
import java.time.OffsetDateTime

data class AltmanZScoreResponse(
    val id: Long,
    val statementId: Long,
    val companyId: Long,
    val companyName: String,
    val year: Int,
    val workingCapitalToTotalAssets: BigDecimal?,
    val retainedEarningsToTotalAssets: BigDecimal?,
    val ebitToTotalAssets: BigDecimal?,
    val marketValueEquityToBookValueDebt: BigDecimal?,
    val salesToTotalAssets: BigDecimal?,
    val zScore: BigDecimal?,
    val riskCategory: RiskCategory?,
    val calculatedAt: OffsetDateTime
)