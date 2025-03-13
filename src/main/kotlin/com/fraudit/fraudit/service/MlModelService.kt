package com.fraudit.fraudit.service

import com.fraudit.fraudit.domain.entity.MlModel
import java.util.UUID

interface MlModelService {
    fun findAll(): List<MlModel>
    fun findById(id: Long): MlModel
    fun findByModelName(modelName: String): List<MlModel>
    fun findByModelNameAndVersion(modelName: String, modelVersion: String): MlModel
    fun findActiveModels(): List<MlModel>
    fun createModel(model: MlModel, userId: UUID): MlModel
    fun updateModel(model: MlModel, userId: UUID): MlModel
    fun deleteModel(id: Long, userId: UUID)
    fun activateModel(id: Long, userId: UUID): MlModel
    fun deactivateModel(id: Long, userId: UUID): MlModel
}