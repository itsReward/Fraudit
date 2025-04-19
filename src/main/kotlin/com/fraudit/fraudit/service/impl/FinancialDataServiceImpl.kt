package com.fraudit.fraudit.service.impl

import com.fraudit.fraudit.domain.entity.FinancialData
import com.fraudit.fraudit.domain.entity.FinancialStatement
import com.fraudit.fraudit.domain.enum.StatementStatus
import com.fraudit.fraudit.repository.FinancialDataRepository
import com.fraudit.fraudit.repository.FinancialStatementRepository
import com.fraudit.fraudit.service.AuditLogService
import com.fraudit.fraudit.service.FinancialDataService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.OffsetDateTime
import java.util.UUID
import jakarta.persistence.EntityNotFoundException

@Service
class FinancialDataServiceImpl(
    private val financialDataRepository: FinancialDataRepository,
    private val financialStatementRepository: FinancialStatementRepository,
    private val auditLogService: AuditLogService
) : FinancialDataService {
    private val logger = LoggerFactory.getLogger(FinancialDataServiceImpl::class.java)

    override fun findAll(): List<FinancialData> = financialDataRepository.findAll()

    override fun findAllPaged(pageable: Pageable): Page<FinancialData> = financialDataRepository.findAll(pageable)

    override fun findById(id: Long): FinancialData = financialDataRepository.findById(id)
        .orElseThrow { EntityNotFoundException("Financial data not found with id: $id") }

    override fun findByStatementId(statementId: Long): FinancialData? = financialDataRepository.findByStatementId(statementId)

    override fun findLatestByCompanyId(companyId: Long): List<FinancialData> =
        financialDataRepository.findLatestByCompanyId(companyId)

    override fun findByCompanyId(companyId: Long, pageable: Pageable): Page<FinancialData> =
        financialDataRepository.findByStatementFiscalYearCompanyId(companyId, pageable)

    @Transactional
    override fun createFinancialData(financialData: FinancialData, userId: UUID): FinancialData {
        // Check if financial data already exists for this statement
        val existingData = financialDataRepository.findByStatementId(financialData.statement.id!!)
        if (existingData != null) {
            throw IllegalStateException("Financial data already exists for statement id: ${financialData.statement.id}")
        }

        // Calculate derived values if not provided
        val enrichedData = calculateDerivedValuesInternal(financialData)

        // Update statement status to PROCESSED
        updateStatementStatus(enrichedData.statement.id!!, StatementStatus.PROCESSED)

        val savedData = financialDataRepository.save(enrichedData)

        // Log the creation event
        auditLogService.logEvent(
            userId = userId,
            action = "CREATE",
            entityType = "FINANCIAL_DATA",
            entityId = savedData.id.toString(),
            details = "Created financial data for statement id: ${financialData.statement.id}"
        )

        return savedData
    }

    @Transactional
    override fun updateFinancialData(financialData: FinancialData, userId: UUID): FinancialData {
        // Ensure the financial data exists
        findById(financialData.id!!)

        // Calculate derived values if not provided
        val enrichedData = calculateDerivedValuesInternal(financialData)

        val savedData = financialDataRepository.save(enrichedData)

        // Log the update event
        auditLogService.logEvent(
            userId = userId,
            action = "UPDATE",
            entityType = "FINANCIAL_DATA",
            entityId = savedData.id.toString(),
            details = "Updated financial data for statement id: ${financialData.statement.id}"
        )

        return savedData
    }

    @Transactional
    override fun deleteFinancialData(id: Long, userId: UUID) {
        val financialData = findById(id)

        // Update statement status to PENDING
        updateStatementStatus(financialData.statement.id!!, StatementStatus.PENDING)

        financialDataRepository.delete(financialData)

        // Log the deletion event
        auditLogService.logEvent(
            userId = userId,
            action = "DELETE",
            entityType = "FINANCIAL_DATA",
            entityId = id.toString(),
            details = "Deleted financial data for statement id: ${financialData.statement.id}"
        )
    }

    @Transactional
    override fun calculateDerivedValues(id: Long, userId: UUID): FinancialData {
        val financialData = findById(id)

        val enrichedData = calculateDerivedValuesInternal(financialData)

        val savedData = financialDataRepository.save(enrichedData)

        // Log the calculation event
        auditLogService.logEvent(
            userId = userId,
            action = "CALCULATE",
            entityType = "FINANCIAL_DATA",
            entityId = savedData.id.toString(),
            details = "Calculated derived values for financial data with id: $id"
        )

        return savedData
    }

    @Transactional
    override fun calculateGrowthRates(statementId: Long, userId: UUID): FinancialData {
        // Get current statement and financial data
        val currentStatement = financialStatementRepository.findById(statementId)
            .orElseThrow { EntityNotFoundException("Financial statement not found with id: $statementId") }

        val currentData = financialDataRepository.findByStatementId(statementId)
            ?: throw EntityNotFoundException("Financial data not found for statement id: $statementId")

        // Get company ID, year, and statement type to find previous statement
        val companyId = currentStatement.fiscalYear.company.id!!
        val currentYear = currentStatement.fiscalYear.year
        val statementType = currentStatement.statementType

        // Find previous year statements of the same type
        val previousStatements = financialStatementRepository.findByFiscalYearCompanyIdAndStatementType(
            companyId, statementType)
            .filter { it.fiscalYear.year < currentYear }
            .sortedByDescending { it.fiscalYear.year }

        if (previousStatements.isEmpty()) {
            // No previous statements found, return current data with null growth rates
            return currentData
        }

        // Get previous year's financial data
        val previousStatementId = previousStatements.first().id!!
        val previousData = financialDataRepository.findByStatementId(previousStatementId)
            ?: return currentData  // No previous data, can't calculate growth

        // Calculate growth rates
        val revenueGrowth = calculateGrowthRate(currentData.revenue, previousData.revenue)
        val grossProfitGrowth = calculateGrowthRate(currentData.grossProfit, previousData.grossProfit)
        val netIncomeGrowth = calculateGrowthRate(currentData.netIncome, previousData.netIncome)
        val assetGrowth = calculateGrowthRate(currentData.totalAssets, previousData.totalAssets)
        val receivablesGrowth = calculateGrowthRate(currentData.accountsReceivable, previousData.accountsReceivable)
        val inventoryGrowth = calculateGrowthRate(currentData.inventory, previousData.inventory)
        val liabilityGrowth = calculateGrowthRate(currentData.totalLiabilities, previousData.totalLiabilities)

        // Update data with growth rates
        val updatedData = currentData.copy(
            revenueGrowth = revenueGrowth,
            grossProfitGrowth = grossProfitGrowth,
            netIncomeGrowth = netIncomeGrowth,
            assetGrowth = assetGrowth,
            receivablesGrowth = receivablesGrowth,
            inventoryGrowth = inventoryGrowth,
            liabilityGrowth = liabilityGrowth
        )

        val savedData = financialDataRepository.save(updatedData)

        // Log the calculation event
        auditLogService.logEvent(
            userId = userId,
            action = "CALCULATE_GROWTH",
            entityType = "FINANCIAL_DATA",
            entityId = savedData.id.toString(),
            details = "Calculated growth rates for financial data with id: ${currentData.id}"
        )

        return savedData
    }

    /**
     * Calculate growth rate between current and previous values
     */
    private fun calculateGrowthRate(current: BigDecimal?, previous: BigDecimal?): BigDecimal? {
        if (current == null || previous == null || previous == BigDecimal.ZERO) {
            return null
        }

        return try {
            current.subtract(previous)
                .divide(previous.abs(), 4, RoundingMode.HALF_UP)
        } catch (e: Exception) {
            logger.warn("Error calculating growth rate: ${e.message}")
            null
        }
    }

    /**
     * Update the status of a financial statement
     */
    private fun updateStatementStatus(statementId: Long, status: StatementStatus) {
        val statement = financialStatementRepository.findById(statementId)
            .orElseThrow { EntityNotFoundException("Financial statement not found with id: $statementId") }

        val updatedStatement = statement.copy(status = status)
        financialStatementRepository.save(updatedStatement)
    }

    /**
     * Calculate derived values from provided financial data
     */
    private fun calculateDerivedValuesInternal(data: FinancialData): FinancialData {
        try {
            // 1. Calculate Gross Profit if not provided
            var grossProfit = data.grossProfit
            if (grossProfit == null && data.revenue != null && data.costOfSales != null) {
                grossProfit = data.revenue!!.subtract(data.costOfSales)
            }

            // 2. Calculate Operating Income if not provided
            var operatingIncome = data.operatingIncome
            if (operatingIncome == null && grossProfit != null && data.operatingExpenses != null) {
                operatingIncome = grossProfit.subtract(data.operatingExpenses)
            }

            // 3. Calculate Earnings Before Tax if not provided
            var earningsBeforeTax = data.earningsBeforeTax
            if (earningsBeforeTax == null && operatingIncome != null) {
                earningsBeforeTax = operatingIncome
                    .subtract(data.interestExpense ?: BigDecimal.ZERO)
                    .add(data.otherIncome ?: BigDecimal.ZERO)
            }

            // 4. Calculate Net Income if not provided
            var netIncome = data.netIncome
            if (netIncome == null && earningsBeforeTax != null && data.incomeTax != null) {
                netIncome = earningsBeforeTax.subtract(data.incomeTax)
            }

            // 5. Calculate Total Current Assets if not provided
            var totalCurrentAssets = data.totalCurrentAssets
            if (totalCurrentAssets == null) {
                val sum = BigDecimal.ZERO
                    .add(data.cash ?: BigDecimal.ZERO)
                    .add(data.shortTermInvestments ?: BigDecimal.ZERO)
                    .add(data.accountsReceivable ?: BigDecimal.ZERO)
                    .add(data.inventory ?: BigDecimal.ZERO)
                    .add(data.otherCurrentAssets ?: BigDecimal.ZERO)

                totalCurrentAssets = if (sum > BigDecimal.ZERO) sum else null
            }

            // 6. Calculate Total Non-Current Assets if not provided
            var totalNonCurrentAssets = data.totalNonCurrentAssets
            if (totalNonCurrentAssets == null) {
                var ppe = data.propertyPlantEquipment ?: BigDecimal.ZERO
                val accumulatedDepreciation = data.accumulatedDepreciation ?: BigDecimal.ZERO

                // If accumulated depreciation is stored as a positive value, subtract it
                if (accumulatedDepreciation > BigDecimal.ZERO) {
                    ppe = ppe.subtract(accumulatedDepreciation)
                }

                val sum = BigDecimal.ZERO
                    .add(ppe)
                    .add(data.intangibleAssets ?: BigDecimal.ZERO)
                    .add(data.longTermInvestments ?: BigDecimal.ZERO)
                    .add(data.otherNonCurrentAssets ?: BigDecimal.ZERO)

                totalNonCurrentAssets = if (sum > BigDecimal.ZERO) sum else null
            }

            // 7. Calculate Total Assets if not provided
            var totalAssets = data.totalAssets
            if (totalAssets == null && totalCurrentAssets != null && totalNonCurrentAssets != null) {
                totalAssets = totalCurrentAssets.add(totalNonCurrentAssets)
            }

            // 8. Calculate Total Current Liabilities if not provided
            var totalCurrentLiabilities = data.totalCurrentLiabilities
            if (totalCurrentLiabilities == null) {
                val sum = BigDecimal.ZERO
                    .add(data.accountsPayable ?: BigDecimal.ZERO)
                    .add(data.shortTermDebt ?: BigDecimal.ZERO)
                    .add(data.accruedLiabilities ?: BigDecimal.ZERO)
                    .add(data.otherCurrentLiabilities ?: BigDecimal.ZERO)

                totalCurrentLiabilities = if (sum > BigDecimal.ZERO) sum else null
            }

            // 9. Calculate Total Non-Current Liabilities if not provided
            var totalNonCurrentLiabilities = data.totalNonCurrentLiabilities
            if (totalNonCurrentLiabilities == null) {
                val sum = BigDecimal.ZERO
                    .add(data.longTermDebt ?: BigDecimal.ZERO)
                    .add(data.deferredTaxes ?: BigDecimal.ZERO)
                    .add(data.otherNonCurrentLiabilities ?: BigDecimal.ZERO)

                totalNonCurrentLiabilities = if (sum > BigDecimal.ZERO) sum else null
            }

            // 10. Calculate Total Liabilities if not provided
            var totalLiabilities = data.totalLiabilities
            if (totalLiabilities == null && totalCurrentLiabilities != null && totalNonCurrentLiabilities != null) {
                totalLiabilities = totalCurrentLiabilities.add(totalNonCurrentLiabilities)
            }

            // 11. Calculate Total Equity if not provided
            var totalEquity = data.totalEquity
            if (totalEquity == null) {
                val sum = BigDecimal.ZERO
                    .add(data.commonStock ?: BigDecimal.ZERO)
                    .add(data.additionalPaidInCapital ?: BigDecimal.ZERO)
                    .add(data.retainedEarnings ?: BigDecimal.ZERO)
                    .subtract(data.treasuryStock ?: BigDecimal.ZERO)
                    .add(data.otherEquity ?: BigDecimal.ZERO)

                totalEquity = if (sum != BigDecimal.ZERO) sum else null
            }

            // Alternatively, calculate Total Equity as Assets - Liabilities
            if (totalEquity == null && totalAssets != null && totalLiabilities != null) {
                totalEquity = totalAssets.subtract(totalLiabilities)
            }

            // 12. Calculate Book Value Per Share if not provided
            var bookValuePerShare = data.bookValuePerShare
            if (bookValuePerShare == null && totalEquity != null && data.sharesOutstanding != null
                && data.sharesOutstanding != BigDecimal.ZERO) {
                bookValuePerShare = totalEquity.divide(data.sharesOutstanding, 4, RoundingMode.HALF_UP)
            }

            // 13. Calculate Earnings Per Share if not provided
            var earningsPerShare = data.earningsPerShare
            if (earningsPerShare == null && netIncome != null && data.sharesOutstanding != null
                && data.sharesOutstanding != BigDecimal.ZERO) {
                earningsPerShare = netIncome.divide(data.sharesOutstanding, 4, RoundingMode.HALF_UP)
            }

            // 14. Calculate Net Change in Cash if not provided
            var netChangeInCash = data.netChangeInCash
            if (netChangeInCash == null) {
                var sum: BigDecimal? = null
                if (data.netCashFromOperating != null &&
                    data.netCashFromInvesting != null &&
                    data.netCashFromFinancing != null) {
                    sum = data.netCashFromOperating!!
                        .add(data.netCashFromInvesting)
                        .add(data.netCashFromFinancing)
                }
                netChangeInCash = sum
            }

            // Create updated financial data entity with calculated values
            return data.copy(
                grossProfit = grossProfit,
                operatingIncome = operatingIncome,
                earningsBeforeTax = earningsBeforeTax,
                netIncome = netIncome,
                totalCurrentAssets = totalCurrentAssets,
                totalNonCurrentAssets = totalNonCurrentAssets,
                totalAssets = totalAssets,
                totalCurrentLiabilities = totalCurrentLiabilities,
                totalNonCurrentLiabilities = totalNonCurrentLiabilities,
                totalLiabilities = totalLiabilities,
                totalEquity = totalEquity,
                bookValuePerShare = bookValuePerShare,
                earningsPerShare = earningsPerShare,
                netChangeInCash = netChangeInCash,
                // Update the timestamp
                updatedAt = OffsetDateTime.now()
            )
        } catch (e: Exception) {
            logger.error("Error calculating derived values: ${e.message}", e)
            // Return the original data if calculation fails
            return data
        }
    }
}