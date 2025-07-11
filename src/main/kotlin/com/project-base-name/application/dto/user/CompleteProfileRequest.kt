package com.projectbasename.application.dto.user

import com.projectbasename.application.dto.company.CompanyResponse
import com.projectbasename.domain.enums.company.CompanyType
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern

/**
 * Request para completar perfil do usuário
 * Obrigatório para usuários UserType.OWNER
 */
data class CompleteProfileRequest(
    @field:NotNull(message = "Tipo de empresa é obrigatório")
    val companyType: CompanyType,

    @field:NotBlank(message = "Documento é obrigatório")
    @field:Pattern(
        regexp = "^[0-9.-/]+$", 
        message = "Documento deve conter apenas números, pontos, traços e barras"
    )
    val document: String,

    val phone: String? = null, 

    val name: String? = null, 

    @field:Email(message = "Email inválido")
    val email: String? = null, 

    val address: String? = null 
)

