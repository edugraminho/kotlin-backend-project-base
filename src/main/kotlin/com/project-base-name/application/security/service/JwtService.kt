package com.projectbasename.application.security.service

import com.projectbasename.domain.enums.member.UserRole
import com.projectbasename.domain.exception.BusinessException
import com.projectbasename.domain.exception.ExceptionType
import com.projectbasename.infrastructure.cache.TokenCacheService
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.Key
import java.util.*

/**
 * Serviço para geração e validação de tokens JWT
 */
@Service
class JwtService(
    private val tokenCacheService: TokenCacheService,
    @Value("\${jwt.secret}") private val secretKey: String,
    @Value("\${jwt.access-token-expiration-seconds}") private val accessTokenExpirationSeconds: Long,
    @Value("\${jwt.refresh-expiration}") private val refreshTokenExpirationSeconds: Long,
    @Value("\${jwt.temp-token-expiration-seconds}") private val tempTokenExpirationSeconds: Long
) {

    private val key: Key = Keys.hmacShaKeyFor(secretKey.toByteArray())

    /**
     * Gera access token com roles e empresa ativa
     */
    fun generateAccessToken(
        userId: Long,
        roles: Set<UserRole>,
        activeCompanyId: Long?
    ): String {
        // Verifica rate limit
        if (!tokenCacheService.checkRateLimit("access_token:$userId")) {
            throw BusinessException(ExceptionType.RATE_LIMIT_EXCEEDED)
        }

        val claims = HashMap<String, Any>()
        claims["roles"] = roles.map { it.name }
        activeCompanyId?.let { claims["activeCompanyId"] = it }
        claims["tokenType"] = "access"

        return generateToken(claims, userId, accessTokenExpirationSeconds)
    }

    /**
     * Gera refresh token
     */
    fun generateRefreshToken(userId: Long): String {
        // Verifica rate limit
        if (!tokenCacheService.checkRateLimit("refresh_token:$userId")) {
            throw BusinessException(ExceptionType.RATE_LIMIT_EXCEEDED)
        }

        val claims = HashMap<String, Any>()
        claims["tokenType"] = "refresh"
        return generateToken(claims, userId, refreshTokenExpirationSeconds)
    }

    /**
     * Gera token temporário para verificação
     */
    fun generateTempToken(userId: Long): String {
        // Verifica rate limit
        if (!tokenCacheService.checkRateLimit("temp_token:$userId")) {
            throw BusinessException(ExceptionType.RATE_LIMIT_EXCEEDED, "Limite de requisições excedido")
        }

        val claims = HashMap<String, Any>()
        claims["tokenType"] = "temp"
        return generateToken(claims, userId, tempTokenExpirationSeconds)
    }

    /**
     * Valida access token
     */
    fun validateAccessToken(token: String): Long {
        return validateToken(token, "access")
    }

    /**
     * Valida refresh token
     */
    fun validateRefreshToken(token: String): Long {
        return validateToken(token, "refresh")
    }

    /**
     * Valida token temporário
     */
    fun validateTempToken(token: String): Long {
        return validateToken(token, "temp")
    }

    /**
     * Extrai roles do token
     */
    fun extractRoles(token: String): Set<UserRole> {
        val claims = extractAllClaims(token)

        @Suppress("UNCHECKED_CAST")
        val roles = claims["roles"] as? List<String> ?: emptyList()
        return roles.mapNotNull { role -> runCatching { UserRole.valueOf(role) }.getOrNull() }.toSet()
    }

    /**
     * Extrai empresa ativa do token
     */
    fun extractActiveCompanyId(token: String): Long? {
        val claims = extractAllClaims(token)
        return claims["activeCompanyId"] as? Long
    }

    /**
     * Gera token JWT
     */
    private fun generateToken(
        extraClaims: Map<String, Any>,
        userId: Long,
        expirationSeconds: Long
    ): String {
        return Jwts.builder()
            .setClaims(extraClaims)
            .setSubject(userId.toString())
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + expirationSeconds * 1000))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    /**
     * Valida token JWT
     */
    private fun validateToken(token: String, expectedType: String): Long {
        try {
            // Check if token is revoked
            if (tokenCacheService.isTokenRevoked(token)) {
                throw BusinessException(ExceptionType.INVALID_TOKEN, "Token foi revogado")
            }

            val claims = extractAllClaims(token)

            // Verifica tipo do token
            val tokenType = claims["tokenType"] as? String
            if (tokenType != expectedType) {
                throw BusinessException(ExceptionType.INVALID_TOKEN, "Tipo de token inválido")
            }

            val userId = claims.subject.toLong()

            // Verifica se token pertence a uma família revogada
            if (tokenCacheService.isTokenFamilyRevoked(userId, token)) {
                throw BusinessException(ExceptionType.INVALID_TOKEN, "Token pertence a uma família revogada")
            }

            return userId
        } catch (e: Exception) {
            if (e is BusinessException) throw e
            throw BusinessException(ExceptionType.INVALID_TOKEN, "Token inválido ou expirado")
        }
    }

    /**
     * Extrai claims do token
     */
    private fun extractAllClaims(token: String): Claims {
        return try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .body
        } catch (e: Exception) {
            throw BusinessException(ExceptionType.INVALID_TOKEN, "Token inválido ou expirado")
        }
    }
}
