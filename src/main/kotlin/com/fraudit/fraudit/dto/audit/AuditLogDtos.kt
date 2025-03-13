package com.fraudit.fraudit.dto.audit

import java.time.OffsetDateTime
import java.util.UUID

data class AuditLogResponse(
    val id: Long,
    val userId: UUID?,
    val username: String?,
    val action: String,
    val entityType: String,
    val entityId: String,
    val details: String?,
    val ipAddress: String?,
    val timestamp: OffsetDateTime
)

data class AuditLogSearchRequest(
    val userId: UUID? = null,
    val action: String? = null,
    val entityType: String? = null,
    val entityId: String? = null,
    val startDate: OffsetDateTime? = null,
    val endDate: OffsetDateTime? = null,
    val page: Int = 0,
    val size: Int = 20
)