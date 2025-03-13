package com.fraudit.fraudit.service

import com.fraudit.fraudit.domain.entity.User
import java.util.UUID

interface UserService {
    fun findAll(): List<User>
    fun findById(id: UUID): User
    fun findByUsername(username: String): User
    fun findByEmail(email: String): User
    fun createUser(user: User): User
    fun updateUser(user: User): User
    fun deleteUser(id: UUID)
    fun changePassword(id: UUID, newPassword: String): User
    fun isUsernameAvailable(username: String): Boolean
    fun isEmailAvailable(email: String): Boolean
    fun findByUsernameOrEmail(usernameOrEmail: String): User
}