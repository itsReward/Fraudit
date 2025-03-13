package com.fraudit.fraudit.controller

import com.fraudit.fraudit.dto.common.ApiResponse
import com.fraudit.fraudit.dto.common.PagedResponse
import com.fraudit.fraudit.dto.user.*
import com.fraudit.fraudit.service.UserService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/users")
class UserController(private val userService: UserService) {

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'REGULATOR')")
    fun getAllUsers(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<PagedResponse<UserSummaryResponse>>> {
        // Implementation for getting all users with pagination
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Users retrieved successfully",
                data = PagedResponse(
                    content = listOf(), // Replace with actual user data
                    page = page,
                    size = size,
                    totalElements = 0, // Replace with actual count
                    totalPages = 0, // Replace with actual page count
                    first = true,
                    last = true
                )
            )
        )
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGULATOR') or #id == authentication.principal.id")
    fun getUserById(@PathVariable id: UUID): ResponseEntity<ApiResponse<UserResponse>> {
        // Implementation for getting a specific user by ID
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "User retrieved successfully",
                data = null // Replace with actual user data
            )
        )
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    fun updateUser(
        @PathVariable id: UUID,
        @Valid @RequestBody userUpdateRequest: UserUpdateRequest
    ): ResponseEntity<ApiResponse<UserResponse>> {
        // Implementation for updating a user
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "User updated successfully",
                data = null // Replace with updated user data
            )
        )
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateUserStatus(
        @PathVariable id: UUID,
        @Valid @RequestBody statusUpdateRequest: UserStatusUpdateRequest
    ): ResponseEntity<ApiResponse<Void>> {
        // Implementation for activating/deactivating a user
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = if (statusUpdateRequest.active) "User activated successfully" else "User deactivated successfully"
            )
        )
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteUser(@PathVariable id: UUID): ResponseEntity<ApiResponse<Void>> {
        // Implementation for deleting a user
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "User deleted successfully"
            )
        )
    }
}