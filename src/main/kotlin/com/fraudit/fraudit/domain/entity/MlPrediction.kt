package com.fraudit.fraudit.domain.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.OffsetDateTime

// ML Predictions Entity
@Entity
@Table(name = "ml_predictions")
data class MlPrediction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prediction_id")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statement_id", nullable = false)
    val statement: FinancialStatement,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    val model: MlModel,

    @Column(name = "fraud_probability", nullable = false)
    val fraudProbability: BigDecimal,

    @Column(name = "feature_importance", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    val featureImportance: String? = null,

    @Column(name = "prediction_explanation")
    val predictionExplanation: String? = null,

    @Column(name = "predicted_at", updatable = false)
    val predictedAt: OffsetDateTime = OffsetDateTime.now()
)