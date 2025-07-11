package com.projectbasename.infrastructure.integration.email.dto

import java.time.LocalDateTime

/**
 * DTO de resposta para operações de email
 */
data class EmailResponse(
    /**
     * Indica se o email foi enviado com sucesso
     */
    val success: Boolean,

    /**
     * Mensagem descritiva do resultado
     */
    val message: String,

    /**
     * ID único do email (para tracking, se disponível)
     */
    val messageId: String? = null,

    /**
     * Timestamp do envio
     */
    val sentAt: LocalDateTime = LocalDateTime.now(),

    /**
     * Email do destinatário
     */
    val recipient: String? = null,

    /**
     * Detalhes do erro, se houver
     */
    val errorDetails: String? = null
) 