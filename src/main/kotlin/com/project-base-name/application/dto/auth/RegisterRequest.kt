package com.projectbasename.application.dto.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * Request para registro de usuário comum
 * Telefone é OBRIGATÓRIO - será enviado SMS para verificação
 * (Login social usa fluxo diferente sem telefone obrigatório)
 */
data class RegisterRequest(
    @field:NotBlank(message = "Nome é obrigatório")
    @field:Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    val name: String,

    @field:NotBlank(message = "Email é obrigatório")
    @field:Email(message = "Email deve ser válido")
    val email: String,

    @field:NotBlank(message = "Telefone é obrigatório")
    @field:Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Formato de telefone inválido")
    val phone: String,

    @field:NotBlank(message = "Senha é obrigatória")
    @field:Size(min = 6, max = 255, message = "Senha deve ter entre 6 e 255 caracteres")
    val password: String,

    /**
     * Token de convite (opcional)
     * Se presente, indica que o usuário está sendo registrado via convite
     * UserType será definido como INVITED automaticamente
     */
    val invitationToken: String? = null
)