package com.fraudit.fraudit.service.impl

import com.fraudit.fraudit.domain.entity.User
import com.fraudit.fraudit.domain.enum.UserRole
import com.fraudit.fraudit.repository.FinancialStatementRepository
import com.fraudit.fraudit.repository.UserRepository
import com.fraudit.fraudit.service.AuditLogService
import com.fraudit.fraudit.service.RefreshTokenService
import com.fraudit.fraudit.service.UserService
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID
import org.springframework.dao.DataIntegrityViolationException

@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val auditLogService: AuditLogService,
    private val refreshTokenService: RefreshTokenService,
    private val financialStatementRepository: FinancialStatementRepository
) : UserService {

    override fun findAll(): List<User> = userRepository.findAll()

    override fun findAll(pageable: Pageable): Page<User> = userRepository.findAll(pageable)

    override fun findById(id: UUID): User = userRepository.findById(id)
        .orElseThrow { EntityNotFoundException("User not found with id: $id") }

    override fun findByUsername(username: String): User = userRepository.findByUsername(username)
        .orElseThrow { EntityNotFoundException("User not found with username: $username") }

    override fun findByEmail(email: String): User = userRepository.findByEmail(email)
        .orElseThrow { EntityNotFoundException("User not found with email: $email") }

    override fun findByRole(role: UserRole, pageable: Pageable): Page<User> =
        userRepository.findByRole(role, pageable)

    override fun findByActive(active: Boolean, pageable: Pageable): Page<User> =
        userRepository.findByActive(active, pageable)

    override fun findByRoleAndActive(role: UserRole, active: Boolean, pageable: Pageable): Page<User> =
        userRepository.findByRoleAndActive(role, active, pageable)

    override fun searchUsers(query: String, pageable: Pageable): Page<User> {
        val searchTerm = "%${query.lowercase()}%"
        return userRepository.searchUsers(searchTerm, pageable)
    }

    @Transactional
    override fun createUser(user: User): User {
        // Validate username and email are not already taken
        if (!isUsernameAvailable(user.username)) {
            throw IllegalArgumentException("Username ${user.username} is already taken")
        }
        if (!isEmailAvailable(user.email)) {
            throw IllegalArgumentException("Email ${user.email} is already in use")
        }

        // Encode password
        val encodedPassword = passwordEncoder.encode(user.password)
        val userWithEncodedPassword = user.copy(password = encodedPassword)

        return userRepository.save(userWithEncodedPassword)
    }

    @Transactional
    override fun updateUser(user: User): User {
        val existingUser = findById(user.id)

        // Check if username is being changed and if it's available
        if (existingUser.username != user.username && !isUsernameAvailable(user.username)) {
            throw IllegalArgumentException("Username ${user.username} is already taken")
        }

        // Check if email is being changed and if it's available
        if (existingUser.email != user.email && !isEmailAvailable(user.email)) {
            throw IllegalArgumentException("Email ${user.email} is already in use")
        }

        // Preserve the original password
        val updatedUser = user.copy(
            password = existingUser.password,
            createdAt = existingUser.createdAt,
            updatedAt = OffsetDateTime.now()
        )

        return userRepository.save(updatedUser)
    }

    @Transactional
    override fun deleteUser(id: UUID) {
        val user = findById(id)

        // Check if user has associated financial statements
        val statementCount = financialStatementRepository.countByUserId(id)
        if (statementCount > 0) {
            throw IllegalStateException("Cannot delete user because they have $statementCount associated financial statements")
        }

        try {
            // Delete refresh tokens first
            invalidateUserSessions(id)

            // Delete user
            userRepository.deleteById(id)
        } catch (e: DataIntegrityViolationException) {
            throw IllegalStateException("Cannot delete user because they have associated records", e)
        }
    }

    @Transactional
    override fun changePassword(id: UUID, currentPassword: String, newPassword: String): Boolean {
        val user = findById(id)

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.password)) {
            return false
        }

        // Encode and update password
        val encodedPassword = passwordEncoder.encode(newPassword)
        val updatedUser = user.copy(
            password = encodedPassword,
            updatedAt = OffsetDateTime.now()
        )
        userRepository.save(updatedUser)

        // Invalidate all sessions (force re-login)
        invalidateUserSessions(id)

        return true
    }

    @Transactional
    override fun resetPassword(id: UUID, newPassword: String): User {
        val user = findById(id)

        // Encode and update password
        val encodedPassword = passwordEncoder.encode(newPassword)
        val updatedUser = user.copy(
            password = encodedPassword,
            updatedAt = OffsetDateTime.now()
        )

        val savedUser = userRepository.save(updatedUser)

        // Invalidate all sessions (force re-login)
        invalidateUserSessions(id)

        return savedUser
    }

    override fun isUsernameAvailable(username: String): Boolean = !userRepository.existsByUsername(username)

    override fun isEmailAvailable(email: String): Boolean = !userRepository.existsByEmail(email)

    override fun findByUsernameOrEmail(usernameOrEmail: String): User = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
        .orElseThrow { EntityNotFoundException("User not found with username or email: $usernameOrEmail") }

    @Transactional
    override fun invalidateUserSessions(userId: UUID) {
        // Delete all refresh tokens for the user
        refreshTokenService.deleteAllUserRefreshTokens(userId)
    }

    override fun getUserActivityStats(userId: UUID, page: Int, size: Int): Map<String, Any> {
        // Check if user exists
        val user = findById(userId)

        // Get audit logs for the user
        val pageable = org.springframework.data.domain.PageRequest.of(
            page,
            size,
            org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "timestamp")
        )

        val auditLogs = auditLogService.findByUserId(userId, pageable)

        // Count actions by type
        val actionCounts = auditLogService.findByUserId(userId)
            .groupBy { it.action }
            .mapValues { it.value.size }

        // Count activities by day for the last 30 days
        val now = java.time.OffsetDateTime.now()
        val thirtyDaysAgo = now.minusDays(30)

        val dailyActivity = auditLogService.findByUserIdAndDateRange(userId, thirtyDaysAgo, now)
            .groupBy { it.timestamp.toLocalDate().toString() }
            .mapValues { it.value.size }

        // Map audit logs to DTOs
        val logEntries = auditLogs.content.map { log ->
            mapOf(
                "id" to log.id,
                "action" to log.action,
                "entityType" to log.entityType,
                "entityId" to log.entityId,
                "details" to log.details,
                "timestamp" to log.timestamp
            )
        }

        // Create pagination info
        val pagination = mapOf(
            "page" to auditLogs.number,
            "size" to auditLogs.size,
            "totalElements" to auditLogs.totalElements,
            "totalPages" to auditLogs.totalPages,
            "first" to auditLogs.isFirst,
            "last" to auditLogs.isLast
        )

        // Return activity statistics and logs
        return mapOf(
            "user" to mapOf(
                "id" to user.id,
                "username" to user.username,
                "email" to user.email
            ),
            "totalActions" to auditLogs.totalElements,
            "actionCounts" to actionCounts,
            "dailyActivity" to dailyActivity,
            "logs" to logEntries,
            "pagination" to pagination
        )
    }

    override fun getUserStats(): Map<String, Any> {
        val allUsers = userRepository.findAll()

        // Total users
        val totalUsers = allUsers.size

        // Users by role
        val usersByRole = allUsers.groupBy { it.role }
            .mapValues { it.value.size }
            .mapKeys { it.key.name }

        // Active vs. inactive users
        val activeUsers = allUsers.count { it.active }
        val inactiveUsers = totalUsers - activeUsers

        // Recently created users (last 30 days)
        val now = OffsetDateTime.now()
        val thirtyDaysAgo = now.minusDays(30)
        val recentUsers = allUsers.count {
            it.createdAt != null && it.createdAt!!.isAfter(thirtyDaysAgo)
        }

        // Get most active users based on audit logs
        val userActivityCounts = auditLogService.findAll().groupBy { it.user?.id }
            .filter { it.key != null }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .map { (userId, count) ->
                val user = userId?.let { findById(it) }
                mapOf(
                    "userId" to userId,
                    "username" to (user?.username ?: "Unknown"),
                    "activityCount" to count
                )
            }

        return mapOf(
            "totalUsers" to totalUsers,
            "activeUsers" to activeUsers,
            "inactiveUsers" to inactiveUsers,
            "recentUsers" to recentUsers,
            "usersByRole" to usersByRole,
            "mostActiveUsers" to userActivityCounts
        )
    }
}