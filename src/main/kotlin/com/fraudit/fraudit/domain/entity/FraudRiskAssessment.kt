package com.fraudit.fraudit.domain.entity

import com.fraudit.fraudit.domain.enum.RiskLevel
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.OffsetDateTime

// Fraud Risk Assessment Entity
@Entity
@Table(name = "fraud_risk_assessment")
data class FraudRiskAssessment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assessment_id")
    val id: Long? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statement_id", nullable = false, unique = true)
    val statement: FinancialStatement,

    @Column(name = "z_score_risk")
    val zScoreRisk: BigDecimal? = null,

    @Column(name = "m_score_risk")
    val mScoreRisk: BigDecimal? = null,

    @Column(name = "f_score_risk")
    val fScoreRisk: BigDecimal? = null,

    @Column(name = "financial_ratio_risk")
    val financialRatioRisk: BigDecimal? = null,

    @Column(name = "ml_prediction_risk")
    val mlPredictionRisk: BigDecimal? = null,

    @Column(name = "overall_risk_score")
    val overallRiskScore: BigDecimal? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level")
    val riskLevel: RiskLevel? = null,

    @Column(name = "assessment_summary")
    val assessmentSummary: String? = null,

    @Column(name = "assessed_at", updatable = false)
    val assessedAt: OffsetDateTime = OffsetDateTime.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessed_by")
    val assessedBy: User? = null,

    @OneToMany(mappedBy = "assessment", cascade = [CascadeType.ALL], orphanRemoval = true)
    val riskAlerts: MutableSet<RiskAlert> = mutableSetOf()
)
