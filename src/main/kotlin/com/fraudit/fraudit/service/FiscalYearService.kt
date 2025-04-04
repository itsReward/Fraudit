package com.fraudit.fraudit.service

import com.fraudit.fraudit.domain.entity.FiscalYear
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface FiscalYearService {
    /**
     * Find all fiscal years
     */
    fun findAll(): List<FiscalYear>

    /**
     * Find all fiscal years with pagination
     */
    fun findAllPaged(pageable: Pageable): Page<FiscalYear>

    /**
     * Find a fiscal year by ID
     */
    fun findById(id: Long): FiscalYear

    /**
     * Find fiscal years by company ID
     */
    fun findByCompanyId(companyId: Long): List<FiscalYear>

    /**
     * Find fiscal years by company ID with pagination
     */
    fun findByCompanyIdPaged(companyId: Long, pageable: Pageable): Page<FiscalYear>

    /**
     * Find a fiscal year by company ID and year
     */
    fun findByCompanyIdAndYear(companyId: Long, year: Int): FiscalYear

    /**
     * Find fiscal years by year
     */
    fun findByYear(year: Int): List<FiscalYear>

    /**
     * Find fiscal years by year with pagination
     */
    fun findByYearPaged(year: Int, pageable: Pageable): Page<FiscalYear>

    /**
     * Find fiscal years by audit status
     */
    fun findByAuditStatus(isAudited: Boolean): List<FiscalYear>

    /**
     * Find fiscal years by audit status with pagination
     */
    fun findByAuditStatusPaged(isAudited: Boolean, pageable: Pageable): Page<FiscalYear>

    /**
     * Create a new fiscal year
     */
    fun createFiscalYear(fiscalYear: FiscalYear, userId: UUID): FiscalYear

    /**
     * Update an existing fiscal year
     */
    fun updateFiscalYear(fiscalYear: FiscalYear, userId: UUID): FiscalYear

    /**
     * Delete a fiscal year
     */
    fun deleteFiscalYear(id: Long, userId: UUID)

    /**
     * Mark a fiscal year as audited
     */
    fun markAsAudited(id: Long, userId: UUID): FiscalYear

    /**
     * Mark a fiscal year as unaudited
     */
    fun markAsUnaudited(id: Long, userId: UUID): FiscalYear

    /**
     * Get the number of statements associated with a fiscal year
     */
    fun getStatementCountForFiscalYear(id: Long): Long
}