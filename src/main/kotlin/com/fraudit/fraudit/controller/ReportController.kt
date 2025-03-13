package com.fraudit.fraudit.controller

import com.fraudit.fraudit.dto.common.ApiResponse
import com.fraudit.fraudit.dto.report.*
import com.fraudit.fraudit.service.ReportService
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.ByteArrayOutputStream
import jakarta.validation.Valid

@RestController
@RequestMapping("/api/reports")
class ReportController(private val reportService: ReportService) {

    @GetMapping("/available")
    fun getAvailableReports(): ResponseEntity<ApiResponse<AvailableReportsResponse>> {
        // Implementation for getting available report types
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Available report types retrieved successfully",
                data = AvailableReportsResponse(
                    reportTypes = listOf(
                        ReportTypeInfo(
                            type = "fraud-risk",
                            name = "Fraud Risk Assessment Report",
                            description = "Detailed report of fraud risk assessment for a specific financial statement",
                            requiredParameters = listOf(
                                ParameterInfo(
                                    name = "statementId",
                                    type = "long",
                                    description = "ID of the financial statement",
                                    required = true
                                )
                            )
                        ),
                        ReportTypeInfo(
                            type = "company-overview",
                            name = "Company Overview Report",
                            description = "Overview report of a company's financial health and fraud risk",
                            requiredParameters = listOf(
                                ParameterInfo(
                                    name = "companyId",
                                    type = "long",
                                    description = "ID of the company",
                                    required = true
                                )
                            )
                        ),
                        ReportTypeInfo(
                            type = "comparative",
                            name = "Comparative Analysis Report",
                            description = "Comparative analysis of multiple companies",
                            requiredParameters = listOf(
                                ParameterInfo(
                                    name = "companyIds",
                                    type = "array",
                                    description = "List of company IDs to compare",
                                    required = true
                                )
                            )
                        ),
                        ReportTypeInfo(
                            type = "high-risk-summary",
                            name = "High Risk Summary Report",
                            description = "Summary report of all high-risk companies",
                            requiredParameters = listOf(
                                ParameterInfo(
                                    name = "riskThreshold",
                                    type = "int",
                                    description = "Minimum risk score to include (1-100)",
                                    required = true
                                )
                            )
                        )
                    )
                )
            )
        )
    }

    @PostMapping("/generate")
    fun generateReport(@Valid @RequestBody request: GenerateReportRequest): ResponseEntity<Resource> {
        // Implementation for generating a report
        val outputStream = ByteArrayOutputStream()

        when (request.reportType) {
            "fraud-risk" -> {
                val statementId = request.parameters["statementId"] as Long
                reportService.generateFraudRiskReport(statementId, outputStream, java.util.UUID.randomUUID())
            }
            "company-overview" -> {
                val companyId = request.parameters["companyId"] as Long
                reportService.generateCompanyOverviewReport(companyId, outputStream, java.util.UUID.randomUUID())
            }
            "comparative" -> {
                @Suppress("UNCHECKED_CAST")
                val companyIds = request.parameters["companyIds"] as List<Long>
                reportService.generateComparativeReport(companyIds, outputStream, java.util.UUID.randomUUID())
            }
            "high-risk-summary" -> {
                val riskThreshold = request.parameters["riskThreshold"] as Int
                reportService.generateHighRiskSummaryReport(riskThreshold, outputStream, java.util.UUID.randomUUID())
            }
            else -> throw IllegalArgumentException("Unsupported report type: ${request.reportType}")
        }

        val content = outputStream.toByteArray()
        val fileName = "${request.reportType}-${System.currentTimeMillis()}.pdf"

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$fileName\"")
            .body(ByteArrayResource(content))
    }
}