package com.fraudit.fraudit.service

import com.fraudit.fraudit.domain.entity.FiscalYear
import java.util.UUID

interface FiscalYearService {
    fun findAll(): List<FiscalYear>
    fun findById(id: Long): FiscalYear
    fun findByCompanyId(companyId: Long): List<FiscalYear>
    fun findByCompanyIdAndYear(companyId: Long, year: Int): FiscalYear
    fun findByYear(year: Int): List<FiscalYear>
    fun createFiscalYear(fiscalYear: FiscalYear, userId: UUID): FiscalYear
    fun updateFiscalYear(fiscalYear: FiscalYear, userId: UUID): FiscalYear
    fun deleteFiscalYear(id: Long, userId: UUID)
    fun markAsAudited(id: Long, userId: UUID): FiscalYear
}