package com.fraudit.fraudit.controller

import com.fraudit.fraudit.dto.auth.*
import com.fraudit.fraudit.dto.common.ApiResponse
import com.fraudit.fraudit.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/login")
    fun login(@Valid @RequestBody loginRequest: LoginRequest): ResponseEntity<ApiResponse<LoginResponse>> {
        val loginResponse = authService.authenticate(loginRequest)

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Login successful",
                data = loginResponse
            )
        )
    }

    @PostMapping("/register")
    fun register(@Valid @RequestBody registerRequest: RegisterRequest): ResponseEntity<ApiResponse<Void>> {
        authService.register(registerRequest)

        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse(
                success = true,
                message = "Registration successful"
            )
        )
    }

    @PostMapping("/refresh")
    fun refreshToken(@Valid @RequestBody tokenRefreshRequest: TokenRefreshRequest): ResponseEntity<ApiResponse<TokenRefreshResponse>> {
        val tokenRefreshResponse = authService.refreshToken(tokenRefreshRequest)

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Token refreshed successfully",
                data = tokenRefreshResponse
            )
        )
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    fun changePassword(
        @Valid @RequestBody passwordChangeRequest: PasswordChangeRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Void>> {
        val userId = UUID.fromString(userDetails.username)

        val success = authService.changePassword(
            userId,
            passwordChangeRequest.currentPassword,
            passwordChangeRequest.newPassword
        )

        return if (success) {
            ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Password changed successfully"
                )
            )
        } else {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse(
                    success = false,
                    message = "Current password is incorrect",
                    errors = listOf("Current password is incorrect")
                )
            )
        }
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    fun logout(@AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<ApiResponse<Void>> {
        val userId = UUID.fromString(userDetails.username)
        authService.logout(userId)

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Logged out successfully"
            )
        )
    }
}