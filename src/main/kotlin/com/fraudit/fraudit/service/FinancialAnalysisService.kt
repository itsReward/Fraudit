package com.fraudit.fraudit.service

import com.fraudit.fraudit.domain.entity.*
import java.util.UUID

interface FinancialAnalysisService {
    fun calculateAllScoresAndRatios(statementId: Long, userId: UUID): FraudRiskAssessment
    fun calculateFinancialRatios(statementId: Long, userId: UUID): FinancialRatios
    fun calculateAltmanZScore(statementId: Long, userId: UUID): AltmanZScore
    fun calculateBeneishMScore(statementId: Long, userId: UUID): BeneishMScore
    fun calculatePiotroskiFScore(statementId: Long, userId: UUID): PiotroskiFScore
    fun prepareMlFeatures(statementId: Long, userId: UUID): MlFeatures
    fun performMlPrediction(statementId: Long, userId: UUID): MlPrediction
    fun assessFraudRisk(statementId: Long, userId: UUID): FraudRiskAssessment
    fun generateRiskAlerts(assessmentId: Long, userId: UUID): List<RiskAlert>
}