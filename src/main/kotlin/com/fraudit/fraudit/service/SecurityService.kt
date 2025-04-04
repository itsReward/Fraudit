package com.fraudit.fraudit.service

import com.fraudit.fraudit.repository.FinancialStatementRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Service for security-related operations
 */
@Service
class SecurityService(
    private val financialStatementRepository: FinancialStatementRepository
) {
    /**
     * Check if the current user is the owner of a financial statement
     * @param statementId The ID of the financial statement
     * @return true if the current user is the owner, false otherwise
     */
    fun isOwner(statementId: Long): Boolean {
        // Get the current authenticated user ID
        val authentication = SecurityContextHolder.getContext().authentication
        val currentUserId = try {
            UUID.fromString(authentication.name)
        } catch (e: Exception) {
            return false
        }

        // Get the statement
        val statement = financialStatementRepository.findById(statementId)

        // Check if the statement exists and if the current user is the owner
        return statement.isPresent && statement.get().user.id == currentUserId
    }
}