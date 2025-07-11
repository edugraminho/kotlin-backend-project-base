package com.projectbasename.application.dto.company

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size

/**
 * DTO para atualização de empresa
 */
data class UpdateCompanyRequest(
    @field:Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    val name: String? = null,

    @field:Size(min = 11, max = 14, message = "Documento deve ser CPF ou CNPJ válido")
    val document: String? = null,

    @field:Email(message = "Email inválido")
    val email: String? = null,

    @field:Size(min = 10, max = 20, message = "Telefone deve ter entre 10 e 20 caracteres")
    val phone: String? = null,

    val address: String? = null,

    val activePlanId: Long? = null
)
