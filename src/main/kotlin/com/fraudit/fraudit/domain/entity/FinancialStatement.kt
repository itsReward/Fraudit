package com.fraudit.fraudit.domain.entity

import com.fraudit.fraudit.domain.enum.StatementStatus
import com.fraudit.fraudit.domain.enum.StatementType
import jakarta.persistence.*
import java.time.OffsetDateTime

// Financial Statement Entity
@Entity
@Table(name = "financial_statements")
class FinancialStatement(
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
){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FinancialStatement) return false
        if (id != null && other.id != null) return id == other.id
        return false
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "FinancialStatement(id=$id, statementType=$statementType, status=$status)"
    }

    fun copy(
        id: Long? = this.id,
        fiscalYear: FiscalYear = this.fiscalYear,
        user: User = this.user,
        statementType: StatementType = this.statementType,
        period: String? = this.period,
        uploadDate: OffsetDateTime = this.uploadDate,
        status: StatementStatus = this.status,
        financialData: FinancialData? = this.financialData,
        documents: MutableSet<DocumentStorage> = this.documents,
        financialRatios: FinancialRatios? = this.financialRatios,
        altmanZScore: AltmanZScore? = this.altmanZScore,
        beneishMScore: BeneishMScore? = this.beneishMScore,
        piotroskiFScore: PiotroskiFScore? = this.piotroskiFScore,
        mlFeatures: MlFeatures? = this.mlFeatures,
        mlPredictions: MutableSet<MlPrediction> = this.mlPredictions,
        fraudRiskAssessment: FraudRiskAssessment? = this.fraudRiskAssessment
    ): FinancialStatement {
        return FinancialStatement(
            id = id,
            fiscalYear = fiscalYear,
            user = user,
            statementType = statementType,
            period = period,
            uploadDate = uploadDate,
            status = status,
            financialData = financialData,
            documents = documents,
            financialRatios = financialRatios,
            altmanZScore = altmanZScore,
            beneishMScore = beneishMScore,
            piotroskiFScore = piotroskiFScore,
            mlFeatures = mlFeatures,
            mlPredictions = mlPredictions,
            fraudRiskAssessment = fraudRiskAssessment
        )
    }

}



