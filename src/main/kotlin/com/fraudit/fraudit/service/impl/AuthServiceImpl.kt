package com.fraudit.fraudit.service.impl

import com.fraudit.fraudit.domain.entity.User
import com.fraudit.fraudit.dto.auth.*
import com.fraudit.fraudit.repository.UserRepository
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
    private val passwordEncoder: PasswordEncoder
) : AuthService {

    @Transactional
    override fun authenticate(loginRequest: LoginRequest): LoginResponse {
        // Try to authenticate with username/email and password
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(loginRequest.username, loginRequest.password)
        )

        // Set authentication in security context
        SecurityContextHolder.getContext().authentication = authentication

        // Find user by username or email
        val user = userRepository.findByUsernameOrEmail(loginRequest.username, loginRequest.username)
            .orElseThrow { BadCredentialsException("Invalid username or password") }

        // Check if user is active
        if (!user.active) {
            throw BadCredentialsException("User account is deactivated")
        }

        // Generate JWT access token
        val accessToken = jwtTokenService.generateAccessToken(user)

        // Generate refresh token if requested (remember me)
        val refreshToken = if (loginRequest.rememberMe) {
            refreshTokenService.createRefreshToken(user.id)
        } else null

        // Get token expiration time
        val expiryDate = jwtTokenService.getExpirationFromToken(accessToken)
        val expiresAt = expiryDate?.time ?: System.currentTimeMillis() + 86400000 // Default to 24 hours

        // Create login response
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

        // Save and return the new user
        return userRepository.save(user)
    }

    @Transactional
    override fun refreshToken(tokenRefreshRequest: TokenRefreshRequest): TokenRefreshResponse {
        // Verify refresh token and get user ID
        val userId = refreshTokenService.verifyRefreshToken(tokenRefreshRequest.refreshToken)
            ?: throw IllegalArgumentException("Invalid refresh token")

        // Find user
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        // Check if user is active
        if (!user.active) {
            throw IllegalArgumentException("User account is deactivated")
        }

        // Generate new JWT access token
        val accessToken = jwtTokenService.generateAccessToken(user)

        // Generate new refresh token
        val newRefreshToken = refreshTokenService.createRefreshToken(user.id)

        // Delete old refresh token
        refreshTokenService.deleteRefreshToken(tokenRefreshRequest.refreshToken)

        // Get token expiration time
        val expiryDate = jwtTokenService.getExpirationFromToken(accessToken)
        val expiresAt = expiryDate?.time ?: System.currentTimeMillis() + 86400000 // Default to 24 hours

        // Create token refresh response
        return TokenRefreshResponse(
            token = accessToken,
            refreshToken = newRefreshToken,
            expiresAt = expiresAt
        )
    }

    @Transactional
    override fun logout(userId: UUID) {
        // Delete all refresh tokens for user (effectively logging them out)
        refreshTokenService.deleteAllUserRefreshTokens(userId)
    }

    @Transactional
    override fun changePassword(userId: UUID, currentPassword: String, newPassword: String): Boolean {
        // Find user
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.password)) {
            return false
        }

        // Update password
        val updatedUser = user.copy(
            password = passwordEncoder.encode(newPassword)
        )

        // Save user
        userRepository.save(updatedUser)

        // Invalidate all sessions (force re-login)
        refreshTokenService.deleteAllUserRefreshTokens(userId)

        return true
    }

    override fun validateToken(token: String): Boolean {
        return jwtTokenService.validateToken(token)
    }
}