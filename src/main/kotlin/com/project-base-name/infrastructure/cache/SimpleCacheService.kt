package com.projectbasename.infrastructure.cache

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

/**
 * Serviço de cache Redis simplificado
 * Cache-aside + TTL + invalidação por padrão de chaves
 */
@Service
class SimpleCacheService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    companion object {
        // TTLs padrão
        val COMPANY_TTL = Duration.ofHours(2)
        val USER_TTL = Duration.ofMinutes(30)
        val LIST_TTL = Duration.ofMinutes(15)
    }

    /**
     * Cache-aside: busca no cache, se não encontrar executa loader e salva
     */
    fun <T> getOrSet(
        key: String,
        clazz: Class<T>,
        ttl: Duration = Duration.ofHours(1),
        loader: () -> T
    ): T {
        // Tenta buscar no cache
        val cached = get(key, clazz)
        if (cached != null) {
            log.debug("Cache HIT: $key")
            return cached
        }

        log.debug("Cache MISS: $key")

        // Executa loader e salva no cache
        val data = loader()
        set(key, data, ttl)
        return data
    }

    /**
     * Cache-aside com reified type (helper)
     */
    final inline fun <reified T> getOrSet(
        key: String,
        ttl: Duration = Duration.ofHours(1),
        noinline loader: () -> T
    ): T = getOrSet(key, T::class.java, ttl, loader)

    /**
     * Salva no cache com TTL
     */
    fun <T> set(key: String, data: T, ttl: Duration) {
        try {
            val json = objectMapper.writeValueAsString(data)
            redisTemplate.opsForValue().set(key, json, ttl)
            log.debug("Cache SET: $key (TTL: $ttl)")
        } catch (e: Exception) {
            log.error("Erro ao salvar no cache: $key", e)
        }
    }

    /**
     * Busca do cache
     */
    fun <T> get(key: String, clazz: Class<T>): T? {
        return try {
            val json = redisTemplate.opsForValue().get(key)
            json?.let { objectMapper.readValue(it, clazz) }
        } catch (e: Exception) {
            log.warn("Erro ao buscar do cache: $key", e)
            null
        }
    }

    /**
     * Busca do cache com reified type (helper)
     */
    final inline fun <reified T> get(key: String): T? = get(key, T::class.java)

    /**
     * Remove chave específica
     */
    fun evict(key: String) {
        try {
            redisTemplate.delete(key)
            log.debug("Cache EVICT: $key")
        } catch (e: Exception) {
            log.error("Erro ao remover do cache: $key", e)
        }
    }

    /**
     * Remove por padrão (ex: "company:*", "user:123:*")
     */
    fun evictPattern(pattern: String) {
        try {
            val keys = redisTemplate.keys(pattern)
            if (keys.isNotEmpty()) {
                redisTemplate.delete(keys)
                log.info("Cache EVICT PATTERN: $pattern (${keys.size} chaves)")
            }
        } catch (e: Exception) {
            log.error("Erro ao invalidar cache por padrão: $pattern", e)
        }
    }

    // Métodos auxiliares para chaves padronizadas
    fun companyKey(id: Long) = "company:$id"
    fun userKey(id: Long) = "user:$id"
    fun userCompaniesKey(userId: Long) = "user_companies:$userId"
} 