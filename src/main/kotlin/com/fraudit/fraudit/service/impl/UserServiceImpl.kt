package com.fraudit.fraudit.service.impl

import com.fraudit.fraudit.domain.entity.User
import com.fraudit.fraudit.repository.UserRepository
import com.fraudit.fraudit.service.AuditLogService
import com.fraudit.fraudit.service.UserService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import jakarta.persistence.EntityNotFoundException

@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val auditLogService: AuditLogService
) : UserService {

    override fun findAll(): List<User> = userRepository.findAll()

    override fun findById(id: UUID): User = userRepository.findById(id)
        .orElseThrow { EntityNotFoundException("User not found with id: $id") }

    override fun findByUsername(username: String): User = userRepository.findByUsername(username)
        .orElseThrow { EntityNotFoundException("User not found with username: $username") }

    override fun findByEmail(email: String): User = userRepository.findByEmail(email)
        .orElseThrow { EntityNotFoundException("User not found with email: $email") }

    @Transactional
    override fun createUser(user: User): User {
        if (!isUsernameAvailable(user.username)) {
            throw IllegalArgumentException("Username ${user.username} is already taken")
        }
        if (!isEmailAvailable(user.email)) {
            throw IllegalArgumentException("Email ${user.email} is already in use")
        }

        val encodedPassword = passwordEncoder.encode(user.password)
        val userWithEncodedPassword = user.copy(password = encodedPassword)
        val savedUser = userRepository.save(userWithEncodedPassword)

        auditLogService.logEvent(
            userId = savedUser.id,
            action = "CREATE",
            entityType = "USER",
            entityId = savedUser.id.toString(),
            details = "Created user: ${savedUser.username}"
        )

        return savedUser
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

        // Preserve the original password if not changed
        val updatedUser = if (user.password.startsWith("{bcrypt}") || user.password == existingUser.password) {
            user
        } else {
            user.copy(password = passwordEncoder.encode(user.password))
        }

        val savedUser = userRepository.save(updatedUser)

        auditLogService.logEvent(
            userId = savedUser.id,
            action = "UPDATE",
            entityType = "USER",
            entityId = savedUser.id.toString(),
            details = "Updated user: ${savedUser.username}"
        )

        return savedUser
    }

    @Transactional
    override fun deleteUser(id: UUID) {
        val user = findById(id)
        userRepository.delete(user)

        auditLogService.logEvent(
            userId = null,
            action = "DELETE",
            entityType = "USER",
            entityId = id.toString(),
            details = "Deleted user: ${user.username}"
        )
    }

    @Transactional
    override fun changePassword(id: UUID, newPassword: String): User {
        val user = findById(id)
        val encodedPassword = passwordEncoder.encode(newPassword)
        val updatedUser = user.copy(password = encodedPassword)
        val savedUser = userRepository.save(updatedUser)

        auditLogService.logEvent(
            userId = savedUser.id,
            action = "PASSWORD_CHANGE",
            entityType = "USER",
            entityId = savedUser.id.toString(),
            details = "Changed password for user: ${savedUser.username}"
        )

        return savedUser
    }

    override fun isUsernameAvailable(username: String): Boolean = !userRepository.existsByUsername(username)

    override fun isEmailAvailable(email: String): Boolean = !userRepository.existsByEmail(email)

    override fun findByUsernameOrEmail(usernameOrEmail: String): User = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
        .orElseThrow { EntityNotFoundException("User not found with username or email: $usernameOrEmail") }
}
