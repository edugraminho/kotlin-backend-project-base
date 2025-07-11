package com.projectbasename.application.dto.payment

import com.projectbasename.domain.enums.payment.EnvironmentType
import com.projectbasename.domain.enums.payment.PaymentProvider
import com.projectbasename.domain.enums.payment.SubscriptionStatus
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * DTO de resposta para dados de assinatura
 */
data class SubscriptionResponse(
    val id: Long,
    val userId: Long,
    val companyId: Long?,
    val provider: PaymentProvider,
    val status: SubscriptionStatus,
    val environmentType: EnvironmentType,
    val productId: String?,
    val integrationCode: String?,
    val price: BigDecimal?,
    val currency: String?,
    val periodType: String?,
    val enabled: Boolean,
    val isActive: Boolean,
    val isTrialing: Boolean,
    val isExpired: Boolean,
    val lastApprovedDate: LocalDateTime?,
    val expirationDate: LocalDateTime?,
    val cancellationDate: LocalDateTime?,
    val trialEndDate: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

/**
 * DTO simplificado para listar assinaturas
 */
data class SubscriptionListResponse(
    val id: Long,
    val provider: PaymentProvider,
    val status: SubscriptionStatus,
    val productId: String?,
    val price: BigDecimal?,
    val currency: String?,
    val enabled: Boolean,
    val lastApprovedDate: LocalDateTime?,
    val expirationDate: LocalDateTime?,
    val createdAt: LocalDateTime
)

/**
 * DTO para status de assinatura do usuário
 */
data class UserSubscriptionStatusResponse(
    val hasActiveSubscription: Boolean,
    val activeSubscriptionCount: Long,
    val currentSubscription: SubscriptionResponse?,
    val allSubscriptions: List<SubscriptionListResponse>
) 