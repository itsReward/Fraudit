package com.fraudit.fraudit.domain.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.OffsetDateTime

// Financial Data Entity
@Entity
@Table(name = "financial_data")
data class FinancialData(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "data_id")
    val id: Long? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statement_id", nullable = false, unique = true)
    val statement: FinancialStatement,

    // Income Statement
    @Column(name = "revenue")
    val revenue: BigDecimal? = null,

    @Column(name = "cost_of_sales")
    val costOfSales: BigDecimal? = null,

    @Column(name = "gross_profit")
    val grossProfit: BigDecimal? = null,

    @Column(name = "operating_expenses")
    val operatingExpenses: BigDecimal? = null,

    @Column(name = "administrative_expenses")
    val administrativeExpenses: BigDecimal? = null,

    @Column(name = "selling_expenses")
    val sellingExpenses: BigDecimal? = null,

    @Column(name = "depreciation")
    val depreciation: BigDecimal? = null,

    @Column(name = "amortization")
    val amortization: BigDecimal? = null,

    @Column(name = "operating_income")
    val operatingIncome: BigDecimal? = null,

    @Column(name = "interest_expense")
    val interestExpense: BigDecimal? = null,

    @Column(name = "other_income")
    val otherIncome: BigDecimal? = null,

    @Column(name = "earnings_before_tax")
    val earningsBeforeTax: BigDecimal? = null,

    @Column(name = "income_tax")
    val incomeTax: BigDecimal? = null,

    @Column(name = "net_income")
    val netIncome: BigDecimal? = null,

    // Balance Sheet - Assets
    @Column(name = "cash")
    val cash: BigDecimal? = null,

    @Column(name = "short_term_investments")
    val shortTermInvestments: BigDecimal? = null,

    @Column(name = "accounts_receivable")
    val accountsReceivable: BigDecimal? = null,

    @Column(name = "inventory")
    val inventory: BigDecimal? = null,

    @Column(name = "other_current_assets")
    val otherCurrentAssets: BigDecimal? = null,

    @Column(name = "total_current_assets")
    val totalCurrentAssets: BigDecimal? = null,

    @Column(name = "property_plant_equipment")
    val propertyPlantEquipment: BigDecimal? = null,

    @Column(name = "accumulated_depreciation")
    val accumulatedDepreciation: BigDecimal? = null,

    @Column(name = "intangible_assets")
    val intangibleAssets: BigDecimal? = null,

    @Column(name = "long_term_investments")
    val longTermInvestments: BigDecimal? = null,

    @Column(name = "other_non_current_assets")
    val otherNonCurrentAssets: BigDecimal? = null,

    @Column(name = "total_non_current_assets")
    val totalNonCurrentAssets: BigDecimal? = null,

    @Column(name = "total_assets")
    val totalAssets: BigDecimal? = null,

    // Balance Sheet - Liabilities
    @Column(name = "accounts_payable")
    val accountsPayable: BigDecimal? = null,

    @Column(name = "short_term_debt")
    val shortTermDebt: BigDecimal? = null,

    @Column(name = "accrued_liabilities")
    val accruedLiabilities: BigDecimal? = null,

    @Column(name = "other_current_liabilities")
    val otherCurrentLiabilities: BigDecimal? = null,

    @Column(name = "total_current_liabilities")
    val totalCurrentLiabilities: BigDecimal? = null,

    @Column(name = "long_term_debt")
    val longTermDebt: BigDecimal? = null,

    @Column(name = "deferred_taxes")
    val deferredTaxes: BigDecimal? = null,

    @Column(name = "other_non_current_liabilities")
    val otherNonCurrentLiabilities: BigDecimal? = null,

    @Column(name = "total_non_current_liabilities")
    val totalNonCurrentLiabilities: BigDecimal? = null,

    @Column(name = "total_liabilities")
    val totalLiabilities: BigDecimal? = null,

    // Balance Sheet - Equity
    @Column(name = "common_stock")
    val commonStock: BigDecimal? = null,

    @Column(name = "additional_paid_in_capital")
    val additionalPaidInCapital: BigDecimal? = null,

    @Column(name = "retained_earnings")
    val retainedEarnings: BigDecimal? = null,

    @Column(name = "treasury_stock")
    val treasuryStock: BigDecimal? = null,

    @Column(name = "other_equity")
    val otherEquity: BigDecimal? = null,

    @Column(name = "total_equity")
    val totalEquity: BigDecimal? = null,

    // Cash Flow Statement
    @Column(name = "net_cash_from_operating")
    val netCashFromOperating: BigDecimal? = null,

    @Column(name = "net_cash_from_investing")
    val netCashFromInvesting: BigDecimal? = null,

    @Column(name = "net_cash_from_financing")
    val netCashFromFinancing: BigDecimal? = null,

    @Column(name = "net_change_in_cash")
    val netChangeInCash: BigDecimal? = null,

    // Market Data
    @Column(name = "market_capitalization")
    val marketCapitalization: BigDecimal? = null,

    @Column(name = "shares_outstanding")
    val sharesOutstanding: BigDecimal? = null,

    @Column(name = "market_price_per_share")
    val marketPricePerShare: BigDecimal? = null,

    @Column(name = "book_value_per_share")
    val bookValuePerShare: BigDecimal? = null,

    @Column(name = "earnings_per_share")
    val earningsPerShare: BigDecimal? = null,

    // Year-over-Year Changes
    @Column(name = "revenue_growth")
    val revenueGrowth: BigDecimal? = null,

    @Column(name = "gross_profit_growth")
    val grossProfitGrowth: BigDecimal? = null,

    @Column(name = "net_income_growth")
    val netIncomeGrowth: BigDecimal? = null,

    @Column(name = "asset_growth")
    val assetGrowth: BigDecimal? = null,

    @Column(name = "receivables_growth")
    val receivablesGrowth: BigDecimal? = null,

    @Column(name = "inventory_growth")
    val inventoryGrowth: BigDecimal? = null,

    @Column(name = "liability_growth")
    val liabilityGrowth: BigDecimal? = null,

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    val createdAt: OffsetDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updated_at")
    val updatedAt: OffsetDateTime? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FinancialData) return false
        if (id != null && other.id != null) return id == other.id
        return false
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "FinancialData(id=$id)"
    }
}
