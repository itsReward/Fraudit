package com.fraudit.fraudit.domain.entity

import com.fraudit.fraudit.domain.enum.RiskCategory
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.OffsetDateTime

// Altman Z-Score Entity
@Entity
@Table(name = "altman_z_score")
data class AltmanZScore(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "z_score_id")
    val id: Long? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statement_id", nullable = false, unique = true)
    val statement: FinancialStatement,

    @Column(name = "working_capital_to_total_assets")
    val workingCapitalToTotalAssets: BigDecimal? = null,

    @Column(name = "retained_earnings_to_total_assets")
    val retainedEarningsToTotalAssets: BigDecimal? = null,

    @Column(name = "ebit_to_total_assets")
    val ebitToTotalAssets: BigDecimal? = null,

    @Column(name = "market_value_equity_to_book_value_debt")
    val marketValueEquityToBookValueDebt: BigDecimal? = null,

    @Column(name = "sales_to_total_assets")
    val salesToTotalAssets: BigDecimal? = null,

    @Column(name = "z_score")
    val zScore: BigDecimal? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_category")
    val riskCategory: RiskCategory? = null,

    @Column(name = "calculated_at", updatable = false)
    val calculatedAt: OffsetDateTime = OffsetDateTime.now()
)