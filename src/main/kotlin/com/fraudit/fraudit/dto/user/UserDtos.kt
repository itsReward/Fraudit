package com.fraudit.fraudit.dto.user

import com.fraudit.fraudit.domain.enum.UserRole
import java.time.OffsetDateTime
import java.util.UUID

data class UserResponse(
    val id: UUID,
    val username: String,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val role: UserRole,
    val active: Boolean,
    val createdAt: OffsetDateTime?,
    val updatedAt: OffsetDateTime?
)

data class UserSummaryResponse(
    val id: UUID,
    val username: String,
    val email: String,
    val fullName: String,
    val role: String
)

data class UserUpdateRequest(
    val firstName: String?,
    val lastName: String?,
    val email: String,
    val role: UserRole?
)

data class UserStatusUpdateRequest(
    val active: Boolean
)

