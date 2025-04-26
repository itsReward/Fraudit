package com.fraudit.fraudit.domain.entity

import com.fraudit.fraudit.domain.enum.StatementStatus
import com.fraudit.fraudit.domain.enum.StatementType
import jakarta.persistence.*
import java.time.OffsetDateTime

// Financial Statement Entity
@Entity
@Table(name = "financial_statements")
data class FinancialStatement(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "statement_id")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_year_id", nullable = false)
    val fiscalYear: FiscalYear,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Enumerated(EnumType.STRING)
    @Column(name = "statement_type", nullable = false)
    val statementType: StatementType,

    @Column(name = "period")
    val period: String? = null,

    @Column(name = "upload_date", updatable = false)
    val uploadDate: OffsetDateTime = OffsetDateTime.now(),

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    var status: StatementStatus = StatementStatus.PENDING,

    @OneToOne(mappedBy = "statement", cascade = [CascadeType.ALL], orphanRemoval = true)
    val financialData: FinancialData? = null,

    @OneToMany(mappedBy = "statement", cascade = [CascadeType.ALL], orphanRemoval = true)
    val documents: MutableSet<DocumentStorage> = mutableSetOf(),

    @OneToOne(mappedBy = "statement", cascade = [CascadeType.ALL], orphanRemoval = true)
    val financialRatios: FinancialRatios? = null,

    @OneToOne(mappedBy = "statement", cascade = [CascadeType.ALL], orphanRemoval = true)
    val altmanZScore: AltmanZScore? = null,

    @OneToOne(mappedBy = "statement", cascade = [CascadeType.ALL], orphanRemoval = true)
    val beneishMScore: BeneishMScore? = null,

    @OneToOne(mappedBy = "statement", cascade = [CascadeType.ALL], orphanRemoval = true)
    val piotroskiFScore: PiotroskiFScore? = null,

    @OneToOne(mappedBy = "statement", cascade = [CascadeType.ALL], orphanRemoval = true)
    val mlFeatures: MlFeatures? = null,

    @OneToMany(mappedBy = "statement", cascade = [CascadeType.ALL], orphanRemoval = true)
    val mlPredictions: MutableSet<MlPrediction> = mutableSetOf(),

    @OneToOne(mappedBy = "statement", cascade = [CascadeType.ALL], orphanRemoval = true)
    val fraudRiskAssessment: FraudRiskAssessment? = null
)


