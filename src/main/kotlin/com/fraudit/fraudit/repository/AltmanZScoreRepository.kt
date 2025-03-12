package com.fraudit.fraudit.repository

import com.fraudit.fraudit.domain.entity.AltmanZScore
import com.fraudit.fraudit.domain.enum.RiskCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface AltmanZScoreRepository : JpaRepository<AltmanZScore, Long> {
    fun findByStatementId(statementId: Long): AltmanZScore?
    fun findByRiskCategory(riskCategory: RiskCategory): List<AltmanZScore>

    @Query("SELECT az FROM AltmanZScore az JOIN az.statement fs JOIN fs.fiscalYear fy JOIN fy.company c WHERE c.id = :companyId ORDER BY fy.year DESC")
    fun findLatestByCompanyId(companyId: Long): List<AltmanZScore>
}
