package com.fraudit.fraudit.service.impl

import com.fraudit.fraudit.domain.entity.FinancialData
import com.fraudit.fraudit.repository.FinancialDataRepository
import com.fraudit.fraudit.repository.FinancialStatementRepository
import com.fraudit.fraudit.service.AuditLogService
import com.fraudit.fraudit.service.FinancialDataService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID
import jakarta.persistence.EntityNotFoundException

@Service
class FinancialDataServiceImpl(
    private val financialDataRepository: FinancialDataRepository,
    private val financialStatementRepository: FinancialStatementRepository,
    private val auditLogService: AuditLogService
) : FinancialDataService {

    override fun findAll(): List<FinancialData> = financialDataRepository.findAll()

    override fun findById(id: Long): FinancialData = financialDataRepository.findById(id)
        .orElseThrow { EntityNotFoundException("Financial data not found with id: $id") }

    override fun findByStatementId(statementId: Long): FinancialData? = financialDataRepository.findByStatementId(statementId)

    override fun findLatestByCompanyId(companyId: Long): List<FinancialData> =
        financialDataRepository.findLatestByCompanyId(companyId)

    @Transactional
    override fun createFinancialData(financialData: FinancialData, userId: UUID): FinancialData {
        // Check if financial data already exists for this statement
        val existingData = financialDataRepository.findByStatementId(financialData.statement.id!!)
        if (existingData != null) {
            throw IllegalStateException("Financial data already exists for statement id: ${financialData.statement.id}")
        }

        val savedData = financialDataRepository.save(financialData)

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

        val savedData = financialDataRepository.save(financialData)

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

        financialDataRepository.delete(financialData)

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

        // Calculate derived values

        // 1. Calculate Gross Profit if not provided
        var grossProfit = financialData.grossProfit
        if (grossProfit == null && financialData.revenue != null && financialData.costOfSales != null) {
            grossProfit = financialData.revenue!!.subtract(financialData.costOfSales)
        }

        // 2. Calculate Operating Income if not provided
        var operatingIncome = financialData.operatingIncome
        if (operatingIncome == null && grossProfit != null && financialData.operatingExpenses != null) {
            operatingIncome = grossProfit.subtract(financialData.operatingExpenses)
        }

        // 3. Calculate Earnings Before Tax if not provided
        var earningsBeforeTax = financialData.earningsBeforeTax
        if (earningsBeforeTax == null && operatingIncome != null) {
            earningsBeforeTax = operatingIncome.subtract(financialData.interestExpense ?: BigDecimal.ZERO)
                .add(financialData.otherIncome ?: BigDecimal.ZERO)
        }

        // 4. Calculate Net Income if not provided
        var netIncome = financialData.netIncome
        if (netIncome == null && earningsBeforeTax != null && financialData.incomeTax != null) {
            netIncome = earningsBeforeTax.subtract(financialData.incomeTax)
        }

        // 5. Calculate Total Current Assets if not provided
        var totalCurrentAssets = financialData.totalCurrentAssets
        if (totalCurrentAssets == null) {
            totalCurrentAssets = BigDecimal.ZERO
                .add(financialData.cash ?: BigDecimal.ZERO)
                .add(financialData.shortTermInvestments ?: BigDecimal.ZERO)
                .add(financialData.accountsReceivable ?: BigDecimal.ZERO)
                .add(financialData.inventory ?: BigDecimal.ZERO)
                .add(financialData.otherCurrentAssets ?: BigDecimal.ZERO)
        }

        // 6. Calculate Total Non-Current Assets if not provided
        var totalNonCurrentAssets = financialData.totalNonCurrentAssets
        if (totalNonCurrentAssets == null) {
            var ppe = financialData.propertyPlantEquipment ?: BigDecimal.ZERO
            val accumulatedDepreciation = financialData.accumulatedDepreciation ?: BigDecimal.ZERO

            // If accumulated depreciation is stored as a positive value, subtract it
            if (accumulatedDepreciation.compareTo(BigDecimal.ZERO) > 0) {
                ppe = ppe.subtract(accumulatedDepreciation)
            }

            totalNonCurrentAssets = BigDecimal.ZERO
                .add(ppe)
                .add(financialData.intangibleAssets ?: BigDecimal.ZERO)
                .add(financialData.longTermInvestments ?: BigDecimal.ZERO)
                .add(financialData.otherNonCurrentAssets ?: BigDecimal.ZERO)
        }

        // 7. Calculate Total Assets if not provided
        var totalAssets = financialData.totalAssets
        if (totalAssets == null && totalCurrentAssets != null && totalNonCurrentAssets != null) {
            totalAssets = totalCurrentAssets.add(totalNonCurrentAssets)
        }

        // 8. Calculate Total Current Liabilities if not provided
        var totalCurrentLiabilities = financialData.totalCurrentLiabilities
        if (totalCurrentLiabilities == null) {
            totalCurrentLiabilities = BigDecimal.ZERO
                .add(financialData.accountsPayable ?: BigDecimal.ZERO)
                .add(financialData.shortTermDebt ?: BigDecimal.ZERO)
                .add(financialData.accruedLiabilities ?: BigDecimal.ZERO)
                .add(financialData.otherCurrentLiabilities ?: BigDecimal.ZERO)
        }

        // 9. Calculate Total Non-Current Liabilities if not provided
        var totalNonCurrentLiabilities = financialData.totalNonCurrentLiabilities
        if (totalNonCurrentLiabilities == null) {
            totalNonCurrentLiabilities = BigDecimal.ZERO
                .add(financialData.longTermDebt ?: BigDecimal.ZERO)
                .add(financialData.deferredTaxes ?: BigDecimal.ZERO)
                .add(financialData.otherNonCurrentLiabilities ?: BigDecimal.ZERO)
        }

        // 10. Calculate Total Liabilities if not provided
        var totalLiabilities = financialData.totalLiabilities
        if (totalLiabilities == null && totalCurrentLiabilities != null && totalNonCurrentLiabilities != null) {
            totalLiabilities = totalCurrentLiabilities.add(totalNonCurrentLiabilities)
        }

        // 11. Calculate Total Equity if not provided
        var totalEquity = financialData.totalEquity
        if (totalEquity == null) {
            totalEquity = BigDecimal.ZERO
                .add(financialData.commonStock ?: BigDecimal.ZERO)
                .add(financialData.additionalPaidInCapital ?: BigDecimal.ZERO)
                .add(financialData.retainedEarnings ?: BigDecimal.ZERO)
                .subtract(financialData.treasuryStock ?: BigDecimal.ZERO)
                .add(financialData.otherEquity ?: BigDecimal.ZERO)
        }

        // 12. Calculate Book Value Per Share if not provided
        var bookValuePerShare = financialData.bookValuePerShare
        if (bookValuePerShare == null && totalEquity != null && financialData.sharesOutstanding != null && financialData.sharesOutstanding != BigDecimal.ZERO) {
            bookValuePerShare = totalEquity.divide(financialData.sharesOutstanding, 4, RoundingMode.HALF_UP)
        }

        // 13. Calculate Earnings Per Share if not provided
        var earningsPerShare = financialData.earningsPerShare
        if (earningsPerShare == null && netIncome != null && financialData.sharesOutstanding != null && financialData.sharesOutstanding != BigDecimal.ZERO) {
            earningsPerShare = netIncome.divide(financialData.sharesOutstanding, 4, RoundingMode.HALF_UP)
        }

        // Create updated financial data entity with calculated values
        val updatedFinancialData = financialData.copy(
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
            earningsPerShare = earningsPerShare
        )

        val savedData = financialDataRepository.save(updatedFinancialData)

        auditLogService.logEvent(
            userId = userId,
            action = "CALCULATE",
            entityType = "FINANCIAL_DATA",
            entityId = savedData.id.toString(),
            details = "Calculated derived values for financial data with id: $id"
        )

        return savedData
    }
}
