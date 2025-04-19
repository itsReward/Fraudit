package com.fraudit.fraudit.service.impl

import com.fraudit.fraudit.domain.entity.DocumentStorage
import com.fraudit.fraudit.repository.DocumentStorageRepository
import com.fraudit.fraudit.repository.FinancialStatementRepository
import com.fraudit.fraudit.service.AuditLogService
import com.fraudit.fraudit.service.DocumentStorageService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
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
    private val logger = LoggerFactory.getLogger(DocumentStorageServiceImpl::class.java)

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
            // Get the financial statement
            val statement = financialStatementRepository.findById(statementId)
                .orElseThrow { EntityNotFoundException("Financial statement not found with id: $statementId") }

            // Create upload directory if it doesn't exist
            val uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize()
            Files.createDirectories(uploadPath)

            // Generate a unique filename with timestamp to prevent collisions
            val timestamp = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            val originalFilename = file.originalFilename ?: "unknown-file"
            val fileExtension = originalFilename.substringAfterLast(".", "")
            val sanitizedFilename = sanitizeFilename(originalFilename.substringBeforeLast("."))
            val fileName = "${statement.fiscalYear.company.stockCode}_${statement.fiscalYear.year}_${sanitizedFilename}_$timestamp.$fileExtension"

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

            // Log the document upload
            auditLogService.logEvent(
                userId = userId,
                action = "UPLOAD",
                entityType = "DOCUMENT",
                entityId = savedDocument.id.toString(),
                details = "Uploaded document: ${originalFilename} for statement id: $statementId"
            )

            return savedDocument
        } catch (ex: IOException) {
            logger.error("Failed to store file", ex)
            throw RuntimeException("Failed to store file: ${ex.message}", ex)
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

            // Log the document deletion
            auditLogService.logEvent(
                userId = userId,
                action = "DELETE",
                entityType = "DOCUMENT",
                entityId = id.toString(),
                details = "Deleted document: ${document.fileName} from statement id: ${document.statement.id}"
            )
        } catch (ex: IOException) {
            logger.error("Failed to delete file", ex)
            throw RuntimeException("Failed to delete file: ${ex.message}", ex)
        }
    }

    override fun getDocumentContent(id: Long): ByteArray {
        val document = findById(id)

        try {
            val filePath = Path.of(document.filePath)
            if (!Files.exists(filePath)) {
                throw IOException("File not found: ${document.filePath}")
            }
            return Files.readAllBytes(filePath)
        } catch (ex: IOException) {
            logger.error("Failed to read file", ex)
            throw RuntimeException("Failed to read file: ${ex.message}", ex)
        }
    }

    /**
     * Sanitize filename to prevent path traversal attacks and ensure valid filenames
     */
    private fun sanitizeFilename(filename: String): String {
        // Replace any character that isn't alphanumeric, a dash, or an underscore with an underscore
        val sanitized = filename.replace(Regex("[^a-zA-Z0-9\\-_]"), "_")
        // Limit length to avoid extremely long filenames
        return if (sanitized.length > 100) sanitized.substring(0, 100) else sanitized
    }
}