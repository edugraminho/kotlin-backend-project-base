package com.base.application.security.verification

import com.base.application.dto.auth.SmsVerificationResponse
import com.base.application.dto.auth.SmsVerificationResult
import com.base.domain.exception.BusinessException
import com.base.domain.exception.ExceptionType
import com.base.domain.service.SmsService
import com.base.infrastructure.cache.SmsCodeCacheService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import kotlin.random.Random

/**
 * Serviço para gerenciar códigos de verificação SMS
 * Integra cache Redis com serviço de domínio SMS
 */
@Service
class SmsCodeService(
    private val smsService: SmsService,
    private val smsCodeCacheService: SmsCodeCacheService,
    @Value("\${verification.sms.expiration-minutes:5}") private val expirationMinutes: Long,
    @Value("\${verification.sms.max-attempts:3}") private val maxAttempts: Int,
    @Value("\${verification.sms.cooldown-seconds:60}") private val cooldownSeconds: Long
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    /**
     * Gera e envia código de verificação SMS usando o serviço de domínio
     */
    fun generateAndSendCode(phone: String): SmsVerificationResponse {
        try {
            val cleanPhone = cleanPhoneNumber(phone)
            validatePhoneNumber(cleanPhone)

            // Verifica se já existe código válido (cooldown)
            if (smsCodeCacheService.hasValidCode(cleanPhone)) {
                val remainingTime = smsCodeCacheService.getExpirationTime(cleanPhone)
                if (remainingTime > (expirationMinutes * 60 - cooldownSeconds)) {
                    throw BusinessException(ExceptionType.SMS_COOLDOWN_ACTIVE)
                }
            }

            val code = generateSixDigitCode()

            // Armazena código no cache antes de enviar
            smsCodeCacheService.storeCode(cleanPhone, code)

            // Usa o serviço de domínio para enviar SMS com template padronizado
            val messageId = smsService.sendVerificationSms(cleanPhone, code, expirationMinutes)

            log.info("Código SMS enviado com sucesso - MessageId: $messageId para ${maskPhone(cleanPhone)}")

            return SmsVerificationResponse(
                success = true,
                message = "Código enviado para ${maskPhone(cleanPhone)}",
                expiresIn = expirationMinutes * 60
            )

        } catch (e: BusinessException) {
            // Remove código do cache se houve erro após armazenar
            try {
                smsCodeCacheService.removeCode(cleanPhoneNumber(phone))
            } catch (ignored: Exception) {
            }
            throw e
        } catch (e: Exception) {
            log.error("Erro inesperado ao enviar código SMS para ${maskPhone(phone)}", e)
            // Remove código do cache se houve erro após armazenar
            try {
                smsCodeCacheService.removeCode(cleanPhoneNumber(phone))
            } catch (ignored: Exception) {
            }
            throw BusinessException(ExceptionType.SMS_SEND_ERROR)
        }
    }

    /**
     * Verifica código SMS fornecido pelo usuário
     */
    fun verifyCode(phone: String, code: String): SmsVerificationResult {
        try {
            val cleanPhone = cleanPhoneNumber(phone)
            val cleanCode = code.trim()

            validateCodeFormat(cleanCode)

            val isValid = smsCodeCacheService.validateCode(cleanPhone, cleanCode)

            return if (isValid) {
                log.info("Código SMS verificado com sucesso para ${maskPhone(cleanPhone)}")
                SmsVerificationResult(
                    success = true,
                    message = "Código verificado com sucesso"
                )
            } else {
                log.warn("Código SMS inválido para ${maskPhone(cleanPhone)}")
                SmsVerificationResult(
                    success = false,
                    message = "Código inválido ou expirado"
                )
            }

        } catch (e: BusinessException) {
            throw e
        } catch (e: Exception) {
            log.error("Erro ao verificar código SMS", e)
            return SmsVerificationResult(
                success = false,
                message = "Erro interno ao verificar código"
            )
        }
    }

    /**
     * Remove código do cache (útil para limpeza manual)
     */
    fun invalidateCode(phone: String) {
        try {
            val cleanPhone = cleanPhoneNumber(phone)
            smsCodeCacheService.removeCode(cleanPhone)
            log.info("Código SMS invalidado para ${maskPhone(cleanPhone)}")
        } catch (e: Exception) {
            log.error("Erro ao invalidar código SMS", e)
        }
    }

    /**
     * Verifica se existe código válido para o telefone
     */
    fun hasValidCode(phone: String): Boolean {
        return try {
            val cleanPhone = cleanPhoneNumber(phone)
            smsCodeCacheService.hasValidCode(cleanPhone)
        } catch (e: Exception) {
            log.error("Erro ao verificar código existente", e)
            false
        }
    }

    /**
     * Obtém tempo restante para expiração do código em segundos
     */
    fun getExpirationTime(phone: String): Long {
        return try {
            val cleanPhone = cleanPhoneNumber(phone)
            smsCodeCacheService.getExpirationTime(cleanPhone)
        } catch (e: Exception) {
            log.error("Erro ao obter tempo de expiração", e)
            0
        }
    }

    /**
     * Valida formato do telefone usando regras de negócio
     */
    fun isValidPhone(phone: String): Boolean {
        return try {
            val cleanPhone = cleanPhoneNumber(phone)
            smsService.isValidBrazilianPhone(cleanPhone)
        } catch (e: Exception) {
            false
        }
    }

    private fun generateSixDigitCode(): String {
        return Random.nextInt(100000, 999999).toString()
    }

    private fun cleanPhoneNumber(phone: String): String {
        // Remove espaços e caracteres especiais, mantém apenas números e +
        return phone.replace(Regex("[^0-9+]"), "")
    }

    private fun validatePhoneNumber(phone: String) {
        when {
            phone.isBlank() -> throw BusinessException(ExceptionType.INVALID_PHONE_FORMAT)
            !smsService.isValidBrazilianPhone(phone) -> throw BusinessException(ExceptionType.INVALID_PHONE_FORMAT)
        }
    }

    private fun validateCodeFormat(code: String) {
        when {
            code.isBlank() -> throw BusinessException(ExceptionType.INVALID_VERIFICATION_CODE)
            code.length != 6 -> throw BusinessException(ExceptionType.INVALID_VERIFICATION_CODE)
            !code.all { it.isDigit() } -> throw BusinessException(ExceptionType.INVALID_VERIFICATION_CODE)
        }
    }

    private fun maskPhone(phone: String): String {
        return if (phone.length >= 4) {
            "*".repeat(phone.length - 4) + phone.takeLast(4)
        } else phone
    }
}