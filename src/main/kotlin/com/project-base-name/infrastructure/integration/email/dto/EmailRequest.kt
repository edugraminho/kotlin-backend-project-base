package com.projectbasename.infrastructure.integration.email.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * DTO para requisição de envio de email
 */
data class EmailRequest(
    @field:NotBlank(message = "Email destinatário é obrigatório")
    @field:Email(message = "Email destinatário deve ser válido")
    val to: String,

    @field:NotBlank(message = "Assunto é obrigatório")
    @field:Size(max = 255, message = "Assunto deve ter no máximo 255 caracteres")
    val subject: String,

    @field:NotBlank(message = "Corpo do email é obrigatório")
    val body: String,

    /**
     * Se verdadeiro, o corpo será interpretado como HTML
     */
    val isHtml: Boolean = false,

    /**
     * Email de resposta (opcional)
     */
    val replyTo: String? = null,

    /**
     * Emails em cópia (opcional)
     */
    val cc: List<String>? = null,

    /**
     * Emails em cópia oculta (opcional)
     */
    val bcc: List<String>? = null
) 