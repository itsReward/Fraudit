package com.fraudit.fraudit.service.impl

import com.fraudit.fraudit.domain.entity.DocumentStorage
import com.fraudit.fraudit.repository.DocumentStorageRepository
import com.fraudit.fraudit.repository.FinancialStatementRepository
import com.fraudit.fraudit.service.AuditLogService
import com.fraudit.fraudit.service.DocumentStorageService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import jakarta.persistence.EntityNotFoundException

@Service
class DocumentStorageServiceImpl(
    private val documentStorageRepository: DocumentStorageRepository,
    private val financialStatementRepository: FinancialStatementRepository,
    private val auditLogService: AuditLogService,
    @Value("\${document.upload.dir:uploads}") private val uploadDir: String
) : DocumentStorageService {

    override fun findAll(): List<DocumentStorage> = documentStorageRepository.findAll()

    override fun findById(id: Long): DocumentStorage = documentStorageRepository.findById(id)
        .orElseThrow { EntityNotFoundException("Document not found with id: $id") }

    override fun findByStatementId(statementId: Long): List<DocumentStorage> =
        documentStorageRepository.findByStatementId(statementId)

    override fun findByFileName(fileName: String): List<DocumentStorage> =
        documentStorageRepository.findByFileName(fileName)

    override fun findByFileType(fileType: String): List<DocumentStorage> =
        documentStorageRepository.findByFileType(fileType)

    @Transactional
    override fun storeDocument(statementId: Long, file: MultipartFile, userId: UUID): DocumentStorage {
        try {
            val statement = financialStatementRepository.findById(statementId)
                .orElseThrow { EntityNotFoundException("Financial statement not found with id: $statementId") }

            // Create upload directory if it doesn't exist
            val uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize()
            Files.createDirectories(uploadPath)

            // Generate a unique filename
            val timestamp = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            val originalFilename = file.originalFilename ?: "unknown-file"
            val fileExtension = originalFilename.substringAfterLast(".", "")
            val fileName = "${statement.fiscalYear.company.stockCode}_${statement.fiscalYear.year}_$timestamp.$fileExtension"

            // Save the file on the server
            val targetLocation = uploadPath.resolve(fileName)
            Files.copy(file.inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING)

            // Create and save document storage record
            val document = DocumentStorage(
                id = null,
                statement = statement,
                fileName = originalFilename,
                fileType = file.contentType ?: "application/octet-stream",
                fileSize = file.size.toInt(),
                filePath = targetLocation.toString(),
                uploadDate = OffsetDateTime.now()
            )

            val savedDocument = documentStorageRepository.save(document)

            auditLogService.logEvent(
                userId = userId,
                action = "UPLOAD",
                entityType = "DOCUMENT",
                entityId = savedDocument.id.toString(),
                details = "Uploaded document: ${originalFilename} for statement id: $statementId"
            )

            return savedDocument
        } catch (ex: IOException) {
            throw RuntimeException("Failed to store file", ex)
        }
    }

    @Transactional
    override fun deleteDocument(id: Long, userId: UUID) {
        val document = findById(id)

        try {
            // Delete the file from the server
            val filePath = Paths.get(document.filePath)
            Files.deleteIfExists(filePath)

            // Delete database record
            documentStorageRepository.delete(document)

            auditLogService.logEvent(
                userId = userId,
                action = "DELETE",
                entityType = "DOCUMENT",
                entityId = id.toString(),
                details = "Deleted document: ${document.fileName}"
            )
        } catch (ex: IOException) {
            throw RuntimeException("Failed to delete file", ex)
        }
    }

    override fun getDocumentContent(id: Long): ByteArray {
        val document = findById(id)

        try {
            val filePath = Paths.get(document.filePath)
            return Files.readAllBytes(filePath)
        } catch (ex: IOException) {
            throw RuntimeException("Failed to read file", ex)
        }
    }
}