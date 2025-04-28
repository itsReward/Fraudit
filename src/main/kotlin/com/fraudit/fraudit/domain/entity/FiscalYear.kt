package com.fraudit.fraudit.domain.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDate
import java.time.OffsetDateTime

// Fiscal Year Entity
@Entity
@Table(name = "fiscal_years")
class FiscalYear(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fiscal_year_id")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    val company: Company,

    @Column(name = "year", nullable = false)
    val year: Int,

    @Column(name = "start_date", nullable = false)
    val startDate: LocalDate,

    @Column(name = "end_date", nullable = false)
    val endDate: LocalDate,

    @Column(name = "is_audited")
    val isAudited: Boolean = false,

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    val createdAt: OffsetDateTime? = null,

    @OneToMany(mappedBy = "fiscalYear", cascade = [CascadeType.ALL], orphanRemoval = true)
    val financialStatements: MutableSet<FinancialStatement> = mutableSetOf()
){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FiscalYear) return false
        if (id != null && other.id != null) return id == other.id
        return false
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "FiscalYear(id=$id, year=$year)"
    }

    fun copy(
        id: Long? = this.id,
        company: Company = this.company,
        year: Int = this.year,
        startDate: LocalDate = this.startDate,
        endDate: LocalDate = this.endDate,
        isAudited: Boolean = this.isAudited,
        createdAt: OffsetDateTime? = this.createdAt,
        financialStatements: MutableSet<FinancialStatement> = this.financialStatements
    ): FiscalYear {
        return FiscalYear(
            id = id,
            company = company,
            year = year,
            startDate = startDate,
            endDate = endDate,
            isAudited = isAudited,
            createdAt = createdAt,
            financialStatements = financialStatements
        )
    }

}

