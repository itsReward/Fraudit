package com.fraudit.fraudit.repository

import com.fraudit.fraudit.domain.entity.PiotroskiFScore
import com.fraudit.fraudit.domain.enum.FinancialStrength
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface PiotroskiFScoreRepository : JpaRepository<PiotroskiFScore, Long> {
    fun findByStatementId(statementId: Long): PiotroskiFScore?
    fun findByFinancialStrength(financialStrength: FinancialStrength): List<PiotroskiFScore>

    @Query("SELECT pf FROM PiotroskiFScore pf JOIN pf.statement fs JOIN fs.fiscalYear fy JOIN fy.company c WHERE c.id = :companyId ORDER BY fy.year DESC")
    fun findLatestByCompanyId(companyId: Long): List<PiotroskiFScore>
}