package com.fraudit.fraudit.repository

import com.fraudit.fraudit.domain.entity.FinancialStatement
import com.fraudit.fraudit.domain.enum.StatementStatus
import com.fraudit.fraudit.domain.enum.StatementType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface FinancialStatementRepository : JpaRepository<FinancialStatement, Long> {
    /**
     * Find statements by user ID
     */
    fun findByUserId(userId: UUID): List<FinancialStatement>

    /**
     * Find statements by user ID with pagination
     */
    fun findByUserId(userId: UUID, pageable: Pageable): Page<FinancialStatement>

    /**
     * Find statements by fiscal year ID
     */
    fun findByFiscalYearId(fiscalYearId: Long): List<FinancialStatement>

    /**
     * Find statements by fiscal year ID with pagination
     */
    fun findByFiscalYearId(fiscalYearId: Long, pageable: Pageable): Page<FinancialStatement>

    /**
     * Find statements by company ID
     */
    fun findByFiscalYearCompanyId(companyId: Long): List<FinancialStatement>

    /**
     * Find statements by company ID with pagination
     */
    fun findByFiscalYearCompanyId(companyId: Long, pageable: Pageable): Page<FinancialStatement>

    /**
     * Find statements by statement type
     */
    fun findByStatementType(statementType: StatementType): List<FinancialStatement>

    /**
     * Find statements by statement type with pagination
     */
    fun findByStatementType(statementType: StatementType, pageable: Pageable): Page<FinancialStatement>

    /**
     * Find statements by status
     */
    fun findByStatus(status: StatementStatus): List<FinancialStatement>

    /**
     * Find statements by status with pagination
     */
    fun findByStatus(status: StatementStatus, pageable: Pageable): Page<FinancialStatement>

    /**
     * Find statements by fiscal year ID and statement type
     */
    fun findByFiscalYearIdAndStatementType(fiscalYearId: Long, statementType: StatementType): List<FinancialStatement>

    /**
     * Find statements by fiscal year ID and statement type with pagination
     */
    fun findByFiscalYearIdAndStatementType(fiscalYearId: Long, statementType: StatementType, pageable: Pageable): Page<FinancialStatement>

    /**
     * Find statements by company ID and statement type
     */
    fun findByFiscalYearCompanyIdAndStatementType(companyId: Long, statementType: StatementType): List<FinancialStatement>

    /**
     * Find statements by company ID and statement type with pagination
     */
    fun findByFiscalYearCompanyIdAndStatementType(companyId: Long, statementType: StatementType, pageable: Pageable): Page<FinancialStatement>

    /**
     * Find statements by company stock code
     */
    @Query("SELECT fs FROM FinancialStatement fs JOIN fs.fiscalYear fy JOIN fy.company c WHERE c.stockCode = :stockCode")
    fun findByCompanyStockCode(stockCode: String): List<FinancialStatement>

    /**
     * Find statements by company stock code with pagination
     */
    @Query("SELECT fs FROM FinancialStatement fs JOIN fs.fiscalYear fy JOIN fy.company c WHERE c.stockCode = :stockCode")
    fun findByCompanyStockCode(stockCode: String, pageable: Pageable): Page<FinancialStatement>

    /**
     * Find statements by company ID and year
     */
    @Query("SELECT fs FROM FinancialStatement fs JOIN fs.fiscalYear fy WHERE fy.company.id = :companyId AND fy.year = :year")
    fun findByCompanyIdAndYear(companyId: Long, year: Int): List<FinancialStatement>

    /**
     * Find statements by company ID, year, and statement type
     */
    @Query("SELECT fs FROM FinancialStatement fs JOIN fs.fiscalYear fy WHERE fy.company.id = :companyId AND fy.year = :year AND fs.statementType = :statementType")
    fun findByCompanyIdAndYearAndType(companyId: Long, year: Int, statementType: StatementType): List<FinancialStatement>

    /**
     * Find latest statements for each company
     */
    @Query("SELECT fs FROM FinancialStatement fs JOIN fs.fiscalYear fy JOIN fy.company c WHERE fs.id IN " +
            "(SELECT MAX(fs2.id) FROM FinancialStatement fs2 JOIN fs2.fiscalYear fy2 WHERE fy2.company.id = c.id GROUP BY fy2.company.id)")
    fun findLatestByCompany(): List<FinancialStatement>

    /**
     * Find statements by period
     */
    fun findByPeriod(period: String): List<FinancialStatement>

    /**
     * Count statements by fiscal year ID
     */
    fun countByFiscalYearId(fiscalYearId: Long): Long

    /**
     * Count statements by user ID
     */
    fun countByUserId(userId: UUID): Long

    /**
     * Find statements by company ID and status
     * (New method for batch processing)
     */
    fun findByFiscalYearCompanyIdAndStatus(companyId: Long, status: StatementStatus): List<FinancialStatement>

    /**
     * Find statements by company ID and status with pagination
     * (New method for batch processing)
     */
    fun findByFiscalYearCompanyIdAndStatus(companyId: Long, status: StatementStatus, pageable: Pageable): Page<FinancialStatement>

    /**
     * Find statements by financial data existence
     * (New method to find statements with financial data)
     */
    @Query("SELECT fs FROM FinancialStatement fs WHERE fs.financialData IS NOT NULL")
    fun findByFinancialDataExists(): List<FinancialStatement>

    /**
     * Find statements by financial data existence with pagination
     * (New method to find statements with financial data)
     */
    @Query("SELECT fs FROM FinancialStatement fs WHERE fs.financialData IS NOT NULL")
    fun findByFinancialDataExists(pageable: Pageable): Page<FinancialStatement>

    /**
     * Find statements by financial data existence but no risk assessment
     * (New method to find statements that need analysis)
     */
    @Query("SELECT fs FROM FinancialStatement fs WHERE fs.financialData IS NOT NULL AND fs.fraudRiskAssessment IS NULL")
    fun findByFinancialDataExistsAndRiskAssessmentNull(): List<FinancialStatement>

    /**
     * Find statements by financial data existence but no risk assessment with pagination
     * (New method to find statements that need analysis)
     */
    @Query("SELECT fs FROM FinancialStatement fs WHERE fs.financialData IS NOT NULL AND fs.fraudRiskAssessment IS NULL")
    fun findByFinancialDataExistsAndRiskAssessmentNull(pageable: Pageable): Page<FinancialStatement>
    
    @Query("SELECT fs FROM FinancialStatement fs JOIN FETCH fs.fiscalYear fy JOIN FETCH fy.company WHERE fs.status = :status")
    fun findByStatusWithCompany(status: StatementStatus): List<FinancialStatement>
}