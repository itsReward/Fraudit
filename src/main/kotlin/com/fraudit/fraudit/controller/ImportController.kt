package com.fraudit.fraudit.controller

import com.fraudit.fraudit.dto.common.ApiResponse
import com.fraudit.fraudit.util.CsvDatabaseImportUtil
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/api/admin/import")
@PreAuthorize("hasRole('ADMIN')")
class ImportController(
    private val csvDatabaseImportUtil: CsvDatabaseImportUtil
) {
    private val logger = LoggerFactory.getLogger(ImportController::class.java)

    @PostMapping("/csv")
    fun importCsvData(
        @RequestParam("file") file: MultipartFile,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        try {
            val userId = UUID.fromString(userDetails.username)
            logger.info("Starting CSV import: ${file.originalFilename} (${file.size} bytes)")

            val result = csvDatabaseImportUtil.importSyntheticDataFromCsv(file, userId)

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "CSV data imported successfully",
                    data = result
                )
            )
        } catch (e: Exception) {
            logger.error("Error importing CSV data: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error importing CSV data",
                    errors = listOf(e.message ?: "An unexpected error occurred")
                )
            )
        }
    }

    @GetMapping("/status")
    fun getImportStatus(): ResponseEntity<ApiResponse<Map<String, Any>>> {
        // This could be enhanced to track long-running import jobs
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Import status retrieved",
                data = mapOf("status" to "No active imports")
            )
        )
    }
}