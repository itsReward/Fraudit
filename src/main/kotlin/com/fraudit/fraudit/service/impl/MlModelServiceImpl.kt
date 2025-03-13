package com.fraudit.fraudit.service.impl

import com.fraudit.fraudit.domain.entity.MlModel
import com.fraudit.fraudit.repository.MlModelRepository
import com.fraudit.fraudit.service.AuditLogService
import com.fraudit.fraudit.service.MlModelService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import jakarta.persistence.EntityNotFoundException

@Service
class MlModelServiceImpl(
    private val mlModelRepository: MlModelRepository,
    private val auditLogService: AuditLogService
) : MlModelService {

    override fun findAll(): List<MlModel> = mlModelRepository.findAll()

    override fun findById(id: Long): MlModel = mlModelRepository.findById(id)
        .orElseThrow { EntityNotFoundException("ML model not found with id: $id") }

    override fun findByModelName(modelName: String): List<MlModel> = mlModelRepository.findByModelName(modelName)

    override fun findByModelNameAndVersion(modelName: String, modelVersion: String): MlModel =
        mlModelRepository.findByModelNameAndModelVersion(modelName, modelVersion)
            .orElseThrow { EntityNotFoundException("ML model not found with name: $modelName and version: $modelVersion") }

    override fun findActiveModels(): List<MlModel> = mlModelRepository.findByIsActive(true)

    @Transactional
    override fun createModel(model: MlModel, userId: UUID): MlModel {
        // Check if a model with the same name and version already exists
        if (mlModelRepository.findByModelNameAndModelVersion(model.modelName, model.modelVersion).isPresent) {
            throw IllegalArgumentException("Model with name ${model.modelName} and version ${model.modelVersion} already exists")
        }

        val savedModel = mlModelRepository.save(model)

        auditLogService.logEvent(
            userId = userId,
            action = "CREATE",
            entityType = "ML_MODEL",
            entityId = savedModel.id.toString(),
            details = "Created ML model: ${model.modelName} (${model.modelVersion})"
        )

        return savedModel
    }

    @Transactional
    override fun updateModel(model: MlModel, userId: UUID): MlModel {
        // Ensure the model exists
        val existingModel = findById(model.id!!)

        // If model name or version is changing, check for uniqueness
        if (existingModel.modelName != model.modelName || existingModel.modelVersion != model.modelVersion) {
            if (mlModelRepository.findByModelNameAndModelVersion(model.modelName, model.modelVersion).isPresent) {
                throw IllegalArgumentException("Model with name ${model.modelName} and version ${model.modelVersion} already exists")
            }
        }

        val savedModel = mlModelRepository.save(model)

        auditLogService.logEvent(
            userId = userId,
            action = "UPDATE",
            entityType = "ML_MODEL",
            entityId = savedModel.id.toString(),
            details = "Updated ML model: ${model.modelName} (${model.modelVersion})"
        )

        return savedModel
    }

    @Transactional
    override fun deleteModel(id: Long, userId: UUID) {
        val model = findById(id)

        // Check if the model is active
        if (model.isActive) {
            throw IllegalStateException("Cannot delete an active model. Deactivate it first.")
        }

        mlModelRepository.delete(model)

        auditLogService.logEvent(
            userId = userId,
            action = "DELETE",
            entityType = "ML_MODEL",
            entityId = id.toString(),
            details = "Deleted ML model: ${model.modelName} (${model.modelVersion})"
        )
    }

    @Transactional
    override fun activateModel(id: Long, userId: UUID): MlModel {
        val model = findById(id)

        // If already active, no change needed
        if (model.isActive) {
            return model
        }

        // Deactivate all current active models with the same model type
        val activeModels = mlModelRepository.findByIsActive(true)
            .filter { it.modelType == model.modelType }

        for (activeModel in activeModels) {
            val deactivatedModel = activeModel.copy(isActive = false)
            mlModelRepository.save(deactivatedModel)

            auditLogService.logEvent(
                userId = userId,
                action = "DEACTIVATE",
                entityType = "ML_MODEL",
                entityId = activeModel.id.toString(),
                details = "Deactivated ML model: ${activeModel.modelName} (${activeModel.modelVersion})"
            )
        }

        // Activate the specified model
        val activatedModel = model.copy(isActive = true)
        val savedModel = mlModelRepository.save(activatedModel)

        auditLogService.logEvent(
            userId = userId,
            action = "ACTIVATE",
            entityType = "ML_MODEL",
            entityId = savedModel.id.toString(),
            details = "Activated ML model: ${model.modelName} (${model.modelVersion})"
        )

        return savedModel
    }

    @Transactional
    override fun deactivateModel(id: Long, userId: UUID): MlModel {
        val model = findById(id)

        // If already inactive, no change needed
        if (!model.isActive) {
            return model
        }

        // Deactivate the model
        val deactivatedModel = model.copy(isActive = false)
        val savedModel = mlModelRepository.save(deactivatedModel)

        auditLogService.logEvent(
            userId = userId,
            action = "DEACTIVATE",
            entityType = "ML_MODEL",
            entityId = savedModel.id.toString(),
            details = "Deactivated ML model: ${model.modelName} (${model.modelVersion})"
        )

        return savedModel
    }
}
