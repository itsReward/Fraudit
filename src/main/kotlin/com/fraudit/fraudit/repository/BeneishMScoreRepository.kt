package com.fraudit.fraudit.repository

import com.fraudit.fraudit.domain.entity.BeneishMScore
import com.fraudit.fraudit.domain.enum.ManipulationProbability
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface BeneishMScoreRepository : JpaRepository<BeneishMScore, Long> {
    fun findByStatementId(statementId: Long): BeneishMScore?
    fun findByManipulationProbability(manipulationProbability: ManipulationProbability): List<BeneishMScore>

    @Query("SELECT bm FROM BeneishMScore bm JOIN bm.statement fs JOIN fs.fiscalYear fy JOIN fy.company c WHERE c.id = :companyId ORDER BY fy.year DESC")
    fun findLatestByCompanyId(companyId: Long): List<BeneishMScore>

    @Modifying
    @Query("DELETE FROM BeneishMScore fr WHERE fr.statement.id = :statementId")
    fun deleteByStatementId(statementId: Long)

}
