package com.fraudit.fraudit.repository

import com.fraudit.fraudit.domain.entity.FinancialData
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface FinancialDataRepository : JpaRepository<FinancialData, Long> {
    /**
     * Find financial data by statement ID
     */
    fun findByStatementId(statementId: Long): FinancialData?

    /**
     * Find latest financial data for each company, sorted by year descending
     */
    @Query("SELECT fd FROM FinancialData fd JOIN fd.statement fs JOIN fs.fiscalYear fy JOIN fy.company c WHERE c.id = :companyId ORDER BY fy.year DESC")
    fun findLatestByCompanyId(companyId: Long): List<FinancialData>

    /**
     * Find financial data for a company with pagination
     */
    fun findByStatementFiscalYearCompanyId(companyId: Long, pageable: Pageable): Page<FinancialData>

    /**
     * Find financial data by company ID and year
     */
    @Query("SELECT fd FROM FinancialData fd JOIN fd.statement fs JOIN fs.fiscalYear fy JOIN fy.company c WHERE c.id = :companyId AND fy.year = :year")
    fun findByCompanyIdAndYear(companyId: Long, year: Int): List<FinancialData>

    /**
     * Find financial data by sector
     */
    @Query("SELECT fd FROM FinancialData fd JOIN fd.statement fs JOIN fs.fiscalYear fy JOIN fy.company c WHERE c.sector = :sector")
    fun findBySector(sector: String): List<FinancialData>

    /**
     * Find financial data by sector with pagination
     */
    @Query("SELECT fd FROM FinancialData fd JOIN fd.statement fs JOIN fs.fiscalYear fy JOIN fy.company c WHERE c.sector = :sector")
    fun findBySector(sector: String, pageable: Pageable): Page<FinancialData>
}