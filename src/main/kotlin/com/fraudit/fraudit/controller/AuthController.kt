package com.fraudit.fraudit.controller

import com.fraudit.fraudit.domain.entity.User
import com.fraudit.fraudit.dto.auth.*
import com.fraudit.fraudit.dto.common.ApiResponse
import com.fraudit.fraudit.service.AuditLogService
import com.fraudit.fraudit.service.AuthService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
    private val auditLogService: AuditLogService
) {
    private val logger = LoggerFactory.getLogger(AuthController::class.java)

    @PostMapping("/login")
    fun login(@Valid @RequestBody loginRequest: LoginRequest): ResponseEntity<ApiResponse<LoginResponse>> {
        try {
            val loginResponse = authService.authenticate(loginRequest)

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Login successful",
                    data = loginResponse
                )
            )
        } catch (e: Exception) {
            logger.error("Login failed: ${e.message}")

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiResponse(
                    success = false,
                    message = "Login failed",
                    errors = listOf(e.message ?: "Invalid username or password")
                )
            )
        }
    }

    @PostMapping("/register")
    fun register(@Valid @RequestBody registerRequest: RegisterRequest): ResponseEntity<ApiResponse<Void>> {
        try {
            val createdUser = authService.register(registerRequest)

            auditLogService.logEvent(
                userId = createdUser.id,
                action = "REGISTER",
                entityType = "USER",
                entityId = createdUser.id.toString(),
                details = "User registered with username: ${createdUser.username}"
            )

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse(
                    success = true,
                    message = "Registration successful"
                )
            )
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse(
                    success = false,
                    message = "Registration failed",
                    errors = listOf(e.message ?: "Registration failed")
                )
            )
        } catch (e: Exception) {
            logger.error("Registration failed: ${e.message}")

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Registration failed",
                    errors = listOf("An unexpected error occurred during registration")
                )
            )
        }
    }

    @PostMapping("/refresh")
    fun refreshToken(@Valid @RequestBody tokenRefreshRequest: TokenRefreshRequest): ResponseEntity<ApiResponse<TokenRefreshResponse>> {
        try {
            val tokenRefreshResponse = authService.refreshToken(tokenRefreshRequest)

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Token refreshed successfully",
                    data = tokenRefreshResponse
                )
            )
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse(
                    success = false,
                    message = "Token refresh failed",
                    errors = listOf(e.message ?: "Invalid refresh token")
                )
            )
        } catch (e: Exception) {
            logger.error("Token refresh failed: ${e.message}")

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Token refresh failed",
                    errors = listOf("An unexpected error occurred during token refresh")
                )
            )
        }
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    fun changePassword(
        @Valid @RequestBody passwordChangeRequest: PasswordChangeRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Void>> {
        // Validate passwords match
        if (passwordChangeRequest.newPassword != passwordChangeRequest.confirmNewPassword) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse(
                    success = false,
                    message = "Passwords do not match",
                    errors = listOf("New password and confirmation password do not match")
                )
            )
        }

        try {
            val userId = UUID.fromString(userDetails.username)
            val success = authService.changePassword(
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
                action = "PASSWORD_CHANGE",
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
        } catch (e: Exception) {
            logger.error("Password change failed: ${e.message}")

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Password change failed",
                    errors = listOf("An unexpected error occurred during password change")
                )
            )
        }
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    fun logout(@AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<ApiResponse<Void>> {
        try {
            val userId = UUID.fromString(userDetails.username)
            authService.logout(userId)

            auditLogService.logEvent(
                userId = userId,
                action = "LOGOUT",
                entityType = "USER",
                entityId = userId.toString(),
                details = "User logged out"
            )

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Logged out successfully"
                )
            )
        } catch (e: Exception) {
            logger.error("Logout failed: ${e.message}")

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Logout failed",
                    errors = listOf("An unexpected error occurred during logout")
                )
            )
        }
    }

    @GetMapping("/validate-token")
    fun validateToken(@RequestHeader("Authorization") authHeader: String): ResponseEntity<ApiResponse<Map<String, Boolean>>> {
        try {
            val token = authHeader.substring(7) // Remove "Bearer " prefix
            val isValid = authService.validateToken(token)

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = if (isValid) "Token is valid" else "Token is invalid",
                    data = mapOf("valid" to isValid)
                )
            )
        } catch (e: Exception) {
            logger.error("Token validation failed: ${e.message}")

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Token is invalid",
                    data = mapOf("valid" to false)
                )
            )
        }
    }
}