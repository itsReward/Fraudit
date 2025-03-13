package com.fraudit.fraudit.service.impl

import com.fraudit.fraudit.repository.*
import com.fraudit.fraudit.service.AuditLogService
import com.fraudit.fraudit.service.ReportService
import org.springframework.stereotype.Service
import java.io.OutputStream
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class ReportServiceImpl(
    private val financialStatementRepository: FinancialStatementRepository,
    private val companyRepository: CompanyRepository,
    private val fraudRiskAssessmentRepository: FraudRiskAssessmentRepository,
    private val altmanZScoreRepository: AltmanZScoreRepository,
    private val beneishMScoreRepository: BeneishMScoreRepository,
    private val piotroskiFScoreRepository: PiotroskiFScoreRepository,
    private val financialRatiosRepository: FinancialRatiosRepository,
    private val mlPredictionRepository: MlPredictionRepository,
    private val auditLogService: AuditLogService
) : ReportService {

    override fun generateFraudRiskReport(statementId: Long, outputStream: OutputStream, userId: UUID) {
        // Get statement details
        val statement = financialStatementRepository.findById(statementId)
            .orElseThrow { IllegalArgumentException("Financial statement not found with id: $statementId") }

        // Get risk assessment
        val assessment = fraudRiskAssessmentRepository.findByStatementId(statementId)
            ?: throw IllegalArgumentException("Fraud risk assessment not found for statement id: $statementId")

        // Get individual scores
        val zScore = altmanZScoreRepository.findByStatementId(statementId)
            ?: throw IllegalArgumentException("Altman Z-Score not found for statement id: $statementId")

        val mScore = beneishMScoreRepository.findByStatementId(statementId)
            ?: throw IllegalArgumentException("Beneish M-Score not found for statement id: $statementId")

        val fScore = piotroskiFScoreRepository.findByStatementId(statementId)
            ?: throw IllegalArgumentException("Piotroski F-Score not found for statement id: $statementId")

        val company = statement.fiscalYear.company

        // Generate PDF report content
        val reportContent = buildString {
            appendLine("FRAUD RISK ASSESSMENT REPORT")
            appendLine("===============================")
            appendLine()
            appendLine("COMPANY: ${company.name} (${company.stockCode})")
            appendLine("FISCAL YEAR: ${statement.fiscalYear.year}")
            appendLine("STATEMENT TYPE: ${statement.statementType.name}")
            appendLine("ASSESSMENT DATE: ${assessment.assessedAt.format(DateTimeFormatter.ISO_DATE)}")
            appendLine()
            appendLine("RISK SUMMARY")
            appendLine("---------------")
            appendLine("OVERALL RISK LEVEL: ${assessment.riskLevel?.name ?: "Unknown"}")
            appendLine("OVERALL RISK SCORE: ${assessment.overallRiskScore ?: BigDecimal.ZERO}")
            appendLine()
            appendLine("COMPONENT SCORES")
            appendLine("---------------")
            appendLine("Altman Z-Score: ${zScore.zScore ?: "N/A"} (Risk Category: ${zScore.riskCategory?.name ?: "Unknown"})")
            appendLine("Beneish M-Score: ${mScore.mScore ?: "N/A"} (Manipulation Probability: ${mScore.manipulationProbability?.name ?: "Unknown"})")
            appendLine("Piotroski F-Score: ${fScore.fScore ?: "N/A"} (Financial Strength: ${fScore.financialStrength?.name ?: "Unknown"})")
            appendLine()
            appendLine("ASSESSMENT DETAILS")
            appendLine("---------------")
            appendLine(assessment.assessmentSummary ?: "No detailed assessment available.")
        }

        // Write to PDF (simplified - in a real implementation, use a PDF library)
        outputStream.write(reportContent.toByteArray())

        auditLogService.logEvent(
            userId = userId,
            action = "GENERATE_REPORT",
            entityType = "FRAUD_RISK_REPORT",
            entityId = statementId.toString(),
            details = "Generated fraud risk report for ${company.name} (${statement.fiscalYear.year})"
        )
    }

    override fun generateCompanyOverviewReport(companyId: Long, outputStream: OutputStream, userId: UUID) {
        // Get company details
        val company = companyRepository.findById(companyId)
            .orElseThrow { IllegalArgumentException("Company not found with id: $companyId") }

        // Get all statements for this company
        val statements = financialStatementRepository.findByFiscalYearCompanyId(companyId)
            .sortedBy { it.fiscalYear.year }

        // Generate PDF report content
        val reportContent = buildString {
            appendLine("COMPANY OVERVIEW REPORT")
            appendLine("===============================")
            appendLine()
            appendLine("COMPANY: ${company.name} (${company.stockCode})")
            appendLine("SECTOR: ${company.sector ?: "Not specified"}")
            appendLine("LISTING DATE: ${company.listingDate ?: "Not specified"}")
            appendLine()

            if (statements.isEmpty()) {
                appendLine("No financial statements available for this company.")
            } else {
                appendLine("FINANCIAL STATEMENTS SUMMARY")
                appendLine("---------------------------")

                statements.forEach { statement ->
                    val assessment = fraudRiskAssessmentRepository.findByStatementId(statement.id!!)

                    appendLine("YEAR: ${statement.fiscalYear.year} (${statement.statementType.name})")
                    if (assessment != null) {
                        appendLine("RISK LEVEL: ${assessment.riskLevel?.name ?: "Unknown"}")
                        appendLine("RISK SCORE: ${assessment.overallRiskScore ?: "N/A"}")
                    } else {
                        appendLine("No risk assessment available for this statement.")
                    }
                    appendLine()
                }
            }

            appendLine("FRAUD RISK TREND")
            appendLine("------------------")
            val assessments = statements.mapNotNull { statement ->
                statement.id?.let { fraudRiskAssessmentRepository.findByStatementId(it) }
            }

            if (assessments.isEmpty()) {
                appendLine("No risk assessment data available for trend analysis.")
            } else {
                assessments.forEach { assessment ->
                    val year = assessment.statement.fiscalYear.year
                    appendLine("$year: ${assessment.overallRiskScore ?: "N/A"} (${assessment.riskLevel?.name ?: "Unknown"})")
                }
            }
        }

        // Write to PDF (simplified - in a real implementation, use a PDF library)
        outputStream.write(reportContent.toByteArray())

        auditLogService.logEvent(
            userId = userId,
            action = "GENERATE_REPORT",
            entityType = "COMPANY_OVERVIEW_REPORT",
            entityId = companyId.toString(),
            details = "Generated company overview report for ${company.name}"
        )
    }

    override fun generateComparativeReport(companyIds: List<Long>, outputStream: OutputStream, userId: UUID) {
        // Get company details
        val companies = companyRepository.findAllById(companyIds)

        if (companies.isEmpty()) {
            throw IllegalArgumentException("No companies found with the provided IDs")
        }

        // Generate PDF report content
        val reportContent = buildString {
            appendLine("COMPARATIVE ANALYSIS REPORT")
            appendLine("===============================")
            appendLine()
            appendLine("Companies included in analysis:")
            companies.forEach { company ->
                appendLine("- ${company.name} (${company.stockCode})")
            }
            appendLine()

            appendLine("RISK LEVEL COMPARISON")
            appendLine("----------------------")

            // Table header
            append("Company".padEnd(30))
            append("Latest Year".padEnd(15))
            append("Risk Score".padEnd(15))
            appendLine("Risk Level")

            appendLine("-".repeat(80))

            companies.forEach { company ->
                val assessments = fraudRiskAssessmentRepository.findLatestByCompanyId(company.id!!)

                if (assessments.isNotEmpty()) {
                    val latestAssessment = assessments.first()
                    val year = latestAssessment.statement.fiscalYear.year

                    append(company.name.take(27).padEnd(30))
                    append("$year".padEnd(15))
                    append("${latestAssessment.overallRiskScore ?: "N/A"}".padEnd(15))
                    appendLine("${latestAssessment.riskLevel?.name ?: "Unknown"}")
                } else {
                    append(company.name.take(27).padEnd(30))
                    append("N/A".padEnd(15))
                    append("N/A".padEnd(15))
                    appendLine("No data")
                }
            }

            appendLine()
            appendLine("FINANCIAL INDICATORS COMPARISON")
            appendLine("--------------------------------")

            // For each company, get the latest financial ratios
            companies.forEach { company ->
                appendLine()
                appendLine("${company.name} (${company.stockCode}):")

                val statements = financialStatementRepository.findByFiscalYearCompanyId(company.id!!)
                    .sortedByDescending { it.fiscalYear.year }

                if (statements.isEmpty()) {
                    appendLine("No financial data available")
                } else {
                    val latestStatement = statements.first()
                    val ratios = financialRatiosRepository.findByStatementId(latestStatement.id!!)

                    if (ratios != null) {
                        appendLine("Year: ${latestStatement.fiscalYear.year}")
                        appendLine("Current Ratio: ${ratios.currentRatio ?: "N/A"}")
                        appendLine("Quick Ratio: ${ratios.quickRatio ?: "N/A"}")
                        appendLine("Debt to Equity: ${ratios.debtToEquity ?: "N/A"}")
                        appendLine("Return on Assets: ${ratios.returnOnAssets ?: "N/A"}")
                        appendLine("Return on Equity: ${ratios.returnOnEquity ?: "N/A"}")
                        appendLine("Net Profit Margin: ${ratios.netProfitMargin ?: "N/A"}")
                    } else {
                        appendLine("No financial ratio data available")
                    }
                }
            }
        }

        // Write to PDF (simplified - in a real implementation, use a PDF library)
        outputStream.write(reportContent.toByteArray())

        auditLogService.logEvent(
            userId = userId,
            action = "GENERATE_REPORT",
            entityType = "COMPARATIVE_REPORT",
            entityId = companyIds.joinToString(","),
            details = "Generated comparative analysis report for ${companies.size} companies"
        )
    }

    override fun generateHighRiskSummaryReport(riskThreshold: Int, outputStream: OutputStream, userId: UUID) {
        // Get all assessments with risk score above threshold
        val highRiskAssessments = fraudRiskAssessmentRepository.findAll()
            .filter { it.overallRiskScore != null && it.overallRiskScore!! >= BigDecimal(riskThreshold) }
            .sortedByDescending { it.overallRiskScore }

        // Generate PDF report content
        val reportContent = buildString {
            appendLine("HIGH RISK COMPANIES SUMMARY REPORT")
            appendLine("===================================")
            appendLine()
            appendLine("Risk threshold: $riskThreshold")
            appendLine("Number of high-risk companies: ${highRiskAssessments.size}")
            appendLine()

            if (highRiskAssessments.isEmpty()) {
                appendLine("No companies with risk score above the threshold were found.")
            } else {
                appendLine("HIGH RISK COMPANIES")
                appendLine("-------------------")

                // Table header
                append("Company".padEnd(30))
                append("Year".padEnd(10))
                append("Risk Score".padEnd(15))
                appendLine("Risk Level")

                appendLine("-".repeat(80))

                highRiskAssessments.forEach { assessment ->
                    val company = assessment.statement.fiscalYear.company
                    val year = assessment.statement.fiscalYear.year

                    append(company.name.take(27).padEnd(30))
                    append("$year".padEnd(10))
                    append("${assessment.overallRiskScore}".padEnd(15))
                    appendLine("${assessment.riskLevel?.name ?: "Unknown"}")
                }

                appendLine()
                appendLine("DETAILED RISK FACTORS")
                appendLine("---------------------")

                highRiskAssessments.forEach { assessment ->
                    val company = assessment.statement.fiscalYear.company

                    appendLine()
                    appendLine("${company.name} (${company.stockCode}) - Year: ${assessment.statement.fiscalYear.year}")
                    appendLine("Risk Score: ${assessment.overallRiskScore} (${assessment.riskLevel?.name ?: "Unknown"})")

                    // Add component risk scores
                    appendLine("Z-Score Risk: ${assessment.zScoreRisk ?: "N/A"}")
                    appendLine("M-Score Risk: ${assessment.mScoreRisk ?: "N/A"}")
                    appendLine("F-Score Risk: ${assessment.fScoreRisk ?: "N/A"}")
                    appendLine("Financial Ratio Risk: ${assessment.financialRatioRisk ?: "N/A"}")
                    appendLine("ML Prediction Risk: ${assessment.mlPredictionRisk ?: "N/A"}")

                    // Add assessment summary if available
                    if (!assessment.assessmentSummary.isNullOrBlank()) {
                        appendLine()
                        appendLine("Assessment Summary:")
                        appendLine(assessment.assessmentSummary)
                    }
                }
            }
        }

        // Write to PDF (simplified - in a real implementation, use a PDF library)
        outputStream.write(reportContent.toByteArray())

        auditLogService.logEvent(
            userId = userId,
            action = "GENERATE_REPORT",
            entityType = "HIGH_RISK_SUMMARY_REPORT",
            entityId = "threshold_$riskThreshold",
            details = "Generated high risk summary report for ${highRiskAssessments.size} companies"
        )
    }
}