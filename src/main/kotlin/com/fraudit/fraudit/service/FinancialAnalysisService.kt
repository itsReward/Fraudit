package com.fraudit.fraudit.service

import com.fraudit.fraudit.domain.entity.*
import com.fraudit.fraudit.domain.enum.RiskLevel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

/**
 * Service for performing financial analysis and fraud detection calculations
 */
interface FinancialAnalysisService {
    /**
     * Calculate all financial scores and ratios for a financial statement
     */
    fun calculateAllScoresAndRatios(statementId: Long, userId: UUID): FraudRiskAssessment

    /**
     * Calculate financial ratios for a financial statement
     */
    fun calculateFinancialRatios(statementId: Long, userId: UUID): FinancialRatios

    /**
     * Calculate Altman Z-Score for a financial statement
     */
    fun calculateAltmanZScore(statementId: Long, userId: UUID): AltmanZScore

    /**
     * Calculate Beneish M-Score for a financial statement
     */
    fun calculateBeneishMScore(statementId: Long, userId: UUID): BeneishMScore

    /**
     * Calculate Piotroski F-Score for a financial statement
     */
    fun calculatePiotroskiFScore(statementId: Long, userId: UUID): PiotroskiFScore

    /**
     * Prepare ML features for a financial statement
     */
    fun prepareMlFeatures(statementId: Long, userId: UUID): MlFeatures

    /**
     * Perform ML prediction for a financial statement
     */
    fun performMlPrediction(statementId: Long, userId: UUID): MlPrediction

    /**
     * Assess fraud risk for a financial statement
     */
    fun assessFraudRisk(statementId: Long, userId: UUID): FraudRiskAssessment

    /**
     * Generate risk alerts for a financial statement based on assessment
     */
    fun generateRiskAlerts(assessmentId: Long, userId: UUID): List<RiskAlert>

    /**
     * Get a fraud risk assessment by its ID
     */
    fun getFraudRiskAssessmentById(id: Long): FraudRiskAssessment

    /**
     * Get a fraud risk assessment by statement ID
     */
    fun getFraudRiskAssessmentByStatementId(statementId: Long): FraudRiskAssessment?

    /**
     * Get all fraud risk assessments with pagination
     */
    fun getAllFraudRiskAssessments(pageable: Pageable): Page<FraudRiskAssessment>

    /**
     * Get fraud risk assessments by company with pagination
     */
    fun getFraudRiskAssessmentsByCompany(companyId: Long, pageable: Pageable): Page<FraudRiskAssessment>

    /**
     * Get fraud risk assessments by risk level with pagination
     */
    fun getFraudRiskAssessmentsByRiskLevel(riskLevel: RiskLevel, pageable: Pageable): Page<FraudRiskAssessment>

    /**
     * Get fraud risk assessments by company and risk level with pagination
     */
    fun getFraudRiskAssessmentsByCompanyAndRiskLevel(companyId: Long, riskLevel: RiskLevel, pageable: Pageable): Page<FraudRiskAssessment>

    /**
     * Get financial ratios for a statement
     */
    fun getFinancialRatios(statementId: Long): FinancialRatios?

    /**
     * Get Altman Z-Score for a statement
     */
    fun getAltmanZScore(statementId: Long): AltmanZScore?

    /**
     * Get Beneish M-Score for a statement
     */
    fun getBeneishMScore(statementId: Long): BeneishMScore?

    /**
     * Get Piotroski F-Score for a statement
     */
    fun getPiotroskiFScore(statementId: Long): PiotroskiFScore?

    /**
     * Delete any existing analysis data for a financial statement
     */
    fun deleteExistingAnalysis(statementId: Long)
}