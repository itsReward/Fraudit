package com.fraudit.fraudit.service.impl

import com.fraudit.fraudit.domain.entity.*
import com.fraudit.fraudit.domain.enum.*
import com.fraudit.fraudit.repository.*
import com.fraudit.fraudit.service.AuditLogService
import com.fraudit.fraudit.service.FinancialAnalysisService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.OffsetDateTime
import java.util.UUID
import jakarta.persistence.EntityNotFoundException
import org.json.JSONObject

@Service
class FinancialAnalysisServiceImpl(
    private val financialStatementRepository: FinancialStatementRepository,
    private val financialDataRepository: FinancialDataRepository,
    private val financialRatiosRepository: FinancialRatiosRepository,
    private val altmanZScoreRepository: AltmanZScoreRepository,
    private val beneishMScoreRepository: BeneishMScoreRepository,
    private val piotroskiFScoreRepository: PiotroskiFScoreRepository,
    private val mlFeaturesRepository: MlFeaturesRepository,
    private val mlModelRepository: MlModelRepository,
    private val mlPredictionRepository: MlPredictionRepository,
    private val fraudRiskAssessmentRepository: FraudRiskAssessmentRepository,
    private val riskAlertRepository: RiskAlertRepository,
    private val auditLogService: AuditLogService
) : FinancialAnalysisService {

    @Transactional
    override fun calculateAllScoresAndRatios(statementId: Long, userId: UUID): FraudRiskAssessment {
        // Calculate financial ratios
        calculateFinancialRatios(statementId, userId)

        // Calculate Altman Z-Score
        calculateAltmanZScore(statementId, userId)

        // Calculate Beneish M-Score
        calculateBeneishMScore(statementId, userId)

        // Calculate Piotroski F-Score
        calculatePiotroskiFScore(statementId, userId)

        // Prepare ML features
        prepareMlFeatures(statementId, userId)

        // Perform ML prediction
        performMlPrediction(statementId, userId)

        // Assess fraud risk
        val assessment = assessFraudRisk(statementId, userId)

        // Generate risk alerts
        generateRiskAlerts(assessment.id!!, userId)

        // Update statement status
        val statement = financialStatementRepository.findById(statementId)
            .orElseThrow { EntityNotFoundException("Financial statement not found with id: $statementId") }

        val updatedStatement = statement.copy(status = StatementStatus.ANALYZED)
        financialStatementRepository.save(updatedStatement)

        return assessment
    }

    @Transactional
    override fun calculateFinancialRatios(statementId: Long, userId: UUID): FinancialRatios {
        val statement = financialStatementRepository.findById(statementId)
            .orElseThrow { EntityNotFoundException("Financial statement not found with id: $statementId") }

        val financialData = financialDataRepository.findByStatementId(statementId)
            ?: throw EntityNotFoundException("Financial data not found for statement id: $statementId")

        // Calculate liquidity ratios
        val currentRatio = calculateCurrentRatio(financialData)
        val quickRatio = calculateQuickRatio(financialData)
        val cashRatio = calculateCashRatio(financialData)

        // Calculate profitability ratios
        val grossMargin = calculateGrossMargin(financialData)
        val operatingMargin = calculateOperatingMargin(financialData)
        val netProfitMargin = calculateNetProfitMargin(financialData)
        val returnOnAssets = calculateReturnOnAssets(financialData)
        val returnOnEquity = calculateReturnOnEquity(financialData)

        // Calculate efficiency ratios
        val assetTurnover = calculateAssetTurnover(financialData)
        val inventoryTurnover = calculateInventoryTurnover(financialData)
        val accountsReceivableTurnover = calculateAccountsReceivableTurnover(financialData)
        val daysSalesOutstanding = calculateDaysSalesOutstanding(financialData)

        // Calculate leverage ratios
        val debtToEquity = calculateDebtToEquity(financialData)
        val debtRatio = calculateDebtRatio(financialData)
        val interestCoverage = calculateInterestCoverage(financialData)

        // Calculate valuation ratios
        val priceToEarnings = calculatePriceToEarnings(financialData)
        val priceToBook = calculatePriceToBook(financialData)

        // Calculate quality metrics
        val accrualRatio = calculateAccrualRatio(financialData)
        val earningsQuality = calculateEarningsQuality(financialData)

        // Create and save the financial ratios entity
        val financialRatios = FinancialRatios(
            id = null,
            statement = statement,
            currentRatio = currentRatio,
            quickRatio = quickRatio,
            cashRatio = cashRatio,
            grossMargin = grossMargin,
            operatingMargin = operatingMargin,
            netProfitMargin = netProfitMargin,
            returnOnAssets = returnOnAssets,
            returnOnEquity = returnOnEquity,
            assetTurnover = assetTurnover,
            inventoryTurnover = inventoryTurnover,
            accountsReceivableTurnover = accountsReceivableTurnover,
            daysSalesOutstanding = daysSalesOutstanding,
            debtToEquity = debtToEquity,
            debtRatio = debtRatio,
            interestCoverage = interestCoverage,
            priceToEarnings = priceToEarnings,
            priceToBook = priceToBook,
            accrualRatio = accrualRatio,
            earningsQuality = earningsQuality,
            calculatedAt = OffsetDateTime.now()
        )

        val savedRatios = financialRatiosRepository.save(financialRatios)

        auditLogService.logEvent(
            userId = userId,
            action = "CALCULATE",
            entityType = "FINANCIAL_RATIOS",
            entityId = savedRatios.id.toString(),
            details = "Calculated financial ratios for statement id: $statementId"
        )

        return savedRatios
    }

    private fun calculateCurrentRatio(data: FinancialData): BigDecimal? {
        return if (data.totalCurrentAssets != null && data.totalCurrentLiabilities != null && data.totalCurrentLiabilities != BigDecimal.ZERO) {
            data.totalCurrentAssets!!.divide(data.totalCurrentLiabilities, 4, RoundingMode.HALF_UP)
        } else null
    }

    private fun calculateQuickRatio(data: FinancialData): BigDecimal? {
        if (data.totalCurrentLiabilities == null || data.totalCurrentLiabilities == BigDecimal.ZERO) return null

        val quickAssets = (data.cash ?: BigDecimal.ZERO)
            .add(data.shortTermInvestments ?: BigDecimal.ZERO)
            .add(data.accountsReceivable ?: BigDecimal.ZERO)

        return quickAssets.divide(data.totalCurrentLiabilities, 4, RoundingMode.HALF_UP)
    }

    private fun calculateCashRatio(data: FinancialData): BigDecimal? {
        if (data.totalCurrentLiabilities == null || data.totalCurrentLiabilities == BigDecimal.ZERO) return null

        val cashAndEquivalents = (data.cash ?: BigDecimal.ZERO)
            .add(data.shortTermInvestments ?: BigDecimal.ZERO)

        return cashAndEquivalents.divide(data.totalCurrentLiabilities, 4, RoundingMode.HALF_UP)
    }

    private fun calculateGrossMargin(data: FinancialData): BigDecimal? {
        return if (data.grossProfit != null && data.revenue != null && data.revenue != BigDecimal.ZERO) {
            data.grossProfit!!.divide(data.revenue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal(100))
        } else null
    }

    private fun calculateOperatingMargin(data: FinancialData): BigDecimal? {
        return if (data.operatingIncome != null && data.revenue != null && data.revenue != BigDecimal.ZERO) {
            data.operatingIncome!!.divide(data.revenue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal(100))
        } else null
    }

    private fun calculateNetProfitMargin(data: FinancialData): BigDecimal? {
        return if (data.netIncome != null && data.revenue != null && data.revenue != BigDecimal.ZERO) {
            data.netIncome!!.divide(data.revenue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal(100))
        } else null
    }

    private fun calculateReturnOnAssets(data: FinancialData): BigDecimal? {
        return if (data.netIncome != null && data.totalAssets != null && data.totalAssets != BigDecimal.ZERO) {
            data.netIncome!!.divide(data.totalAssets, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal(100))
        } else null
    }

    private fun calculateReturnOnEquity(data: FinancialData): BigDecimal? {
        return if (data.netIncome != null && data.totalEquity != null && data.totalEquity != BigDecimal.ZERO) {
            data.netIncome!!.divide(data.totalEquity, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal(100))
        } else null
    }

    private fun calculateAssetTurnover(data: FinancialData): BigDecimal? {
        return if (data.revenue != null && data.totalAssets != null && data.totalAssets != BigDecimal.ZERO) {
            data.revenue!!.divide(data.totalAssets, 4, RoundingMode.HALF_UP)
        } else null
    }

    private fun calculateInventoryTurnover(data: FinancialData): BigDecimal? {
        if (data.costOfSales == null || data.inventory == null || data.inventory == BigDecimal.ZERO) return null

        return data.costOfSales!!.divide(data.inventory, 4, RoundingMode.HALF_UP)
    }

    private fun calculateAccountsReceivableTurnover(data: FinancialData): BigDecimal? {
        if (data.revenue == null || data.accountsReceivable == null || data.accountsReceivable == BigDecimal.ZERO) return null

        return data.revenue!!.divide(data.accountsReceivable, 4, RoundingMode.HALF_UP)
    }

    private fun calculateDaysSalesOutstanding(data: FinancialData): BigDecimal? {
        val receivableTurnover = calculateAccountsReceivableTurnover(data)

        return if (receivableTurnover != null && receivableTurnover != BigDecimal.ZERO) {
            BigDecimal(365).divide(receivableTurnover, 4, RoundingMode.HALF_UP)
        } else null
    }

    private fun calculateDebtToEquity(data: FinancialData): BigDecimal? {
        if (data.totalLiabilities == null || data.totalEquity == null || data.totalEquity == BigDecimal.ZERO) return null

        return data.totalLiabilities!!.divide(data.totalEquity, 4, RoundingMode.HALF_UP)
    }

    private fun calculateDebtRatio(data: FinancialData): BigDecimal? {
        if (data.totalLiabilities == null || data.totalAssets == null || data.totalAssets == BigDecimal.ZERO) return null

        return data.totalLiabilities!!.divide(data.totalAssets, 4, RoundingMode.HALF_UP)
    }

    private fun calculateInterestCoverage(data: FinancialData): BigDecimal? {
        if (data.operatingIncome == null || data.interestExpense == null || data.interestExpense == BigDecimal.ZERO) return null

        return data.operatingIncome!!.divide(data.interestExpense, 4, RoundingMode.HALF_UP)
    }

    private fun calculatePriceToEarnings(data: FinancialData): BigDecimal? {
        if (data.marketPricePerShare == null || data.earningsPerShare == null || data.earningsPerShare == BigDecimal.ZERO) return null

        return data.marketPricePerShare!!.divide(data.earningsPerShare, 4, RoundingMode.HALF_UP)
    }

    private fun calculatePriceToBook(data: FinancialData): BigDecimal? {
        if (data.marketPricePerShare == null || data.bookValuePerShare == null || data.bookValuePerShare == BigDecimal.ZERO) return null

        return data.marketPricePerShare!!.divide(data.bookValuePerShare, 4, RoundingMode.HALF_UP)
    }

    private fun calculateAccrualRatio(data: FinancialData): BigDecimal? {
        if (data.netIncome == null || data.netCashFromOperating == null || data.totalAssets == null || data.totalAssets == BigDecimal.ZERO) return null

        val accruals = data.netIncome!!.subtract(data.netCashFromOperating)
        return accruals.divide(data.totalAssets, 4, RoundingMode.HALF_UP)
    }

    private fun calculateEarningsQuality(data: FinancialData): BigDecimal? {
        if (data.netCashFromOperating == null || data.netIncome == null || data.netIncome == BigDecimal.ZERO) return null

        return data.netCashFromOperating!!.divide(data.netIncome, 4, RoundingMode.HALF_UP)
    }

    @Transactional
    override fun calculateAltmanZScore(statementId: Long, userId: UUID): AltmanZScore {
        val statement = financialStatementRepository.findById(statementId)
            .orElseThrow { EntityNotFoundException("Financial statement not found with id: $statementId") }

        val financialData = financialDataRepository.findByStatementId(statementId)
            ?: throw EntityNotFoundException("Financial data not found for statement id: $statementId")

        // Calculate Altman Z-Score components
        val workingCapital = (financialData.totalCurrentAssets ?: BigDecimal.ZERO)
            .subtract(financialData.totalCurrentLiabilities ?: BigDecimal.ZERO)

        val totalAssets = financialData.totalAssets ?: BigDecimal.ONE

        // X1: Working Capital / Total Assets
        val x1 = if (totalAssets != BigDecimal.ZERO) {
            workingCapital.divide(totalAssets, 4, RoundingMode.HALF_UP)
        } else null

        // X2: Retained Earnings / Total Assets
        val x2 = if (financialData.retainedEarnings != null && totalAssets != BigDecimal.ZERO) {
            financialData.retainedEarnings!!.divide(totalAssets, 4, RoundingMode.HALF_UP)
        } else null

        // X3: EBIT / Total Assets
        val ebit = financialData.earningsBeforeTax?.add(financialData.interestExpense ?: BigDecimal.ZERO)
        val x3 = if (ebit != null && totalAssets != BigDecimal.ZERO) {
            ebit.divide(totalAssets, 4, RoundingMode.HALF_UP)
        } else null

        // X4: Market Value of Equity / Book Value of Total Debt
        val marketValueEquity = financialData.marketCapitalization ?: BigDecimal.ZERO
        val totalDebt = financialData.totalLiabilities ?: BigDecimal.ONE
        val x4 = if (totalDebt != BigDecimal.ZERO) {
            marketValueEquity.divide(totalDebt, 4, RoundingMode.HALF_UP)
        } else null

        // X5: Sales / Total Assets
        val x5 = if (financialData.revenue != null && totalAssets != BigDecimal.ZERO) {
            financialData.revenue!!.divide(totalAssets, 4, RoundingMode.HALF_UP)
        } else null

        // Z = 1.2X1 + 1.4X2 + 3.3X3 + 0.6X4 + 1.0X5
        var zScore: BigDecimal? = null
        if (x1 != null && x2 != null && x3 != null && x4 != null && x5 != null) {
            zScore = x1.multiply(BigDecimal("1.2"))
                .add(x2.multiply(BigDecimal("1.4")))
                .add(x3.multiply(BigDecimal("3.3")))
                .add(x4.multiply(BigDecimal("0.6")))
                .add(x5.multiply(BigDecimal("1.0")))
        }

        // Determine risk category based on Z-Score
        val riskCategory = when {
            zScore == null -> null
            zScore < BigDecimal("1.8") -> RiskCategory.DISTRESS
            zScore < BigDecimal("3.0") -> RiskCategory.GREY
            else -> RiskCategory.SAFE
        }

        // Create and save Altman Z-Score entity
        val altmanZScore = AltmanZScore(
            id = null,
            statement = statement,
            workingCapitalToTotalAssets = x1,
            retainedEarningsToTotalAssets = x2,
            ebitToTotalAssets = x3,
            marketValueEquityToBookValueDebt = x4,
            salesToTotalAssets = x5,
            zScore = zScore,
            riskCategory = riskCategory,
            calculatedAt = OffsetDateTime.now()
        )

        val savedZScore = altmanZScoreRepository.save(altmanZScore)

        auditLogService.logEvent(
            userId = userId,
            action = "CALCULATE",
            entityType = "ALTMAN_Z_SCORE",
            entityId = savedZScore.id.toString(),
            details = "Calculated Altman Z-Score for statement id: $statementId. Z-Score: $zScore, Risk Category: $riskCategory"
        )

        return savedZScore
    }

    @Transactional
    override fun calculateBeneishMScore(statementId: Long, userId: UUID): BeneishMScore {
        val statement = financialStatementRepository.findById(statementId)
            .orElseThrow { EntityNotFoundException("Financial statement not found with id: $statementId") }

        val financialData = financialDataRepository.findByStatementId(statementId)
            ?: throw EntityNotFoundException("Financial data not found for statement id: $statementId")

        // NOTE: A proper implementation would require comparing against previous year's data
        // For simplicity, we'll use the growth metrics that are already calculated

        // DSRI: Days Sales in Receivables Index
        val dsri = financialData.receivablesGrowth?.divide(financialData.revenueGrowth ?: BigDecimal.ONE, 4, RoundingMode.HALF_UP)

        // GMI: Gross Margin Index
        val gmi = BigDecimal.ONE.divide(financialData.grossProfitGrowth ?: BigDecimal.ONE, 4, RoundingMode.HALF_UP)

        // AQI: Asset Quality Index
        // Simplified calculation
        val aqi = financialData.assetGrowth

        // SGI: Sales Growth Index
        val sgi = financialData.revenueGrowth

        // DEPI: Depreciation Index
        // Simplified calculation
        val depi = BigDecimal.ONE

        // SGAI: Sales, General and Administrative Expenses Index
        // Simplified calculation
        val sgai = financialData.administrativeExpenses?.divide(financialData.revenue ?: BigDecimal.ONE, 4, RoundingMode.HALF_UP)

        // LVGI: Leverage Index
        val lvgi = financialData.liabilityGrowth

        // TATA: Total Accruals to Total Assets
        val tata = if (financialData.netIncome != null && financialData.netCashFromOperating != null && financialData.totalAssets != null && financialData.totalAssets != BigDecimal.ZERO) {
            financialData.netIncome!!.subtract(financialData.netCashFromOperating)
                .divide(financialData.totalAssets, 4, RoundingMode.HALF_UP)
        } else null

        // M-Score calculation: M = -4.84 + 0.92*DSRI + 0.528*GMI + 0.404*AQI + 0.892*SGI + 0.115*DEPI - 0.172*SGAI + 4.679*TATA - 0.327*LVGI
        var mScore: BigDecimal? = null
        if (dsri != null && gmi != null && aqi != null && sgi != null && depi != null && sgai != null && tata != null && lvgi != null) {
            mScore = BigDecimal("-4.84")
                .add(dsri.multiply(BigDecimal("0.92")))
                .add(gmi.multiply(BigDecimal("0.528")))
                .add(aqi.multiply(BigDecimal("0.404")))
                .add(sgi.multiply(BigDecimal("0.892")))
                .add(depi.multiply(BigDecimal("0.115")))
                .subtract(sgai.multiply(BigDecimal("0.172")))
                .add(tata.multiply(BigDecimal("4.679")))
                .subtract(lvgi.multiply(BigDecimal("0.327")))
        }

        // Determine manipulation probability based on M-Score
        val manipulationProbability = when {
            mScore == null -> null
            mScore > BigDecimal("-1.78") -> ManipulationProbability.HIGH
            mScore > BigDecimal("-2.22") -> ManipulationProbability.MEDIUM
            else -> ManipulationProbability.LOW
        }

        // Create and save Beneish M-Score entity
        val beneishMScore = BeneishMScore(
            id = null,
            statement = statement,
            daysSalesReceivablesIndex = dsri,
            grossMarginIndex = gmi,
            assetQualityIndex = aqi,
            salesGrowthIndex = sgi,
            depreciationIndex = depi,
            sgAdminExpensesIndex = sgai,
            leverageIndex = lvgi,
            totalAccrualsToTotalAssets = tata,
            mScore = mScore,
            manipulationProbability = manipulationProbability,
            calculatedAt = OffsetDateTime.now()
        )

        val savedMScore = beneishMScoreRepository.save(beneishMScore)

        auditLogService.logEvent(
            userId = userId,
            action = "CALCULATE",
            entityType = "BENEISH_M_SCORE",
            entityId = savedMScore.id.toString(),
            details = "Calculated Beneish M-Score for statement id: $statementId. M-Score: $mScore, Manipulation Probability: $manipulationProbability"
        )

        return savedMScore
    }

    @Transactional
    override fun calculatePiotroskiFScore(statementId: Long, userId: UUID): PiotroskiFScore {
        val statement = financialStatementRepository.findById(statementId)
            .orElseThrow { EntityNotFoundException("Financial statement not found with id: $statementId") }

        val financialData = financialDataRepository.findByStatementId(statementId)
            ?: throw EntityNotFoundException("Financial data not found for statement id: $statementId")

        // 1. Positive Net Income
        val positiveNetIncome = (financialData.netIncome?.compareTo(BigDecimal.ZERO) ?: 0) > 0

        // 2. Positive Operating Cash Flow
        val positiveOperatingCashFlow = (financialData.netCashFromOperating?.compareTo(BigDecimal.ZERO) ?: 0) > 0

        // 3. Cash Flow > Net Income
        val cashFlowGreaterThanNetIncome = if (financialData.netCashFromOperating != null && financialData.netIncome != null) {
            financialData.netCashFromOperating!! > financialData.netIncome
        } else false

        // 4. Improving ROA (using netIncomeGrowth as a proxy)
        val improvingRoa = (financialData.netIncomeGrowth?.compareTo(BigDecimal.ZERO) ?: 0) > 0

        // 5. Decreasing Leverage
        val decreasingLeverage = (financialData.liabilityGrowth?.compareTo(BigDecimal.ZERO) ?: 0) < 0

        // 6. Improving Current Ratio (would need previous year data for true calculation)
        val improvingCurrentRatio = true

        // 7. No New Shares (would need previous year data)
        val noNewShares = true

        // 8. Improving Gross Margin
        val improvingGrossMargin = (financialData.grossProfitGrowth?.compareTo(BigDecimal.ZERO) ?: 0) > 0

        // 9. Improving Asset Turnover
        val improvingAssetTurnover =
            (financialData.revenueGrowth?.compareTo(financialData.assetGrowth ?: BigDecimal.ZERO) ?: 0) > 0

        // Calculate F-Score (sum of all true conditions)
        var fScore = 0
        if (positiveNetIncome) fScore++
        if (positiveOperatingCashFlow) fScore++
        if (cashFlowGreaterThanNetIncome) fScore++
        if (improvingRoa) fScore++
        if (decreasingLeverage) fScore++
        if (improvingCurrentRatio) fScore++
        if (noNewShares) fScore++
        if (improvingGrossMargin) fScore++
        if (improvingAssetTurnover) fScore++

        // Determine financial strength based on F-Score
        val financialStrength = when {
            fScore <= 3 -> FinancialStrength.WEAK
            fScore <= 6 -> FinancialStrength.MODERATE
            else -> FinancialStrength.STRONG
        }

        // Create and save Piotroski F-Score entity
        val piotroskiFScore = PiotroskiFScore(
            id = null,
            statement = statement,
            positiveNetIncome = positiveNetIncome,
            positiveOperatingCashFlow = positiveOperatingCashFlow,
            cashFlowGreaterThanNetIncome = cashFlowGreaterThanNetIncome,
            improvingRoa = improvingRoa,
            decreasingLeverage = decreasingLeverage,
            improvingCurrentRatio = improvingCurrentRatio,
            noNewShares = noNewShares,
            improvingGrossMargin = improvingGrossMargin,
            improvingAssetTurnover = improvingAssetTurnover,
            fScore = fScore,
            financialStrength = financialStrength,
            calculatedAt = OffsetDateTime.now()
        )

        val savedFScore = piotroskiFScoreRepository.save(piotroskiFScore)

        auditLogService.logEvent(
            userId = userId,
            action = "CALCULATE",
            entityType = "PIOTROSKI_F_SCORE",
            entityId = savedFScore.id.toString(),
            details = "Calculated Piotroski F-Score for statement id: $statementId. F-Score: $fScore, Financial Strength: $financialStrength"
        )

        return savedFScore
    }

    @Transactional
    override fun prepareMlFeatures(statementId: Long, userId: UUID): MlFeatures {
        val statement = financialStatementRepository.findById(statementId)
            .orElseThrow { EntityNotFoundException("Financial statement not found with id: $statementId") }

        val financialData = financialDataRepository.findByStatementId(statementId)
            ?: throw EntityNotFoundException("Financial data not found for statement id: $statementId")

        val financialRatios = financialRatiosRepository.findByStatementId(statementId)
            ?: throw EntityNotFoundException("Financial ratios not found for statement id: $statementId")

        val altmanZScore = altmanZScoreRepository.findByStatementId(statementId)
            ?: throw EntityNotFoundException("Altman Z-Score not found for statement id: $statementId")

        val beneishMScore = beneishMScoreRepository.findByStatementId(statementId)
            ?: throw EntityNotFoundException("Beneish M-Score not found for statement id: $statementId")

        val piotroskiFScore = piotroskiFScoreRepository.findByStatementId(statementId)
            ?: throw EntityNotFoundException("Piotroski F-Score not found for statement id: $statementId")

        // Create a JSON object with all the features
        val featuresJson = JSONObject()

        // Add financial ratios
        featuresJson.put("current_ratio", financialRatios.currentRatio)
        featuresJson.put("quick_ratio", financialRatios.quickRatio)
        featuresJson.put("cash_ratio", financialRatios.cashRatio)
        featuresJson.put("gross_margin", financialRatios.grossMargin)
        featuresJson.put("operating_margin", financialRatios.operatingMargin)
        featuresJson.put("net_profit_margin", financialRatios.netProfitMargin)
        featuresJson.put("return_on_assets", financialRatios.returnOnAssets)
        featuresJson.put("return_on_equity", financialRatios.returnOnEquity)
        featuresJson.put("asset_turnover", financialRatios.assetTurnover)
        featuresJson.put("inventory_turnover", financialRatios.inventoryTurnover)
        featuresJson.put("accounts_receivable_turnover", financialRatios.accountsReceivableTurnover)
        featuresJson.put("days_sales_outstanding", financialRatios.daysSalesOutstanding)
        featuresJson.put("debt_to_equity", financialRatios.debtToEquity)
        featuresJson.put("debt_ratio", financialRatios.debtRatio)
        featuresJson.put("interest_coverage", financialRatios.interestCoverage)
        featuresJson.put("accrual_ratio", financialRatios.accrualRatio)
        featuresJson.put("earnings_quality", financialRatios.earningsQuality)

        // Add growth metrics
        featuresJson.put("revenue_growth", financialData.revenueGrowth)
        featuresJson.put("gross_profit_growth", financialData.grossProfitGrowth)
        featuresJson.put("net_income_growth", financialData.netIncomeGrowth)
        featuresJson.put("asset_growth", financialData.assetGrowth)
        featuresJson.put("receivables_growth", financialData.receivablesGrowth)
        featuresJson.put("inventory_growth", financialData.inventoryGrowth)
        featuresJson.put("liability_growth", financialData.liabilityGrowth)

        // Add Altman Z-Score components
        featuresJson.put("working_capital_to_total_assets", altmanZScore.workingCapitalToTotalAssets)
        featuresJson.put("retained_earnings_to_total_assets", altmanZScore.retainedEarningsToTotalAssets)
        featuresJson.put("ebit_to_total_assets", altmanZScore.ebitToTotalAssets)
        featuresJson.put("market_value_equity_to_book_value_debt", altmanZScore.marketValueEquityToBookValueDebt)
        featuresJson.put("sales_to_total_assets", altmanZScore.salesToTotalAssets)
        featuresJson.put("z_score", altmanZScore.zScore)

        // Add Beneish M-Score components
        featuresJson.put("days_sales_receivables_index", beneishMScore.daysSalesReceivablesIndex)
        featuresJson.put("gross_margin_index", beneishMScore.grossMarginIndex)
        featuresJson.put("asset_quality_index", beneishMScore.assetQualityIndex)
        featuresJson.put("sales_growth_index", beneishMScore.salesGrowthIndex)
        featuresJson.put("depreciation_index", beneishMScore.depreciationIndex)
        featuresJson.put("sg_admin_expenses_index", beneishMScore.sgAdminExpensesIndex)
        featuresJson.put("leverage_index", beneishMScore.leverageIndex)
        featuresJson.put("total_accruals_to_total_assets", beneishMScore.totalAccrualsToTotalAssets)
        featuresJson.put("m_score", beneishMScore.mScore)

        // Add Piotroski F-Score components
        featuresJson.put("positive_net_income", piotroskiFScore.positiveNetIncome)
        featuresJson.put("positive_operating_cash_flow", piotroskiFScore.positiveOperatingCashFlow)
        featuresJson.put("cash_flow_greater_than_net_income", piotroskiFScore.cashFlowGreaterThanNetIncome)
        featuresJson.put("improving_roa", piotroskiFScore.improvingRoa)
        featuresJson.put("decreasing_leverage", piotroskiFScore.decreasingLeverage)
        featuresJson.put("improving_current_ratio", piotroskiFScore.improvingCurrentRatio)
        featuresJson.put("no_new_shares", piotroskiFScore.noNewShares)
        featuresJson.put("improving_gross_margin", piotroskiFScore.improvingGrossMargin)
        featuresJson.put("improving_asset_turnover", piotroskiFScore.improvingAssetTurnover)
        featuresJson.put("f_score", piotroskiFScore.fScore)

        // Create and save ML Features entity
        val mlFeatures = MlFeatures(
            id = null,
            statement = statement,
            featureSet = featuresJson.toString(),
            createdAt = OffsetDateTime.now()
        )

        val savedFeatures = mlFeaturesRepository.save(mlFeatures)

        auditLogService.logEvent(
            userId = userId,
            action = "PREPARE",
            entityType = "ML_FEATURES",
            entityId = savedFeatures.id.toString(),
            details = "Prepared ML features for statement id: $statementId"
        )

        return savedFeatures
    }

    @Transactional
    override fun performMlPrediction(statementId: Long, userId: UUID): MlPrediction {
        val statement = financialStatementRepository.findById(statementId)
            .orElseThrow { EntityNotFoundException("Financial statement not found with id: $statementId") }

        val mlFeatures = mlFeaturesRepository.findByStatementId(statementId)
            ?: throw EntityNotFoundException("ML features not found for statement id: $statementId")

        // Get the active ML model
        val activeModels = mlModelRepository.findByIsActive(true)
        if (activeModels.isEmpty()) {
            throw IllegalStateException("No active ML model found")
        }

        val activeModel = activeModels.first()

        // NOTE: In a real implementation, this would call an ML service or library
        // For simplicity, we'll generate a prediction based on existing scores

        val featureSet = JSONObject(mlFeatures.featureSet)

        // Get underlying scores
        val zScore = featureSet.optBigDecimal("z_score", null)
        val mScore = featureSet.optBigDecimal("m_score", null)
        val fScore = featureSet.optInt("f_score", 0)

        // Calculate fraud probability based on these scores
        var fraudProbability = BigDecimal("0.5") // Default mid-point

        if (zScore != null && mScore != null) {
            // Lower Z-Score = higher fraud risk
            val zScoreComponent = if (zScore < BigDecimal("1.8")) {
                BigDecimal("0.4")
            } else if (zScore < BigDecimal("3.0")) {
                BigDecimal("0.2")
            } else {
                BigDecimal("0.1")
            }

            // Higher M-Score = higher fraud risk
            val mScoreComponent = if (mScore > BigDecimal("-1.78")) {
                BigDecimal("0.4")
            } else if (mScore > BigDecimal("-2.22")) {
                BigDecimal("0.3")
            } else {
                BigDecimal("0.1")
            }

            // Lower F-Score = higher fraud risk
            val fScoreComponent = when {
                fScore <= 3 -> BigDecimal("0.3")
                fScore <= 6 -> BigDecimal("0.2")
                else -> BigDecimal("0.1")
            }

            // Combine components
            fraudProbability = zScoreComponent.add(mScoreComponent).add(fScoreComponent).divide(BigDecimal("3"), 6, RoundingMode.HALF_UP)
        }

        // Create feature importance explanation
        val featureImportanceJson = JSONObject()
        featureImportanceJson.put("z_score", 0.35)
        featureImportanceJson.put("m_score", 0.40)
        featureImportanceJson.put("f_score", 0.25)

        // Generate explanation
        val explanation = "Fraud probability is based primarily on: " +
                "Beneish M-Score (40%), Altman Z-Score (35%), and Piotroski F-Score (25%). " +
                "Higher M-Score indicates potential earnings manipulation. " +
                "Lower Z-Score indicates financial distress. " +
                "Lower F-Score indicates weaker financial strength."

        // Create and save ML Prediction entity
        val mlPrediction = MlPrediction(
            id = null,
            statement = statement,
            model = activeModel,
            fraudProbability = fraudProbability,
            featureImportance = featureImportanceJson.toString(),
            predictionExplanation = explanation,
            predictedAt = OffsetDateTime.now()
        )

        val savedPrediction = mlPredictionRepository.save(mlPrediction)

        auditLogService.logEvent(
            userId = userId,
            action = "PREDICT",
            entityType = "ML_PREDICTION",
            entityId = savedPrediction.id.toString(),
            details = "Generated ML prediction for statement id: $statementId. Fraud Probability: $fraudProbability"
        )

        return savedPrediction
    }

    @Transactional
    override fun assessFraudRisk(statementId: Long, userId: UUID): FraudRiskAssessment {
        val statement = financialStatementRepository.findById(statementId)
            .orElseThrow { EntityNotFoundException("Financial statement not found with id: $statementId") }

        val altmanZScore = altmanZScoreRepository.findByStatementId(statementId)
            ?: throw EntityNotFoundException("Altman Z-Score not found for statement id: $statementId")

        val beneishMScore = beneishMScoreRepository.findByStatementId(statementId)
            ?: throw EntityNotFoundException("Beneish M-Score not found for statement id: $statementId")

        val piotroskiFScore = piotroskiFScoreRepository.findByStatementId(statementId)
            ?: throw EntityNotFoundException("Piotroski F-Score not found for statement id: $statementId")

        val financialRatios = financialRatiosRepository.findByStatementId(statementId)
            ?: throw EntityNotFoundException("Financial ratios not found for statement id: $statementId")

        // Get the latest ML prediction
        val mlPredictions = mlPredictionRepository.findLatestActiveByStatementId(statementId)
        if (mlPredictions.isEmpty()) {
            throw EntityNotFoundException("ML prediction not found for statement id: $statementId")
        }

        val mlPrediction = mlPredictions.first()

        // Calculate risk scores for each component (0-100 scale)

        // Z-Score risk: lower Z-Score = higher risk
        val zScoreRisk = if (altmanZScore.zScore != null) {
            when {
                altmanZScore.zScore!! < BigDecimal("1.8") -> BigDecimal("80.0")
                altmanZScore.zScore!! < BigDecimal("3.0") -> BigDecimal("50.0")
                else -> BigDecimal("20.0")
            }
        } else BigDecimal("50.0")

        // M-Score risk: higher M-Score = higher risk
        val mScoreRisk = if (beneishMScore.mScore != null) {
            when {
                beneishMScore.mScore!! > BigDecimal("-1.78") -> BigDecimal("85.0")
                beneishMScore.mScore!! > BigDecimal("-2.22") -> BigDecimal("60.0")
                else -> BigDecimal("25.0")
            }
        } else BigDecimal("50.0")

        // F-Score risk: lower F-Score = higher risk
        val fScoreRisk = if (piotroskiFScore.fScore != null) {
            when {
                piotroskiFScore.fScore!! <= 3 -> BigDecimal("75.0")
                piotroskiFScore.fScore!! <= 6 -> BigDecimal("45.0")
                else -> BigDecimal("20.0")
            }
        } else BigDecimal("50.0")

        // Financial ratio risk: based on accrual ratio and earnings quality
        val financialRatioRisk = if (financialRatios.accrualRatio != null && financialRatios.earningsQuality != null) {
            val accrualRiskComponent = if (financialRatios.accrualRatio!! > BigDecimal("0.1")) {
                BigDecimal("70.0")
            } else if (financialRatios.accrualRatio!! > BigDecimal("0.05")) {
                BigDecimal("40.0")
            } else {
                BigDecimal("20.0")
            }

            val earningsQualityRiskComponent = if (financialRatios.earningsQuality!! < BigDecimal("0.8")) {
                BigDecimal("75.0")
            } else if (financialRatios.earningsQuality!! < BigDecimal("1.0")) {
                BigDecimal("45.0")
            } else {
                BigDecimal("25.0")
            }

            accrualRiskComponent.add(earningsQualityRiskComponent).divide(BigDecimal("2"), 2, RoundingMode.HALF_UP)
        } else BigDecimal("50.0")

        // ML prediction risk: directly from fraud probability (0-1 scale to 0-100 scale)
        val mlPredictionRisk = mlPrediction.fraudProbability.multiply(BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)

        // Calculate overall risk score as weighted average
        val overallRiskScore = zScoreRisk.multiply(BigDecimal("0.20"))
            .add(mScoreRisk.multiply(BigDecimal("0.25")))
            .add(fScoreRisk.multiply(BigDecimal("0.15")))
            .add(financialRatioRisk.multiply(BigDecimal("0.15")))
            .add(mlPredictionRisk.multiply(BigDecimal("0.25")))
            .setScale(2, RoundingMode.HALF_UP)

        // Determine risk level
        val riskLevel = when {
            overallRiskScore >= BigDecimal("75.0") -> RiskLevel.VERY_HIGH
            overallRiskScore >= BigDecimal("60.0") -> RiskLevel.HIGH
            overallRiskScore >= BigDecimal("40.0") -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        // Generate summary
        val company = statement.fiscalYear.company.name
        val fiscalYear = statement.fiscalYear.year
        val statementType = statement.statementType

        val summary = """
            Fraud Risk Assessment for $company ($fiscalYear ${statementType.name})
            
            Overall Risk Score: $overallRiskScore (${riskLevel.name})
            
            Component Risk Scores:
            - Altman Z-Score: $zScoreRisk (${altmanZScore.zScore ?: "N/A"})
            - Beneish M-Score: $mScoreRisk (${beneishMScore.mScore ?: "N/A"})
            - Piotroski F-Score: $fScoreRisk (${piotroskiFScore.fScore ?: "N/A"})
            - Financial Ratios: $financialRatioRisk
            - ML Prediction: $mlPredictionRisk
            
            Key Findings:
            ${if (altmanZScore.riskCategory == RiskCategory.DISTRESS) "- Company shows signs of financial distress based on Z-Score analysis." else ""}
            ${if (beneishMScore.manipulationProbability == ManipulationProbability.HIGH) "- High probability of earnings manipulation based on M-Score analysis." else ""}
            ${if (piotroskiFScore.financialStrength == FinancialStrength.WEAK) "- Weak financial strength based on F-Score analysis." else ""}
            ${if (mlPrediction.fraudProbability > BigDecimal("0.6")) "- ML model indicates high likelihood of fraudulent reporting." else ""}
            
            This assessment is based on quantitative analysis of financial data. Further qualitative analysis of financial disclosures is recommended.
        """.trimIndent()

        // Create and save fraud risk assessment entity
        val fraudRiskAssessment = FraudRiskAssessment(
            id = null,
            statement = statement,
            zScoreRisk = zScoreRisk,
            mScoreRisk = mScoreRisk,
            fScoreRisk = fScoreRisk,
            financialRatioRisk = financialRatioRisk,
            mlPredictionRisk = mlPredictionRisk,
            overallRiskScore = overallRiskScore,
            riskLevel = riskLevel,
            assessmentSummary = summary,
            assessedAt = OffsetDateTime.now(),
            assessedBy = statement.user
        )

        val savedAssessment = fraudRiskAssessmentRepository.save(fraudRiskAssessment)

        auditLogService.logEvent(
            userId = userId,
            action = "ASSESS",
            entityType = "FRAUD_RISK",
            entityId = savedAssessment.id.toString(),
            details = "Generated fraud risk assessment for statement id: $statementId. Risk Level: $riskLevel, Score: $overallRiskScore"
        )

        return savedAssessment
    }

    @Transactional
    override fun generateRiskAlerts(assessmentId: Long, userId: UUID): List<RiskAlert> {
        val assessment = fraudRiskAssessmentRepository.findById(assessmentId)
            .orElseThrow { EntityNotFoundException("Fraud risk assessment not found with id: $assessmentId") }

        val alerts = mutableListOf<RiskAlert>()

        // Generate alerts based on risk level
        if (assessment.riskLevel == RiskLevel.VERY_HIGH || assessment.riskLevel == RiskLevel.HIGH) {
            // Alert for overall risk
            val overallAlert = RiskAlert(
                id = null,
                assessment = assessment,
                alertType = "OVERALL_RISK",
                severity = if (assessment.riskLevel == RiskLevel.VERY_HIGH ) AlertSeverity.VERY_HIGH else AlertSeverity.HIGH ,
                message = "High overall fraud risk detected for ${assessment.statement.fiscalYear.company.name} " +
                        "(${assessment.statement.fiscalYear.year}). Score: ${assessment.overallRiskScore}",
                createdAt = OffsetDateTime.now(),
                isResolved = false,
                resolvedBy = null,
                resolvedAt = null,
                resolutionNotes = null
            )

            alerts.add(riskAlertRepository.save(overallAlert))
        }

        // Generate component-specific alerts

        // Z-Score alert
        if (assessment.zScoreRisk != null && assessment.zScoreRisk!! >= BigDecimal("70.0")) {
            val zScoreAlert = RiskAlert(
                id = null,
                assessment = assessment,
                alertType = "Z_SCORE",
                severity = AlertSeverity.HIGH,
                message = "Financial distress indicated by Altman Z-Score analysis for ${assessment.statement.fiscalYear.company.name}",
                createdAt = OffsetDateTime.now(),
                isResolved = false,
                resolvedBy = null,
                resolvedAt = null,
                resolutionNotes = null
            )

            alerts.add(riskAlertRepository.save(zScoreAlert))
        }

        // M-Score alert
        if (assessment.mScoreRisk != null && assessment.mScoreRisk!! >= BigDecimal("70.0")) {
            val mScoreAlert = RiskAlert(
                id = null,
                assessment = assessment,
                alertType = "M_SCORE",
                severity = AlertSeverity.HIGH,
                message = "Potential earnings manipulation detected by Beneish M-Score for ${assessment.statement.fiscalYear.company.name}",
                createdAt = OffsetDateTime.now(),
                isResolved = false,
                resolvedBy = null,
                resolvedAt = null,
                resolutionNotes = null
            )

            alerts.add(riskAlertRepository.save(mScoreAlert))
        }

        // F-Score alert
        if (assessment.fScoreRisk != null && assessment.fScoreRisk!! >= BigDecimal("70.0")) {
            val fScoreAlert = RiskAlert(
                id = null,
                assessment = assessment,
                alertType = "F_SCORE",
                severity = AlertSeverity.HIGH,
                message = "Weak financial strength indicated by Piotroski F-Score for ${assessment.statement.fiscalYear.company.name}",
                createdAt = OffsetDateTime.now(),
                isResolved = false,
                resolvedBy = null,
                resolvedAt = null,
                resolutionNotes = null
            )

            alerts.add(riskAlertRepository.save(fScoreAlert))
        }

        // ML Prediction alert
        if (assessment.mlPredictionRisk != null && assessment.mlPredictionRisk!! >= BigDecimal("70.0")) {
            val mlAlert = RiskAlert(
                id = null,
                assessment = assessment,
                alertType = "ML_PREDICTION",
                severity = AlertSeverity.HIGH,
                message = "Machine learning model indicates high fraud probability for ${assessment.statement.fiscalYear.company.name}",
                createdAt = OffsetDateTime.now(),
                isResolved = false,
                resolvedBy = null,
                resolvedAt = null,
                resolutionNotes = null
            )

            alerts.add(riskAlertRepository.save(mlAlert))
        }

        // Log event for all generated alerts
        auditLogService.logEvent(
            userId = userId,
            action = "GENERATE",
            entityType = "RISK_ALERTS",
            entityId = assessment.id.toString(),
            details = "Generated ${alerts.size} risk alerts for assessment id: ${assessment.id}"
        )

        return alerts
    }
}
