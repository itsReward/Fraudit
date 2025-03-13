package com.fraudit.fraudit.service

import com.fraudit.fraudit.domain.entity.DocumentStorage
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

interface DocumentStorageService {
    fun findAll(): List<DocumentStorage>
    fun findById(id: Long): DocumentStorage
    fun findByStatementId(statementId: Long): List<DocumentStorage>
    fun findByFileName(fileName: String): List<DocumentStorage>
    fun findByFileType(fileType: String): List<DocumentStorage>
    fun storeDocument(statementId: Long, file: MultipartFile, userId: UUID): DocumentStorage
    fun deleteDocument(id: Long, userId: UUID)
    fun getDocumentContent(id: Long): ByteArray
}