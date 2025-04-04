package com.fraudit.fraudit.security

import com.fraudit.fraudit.repository.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class CustomUserDetailsService(private val userRepository: UserRepository) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        // We're using UUID as the username in JWT, so we need to check if it's a UUID
        return if (isValidUUID(username)) {
            // Load user by ID
            val userId = UUID.fromString(username)
            val user = userRepository.findById(userId)
                .orElseThrow { UsernameNotFoundException("User not found with id: $username") }

            // Create UserDetails object
            User.builder()
                .username(user.id.toString())
                .password(user.password)
                .disabled(!user.active)
                .accountExpired(false)
                .credentialsExpired(false)
                .accountLocked(false)
                .authorities(SimpleGrantedAuthority("ROLE_${user.role.name}"))
                .build()
        } else {
            // Load user by username or email for login
            val user = userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow { UsernameNotFoundException("User not found with username or email: $username") }

            // Create UserDetails object
            User.builder()
                .username(user.id.toString()) // We use UUID as the username for subsequent requests
                .password(user.password)
                .disabled(!user.active)
                .accountExpired(false)
                .credentialsExpired(false)
                .accountLocked(false)
                .authorities(SimpleGrantedAuthority("ROLE_${user.role.name}"))
                .build()
        }
    }

    private fun isValidUUID(str: String): Boolean {
        return try {
            UUID.fromString(str)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}