package com.projectbasename.application.dto.auth

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * DTO para redefinição de senha usando token de recuperação
 */
data class ResetPasswordRequest(
    @field:NotBlank(message = "Token é obrigatório")
    val token: String,

    @field:NotBlank(message = "Nova senha é obrigatória")
    @field:Size(min = 6, max = 255, message = "Nova senha deve ter entre 6 e 255 caracteres")
    val newPassword: String
) 