package com.projectbasename.domain.service

import com.projectbasename.domain.entity.CompanyEntity
import com.projectbasename.domain.entity.SubscriptionEntity
import com.projectbasename.domain.entity.UserEntity
import com.projectbasename.domain.enums.payment.EnvironmentType
import com.projectbasename.domain.enums.payment.PaymentProvider
import com.projectbasename.domain.enums.payment.SubscriptionStatus
import com.projectbasename.domain.exception.BusinessException
import com.projectbasename.domain.exception.ExceptionType
import com.projectbasename.domain.repository.SubscriptionRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

/**
 * Serviço responsável pelo gerenciamento de assinaturas no sistema
 */
@Service
class SubscriptionService(
    private val subscriptionRepository: SubscriptionRepository,
    private val userService: UserService,
    private val companyService: CompanyService
) {
    
    private val log = LoggerFactory.getLogger(this::class.java)

    companion object {
        // Cache removido para evitar problemas de referências circulares
        // As entidades JPA contêm referências circulares que causam loops infinitos na serialização
    }

    /**
     * Busca assinatura por ID
     */
    fun findById(id: Long): SubscriptionEntity {
        return subscriptionRepository.findById(id)
            .orElseThrow { BusinessException(ExceptionType.SUBSCRIPTION_NOT_FOUND) }
    }

    /**
     * Busca assinatura por código de integração com lock pessimista
     */
    @Transactional
    fun findByIntegrationCodeWithLock(integrationCode: String): SubscriptionEntity {
        return subscriptionRepository.findByIntegrationCodeWithLock(integrationCode)
            ?: throw BusinessException(ExceptionType.SUBSCRIPTION_NOT_FOUND)
    }

    /**
     * Busca assinatura por código de integração
     */
    fun findByIntegrationCode(integrationCode: String): SubscriptionEntity? {
        return subscriptionRepository.findByIntegrationCode(integrationCode)
    }

    /**
     * Busca assinaturas ativas do usuário
     */
    fun findActiveSubscriptionsByUserId(userId: Long): List<SubscriptionEntity> {
        return subscriptionRepository.findActiveByUserId(userId)
    }

    /**
     * Busca assinatura ativa mais recente do usuário
     */
    fun findLatestActiveSubscription(userId: Long): SubscriptionEntity? {
        return subscriptionRepository.findLatestActiveByUserId(userId).firstOrNull()
    }

    /**
     * Verifica se usuário tem assinatura ativa
     */
    fun hasActiveSubscription(userId: Long): Boolean {
        return subscriptionRepository.hasActiveSubscription(userId)
    }

    /**
     * Cria nova assinatura
     */
    @Transactional
    fun createSubscription(
        userId: Long,
        companyId: Long? = null,
        provider: PaymentProvider,
        status: SubscriptionStatus,
        environmentType: EnvironmentType,
        integrationCode: String? = null,
        productId: String? = null,
        price: BigDecimal? = null,
        currency: String? = null
    ): SubscriptionEntity {
        
        val user = userService.findUserEntityById(userId)
        val company = companyId?.let { companyService.findCompanyEntityById(it) }

        // Verifica se já existe assinatura com o mesmo código de integração
        integrationCode?.let { code ->
            if (subscriptionRepository.findByIntegrationCode(code) != null) {
                throw BusinessException(ExceptionType.SUBSCRIPTION_ALREADY_EXISTS)
            }
        }

        val subscription = SubscriptionEntity(
            user = user,
            company = company,
            provider = provider,
            status = status,
            environmentType = environmentType,
            integrationCode = integrationCode,
            productId = productId,
            price = price,
            currency = currency
        )

        val savedSubscription = subscriptionRepository.save(subscription)
        
        // Cache removido para evitar problemas de referências circulares
        
        log.info("Nova assinatura criada: ID=${savedSubscription.id}, Usuário=$userId, Provider=$provider")
        
        return savedSubscription
    }

    /**
     * Cria assinatura gratuita
     */
    @Transactional
    fun createFreeSubscription(userId: Long, companyId: Long): SubscriptionEntity {
        // Desativa assinaturas ativas existentes
        val activeSubscriptions = findActiveSubscriptionsByUserId(userId)
        activeSubscriptions.forEach { subscription ->
            subscription.cancel()
            subscriptionRepository.save(subscription)
        }

        return createSubscription(
            userId = userId,
            companyId = companyId,
            provider = PaymentProvider.REVENUECAT, // ou um provider específico para gratuitos
            status = SubscriptionStatus.ACTIVE,
            environmentType = EnvironmentType.PRODUCTION,
            integrationCode = generateFreeSubscriptionCode(userId, companyId),
            productId = "free_plan",
            price = BigDecimal.ZERO,
            currency = "BRL"
        )
    }

    /**
     * Atualiza assinatura
     */
    @Transactional
    fun updateSubscription(subscription: SubscriptionEntity): SubscriptionEntity {
        subscription.updateTimestamp()
        val updatedSubscription = subscriptionRepository.save(subscription)
        
        // Cache removido para evitar problemas de referências circulares
        
        return updatedSubscription
    }

    /**
     * Ativa assinatura
     */
    @Transactional
    fun activateSubscription(subscriptionId: Long): SubscriptionEntity {
        val subscription = findById(subscriptionId)
        subscription.activate()
        return updateSubscription(subscription)
    }

    /**
     * Cancela assinatura
     */
    @Transactional
    fun cancelSubscription(subscriptionId: Long): SubscriptionEntity {
        val subscription = findById(subscriptionId)
        subscription.cancel()
        
        log.info("Assinatura cancelada: ID=$subscriptionId, Usuário=${subscription.user.id}")
        
        return updateSubscription(subscription)
    }

    /**
     * Reativa assinatura cancelada
     */
    @Transactional
    fun reactivateSubscription(subscriptionId: Long): SubscriptionEntity {
        val subscription = findById(subscriptionId)
        
        if (subscription.status != SubscriptionStatus.CANCELED) {
            throw BusinessException(ExceptionType.INVALID_SUBSCRIPTION_STATUS)
        }
        
        subscription.reactivate()
        
        log.info("Assinatura reativada: ID=$subscriptionId, Usuário=${subscription.user.id}")
        
        return updateSubscription(subscription)
    }

    /**
     * Processa expiração de assinaturas
     */
    @Transactional
    fun processExpiredSubscriptions() {
        val expiredSubscriptions = subscriptionRepository.findExpiredSubscriptions()
        
        expiredSubscriptions.forEach { subscription ->
            subscription.status = SubscriptionStatus.EXPIRED
            subscription.enabled = false
            subscription.updateTimestamp()
            subscriptionRepository.save(subscription)
            
            log.info("Assinatura expirada processada: ID=${subscription.id}, Usuário=${subscription.user.id}")
        }
        
        if (expiredSubscriptions.isNotEmpty()) {
            log.info("Processadas ${expiredSubscriptions.size} assinaturas expiradas")
        }
    }

    /**
     * Gera código de integração para assinatura gratuita
     */
    private fun generateFreeSubscriptionCode(userId: Long, companyId: Long): String {
        return "free_${userId}_${companyId}_${System.currentTimeMillis()}"
    }
} 