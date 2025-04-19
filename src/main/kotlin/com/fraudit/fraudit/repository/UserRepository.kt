package com.fraudit.fraudit.repository

import com.fraudit.fraudit.domain.entity.User
import com.fraudit.fraudit.domain.enum.UserRole
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface UserRepository : JpaRepository<User, UUID> {
    /**
     * Find user by username
     */
    fun findByUsername(username: String): Optional<User>

    /**
     * Find user by email
     */
    fun findByEmail(email: String): Optional<User>

    /**
     * Check if username exists
     */
    fun existsByUsername(username: String): Boolean

    /**
     * Check if email exists
     */
    fun existsByEmail(email: String): Boolean

    /**
     * Find user by username or email
     */
    fun findByUsernameOrEmail(username: String, email: String): Optional<User>

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
    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.username) LIKE :query OR " +
            "LOWER(u.email) LIKE :query OR " +
            "LOWER(CONCAT(COALESCE(u.firstName, ''), ' ', COALESCE(u.lastName, ''))) LIKE :query")
    fun searchUsers(@Param("query") query: String, pageable: Pageable): Page<User>

    /**
     * Count users by role
     */
    @Query("SELECT u.role, COUNT(u) FROM User u GROUP BY u.role")
    fun countByRole(): List<Array<Any>>
}