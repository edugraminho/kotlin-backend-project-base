package com.projectbasename.application.dto.user

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size

/**
 * DTO para atualização de usuário
 */
data class UpdateUserRequest(
    @field:Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    val name: String? = null,

    @field:Email(message = "Email inválido")
    val email: String? = null,

    @field:Size(min = 10, max = 20, message = "Telefone deve ter entre 10 e 20 caracteres")
    val phone: String? = null,

    @field:Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    val password: String? = null,

    @field:Size(min = 11, max = 14, message = "Documento deve ser CPF ou CNPJ válido")
    val document: String? = null,

    val address: String? = null,

    val profileImageUrl: String? = null
)
