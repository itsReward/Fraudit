package com.fraudit.fraudit.service

import com.fraudit.fraudit.domain.entity.Company
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface CompanyService {
    /**
     * Find all companies
     */
    fun findAll(): List<Company>

    /**
     * Find all companies with pagination
     */
    fun findAllPaged(pageable: Pageable): Page<Company>

    /**
     * Find a company by ID
     */
    fun findById(id: Long): Company

    /**
     * Find a company by name
     */
    fun findByName(name: String): Company

    /**
     * Find a company by stock code
     */
    fun findByStockCode(stockCode: String): Company

    /**
     * Find companies by sector
     */
    fun findBySector(sector: String): List<Company>

    /**
     * Find companies by sector with pagination
     */
    fun findBySectorPaged(sector: String, pageable: Pageable): Page<Company>

    /**
     * Create a new company
     */
    fun createCompany(company: Company, userId: UUID): Company

    /**
     * Update an existing company
     */
    fun updateCompany(company: Company, userId: UUID): Company

    /**
     * Delete a company
     */
    fun deleteCompany(id: Long, userId: UUID)

    /**
     * Check if a company name is available
     */
    fun isCompanyNameAvailable(name: String): Boolean

    /**
     * Check if a stock code is available
     */
    fun isStockCodeAvailable(stockCode: String): Boolean
}