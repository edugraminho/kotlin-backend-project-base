package com.projectbasename.application.security.service

import com.projectbasename.domain.exception.BusinessException
import com.projectbasename.domain.exception.ExceptionType
import com.projectbasename.infrastructure.cache.SmsCodeCacheService
import com.projectbasename.infrastructure.integration.email.EmailService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import kotlin.random.Random

/**
 * Serviço de verificação por email
 * Integrado com o sistema de email Microsoft 365
 */
@Service
class EmailVerificationService(
    private val emailService: EmailService,
    private val smsCodeCacheService: SmsCodeCacheService,
    @Value("\${verification.email.expiration-minutes:10}") private val expirationMinutes: Long
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    /**
     * Envia código de verificação por email
     */
    fun sendEmailVerification(email: String, userId: Long): EmailVerificationResponse {
        val code = generateSixDigitCode()

        // Salvar código no cache (reutilizando SmsCodeCacheService)
        val cacheKey = "email_verification:$email"
        smsCodeCacheService.saveCode(cacheKey, code, expirationMinutes)

        try {
            // Buscar nome do usuário (opcional, se necessário)
            val userName = extractNameFromEmail(email)

            val emailResponse = emailService.sendVerificationCodeEmail(
                recipientEmail = email,
                userName = userName,
                verificationCode = code,
                expirationMinutes = expirationMinutes
            )

            if (!emailResponse.success) {
                log.warn("Falha ao enviar email de verificação: ${emailResponse.message}")
                throw BusinessException(ExceptionType.EMAIL_SEND_ERROR)
            }

            log.info("Código de verificação enviado por email para: ${maskEmail(email)}")

        } catch (e: BusinessException) {
            throw e
        } catch (e: Exception) {
            log.error("Erro inesperado ao enviar email de verificação", e)
            throw BusinessException(ExceptionType.EMAIL_SEND_ERROR)
        }

        return EmailVerificationResponse(
            success = true,
            message = "Código enviado para $email",
            expiresIn = expirationMinutes * 60 // em segundos
        )
    }

    /**
     * Verifica código de email
     */
    fun verifyEmailCode(email: String, code: String): EmailVerificationResult {
        val cacheKey = "email_verification:$email"
        
        val storedCode = smsCodeCacheService.getCode(cacheKey)
            ?: return EmailVerificationResult(false, "Código inválido ou expirado")

        if (storedCode != code) {
            return EmailVerificationResult(false, "Código inválido")
        }

        // Remover código do cache após verificação bem-sucedida
        smsCodeCacheService.removeCode(cacheKey)

        log.info("Email verificado com sucesso: ${maskEmail(email)}")

        return EmailVerificationResult(true, "Email verificado com sucesso")
    }

    /**
     * Reenvia código de verificação
     */
    fun resendVerificationCode(email: String, userId: Long): EmailVerificationResponse {
        // Verificar cooldown para evitar spam
        val cooldownKey = "email_cooldown:$email"
        if (smsCodeCacheService.isOnCooldown(cooldownKey)) {
            throw BusinessException(ExceptionType.EMAIL_COOLDOWN_ACTIVE)
        }

        // Aplicar cooldown antes de enviar
        smsCodeCacheService.applyCooldown(cooldownKey, 60) // 1 minuto de cooldown

        return sendEmailVerification(email, userId)
    }

    /**
     * Gera código de seis dígitos
     */
    private fun generateSixDigitCode(): String {
        return Random.nextInt(100000, 999999).toString()
    }

    /**
     * Extrai nome do email (parte antes do @)
     */
    private fun extractNameFromEmail(email: String): String {
        return email.substringBefore("@")
            .replace(".", " ")
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { it.uppercase() }
}
    }

    /**
     * Mascara email para logs
     */
    private fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return email
        
        val username = parts[0]
        val domain = parts[1]
        
        val maskedUsername = when {
            username.length <= 2 -> username
            username.length <= 4 -> username.take(2) + "*".repeat(username.length - 2)
            else -> username.take(2) + "*".repeat(username.length - 4) + username.takeLast(2)
        }
        
        return "$maskedUsername@$domain"
    }
}

/**
 * Resposta de verificação de email
 */
data class EmailVerificationResponse(
    val success: Boolean,
    val message: String,
    val expiresIn: Long = 0
)

/**
 * Resultado de verificação de código
 */
data class EmailVerificationResult(
    val success: Boolean,
    val message: String
)
