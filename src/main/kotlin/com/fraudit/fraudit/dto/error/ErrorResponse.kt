package com.fraudit.fraudit.dto.error

import java.time.OffsetDateTime

data class ErrorResponse(
    val status: Int,
    val message: String,
    val errors: List<String> = emptyList(),
    val path: String,
    val timestamp: OffsetDateTime = OffsetDateTime.now()
)

data class ValidationError(
    val field: String,
    val message: String
)