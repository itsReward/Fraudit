package com.fraudit.fraudit.service.impl

import com.fraudit.fraudit.domain.entity.Company
import com.fraudit.fraudit.repository.CompanyRepository
import com.fraudit.fraudit.service.AuditLogService
import com.fraudit.fraudit.service.CompanyService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import jakarta.persistence.EntityNotFoundException

@Service
class CompanyServiceImpl(
    private val companyRepository: CompanyRepository,
    private val auditLogService: AuditLogService
) : CompanyService {

    override fun findAll(): List<Company> = companyRepository.findAll()

    override fun findAllPaged(pageable: Pageable): Page<Company> = companyRepository.findAll(pageable)

    override fun findById(id: Long): Company = companyRepository.findById(id)
        .orElseThrow { EntityNotFoundException("Company not found with id: $id") }

    override fun findByName(name: String): Company = companyRepository.findByName(name)
        .orElseThrow { EntityNotFoundException("Company not found with name: $name") }

    override fun findByStockCode(stockCode: String): Company = companyRepository.findByStockCode(stockCode)
        .orElseThrow { EntityNotFoundException("Company not found with stock code: $stockCode") }

    override fun findBySector(sector: String): List<Company> = companyRepository.findBySector(sector)

    override fun findBySectorPaged(sector: String, pageable: Pageable): Page<Company> =
        companyRepository.findBySector(sector, pageable)

    @Transactional
    override fun createCompany(company: Company, userId: UUID): Company {
        if (!isCompanyNameAvailable(company.name)) {
            throw IllegalArgumentException("Company name ${company.name} is already taken")
        }
        if (!isStockCodeAvailable(company.stockCode)) {
            throw IllegalArgumentException("Stock code ${company.stockCode} is already in use")
        }

        val savedCompany = companyRepository.save(company)

        auditLogService.logEvent(
            userId = userId,
            action = "CREATE",
            entityType = "COMPANY",
            entityId = savedCompany.id.toString(),
            details = "Created company: ${savedCompany.name} (${savedCompany.stockCode})"
        )

        return savedCompany
    }

    @Transactional
    override fun updateCompany(company: Company, userId: UUID): Company {
        val existingCompany = findById(company.id!!)

        // Check if name is being changed and if it's available
        if (existingCompany.name != company.name && !isCompanyNameAvailable(company.name)) {
            throw IllegalArgumentException("Company name ${company.name} is already taken")
        }

        // Check if stock code is being changed and if it's available
        if (existingCompany.stockCode != company.stockCode && !isStockCodeAvailable(company.stockCode)) {
            throw IllegalArgumentException("Stock code ${company.stockCode} is already in use")
        }

        val savedCompany = companyRepository.save(company)

        auditLogService.logEvent(
            userId = userId,
            action = "UPDATE",
            entityType = "COMPANY",
            entityId = savedCompany.id.toString(),
            details = "Updated company: ${savedCompany.name} (${savedCompany.stockCode})"
        )

        return savedCompany
    }

    @Transactional
    override fun deleteCompany(id: Long, userId: UUID) {
        val company = findById(id)

        try {
            companyRepository.delete(company)

            auditLogService.logEvent(
                userId = userId,
                action = "DELETE",
                entityType = "COMPANY",
                entityId = id.toString(),
                details = "Deleted company: ${company.name} (${company.stockCode})"
            )
        } catch (e: Exception) {
            throw IllegalStateException("Cannot delete company because it has associated records", e)
        }
    }

    override fun isCompanyNameAvailable(name: String): Boolean = !companyRepository.existsByName(name)

    override fun isStockCodeAvailable(stockCode: String): Boolean = !companyRepository.existsByStockCode(stockCode)
}