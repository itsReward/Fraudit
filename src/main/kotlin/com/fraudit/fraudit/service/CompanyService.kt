package com.fraudit.fraudit.service

import com.fraudit.fraudit.domain.entity.Company
import java.util.UUID

interface CompanyService {
    fun findAll(): List<Company>
    fun findById(id: Long): Company
    fun findByName(name: String): Company
    fun findByStockCode(stockCode: String): Company
    fun findBySector(sector: String): List<Company>
    fun createCompany(company: Company, userId: UUID): Company
    fun updateCompany(company: Company, userId: UUID): Company
    fun deleteCompany(id: Long, userId: UUID)
    fun isCompanyNameAvailable(name: String): Boolean
    fun isStockCodeAvailable(stockCode: String): Boolean
}
