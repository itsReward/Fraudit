package com.fraudit.fraudit.dto.ml

import java.math.BigDecimal
import java.time.OffsetDateTime

data class MlPredictionResponse(
    val id: Long,
    val statementId: Long,
    val companyId: Long,
    val companyName: String,
    val year: Int,
    val modelId: Long,
    val modelName: String,
    val modelVersion: String,
    val fraudProbability: BigDecimal,
    val featureImportance: Map<String, Double>?,
    val predictionExplanation: String?,
    val predictedAt: OffsetDateTime
)