package com.projectbasename.domain.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Entidade para tokens de recuperação de senha
 */
@Entity
@Table(name = "password_reset_tokens")
class PasswordResetTokenEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "token", nullable = false, unique = true, length = 255)
    val token: String,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: LocalDateTime,

    @Column(name = "used", nullable = false)
    var used: Boolean = false,

    @Column(name = "used_at")
    var usedAt: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Verifica se o token expirou
     */
    fun isExpired(): Boolean = LocalDateTime.now().isAfter(expiresAt)

    /**
     * Verifica se o token é válido (não usado e não expirado)
     */
    fun isValid(): Boolean = !used && !isExpired()
} 