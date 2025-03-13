package com.fraudit.fraudit.dto.assessment

import com.fraudit.fraudit.domain.enum.FinancialStrength
import java.time.OffsetDateTime

data class PiotroskiFScoreResponse(
    val id: Long,
    val statementId: Long,
    val companyId: Long,
    val companyName: String,
    val year: Int,
    val positiveNetIncome: Boolean?,
    val positiveOperatingCashFlow: Boolean?,
    val cashFlowGreaterThanNetIncome: Boolean?,
    val improvingRoa: Boolean?,
    val decreasingLeverage: Boolean?,
    val improvingCurrentRatio: Boolean?,
    val noNewShares: Boolean?,
    val improvingGrossMargin: Boolean?,
    val improvingAssetTurnover: Boolean?,
    val fScore: Int?,
    val financialStrength: FinancialStrength?,
    val calculatedAt: OffsetDateTime
)