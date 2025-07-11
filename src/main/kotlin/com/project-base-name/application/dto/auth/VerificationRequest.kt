package com.projectbasename.application.dto.auth

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

/**
 * Request para verificação de código SMS
 */
data class VerifySmsRequest(
    @field:NotBlank(message = "Token temporário é obrigatório")
    val tempToken: String,

    @field:NotBlank(message = "Código é obrigatório")
    @field:Pattern(regexp = "^\\d{6}$", message = "Código deve ter 6 dígitos")
    val code: String,

    val activeCompanyId: Long? = null
)

/**
 * Request para ativar conta após registro
 */
data class ActivateAccountRequest(
    @field:NotBlank(message = "Token temporário é obrigatório")
    val tempToken: String,

    @field:Pattern(regexp = "\\d{6}", message = "Código deve ter 6 dígitos")
    val code: String
)

/**
 * Request para reenviar código
 */
data class ResendCodeRequest(
    @field:NotBlank(message = "Token temporário é obrigatório")
    val tempToken: String
)
