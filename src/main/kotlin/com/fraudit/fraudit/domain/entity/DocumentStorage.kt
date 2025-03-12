package com.fraudit.fraudit.domain.entity

import jakarta.persistence.*
import java.time.OffsetDateTime


// Document Storage Entity
@Entity
@Table(name = "document_storage")
data class DocumentStorage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statement_id", nullable = false)
    val statement: FinancialStatement,

    @Column(name = "file_name", nullable = false)
    val fileName: String,

    @Column(name = "file_type", nullable = false)
    val fileType: String,

    @Column(name = "file_size", nullable = false)
    val fileSize: Int,

    @Column(name = "file_path", nullable = false)
    val filePath: String,

    @Column(name = "upload_date", updatable = false)
    val uploadDate: OffsetDateTime = OffsetDateTime.now()
)
