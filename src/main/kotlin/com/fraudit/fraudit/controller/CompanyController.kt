package com.fraudit.fraudit.controller

import com.fraudit.fraudit.domain.entity.Company
import com.fraudit.fraudit.dto.common.ApiResponse
import com.fraudit.fraudit.dto.common.PagedResponse
import com.fraudit.fraudit.dto.company.*
import com.fraudit.fraudit.service.CompanyService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@RestController
@RequestMapping("/api/companies")
class CompanyController(private val companyService: CompanyService) {

    @GetMapping
    fun getAllCompanies(
        @RequestParam(required = false) sector: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<PagedResponse<CompanySummaryResponse>>> {
        val pageable = PageRequest.of(page, size)

        val companiesPage = if (sector != null) {
            companyService.findBySectorPaged(sector, pageable)
        } else {
            companyService.findAllPaged(pageable)
        }

        val pagedResponse = createPagedResponse(companiesPage) { company ->
            mapToCompanySummaryResponse(company)
        }

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Companies retrieved successfully",
                data = pagedResponse
            )
        )
    }

    @GetMapping("/{id}")
    fun getCompanyById(@PathVariable id: Long): ResponseEntity<ApiResponse<CompanyResponse>> {
        val company = companyService.findById(id)

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Company retrieved successfully",
                data = mapToCompanyResponse(company)
            )
        )
    }

    @GetMapping("/stock/{stockCode}")
    fun getCompanyByStockCode(@PathVariable stockCode: String): ResponseEntity<ApiResponse<CompanyResponse>> {
        val company = companyService.findByStockCode(stockCode)

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Company retrieved successfully",
                data = mapToCompanyResponse(company)
            )
        )
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'REGULATOR')")
    fun createCompany(
        @Valid @RequestBody companyRequest: CompanyRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<CompanyResponse>> {
        val userId = UUID.fromString(userDetails.username)

        val company = Company(
            id = null,
            name = companyRequest.name,
            stockCode = companyRequest.stockCode,
            sector = companyRequest.sector,
            listingDate = companyRequest.listingDate,
            description = companyRequest.description
        )

        val createdCompany = companyService.createCompany(company, userId)

        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse(
                success = true,
                message = "Company created successfully",
                data = mapToCompanyResponse(createdCompany)
            )
        )
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGULATOR')")
    fun updateCompany(
        @PathVariable id: Long,
        @Valid @RequestBody companyRequest: CompanyRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<CompanyResponse>> {
        val userId = UUID.fromString(userDetails.username)

        // Get existing company to preserve created timestamp
        val existingCompany = companyService.findById(id)

        val company = existingCompany.copy(
            name = companyRequest.name,
            stockCode = companyRequest.stockCode,
            sector = companyRequest.sector,
            listingDate = companyRequest.listingDate,
            description = companyRequest.description
        )

        val updatedCompany = companyService.updateCompany(company, userId)

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Company updated successfully",
                data = mapToCompanyResponse(updatedCompany)
            )
        )
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteCompany(
        @PathVariable id: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Void>> {
        val userId = UUID.fromString(userDetails.username)

        companyService.deleteCompany(id, userId)

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Company deleted successfully"
            )
        )
    }

    @GetMapping("/check-name")
    fun checkNameAvailability(@RequestParam name: String): ResponseEntity<ApiResponse<Map<String, Boolean>>> {
        val isAvailable = companyService.isCompanyNameAvailable(name)

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = if (isAvailable) "Company name is available" else "Company name is already taken",
                data = mapOf("available" to isAvailable)
            )
        )
    }

    @GetMapping("/check-stock-code")
    fun checkStockCodeAvailability(@RequestParam stockCode: String): ResponseEntity<ApiResponse<Map<String, Boolean>>> {
        val isAvailable = companyService.isStockCodeAvailable(stockCode)

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = if (isAvailable) "Stock code is available" else "Stock code is already in use",
                data = mapOf("available" to isAvailable)
            )
        )
    }

    @GetMapping("/sectors")
    fun getAllSectors(): ResponseEntity<ApiResponse<List<String>>> {
        val companies = companyService.findAll()
        val sectors = companies.mapNotNull { it.sector }.distinct().sorted()

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Sectors retrieved successfully",
                data = sectors
            )
        )
    }

    // Helper methods for mapping entities to DTOs

    private fun mapToCompanyResponse(company: Company): CompanyResponse {
        return CompanyResponse(
            id = company.id!!,
            name = company.name,
            stockCode = company.stockCode,
            sector = company.sector,
            listingDate = company.listingDate,
            description = company.description,
            createdAt = company.createdAt,
            updatedAt = company.updatedAt
        )
    }

    private fun mapToCompanySummaryResponse(company: Company): CompanySummaryResponse {
        return CompanySummaryResponse(
            id = company.id!!,
            name = company.name,
            stockCode = company.stockCode,
            sector = company.sector
        )
    }

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