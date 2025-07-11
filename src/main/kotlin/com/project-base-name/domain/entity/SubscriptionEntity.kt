package com.projectbasename.domain.entity

import com.projectbasename.domain.enums.payment.EnvironmentType
import com.projectbasename.domain.enums.payment.PaymentProvider
import com.projectbasename.domain.enums.payment.SubscriptionStatus
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Entidade que representa uma assinatura no sistema
 * Corresponde à tabela 'subscriptions'
 */
@Entity
@Table(name = "subscriptions")
data class SubscriptionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = true)
    val company: CompanyEntity? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    val provider: PaymentProvider,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: SubscriptionStatus,

    @Enumerated(EnumType.STRING)
    @Column(name = "environment_type", nullable = false)
    var environmentType: EnvironmentType,

    @Column(name = "provider_subscription_id", unique = true)
    var providerSubscriptionId: String? = null,

    @Column(name = "original_transaction_id")
    var originalTransactionId: String? = null,

    @Column(name = "product_id")
    var productId: String? = null,

    @Column(name = "integration_code", unique = true)
    var integrationCode: String? = null,

    @Column(name = "price", precision = 10, scale = 2)
    var price: BigDecimal? = null,

    @Column(name = "currency", length = 3)
    var currency: String? = null,

    @Column(name = "period_type", length = 50)
    var periodType: String? = null,

    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = true,

    @Column(name = "last_approved_date")
    var lastApprovedDate: LocalDateTime? = null,

    @Column(name = "expiration_date")
    var expirationDate: LocalDateTime? = null,

    @Column(name = "cancellation_date")
    var cancellationDate: LocalDateTime? = null,

    @Column(name = "trial_end_date")
    var trialEndDate: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    
    /**
     * Atualiza o timestamp de modificação
     */
    fun updateTimestamp() {
        updatedAt = LocalDateTime.now()
    }

    /**
     * Verifica se a assinatura está ativa
     */
    fun isActive(): Boolean {
        return status == SubscriptionStatus.ACTIVE && enabled
    }

    /**
     * Verifica se a assinatura está em período de teste
     */
    fun isTrialing(): Boolean {
        return status == SubscriptionStatus.TRIALING && 
               trialEndDate?.isAfter(LocalDateTime.now()) == true
    }

    /**
     * Verifica se a assinatura expirou
     */
    fun isExpired(): Boolean {
        return expirationDate?.isBefore(LocalDateTime.now()) == true
    }

    /**
     * Ativa a assinatura
     */
    fun activate() {
        status = SubscriptionStatus.ACTIVE
        enabled = true
        lastApprovedDate = LocalDateTime.now()
        cancellationDate = null
        updateTimestamp()
    }

    /**
     * Cancela a assinatura
     */
    fun cancel() {
        status = SubscriptionStatus.CANCELED
        enabled = false
        cancellationDate = LocalDateTime.now()
        updateTimestamp()
    }

    /**
     * Reativa assinatura cancelada
     */
    fun reactivate() {
        if (status == SubscriptionStatus.CANCELED) {
            activate()
        }
    }
} 