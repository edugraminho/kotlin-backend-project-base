package com.base.infrastructure.cache

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

/**
 * Serviço de cache Redis para códigos SMS
 * Gerencia armazenamento temporário e validação de códigos de verificação
 */
@Service
class SmsCodeCacheService(
    private val redisTemplate: RedisTemplate<String, String>,
    @Value("\${verification.sms.expiration-minutes:5}") private val expirationMinutes: Long,
    @Value("\${verification.sms.max-attempts:3}") private val maxAttempts: Int
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    companion object {
        private const val SMS_CODE_PREFIX = "sms_code:"
        private const val SMS_ATTEMPTS_PREFIX = "sms_attempts:"
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
     * Remove código do cache
     */
    fun removeCode(phone: String) {
        try {
            val key = getCodeKey(phone)
            val attemptsKey = getAttemptsKey(phone)

            redisTemplate.delete(key)
            redisTemplate.delete(attemptsKey)

            log.info("Código SMS removido para telefone: ${maskPhone(phone)}")
        } catch (e: Exception) {
            log.error("Erro ao remover código SMS", e)
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

    private fun getCodeKey(phone: String): String = SMS_CODE_PREFIX + phone
    
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