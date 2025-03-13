package com.fraudit.fraudit.service.impl

import com.fraudit.fraudit.domain.entity.FiscalYear
import com.fraudit.fraudit.repository.CompanyRepository
import com.fraudit.fraudit.repository.FiscalYearRepository
import com.fraudit.fraudit.service.AuditLogService
import com.fraudit.fraudit.service.FiscalYearService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import jakarta.persistence.EntityNotFoundException

@Service
class FiscalYearServiceImpl(
    private val fiscalYearRepository: FiscalYearRepository,
    private val companyRepository: CompanyRepository,
    private val auditLogService: AuditLogService
) : FiscalYearService {

    override fun findAll(): List<FiscalYear> = fiscalYearRepository.findAll()

    override fun findById(id: Long): FiscalYear = fiscalYearRepository.findById(id)
        .orElseThrow { EntityNotFoundException("Fiscal year not found with id: $id") }

    override fun findByCompanyId(companyId: Long): List<FiscalYear> = fiscalYearRepository.findByCompanyId(companyId)

    override fun findByCompanyIdAndYear(companyId: Long, year: Int): FiscalYear {
        val company = companyRepository.findById(companyId)
            .orElseThrow { EntityNotFoundException("Company not found with id: $companyId") }

        return fiscalYearRepository.findByCompanyAndYear(company, year)
            .orElseThrow { EntityNotFoundException("Fiscal year not found for company id: $companyId and year: $year") }
    }

    override fun findByYear(year: Int): List<FiscalYear> = fiscalYearRepository.findByYear(year)

    @Transactional
    override fun createFiscalYear(fiscalYear: FiscalYear, userId: UUID): FiscalYear {
        // Check if this fiscal year already exists for the company
        if (fiscalYearRepository.existsByCompanyIdAndYear(fiscalYear.company.id!!, fiscalYear.year)) {
            throw IllegalArgumentException("Fiscal year ${fiscalYear.year} already exists for company ${fiscalYear.company.name}")
        }

        // Validate start date is before end date
        if (fiscalYear.startDate.isAfter(fiscalYear.endDate)) {
            throw IllegalArgumentException("Start date must be before end date")
        }

        val savedFiscalYear = fiscalYearRepository.save(fiscalYear)

        auditLogService.logEvent(
            userId = userId,
            action = "CREATE",
            entityType = "FISCAL_YEAR",
            entityId = savedFiscalYear.id.toString(),
            details = "Created fiscal year ${savedFiscalYear.year} for company: ${fiscalYear.company.name}"
        )

        return savedFiscalYear
    }

    @Transactional
    override fun updateFiscalYear(fiscalYear: FiscalYear, userId: UUID): FiscalYear {
        val existingFiscalYear = findById(fiscalYear.id!!)

        // Validate start date is before end date
        if (fiscalYear.startDate.isAfter(fiscalYear.endDate)) {
            throw IllegalArgumentException("Start date must be before end date")
        }

        // If the company or year is changing, check for uniqueness
        if (existingFiscalYear.company.id != fiscalYear.company.id || existingFiscalYear.year != fiscalYear.year) {
            if (fiscalYearRepository.existsByCompanyIdAndYear(fiscalYear.company.id!!, fiscalYear.year)) {
                throw IllegalArgumentException("Fiscal year ${fiscalYear.year} already exists for company ${fiscalYear.company.name}")
            }
        }

        val savedFiscalYear = fiscalYearRepository.save(fiscalYear)

        auditLogService.logEvent(
            userId = userId,
            action = "UPDATE",
            entityType = "FISCAL_YEAR",
            entityId = savedFiscalYear.id.toString(),
            details = "Updated fiscal year ${savedFiscalYear.year} for company: ${fiscalYear.company.name}"
        )

        return savedFiscalYear
    }

    @Transactional
    override fun deleteFiscalYear(id: Long, userId: UUID) {
        val fiscalYear = findById(id)
        fiscalYearRepository.delete(fiscalYear)

        auditLogService.logEvent(
            userId = userId,
            action = "DELETE",
            entityType = "FISCAL_YEAR",
            entityId = id.toString(),
            details = "Deleted fiscal year ${fiscalYear.year} for company: ${fiscalYear.company.name}"
        )
    }

    @Transactional
    override fun markAsAudited(id: Long, userId: UUID): FiscalYear {
        val fiscalYear = findById(id)

        // Update only the isAudited field to true
        val updatedFiscalYear = fiscalYear.copy(isAudited = true)
        val savedFiscalYear = fiscalYearRepository.save(updatedFiscalYear)

        auditLogService.logEvent(
            userId = userId,
            action = "MARK_AUDITED",
            entityType = "FISCAL_YEAR",
            entityId = id.toString(),
            details = "Marked fiscal year ${fiscalYear.year} as audited for company: ${fiscalYear.company.name}"
        )

        return savedFiscalYear
    }
}