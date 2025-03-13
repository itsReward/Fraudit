package com.fraudit.fraudit.dto.assessment

import com.fraudit.fraudit.domain.enum.ManipulationProbability
import java.math.BigDecimal
import java.time.OffsetDateTime

data class BeneishMScoreResponse(
    val id: Long,
    val statementId: Long,
    val companyId: Long,
    val companyName: String,
    val year: Int,
    val daysSalesReceivablesIndex: BigDecimal?,
    val grossMarginIndex: BigDecimal?,
    val assetQualityIndex: BigDecimal?,
    val salesGrowthIndex: BigDecimal?,
    val depreciationIndex: BigDecimal?,
    val sgAdminExpensesIndex: BigDecimal?,
    val leverageIndex: BigDecimal?,
    val totalAccrualsToTotalAssets: BigDecimal?,
    val mScore: BigDecimal?,
    val manipulationProbability: ManipulationProbability?,
    val calculatedAt: OffsetDateTime
)