package com.base.application.dto.company

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * DTO para criação de empresa de negócios (quando o usuário já existe)
 */
data class CreateBusinessCompanyRequest(
    @field:NotBlank(message = "Nome da empresa é obrigatório")
    @field:Size(min = 2, max = 150, message = "Nome deve ter entre 2 e 150 caracteres")
    val name: String,

    @field:NotBlank(message = "CNPJ é obrigatório")
    @field:Size(min = 14, max = 14, message = "CNPJ deve ter 14 dígitos")
    val document: String,

    @field:Email(message = "Email deve ter formato válido")
    val email: String? = null,

    @field:Size(min = 10, max = 20, message = "Telefone deve ter entre 10 e 20 caracteres")
    val phone: String? = null,

    val address: String? = null
)