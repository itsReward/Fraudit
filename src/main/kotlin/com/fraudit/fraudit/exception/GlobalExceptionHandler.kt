package com.fraudit.fraudit.exception

import com.fraudit.fraudit.dto.common.ApiResponse
import com.fraudit.fraudit.dto.error.ErrorResponse
import com.fraudit.fraudit.dto.error.ValidationError
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.MaxUploadSizeExceededException
import java.time.OffsetDateTime

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(EntityNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleEntityNotFound(ex: EntityNotFoundException, request: WebRequest): ResponseEntity<ApiResponse<Void>> {
        logger.error("Resource not found: ${ex.message}")

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiResponse(
                success = false,
                message = ex.message ?: "Resource not found",
                errors = listOf(ex.message ?: "Resource not found")
            )
        )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgument(ex: IllegalArgumentException, request: WebRequest): ResponseEntity<ApiResponse<Void>> {
        logger.error("Bad request: ${ex.message}")

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiResponse(
                success = false,
                message = ex.message ?: "Invalid request parameters",
                errors = listOf(ex.message ?: "Invalid request parameters")
            )
        )
    }

    @ExceptionHandler(IllegalStateException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleIllegalState(ex: IllegalStateException, request: WebRequest): ResponseEntity<ApiResponse<Void>> {
        logger.error("Conflict: ${ex.message}")

        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ApiResponse(
                success = false,
                message = ex.message ?: "Operation could not be completed due to current state",
                errors = listOf(ex.message ?: "Operation could not be completed due to current state")
            )
        )
    }

    @ExceptionHandler(BadCredentialsException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleBadCredentials(ex: BadCredentialsException, request: WebRequest): ResponseEntity<ApiResponse<Void>> {
        logger.error("Authentication failed: ${ex.message}")

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            ApiResponse(
                success = false,
                message = "Invalid username or password",
                errors = listOf("Invalid username or password")
            )
        )
    }

    @ExceptionHandler(AccessDeniedException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleAccessDenied(ex: AccessDeniedException, request: WebRequest): ResponseEntity<ApiResponse<Void>> {
        logger.error("Access denied: ${ex.message}")

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ApiResponse(
                success = false,
                message = "Access denied: you don't have permission to access this resource",
                errors = listOf("Access denied: you don't have permission to access this resource")
            )
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException, request: WebRequest): ResponseEntity<ApiResponse<List<ValidationError>>> {
        logger.error("Validation error: ${ex.message}")

        val validationErrors = ex.bindingResult.allErrors.map { error ->
            ValidationError(
                field = (error as? FieldError)?.field ?: "unknown",
                message = error.defaultMessage ?: "Validation failed"
            )
        }

        val errorMessages = validationErrors.map { "${it.field}: ${it.message}" }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiResponse(
                success = false,
                message = "Validation failed",
                errors = errorMessages,
                data = validationErrors
            )
        )
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleGenericException(ex: Exception, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.error("Internal server error: ${ex.message}", ex)

        val errorResponse = ErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            message = "An unexpected error occurred",
            path = request.getDescription(false).substring(4),
            timestamp = OffsetDateTime.now()
        )

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxSizeException(exc: MaxUploadSizeExceededException, request: WebRequest): ResponseEntity<Any> {
        return ResponseEntity
            .status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body("File size exceeds the maximum allowed limit")
    }

    @ExceptionHandler(NumberFormatException::class, MethodArgumentTypeMismatchException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleNumberFormatException(ex: Exception, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.error("Bad request - Invalid number format: ${ex.message}", ex)

        val errorResponse = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            message = when (ex) {
                is NumberFormatException -> "Invalid number format: ${ex.message}"
                is MethodArgumentTypeMismatchException -> {
                    val paramName = ex.name ?: "unknown"
                    "Invalid value for parameter '$paramName'. Expected a valid number."
                }
                else -> "Invalid number format in request"
            },
            path = request.getDescription(false).substring(4),
            timestamp = OffsetDateTime.now()
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }


}