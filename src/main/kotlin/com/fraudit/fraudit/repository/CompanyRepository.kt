package com.fraudit.fraudit.repository

import com.fraudit.fraudit.domain.entity.Company
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface CompanyRepository : JpaRepository<Company, Long> {
    fun findByName(name: String): Optional<Company>
    fun findByStockCode(stockCode: String): Optional<Company>
    fun existsByName(name: String): Boolean
    fun existsByStockCode(stockCode: String): Boolean
    fun findBySector(sector: String): List<Company>
    fun findBySector(sector: String, pageable: Pageable): Page<Company>
}