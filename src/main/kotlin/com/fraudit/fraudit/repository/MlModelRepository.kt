package com.fraudit.fraudit.repository

import com.fraudit.fraudit.domain.entity.MlModel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface MlModelRepository : JpaRepository<MlModel, Long> {
    fun findByModelName(modelName: String): List<MlModel>
    fun findByModelNameAndModelVersion(modelName: String, modelVersion: String): Optional<MlModel>
    fun findByIsActive(isActive: Boolean): List<MlModel>
    fun findByCreatedById(userId: UUID): List<MlModel>
}
