package com.fraudit.fraudit.controller

import com.fraudit.fraudit.dto.auth.*
import com.fraudit.fraudit.dto.common.ApiResponse
import com.fraudit.fraudit.service.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/auth")
class AuthController(private val userService: UserService) {

    @PostMapping("/login")
    fun login(@Valid @RequestBody loginRequest: LoginRequest): ResponseEntity<ApiResponse<LoginResponse>> {
        // Handle login logic
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Login successful",
                data = LoginResponse(
                    userId = UUID.randomUUID(), // Replace with actual user ID
                    username = loginRequest.username,
                    email = "user@example.com", // Replace with actual email
                    firstName = "John", // Replace with actual first name
                    lastName = "Doe", // Replace with actual last name
                    role = "ANALYST", // Replace with actual role
                    token = "jwt-token", // Replace with actual JWT token
                    expiresAt = System.currentTimeMillis() + 3600000 // 1 hour from now
                )
            )
        )
    }

    @PostMapping("/register")
    fun register(@Valid @RequestBody registerRequest: RegisterRequest): ResponseEntity<ApiResponse<Void>> {
        // Handle registration logic
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse(
                success = true,
                message = "Registration successful"
            )
        )
    }

    @PostMapping("/refresh")
    fun refreshToken(@Valid @RequestBody tokenRefreshRequest: TokenRefreshRequest): ResponseEntity<ApiResponse<TokenRefreshResponse>> {
        // Handle token refresh logic
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Token refreshed successfully",
                data = TokenRefreshResponse(
                    token = "new-jwt-token", // Replace with actual JWT token
                    refreshToken = "new-refresh-token", // Replace with actual refresh token
                    expiresAt = System.currentTimeMillis() + 3600000 // 1 hour from now
                )
            )
        )
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    fun changePassword(@Valid @RequestBody passwordChangeRequest: PasswordChangeRequest): ResponseEntity<ApiResponse<Void>> {
        // Handle password change logic
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Password changed successfully"
            )
        )
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    fun logout(): ResponseEntity<ApiResponse<Void>> {
        // Handle logout logic
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Logged out successfully"
            )
        )
    }
}