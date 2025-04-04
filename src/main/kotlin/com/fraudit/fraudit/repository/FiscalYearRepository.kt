package com.fraudit.fraudit.repository

import com.fraudit.fraudit.domain.entity.Company
import com.fraudit.fraudit.domain.entity.FiscalYear
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface FiscalYearRepository : JpaRepository<FiscalYear, Long> {
    /**
     * Find fiscal years by company and year
     */
    fun findByCompanyAndYear(company: Company, year: Int): Optional<FiscalYear>

    /**
     * Find fiscal years by company ID
     */
    fun findByCompanyId(companyId: Long): List<FiscalYear>

    /**
     * Find fiscal years by company ID with pagination
     */
    fun findByCompanyId(companyId: Long, pageable: Pageable): Page<FiscalYear>

    /**
     * Find fiscal years by year
     */
    fun findByYear(year: Int): List<FiscalYear>

    /**
     * Find fiscal years by year with pagination
     */
    fun findByYear(year: Int, pageable: Pageable): Page<FiscalYear>

    /**
     * Find fiscal years by audit status
     */
    fun findByIsAudited(isAudited: Boolean): List<FiscalYear>

    /**
     * Find fiscal years by audit status with pagination
     */
    fun findByIsAudited(isAudited: Boolean, pageable: Pageable): Page<FiscalYear>

    /**
     * Check if a fiscal year exists for a company and year
     */
    fun existsByCompanyIdAndYear(companyId: Long, year: Int): Boolean

    /**
     * Count the number of statements associated with a fiscal year
     */
    @Query("SELECT COUNT(fs) FROM FinancialStatement fs WHERE fs.fiscalYear.id = :fiscalYearId")
    fun countStatementsByFiscalYearId(fiscalYearId: Long): Long

    /**
     * Find fiscal years by company ID sorted by year descending
     */
    @Query("SELECT fy FROM FiscalYear fy WHERE fy.company.id = :companyId ORDER BY fy.year DESC")
    fun findByCompanyIdOrderByYearDesc(companyId: Long): List<FiscalYear>

    /**
     * Find latest fiscal year for each company
     */
    @Query("SELECT fy FROM FiscalYear fy WHERE fy.id IN " +
            "(SELECT MAX(fy2.id) FROM FiscalYear fy2 GROUP BY fy2.company.id)")
    fun findLatestByCompany(): List<FiscalYear>

    /**
     * Find fiscal years with at least one financial statement
     */
    @Query("SELECT DISTINCT fy FROM FiscalYear fy JOIN fy.financialStatements fs")
    fun findWithStatements(): List<FiscalYear>

    /**
     * Find fiscal years with at least one financial statement with pagination
     */
    @Query("SELECT DISTINCT fy FROM FiscalYear fy JOIN fy.financialStatements fs")
    fun findWithStatements(pageable: Pageable): Page<FiscalYear>
}