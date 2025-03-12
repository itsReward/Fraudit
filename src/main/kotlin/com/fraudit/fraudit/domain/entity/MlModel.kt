package com.fraudit.fraudit.domain.entity

import jakarta.persistence.*
import java.time.OffsetDateTime

// ML Models Entity
@Entity
@Table(name = "ml_models")
data class MlModel(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "model_id")
    val id: Long? = null,

    @Column(name = "model_name", nullable = false)
    val modelName: String,

    @Column(name = "model_type", nullable = false)
    val modelType: String = "RANDOM_FOREST",

    @Column(name = "model_version", nullable = false)
    val modelVersion: String,

    @Column(name = "feature_list", nullable = false)
    val featureList: String,

    @Column(name = "performance_metrics", nullable = false)
    val performanceMetrics: String,

    @Column(name = "trained_date", updatable = false)
    val trainedDate: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = false,

    @Column(name = "model_path", nullable = false)
    val modelPath: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    val createdBy: User? = null,

    @OneToMany(mappedBy = "model", cascade = [CascadeType.ALL], orphanRemoval = true)
    val predictions: MutableSet<MlPrediction> = mutableSetOf()
)
