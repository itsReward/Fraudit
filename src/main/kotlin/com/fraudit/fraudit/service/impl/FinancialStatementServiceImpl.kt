package com.fraudit.fraudit.service.impl

import com.fraudit.fraudit.domain.entity.FinancialStatement
import com.fraudit.fraudit.domain.enum.StatementStatus
import com.fraudit.fraudit.domain.enum.StatementType
import com.fraudit.fraudit.repository.FinancialStatementRepository
import com.fraudit.fraudit.service.AuditLogService
import com.fraudit.fraudit.service.FinancialStatementService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import jakarta.persistence.EntityNotFoundException

@Service
class FinancialStatementServiceImpl(
    private val financialStatementRepository: FinancialStatementRepository,
    private val auditLogService: AuditLogService
) : FinancialStatementService {

    override fun findAll(): List<FinancialStatement> = financialStatementRepository.findAll()

    override fun findById(id: Long): FinancialStatement = financialStatementRepository.findById(id)
        .orElseThrow { EntityNotFoundException("Financial statement not found with id: $id") }

    override fun findByUserId(userId: UUID): List<FinancialStatement> = financialStatementRepository.findByUserId(userId)

    override fun findByFiscalYearId(fiscalYearId: Long): List<FinancialStatement> =
        financialStatementRepository.findByFiscalYearId(fiscalYearId)

    override fun findByCompanyId(companyId: Long): List<FinancialStatement> =
        financialStatementRepository.findByFiscalYearCompanyId(companyId)

    override fun findByStatementType(statementType: StatementType): List<FinancialStatement> =
        financialStatementRepository.findByStatementType(statementType)

    override fun findByStatus(status: StatementStatus): List<FinancialStatement> =
        financialStatementRepository.findByStatus(status)

    override fun findByCompanyStockCode(stockCode: String): List<FinancialStatement> =
        financialStatementRepository.findByCompanyStockCode(stockCode)

    @Transactional
    override fun createStatement(statement: FinancialStatement): FinancialStatement {
        val savedStatement = financialStatementRepository.save(statement)

        auditLogService.logEvent(
            userId = statement.user.id,
            action = "CREATE",
            entityType = "FINANCIAL_STATEMENT",
            entityId = savedStatement.id.toString(),
            details = "Created ${statement.statementType} statement for fiscal year ${statement.fiscalYear.year}"
        )

        return savedStatement
    }

    @Transactional
    override fun updateStatement(statement: FinancialStatement): FinancialStatement {
        val existingStatement = findById(statement.id!!)

        val savedStatement = financialStatementRepository.save(statement)

        auditLogService.logEvent(
            userId = statement.user.id,
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