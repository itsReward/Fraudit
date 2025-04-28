package com.fraudit.fraudit.repository

import com.fraudit.fraudit.domain.entity.FraudRiskAssessment
import com.fraudit.fraudit.domain.enum.RiskLevel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface FraudRiskAssessmentRepository : JpaRepository<FraudRiskAssessment, Long> {
    fun findByStatementId(statementId: Long): FraudRiskAssessment?
    fun findByRiskLevel(riskLevel: RiskLevel): List<FraudRiskAssessment>
    fun findByAssessedById(userId: UUID): List<FraudRiskAssessment>

    @Query("SELECT fra FROM FraudRiskAssessment fra JOIN fra.statement fs JOIN fs.fiscalYear fy JOIN fy.company c WHERE c.id = :companyId ORDER BY fy.year DESC")
    fun findLatestByCompanyId(companyId: Long): List<FraudRiskAssessment>

    @Modifying
    @Query("DELETE FROM FraudRiskAssessment fr WHERE fr.statement.id = :statementId")
    fun deleteByStatementId(statementId: Long)

}