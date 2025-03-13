package com.fraudit.fraudit.service

import com.fraudit.fraudit.domain.entity.FinancialData
import java.util.UUID

interface FinancialDataService {
    fun findAll(): List<FinancialData>
    fun findById(id: Long): FinancialData
    fun findByStatementId(statementId: Long): FinancialData?
    fun findLatestByCompanyId(companyId: Long): List<FinancialData>
    fun createFinancialData(financialData: FinancialData, userId: UUID): FinancialData
    fun updateFinancialData(financialData: FinancialData, userId: UUID): FinancialData
    fun deleteFinancialData(id: Long, userId: UUID)
    fun calculateDerivedValues(id: Long, userId: UUID): FinancialData
}