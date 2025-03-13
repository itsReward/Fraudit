package com.fraudit.fraudit.dto.report

data class GenerateReportRequest(
    val reportType: String,
    val parameters: Map<String, Any>
)

data class AvailableReportsResponse(
    val reportTypes: List<ReportTypeInfo>
)

data class ReportTypeInfo(
    val type: String,
    val name: String,
    val description: String,
    val requiredParameters: List<ParameterInfo>
)

data class ParameterInfo(
    val name: String,
    val type: String,
    val description: String,
    val required: Boolean
)