package com.fraudit.fraudit.service.impl

import com.fraudit.fraudit.domain.entity.User
import com.fraudit.fraudit.domain.enum.UserRole
import com.fraudit.fraudit.repository.UserRepository
import com.fraudit.fraudit.service.AuditLogService
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class DefaultAdminService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val auditLogService: AuditLogService
) {

    companion object {
        private const val DEFAULT_ADMIN_USERNAME = "admin"
        private const val DEFAULT_ADMIN_EMAIL = "admin@fraudit.com"
        private const val DEFAULT_ADMIN_PASSWORD = "fraudit_admin_2023!"
        private const val DEFAULT_ADMIN_FIRST_NAME = "System"
        private const val DEFAULT_ADMIN_LAST_NAME = "Administrator"
    }

    @EventListener(ApplicationReadyEvent::class)
    @Transactional
    fun createDefaultAdminIfNeeded() {
        // Check if any users exist in the database
        if (userRepository.count() == 0L) {
            // Create a default admin user
            val encodedPassword = passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD)
            val adminUser = User(
                id = UUID.randomUUID(),
                username = DEFAULT_ADMIN_USERNAME,
                email = DEFAULT_ADMIN_EMAIL,
                password = encodedPassword,
                firstName = DEFAULT_ADMIN_FIRST_NAME,
                lastName = DEFAULT_ADMIN_LAST_NAME,
                role = UserRole.ADMIN,
                active = true
            )

            // Save the admin user
            val savedUser = userRepository.save(adminUser)

            // Log the creation of the default admin user
            auditLogService.logEvent(
                userId = savedUser.id,
                action = "SYSTEM_INIT",
                entityType = "USER",
                entityId = savedUser.id.toString(),
                details = "Created default administrator account during system initialization"
            )

            // Log to console as well
            println("===================================================")
            println("Created default admin user:")
            println("Username: $DEFAULT_ADMIN_USERNAME")
            println("Password: $DEFAULT_ADMIN_PASSWORD")
            println("Please change this password after first login!")
            println("===================================================")
        }
    }
}