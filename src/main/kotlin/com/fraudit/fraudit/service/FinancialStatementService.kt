package com.fraudit.fraudit.service

import com.fraudit.fraudit.domain.entity.FinancialStatement
import com.fraudit.fraudit.domain.enum.StatementStatus
import com.fraudit.fraudit.domain.enum.StatementType
import java.util.UUID

interface FinancialStatementService {
    fun findAll(): List<FinancialStatement>
    fun findById(id: Long): FinancialStatement
    fun findByUserId(userId: UUID): List<FinancialStatement>
    fun findByFiscalYearId(fiscalYearId: Long): List<FinancialStatement>
    fun findByCompanyId(companyId: Long): List<FinancialStatement>
    fun findByStatementType(statementType: StatementType): List<FinancialStatement>
    fun findByStatus(status: StatementStatus): List<FinancialStatement>
    fun findByCompanyStockCode(stockCode: String): List<FinancialStatement>
    fun createStatement(statement: FinancialStatement): FinancialStatement
    fun updateStatement(statement: FinancialStatement): FinancialStatement
    fun deleteStatement(id: Long, userId: UUID)
    fun updateStatus(id: Long, status: StatementStatus, userId: UUID): FinancialStatement
}
