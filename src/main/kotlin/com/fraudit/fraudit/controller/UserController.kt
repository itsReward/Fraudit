package com.fraudit.fraudit.controller

import com.fraudit.fraudit.domain.entity.User
import com.fraudit.fraudit.domain.enum.UserRole
import com.fraudit.fraudit.dto.common.ApiResponse
import com.fraudit.fraudit.dto.common.PagedResponse
import com.fraudit.fraudit.dto.user.*
import com.fraudit.fraudit.service.AuditLogService
import com.fraudit.fraudit.service.UserService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService,
    private val auditLogService: AuditLogService
) {
    private val logger = LoggerFactory.getLogger(UserController::class.java)

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'REGULATOR')")
    fun getAllUsers(
        @RequestParam(required = false) role: UserRole?,
        @RequestParam(required = false) active: Boolean?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "username") sortBy: String,
        @RequestParam(defaultValue = "ASC") sortDirection: String
    ): ResponseEntity<ApiResponse<PagedResponse<UserSummaryResponse>>> {
        try {
            val direction = if (sortDirection.equals("ASC", ignoreCase = true))
                Sort.Direction.ASC else Sort.Direction.DESC

            val pageable = PageRequest.of(page, size, Sort.by(direction, sortBy))

            // Apply filters if provided
            val usersPage = when {
                role != null && active != null -> userService.findByRoleAndActive(role, active, pageable)
                role != null -> userService.findByRole(role, pageable)
                active != null -> userService.findByActive(active, pageable)
                else -> userService.findAll(pageable)
            }

            val pagedResponse = createPagedResponse(usersPage) { user ->
                mapToUserSummaryResponse(user)
            }

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Users retrieved successfully",
                    data = pagedResponse
                )
            )
        } catch (e: Exception) {
            logger.error("Error retrieving users: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error retrieving users",
                    errors = listOf(e.message ?: "Unknown error")
                )
            )
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGULATOR') or #id == authentication.principal.username")
    fun getUserById(@PathVariable id: UUID): ResponseEntity<ApiResponse<UserResponse>> {
        try {
            val user = userService.findById(id)

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "User retrieved successfully",
                    data = mapToUserResponse(user)
                )
            )
        } catch (e: Exception) {
            logger.error("Error retrieving user: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse(
                    success = false,
                    message = "User not found",
                    errors = listOf(e.message ?: "User not found")
                )
            )
        }
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    fun getCurrentUser(@AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<ApiResponse<UserResponse>> {
        try {
            val userId = UUID.fromString(userDetails.username)
            val user = userService.findById(userId)

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Current user retrieved successfully",
                    data = mapToUserResponse(user)
                )
            )
        } catch (e: Exception) {
            logger.error("Error retrieving current user: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error retrieving current user",
                    errors = listOf(e.message ?: "Unknown error")
                )
            )
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun createUser(
        @Valid @RequestBody createUserRequest: CreateUserRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<UserResponse>> {
        try {
            val adminId = UUID.fromString(userDetails.username)

            // Validate username and email availability
            if (!userService.isUsernameAvailable(createUserRequest.username)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse(
                        success = false,
                        message = "Username already taken",
                        errors = listOf("Username '${createUserRequest.username}' is already taken")
                    )
                )
            }

            if (!userService.isEmailAvailable(createUserRequest.email)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse(
                        success = false,
                        message = "Email already in use",
                        errors = listOf("Email '${createUserRequest.email}' is already in use")
                    )
                )
            }

            // Create new user entity
            val newUser = User(
                id = UUID.randomUUID(),
                username = createUserRequest.username,
                email = createUserRequest.email,
                password = createUserRequest.password, // Will be encoded in the service
                firstName = createUserRequest.firstName,
                lastName = createUserRequest.lastName,
                role = createUserRequest.role,
                active = true
            )

            val createdUser = userService.createUser(newUser)

            auditLogService.logEvent(
                userId = adminId,
                action = "CREATE_USER",
                entityType = "USER",
                entityId = createdUser.id.toString(),
                details = "Admin created new user: ${createdUser.username} with role ${createdUser.role}"
            )

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse(
                    success = true,
                    message = "User created successfully",
                    data = mapToUserResponse(createdUser)
                )
            )
        } catch (e: Exception) {
            logger.error("Error creating user: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse(
                    success = false,
                    message = "Error creating user",
                    errors = listOf(e.message ?: "Unknown error")
                )
            )
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.username")
    fun updateUser(
        @PathVariable id: UUID,
        @Valid @RequestBody userUpdateRequest: UserUpdateRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<UserResponse>> {
        try {
            val currentUserId = UUID.fromString(userDetails.username)
            val existingUser = userService.findById(id)

            // Check if non-admin user is trying to change their role
            val isCurrentUser = id == currentUserId
            val isAdmin = userDetails.authorities.any { it.authority == "ROLE_ADMIN" }

            if (isCurrentUser && !isAdmin && userUpdateRequest.role != null && userUpdateRequest.role != existingUser.role) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiResponse(
                        success = false,
                        message = "You cannot change your own role",
                        errors = listOf("You cannot change your own role")
                    )
                )
            }

            // Check email availability if changing email
            if (userUpdateRequest.email != existingUser.email && !userService.isEmailAvailable(userUpdateRequest.email)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse(
                        success = false,
                        message = "Email already in use",
                        errors = listOf("Email '${userUpdateRequest.email}' is already in use")
                    )
                )
            }

            // Create updated user entity
            val updatedUser = existingUser.copy(
                firstName = userUpdateRequest.firstName ?: existingUser.firstName,
                lastName = userUpdateRequest.lastName ?: existingUser.lastName,
                email = userUpdateRequest.email,
                role = userUpdateRequest.role ?: existingUser.role
            )

            val savedUser = userService.updateUser(updatedUser)

            auditLogService.logEvent(
                userId = currentUserId,
                action = "UPDATE_USER",
                entityType = "USER",
                entityId = savedUser.id.toString(),
                details = if (isCurrentUser) "User updated own profile" else "Admin updated user: ${savedUser.username}"
            )

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "User updated successfully",
                    data = mapToUserResponse(savedUser)
                )
            )
        } catch (e: Exception) {
            logger.error("Error updating user: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse(
                    success = false,
                    message = "Error updating user",
                    errors = listOf(e.message ?: "Unknown error")
                )
            )
        }
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateUserStatus(
        @PathVariable id: UUID,
        @Valid @RequestBody statusUpdateRequest: UserStatusUpdateRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<UserResponse>> {
        try {
            val adminId = UUID.fromString(userDetails.username)

            // Prevent deactivating own account
            if (id == adminId && !statusUpdateRequest.active) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiResponse(
                        success = false,
                        message = "You cannot deactivate your own account",
                        errors = listOf("You cannot deactivate your own account")
                    )
                )
            }

            val existingUser = userService.findById(id)

            // If status is already the requested status, return success without changes
            if (existingUser.active == statusUpdateRequest.active) {
                return ResponseEntity.ok(
                    ApiResponse(
                        success = true,
                        message = if (statusUpdateRequest.active)
                            "User is already active"
                        else
                            "User is already inactive",
                        data = mapToUserResponse(existingUser)
                    )
                )
            }

            // Update user status
            val updatedUser = existingUser.copy(
                active = statusUpdateRequest.active
            )

            val savedUser = userService.updateUser(updatedUser)

            // Force user logout if deactivated
            if (!statusUpdateRequest.active) {
                userService.invalidateUserSessions(id)
            }

            auditLogService.logEvent(
                userId = adminId,
                action = if (statusUpdateRequest.active) "ACTIVATE_USER" else "DEACTIVATE_USER",
                entityType = "USER",
                entityId = savedUser.id.toString(),
                details = "Admin ${if (statusUpdateRequest.active) "activated" else "deactivated"} user: ${savedUser.username}"
            )

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = if (statusUpdateRequest.active) "User activated successfully" else "User deactivated successfully",
                    data = mapToUserResponse(savedUser)
                )
            )
        } catch (e: Exception) {
            logger.error("Error updating user status: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse(
                    success = false,
                    message = "Error updating user status",
                    errors = listOf(e.message ?: "Unknown error")
                )
            )
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteUser(
        @PathVariable id: UUID,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Void>> {
        try {
            val adminId = UUID.fromString(userDetails.username)

            // Prevent deleting own account
            if (id == adminId) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiResponse(
                        success = false,
                        message = "You cannot delete your own account",
                        errors = listOf("You cannot delete your own account")
                    )
                )
            }

            val user = userService.findById(id)

            // Check if the default admin is being deleted
            if (user.username == "admin" && user.email == "admin@fraudit.com") {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiResponse(
                        success = false,
                        message = "Cannot delete the default admin account",
                        errors = listOf("Cannot delete the default admin account")
                    )
                )
            }

            // Try to delete the user
            try {
                userService.deleteUser(id)
            } catch (e: IllegalStateException) {
                // User has associated records
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    ApiResponse(
                        success = false,
                        message = "Cannot delete user because they have associated records",
                        errors = listOf(e.message ?: "User has associated records and cannot be deleted")
                    )
                )
            }

            auditLogService.logEvent(
                userId = adminId,
                action = "DELETE_USER",
                entityType = "USER",
                entityId = id.toString(),
                details = "Admin deleted user: ${user.username}"
            )

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "User deleted successfully"
                )
            )
        } catch (e: Exception) {
            logger.error("Error deleting user: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse(
                    success = false,
                    message = "Error deleting user",
                    errors = listOf(e.message ?: "Unknown error")
                )
            )
        }
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("hasRole('ADMIN')")
    fun resetPassword(
        @PathVariable id: UUID,
        @Valid @RequestBody resetPasswordRequest: AdminPasswordResetRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Void>> {
        try {
            val adminId = UUID.fromString(userDetails.username)

            // Get the user
            val user = userService.findById(id)

            // Reset the password
            userService.resetPassword(id, resetPasswordRequest.newPassword)

            // Force user logout
            userService.invalidateUserSessions(id)

            auditLogService.logEvent(
                userId = adminId,
                action = "RESET_PASSWORD",
                entityType = "USER",
                entityId = id.toString(),
                details = "Admin reset password for user: ${user.username}"
            )

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Password reset successfully"
                )
            )
        } catch (e: Exception) {
            logger.error("Error resetting password: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse(
                    success = false,
                    message = "Error resetting password",
                    errors = listOf(e.message ?: "Unknown error")
                )
            )
        }
    }

    @GetMapping("/roles")
    fun getAllRoles(): ResponseEntity<ApiResponse<List<String>>> {
        val roles = UserRole.values().map { it.name }

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "User roles retrieved successfully",
                data = roles
            )
        )
    }

    @GetMapping("/check-username")
    fun checkUsernameAvailability(@RequestParam username: String): ResponseEntity<ApiResponse<Map<String, Boolean>>> {
        val isAvailable = userService.isUsernameAvailable(username)

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = if (isAvailable) "Username is available" else "Username is already taken",
                data = mapOf("available" to isAvailable)
            )
        )
    }

    @GetMapping("/check-email")
    fun checkEmailAvailability(@RequestParam email: String): ResponseEntity<ApiResponse<Map<String, Boolean>>> {
        val isAvailable = userService.isEmailAvailable(email)

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = if (isAvailable) "Email is available" else "Email is already in use",
                data = mapOf("available" to isAvailable)
            )
        )
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    fun searchUsers(
        @RequestParam query: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<PagedResponse<UserSummaryResponse>>> {
        try {
            val pageable = PageRequest.of(page, size)
            val usersPage = userService.searchUsers(query, pageable)

            val pagedResponse = createPagedResponse(usersPage) { user ->
                mapToUserSummaryResponse(user)
            }

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Users retrieved successfully",
                    data = pagedResponse
                )
            )
        } catch (e: Exception) {
            logger.error("Error searching users: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error searching users",
                    errors = listOf(e.message ?: "Unknown error")
                )
            )
        }
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    fun getUserStats(): ResponseEntity<ApiResponse<Map<String, Any>>> {
        try {
            val stats = userService.getUserStats()

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "User statistics retrieved successfully",
                    data = stats
                )
            )
        } catch (e: Exception) {
            logger.error("Error retrieving user statistics: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error retrieving user statistics",
                    errors = listOf(e.message ?: "Unknown error")
                )
            )
        }
    }

    // Utility methods

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

    private fun mapToUserSummaryResponse(user: User): UserSummaryResponse {
        val fullName = listOfNotNull(user.firstName, user.lastName)
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" ")
            ?: "N/A"

        return UserSummaryResponse(
            id = user.id,
            username = user.username,
            email = user.email,
            fullName = fullName,
            role = user.role.name
        )
    }

    private fun <T, R> createPagedResponse(page: Page<T>, mapper: (T) -> R): PagedResponse<R> {
        return PagedResponse(
            content = page.content.map(mapper),
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            first = page.isFirst,
            last = page.isLast
        )
    }
}