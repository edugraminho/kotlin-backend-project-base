package com.projectbasename.application.dto.auth

/**
 * DTO de resposta para operações de recuperação de senha
 */
data class PasswordResetResponse(
    /**
     * Indica se a operação foi bem-sucedida
     */
    val success: Boolean,

    /**
     * Mensagem descritiva do resultado
     */
    val message: String,

    /**
     * Tempo de expiração em segundos (opcional)
     */
    val expiresIn: Long? = null
) 