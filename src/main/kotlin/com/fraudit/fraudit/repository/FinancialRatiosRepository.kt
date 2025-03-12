package com.fraudit.fraudit.repository

import com.fraudit.fraudit.domain.entity.FinancialRatios
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface FinancialRatiosRepository : JpaRepository<FinancialRatios, Long> {
    fun findByStatementId(statementId: Long): FinancialRatios?

    @Query("SELECT fr FROM FinancialRatios fr JOIN fr.statement fs JOIN fs.fiscalYear fy JOIN fy.company c WHERE c.id = :companyId ORDER BY fy.year DESC")
    fun findLatestByCompanyId(companyId: Long): List<FinancialRatios>
}