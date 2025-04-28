package com.fraudit.fraudit.repository

import com.fraudit.fraudit.domain.entity.DocumentStorage
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DocumentStorageRepository : JpaRepository<DocumentStorage, Long> {
    fun findByStatementId(statementId: Long): List<DocumentStorage>
    fun findByFileName(fileName: String): List<DocumentStorage>
    fun findByFileType(fileType: String): List<DocumentStorage>

    /**
     * Find all documents with pagination and optional filtering
     */
    override fun findAll(pageable: Pageable): Page<DocumentStorage>

    /**
     * Find documents by statement ID with pagination
     */
    fun findByStatementId(statementId: Long, pageable: Pageable): Page<DocumentStorage>

    /**
     * Find documents by file type with pagination
     */
    fun findByFileType(fileType: String, pageable: Pageable): Page<DocumentStorage>
}