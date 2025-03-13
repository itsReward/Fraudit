package com.fraudit.fraudit.dto.ml

import java.time.OffsetDateTime
import java.util.UUID

data class MlModelRequest(
    val modelName: String,
    val modelType: String = "RANDOM_FOREST",
    val modelVersion: String,
    val featureList: String,
    val performanceMetrics: String,
    val isActive: Boolean = false,
    val modelPath: String
)

data class MlModelResponse(
    val id: Long,
    val modelName: String,
    val modelType: String,
    val modelVersion: String,
    val featureList: String,
    val performanceMetrics: String,
    val trainedDate: OffsetDateTime,
    val isActive: Boolean,
    val modelPath: String,
    val createdById: UUID?,
    val createdByUsername: String?
)

data class MlModelSummaryResponse(
    val id: Long,
    val modelName: String,
    val modelVersion: String,
    val modelType: String,
    val isActive: Boolean,
    val trainedDate: OffsetDateTime
)