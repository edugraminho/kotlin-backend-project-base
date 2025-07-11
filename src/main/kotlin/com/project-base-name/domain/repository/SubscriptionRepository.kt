package com.projectbasename.domain.repository

import com.projectbasename.domain.entity.SubscriptionEntity
import com.projectbasename.domain.enums.payment.PaymentProvider
import com.projectbasename.domain.enums.payment.SubscriptionStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import jakarta.persistence.LockModeType
import java.util.*

/**
 * Repositório para operações relacionadas a assinaturas
 */
@Repository
interface SubscriptionRepository : JpaRepository<SubscriptionEntity, Long> {

    /**
     * Busca assinatura por código de integração com lock pessimista
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SubscriptionEntity s WHERE s.integrationCode = :integrationCode")
    fun findByIntegrationCodeWithLock(@Param("integrationCode") integrationCode: String): SubscriptionEntity?

    /**
     * Busca assinatura por código de integração
     */
    fun findByIntegrationCode(integrationCode: String): SubscriptionEntity?

    /**
     * Busca assinatura por ID da assinatura do provedor
     */
    fun findByProviderSubscriptionId(providerSubscriptionId: String): SubscriptionEntity?

    /**
     * Busca assinatura por transaction ID original
     */
    fun findByOriginalTransactionId(originalTransactionId: String): SubscriptionEntity?

    /**
     * Busca assinaturas ativas do usuário
     */
    @Query("SELECT s FROM SubscriptionEntity s WHERE s.user.id = :userId AND s.status = :status AND s.enabled = true")
    fun findActiveByUserId(@Param("userId") userId: Long, @Param("status") status: SubscriptionStatus = SubscriptionStatus.ACTIVE): List<SubscriptionEntity>

    /**
     * Busca assinaturas do usuário por provedor
     */
    @Query("SELECT s FROM SubscriptionEntity s WHERE s.user.id = :userId AND s.provider = :provider ORDER BY s.createdAt DESC")
    fun findByUserIdAndProvider(@Param("userId") userId: Long, @Param("provider") provider: PaymentProvider): List<SubscriptionEntity>

    /**
     * Busca assinaturas da empresa
     */
    @Query("SELECT s FROM SubscriptionEntity s WHERE s.company.id = :companyId ORDER BY s.createdAt DESC")
    fun findByCompanyId(@Param("companyId") companyId: Long): List<SubscriptionEntity>

    /**
     * Busca assinaturas ativas da empresa
     */
    @Query("SELECT s FROM SubscriptionEntity s WHERE s.company.id = :companyId AND s.status = :status AND s.enabled = true")
    fun findActiveByCompanyId(@Param("companyId") companyId: Long, @Param("status") status: SubscriptionStatus = SubscriptionStatus.ACTIVE): List<SubscriptionEntity>

    /**
     * Busca assinatura ativa mais recente do usuário
     */
    @Query("SELECT s FROM SubscriptionEntity s WHERE s.user.id = :userId AND s.status = :status AND s.enabled = true ORDER BY s.lastApprovedDate DESC, s.id DESC")
    fun findLatestActiveByUserId(@Param("userId") userId: Long, @Param("status") status: SubscriptionStatus = SubscriptionStatus.ACTIVE): List<SubscriptionEntity>

    /**
     * Busca assinaturas que expiraram
     */
    @Query("SELECT s FROM SubscriptionEntity s WHERE s.expirationDate < CURRENT_TIMESTAMP AND s.status != :status")
    fun findExpiredSubscriptions(@Param("status") status: SubscriptionStatus = SubscriptionStatus.EXPIRED): List<SubscriptionEntity>

    /**
     * Conta assinaturas ativas do usuário
     */
    @Query("SELECT COUNT(s) FROM SubscriptionEntity s WHERE s.user.id = :userId AND s.status = :status AND s.enabled = true")
    fun countActiveByUserId(@Param("userId") userId: Long, @Param("status") status: SubscriptionStatus = SubscriptionStatus.ACTIVE): Long

    /**
     * Verifica se usuário tem assinatura ativa
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM SubscriptionEntity s WHERE s.user.id = :userId AND s.status = :status AND s.enabled = true")
    fun hasActiveSubscription(@Param("userId") userId: Long, @Param("status") status: SubscriptionStatus = SubscriptionStatus.ACTIVE): Boolean
} 