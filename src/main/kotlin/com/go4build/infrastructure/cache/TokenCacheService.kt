package com.base.infrastructure.cache

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Serviço de cache Redis para tokens e tentativas de login
 * Gerencia revogação de tokens, controle de tentativas de login e rate limiting
 */
@Service
class TokenCacheService(
    private val redisTemplate: RedisTemplate<String, String>,
    @Value("\${jwt.access-token-expiration-seconds:86400}") private val tokenExpirationSeconds: Long,
    @Value("\${auth.login.max-attempts:5}") private val maxLoginAttempts: Int,
    @Value("\${auth.login.lockout-minutes:15}") private val lockoutMinutes: Long,
    @Value("\${auth.rate-limit.tokens-per-minute:60}") private val tokensPerMinute: Int
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    companion object {
        private const val REVOKED_TOKEN_PREFIX = "revoked_token:"
        private const val LOGIN_ATTEMPTS_PREFIX = "login_attempts:"
        private const val LOGIN_LOCKOUT_PREFIX = "login_lockout:"
        private const val TOKEN_FAMILY_PREFIX = "token_family:"
        private const val RATE_LIMIT_PREFIX = "rate_limit:"
    }

    /**
     * Adiciona token à lista de revogados
     */
    fun revokeToken(token: String, userId: Long? = null) {
        try {
            val key = getRevokedTokenKey(token)
            redisTemplate.opsForValue().set(
                key,
                Instant.now().toString(),
                Duration.ofSeconds(tokenExpirationSeconds * 2)
            )
            
            // Se tiver userId, adiciona ao token family para revogar tokens relacionados
            userId?.let {
                val familyKey = getTokenFamilyKey(it)
                redisTemplate.opsForSet().add(familyKey, token)
                redisTemplate.expire(familyKey, Duration.ofSeconds(tokenExpirationSeconds * 2))
            }
            
            log.debug("Token revogado: ${maskToken(token)}")
        } catch (e: Exception) {
            log.error("Erro ao revogar token", e)
            throw e
        }
    }

    /**
     * Verifica se token está revogado
     */
    fun isTokenRevoked(token: String): Boolean {
        return try {
            val key = getRevokedTokenKey(token)
            redisTemplate.hasKey(key)
        } catch (e: Exception) {
            log.error("Erro ao verificar token revogado", e)
            false
        }
    }

    /**
     * Verifica se token pertence a uma família revogada
     */
    fun isTokenFamilyRevoked(userId: Long, token: String): Boolean {
        return try {
            val familyKey = getTokenFamilyKey(userId)
            redisTemplate.opsForSet().isMember(familyKey, token)
        } catch (e: Exception) {
            log.error("Erro ao verificar família de token", e)
            false
        }
    }

    /**
     * Incrementa contador de tentativas de login
     */
    fun incrementLoginAttempts(email: String) {
        try {
            val attemptsKey = getLoginAttemptsKey(email)
            val currentAttempts = getCurrentLoginAttempts(email)

            redisTemplate.opsForValue().set(
                attemptsKey,
                (currentAttempts + 1).toString(),
                Duration.ofMinutes(lockoutMinutes)
            )

            // Se atingiu limite, adiciona lockout
            if (currentAttempts + 1 >= maxLoginAttempts) {
                val lockoutKey = getLoginLockoutKey(email)
                redisTemplate.opsForValue().set(
                    lockoutKey,
                    Instant.now().toString(),
                    Duration.ofMinutes(lockoutMinutes)
                )
                log.warn("Conta bloqueada por excesso de tentativas: ${maskEmail(email)}")
            }

            log.debug("Tentativa de login incrementada para: ${maskEmail(email)}")
        } catch (e: Exception) {
            log.error("Erro ao incrementar tentativas de login", e)
            throw e
        }
    }

    /**
     * Reseta contador de tentativas de login
     */
    fun resetLoginAttempts(email: String) {
        try {
            val attemptsKey = getLoginAttemptsKey(email)
            val lockoutKey = getLoginLockoutKey(email)

            redisTemplate.delete(attemptsKey)
            redisTemplate.delete(lockoutKey)

            log.debug("Tentativas de login resetadas para: ${maskEmail(email)}")
        } catch (e: Exception) {
            log.error("Erro ao resetar tentativas de login", e)
            throw e
        }
    }

    /**
     * Verifica se email está bloqueado por excesso de tentativas
     */
    fun isLoginLocked(email: String): Boolean {
        return try {
            val lockoutKey = getLoginLockoutKey(email)
            redisTemplate.hasKey(lockoutKey)
        } catch (e: Exception) {
            log.error("Erro ao verificar bloqueio de login", e)
            false
        }
    }

    /**
     * Obtém tempo restante do bloqueio em segundos
     */
    fun getLockoutRemainingTime(email: String): Long {
        return try {
            val lockoutKey = getLoginLockoutKey(email)
            val expiration = redisTemplate.getExpire(lockoutKey)
            if (expiration > 0) expiration else 0
        } catch (e: Exception) {
            log.error("Erro ao obter tempo restante do bloqueio", e)
            0
        }
    }

    /**
     * Verifica rate limit para operações com token
     */
    fun checkRateLimit(key: String): Boolean {
        return try {
            val rateKey = getRateLimitKey(key)
            val currentCount = redisTemplate.opsForValue().increment(rateKey) ?: 1
            
            if (currentCount == 1L) {
                redisTemplate.expire(rateKey, 1, TimeUnit.MINUTES)
            }
            
            currentCount <= tokensPerMinute
        } catch (e: Exception) {
            log.error("Erro ao verificar rate limit", e)
            true // Em caso de erro, permite a operação
        }
    }

    private fun getCurrentLoginAttempts(email: String): Int {
        return try {
            val attemptsKey = getLoginAttemptsKey(email)
            redisTemplate.opsForValue().get(attemptsKey)?.toIntOrNull() ?: 0
        } catch (e: Exception) {
            log.error("Erro ao obter tentativas atuais", e)
            0
        }
    }

    private fun getRevokedTokenKey(token: String): String = REVOKED_TOKEN_PREFIX + token
    private fun getLoginAttemptsKey(email: String): String = LOGIN_ATTEMPTS_PREFIX + email
    private fun getLoginLockoutKey(email: String): String = LOGIN_LOCKOUT_PREFIX + email
    private fun getTokenFamilyKey(userId: Long): String = TOKEN_FAMILY_PREFIX + userId
    private fun getRateLimitKey(key: String): String = RATE_LIMIT_PREFIX + key

    private fun maskToken(token: String): String {
        return if (token.length >= 8) {
            token.take(4) + "*".repeat(token.length - 8) + token.takeLast(4)
        } else token
    }

    private fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return email

        val username = parts[0]
        val domain = parts[1]

        val maskedUsername = if (username.length > 2) {
            username.take(2) + "*".repeat(username.length - 2)
        } else username

        return "$maskedUsername@$domain"
    }
} 