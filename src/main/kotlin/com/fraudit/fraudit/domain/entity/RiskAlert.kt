package com.fraudit.fraudit.domain.entity

import com.fraudit.fraudit.domain.enum.AlertSeverity
import jakarta.persistence.*
import java.time.OffsetDateTime


// Risk Alerts Entity
@Entity
@Table(name = "risk_alerts")
data class RiskAlert(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_id")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id", nullable = false)
    val assessment: FraudRiskAssessment,

    @Column(name = "alert_type", nullable = false)
    val alertType: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    val severity: AlertSeverity,

    @Column(name = "message", nullable = false)
    val message: String,

    @Column(name = "created_at", updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "is_resolved", nullable = false)
    val isResolved: Boolean = false,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    val resolvedBy: User? = null,

    @Column(name = "resolved_at")
    val resolvedAt: OffsetDateTime? = null,

    @Column(name = "resolution_notes")
    val resolutionNotes: String? = null

)