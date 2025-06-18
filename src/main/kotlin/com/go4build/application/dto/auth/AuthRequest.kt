package com.base.application.dto.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * Request para login com SMS
 */
data class LoginRequest(
    @field:NotBlank(message = "Email é obrigatório")
    @field:Email(message = "Email inválido")
    val email: String,

    @field:NotBlank(message = "Senha é obrigatória")
    @field:Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    val password: String
)

/**
 * Request para refresh token
 */
data class RefreshTokenRequest(
    @field:NotBlank(message = "Refresh token é obrigatório")
    val refreshToken: String,
    val activeCompanyId: Long? = null
)

/**
 * Request para logout
 */
data class LogoutRequest(
    @field:NotBlank(message = "Access token é obrigatório")
    val accessToken: String
)
