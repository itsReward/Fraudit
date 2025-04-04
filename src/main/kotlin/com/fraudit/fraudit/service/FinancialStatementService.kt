package com.fraudit.fraudit.service

import com.fraudit.fraudit.domain.entity.FinancialStatement
import com.fraudit.fraudit.domain.enum.StatementStatus
import com.fraudit.fraudit.domain.enum.StatementType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface FinancialStatementService {
    /**
     * Find all financial statements
     */
    fun findAll(): List<FinancialStatement>

    /**
     * Find all financial statements with pagination
     */
    fun findAllPaged(pageable: Pageable): Page<FinancialStatement>

    /**
     * Find a financial statement by ID
     */
    fun findById(id: Long): FinancialStatement

    /**
     * Find financial statements by user ID
     */
    fun findByUserId(userId: UUID): List<FinancialStatement>

    /**
     * Find financial statements by user ID with pagination
     */
    fun findByUserIdPaged(userId: UUID, pageable: Pageable): Page<FinancialStatement>

    /**
     * Find financial statements by fiscal year ID
     */
    fun findByFiscalYearId(fiscalYearId: Long): List<FinancialStatement>

    /**
     * Find financial statements by fiscal year ID with pagination
     */
    fun findByFiscalYearIdPaged(fiscalYearId: Long, pageable: Pageable): Page<FinancialStatement>

    /**
     * Find financial statements by company ID
     */
    fun findByCompanyId(companyId: Long): List<FinancialStatement>

    /**
     * Find financial statements by company ID with pagination
     */
    fun findByCompanyIdPaged(companyId: Long, pageable: Pageable): Page<FinancialStatement>

    /**
     * Find financial statements by statement type
     */
    fun findByStatementType(statementType: StatementType): List<FinancialStatement>

    /**
     * Find financial statements by statement type with pagination
     */
    fun findByStatementTypePaged(statementType: StatementType, pageable: Pageable): Page<FinancialStatement>

    /**
     * Find financial statements by status
     */
    fun findByStatus(status: StatementStatus): List<FinancialStatement>

    /**
     * Find financial statements by status with pagination
     */
    fun findByStatusPaged(status: StatementStatus, pageable: Pageable): Page<FinancialStatement>

    /**
     * Find financial statements by company stock code
     */
    fun findByCompanyStockCode(stockCode: String): List<FinancialStatement>

    /**
     * Find financial statements by company ID and year
     */
    fun findByCompanyIdAndYear(companyId: Long, year: Int): List<FinancialStatement>

    /**
     * Find financial statements by company ID, year, and statement type
     */
    fun findByCompanyIdAndYearAndType(companyId: Long, year: Int, statementType: StatementType): List<FinancialStatement>

    /**
     * Find financial statements by fiscal year ID and statement type
     */
    fun findByFiscalYearIdAndStatementType(fiscalYearId: Long, statementType: StatementType): List<FinancialStatement>

    /**
     * Create a new financial statement
     */
    fun createStatement(statement: FinancialStatement, userId: UUID): FinancialStatement

    /**
     * Update an existing financial statement
     */
    fun updateStatement(statement: FinancialStatement, userId: UUID): FinancialStatement

    /**
     * Delete a financial statement
     */
    fun deleteStatement(id: Long, userId: UUID)

    /**
     * Update the status of a financial statement
     */
    fun updateStatus(id: Long, status: StatementStatus, userId: UUID): FinancialStatement
}