package com.fraudit.fraudit.repository

import com.fraudit.fraudit.domain.entity.Company
import com.fraudit.fraudit.domain.entity.FiscalYear
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface FiscalYearRepository : JpaRepository<FiscalYear, Long> {
    fun findByCompanyAndYear(company: Company, year: Int): Optional<FiscalYear>
    fun findByCompanyId(companyId: Long): List<FiscalYear>
    fun findByYear(year: Int): List<FiscalYear>
    fun existsByCompanyIdAndYear(companyId: Long, year: Int): Boolean
}