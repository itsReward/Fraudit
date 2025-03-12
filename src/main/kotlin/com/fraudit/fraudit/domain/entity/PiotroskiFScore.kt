package com.fraudit.fraudit.domain.entity

import com.fraudit.fraudit.domain.enum.FinancialStrength
import jakarta.persistence.*
import java.time.OffsetDateTime

// Piotroski F-Score Entity
@Entity
@Table(name = "piotroski_f_score")
data class PiotroskiFScore(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "f_score_id")
    val id: Long? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statement_id", nullable = false, unique = true)
    val statement: FinancialStatement,

    @Column(name = "positive_net_income")
    val positiveNetIncome: Boolean? = null,

    @Column(name = "positive_operating_cash_flow")
    val positiveOperatingCashFlow: Boolean? = null,

    @Column(name = "cash_flow_greater_than_net_income")
    val cashFlowGreaterThanNetIncome: Boolean? = null,

    @Column(name = "improving_roa")
    val improvingRoa: Boolean? = null,

    @Column(name = "decreasing_leverage")
    val decreasingLeverage: Boolean? = null,

    @Column(name = "improving_current_ratio")
    val improvingCurrentRatio: Boolean? = null,

    @Column(name = "no_new_shares")
    val noNewShares: Boolean? = null,

    @Column(name = "improving_gross_margin")
    val improvingGrossMargin: Boolean? = null,

    @Column(name = "improving_asset_turnover")
    val improvingAssetTurnover: Boolean? = null,

    @Column(name = "f_score")
    val fScore: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "financial_strength")
    val financialStrength: FinancialStrength? = null,

    @Column(name = "calculated_at", updatable = false)
    val calculatedAt: OffsetDateTime = OffsetDateTime.now()
)