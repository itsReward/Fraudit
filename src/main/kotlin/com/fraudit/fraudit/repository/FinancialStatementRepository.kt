package com.fraudit.fraudit.repository

import com.fraudit.fraudit.domain.entity.FinancialStatement
import com.fraudit.fraudit.domain.enum.StatementStatus
import com.fraudit.fraudit.domain.enum.StatementType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface FinancialStatementRepository : JpaRepository<FinancialStatement, Long> {
    fun findByUserId(userId: UUID): List<FinancialStatement>
    fun findByFiscalYearId(fiscalYearId: Long): List<FinancialStatement>
    fun findByFiscalYearCompanyId(companyId: Long): List<FinancialStatement>
    fun findByStatementType(statementType: StatementType): List<FinancialStatement>
    fun findByStatus(status: StatementStatus): List<FinancialStatement>
    fun findByFiscalYearIdAndStatementType(fiscalYearId: Long, statementType: StatementType): List<FinancialStatement>

    @Query("SELECT fs FROM FinancialStatement fs JOIN fs.fiscalYear fy JOIN fy.company c WHERE c.stockCode = :stockCode")
    fun findByCompanyStockCode(stockCode: String): List<FinancialStatement>
}
