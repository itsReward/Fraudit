package com.fraudit.fraudit.repository

import com.fraudit.fraudit.domain.entity.MlFeatures
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MlFeaturesRepository : JpaRepository<MlFeatures, Long> {
    fun findByStatementId(statementId: Long): MlFeatures?
}

