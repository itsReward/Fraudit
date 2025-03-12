package com.fraudit.fraudit.repository

import com.fraudit.fraudit.domain.entity.MlPrediction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface MlPredictionRepository : JpaRepository<MlPrediction, Long> {
    fun findByStatementId(statementId: Long): List<MlPrediction>
    fun findByModelId(modelId: Long): List<MlPrediction>

    @Query("SELECT mp FROM MlPrediction mp WHERE mp.statement.id = :statementId AND mp.model.isActive = true ORDER BY mp.predictedAt DESC")
    fun findLatestActiveByStatementId(statementId: Long): List<MlPrediction>
}