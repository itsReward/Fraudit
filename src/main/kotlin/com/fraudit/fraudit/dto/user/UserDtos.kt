package com.fraudit.fraudit.dto.user

import com.fraudit.fraudit.domain.enum.UserRole
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
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


data class CreateUserRequest(
    @field:NotBlank(message = "Username is required")
    @field:Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    val username: String,

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be valid")
    val email: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    val password: String,

    val firstName: String?,

    val lastName: String?,

    val role: UserRole = UserRole.ANALYST
)

/**
 * Request DTO for updating a user's profile
 */
data class ProfileUpdateRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be valid")
    val email: String,

    @field:Size(max = 100, message = "First name cannot exceed 100 characters")
    val firstName: String?,

    @field:Size(max = 100, message = "Last name cannot exceed 100 characters")
    val lastName: String?
)

/**
 * Request DTO for admin password reset
 */
data class AdminPasswordResetRequest(
    @field:NotBlank(message = "New password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    val newPassword: String
)

