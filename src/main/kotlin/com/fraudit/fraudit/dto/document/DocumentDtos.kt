package com.fraudit.fraudit.dto.document

import java.time.OffsetDateTime

data class DocumentResponse(
    val id: Long,
    val statementId: Long,
    val fileName: String,
    val fileType: String,
    val fileSize: Int,
    val uploadDate: OffsetDateTime
)

data class DocumentUploadRequest(
    val statementId: Long
    // The file will be uploaded as multipart/form-data
)