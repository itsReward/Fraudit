package com.fraudit.fraudit.service.impl

import com.fraudit.fraudit.domain.entity.FinancialStatement
import com.fraudit.fraudit.domain.entity.User
import com.fraudit.fraudit.domain.enum.StatementStatus
import com.fraudit.fraudit.domain.enum.StatementType
import com.fraudit.fraudit.repository.FinancialStatementRepository
import com.fraudit.fraudit.repository.FiscalYearRepository
import com.fraudit.fraudit.repository.UserRepository
import com.fraudit.fraudit.service.AuditLogService
import com.fraudit.fraudit.service.FinancialStatementService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID
import jakarta.persistence.EntityNotFoundException

@Service
class FinancialStatementServiceImpl(
    private val financialStatementRepository: FinancialStatementRepository,
    private val fiscalYearRepository: FiscalYearRepository,
    private val userRepository: UserRepository,
    private val auditLogService: AuditLogService
) : FinancialStatementService {

    override fun findAll(): List<FinancialStatement> = financialStatementRepository.findAll()

    override fun findAllPaged(pageable: Pageable): Page<FinancialStatement> =
        financialStatementRepository.findAll(pageable)

    override fun findById(id: Long): FinancialStatement = financialStatementRepository.findById(id)
        .orElseThrow { EntityNotFoundException("Financial statement not found with id: $id") }

    override fun findByUserId(userId: UUID): List<FinancialStatement> =
        financialStatementRepository.findByUserId(userId)

    override fun findByUserIdPaged(userId: UUID, pageable: Pageable): Page<FinancialStatement> =
        financialStatementRepository.findByUserId(userId, pageable)

    override fun findByFiscalYearId(fiscalYearId: Long): List<FinancialStatement> =
        financialStatementRepository.findByFiscalYearId(fiscalYearId)

    override fun findByFiscalYearIdPaged(fiscalYearId: Long, pageable: Pageable): Page<FinancialStatement> =
        financialStatementRepository.findByFiscalYearId(fiscalYearId, pageable)

    override fun findByCompanyId(companyId: Long): List<FinancialStatement> =
        financialStatementRepository.findByFiscalYearCompanyId(companyId)

    override fun findByCompanyIdPaged(companyId: Long, pageable: Pageable): Page<FinancialStatement> =
        financialStatementRepository.findByFiscalYearCompanyId(companyId, pageable)

    override fun findByStatementType(statementType: StatementType): List<FinancialStatement> =
        financialStatementRepository.findByStatementType(statementType)

    override fun findByStatementTypePaged(statementType: StatementType, pageable: Pageable): Page<FinancialStatement> =
        financialStatementRepository.findByStatementType(statementType, pageable)

    override fun findByStatus(status: StatementStatus): List<FinancialStatement> =
        financialStatementRepository.findByStatus(status)

    override fun findByStatusPaged(status: StatementStatus, pageable: Pageable): Page<FinancialStatement> =
        financialStatementRepository.findByStatus(status, pageable)

    override fun findByCompanyStockCode(stockCode: String): List<FinancialStatement> =
        financialStatementRepository.findByCompanyStockCode(stockCode)

    override fun findByCompanyIdAndYear(companyId: Long, year: Int): List<FinancialStatement> =
        financialStatementRepository.findByCompanyIdAndYear(companyId, year)

    override fun findByCompanyIdAndYearAndType(companyId: Long, year: Int, statementType: StatementType): List<FinancialStatement> =
        financialStatementRepository.findByCompanyIdAndYearAndType(companyId, year, statementType)

    override fun findByFiscalYearIdAndStatementType(fiscalYearId: Long, statementType: StatementType): List<FinancialStatement> =
        financialStatementRepository.findByFiscalYearIdAndStatementType(fiscalYearId, statementType)

    @Transactional
    override fun createStatement(statement: FinancialStatement, userId: UUID): FinancialStatement {
        // Validate fiscal year exists
        if (!fiscalYearRepository.existsById(statement.fiscalYear.id!!)) {
            throw EntityNotFoundException("Fiscal year not found with id: ${statement.fiscalYear.id}")
        }

        // Validate user exists
        if (!userRepository.existsById(statement.user.id)) {
            throw EntityNotFoundException("User not found with id: ${statement.user.id}")
        }

        // Check for duplicate statement types for the same fiscal year
        val existingStatements = financialStatementRepository.findByFiscalYearIdAndStatementType(
            statement.fiscalYear.id!!, statement.statementType)

        if (existingStatements.isNotEmpty() &&
            (statement.period == null || existingStatements.any { it.period == statement.period })) {
            throw IllegalStateException("A ${statement.statementType} statement" +
                    (if (statement.period != null) " for period ${statement.period}" else "") +
                    " already exists for fiscal year ${statement.fiscalYear.year}")
        }

        // Create with default status PENDING
        val statementWithStatus = statement.copy(
            status = StatementStatus.PENDING,
            uploadDate = OffsetDateTime.now()
        )

        val savedStatement = financialStatementRepository.save(statementWithStatus)

        auditLogService.logEvent(
            userId = userId,
            action = "CREATE",
            entityType = "FINANCIAL_STATEMENT",
            entityId = savedStatement.id.toString(),
            details = "Created ${statement.statementType} statement for fiscal year ${statement.fiscalYear.year}"
        )

        return savedStatement
    }

    @Transactional
    override fun updateStatement(statement: FinancialStatement, userId: UUID): FinancialStatement {
        // Verify statement exists
        val existingStatement = findById(statement.id!!)

        // Check if fiscal year is being changed
        if (existingStatement.fiscalYear.id != statement.fiscalYear.id) {
            // Validate new fiscal year exists
            if (!fiscalYearRepository.existsById(statement.fiscalYear.id!!)) {
                throw EntityNotFoundException("Fiscal year not found with id: ${statement.fiscalYear.id}")
            }

            // Check for duplicate statement types for the new fiscal year
            val existingStatements = financialStatementRepository.findByFiscalYearIdAndStatementType(
                statement.fiscalYear.id!!, statement.statementType)

            if (existingStatements.isNotEmpty() &&
                (statement.period == null || existingStatements.any { it.period == statement.period })) {
                throw IllegalStateException("A ${statement.statementType} statement" +
                        (if (statement.period != null) " for period ${statement.period}" else "") +
                        " already exists for fiscal year ${statement.fiscalYear.year}")
            }
        }
        // If only statement type or period is changing, still need to check for duplicates
        else if (existingStatement.statementType != statement.statementType ||
            existingStatement.period != statement.period) {

            val existingStatements = financialStatementRepository.findByFiscalYearIdAndStatementType(
                statement.fiscalYear.id!!, statement.statementType)
                .filter { it.id != statement.id }

            if (existingStatements.isNotEmpty() &&
                (statement.period == null || existingStatements.any { it.period == statement.period })) {
                throw IllegalStateException("A ${statement.statementType} statement" +
                        (if (statement.period != null) " for period ${statement.period}" else "") +
                        " already exists for fiscal year ${statement.fiscalYear.year}")
            }
        }

        // Preserve original upload date
        val updatedStatement = statement.copy(
            uploadDate = existingStatement.uploadDate,
            // Don't allow status to be updated directly
            status = existingStatement.status
        )

        val savedStatement = financialStatementRepository.save(updatedStatement)

        auditLogService.logEvent(
            userId = userId,
            action = "UPDATE",
            entityType = "FINANCIAL_STATEMENT",
            entityId = savedStatement.id.toString(),
            details = "Updated ${statement.statementType} statement for fiscal year ${statement.fiscalYear.year}"
        )

        return savedStatement
    }

    @Transactional
    override fun deleteStatement(id: Long, userId: UUID) {
        val statement = findById(id)

        // Check if the statement has associated records that would be deleted
        if (statement.financialData != null ||
            statement.fraudRiskAssessment != null ||
            statement.altmanZScore != null ||
            statement.beneishMScore != null ||
            statement.piotroskiFScore != null ||
            statement.documents.isNotEmpty() ||
            statement.mlPredictions.isNotEmpty()) {

            throw IllegalStateException("Cannot delete financial statement because it has associated records")
        }

        financialStatementRepository.delete(statement)

        auditLogService.logEvent(
            userId = userId,
            action = "DELETE",
            entityType = "FINANCIAL_STATEMENT",
            entityId = id.toString(),
            details = "Deleted ${statement.statementType} statement for fiscal year ${statement.fiscalYear.year}"
        )
    }

    @Transactional
    override fun updateStatus(id: Long, status: StatementStatus, userId: UUID): FinancialStatement {
        val statement = findById(id)

        // Update only the status field
        val updatedStatement = statement.copy(status = status)
        val savedStatement = financialStatementRepository.save(updatedStatement)

        auditLogService.logEvent(
            userId = userId,
            action = "UPDATE_STATUS",
            entityType = "FINANCIAL_STATEMENT",
            entityId = id.toString(),
            details = "Updated status to $status for ${statement.statementType} statement"
        )

        return savedStatement
    }
}