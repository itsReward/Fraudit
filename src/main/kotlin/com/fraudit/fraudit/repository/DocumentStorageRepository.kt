package com.fraudit.fraudit.repository

import com.fraudit.fraudit.domain.entity.DocumentStorage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DocumentStorageRepository : JpaRepository<DocumentStorage, Long> {
    fun findByStatementId(statementId: Long): List<DocumentStorage>
    fun findByFileName(fileName: String): List<DocumentStorage>
    fun findByFileType(fileType: String): List<DocumentStorage>
}