package com.fraudit.fraudit.domain.entity

import com.fraudit.fraudit.domain.enum.ManipulationProbability
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.OffsetDateTime

// Beneish M-Score Entity
@Entity
@Table(name = "beneish_m_score")
data class BeneishMScore(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "m_score_id")
    val id: Long? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statement_id", nullable = false, unique = true)
    val statement: FinancialStatement,

    @Column(name = "days_sales_receivables_index")
    val daysSalesReceivablesIndex: BigDecimal? = null,

    @Column(name = "gross_margin_index")
    val grossMarginIndex: BigDecimal? = null,

    @Column(name = "asset_quality_index")
    val assetQualityIndex: BigDecimal? = null,

    @Column(name = "sales_growth_index")
    val salesGrowthIndex: BigDecimal? = null,

    @Column(name = "depreciation_index")
    val depreciationIndex: BigDecimal? = null,

    @Column(name = "sg_admin_expenses_index")
    val sgAdminExpensesIndex: BigDecimal? = null,

    @Column(name = "leverage_index")
    val leverageIndex: BigDecimal? = null,

    @Column(name = "total_accruals_to_total_assets")
    val totalAccrualsToTotalAssets: BigDecimal? = null,

    @Column(name = "m_score")
    val mScore: BigDecimal? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "manipulation_probability")
    val manipulationProbability: ManipulationProbability? = null,

    @Column(name = "calculated_at", updatable = false)
    val calculatedAt: OffsetDateTime = OffsetDateTime.now()
)
