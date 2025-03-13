package com.fraudit.fraudit.dto.ratios

import java.math.BigDecimal
import java.time.OffsetDateTime

data class FinancialRatiosResponse(
    val id: Long,
    val statementId: Long,
    val companyId: Long,
    val companyName: String,
    val year: Int,

    // Liquidity Ratios
    val currentRatio: BigDecimal?,
    val quickRatio: BigDecimal?,
    val cashRatio: BigDecimal?,

    // Profitability Ratios
    val grossMargin: BigDecimal?,
    val operatingMargin: BigDecimal?,
    val netProfitMargin: BigDecimal?,
    val returnOnAssets: BigDecimal?,
    val returnOnEquity: BigDecimal?,

    // Efficiency Ratios
    val assetTurnover: BigDecimal?,
    val inventoryTurnover: BigDecimal?,
    val accountsReceivableTurnover: BigDecimal?,
    val daysSalesOutstanding: BigDecimal?,

    // Leverage Ratios
    val debtToEquity: BigDecimal?,
    val debtRatio: BigDecimal?,
    val interestCoverage: BigDecimal?,

    // Valuation Ratios
    val priceToEarnings: BigDecimal?,
    val priceToBook: BigDecimal?,

    // Quality Metrics
    val accrualRatio: BigDecimal?,
    val earningsQuality: BigDecimal?,

    val calculatedAt: OffsetDateTime
)

data class FinancialRatiosSummaryResponse(
    val id: Long,
    val statementId: Long,
    val companyName: String,
    val year: Int,
    val currentRatio: BigDecimal?,
    val quickRatio: BigDecimal?,
    val netProfitMargin: BigDecimal?,
    val returnOnEquity: BigDecimal?,
    val debtToEquity: BigDecimal?,
    val calculatedAt: OffsetDateTime
)