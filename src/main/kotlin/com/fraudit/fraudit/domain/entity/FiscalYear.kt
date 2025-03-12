package com.fraudit.fraudit.domain.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDate
import java.time.OffsetDateTime

// Fiscal Year Entity
@Entity
@Table(name = "fiscal_years")
data class FiscalYear(
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
)
