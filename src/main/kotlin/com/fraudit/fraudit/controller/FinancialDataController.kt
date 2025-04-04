package com.fraudit.fraudit.controller

import com.fraudit.fraudit.domain.entity.FinancialData
import com.fraudit.fraudit.dto.common.ApiResponse
import com.fraudit.fraudit.dto.common.PagedResponse
import com.fraudit.fraudit.dto.financial.*
import com.fraudit.fraudit.service.FinancialDataService
import com.fraudit.fraudit.service.FinancialStatementService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.util.UUID

@RestController
@RequestMapping("/api/financial-data")
class FinancialDataController(
    private val financialDataService: FinancialDataService,
    private val financialStatementService: FinancialStatementService
) {

    @GetMapping("/{id}")
    fun getFinancialDataById(@PathVariable id: Long): ResponseEntity<ApiResponse<FinancialDataResponse>> {
        val financialData = financialDataService.findById(id)

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Financial data retrieved successfully",
                data = mapToFinancialDataResponse(financialData)
            )
        )
    }

    @GetMapping("/statement/{statementId}")
    fun getFinancialDataByStatementId(@PathVariable statementId: Long): ResponseEntity<ApiResponse<FinancialDataResponse>> {
        val financialData = financialDataService.findByStatementId(statementId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse(
                    success = false,
                    message = "Financial data not found for statement id: $statementId",
                    errors = listOf("Financial data not found for statement id: $statementId")
                )
            )

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Financial data retrieved successfully",
                data = mapToFinancialDataResponse(financialData)
            )
        )
    }

    @GetMapping("/company/{companyId}")
    fun getFinancialDataByCompanyId(
        @PathVariable companyId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<PagedResponse<FinancialDataSummaryResponse>>> {
        val pageable = PageRequest.of(page, size)
        val financialDataPage = financialDataService.findByCompanyId(companyId, pageable)

        val pagedResponse = createPagedResponse(financialDataPage) { financialData ->
            mapToFinancialDataSummaryResponse(financialData)
        }

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Financial data retrieved successfully",
                data = pagedResponse
            )
        )
    }

    @GetMapping("/company/{companyId}/latest")
    fun getLatestFinancialDataByCompanyId(@PathVariable companyId: Long): ResponseEntity<ApiResponse<FinancialDataResponse>> {
        val latestData = financialDataService.findLatestByCompanyId(companyId)

        if (latestData.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse(
                    success = false,
                    message = "No financial data found for company id: $companyId",
                    errors = listOf("No financial data found for company id: $companyId")
                )
            )
        }

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Latest financial data retrieved successfully",
                data = mapToFinancialDataResponse(latestData.first())
            )
        )
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    fun createFinancialData(
        @Valid @RequestBody request: FinancialDataRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<FinancialDataResponse>> {
        val userId = UUID.fromString(userDetails.username)

        // Get the statement
        val statement = financialStatementService.findById(request.statementId)

        // Create the financial data entity
        val financialData = FinancialData(
            id = null,
            statement = statement,

            // Income Statement
            revenue = request.revenue,
            costOfSales = request.costOfSales,
            grossProfit = request.grossProfit,
            operatingExpenses = request.operatingExpenses,
            administrativeExpenses = request.administrativeExpenses,
            sellingExpenses = request.sellingExpenses,
            depreciation = request.depreciation,
            amortization = request.amortization,
            operatingIncome = request.operatingIncome,
            interestExpense = request.interestExpense,
            otherIncome = request.otherIncome,
            earningsBeforeTax = request.earningsBeforeTax,
            incomeTax = request.incomeTax,
            netIncome = request.netIncome,

            // Balance Sheet - Assets
            cash = request.cash,
            shortTermInvestments = request.shortTermInvestments,
            accountsReceivable = request.accountsReceivable,
            inventory = request.inventory,
            otherCurrentAssets = request.otherCurrentAssets,
            totalCurrentAssets = request.totalCurrentAssets,
            propertyPlantEquipment = request.propertyPlantEquipment,
            accumulatedDepreciation = request.accumulatedDepreciation,
            intangibleAssets = request.intangibleAssets,
            longTermInvestments = request.longTermInvestments,
            otherNonCurrentAssets = request.otherNonCurrentAssets,
            totalNonCurrentAssets = request.totalNonCurrentAssets,
            totalAssets = request.totalAssets,

            // Balance Sheet - Liabilities
            accountsPayable = request.accountsPayable,
            shortTermDebt = request.shortTermDebt,
            accruedLiabilities = request.accruedLiabilities,
            otherCurrentLiabilities = request.otherCurrentLiabilities,
            totalCurrentLiabilities = request.totalCurrentLiabilities,
            longTermDebt = request.longTermDebt,
            deferredTaxes = request.deferredTaxes,
            otherNonCurrentLiabilities = request.otherNonCurrentLiabilities,
            totalNonCurrentLiabilities = request.totalNonCurrentLiabilities,
            totalLiabilities = request.totalLiabilities,

            // Balance Sheet - Equity
            commonStock = request.commonStock,
            additionalPaidInCapital = request.additionalPaidInCapital,
            retainedEarnings = request.retainedEarnings,
            treasuryStock = request.treasuryStock,
            otherEquity = request.otherEquity,
            totalEquity = request.totalEquity,

            // Cash Flow
            netCashFromOperating = request.netCashFromOperating,
            netCashFromInvesting = request.netCashFromInvesting,
            netCashFromFinancing = request.netCashFromFinancing,
            netChangeInCash = request.netChangeInCash,

            // Market Data
            marketCapitalization = request.marketCapitalization,
            sharesOutstanding = request.sharesOutstanding,
            marketPricePerShare = request.marketPricePerShare,
            bookValuePerShare = request.bookValuePerShare,
            earningsPerShare = request.earningsPerShare,

            // Growth metrics will be calculated automatically
            revenueGrowth = null,
            grossProfitGrowth = null,
            netIncomeGrowth = null,
            assetGrowth = null,
            receivablesGrowth = null,
            inventoryGrowth = null,
            liabilityGrowth = null,

            createdAt = null,
            updatedAt = null
        )

        // Create the financial data and calculate derived values
        val createdData = financialDataService.createFinancialData(financialData, userId)

        // Calculate growth rates if possible
        val finalData = try {
            financialDataService.calculateGrowthRates(request.statementId, userId)
        } catch (e: Exception) {
            // Growth rates calculation failed, but we can still return the created data
            createdData
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse(
                success = true,
                message = "Financial data created successfully",
                data = mapToFinancialDataResponse(finalData)
            )
        )
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    fun updateFinancialData(
        @PathVariable id: Long,
        @Valid @RequestBody request: FinancialDataRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<FinancialDataResponse>> {
        val userId = UUID.fromString(userDetails.username)

        // Verify the financial data exists
        val existingData = financialDataService.findById(id)

        // Verify the statement ID matches
        if (existingData.statement.id != request.statementId) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse(
                    success = false,
                    message = "Statement ID in request does not match the financial data record",
                    errors = listOf("Statement ID in request does not match the financial data record")
                )
            )
        }

        // Create the updated financial data entity
        val financialData = existingData.copy(
            // Income Statement
            revenue = request.revenue,
            costOfSales = request.costOfSales,
            grossProfit = request.grossProfit,
            operatingExpenses = request.operatingExpenses,
            administrativeExpenses = request.administrativeExpenses,
            sellingExpenses = request.sellingExpenses,
            depreciation = request.depreciation,
            amortization = request.amortization,
            operatingIncome = request.operatingIncome,
            interestExpense = request.interestExpense,
            otherIncome = request.otherIncome,
            earningsBeforeTax = request.earningsBeforeTax,
            incomeTax = request.incomeTax,
            netIncome = request.netIncome,

            // Balance Sheet - Assets
            cash = request.cash,
            shortTermInvestments = request.shortTermInvestments,
            accountsReceivable = request.accountsReceivable,
            inventory = request.inventory,
            otherCurrentAssets = request.otherCurrentAssets,
            totalCurrentAssets = request.totalCurrentAssets,
            propertyPlantEquipment = request.propertyPlantEquipment,
            accumulatedDepreciation = request.accumulatedDepreciation,
            intangibleAssets = request.intangibleAssets,
            longTermInvestments = request.longTermInvestments,
            otherNonCurrentAssets = request.otherNonCurrentAssets,
            totalNonCurrentAssets = request.totalNonCurrentAssets,
            totalAssets = request.totalAssets,

            // Balance Sheet - Liabilities
            accountsPayable = request.accountsPayable,
            shortTermDebt = request.shortTermDebt,
            accruedLiabilities = request.accruedLiabilities,
            otherCurrentLiabilities = request.otherCurrentLiabilities,
            totalCurrentLiabilities = request.totalCurrentLiabilities,
            longTermDebt = request.longTermDebt,
            deferredTaxes = request.deferredTaxes,
            otherNonCurrentLiabilities = request.otherNonCurrentLiabilities,
            totalNonCurrentLiabilities = request.totalNonCurrentLiabilities,
            totalLiabilities = request.totalLiabilities,

            // Balance Sheet - Equity
            commonStock = request.commonStock,
            additionalPaidInCapital = request.additionalPaidInCapital,
            retainedEarnings = request.retainedEarnings,
            treasuryStock = request.treasuryStock,
            otherEquity = request.otherEquity,
            totalEquity = request.totalEquity,

            // Cash Flow
            netCashFromOperating = request.netCashFromOperating,
            netCashFromInvesting = request.netCashFromInvesting,
            netCashFromFinancing = request.netCashFromFinancing,
            netChangeInCash = request.netChangeInCash,

            // Market Data
            marketCapitalization = request.marketCapitalization,
            sharesOutstanding = request.sharesOutstanding,
            marketPricePerShare = request.marketPricePerShare,
            bookValuePerShare = request.bookValuePerShare,
            earningsPerShare = request.earningsPerShare
        )

        // Update the financial data and calculate derived values
        val updatedData = financialDataService.updateFinancialData(financialData, userId)

        // Calculate growth rates if possible
        val finalData = try {
            financialDataService.calculateGrowthRates(request.statementId, userId)
        } catch (e: Exception) {
            // Growth rates calculation failed, but we can still return the updated data
            updatedData
        }

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Financial data updated successfully",
                data = mapToFinancialDataResponse(finalData)
            )
        )
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    fun deleteFinancialData(
        @PathVariable id: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Void>> {
        val userId = UUID.fromString(userDetails.username)

        financialDataService.deleteFinancialData(id, userId)

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Financial data deleted successfully"
            )
        )
    }

    @PostMapping("/{id}/calculate-derived")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    fun calculateDerivedValues(
        @PathVariable id: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<FinancialDataResponse>> {
        val userId = UUID.fromString(userDetails.username)

        val updatedData = financialDataService.calculateDerivedValues(id, userId)

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Derived values calculated successfully",
                data = mapToFinancialDataResponse(updatedData)
            )
        )
    }

    @PostMapping("/statement/{statementId}/calculate-growth")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    fun calculateGrowthRates(
        @PathVariable statementId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<FinancialDataResponse>> {
        val userId = UUID.fromString(userDetails.username)

        val updatedData = financialDataService.calculateGrowthRates(statementId, userId)

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Growth rates calculated successfully",
                data = mapToFinancialDataResponse(updatedData)
            )
        )
    }

    @GetMapping("/metrics/summary")
    fun getFinancialMetricsSummary(
        @RequestParam companyIds: List<Long>,
        @RequestParam(required = false) year: Int?
    ): ResponseEntity<ApiResponse<List<FinancialMetricsSummaryResponse>>> {
        // This endpoint provides a high-level metrics summary for multiple companies
        val metrics = mutableListOf<FinancialMetricsSummaryResponse>()

        for (companyId in companyIds) {
            val financialDataList = if (year != null) {
                // Find financial data for specific year
                financialDataService.findLatestByCompanyId(companyId)
                    .filter { it.statement.fiscalYear.year == year }
            } else {
                // Find latest financial data
                financialDataService.findLatestByCompanyId(companyId).take(1)
            }

            if (financialDataList.isNotEmpty()) {
                val data = financialDataList.first()
                val company = data.statement.fiscalYear.company
                val fiscalYear = data.statement.fiscalYear

                // Calculate key financial metrics
                val profitMargin = calculateProfitMargin(data)
                val currentRatio = calculateCurrentRatio(data)
                val debtToEquity = calculateDebtToEquity(data)
                val returnOnAssets = calculateReturnOnAssets(data)

                metrics.add(
                    FinancialMetricsSummaryResponse(
                        companyId = company.id!!,
                        companyName = company.name,
                        stockCode = company.stockCode,
                        year = fiscalYear.year,
                        revenue = data.revenue,
                        netIncome = data.netIncome,
                        totalAssets = data.totalAssets,
                        totalLiabilities = data.totalLiabilities,
                        totalEquity = data.totalEquity,
                        profitMargin = profitMargin,
                        currentRatio = currentRatio,
                        debtToEquity = debtToEquity,
                        returnOnAssets = returnOnAssets,
                        statementId = data.statement.id!!
                    )
                )
            }
        }

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Financial metrics summary retrieved successfully",
                data = metrics
            )
        )
    }

    @GetMapping("/trend/{companyId}")
    fun getFinancialTrend(
        @PathVariable companyId: Long,
        @RequestParam(required = false) startYear: Int?,
        @RequestParam(required = false) endYear: Int?
    ): ResponseEntity<ApiResponse<List<FinancialTrendResponse>>> {
        // Get all financial data for the company
        val allData = financialDataService.findLatestByCompanyId(companyId)
            .sortedBy { it.statement.fiscalYear.year }

        if (allData.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse(
                    success = false,
                    message = "No financial data found for company id: $companyId",
                    errors = listOf("No financial data found for company id: $companyId")
                )
            )
        }

        // Filter by year range if specified
        val filteredData = allData.filter { data ->
            val year = data.statement.fiscalYear.year
            (startYear == null || year >= startYear) && (endYear == null || year <= endYear)
        }

        // Map to trend response
        val trend = filteredData.map { data ->
            FinancialTrendResponse(
                year = data.statement.fiscalYear.year,
                revenue = data.revenue,
                netIncome = data.netIncome,
                totalAssets = data.totalAssets,
                totalLiabilities = data.totalLiabilities,
                totalEquity = data.totalEquity,
                revenueGrowth = data.revenueGrowth,
                netIncomeGrowth = data.netIncomeGrowth,
                assetGrowth = data.assetGrowth,
                liabilityGrowth = data.liabilityGrowth
            )
        }

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Financial trend retrieved successfully",
                data = trend
            )
        )
    }

    // Additional helper methods for calculations

    private fun calculateProfitMargin(data: FinancialData): BigDecimal? {
        if (data.netIncome == null || data.revenue == null || data.revenue == BigDecimal.ZERO) {
            return null
        }
        return data.netIncome!!.divide(data.revenue, 4, BigDecimal.ROUND_HALF_UP)
            .multiply(BigDecimal(100))
    }

    private fun calculateCurrentRatio(data: FinancialData): BigDecimal? {
        if (data.totalCurrentAssets == null || data.totalCurrentLiabilities == null ||
            data.totalCurrentLiabilities == BigDecimal.ZERO) {
            return null
        }
        return data.totalCurrentAssets!!.divide(data.totalCurrentLiabilities, 4, BigDecimal.ROUND_HALF_UP)
    }

    private fun calculateDebtToEquity(data: FinancialData): BigDecimal? {
        if (data.totalLiabilities == null || data.totalEquity == null ||
            data.totalEquity == BigDecimal.ZERO) {
            return null
        }
        return data.totalLiabilities!!.divide(data.totalEquity, 4, BigDecimal.ROUND_HALF_UP)
    }

    private fun calculateReturnOnAssets(data: FinancialData): BigDecimal? {
        if (data.netIncome == null || data.totalAssets == null ||
            data.totalAssets == BigDecimal.ZERO) {
            return null
        }
        return data.netIncome!!.divide(data.totalAssets, 4, BigDecimal.ROUND_HALF_UP)
            .multiply(BigDecimal(100))
    }

    /**
     * Helper method to map a FinancialData entity to a FinancialDataResponse DTO
     */
    private fun mapToFinancialDataResponse(financialData: FinancialData): FinancialDataResponse {
        val statement = financialData.statement
        val company = statement.fiscalYear.company

        return FinancialDataResponse(
            id = financialData.id!!,
            statementId = statement.id!!,
            companyId = company.id!!,
            companyName = company.name,
            year = statement.fiscalYear.year,

            // Income Statement
            revenue = financialData.revenue,
            costOfSales = financialData.costOfSales,
            grossProfit = financialData.grossProfit,
            operatingExpenses = financialData.operatingExpenses,
            administrativeExpenses = financialData.administrativeExpenses,
            sellingExpenses = financialData.sellingExpenses,
            depreciation = financialData.depreciation,
            amortization = financialData.amortization,
            operatingIncome = financialData.operatingIncome,
            interestExpense = financialData.interestExpense,
            otherIncome = financialData.otherIncome,
            earningsBeforeTax = financialData.earningsBeforeTax,
            incomeTax = financialData.incomeTax,
            netIncome = financialData.netIncome,

            // Balance Sheet - Assets
            cash = financialData.cash,
            shortTermInvestments = financialData.shortTermInvestments,
            accountsReceivable = financialData.accountsReceivable,
            inventory = financialData.inventory,
            otherCurrentAssets = financialData.otherCurrentAssets,
            totalCurrentAssets = financialData.totalCurrentAssets,
            propertyPlantEquipment = financialData.propertyPlantEquipment,
            accumulatedDepreciation = financialData.accumulatedDepreciation,
            intangibleAssets = financialData.intangibleAssets,
            longTermInvestments = financialData.longTermInvestments,
            otherNonCurrentAssets = financialData.otherNonCurrentAssets,
            totalNonCurrentAssets = financialData.totalNonCurrentAssets,
            totalAssets = financialData.totalAssets,

            // Balance Sheet - Liabilities
            accountsPayable = financialData.accountsPayable,
            shortTermDebt = financialData.shortTermDebt,
            accruedLiabilities = financialData.accruedLiabilities,
            otherCurrentLiabilities = financialData.otherCurrentLiabilities,
            totalCurrentLiabilities = financialData.totalCurrentLiabilities,
            longTermDebt = financialData.longTermDebt,
            deferredTaxes = financialData.deferredTaxes,
            otherNonCurrentLiabilities = financialData.otherNonCurrentLiabilities,
            totalNonCurrentLiabilities = financialData.totalNonCurrentLiabilities,
            totalLiabilities = financialData.totalLiabilities,

            // Balance Sheet - Equity
            commonStock = financialData.commonStock,
            additionalPaidInCapital = financialData.additionalPaidInCapital,
            retainedEarnings = financialData.retainedEarnings,
            treasuryStock = financialData.treasuryStock,
            otherEquity = financialData.otherEquity,
            totalEquity = financialData.totalEquity,

            // Cash Flow
            netCashFromOperating = financialData.netCashFromOperating,
            netCashFromInvesting = financialData.netCashFromInvesting,
            netCashFromFinancing = financialData.netCashFromFinancing,
            netChangeInCash = financialData.netChangeInCash,

            // Market Data
            marketCapitalization = financialData.marketCapitalization,
            sharesOutstanding = financialData.sharesOutstanding,
            marketPricePerShare = financialData.marketPricePerShare,
            bookValuePerShare = financialData.bookValuePerShare,
            earningsPerShare = financialData.earningsPerShare,

            // Growth Metrics
            revenueGrowth = financialData.revenueGrowth,
            grossProfitGrowth = financialData.grossProfitGrowth,
            netIncomeGrowth = financialData.netIncomeGrowth,
            assetGrowth = financialData.assetGrowth,
            receivablesGrowth = financialData.receivablesGrowth,
            inventoryGrowth = financialData.inventoryGrowth,
            liabilityGrowth = financialData.liabilityGrowth,

            createdAt = financialData.createdAt,
            updatedAt = financialData.updatedAt
        )
    }

    /**
     * Helper method to map a FinancialData entity to a FinancialDataSummaryResponse DTO
     */
    private fun mapToFinancialDataSummaryResponse(financialData: FinancialData): FinancialDataSummaryResponse {
        val statement = financialData.statement
        val company = statement.fiscalYear.company

        return FinancialDataSummaryResponse(
            id = financialData.id!!,
            statementId = statement.id!!,
            companyName = company.name,
            year = statement.fiscalYear.year,
            revenue = financialData.revenue,
            netIncome = financialData.netIncome,
            totalAssets = financialData.totalAssets,
            totalLiabilities = financialData.totalLiabilities,
            updatedAt = financialData.updatedAt
        )
    }

    /**
     * Helper method to create a paged response
     */
    private fun <T, R> createPagedResponse(page: Page<T>, mapper: (T) -> R): PagedResponse<R> {
        return PagedResponse(
            content = page.content.map(mapper),
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            first = page.isFirst,
            last = page.isLast
        )
    }
}