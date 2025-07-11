package com.projectbasename.domain.repository

import com.projectbasename.domain.entity.PasswordResetTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Repositório para tokens de recuperação de senha
 */
@Repository
interface PasswordResetTokenRepository : JpaRepository<PasswordResetTokenEntity, Long> {

    /**
     * Busca token ativo (não usado) pelo valor
     */
    fun findByTokenAndUsedFalse(token: String): PasswordResetTokenEntity?

    /**
     * Busca tokens por usuário
     */
    fun findByUserId(userId: Long): List<PasswordResetTokenEntity>

    /**
     * Invalida todos os tokens de um usuário
     */
    @Modifying
    @Query("UPDATE PasswordResetTokenEntity p SET p.used = true, p.usedAt = :now WHERE p.userId = :userId AND p.used = false")
    fun invalidateAllByUserId(userId: Long, now: LocalDateTime = LocalDateTime.now())

    /**
     * Remove tokens expirados (limpeza automática)
     */
    @Modifying
    @Query("DELETE FROM PasswordResetTokenEntity p WHERE p.expiresAt < :now")
    fun deleteExpiredTokens(now: LocalDateTime = LocalDateTime.now())

    /**
     * Conta tokens ativos por usuário (para validação)
     */
    fun countByUserIdAndUsedFalse(userId: Long): Int
} 