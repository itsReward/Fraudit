package com.fraudit.fraudit.service

import com.fraudit.fraudit.domain.entity.User
import com.fraudit.fraudit.domain.enum.UserRole
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface UserService {
    /**
     * Find all users
     */
    fun findAll(): List<User>

    /**
     * Find all users with pagination
     */
    fun findAll(pageable: Pageable): Page<User>

    /**
     * Find user by ID
     */
    fun findById(id: UUID): User

    /**
     * Find user by username
     */
    fun findByUsername(username: String): User

    /**
     * Find user by email
     */
    fun findByEmail(email: String): User

    /**
     * Find users by role
     */
    fun findByRole(role: UserRole, pageable: Pageable): Page<User>

    /**
     * Find users by active status
     */
    fun findByActive(active: Boolean, pageable: Pageable): Page<User>

    /**
     * Find users by role and active status
     */
    fun findByRoleAndActive(role: UserRole, active: Boolean, pageable: Pageable): Page<User>

    /**
     * Search users by username, email, or name
     */
    fun searchUsers(query: String, pageable: Pageable): Page<User>

    /**
     * Create a new user
     */
    fun createUser(user: User): User

    /**
     * Update an existing user
     */
    fun updateUser(user: User): User

    /**
     * Delete a user
     */
    fun deleteUser(id: UUID)

    /**
     * Change a user's password
     * @return true if password was changed successfully, false otherwise
     */
    fun changePassword(id: UUID, currentPassword: String, newPassword: String): Boolean

    /**
     * Admin reset of user password
     */
    fun resetPassword(id: UUID, newPassword: String): User

    /**
     * Check if a username is available
     */
    fun isUsernameAvailable(username: String): Boolean

    /**
     * Check if an email is available
     */
    fun isEmailAvailable(email: String): Boolean

    /**
     * Find a user by username or email
     */
    fun findByUsernameOrEmail(usernameOrEmail: String): User

    /**
     * Invalidate all sessions for a user
     */
    fun invalidateUserSessions(userId: UUID)

    /**
     * Get user activity statistics and logs
     * @param userId The user ID
     * @param page Page number for pagination
     * @param size Items per page
     * @return Map containing activity statistics and logs
     */
    fun getUserActivityStats(userId: UUID, page: Int, size: Int): Map<String, Any>

    /**
     * Get overall user statistics
     * @return Map containing user statistics
     */
    fun getUserStats(): Map<String, Any>
}