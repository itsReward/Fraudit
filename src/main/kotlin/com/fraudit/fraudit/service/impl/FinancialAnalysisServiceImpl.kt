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
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import kotlin.math.abs
import kotlin.math.exp

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

    val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    override fun calculateAllScoresAndRatios(statementId: Long, userId: UUID): FraudRiskAssessment {

        deleteExistingAnalysis(statementId)

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
        val currentRatio = capNumericValue(calculateCurrentRatio(financialData), 1e9, -1e9)
        val quickRatio = capNumericValue(calculateQuickRatio(financialData), 1e9, -1e9)
        val cashRatio = capNumericValue(calculateCashRatio(financialData), 1e9, -1e9)


        // Calculate profitability ratios
        val grossMargin = capNumericValue(calculateGrossMargin(financialData), 1e9, -1e9)
        val operatingMargin = capNumericValue(calculateOperatingMargin(financialData), 1e9, -1e9)
        val netProfitMargin = capNumericValue(calculateNetProfitMargin(financialData), 1e9, -1e9)
        val returnOnAssets = capNumericValue(calculateReturnOnAssets(financialData), 1e9, -1e9)
        val returnOnEquity = capNumericValue(calculateReturnOnEquity(financialData), 1e9, -1e9)

        // Calculate efficiency ratios
        val assetTurnover = capNumericValue(calculateAssetTurnover(financialData), 1e9, -1e9)
        val inventoryTurnover = capNumericValue(calculateInventoryTurnover(financialData), 1e9, -1e9)
        val accountsReceivableTurnover = capNumericValue(calculateAccountsReceivableTurnover(financialData), 1e9, -1e9)
        val daysSalesOutstanding = capNumericValue(calculateDaysSalesOutstanding(financialData), 1e9, -1e9)

        // Calculate leverage ratios
        val debtToEquity = capNumericValue(calculateDebtToEquity(financialData), 1e9, -1e9)
        val debtRatio = capNumericValue(calculateDebtRatio(financialData), 1e9, -1e9)
        val interestCoverage = capNumericValue(calculateInterestCoverage(financialData), 1e9, -1e9)

        // Calculate valuation ratios
        val priceToEarnings = capNumericValue(calculatePriceToEarnings(financialData), 1e9, -1e9)
        val priceToBook = capNumericValue(calculatePriceToBook(financialData), 1e9, -1e9)

        // Calculate quality metrics
        val accrualRatio = capNumericValue(calculateAccrualRatio(financialData), 1e9, -1e9)
        val earningsQuality = capNumericValue(calculateEarningsQuality(financialData), 1e9, -1e9)


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

        // Add validation before saving to prevent numeric overflow
        validateNumericValues(financialRatios)


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
        if (data.marketPricePerShare == null ||
            data.earningsPerShare == null ||
            data.earningsPerShare!!.compareTo(BigDecimal.ZERO) == 0) {
            return null
        }

        return try {
            data.marketPricePerShare!!.divide(data.earningsPerShare, 4, RoundingMode.HALF_UP)
        } catch (e: ArithmeticException) {
            // Log that division by zero occurred
            null
        }
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
        // Add validation before saving
        validateAltmanZScore(altmanZScore)


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

        // Add validation before saving
        validateBeneishMScore(beneishMScore)


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

        // Add validation before saving
        validatePiotroskiFScore(piotroskiFScore)


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
        // Get financial statement and financial data
        val statement = financialStatementRepository.findById(statementId)
            .orElseThrow { EntityNotFoundException("Financial statement not found with id: $statementId") }

        // Get ML features
        val mlFeatures = mlFeaturesRepository.findByStatementId(statementId)
            ?: throw EntityNotFoundException("ML features not found for statement id: $statementId")

        try {
            // Find active model or activate the latest model if none is active
            val model = findOrActivateModel(userId)

            logger.info("Using model ${model.modelName} v${model.modelVersion} for prediction")

            // Prepare feature vector for model input
            val featureVector = prepareFeatureVector(mlFeatures)

            // Calculate fraud probability using the model
            val predictionResult = applyModel(model, featureVector)

            // Extract important features for explanation
            val featureImportance = generateFeatureImportance(predictionResult, featureVector)

            // Generate human-readable explanation
            val explanation = generateExplanation(predictionResult, featureImportance)

            // Create and save prediction
            val mlPrediction = MlPrediction(
                id = null,
                statement = statement,
                model = model,
                fraudProbability = predictionResult.fraudProbability,
                featureImportance = featureImportance,
                predictionExplanation = explanation,
                predictedAt = OffsetDateTime.now()
            )

            val savedPrediction = mlPredictionRepository.save(mlPrediction)

            // Log the event
            auditLogService.logEvent(
                userId = userId,
                action = "PREDICT",
                entityType = "ML_PREDICTION",
                entityId = savedPrediction.id.toString(),
                details = "Created ML prediction for statement ID: $statementId using model: ${model.modelName} v${model.modelVersion}"
            )

            return savedPrediction
        } catch (e: Exception) {
            logger.error("Error during ML prediction: ${e.message}", e)
            throw e
        }
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

    override fun getFraudRiskAssessmentById(id: Long): FraudRiskAssessment {
        return fraudRiskAssessmentRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Fraud risk assessment not found with id: $id") }
    }

    override fun getFraudRiskAssessmentByStatementId(statementId: Long): FraudRiskAssessment? {
        return fraudRiskAssessmentRepository.findByStatementId(statementId)
    }

    override fun getAllFraudRiskAssessments(pageable: Pageable): Page<FraudRiskAssessment> {
        return fraudRiskAssessmentRepository.findAll(pageable)
    }

    override fun getFraudRiskAssessmentsByCompany(companyId: Long, pageable: Pageable): Page<FraudRiskAssessment> {
        val assessments = fraudRiskAssessmentRepository.findLatestByCompanyId(companyId)

        // Convert list to Page
        val start = pageable.offset.toInt()
        val end = minOf(start + pageable.pageSize, assessments.size)

        val pagedItems = if (start < assessments.size) {
            assessments.subList(start, end)
        } else {
            emptyList()
        }

        return PageImpl(pagedItems, pageable, assessments.size.toLong())
    }

    override fun getFraudRiskAssessmentsByRiskLevel(riskLevel: RiskLevel, pageable: Pageable): Page<FraudRiskAssessment> {
        // Get all assessments for the given risk level (this returns a List)
        val allAssessments = fraudRiskAssessmentRepository.findByRiskLevel(riskLevel)

        // Manual pagination since the repository method doesn't support it
        val start = pageable.offset.toInt()
        val end = minOf(start + pageable.pageSize, allAssessments.size)

        val pagedItems = if (start < allAssessments.size) {
            allAssessments.subList(start, end)
        } else {
            emptyList()
        }

        // Create a Page from the list
        return PageImpl(pagedItems, pageable, allAssessments.size.toLong())
    }

    override fun getFraudRiskAssessmentsByCompanyAndRiskLevel(
        companyId: Long,
        riskLevel: RiskLevel,
        pageable: Pageable
    ): Page<FraudRiskAssessment> {
        // Get all assessments for the company
        val companyAssessments = fraudRiskAssessmentRepository.findLatestByCompanyId(companyId)

        // Filter by risk level
        val filteredAssessments = companyAssessments.filter { it.riskLevel == riskLevel }

        // Convert list to Page
        val start = pageable.offset.toInt()
        val end = minOf(start + pageable.pageSize, filteredAssessments.size)

        val pagedItems = if (start < filteredAssessments.size) {
            filteredAssessments.subList(start, end)
        } else {
            emptyList()
        }

        return PageImpl(pagedItems, pageable, filteredAssessments.size.toLong())
    }

    override fun getFinancialRatios(statementId: Long): FinancialRatios? {
        return financialRatiosRepository.findByStatementId(statementId)
    }

    override fun getAltmanZScore(statementId: Long): AltmanZScore? {
        return altmanZScoreRepository.findByStatementId(statementId)
    }

    override fun getBeneishMScore(statementId: Long): BeneishMScore? {
        return beneishMScoreRepository.findByStatementId(statementId)
    }

    override fun getPiotroskiFScore(statementId: Long): PiotroskiFScore? {
        return piotroskiFScoreRepository.findByStatementId(statementId)
    }


    @Transactional
    override fun deleteExistingAnalysis(statementId: Long) {
        try {
            // Delete any existing financial ratios
            financialRatiosRepository.deleteByStatementId(statementId)

            // Delete any existing Altman Z-Score
            altmanZScoreRepository.deleteByStatementId(statementId)

            // Delete any existing Beneish M-Score
            beneishMScoreRepository.deleteByStatementId(statementId)

            // Delete any existing Piotroski F-Score
            piotroskiFScoreRepository.deleteByStatementId(statementId)

            // Delete any existing ML features
            mlFeaturesRepository.deleteByStatementId(statementId)

            // Delete any existing ML predictions
            mlPredictionRepository.deleteByStatementId(statementId)

            // Delete any existing fraud risk assessment
            fraudRiskAssessmentRepository.deleteByStatementId(statementId)

            // Delete any existing risk alerts
            riskAlertRepository.deleteByStatementId(statementId)
        } catch (e: Exception) {
            // Log but don't throw so that processing can continue even if some entities don't exist
            logger.warn("Could not delete all existing analysis data: ${e.message}")
        }
    }


    /**
     * Validates numeric values to prevent database overflow.
     * Logs warnings if any FinancialRatios values exceed safe database limits
     */
    private fun validateNumericValues(ratios: FinancialRatios) {
        val maxSafeValue = 1e9
        val minSafeValue = -1e9

        // Check each ratio value and log warnings if they exceed safe limits
        logIfUnsafe(ratios.currentRatio, "currentRatio", maxSafeValue, minSafeValue)
        logIfUnsafe(ratios.quickRatio, "quickRatio", maxSafeValue, minSafeValue)
        logIfUnsafe(ratios.cashRatio, "cashRatio", maxSafeValue, minSafeValue)
        logIfUnsafe(ratios.grossMargin, "grossMargin", maxSafeValue, minSafeValue)
        logIfUnsafe(ratios.operatingMargin, "operatingMargin", maxSafeValue, minSafeValue)
        logIfUnsafe(ratios.netProfitMargin, "netProfitMargin", maxSafeValue, minSafeValue)
        logIfUnsafe(ratios.returnOnAssets, "returnOnAssets", maxSafeValue, minSafeValue)
        logIfUnsafe(ratios.returnOnEquity, "returnOnEquity", maxSafeValue, minSafeValue)
        logIfUnsafe(ratios.assetTurnover, "assetTurnover", maxSafeValue, minSafeValue)
        logIfUnsafe(ratios.inventoryTurnover, "inventoryTurnover", maxSafeValue, minSafeValue)
        logIfUnsafe(ratios.accountsReceivableTurnover, "accountsReceivableTurnover", maxSafeValue, minSafeValue)
        logIfUnsafe(ratios.daysSalesOutstanding, "daysSalesOutstanding", maxSafeValue, minSafeValue)
        logIfUnsafe(ratios.debtToEquity, "debtToEquity", maxSafeValue, minSafeValue)
        logIfUnsafe(ratios.debtRatio, "debtRatio", maxSafeValue, minSafeValue)
        logIfUnsafe(ratios.interestCoverage, "interestCoverage", maxSafeValue, minSafeValue)
        logIfUnsafe(ratios.priceToEarnings, "priceToEarnings", maxSafeValue, minSafeValue)
        logIfUnsafe(ratios.priceToBook, "priceToBook", maxSafeValue, minSafeValue)
        logIfUnsafe(ratios.earningsQuality, "earningsQuality", maxSafeValue, minSafeValue)
        logIfUnsafe(ratios.accrualRatio, "accrualRatio", maxSafeValue, minSafeValue)
    }
    /**
     * Caps a numeric value to prevent overflow
     */
    private fun capNumericValue(value: BigDecimal?, max: Double, min: Double): BigDecimal? {
        if (value == null) return null

        return when {
            value.toDouble() > max -> BigDecimal.valueOf(max)
            value.toDouble() < min -> BigDecimal.valueOf(min)
            value.toDouble().isNaN() -> BigDecimal.ZERO  // Handle NaN
            value.toDouble().isInfinite() -> {  // Handle infinity
                if (value.toDouble() > 0) BigDecimal.valueOf(max)
                else BigDecimal.valueOf(min)
            }
            else -> value
        }
    }

    /**
     * Logs warnings if any AltmanZScore values exceed safe database limits
     */
    private fun validateAltmanZScore(score: AltmanZScore) {
        val maxSafeValue = 1e9
        val minSafeValue = -1e9

        // Check each value and log warnings if they exceed safe limits
        logIfUnsafe(score.workingCapitalToTotalAssets, "workingCapitalToTotalAssets", maxSafeValue, minSafeValue)
        logIfUnsafe(score.retainedEarningsToTotalAssets, "retainedEarningsToTotalAssets", maxSafeValue, minSafeValue)
        logIfUnsafe(score.ebitToTotalAssets, "ebitToTotalAssets", maxSafeValue, minSafeValue)
        logIfUnsafe(score.marketValueEquityToBookValueDebt, "marketValueEquityToBookValueDebt", maxSafeValue, minSafeValue)
        logIfUnsafe(score.salesToTotalAssets, "salesToTotalAssets", maxSafeValue, minSafeValue)
        logIfUnsafe(score.zScore, "zScore", maxSafeValue, minSafeValue)
    }

    /**
     * Logs warnings if any BeneishMScore values exceed safe database limits
     */
    private fun validateBeneishMScore(score: BeneishMScore) {
        val maxSafeValue = 1e9
        val minSafeValue = -1e9

        // Check each value and log warnings if they exceed safe limits
        logIfUnsafe(score.daysSalesReceivablesIndex, "daysSalesReceivablesIndex", maxSafeValue, minSafeValue)
        logIfUnsafe(score.grossMarginIndex, "grossMarginIndex", maxSafeValue, minSafeValue)
        logIfUnsafe(score.assetQualityIndex, "assetQualityIndex", maxSafeValue, minSafeValue)
        logIfUnsafe(score.salesGrowthIndex, "salesGrowthIndex", maxSafeValue, minSafeValue)
        logIfUnsafe(score.depreciationIndex, "depreciationIndex", maxSafeValue, minSafeValue)
        logIfUnsafe(score.sgAdminExpensesIndex, "sgAdminExpensesIndex", maxSafeValue, minSafeValue)
        logIfUnsafe(score.leverageIndex, "leverageIndex", maxSafeValue, minSafeValue)
        logIfUnsafe(score.totalAccrualsToTotalAssets, "totalAccrualsToTotalAssets", maxSafeValue, minSafeValue)
        logIfUnsafe(score.mScore, "mScore", maxSafeValue, minSafeValue)
    }


    /**
     * Similar validation for PiotroskiFScore
     */
    private fun validatePiotroskiFScore(score: PiotroskiFScore) {
        // Piotroski F-Score components are typically 0 or 1, so we mainly need to cap the final score
        val maxSafeValue = 1e9
        val minSafeValue = -1e9

        if (score.fScore != null) {
            val value = score.fScore!!.toDouble()
            if (value > maxSafeValue || value < minSafeValue || value.isNaN() || value.isInfinite()) {
                logger.warn("PiotroskiFScore fScore value exceeds safe database limits: $value")
            }
        }

        // Cap individual components if they exist as BigDecimal fields
        // We'll use reflection to safely check and cap all BigDecimal fields
        score.javaClass.declaredFields.forEach { field ->
            field.isAccessible = true
            val value = field.get(score)
            if (value is BigDecimal) {
                val cappedValue = capNumericValue(value, maxSafeValue, minSafeValue)
                field.set(score, cappedValue)
            }
        }
    }

    /**
     * Helper function to log warnings for unsafe values
     */
    private fun logIfUnsafe(value: BigDecimal?, fieldName: String, max: Double, min: Double) {
        if (value == null) return

        val doubleValue = value.toDouble()
        if (doubleValue > max || doubleValue < min || doubleValue.isNaN() || doubleValue.isInfinite()) {
            logger.warn("Value for $fieldName exceeds safe database limits: $doubleValue")
        }
    }


    /**
    * Finds an active ML model or activates the latest model if none is active
    */
    private fun findOrActivateModel(userId: UUID): MlModel {
        // First, try to find an active model
        val activeModels = mlModelRepository.findByIsActive(true)

        if (activeModels.isNotEmpty()) {
            return activeModels.first()
        }

        logger.info("No active ML model found, attempting to activate the latest model")

        // No active model found, find the latest model by creation date/version
        val allModels = mlModelRepository.findAll()

        if (allModels.isEmpty()) {
            throw IllegalStateException("No ML models found in the database")
        }

        // Sort by creation date (descending) and get the most recent
        val latestModel = allModels.sortedByDescending { it.trainedDate }.first()

        logger.info("Activating latest model: ${latestModel.modelName} v${latestModel.modelVersion}")

        // Activate the latest model
        val activatedModel = latestModel.copy(isActive = true)
        val savedModel = mlModelRepository.save(activatedModel)

        // Log the automatic activation
        auditLogService.logEvent(
            userId = userId,
            action = "AUTO_ACTIVATE",
            entityType = "ML_MODEL",
            entityId = savedModel.id.toString(),
            details = "Automatically activated ML model: ${savedModel.modelName} (${savedModel.modelVersion}) - no active model was found"
        )

        return savedModel
    }


    /**
     * Data class to hold prediction results
     */
    private data class PredictionResult(
        val fraudProbability: BigDecimal,
        val importantFeatures: List<Pair<String, Double>>,
        val metadata: Map<String, Any>
    )

    /**
     * Applies the ML model to feature vector to get prediction
     */
    private fun applyModel(model: MlModel, features: Map<String, Any>): PredictionResult {
        try {
            // Parse the model definition and feature list
            val modelType = model.modelType
            val featureIndices = try {
                JSONObject(model.featureList)
            } catch (e: Exception) {
                logger.warn("Could not parse feature list from model: ${e.message}")
                null
            }

            // Get performance metrics
            val performanceMetrics = try {
                JSONObject(model.performanceMetrics)
            } catch (e: Exception) {
                logger.warn("Could not parse performance metrics from model: ${e.message}")
                null
            }

            when (modelType) {
                "RANDOM_FOREST" -> {
                    // Use feature indices if available to prioritize features
                    val prioritizedFeatures = if (featureIndices != null) {
                        // Extract feature indices and sort by importance (lower index = more important)
                        val featureRankings = featureIndices.keys().asSequence()
                            .associate { key -> key to (featureIndices.optInt(key, 999)) }
                            .toList()
                            .sortedBy { it.second }
                            .take(10) // Top 10 most important features
                            .map { it.first }
                            .toList()
                    } else {
                        // Default important features if indices not available
                        listOf("current_ratio", "quick_ratio", "net_profit_margin", "z_score", "m_score", "f_score")
                    }

                    // Check for red flags in prioritized features
                    val redFlags = mutableListOf<Pair<String, Double>>()

                    // Map from snake_case to camelCase for feature accessing
                    val snakeToCamel = mapOf(
                        "current_ratio" to "currentRatio",
                        "quick_ratio" to "quickRatio",
                        "net_profit_margin" to "netProfitMargin",
                        "z_score" to "zScore",
                        "m_score" to "mScore",
                        "f_score" to "fScore",
                        "return_on_assets" to "returnOnAssets",
                        "debt_to_equity" to "debtToEquity",
                        "total_accruals_to_total_assets" to "totalAccrualsToTotalAssets",
                        "asset_turnover" to "assetTurnover",
                        "gross_margin" to "grossMargin",
                        "operating_margin" to "operatingMargin",
                        "days_sales_receivables_index" to "daysSalesReceivablesIndex",
                        "sales_growth_index" to "salesGrowthIndex"
                    )

                    // Define threshold checks for different features
                    val thresholdChecks = mapOf(
                        "currentRatio" to { value: Double -> (value < 1.0) to 0.7 },
                        "quickRatio" to { value: Double -> (value < 0.5) to 0.6 },
                        "netProfitMargin" to { value: Double -> (value < 0.0) to 0.8 },
                        "zScore" to { value: Double -> (value < 1.8) to 0.9 },
                        "mScore" to { value: Double -> (value > -2.22) to 0.85 },
                        "fScore" to { value: Double -> (value < 3.0) to 0.75 },
                        "returnOnAssets" to { value: Double -> (value < 0.02) to 0.65 },
                        "debtToEquity" to { value: Double -> (value > 2.0) to 0.7 },
                        "totalAccrualsToTotalAssets" to { value: Double -> (value > 0.1) to 0.8 },
                        "assetTurnover" to { value: Double -> (value < 0.5) to 0.5 }
                    )

                    // Apply threshold checks to features
                    // Use a default list of features if prioritizedFeatures can't be iterated
                    val defaultFeatures = listOf(
                        "currentRatio", "quickRatio", "netProfitMargin",
                        "zScore", "mScore", "fScore"
                    )

                    // Try to use the existing collection or fall back to default
                    val featuresToCheck = try {
                        prioritizedFeatures as? List<String> ?: defaultFeatures
                    } catch (e: Exception) {
                        logger.warn("Could not cast prioritizedFeatures to List<String>, using default features", e)
                        defaultFeatures
                    }

                    for (featureName in featuresToCheck) {
                        val camelCaseName = featureName // Assume it's already in camelCase format

                        // Get feature value
                        val featureValue = features[camelCaseName] as? Double ?: continue

                        // Apply threshold check if one exists
                        thresholdChecks[camelCaseName]?.let { check ->
                            val (exceedsThreshold, weight) = check(featureValue)
                            if (exceedsThreshold) {
                                redFlags.add(Pair(camelCaseName, weight))
                            }
                        }
                    }

                    // Calculate weighted probability based on red flags
                    val redFlagCount = redFlags.size
                    val totalWeight = redFlags.sumOf { it.second }

                    val baseProbability = when {
                        redFlagCount >= 4 -> 0.85
                        redFlagCount >= 2 -> 0.65
                        redFlagCount >= 1 -> 0.45
                        else -> 0.25
                    }

                    // Adjust probability based on weights and performance metrics
                    val modelAccuracy = performanceMetrics?.optDouble("accuracy", 0.0) ?: 0.0
                    val accuracyFactor = if (modelAccuracy > 0) modelAccuracy / 100.0 else 1.0

                    val adjustedProbability = if (redFlags.isEmpty()) {
                        baseProbability * accuracyFactor
                    } else {
                        minOf(0.95, (baseProbability + (totalWeight / (redFlags.size * 10))) * accuracyFactor)
                    }

                    return PredictionResult(
                        fraudProbability = BigDecimal(adjustedProbability).setScale(4, RoundingMode.HALF_UP),
                        importantFeatures = redFlags,
                        metadata = mapOf(
                            "redFlagCount" to redFlagCount,
                            "modelType" to "RANDOM_FOREST",
                            "modelVersion" to model.modelVersion,
                            "modelAccuracy" to modelAccuracy
                        )
                    )
                }
                "NEURAL_NETWORK" -> {
                    // Get feature indices for neural network weights if available
                    val featureWeights = if (featureIndices != null) {
                        // Extract top features and assign weights based on importance
                        featureIndices.keys().asSequence()
                            .take(10) // Use top 10 features
                            .associate { key ->
                                val camelCase = snakeToCamel[key] ?: key
                                val importance = featureIndices.optInt(key, 999)
                                // Invert and normalize weights (lower index = more important)
                                val weight = if (importance < 20) {
                                    // Top features
                                    if (key.contains("m_score") || key.contains("receivables") ||
                                        key.contains("accruals") || key.contains("sales_growth")) {
                                        0.6 // These typically positively correlate with fraud
                                    } else {
                                        -0.5 // These typically negatively correlate with fraud
                                    }
                                } else {
                                    // Less important features get smaller weights
                                    if (key.contains("m_score") || key.contains("receivables") ||
                                        key.contains("accruals") || key.contains("sales_growth")) {
                                        0.3
                                    } else {
                                        -0.25
                                    }
                                }
                                camelCase to weight
                            }
                    } else {
                        // Default weights if feature indices not available
                        mapOf(
                            "currentRatio" to -0.3,
                            "quickRatio" to -0.25,
                            "netProfitMargin" to -0.4,
                            "returnOnAssets" to -0.35,
                            "zScore" to -0.5,
                            "mScore" to 0.6,
                            "fScore" to -0.45,
                            "totalAccrualsToTotalAssets" to 0.3,
                            "salesGrowthIndex" to 0.2
                        )
                    }

                    // Calculate weighted sum
                    var sum = 0.0
                    val importantFeatures = mutableListOf<Pair<String, Double>>()

                    for ((key, weight) in featureWeights) {
                        val value = features[key] as? Double ?: 0.0
                        val contribution = value * weight
                        sum += contribution

                        // Track important features and their contribution
                        if (abs(contribution) > 0.1) {
                            importantFeatures.add(Pair(key, abs(contribution)))
                        }
                    }

                    // Apply sigmoid function to get probability
                    val probability = 1.0 / (1.0 + exp(-sum))

                    // Sort features by importance
                    val sortedFeatures = importantFeatures.sortedByDescending { it.second }

                    return PredictionResult(
                        fraudProbability = BigDecimal(probability).setScale(4, RoundingMode.HALF_UP),
                        importantFeatures = sortedFeatures,
                        metadata = mapOf(
                            "rawScore" to sum,
                            "modelType" to "NEURAL_NETWORK",
                            "modelVersion" to model.modelVersion
                        )
                    )
                }
                else -> {
                    // Use top features from model if available
                    val keyFeatures = if (featureIndices != null) {
                        featureIndices.keys().asSequence()
                            .sortedBy { featureIndices.optInt(it, 999) }
                            .take(3)
                            .map { snakeToCamel[it] ?: it }
                            .filter { features.containsKey(it) }
                            .toList()
                    } else {
                        listOf("zScore", "mScore", "fScore")
                    }

                    // Get feature values
                    val zScore = features[keyFeatures.getOrNull(0) ?: "zScore"] as? Double ?: 0.0
                    val mScore = features[keyFeatures.getOrNull(1) ?: "mScore"] as? Double ?: 0.0
                    val fScore = features[keyFeatures.getOrNull(2) ?: "fScore"] as? Double ?: 0.0

                    // Simple logistic regression formula
                    val score = -3.0 +
                            (-0.5 * zScore) +
                            (0.8 * mScore) +
                            (-0.6 * fScore)

                    val probability = 1.0 / (1.0 + exp(-score))

                    val importantFeatures = listOf(
                        Pair(keyFeatures.getOrNull(0) ?: "zScore", abs(-0.5 * zScore)),
                        Pair(keyFeatures.getOrNull(1) ?: "mScore", abs(0.8 * mScore)),
                        Pair(keyFeatures.getOrNull(2) ?: "fScore", abs(-0.6 * fScore))
                    ).sortedByDescending { it.second }

                    return PredictionResult(
                        fraudProbability = BigDecimal(probability).setScale(4, RoundingMode.HALF_UP),
                        importantFeatures = importantFeatures,
                        metadata = mapOf(
                            "rawScore" to score,
                            "modelType" to "LOGISTIC_REGRESSION",
                            "modelVersion" to model.modelVersion
                        )
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("Error applying ML model: ${e.message}", e)

            // Fallback to simple prediction based on key fraud indicators
            logger.info("Using fallback prediction method")

            val zScore = features["zScore"] as? Double ?: 0.0
            val mScore = features["mScore"] as? Double ?: 0.0
            val fScore = features["fScore"] as? Double ?: 0.0

            // Simple weighted average for fallback
            val zScoreWeight = if (zScore < 1.8) 0.35 else 0.1
            val mScoreWeight = if (mScore > -2.22) 0.45 else 0.15
            val fScoreWeight = if (fScore < 3.0) 0.2 else 0.05

            val fraudProbability = (zScoreWeight * (1.0 - (zScore / 3.0).coerceIn(0.0, 1.0))) +
                    (mScoreWeight * ((mScore + 5.0) / 10.0).coerceIn(0.0, 1.0)) +
                    (fScoreWeight * (1.0 - (fScore / 9.0)).coerceIn(0.0, 1.0))

            val importantFeatures = listOf(
                Pair("zScore", zScoreWeight),
                Pair("mScore", mScoreWeight),
                Pair("fScore", fScoreWeight)
            ).sortedByDescending { it.second }

            return PredictionResult(
                fraudProbability = BigDecimal(fraudProbability.coerceIn(0.0, 0.95))
                    .setScale(4, RoundingMode.HALF_UP),
                importantFeatures = importantFeatures,
                metadata = mapOf(
                    "fallback" to true,
                    "error" to e.message.toString(),
                    "modelType" to model.modelType,
                    "modelVersion" to model.modelVersion
                )
            )
        }
    }

    // Helper function to map snake_case keys to camelCase for consistent use
    private val snakeToCamel = mapOf(
        "current_ratio" to "currentRatio",
        "quick_ratio" to "quickRatio",
        "cash_ratio" to "cashRatio",
        "gross_margin" to "grossMargin",
        "operating_margin" to "operatingMargin",
        "net_profit_margin" to "netProfitMargin",
        "return_on_assets" to "returnOnAssets",
        "return_on_equity" to "returnOnEquity",
        "asset_turnover" to "assetTurnover",
        "inventory_turnover" to "inventoryTurnover",
        "accounts_receivable_turnover" to "accountsReceivableTurnover",
        "days_sales_outstanding" to "daysSalesOutstanding",
        "debt_to_equity" to "debtToEquity",
        "debt_ratio" to "debtRatio",
        "interest_coverage" to "interestCoverage",
        "accrual_ratio" to "accrualRatio",
        "earnings_quality" to "earningsQuality",
        "working_capital_to_total_assets" to "workingCapitalToTotalAssets",
        "retained_earnings_to_total_assets" to "retainedEarningsToTotalAssets",
        "ebit_to_total_assets" to "ebitToTotalAssets",
        "market_value_equity_to_book_value_debt" to "marketValueEquityToTotalLiabilities",
        "sales_to_total_assets" to "salesToTotalAssets",
        "z_score" to "zScore",
        "days_sales_receivables_index" to "daysSalesReceivablesIndex",
        "gross_margin_index" to "grossMarginIndex",
        "asset_quality_index" to "assetQualityIndex",
        "sales_growth_index" to "salesGrowthIndex",
        "depreciation_index" to "depreciationIndex",
        "sg_admin_expenses_index" to "sgaExpensesIndex",
        "leverage_index" to "leverageIndex",
        "total_accruals_to_total_assets" to "totalAccrualsToTotalAssets",
        "m_score" to "mScore",
        "f_score" to "fScore"
    )
    /**
     * Generates feature importance JSON for storage
     */
    private fun generateFeatureImportance(result: PredictionResult, features: Map<String, Any>): String {
        val jsonObject = JSONObject()

        // Add important features
        val importantFeaturesJson = JSONObject()
        for ((feature, importance) in result.importantFeatures) {
            importantFeaturesJson.put(feature, importance)
        }
        jsonObject.put("importantFeatures", importantFeaturesJson)

        // Add metadata
        val metadataJson = JSONObject()
        for ((key, value) in result.metadata) {
            metadataJson.put(key, value)
        }
        jsonObject.put("metadata", metadataJson)

        // Add prediction summary
        jsonObject.put("fraudProbability", result.fraudProbability)
        jsonObject.put("timestamp", OffsetDateTime.now().toString())

        return jsonObject.toString()
    }

    /**
     * Generates a human-readable explanation of the prediction
     */
    private fun generateExplanation(result: PredictionResult, featureImportanceJson: String): String {
        val fraudProbability = result.fraudProbability.toDouble()
        val importantFeatures = result.importantFeatures

        val explanationBuilder = StringBuilder()

        // Add headline
        explanationBuilder.append("Fraud Risk Assessment: ")
        when {
            fraudProbability >= 0.75 -> explanationBuilder.append("High Risk (${fraudProbability * 100}%)")
            fraudProbability >= 0.5 -> explanationBuilder.append("Moderate Risk (${fraudProbability * 100}%)")
            fraudProbability >= 0.25 -> explanationBuilder.append("Low Risk (${fraudProbability * 100}%)")
            else -> explanationBuilder.append("Very Low Risk (${fraudProbability * 100}%)")
        }

        explanationBuilder.append("\n\nKey factors influencing this assessment:\n")

        // Add explanation for top features
        importantFeatures.take(3).forEach { (feature, importance) ->
            val featureName = when (feature) {
                "currentRatio" -> "Current Ratio"
                "quickRatio" -> "Quick Ratio"
                "netProfitMargin" -> "Net Profit Margin"
                "returnOnAssets" -> "Return on Assets"
                "zScore" -> "Altman Z-Score"
                "mScore" -> "Beneish M-Score"
                "fScore" -> "Piotroski F-Score"
                "totalAccrualsToTotalAssets" -> "Total Accruals to Total Assets"
                "salesGrowthIndex" -> "Sales Growth Index"
                else -> feature
            }

            explanationBuilder.append("- $featureName: ")
            explanationBuilder.append(generateFeatureExplanation(feature, importance))
            explanationBuilder.append("\n")
        }

        return explanationBuilder.toString()
    }

    /**
     * Generates explanation for a specific feature
     */
    private fun generateFeatureExplanation(feature: String, importance: Double): String {
        return when (feature) {
            "zScore" -> "The Altman Z-Score indicates ${if (importance > 0.5) "significant" else "some"} financial distress, which correlates with higher fraud risk."
            "mScore" -> "The Beneish M-Score suggests ${if (importance > 0.6) "strong" else "potential"} earnings manipulation."
            "fScore" -> "The Piotroski F-Score shows ${if (importance > 0.5) "poor" else "concerning"} financial health, increasing fraud risk."
            "currentRatio", "quickRatio", "cashRatio" -> "Liquidity ratios indicate ${if (importance > 0.5) "severe" else "potential"} cash flow issues, a potential motive for fraud."
            "netProfitMargin", "returnOnAssets", "returnOnEquity" -> "Profitability metrics show ${if (importance > 0.4) "significant" else "some"} underperformance, which may incentivize financial manipulation."
            "totalAccrualsToTotalAssets" -> "High accruals relative to assets suggest ${if (importance > 0.3) "aggressive" else "questionable"} accounting practices."
            "salesGrowthIndex" -> "Unusual sales growth patterns ${if (importance > 0.2) "strongly indicate" else "suggest"} potential revenue manipulation."
            else -> "This factor shows ${if (importance > 0.5) "significant" else "some"} anomalies compared to industry norms."
        }
    }

    /**
     * Prepares the feature vector from ML features
     */
    private fun prepareFeatureVector(mlFeatures: MlFeatures): Map<String, Any> {
        // Create a map of feature name to value
        val features = mutableMapOf<String, Any>()

        try {
            // Parse the JSON feature set
            val featureSetJson = JSONObject(mlFeatures.featureSet)

            // Extract features from JSON
            for (key in featureSetJson.keys()) {
                val value = featureSetJson.opt(key)
                when (value) {
                    is Number -> features[key] = value.toDouble()
                    is String -> features[key] = value
                    is Boolean -> features[key] = value
                    JSONObject.NULL -> features[key] = 0.0
                    else -> features[key] = value.toString()
                }
            }

            // Default values for any missing keys
            val defaultFeatures = listOf(
                "currentRatio", "quickRatio", "cashRatio", "grossMargin", "operatingMargin",
                "netProfitMargin", "returnOnAssets", "returnOnEquity", "assetTurnover",
                "debtToEquity", "debtRatio", "workingCapitalToTotalAssets",
                "retainedEarningsToTotalAssets", "ebitToTotalAssets",
                "marketValueEquityToTotalLiabilities", "salesToTotalAssets", "zScore",
                "daysSalesReceivablesIndex", "grossMarginIndex", "assetQualityIndex",
                "salesGrowthIndex", "depreciationIndex", "sgaExpensesIndex", "leverageIndex",
                "totalAccrualsToTotalAssets", "mScore", "fScore"
            )

            // Ensure all default features exist (with 0.0 if missing)
            for (feature in defaultFeatures) {
                if (!features.containsKey(feature)) {
                    features[feature] = 0.0
                }
            }

            // Add statement ID as a feature
            features["statementId"] = mlFeatures.statement.id!!

            logger.debug("Prepared feature vector with ${features.size} features")

        } catch (e: Exception) {
            logger.error("Error parsing ML feature set: ${e.message}", e)
            // Provide minimal default features if parsing fails
            features["error"] = true
            features["fallback"] = true
            features["zScore"] = 0.0
            features["mScore"] = 0.0
            features["fScore"] = 0.0
        }

        return features
    }
}
