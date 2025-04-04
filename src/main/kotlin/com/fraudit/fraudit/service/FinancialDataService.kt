package com.fraudit.fraudit.service

import com.fraudit.fraudit.domain.entity.FinancialData
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface FinancialDataService {
    /**
     * Find all financial data records
     */
    fun findAll(): List<FinancialData>

    /**
     * Find all financial data records with pagination
     */
    fun findAllPaged(pageable: Pageable): Page<FinancialData>

    /**
     * Find financial data by ID
     */
    fun findById(id: Long): FinancialData

    /**
     * Find financial data by statement ID
     */
    fun findByStatementId(statementId: Long): FinancialData?

    /**
     * Find latest financial data for a company
     */
    fun findLatestByCompanyId(companyId: Long): List<FinancialData>

    /**
     * Find financial data for a company with pagination
     */
    fun findByCompanyId(companyId: Long, pageable: Pageable): Page<FinancialData>

    /**
     * Create a new financial data record
     */
    fun createFinancialData(financialData: FinancialData, userId: UUID): FinancialData

    /**
     * Update an existing financial data record
     */
    fun updateFinancialData(financialData: FinancialData, userId: UUID): FinancialData

    /**
     * Delete a financial data record
     */
    fun deleteFinancialData(id: Long, userId: UUID)

    /**
     * Calculate derived values for a financial data record
     */
    fun calculateDerivedValues(id: Long, userId: UUID): FinancialData

    /**
     * Calculate growth rates compared to previous period
     */
    fun calculateGrowthRates(statementId: Long, userId: UUID): FinancialData
}