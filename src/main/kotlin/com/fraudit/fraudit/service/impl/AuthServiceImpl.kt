package com.fraudit.fraudit.service.impl

import com.fraudit.fraudit.domain.entity.User
import com.fraudit.fraudit.domain.enum.UserRole
import com.fraudit.fraudit.dto.auth.LoginRequest
import com.fraudit.fraudit.dto.auth.LoginResponse
import com.fraudit.fraudit.dto.auth.RegisterRequest
import com.fraudit.fraudit.dto.auth.TokenRefreshRequest
import com.fraudit.fraudit.dto.auth.TokenRefreshResponse
import com.fraudit.fraudit.repository.UserRepository
import com.fraudit.fraudit.service.AuditLogService
import com.fraudit.fraudit.service.AuthService
import com.fraudit.fraudit.service.JwtTokenService
import com.fraudit.fraudit.service.RefreshTokenService
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID


@Service
class AuthServiceImpl(
    private val authenticationManager: AuthenticationManager,
    private val userRepository: UserRepository,
    private val jwtTokenService: JwtTokenService,
    private val refreshTokenService: RefreshTokenService,
    private val passwordEncoder: PasswordEncoder,
    private val auditLogService: AuditLogService
) : AuthService {

    @Transactional
    override fun authenticate(loginRequest: LoginRequest): LoginResponse {
        // Authenticate user
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(loginRequest.username, loginRequest.password)
        )

        // Set authentication in context
        SecurityContextHolder.getContext().authentication = authentication

        // Find user
        val user = userRepository.findByUsernameOrEmail(loginRequest.username, loginRequest.username)
            .orElseThrow { BadCredentialsException("Invalid username or password") }

        // Generate JWT token
        val accessToken = jwtTokenService.generateAccessToken(user)

        // Generate refresh token if requested
        val refreshToken = if (loginRequest.rememberMe) {
            refreshTokenService.createRefreshToken(user.id)
        } else null

        // Log login event
        auditLogService.logEvent(
            userId = user.id,
            action = "LOGIN",
            entityType = "USER",
            entityId = user.id.toString(),
            details = "User logged in"
        )

        // Expiry timestamp (from JWT token)
        val expiration = jwtTokenService.getExpirationFromToken(accessToken)
        val expiresAt = expiration?.time ?: (System.currentTimeMillis() + 86400000) // 24 hours

        // Return login response
        return LoginResponse(
            userId = user.id,
            username = user.username,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            role = user.role.name,
            token = accessToken,
            expiresAt = expiresAt
        )
    }

    @Transactional
    override fun register(registerRequest: RegisterRequest): User {
        // Check if username is already taken
        if (userRepository.existsByUsername(registerRequest.username)) {
            throw IllegalArgumentException("Username ${registerRequest.username} is already taken")
        }

        // Check if email is already in use
        if (userRepository.existsByEmail(registerRequest.email)) {
            throw IllegalArgumentException("Email ${registerRequest.email} is already in use")
        }

        // Create new user
        val user = User(
            id = UUID.randomUUID(),
            username = registerRequest.username,
            email = registerRequest.email,
            password = passwordEncoder.encode(registerRequest.password),
            firstName = registerRequest.firstName,
            lastName = registerRequest.lastName,
            role = registerRequest.role,
            active = true
        )

        // Save user
        val savedUser = userRepository.save(user)

        // Log registration event
        auditLogService.logEvent(
            userId = savedUser.id,
            action = "REGISTER",
            entityType = "USER",
            entityId = savedUser.id.toString(),
            details = "User registered"
        )

        return savedUser
    }

    @Transactional
    override fun refreshToken(tokenRefreshRequest: TokenRefreshRequest): TokenRefreshResponse {
        // Verify refresh token
        val userId = refreshTokenService.verifyRefreshToken(tokenRefreshRequest.refreshToken)
            ?: throw IllegalArgumentException("Invalid refresh token")

        // Find user
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        // Generate new tokens
        val accessToken = jwtTokenService.generateAccessToken(user)
        val refreshToken = refreshTokenService.createRefreshToken(user.id)

        // Delete old refresh token
        refreshTokenService.deleteRefreshToken(tokenRefreshRequest.refreshToken)

        // Expiry timestamp (from JWT token)
        val expiration = jwtTokenService.getExpirationFromToken(accessToken)
        val expiresAt = expiration?.time ?: (System.currentTimeMillis() + 86400000) // 24 hours

        return TokenRefreshResponse(
            token = accessToken,
            refreshToken = refreshToken,
            expiresAt = expiresAt
        )
    }

    @Transactional
    override fun logout(userId: UUID) {
        // Delete all refresh tokens for user
        refreshTokenService.deleteAllUserRefreshTokens(userId)

        // Log logout event
        auditLogService.logEvent(
            userId = userId,
            action = "LOGOUT",
            entityType = "USER",
            entityId = userId.toString(),
            details = "User logged out"
        )
    }

    @Transactional
    override fun changePassword(userId: UUID, currentPassword: String, newPassword: String): Boolean {
        // Find user
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        // Check current password
        if (!passwordEncoder.matches(currentPassword, user.password)) {
            return false
        }

        // Update password
        val updatedUser = user.copy(
            password = passwordEncoder.encode(newPassword)
        )

        // Save user
        userRepository.save(updatedUser)

        // Delete all refresh tokens for user (force re-login)
        refreshTokenService.deleteAllUserRefreshTokens(userId)

        // Log password change event
        auditLogService.logEvent(
            userId = userId,
            action = "PASSWORD_CHANGE",
            entityType = "USER",
            entityId = userId.toString(),
            details = "User changed password"
        )

        return true
    }
}