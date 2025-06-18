package com.base.application.dto.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * Request para registro de usuário
 */
data class RegisterRequest(
    @field:NotBlank(message = "Nome é obrigatório")
    @field:Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    val name: String,

    @field:NotBlank(message = "Email é obrigatório")
    @field:Email(message = "Email inválido")
    val email: String,

    @field:NotBlank(message = "Senha é obrigatória")
    @field:Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    val password: String,

    @field:NotBlank(message = "Telefone é obrigatório")
    @field:Pattern(regexp = "^\\+?[1-9]\\d{1,14}\$", message = "Formato de telefone inválido")
    val phone: String,

    @field:NotBlank(message = "Documento é obrigatório")
    @field:Size(min = 11, max = 14, message = "Documento deve ter entre 11 e 14 caracteres")
    val document: String,

    val address: String? = null,
    val profileImageUrl: String? = null
)
