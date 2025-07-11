package com.projectbasename.domain.service

import com.projectbasename.application.dto.auth.PasswordResetRequest
import com.projectbasename.application.dto.auth.PasswordResetResponse
import com.projectbasename.application.dto.auth.ResetPasswordRequest
import com.projectbasename.domain.entity.PasswordResetTokenEntity
import com.projectbasename.domain.exception.BusinessException
import com.projectbasename.domain.exception.ExceptionType
import com.projectbasename.domain.repository.PasswordResetTokenRepository
import com.projectbasename.infrastructure.cache.TokenCacheService
import com.projectbasename.infrastructure.integration.email.EmailService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

/**
 * Serviço de recuperação de senha
 */
@Service
@Transactional
class PasswordResetService(
    private val userService: UserService,
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val emailService: EmailService,
    private val passwordEncoder: PasswordEncoder,
    private val tokenCacheService: TokenCacheService,
    @Value("\${api.server.url:http://localhost:8080}") private val baseUrl: String,
    @Value("\${password-reset.token.expiration-hours:1}") private val tokenExpirationHours: Long,
    @Value("\${password-reset.cooldown-minutes:5}") private val cooldownMinutes: Long
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    /**
     * Solicita recuperação de senha
     */
    fun requestPasswordReset(request: PasswordResetRequest): PasswordResetResponse {
        // Verificar cooldown para evitar spam
        val cooldownKey = "password_reset:${request.email}"
        if (tokenCacheService.isRateLimited(cooldownKey, cooldownMinutes.toInt())) {
            throw BusinessException(ExceptionType.EMAIL_COOLDOWN_ACTIVE)
        }

        // Buscar usuário pelo email
        val user = userService.findUserEntityByEmail(request.email)
            ?: throw BusinessException(ExceptionType.USER_NOT_FOUND)

        // Invalidar tokens anteriores do usuário
        passwordResetTokenRepository.invalidateAllByUserId(user.id!!)

        // Gerar novo token
        val token = generateResetToken()
        val expiresAt = LocalDateTime.now().plusHours(tokenExpirationHours)

        val resetTokenEntity = PasswordResetTokenEntity(
            token = token,
            userId = user.id!!,
            expiresAt = expiresAt,
            used = false
        )

        passwordResetTokenRepository.save(resetTokenEntity)

        // Enviar email de recuperação
        try {
            val emailResponse = emailService.sendPasswordResetEmail(
                recipientEmail = user.email,
                userName = user.name,
                resetToken = token,
                baseUrl = baseUrl
            )

            if (!emailResponse.success) {
                log.warn("Falha ao enviar email de recuperação: ${emailResponse.message}")
                throw BusinessException(ExceptionType.EMAIL_SEND_ERROR)
            }

            log.info("Email de recuperação enviado para: ${maskEmail(user.email)}")

            // Aplicar cooldown
            tokenCacheService.applyRateLimit(cooldownKey, cooldownMinutes.toInt())

        } catch (e: Exception) {
            log.error("Erro ao enviar email de recuperação", e)
            throw BusinessException(ExceptionType.EMAIL_SEND_ERROR)
        }

        return PasswordResetResponse(
            success = true,
            message = "Email de recuperação enviado com sucesso",
            expiresIn = tokenExpirationHours * 3600 // em segundos
        )
    }

    /**
     * Valida token de recuperação
     */
    fun validateResetToken(token: String): Boolean {
        val resetToken = passwordResetTokenRepository.findByTokenAndUsedFalse(token)
            ?: return false

        return !resetToken.isExpired()
    }

    /**
     * Redefine senha usando token
     */
    fun resetPassword(request: ResetPasswordRequest): PasswordResetResponse {
        val resetToken = passwordResetTokenRepository.findByTokenAndUsedFalse(request.token)
            ?: throw BusinessException(ExceptionType.INVALID_TOKEN)

        if (resetToken.isExpired()) {
            throw BusinessException(ExceptionType.TOKEN_EXPIRED)
        }

        val user = userService.findUserEntityById(resetToken.userId)

        // Atualizar senha do usuário
        user.password = passwordEncoder.encode(request.newPassword)
        user.updateTimestamp()
        userService.updateUserEntity(user)

        // Marcar token como usado
        resetToken.used = true
        resetToken.usedAt = LocalDateTime.now()
        passwordResetTokenRepository.save(resetToken)

        // Revogar todos os tokens JWT do usuário por segurança
        tokenCacheService.revokeAllUserTokens(user.id!!)

        log.info("Senha redefinida com sucesso para usuário: ${maskEmail(user.email)}")

        return PasswordResetResponse(
            success = true,
            message = "Senha redefinida com sucesso"
        )
    }

    /**
     * Busca dados do token para exibição
     */
    fun getTokenInfo(token: String): Map<String, Any> {
        val resetToken = passwordResetTokenRepository.findByTokenAndUsedFalse(token)
            ?: throw BusinessException(ExceptionType.INVALID_TOKEN)

        if (resetToken.isExpired()) {
            throw BusinessException(ExceptionType.TOKEN_EXPIRED)
        }

        val user = userService.findUserEntityById(resetToken.userId)

        return mapOf(
            "email" to maskEmail(user.email),
            "userName" to user.name,
            "expiresAt" to resetToken.expiresAt,
            "isValid" to (!resetToken.isExpired() && !resetToken.used)
        )
    }

    /**
     * Gera token de recuperação seguro
     */
    private fun generateResetToken(): String {
        return UUID.randomUUID().toString().replace("-", "")
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