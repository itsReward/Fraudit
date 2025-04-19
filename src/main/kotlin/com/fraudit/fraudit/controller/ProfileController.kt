package com.fraudit.fraudit.controller

import com.fraudit.fraudit.domain.entity.User
import com.fraudit.fraudit.dto.auth.PasswordChangeRequest
import com.fraudit.fraudit.dto.common.ApiResponse
import com.fraudit.fraudit.dto.user.ProfileUpdateRequest
import com.fraudit.fraudit.dto.user.UserResponse
import com.fraudit.fraudit.service.AuditLogService
import com.fraudit.fraudit.service.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/profile")
@PreAuthorize("isAuthenticated()")
class ProfileController(
    private val userService: UserService,
    private val auditLogService: AuditLogService
) {

    @GetMapping
    fun getProfile(@AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<ApiResponse<UserResponse>> {
        val userId = UUID.fromString(userDetails.username)
        val user = userService.findById(userId)

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Profile retrieved successfully",
                data = mapToUserResponse(user)
            )
        )
    }

    @PutMapping
    fun updateProfile(
        @Valid @RequestBody profileUpdateRequest: ProfileUpdateRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<UserResponse>> {
        val userId = UUID.fromString(userDetails.username)
        val existingUser = userService.findById(userId)

        // Update user with profile changes
        val updatedUser = existingUser.copy(
            firstName = profileUpdateRequest.firstName,
            lastName = profileUpdateRequest.lastName,
            email = profileUpdateRequest.email
        )

        // Only change these if the email is actually changing
        if (existingUser.email != profileUpdateRequest.email && !userService.isEmailAvailable(profileUpdateRequest.email)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse(
                    success = false,
                    message = "Email is already in use",
                    errors = listOf("Email address is already in use by another account")
                )
            )
        }

        val savedUser = userService.updateUser(updatedUser)

        auditLogService.logEvent(
            userId = userId,
            action = "UPDATE_PROFILE",
            entityType = "USER",
            entityId = userId.toString(),
            details = "User updated their profile information"
        )

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Profile updated successfully",
                data = mapToUserResponse(savedUser)
            )
        )
    }

    @PutMapping("/password")
    fun changePassword(
        @Valid @RequestBody passwordChangeRequest: PasswordChangeRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Void>> {
        val userId = UUID.fromString(userDetails.username)

        // Check if passwords match
        if (passwordChangeRequest.newPassword != passwordChangeRequest.confirmNewPassword) {
            return ResponseEntity.badRequest().body(
                ApiResponse(
                    success = false,
                    message = "Passwords do not match",
                    errors = listOf("New password and confirmation do not match")
                )
            )
        }

        // Change password
        val success = userService.changePassword(
            userId,
            passwordChangeRequest.currentPassword,
            passwordChangeRequest.newPassword
        )

        if (!success) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiResponse(
                    success = false,
                    message = "Current password is incorrect",
                    errors = listOf("Current password is incorrect")
                )
            )
        }

        auditLogService.logEvent(
            userId = userId,
            action = "CHANGE_PASSWORD",
            entityType = "USER",
            entityId = userId.toString(),
            details = "User changed their password"
        )

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Password changed successfully"
            )
        )
    }

    @GetMapping("/audit-logs")
    fun getUserActivityLogs(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        val userId = UUID.fromString(userDetails.username)

        // Get activity statistics for the user
        val activityStats = userService.getUserActivityStats(userId, page, size)

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "User activity logs retrieved successfully",
                data = activityStats
            )
        )
    }

    private fun mapToUserResponse(user: User): UserResponse {
        return UserResponse(
            id = user.id,
            username = user.username,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            role = user.role,
            active = user.active,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt
        )
    }
}