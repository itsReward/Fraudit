package com.fraudit.fraudit.dto.document

import java.time.OffsetDateTime
import java.util.UUID

/**
 * Response DTO for document details
 */
data class DocumentResponse(
    val id: Long,
    val statementId: Long,
    val fileName: String,
    val fileType: String,
    val fileSize: Int,
    val uploadDate: OffsetDateTime
)

/**
 * Extended response DTO for document details with additional information
 */
data class DocumentDetailResponse(
    val id: Long,
    val statementId: Long,
    val companyId: Long,
    val companyName: String,
    val fiscalYear: Int,
    val fileName: String,
    val fileType: String,
    val fileSize: Int,
    val uploadDate: OffsetDateTime,
    val uploadedByUserId: UUID?,
    val uploadedByUsername: String?
)

/**
 * Request DTO for document upload
 */
data class DocumentUploadRequest(
    val statementId: Long
    // The file will be uploaded as multipart/form-data
)

/**
 * Response DTO for document statistics
 */
data class DocumentStatisticsResponse(
    val totalDocuments: Int,
    val totalSize: Long,
    val documentsByType: Map<String, Int>,
    val recentUploads: List<DocumentResponse>
)

/**
 * Request DTO for document search
 */
data class DocumentSearchRequest(
    val fileName: String? = null,
    val fileType: String? = null,
    val statementId: Long? = null,
    val companyId: Long? = null,
    val uploadDateFrom: OffsetDateTime? = null,
    val uploadDateTo: OffsetDateTime? = null,
    val page: Int = 0,
    val size: Int = 10,
    val sortBy: String = "uploadDate",
    val sortDirection: String = "DESC"
)