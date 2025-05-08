package com.fraudit.fraudit.domain.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDate
import java.time.OffsetDateTime

// Company Entity
@Entity
@Table(name = "companies")
class Company(
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
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Company) return false
        if (id != null && other.id != null) return id == other.id
        return false
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "Company(id=$id, name='$name', stockCode='$stockCode')"
    }

    fun copy(
        id: Long? = this.id,
        name: String = this.name,
        stockCode: String = this.stockCode,
        sector: String? = this.sector,
        listingDate: LocalDate? = this.listingDate,
        description: String? = this.description,
        createdAt: OffsetDateTime? = this.createdAt,
        updatedAt: OffsetDateTime? = this.updatedAt,
        fiscalYears: MutableSet<FiscalYear> = this.fiscalYears
    ): Company {
        return Company(
            id = id,
            name = name,
            stockCode = stockCode,
            sector = sector,
            listingDate = listingDate,
            description = description,
            createdAt = createdAt,
            updatedAt = updatedAt,
            fiscalYears = fiscalYears
        )
    }
}