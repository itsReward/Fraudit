package com.fraudit.fraudit.util

import com.fraudit.fraudit.domain.entity.*
import com.fraudit.fraudit.domain.enum.*
import com.fraudit.fraudit.repository.*
import com.fraudit.fraudit.service.AuditLogService
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.BufferedReader
import java.io.InputStreamReader
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

/**
 * Utility for importing CSV data directly into the database
 */
@Component
class CsvDatabaseImportUtil(
    private val companyRepository: CompanyRepository,
    private val fiscalYearRepository: FiscalYearRepository,
    private val financialStatementRepository: FinancialStatementRepository,
    private val financialDataRepository: FinancialDataRepository,
    private val altmanZScoreRepository: AltmanZScoreRepository,
    private val beneishMScoreRepository: BeneishMScoreRepository,
    private val piotroskiFScoreRepository: PiotroskiFScoreRepository,
    private val financialRatiosRepository: FinancialRatiosRepository,
    private val mlFeaturesRepository: MlFeaturesRepository,
    private val auditLogService: AuditLogService,
    private val userRepository: UserRepository
) {
    private val logger = LoggerFactory.getLogger(CsvDatabaseImportUtil::class.java)

    /**
     * Import synthetic fraud data from CSV and create all necessary database entities
     */
    @Transactional
    fun importSyntheticDataFromCsv(file: MultipartFile, userId: UUID): Map<String, Any> {
        logger.info("Starting synthetic data import from CSV: ${file.originalFilename}")

        // Track stats for the import process
        val stats = mutableMapOf<String, Any>()
        var successCount = 0
        var errorCount = 0
        val errors = mutableListOf<String>()

        try {
            // Find or create system user if userId isn't provided
            val user = userRepository.findById(userId).orElseThrow {
                IllegalArgumentException("User not found with ID: $userId")
            }

            // Parse CSV file
            val reader = BufferedReader(InputStreamReader(file.inputStream))
            val csvParser = CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())

            // Get headers for validation
            val headers = csvParser.headerNames
            logger.info("CSV headers: ${headers.joinToString(", ")}")

            // Process each record
            for ((index, record) in csvParser.withIndex()) {
                try {
                    // Create a unique identifier for this record
                    val recordId = index + 1

                    // 1. Create or get Company
                    val company = getOrCreateCompany(recordId, userId)

                    // 2. Create or get Fiscal Year
                    val fiscalYear = getOrCreateFiscalYear(company, 2024 - (recordId % 5), userId)

                    // 3. Create Financial Statement
                    val statement = createFinancialStatement(fiscalYear, user, userId)

                    // 4. Create Financial Data from CSV row
                    val financialData = createFinancialData(statement, record, userId)

                    // 5. Calculate and create financial metrics
                    calculateFinancialMetrics(statement, financialData, userId)

                    // 6. Create ML Features
                    createMlFeatures(statement, record, userId)

                    successCount++

                    // Log progress every 100 records
                    if (successCount % 100 == 0) {
                        logger.info("Processed $successCount records successfully")
                    }
                } catch (e: Exception) {
                    logger.error("Error processing record at index $index: ${e.message}", e)
                    errors.add("Record $index: ${e.message}")
                    errorCount++
                }
            }

            // Log summary
            logger.info("CSV import completed: $successCount successes, $errorCount errors")

            // Create audit log entry
            auditLogService.logEvent(
                userId = userId,
                action = "IMPORT_CSV",
                entityType = "SYNTHETIC_DATA",
                entityId = "batch",
                details = "Imported synthetic data from CSV: $successCount successes, $errorCount errors"
            )

            // Compile statistics
            stats["fileName"] = file.originalFilename ?: "unknown"
            stats["fileSize"] = file.size
            stats["totalRecords"] = successCount + errorCount
            stats["successCount"] = successCount
            stats["errorCount"] = errorCount
            stats["statements"] = financialStatementRepository.count()
            stats["companies"] = companyRepository.count()
            stats["errors"] = errors.take(20) // Limit to first 20 errors

            return stats
        } catch (e: Exception) {
            logger.error("Critical error during CSV import: ${e.message}", e)
            throw e
        }
    }

    /**
     * Create or get a company for this synthetic record
     */
    private fun getOrCreateCompany(recordId: Int, userId: UUID): Company {
        val companyName = "Synthetic Company $recordId"
        val stockCode = "SYN$recordId"

        // Check if company already exists
        val existingCompany = companyRepository.findByStockCode(stockCode)
        if (existingCompany.isPresent) {
            return existingCompany.get()
        }

        // Create new company
        val company = Company(
            id = null,
            name = companyName,
            stockCode = stockCode,
            sector = getSectorForRecord(recordId),
            listingDate = LocalDate.now().minusYears((recordId % 20).toLong()),
            description = "Synthetic company for fraud detection model training",
            createdAt = OffsetDateTime.now(),
            updatedAt = OffsetDateTime.now()
        )

        return companyRepository.save(company)
    }

    /**
     * Get a realistic sector based on record ID
     */
    private fun getSectorForRecord(recordId: Int): String {
        val sectors = listOf(
            "Technology", "Financial Services", "Healthcare", "Consumer Goods",
            "Industrial", "Energy", "Materials", "Utilities", "Real Estate", "Communication Services"
        )
        return sectors[recordId % sectors.size]
    }

    /**
     * Create or get a fiscal year for this company
     */
    private fun getOrCreateFiscalYear(company: Company, year: Int, userId: UUID): FiscalYear {
        // Check if fiscal year already exists
        val existingFiscalYear = fiscalYearRepository.findByCompanyAndYear(company, year)
        if (existingFiscalYear.isPresent) {
            return existingFiscalYear.get()
        }

        // Create new fiscal year
        val fiscalYear = FiscalYear(
            id = null,
            company = company,
            year = year,
            startDate = LocalDate.of(year, 1, 1),
            endDate = LocalDate.of(year, 12, 31),
            isAudited = true,
            createdAt = OffsetDateTime.now()
        )

        return fiscalYearRepository.save(fiscalYear)
    }

    /**
     * Create a financial statement for this fiscal year
     */
    private fun createFinancialStatement(fiscalYear: FiscalYear, user: User, userId: UUID): FinancialStatement {
        // Create financial statement
        val statement = FinancialStatement(
            id = null,
            fiscalYear = fiscalYear,
            user = user,
            statementType = StatementType.ANNUAL,
            period = null,
            uploadDate = OffsetDateTime.now(),
            status = StatementStatus.PENDING
        )

        return financialStatementRepository.save(statement)
    }

    /**
     * Create financial data from CSV record
     */
    private fun createFinancialData(statement: FinancialStatement, record: org.apache.commons.csv.CSVRecord, userId: UUID): FinancialData {
        // Extract financial data from CSV
        // This mapping depends on your CSV structure
        val financialData = FinancialData(
            id = null,
            statement = statement,

            // Income Statement data
            revenue = getBigDecimalFromRecord(record, "revenue", 1000000),
            costOfSales = getBigDecimalFromRecord(record, "cost_of_sales", 600000),
            grossProfit = getBigDecimalFromRecord(record, "gross_profit", 400000),
            operatingExpenses = getBigDecimalFromRecord(record, "operating_expenses", 200000),
            administrativeExpenses = getBigDecimalFromRecord(record, "administrative_expenses", 100000),
            sellingExpenses = getBigDecimalFromRecord(record, "selling_expenses", 50000),
            depreciation = getBigDecimalFromRecord(record, "depreciation", 30000),
            amortization = getBigDecimalFromRecord(record, "amortization", 20000),
            operatingIncome = getBigDecimalFromRecord(record, "operating_income", 200000),
            interestExpense = getBigDecimalFromRecord(record, "interest_expense", 25000),
            otherIncome = getBigDecimalFromRecord(record, "other_income", 10000),
            earningsBeforeTax = getBigDecimalFromRecord(record, "earnings_before_tax", 185000),
            incomeTax = getBigDecimalFromRecord(record, "income_tax", 55000),
            netIncome = getBigDecimalFromRecord(record, "net_income", 130000),

            // Balance Sheet - Assets
            cash = getBigDecimalFromRecord(record, "cash", 200000),
            shortTermInvestments = getBigDecimalFromRecord(record, "short_term_investments", 100000),
            accountsReceivable = getBigDecimalFromRecord(record, "accounts_receivable", 150000),
            inventory = getBigDecimalFromRecord(record, "inventory", 300000),
            otherCurrentAssets = getBigDecimalFromRecord(record, "other_current_assets", 50000),
            totalCurrentAssets = getBigDecimalFromRecord(record, "total_current_assets", 800000),
            propertyPlantEquipment = getBigDecimalFromRecord(record, "property_plant_equipment", 500000),
            accumulatedDepreciation = getBigDecimalFromRecord(record, "accumulated_depreciation", 150000),
            intangibleAssets = getBigDecimalFromRecord(record, "intangible_assets", 200000),
            longTermInvestments = getBigDecimalFromRecord(record, "long_term_investments", 100000),
            otherNonCurrentAssets = getBigDecimalFromRecord(record, "other_non_current_assets", 50000),
            totalNonCurrentAssets = getBigDecimalFromRecord(record, "total_non_current_assets", 700000),
            totalAssets = getBigDecimalFromRecord(record, "total_assets", 1500000),

            // Balance Sheet - Liabilities
            accountsPayable = getBigDecimalFromRecord(record, "accounts_payable", 100000),
            shortTermDebt = getBigDecimalFromRecord(record, "short_term_debt", 50000),
            accruedLiabilities = getBigDecimalFromRecord(record, "accrued_liabilities", 75000),
            otherCurrentLiabilities = getBigDecimalFromRecord(record, "other_current_liabilities", 25000),
            totalCurrentLiabilities = getBigDecimalFromRecord(record, "total_current_liabilities", 250000),
            longTermDebt = getBigDecimalFromRecord(record, "long_term_debt", 400000),
            deferredTaxes = getBigDecimalFromRecord(record, "deferred_taxes", 50000),
            otherNonCurrentLiabilities = getBigDecimalFromRecord(record, "other_non_current_liabilities", 50000),
            totalNonCurrentLiabilities = getBigDecimalFromRecord(record, "total_non_current_liabilities", 500000),
            totalLiabilities = getBigDecimalFromRecord(record, "total_liabilities", 750000),

            // Balance Sheet - Equity
            commonStock = getBigDecimalFromRecord(record, "common_stock", 100000),
            additionalPaidInCapital = getBigDecimalFromRecord(record, "additional_paid_in_capital", 200000),
            retainedEarnings = getBigDecimalFromRecord(record, "retained_earnings", 450000),
            treasuryStock = getBigDecimalFromRecord(record, "treasury_stock", 0),
            otherEquity = getBigDecimalFromRecord(record, "other_equity", 0),
            totalEquity = getBigDecimalFromRecord(record, "total_equity", 750000),

            // Cash Flow
            netCashFromOperating = getBigDecimalFromRecord(record, "net_cash_from_operating", 180000),
            netCashFromInvesting = getBigDecimalFromRecord(record, "net_cash_from_investing", -120000),
            netCashFromFinancing = getBigDecimalFromRecord(record, "net_cash_from_financing", -30000),
            netChangeInCash = getBigDecimalFromRecord(record, "net_change_in_cash", 30000),

            // Market Data
            marketCapitalization = getBigDecimalFromRecord(record, "market_capitalization", 2000000),
            sharesOutstanding = getBigDecimalFromRecord(record, "shares_outstanding", 1000000),
            marketPricePerShare = getBigDecimalFromRecord(record, "market_price_per_share", 2),
            bookValuePerShare = getBigDecimalFromRecord(record, "book_value_per_share", 1),
            earningsPerShare = getBigDecimalFromRecord(record, "earnings_per_share", 1),

            // Growth Metrics
            revenueGrowth = getOptionalBigDecimalFromRecord(record, "revenue_growth"),
            grossProfitGrowth = getOptionalBigDecimalFromRecord(record, "gross_profit_growth"),
            netIncomeGrowth = getOptionalBigDecimalFromRecord(record, "net_income_growth"),
            assetGrowth = getOptionalBigDecimalFromRecord(record, "asset_growth"),
            receivablesGrowth = getOptionalBigDecimalFromRecord(record, "receivables_growth"),
            inventoryGrowth = getOptionalBigDecimalFromRecord(record, "inventory_growth"),
            liabilityGrowth = getOptionalBigDecimalFromRecord(record, "liability_growth"),

            // Metadata
            createdAt = OffsetDateTime.now(),
            updatedAt = null
        )

        // Save to database
        statement.status = StatementStatus.PROCESSED
        financialStatementRepository.save(statement)
        return financialDataRepository.save(financialData)
    }

    /**
     * Calculate and create financial metrics based on financial data
     */
    private fun calculateFinancialMetrics(statement: FinancialStatement, financialData: FinancialData, userId: UUID) {
        // 1. Calculate Financial Ratios
        val financialRatios = FinancialRatios(
            id = null,
            statement = statement,
            currentRatio = calculateRatio(financialData.totalCurrentAssets, financialData.totalCurrentLiabilities),
            quickRatio = calculateRatio(
                financialData.totalCurrentAssets?.minus(financialData.inventory ?: BigDecimal.ZERO),
                financialData.totalCurrentLiabilities
            ),
            cashRatio = calculateRatio(financialData.cash, financialData.totalCurrentLiabilities),
            grossMargin = calculateRatio(financialData.grossProfit, financialData.revenue),
            operatingMargin = calculateRatio(financialData.operatingIncome, financialData.revenue),
            netProfitMargin = calculateRatio(financialData.netIncome, financialData.revenue),
            returnOnAssets = calculateRatio(financialData.netIncome, financialData.totalAssets),
            returnOnEquity = calculateRatio(financialData.netIncome, financialData.totalEquity),
            assetTurnover = calculateRatio(financialData.revenue, financialData.totalAssets),
            inventoryTurnover = calculateRatio(financialData.costOfSales, financialData.inventory),
            accountsReceivableTurnover = calculateRatio(financialData.revenue, financialData.accountsReceivable),
            daysSalesOutstanding = calculateDSO(financialData.accountsReceivable, financialData.revenue),
            debtToEquity = calculateRatio(financialData.totalLiabilities, financialData.totalEquity),
            debtRatio = calculateRatio(financialData.totalLiabilities, financialData.totalAssets),
            interestCoverage = calculateRatio(financialData.operatingIncome, financialData.interestExpense),
            accrualRatio = calculateAccrualRatio(financialData),
            earningsQuality = calculateEarningsQuality(financialData),
            calculatedAt = OffsetDateTime.now()
        )
        financialRatiosRepository.save(financialRatios)

        // 2. Calculate Altman Z-Score
        val altmanZScore = AltmanZScore(
            id = null,
            statement = statement,
            workingCapitalToTotalAssets = calculateRatio(
                financialData.totalCurrentAssets?.minus(financialData.totalCurrentLiabilities ?: BigDecimal.ZERO),
                financialData.totalAssets
            ),
            retainedEarningsToTotalAssets = calculateRatio(financialData.retainedEarnings, financialData.totalAssets),
            ebitToTotalAssets = calculateRatio(
                financialData.earningsBeforeTax?.plus(financialData.interestExpense ?: BigDecimal.ZERO),
                financialData.totalAssets
            ),
            marketValueEquityToBookValueDebt = calculateRatio(
                financialData.marketCapitalization,
                financialData.totalLiabilities
            ),
            salesToTotalAssets = calculateRatio(financialData.revenue, financialData.totalAssets),
            zScore = calculateZScore(financialData),
            riskCategory = determineZScoreRiskCategory(calculateZScore(financialData)),
            calculatedAt = OffsetDateTime.now()
        )
        altmanZScoreRepository.save(altmanZScore)

        // 3. Calculate Beneish M-Score
        val beneishMScore = BeneishMScore(
            id = null,
            statement = statement,
            daysSalesReceivablesIndex = BigDecimal("1.1"), // Placeholder
            grossMarginIndex = BigDecimal("0.95"), // Placeholder
            assetQualityIndex = BigDecimal("1.05"), // Placeholder
            salesGrowthIndex = BigDecimal("1.2"), // Placeholder
            depreciationIndex = BigDecimal("1.0"), // Placeholder
            sgAdminExpensesIndex = BigDecimal("1.0"), // Placeholder
            leverageIndex = BigDecimal("1.0"), // Placeholder
            totalAccrualsToTotalAssets = calculateAccrualRatio(financialData),
            mScore = calculateMScore(financialData), // Simplified
            manipulationProbability = determineMScoreManipulationProbability(calculateMScore(financialData)),
            calculatedAt = OffsetDateTime.now()
        )
        beneishMScoreRepository.save(beneishMScore)

        // 4. Calculate Piotroski F-Score
        val piotroskiFScore = PiotroskiFScore(
            id = null,
            statement = statement,
            positiveNetIncome = financialData.netIncome?.compareTo(BigDecimal.ZERO) ?: 0 > 0,
            positiveOperatingCashFlow = financialData.netCashFromOperating?.compareTo(BigDecimal.ZERO) ?: 0 > 0,
            cashFlowGreaterThanNetIncome = (financialData.netCashFromOperating?.compareTo(financialData.netIncome ?: BigDecimal.ZERO) ?: 0) > 0,
            improvingRoa = true, // Placeholder
            decreasingLeverage = true, // Placeholder
            improvingCurrentRatio = true, // Placeholder
            noNewShares = true, // Placeholder
            improvingGrossMargin = true, // Placeholder
            improvingAssetTurnover = true, // Placeholder
            fScore = calculateFScore(financialData), // Simplified
            financialStrength = determineFScoreFinancialStrength(calculateFScore(financialData)),
            calculatedAt = OffsetDateTime.now()
        )
        piotroskiFScoreRepository.save(piotroskiFScore)
    }

    /**
     * Create ML Features from CSV data for training
     */
    private fun createMlFeatures(statement: FinancialStatement, record: org.apache.commons.csv.CSVRecord, userId: UUID) {
        // Get individual components
        val altmanZScore = altmanZScoreRepository.findByStatementId(statement.id!!)
            ?: throw IllegalStateException("Z-Score not found for statement: ${statement.id}")

        val beneishMScore = beneishMScoreRepository.findByStatementId(statement.id!!)
            ?: throw IllegalStateException("M-Score not found for statement: ${statement.id}")

        val piotroskiFScore = piotroskiFScoreRepository.findByStatementId(statement.id!!)
            ?: throw IllegalStateException("F-Score not found for statement: ${statement.id}")

        val financialRatios = financialRatiosRepository.findByStatementId(statement.id!!)
            ?: throw IllegalStateException("Financial ratios not found for statement: ${statement.id}")

        // Create feature JSON
        val featuresJson = JSONObject()

        // Add financial ratios
        featuresJson.put("current_ratio", financialRatios.currentRatio?.toDouble() ?: 0.0)
        featuresJson.put("quick_ratio", financialRatios.quickRatio?.toDouble() ?: 0.0)
        featuresJson.put("cash_ratio", financialRatios.cashRatio?.toDouble() ?: 0.0)
        featuresJson.put("gross_margin", financialRatios.grossMargin?.toDouble() ?: 0.0)
        featuresJson.put("operating_margin", financialRatios.operatingMargin?.toDouble() ?: 0.0)
        featuresJson.put("net_profit_margin", financialRatios.netProfitMargin?.toDouble() ?: 0.0)
        featuresJson.put("return_on_assets", financialRatios.returnOnAssets?.toDouble() ?: 0.0)
        featuresJson.put("return_on_equity", financialRatios.returnOnEquity?.toDouble() ?: 0.0)
        featuresJson.put("asset_turnover", financialRatios.assetTurnover?.toDouble() ?: 0.0)
        featuresJson.put("inventory_turnover", financialRatios.inventoryTurnover?.toDouble() ?: 0.0)
        featuresJson.put("accounts_receivable_turnover", financialRatios.accountsReceivableTurnover?.toDouble() ?: 0.0)
        featuresJson.put("days_sales_outstanding", financialRatios.daysSalesOutstanding?.toDouble() ?: 0.0)
        featuresJson.put("debt_to_equity", financialRatios.debtToEquity?.toDouble() ?: 0.0)
        featuresJson.put("debt_ratio", financialRatios.debtRatio?.toDouble() ?: 0.0)
        featuresJson.put("interest_coverage", financialRatios.interestCoverage?.toDouble() ?: 0.0)
        featuresJson.put("accrual_ratio", financialRatios.accrualRatio?.toDouble() ?: 0.0)
        featuresJson.put("earnings_quality", financialRatios.earningsQuality?.toDouble() ?: 0.0)

        // Add Z-Score components
        featuresJson.put("working_capital_to_total_assets", altmanZScore.workingCapitalToTotalAssets?.toDouble() ?: 0.0)
        featuresJson.put("retained_earnings_to_total_assets", altmanZScore.retainedEarningsToTotalAssets?.toDouble() ?: 0.0)
        featuresJson.put("ebit_to_total_assets", altmanZScore.ebitToTotalAssets?.toDouble() ?: 0.0)
        featuresJson.put("market_value_equity_to_book_value_debt", altmanZScore.marketValueEquityToBookValueDebt?.toDouble() ?: 0.0)
        featuresJson.put("sales_to_total_assets", altmanZScore.salesToTotalAssets?.toDouble() ?: 0.0)
        featuresJson.put("z_score", altmanZScore.zScore?.toDouble() ?: 0.0)

        // Add M-Score components
        featuresJson.put("days_sales_receivables_index", beneishMScore.daysSalesReceivablesIndex?.toDouble() ?: 0.0)
        featuresJson.put("gross_margin_index", beneishMScore.grossMarginIndex?.toDouble() ?: 0.0)
        featuresJson.put("asset_quality_index", beneishMScore.assetQualityIndex?.toDouble() ?: 0.0)
        featuresJson.put("sales_growth_index", beneishMScore.salesGrowthIndex?.toDouble() ?: 0.0)
        featuresJson.put("depreciation_index", beneishMScore.depreciationIndex?.toDouble() ?: 0.0)
        featuresJson.put("sg_admin_expenses_index", beneishMScore.sgAdminExpensesIndex?.toDouble() ?: 0.0)
        featuresJson.put("leverage_index", beneishMScore.leverageIndex?.toDouble() ?: 0.0)
        featuresJson.put("total_accruals_to_total_assets", beneishMScore.totalAccrualsToTotalAssets?.toDouble() ?: 0.0)
        featuresJson.put("m_score", beneishMScore.mScore?.toDouble() ?: 0.0)

        // Add F-Score components
        featuresJson.put("positive_net_income", piotroskiFScore.positiveNetIncome ?: false)
        featuresJson.put("positive_operating_cash_flow", piotroskiFScore.positiveOperatingCashFlow ?: false)
        featuresJson.put("cash_flow_greater_than_net_income", piotroskiFScore.cashFlowGreaterThanNetIncome ?: false)
        featuresJson.put("improving_roa", piotroskiFScore.improvingRoa ?: false)
        featuresJson.put("decreasing_leverage", piotroskiFScore.decreasingLeverage ?: false)
        featuresJson.put("improving_current_ratio", piotroskiFScore.improvingCurrentRatio ?: false)
        featuresJson.put("no_new_shares", piotroskiFScore.noNewShares ?: false)
        featuresJson.put("improving_gross_margin", piotroskiFScore.improvingGrossMargin ?: false)
        featuresJson.put("improving_asset_turnover", piotroskiFScore.improvingAssetTurnover ?: false)
        featuresJson.put("f_score", piotroskiFScore.fScore ?: 0)

        // Add fraud label from CSV
        if (record.isMapped("fraud")) {
            try {
                val fraudValue = record.get("fraud").toIntOrNull() ?: 0
                featuresJson.put("fraud", fraudValue)
            } catch (e: Exception) {
                // Default to non-fraud if not available or invalid
                featuresJson.put("fraud", 0)
            }
        } else {
            // For synthetic data, determine fraud based on known indicators
            val isFraud = (beneishMScore.mScore?.toDouble() ?: -3.0) > -1.78 ||
                    (altmanZScore.zScore?.toDouble() ?: 3.0) < 1.8
            featuresJson.put("fraud", if (isFraud) 1 else 0)
        }

        // Create and save ML Features entity
        val mlFeatures = MlFeatures(
            id = null,
            statement = statement,
            featureSet = featuresJson.toString(),
            createdAt = OffsetDateTime.now()
        )

        mlFeaturesRepository.save(mlFeatures)
    }

    // Helper methods

    private fun getBigDecimalFromRecord(record: org.apache.commons.csv.CSVRecord, fieldName: String, defaultValue: Int): BigDecimal {
        return try {
            if (record.isMapped(fieldName)) {
                val value = record.get(fieldName).toDoubleOrNull()
                if (value != null) BigDecimal.valueOf(value) else BigDecimal(defaultValue)
            } else {
                BigDecimal(defaultValue)
            }
        } catch (e: Exception) {
            BigDecimal(defaultValue)
        }
    }

    private fun getOptionalBigDecimalFromRecord(record: org.apache.commons.csv.CSVRecord, fieldName: String): BigDecimal? {
        return try {
            if (record.isMapped(fieldName)) {
                val value = record.get(fieldName).toDoubleOrNull()
                if (value != null) BigDecimal.valueOf(value) else null
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateRatio(numerator: BigDecimal?, denominator: BigDecimal?): BigDecimal? {
        if (numerator == null || denominator == null || denominator == BigDecimal.ZERO) {
            return null
        }
        return numerator.divide(denominator, 4, BigDecimal.ROUND_HALF_UP)
    }

    private fun calculateDSO(receivables: BigDecimal?, revenue: BigDecimal?): BigDecimal? {
        if (receivables == null || revenue == null || revenue == BigDecimal.ZERO) {
            return null
        }
        // DSO = (Accounts Receivable / Annual Revenue) * 365
        return receivables.multiply(BigDecimal(365)).divide(revenue, 2, BigDecimal.ROUND_HALF_UP)
    }

    private fun calculateAccrualRatio(financialData: FinancialData): BigDecimal? {
        // Simplified accrual ratio calculation
        if (financialData.netIncome == null || financialData.netCashFromOperating == null || financialData.totalAssets == null || financialData.totalAssets == BigDecimal.ZERO) {
            return null
        }

        val accruals = financialData.netIncome!!.subtract(financialData.netCashFromOperating)
        return accruals.divide(financialData.totalAssets, 4, BigDecimal.ROUND_HALF_UP)
    }

    private fun calculateEarningsQuality(financialData: FinancialData): BigDecimal? {
        // Earnings quality = Cash flow from operations / Net income
        if (financialData.netCashFromOperating == null || financialData.netIncome == null || financialData.netIncome == BigDecimal.ZERO) {
            return null
        }

        return financialData.netCashFromOperating!!.divide(financialData.netIncome, 4, BigDecimal.ROUND_HALF_UP)
    }

    private fun calculateZScore(financialData: FinancialData): BigDecimal? {
        // Simplified Z-Score calculation
        // Z-Score = 1.2*X1 + 1.4*X2 + 3.3*X3 + 0.6*X4 + 1.0*X5
        if (financialData.totalAssets == null || financialData.totalAssets == BigDecimal.ZERO) {
            return null
        }

        val workingCapital = financialData.totalCurrentAssets?.minus(financialData.totalCurrentLiabilities ?: BigDecimal.ZERO)
            ?: BigDecimal.ZERO
        val retainedEarnings = financialData.retainedEarnings ?: BigDecimal.ZERO
        val ebit = financialData.earningsBeforeTax?.plus(financialData.interestExpense ?: BigDecimal.ZERO) ?: BigDecimal.ZERO
        val marketValue = financialData.marketCapitalization ?: BigDecimal.ZERO
        val sales = financialData.revenue ?: BigDecimal.ZERO
        val totalDebt = financialData.totalLiabilities ?: BigDecimal.ONE

        val x1 = workingCapital.divide(financialData.totalAssets, 4, BigDecimal.ROUND_HALF_UP)
        val x2 = retainedEarnings.divide(financialData.totalAssets, 4, BigDecimal.ROUND_HALF_UP)
        val x3 = ebit.divide(financialData.totalAssets, 4, BigDecimal.ROUND_HALF_UP)
        val x4 = marketValue.divide(totalDebt, 4, BigDecimal.ROUND_HALF_UP)
        val x5 = sales.divide(financialData.totalAssets, 4, BigDecimal.ROUND_HALF_UP)

        return BigDecimal("1.2").multiply(x1)
            .add(BigDecimal("1.4").multiply(x2))
            .add(BigDecimal("3.3").multiply(x3))
            .add(BigDecimal("0.6").multiply(x4))
            .add(x5)
    }

    private fun determineZScoreRiskCategory(zScore: BigDecimal?): RiskCategory? {
        if (zScore == null) return null

        return when {
            zScore < BigDecimal("1.8") -> RiskCategory.SAFE
            zScore < BigDecimal("3.0") -> RiskCategory.GREY
            else -> RiskCategory.DISTRESS
        }
    }

    private fun calculateMScore(financialData: FinancialData): BigDecimal? {
        // Simplified M-Score calculation (normally based on 8 variables)
        // Here we'll use a very simplified approach based on accruals
        val accrualRatio = calculateAccrualRatio(financialData) ?: return null

        // Higher accrual ratio increases M-Score
        val baseScore = BigDecimal("-2.22") // Base score
        val accrualFactor = BigDecimal("5.0").multiply(accrualRatio) // Impact of accruals

        // Add random variation for synthetic data
        val randomVariation = BigDecimal((Math.random() * 0.5 - 0.25).toString())

        return baseScore.add(accrualFactor).add(randomVariation)
    }

    private fun determineMScoreManipulationProbability(mScore: BigDecimal?): ManipulationProbability? {
        if (mScore == null) return null

        return when {
            mScore > BigDecimal("-1.78") -> ManipulationProbability.HIGH
            mScore > BigDecimal("-2.22") -> ManipulationProbability.MEDIUM
            else -> ManipulationProbability.LOW
        }
    }

    private fun calculateFScore(financialData: FinancialData): Int {
        // Simplified F-Score calculation (0-9 score)
        var score = 0

        // Profitability criteria
        if (financialData.netIncome?.compareTo(BigDecimal.ZERO) ?: 0 > 0) score++
        if (financialData.netCashFromOperating?.compareTo(BigDecimal.ZERO) ?: 0 > 0) score++
        if (financialData.netCashFromOperating?.compareTo(financialData.netIncome ?: BigDecimal.ZERO) ?: 0 > 0) score++

        // For synthetic data, add random points to remaining criteria
        score += (Math.random() * 6).toInt()

        return score
    }

    private fun determineFScoreFinancialStrength(fScore: Int): FinancialStrength? {
        return when {
            fScore >= 7 -> FinancialStrength.STRONG
            fScore >= 4 -> FinancialStrength.MODERATE
            else -> FinancialStrength.WEAK
        }
    }
}