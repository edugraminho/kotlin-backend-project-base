package com.projectbasename.infrastructure.cache

import com.projectbasename.domain.exception.BusinessException
import com.projectbasename.domain.exception.ExceptionType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import kotlin.random.Random

/**
 * Serviço de cache para códigos SMS usando Redis
 * Também pode ser usado para códigos de email e outros códigos temporários
 */
@Service
class SmsCodeCacheService(
    private val redisTemplate: RedisTemplate<String, String>,
    @Value("\${verification.sms.expiration-minutes:5}") private val expirationMinutes: Long,
    @Value("\${verification.sms.max-attempts:3}") private val maxAttempts: Int,
    @Value("\${verification.sms.dev-mode:false}") private val devMode: Boolean
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    companion object {
        private const val SMS_CODE_PREFIX = "sms_code:"
        private const val SMS_COOLDOWN_PREFIX = "sms_cooldown:"
        private const val SMS_ATTEMPTS_PREFIX = "sms_attempts:"
    }

    /**
     * Gera e salva código SMS no cache
     */
    fun generateAndSaveCode(phone: String, expirationMinutes: Long): String {
        val code = generateSixDigitCode()
        val key = getCodeKey(phone)

        try {
            redisTemplate.opsForValue().set(key, code, Duration.ofMinutes(expirationMinutes))
        } catch (e: Exception) {
            log.error("Erro ao salvar código SMS no cache para $phone", e)
            throw BusinessException(ExceptionType.SMS_SEND_ERROR)
        }

        return code
    }

    /**
     * Verifica código SMS
     */
    fun verifyCode(phone: String, providedCode: String): Boolean {
        val key = getCodeKey(phone)

        return try {
            val storedCode = redisTemplate.opsForValue().get(key)
            if (storedCode != null && storedCode == providedCode) {
                // Remove código após verificação bem-sucedida
                redisTemplate.delete(key)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            log.error("Erro ao verificar código SMS para $phone", e)
            false
        }
    }

    /**
     * Aplica cooldown para SMS
     */
    fun applySmsCodeCooldown(phone: String, cooldownSeconds: Int) {
        val key = getSmsCodeCooldownKey(phone)

        try {
            redisTemplate.opsForValue().set(key, "cooldown", Duration.ofSeconds(cooldownSeconds.toLong()))
        } catch (e: Exception) {
            log.error("Erro ao aplicar cooldown SMS para $phone", e)
        }
    }

    /**
     * Verifica se telefone está em cooldown
     */
    fun isOnSmsCodeCooldown(phone: String): Boolean {
        val key = getSmsCodeCooldownKey(phone)

        return try {
            redisTemplate.hasKey(key)
        } catch (e: Exception) {
            log.error("Erro ao verificar cooldown SMS para $phone", e)
            false
        }
    }

    /**
     * Remove código SMS do cache
     */
    fun removeSmsCode(phone: String) {
        val key = getCodeKey(phone)

        try {
            redisTemplate.delete(key)
        } catch (e: Exception) {
            log.error("Erro ao remover código SMS para $phone", e)
        }
    }

    /**
     * Salva código genérico no cache
     */
    fun saveCode(key: String, code: String, expirationMinutes: Long) {
        try {
            redisTemplate.opsForValue().set(
                key,
                code,
                Duration.ofMinutes(expirationMinutes)
            )
        } catch (e: Exception) {
            log.error("Erro ao salvar código no cache: $key", e)
            throw BusinessException(ExceptionType.EMAIL_SEND_ERROR)
        }
    }

    /**
     * Busca código do cache
     */
    fun getCode(key: String): String? {
        return try {
            redisTemplate.opsForValue().get(key)
        } catch (e: Exception) {
            log.error("Erro ao buscar código do cache: $key", e)
            null
        }
    }

    /**
     * Remove código genérico do cache
     */
    fun removeCode(key: String) {
        try {
            redisTemplate.delete(key)
        } catch (e: Exception) {
            log.error("Erro ao remover código do cache: $key", e)
        }
    }

    /**
     * Verifica se está em cooldown
     */
    fun isOnCooldown(key: String): Boolean {
        return try {
            redisTemplate.hasKey(key)
        } catch (e: Exception) {
            log.error("Erro ao verificar cooldown: $key", e)
            false
        }
    }

    /**
     * Aplica cooldown
     */
    fun applyCooldown(key: String, durationSeconds: Int) {
        try {
            redisTemplate.opsForValue().set(
                key,
                "cooldown",
                Duration.ofSeconds(durationSeconds.toLong())
            )
        } catch (e: Exception) {
            log.error("Erro ao aplicar cooldown: $key", e)
        }
    }

    private fun getCodeKey(phone: String): String = SMS_CODE_PREFIX + phone

    private fun getSmsCodeCooldownKey(phone: String): String = SMS_COOLDOWN_PREFIX + phone

    private fun generateSixDigitCode(): String {
        return Random.nextInt(100000, 999999).toString()
    }

    /**
     * Armazena código SMS no cache
     * @param phone Número de telefone
     * @param code Código de 6 dígitos
     */
    fun storeCode(phone: String, code: String) {
        val key = getCodeKey(phone)
        val attemptsKey = getAttemptsKey(phone)

        try {
            redisTemplate.opsForValue().set(
                key,
                code,
                Duration.ofMinutes(expirationMinutes)
            )

            // Reset attempts counter
            redisTemplate.delete(attemptsKey)

            log.info("Código SMS armazenado para telefone: ${maskPhone(phone)}")
        } catch (e: Exception) {
            log.error("Erro ao armazenar código SMS no cache", e)
            throw e
        }
    }

    /**
     * Valida código SMS
     * @param phone Número de telefone
     * @param code Código fornecido pelo usuário
     * @return true se válido, false caso contrário
     */
    fun validateCode(phone: String, code: String): Boolean {
        // Em modo dev, aceita qualquer código de 6 dígitos
        if (devMode && code.length == 6 && code.all { it.isDigit() }) {
            log.info("Modo DEV: Código aceito automaticamente para ${maskPhone(phone)}")
            return true
        }

        val key = getCodeKey(phone)
        val attemptsKey = getAttemptsKey(phone)

        try {
            // Verifica se excedeu tentativas
            val attempts = getCurrentAttempts(phone)
            if (attempts >= maxAttempts) {
                log.warn("Máximo de tentativas excedido para telefone: ${maskPhone(phone)}")
                return false
            }

            // Busca código armazenado
            val storedCode = redisTemplate.opsForValue().get(key)

            if (storedCode == null) {
                log.warn("Código não encontrado ou expirado para telefone: ${maskPhone(phone)}")
                incrementAttempts(phone)
                return false
            }

            val isValid = storedCode == code

            if (isValid) {
                // Remove código e tentativas após validação bem-sucedida
                redisTemplate.delete(key)
                redisTemplate.delete(attemptsKey)
                log.info("Código SMS validado com sucesso para telefone: ${maskPhone(phone)}")
            } else {
                incrementAttempts(phone)
                log.warn("Código SMS inválido para telefone: ${maskPhone(phone)}")
            }

            return isValid
        } catch (e: Exception) {
            log.error("Erro ao validar código SMS", e)
            return false
        }
    }

    /**
     * Verifica se existe código válido para o telefone
     */
    fun hasValidCode(phone: String): Boolean {
        return try {
            val key = getCodeKey(phone)
            redisTemplate.hasKey(key)
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
            val key = getCodeKey(phone)
            val expiration = redisTemplate.getExpire(key)
            if (expiration > 0) expiration else 0
        } catch (e: Exception) {
            log.error("Erro ao obter tempo de expiração", e)
            0
        }
    }
    
    private fun getAttemptsKey(phone: String): String = SMS_ATTEMPTS_PREFIX + phone

    private fun getCurrentAttempts(phone: String): Int {
        return try {
            val attemptsKey = getAttemptsKey(phone)
            redisTemplate.opsForValue().get(attemptsKey)?.toIntOrNull() ?: 0
        } catch (e: Exception) {
            log.error("Erro ao obter tentativas atuais", e)
            0
        }
    }

    private fun incrementAttempts(phone: String) {
        try {
            val attemptsKey = getAttemptsKey(phone)
            val currentAttempts = getCurrentAttempts(phone)

            redisTemplate.opsForValue().set(
                attemptsKey,
                (currentAttempts + 1).toString(),
                Duration.ofMinutes(expirationMinutes)
            )
        } catch (e: Exception) {
            log.error("Erro ao incrementar tentativas", e)
        }
    }

    private fun maskPhone(phone: String): String {
        return if (phone.length >= 4) {
            "*".repeat(phone.length - 4) + phone.takeLast(4)
        } else phone
    }
}