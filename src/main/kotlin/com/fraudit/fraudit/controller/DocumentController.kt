package com.fraudit.fraudit.controller

import com.fraudit.fraudit.dto.common.ApiResponse
import com.fraudit.fraudit.dto.document.*
import com.fraudit.fraudit.service.DocumentStorageService
import com.fraudit.fraudit.service.FinancialStatementService
import org.slf4j.LoggerFactory
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/api/documents")
class DocumentController(
    private val documentStorageService: DocumentStorageService,
    private val financialStatementService: FinancialStatementService
) {
    private val logger = LoggerFactory.getLogger(DocumentController::class.java)

    @GetMapping("/statement/{statementId}")
    fun getDocumentsByStatementId(@PathVariable statementId: Long): ResponseEntity<ApiResponse<List<DocumentResponse>>> {
        try {
            // Check if statement exists
            financialStatementService.findById(statementId)

            // Get documents for the statement
            val documents = documentStorageService.findByStatementId(statementId)

            val documentResponses = documents.map { document ->
                DocumentResponse(
                    id = document.id!!,
                    statementId = document.statement.id!!,
                    fileName = document.fileName,
                    fileType = document.fileType,
                    fileSize = document.fileSize,
                    uploadDate = document.uploadDate
                )
            }

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Documents retrieved successfully",
                    data = documentResponses
                )
            )
        } catch (e: Exception) {
            logger.error("Error retrieving documents for statement ID $statementId: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse(
                    success = false,
                    message = "Error retrieving documents",
                    errors = listOf(e.message ?: "Statement not found or error occurred")
                )
            )
        }
    }

    @GetMapping("/{id}")
    fun getDocumentById(@PathVariable id: Long): ResponseEntity<ApiResponse<DocumentResponse>> {
        try {
            val document = documentStorageService.findById(id)

            val documentResponse = DocumentResponse(
                id = document.id!!,
                statementId = document.statement.id!!,
                fileName = document.fileName,
                fileType = document.fileType,
                fileSize = document.fileSize,
                uploadDate = document.uploadDate
            )

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Document retrieved successfully",
                    data = documentResponse
                )
            )
        } catch (e: Exception) {
            logger.error("Error retrieving document with ID $id: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse(
                    success = false,
                    message = "Document not found",
                    errors = listOf(e.message ?: "Document not found")
                )
            )
        }
    }

    @GetMapping("/{id}/download")
    fun downloadDocument(@PathVariable id: Long): ResponseEntity<Resource> {
        try {
            val document = documentStorageService.findById(id)
            val content = documentStorageService.getDocumentContent(id)

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.fileType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${document.fileName}\"")
                .body(ByteArrayResource(content))
        } catch (e: Exception) {
            logger.error("Error downloading document with ID $id: ${e.message}", e)
            // Return an error response
            throw e
        }
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    fun uploadDocument(
        @RequestParam("statementId") statementId: Long,
        @RequestParam("file") file: MultipartFile,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<DocumentResponse>> {
        try {
            val userId = UUID.fromString(userDetails.username)

            // Check if file is empty
            if (file.isEmpty) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse(
                        success = false,
                        message = "Please select a file to upload",
                        errors = listOf("Empty file")
                    )
                )
            }

            // Check file size (10 MB limit)
            val maxFileSize = 10 * 1024 * 1024 // 10 MB in bytes
            if (file.size > maxFileSize) {
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(
                    ApiResponse(
                        success = false,
                        message = "File size exceeds the limit of 10 MB",
                        errors = listOf("File too large")
                    )
                )
            }

            // Check if statement exists and get owner information
            val statement = financialStatementService.findById(statementId)

            // Check if the user is admin or the owner of the statement
            if (statement.user.id != userId && !userDetails.authorities.any { it.authority == "ROLE_ADMIN" }) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiResponse(
                        success = false,
                        message = "You don't have permission to upload documents to this statement",
                        errors = listOf("Insufficient permissions")
                    )
                )
            }

            // Store the document
            val document = documentStorageService.storeDocument(statementId, file, userId)

            val documentResponse = DocumentResponse(
                id = document.id!!,
                statementId = document.statement.id!!,
                fileName = document.fileName,
                fileType = document.fileType,
                fileSize = document.fileSize,
                uploadDate = document.uploadDate
            )

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse(
                    success = true,
                    message = "Document uploaded successfully",
                    data = documentResponse
                )
            )
        } catch (e: Exception) {
            logger.error("Error uploading document: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse(
                    success = false,
                    message = "Error uploading document",
                    errors = listOf(e.message ?: "Error occurred while uploading")
                )
            )
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN') or @securityService.isOwner(#statementId)")
    fun deleteDocument(
        @PathVariable id: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Void>> {
        try {
            val userId = UUID.fromString(userDetails.username)

            // Get the document to check ownership
            val document = documentStorageService.findById(id)
            val statementId = document.statement.id!!

            // Check if the user is admin or the owner of the statement
            val statement = financialStatementService.findById(statementId)
            if (statement.user.id != userId && !userDetails.authorities.any { it.authority == "ROLE_ADMIN" }) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiResponse(
                        success = false,
                        message = "You don't have permission to delete this document",
                        errors = listOf("Insufficient permissions")
                    )
                )
            }

            // Delete the document
            documentStorageService.deleteDocument(id, userId)

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Document deleted successfully"
                )
            )
        } catch (e: Exception) {
            logger.error("Error deleting document with ID $id: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse(
                    success = false,
                    message = "Error deleting document",
                    errors = listOf(e.message ?: "Error occurred while deleting")
                )
            )
        }
    }
}