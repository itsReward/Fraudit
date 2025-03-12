package com.fraudit.fraudit.domain.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.OffsetDateTime

// Financial Ratios Entity
@Entity
@Table(name = "financial_ratios")
data class FinancialRatios(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ratio_id")
    val id: Long? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statement_id", nullable = false, unique = true)
    val statement: FinancialStatement,

    // Liquidity Ratios
    @Column(name = "current_ratio")
    val currentRatio: BigDecimal? = null,

    @Column(name = "quick_ratio")
    val quickRatio: BigDecimal? = null,

    @Column(name = "cash_ratio")
    val cashRatio: BigDecimal? = null,

    // Profitability Ratios
    @Column(name = "gross_margin")
    val grossMargin: BigDecimal? = null,

    @Column(name = "operating_margin")
    val operatingMargin: BigDecimal? = null,

    @Column(name = "net_profit_margin")
    val netProfitMargin: BigDecimal? = null,

    @Column(name = "return_on_assets")
    val returnOnAssets: BigDecimal? = null,

    @Column(name = "return_on_equity")
    val returnOnEquity: BigDecimal? = null,

    // Efficiency Ratios
    @Column(name = "asset_turnover")
    val assetTurnover: BigDecimal? = null,

    @Column(name = "inventory_turnover")
    val inventoryTurnover: BigDecimal? = null,

    @Column(name = "accounts_receivable_turnover")
    val accountsReceivableTurnover: BigDecimal? = null,

    @Column(name = "days_sales_outstanding")
    val daysSalesOutstanding: BigDecimal? = null,

    // Leverage Ratios
    @Column(name = "debt_to_equity")
    val debtToEquity: BigDecimal? = null,

    @Column(name = "debt_ratio")
    val debtRatio: BigDecimal? = null,

    @Column(name = "interest_coverage")
    val interestCoverage: BigDecimal? = null,

    // Valuation Ratios
    @Column(name = "price_to_earnings")
    val priceToEarnings: BigDecimal? = null,

    @Column(name = "price_to_book")
    val priceToBook: BigDecimal? = null,

    // Quality Metrics
    @Column(name = "accrual_ratio")
    val accrualRatio: BigDecimal? = null,

    @Column(name = "earnings_quality")
    val earningsQuality: BigDecimal? = null,

    @Column(name = "calculated_at", updatable = false)
    val calculatedAt: OffsetDateTime = OffsetDateTime.now()
)
