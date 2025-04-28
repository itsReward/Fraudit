package com.fraudit.fraudit.repository

import com.fraudit.fraudit.domain.entity.MlFeatures
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface MlFeaturesRepository : JpaRepository<MlFeatures, Long> {
    fun findByStatementId(statementId: Long): MlFeatures?


    @Modifying
    @Query("DELETE FROM MlFeatures fr WHERE fr.statement.id = :statementId")
    fun deleteByStatementId(statementId: Long)

}

