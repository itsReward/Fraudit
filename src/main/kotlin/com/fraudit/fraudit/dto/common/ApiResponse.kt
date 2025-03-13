package com.fraudit.fraudit.dto.common

import java.time.OffsetDateTime

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
    val errors: List<String> = emptyList(),
    val timestamp: OffsetDateTime = OffsetDateTime.now()
)

data class PagedResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val first: Boolean,
    val last: Boolean
)