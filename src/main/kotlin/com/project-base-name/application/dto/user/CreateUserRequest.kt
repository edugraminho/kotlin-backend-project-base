package com.projectbasename.application.dto.user

import com.projectbasename.domain.enums.company.CompanyType
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/**
 * Request para criação completa de usuário com empresa
 * Phone é opcional - será usado fallback se necessário
 */
data class CreateUserRequest(
    @field:NotBlank(message = "Nome é obrigatório")
    @field:Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    val name: String,

    @field:NotBlank(message = "Email é obrigatório")
    @field:Email(message = "Email deve ser válido")
    val email: String,

    @field:Size(min = 10, max = 20, message = "Telefone deve ter entre 10 e 20 caracteres")
    val phone: String? = null,

    @field:NotBlank(message = "Senha é obrigatória")
    @field:Size(min = 6, max = 255, message = "Senha deve ter entre 6 e 255 caracteres")
    val password: String,

    @field:NotBlank(message = "Documento é obrigatório")
    @field:Size(min = 11, max = 14, message = "Documento deve ser CPF ou CNPJ válido")
    val document: String,

    @field:NotNull(message = "Tipo da empresa é obrigatório")
    val companyType: CompanyType,

    val companyName: String? = null,
    val companyDocument: String? = null,
    val companyEmail: String? = null,
    val companyPhone: String? = null,
    val companyAddress: String? = null
)