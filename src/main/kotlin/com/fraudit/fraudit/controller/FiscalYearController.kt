package com.fraudit.fraudit.controller

import com.fraudit.fraudit.dto.common.ApiResponse
import com.fraudit.fraudit.dto.fiscalyear.*
import com.fraudit.fraudit.service.FiscalYearService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid

@RestController
@RequestMapping("/api/fiscal-years")
class FiscalYearController(private val fiscalYearService: FiscalYearService) {

    @GetMapping
    fun getAllFiscalYears(
        @RequestParam(required = false) companyId: Long?,
        @RequestParam(required = false) year: Int?
    ): ResponseEntity<ApiResponse<List<FiscalYearSummaryResponse>>> {
        // Implementation for getting all fiscal years with optional filtering
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Fiscal years retrieved successfully",
                data = listOf() // Replace with actual fiscal year data
            )
        )
    }

    @GetMapping("/{id}")
    fun getFiscalYearById(@PathVariable id: Long): ResponseEntity<ApiResponse<FiscalYearResponse>> {
        // Implementation for getting a specific fiscal year by ID
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Fiscal year retrieved successfully",
                data = null // Replace with actual fiscal year data
            )
        )
    }

    @GetMapping("/company/{companyId}/year/{year}")
    fun getFiscalYearByCompanyAndYear(
        @PathVariable companyId: Long,
        @PathVariable year: Int
    ): ResponseEntity<ApiResponse<FiscalYearResponse>> {
        // Implementation for getting a fiscal year by company ID and year
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Fiscal year retrieved successfully",
                data = null // Replace with actual fiscal year data
            )
        )
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'REGULATOR')")
    fun createFiscalYear(@Valid @RequestBody fiscalYearRequest: FiscalYearRequest): ResponseEntity<ApiResponse<FiscalYearResponse>> {
        // Implementation for creating a new fiscal year
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse(
                success = true,
                message = "Fiscal year created successfully",
                data = null // Replace with created fiscal year data
            )
        )
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGULATOR')")
    fun updateFiscalYear(
        @PathVariable id: Long,
        @Valid @RequestBody fiscalYearUpdateRequest: FiscalYearUpdateRequest
    ): ResponseEntity<ApiResponse<FiscalYearResponse>> {
        // Implementation for updating a fiscal year
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Fiscal year updated successfully",
                data = null // Replace with updated fiscal year data
            )
        )
    }

    @PutMapping("/{id}/audit")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGULATOR', 'AUDITOR')")
    fun markFiscalYearAsAudited(@PathVariable id: Long): ResponseEntity<ApiResponse<FiscalYearResponse>> {
        // Implementation for marking a fiscal year as audited
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Fiscal year marked as audited successfully",
                data = null // Replace with updated fiscal year data
            )
        )
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteFiscalYear(@PathVariable id: Long): ResponseEntity<ApiResponse<Void>> {
        // Implementation for deleting a fiscal year
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Fiscal year deleted successfully"
            )
        )
    }
}