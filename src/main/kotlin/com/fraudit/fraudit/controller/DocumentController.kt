package com.fraudit.fraudit.controller

import com.fraudit.fraudit.dto.common.ApiResponse
import com.fraudit.fraudit.dto.document.*
import com.fraudit.fraudit.service.DocumentStorageService
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/documents")
class DocumentController(private val documentStorageService: DocumentStorageService) {

    @GetMapping("/statement/{statementId}")
    fun getDocumentsByStatementId(@PathVariable statementId: Long): ResponseEntity<ApiResponse<List<DocumentResponse>>> {
        // Implementation for getting all documents for a specific statement
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Documents retrieved successfully",
                data = listOf() // Replace with actual document data
            )
        )
    }

    @GetMapping("/{id}")
    fun getDocumentById(@PathVariable id: Long): ResponseEntity<ApiResponse<DocumentResponse>> {
        // Implementation for getting document metadata by ID
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Document retrieved successfully",
                data = null // Replace with actual document data
            )
        )
    }

    @GetMapping("/{id}/download")
    fun downloadDocument(@PathVariable id: Long): ResponseEntity<Resource> {
        // Implementation for downloading a document
        val document = DocumentResponse(id = id, statementId = 1, fileName = "sample.pdf", fileType = "application/pdf", fileSize = 1024, uploadDate = java.time.OffsetDateTime.now())
        val content = ByteArray(0) // Replace with actual document content

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(document.fileType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${document.fileName}\"")
            .body(ByteArrayResource(content))
    }

    @PostMapping("/upload")
    fun uploadDocument(
        @RequestParam("statementId") statementId: Long,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<ApiResponse<DocumentResponse>> {
        // Implementation for uploading a document
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse(
                success = true,
                message = "Document uploaded successfully",
                data = null // Replace with uploaded document data
            )
        )
    }

    @DeleteMapping("/{id}")
    fun deleteDocument(@PathVariable id: Long): ResponseEntity<ApiResponse<Void>> {
        // Implementation for deleting a document
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Document deleted successfully"
            )
        )
    }
}