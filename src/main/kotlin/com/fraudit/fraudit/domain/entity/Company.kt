package com.fraudit.fraudit.domain.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDate
import java.time.OffsetDateTime

// Company Entity
@Entity
@Table(name = "companies")
data class Company(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "company_id")
    val id: Long? = null,

    @Column(name = "company_name", nullable = false, unique = true)
    val name: String,

    @Column(name = "stock_code", nullable = false, unique = true)
    val stockCode: String,

    @Column(name = "sector")
    val sector: String? = null,

    @Column(name = "listing_date")
    val listingDate: LocalDate? = null,

    @Column(name = "description")
    val description: String? = null,

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    val createdAt: OffsetDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updated_at")
    val updatedAt: OffsetDateTime? = null,

    @OneToMany(mappedBy = "company", cascade = [CascadeType.ALL], orphanRemoval = true)
    val fiscalYears: MutableSet<FiscalYear> = mutableSetOf()
)
