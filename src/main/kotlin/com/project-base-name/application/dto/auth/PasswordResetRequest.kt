package com.projectbasename.application.dto.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

/**
 * DTO para solicitação de recuperação de senha
 */
data class PasswordResetRequest(
    @field:NotBlank(message = "Email é obrigatório")
    @field:Email(message = "Email deve ser válido")
    val email: String
) 