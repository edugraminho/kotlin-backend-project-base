package com.base.application.dto.company

import com.base.domain.enums.company.CompanyType
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CreateCompanyRequest(
    @field:NotBlank(message = "Nome é obrigatório")
    @field:Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    val name: String,

    @field:NotBlank(message = "Documento é obrigatório")
    @field:Size(min = 11, max = 14, message = "Documento deve ser CPF ou CNPJ válido")
    val document: String,

    @field:NotBlank(message = "Email é obrigatório")
    @field:Email(message = "Email inválido")
    val email: String,

    @field:NotBlank(message = "Telefone é obrigatório")
    @field:Size(min = 10, max = 20, message = "Telefone deve ter entre 10 e 20 caracteres")
    val phone: String,

    val address: String? = null,

    @field:NotNull(message = "Tipo da empresa é obrigatório")
    val companyType: CompanyType,

    @field:NotNull(message = "ID do proprietário é obrigatório")
    val ownerId: Long
) 