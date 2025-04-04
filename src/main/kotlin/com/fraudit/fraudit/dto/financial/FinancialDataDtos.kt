package com.fraudit.fraudit.dto.financial

import java.math.BigDecimal
import java.time.OffsetDateTime

data class FinancialDataRequest(
    val statementId: Long,
    // Income Statement
    val revenue: BigDecimal?,
    val costOfSales: BigDecimal?,
    val grossProfit: BigDecimal?,
    val operatingExpenses: BigDecimal?,
    val administrativeExpenses: BigDecimal?,
    val sellingExpenses: BigDecimal?,
    val depreciation: BigDecimal?,
    val amortization: BigDecimal?,
    val operatingIncome: BigDecimal?,
    val interestExpense: BigDecimal?,
    val otherIncome: BigDecimal?,
    val earningsBeforeTax: BigDecimal?,
    val incomeTax: BigDecimal?,
    val netIncome: BigDecimal?,

    // Balance Sheet - Assets
    val cash: BigDecimal?,
    val shortTermInvestments: BigDecimal?,
    val accountsReceivable: BigDecimal?,
    val inventory: BigDecimal?,
    val otherCurrentAssets: BigDecimal?,
    val totalCurrentAssets: BigDecimal?,
    val propertyPlantEquipment: BigDecimal?,
    val accumulatedDepreciation: BigDecimal?,
    val intangibleAssets: BigDecimal?,
    val longTermInvestments: BigDecimal?,
    val otherNonCurrentAssets: BigDecimal?,
    val totalNonCurrentAssets: BigDecimal?,
    val totalAssets: BigDecimal?,

    // Balance Sheet - Liabilities
    val accountsPayable: BigDecimal?,
    val shortTermDebt: BigDecimal?,
    val accruedLiabilities: BigDecimal?,
    val otherCurrentLiabilities: BigDecimal?,
    val totalCurrentLiabilities: BigDecimal?,
    val longTermDebt: BigDecimal?,
    val deferredTaxes: BigDecimal?,
    val otherNonCurrentLiabilities: BigDecimal?,
    val totalNonCurrentLiabilities: BigDecimal?,
    val totalLiabilities: BigDecimal?,

    // Balance Sheet - Equity
    val commonStock: BigDecimal?,
    val additionalPaidInCapital: BigDecimal?,
    val retainedEarnings: BigDecimal?,
    val treasuryStock: BigDecimal?,
    val otherEquity: BigDecimal?,
    val totalEquity: BigDecimal?,

    // Cash Flow
    val netCashFromOperating: BigDecimal?,
    val netCashFromInvesting: BigDecimal?,
    val netCashFromFinancing: BigDecimal?,
    val netChangeInCash: BigDecimal?,

    // Market Data
    val marketCapitalization: BigDecimal?,
    val sharesOutstanding: BigDecimal?,
    val marketPricePerShare: BigDecimal?,
    val bookValuePerShare: BigDecimal?,
    val earningsPerShare: BigDecimal?
)

data class FinancialDataResponse(
    val id: Long,
    val statementId: Long,
    val companyId: Long,
    val companyName: String,
    val year: Int,

    // Income Statement
    val revenue: BigDecimal?,
    val costOfSales: BigDecimal?,
    val grossProfit: BigDecimal?,
    val operatingExpenses: BigDecimal?,
    val administrativeExpenses: BigDecimal?,
    val sellingExpenses: BigDecimal?,
    val depreciation: BigDecimal?,
    val amortization: BigDecimal?,
    val operatingIncome: BigDecimal?,
    val interestExpense: BigDecimal?,
    val otherIncome: BigDecimal?,
    val earningsBeforeTax: BigDecimal?,
    val incomeTax: BigDecimal?,
    val netIncome: BigDecimal?,

    // Balance Sheet - Assets
    val cash: BigDecimal?,
    val shortTermInvestments: BigDecimal?,
    val accountsReceivable: BigDecimal?,
    val inventory: BigDecimal?,
    val otherCurrentAssets: BigDecimal?,
    val totalCurrentAssets: BigDecimal?,
    val propertyPlantEquipment: BigDecimal?,
    val accumulatedDepreciation: BigDecimal?,
    val intangibleAssets: BigDecimal?,
    val longTermInvestments: BigDecimal?,
    val otherNonCurrentAssets: BigDecimal?,
    val totalNonCurrentAssets: BigDecimal?,
    val totalAssets: BigDecimal?,

    // Balance Sheet - Liabilities
    val accountsPayable: BigDecimal?,
    val shortTermDebt: BigDecimal?,
    val accruedLiabilities: BigDecimal?,
    val otherCurrentLiabilities: BigDecimal?,
    val totalCurrentLiabilities: BigDecimal?,
    val longTermDebt: BigDecimal?,
    val deferredTaxes: BigDecimal?,
    val otherNonCurrentLiabilities: BigDecimal?,
    val totalNonCurrentLiabilities: BigDecimal?,
    val totalLiabilities: BigDecimal?,

    // Balance Sheet - Equity
    val commonStock: BigDecimal?,
    val additionalPaidInCapital: BigDecimal?,
    val retainedEarnings: BigDecimal?,
    val treasuryStock: BigDecimal?,
    val otherEquity: BigDecimal?,
    val totalEquity: BigDecimal?,

    // Cash Flow
    val netCashFromOperating: BigDecimal?,
    val netCashFromInvesting: BigDecimal?,
    val netCashFromFinancing: BigDecimal?,
    val netChangeInCash: BigDecimal?,

    // Market Data
    val marketCapitalization: BigDecimal?,
    val sharesOutstanding: BigDecimal?,
    val marketPricePerShare: BigDecimal?,
    val bookValuePerShare: BigDecimal?,
    val earningsPerShare: BigDecimal?,

    // Growth Metrics
    val revenueGrowth: BigDecimal?,
    val grossProfitGrowth: BigDecimal?,
    val netIncomeGrowth: BigDecimal?,
    val assetGrowth: BigDecimal?,
    val receivablesGrowth: BigDecimal?,
    val inventoryGrowth: BigDecimal?,
    val liabilityGrowth: BigDecimal?,

    val createdAt: OffsetDateTime?,
    val updatedAt: OffsetDateTime?
)

data class FinancialDataSummaryResponse(
    val id: Long,
    val statementId: Long,
    val companyName: String,
    val year: Int,
    val revenue: BigDecimal?,
    val netIncome: BigDecimal?,
    val totalAssets: BigDecimal?,
    val totalLiabilities: BigDecimal?,
    val updatedAt: OffsetDateTime?
)

/**
 * Response DTO for financial trend data over multiple years
 */
data class FinancialTrendResponse(
    val year: Int,
    val revenue: BigDecimal?,
    val netIncome: BigDecimal?,
    val totalAssets: BigDecimal?,
    val totalLiabilities: BigDecimal?,
    val totalEquity: BigDecimal?,
    val revenueGrowth: BigDecimal?,
    val netIncomeGrowth: BigDecimal?,
    val assetGrowth: BigDecimal?,
    val liabilityGrowth: BigDecimal?
)

/**
 * Response DTO for financial metrics summary
 */
data class FinancialMetricsSummaryResponse(
    val companyId: Long,
    val companyName: String,
    val stockCode: String,
    val year: Int,
    val revenue: BigDecimal?,
    val netIncome: BigDecimal?,
    val totalAssets: BigDecimal?,
    val totalLiabilities: BigDecimal?,
    val totalEquity: BigDecimal?,
    val profitMargin: BigDecimal?,
    val currentRatio: BigDecimal?,
    val debtToEquity: BigDecimal?,
    val returnOnAssets: BigDecimal?,
    val statementId: Long
)