package com.base.application.config

import com.base.domain.exception.BusinessException
import com.base.domain.exception.ExceptionType
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import java.time.LocalDateTime

/**
 * Tratamento global de exceções
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    /**
     * Trata exceções de negócio customizadas
     */
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(
        ex: BusinessException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Business exception: ${ex.code} - ${ex.message}")

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = ex.httpStatus.value(),
            error = ex.httpStatus.reasonPhrase,
            code = ex.code,
            message = ex.message ?: "Erro de negócio",
            details = ex.details,
            path = request.getDescription(false).removePrefix("uri=")
        )

        return ResponseEntity(errorResponse, ex.httpStatus)
    }

    /**
     * Trata erros de validação de campos
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(
        ex: MethodArgumentNotValidException,
        request: WebRequest
    ): ResponseEntity<ValidationErrorResponse> {
        logger.warn("Validation error: ${ex.message}")

        val fieldErrors = ex.bindingResult.allErrors.map { error ->
            when (error) {
                is FieldError -> FieldErrorDetail(
                    field = error.field,
                    rejectedValue = error.rejectedValue?.toString(),
                    message = error.defaultMessage ?: "Valor inválido"
                )

                else -> FieldErrorDetail(
                    field = "unknown",
                    rejectedValue = null,
                    message = error.defaultMessage ?: "Erro de validação"
                )
            }
        }

        val errorResponse = ValidationErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Validation Failed",
            code = ExceptionType.VALIDATION_ERROR.code,
            message = "Erro de validação nos campos",
            path = request.getDescription(false).removePrefix("uri="),
            fieldErrors = fieldErrors
        )

        return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
    }

    /**
     * Trata exceções genéricas não capturadas
     */
    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error", ex)

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            code = ExceptionType.INTERNAL_SERVER_ERROR.code,
            message = "Erro interno do servidor",
            details = if (isDevEnvironment()) ex.message else null,
            path = request.getDescription(false).removePrefix("uri=")
        )

        return ResponseEntity(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    /**
     * Verifica se está em ambiente de desenvolvimento
     */
    private fun isDevEnvironment(): Boolean {
        val activeProfiles = System.getProperty("spring.profiles.active", "")
        return activeProfiles.contains("dev") || activeProfiles.contains("local")
    }
}

/**
 * Estrutura de resposta para erros
 */
data class ErrorResponse(
    val timestamp: LocalDateTime,
    val status: Int,
    val error: String,
    val code: String,
    val message: String,
    val details: String? = null,
    val path: String
)

/**
 * Estrutura de resposta para erros de validação
 */
data class ValidationErrorResponse(
    val timestamp: LocalDateTime,
    val status: Int,
    val error: String,
    val code: String,
    val message: String,
    val path: String,
    val fieldErrors: List<FieldErrorDetail>
)

/**
 * Detalhe de erro de campo
 */
data class FieldErrorDetail(
    val field: String,
    val rejectedValue: String?,
    val message: String
)
