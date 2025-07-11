package com.projectbasename.application.mapper

import com.projectbasename.application.dto.payment.SubscriptionListResponse
import com.projectbasename.application.dto.payment.SubscriptionResponse
import com.projectbasename.application.dto.payment.UserSubscriptionStatusResponse
import com.projectbasename.domain.entity.SubscriptionEntity

/**
 * Mapper para converter entidades de assinatura em DTOs de resposta
 */
object SubscriptionMapper {

    /**
     * Converte SubscriptionEntity para SubscriptionResponse completo
     */
    fun toResponse(entity: SubscriptionEntity): SubscriptionResponse {
        return SubscriptionResponse(
            id = entity.id!!,
            userId = entity.user.id!!,
            companyId = entity.company?.id,
            provider = entity.provider,
            status = entity.status,
            environmentType = entity.environmentType,
            productId = entity.productId,
            integrationCode = entity.integrationCode,
            price = entity.price,
            currency = entity.currency,
            periodType = entity.periodType,
            enabled = entity.enabled,
            isActive = entity.isActive(),
            isTrialing = entity.isTrialing(),
            isExpired = entity.isExpired(),
            lastApprovedDate = entity.lastApprovedDate,
            expirationDate = entity.expirationDate,
            cancellationDate = entity.cancellationDate,
            trialEndDate = entity.trialEndDate,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    /**
     * Converte SubscriptionEntity para SubscriptionListResponse resumido
     */
    fun toListResponse(entity: SubscriptionEntity): SubscriptionListResponse {
        return SubscriptionListResponse(
            id = entity.id!!,
            provider = entity.provider,
            status = entity.status,
            productId = entity.productId,
            price = entity.price,
            currency = entity.currency,
            enabled = entity.enabled,
            lastApprovedDate = entity.lastApprovedDate,
            expirationDate = entity.expirationDate,
            createdAt = entity.createdAt
        )
    }

    /**
     * Converte lista de entidades para lista de respostas
     */
    fun toListResponses(entities: List<SubscriptionEntity>): List<SubscriptionListResponse> {
        return entities.map { toListResponse(it) }
    }

    /**
     * Converte lista de entidades para lista de respostas completas
     */
    fun toResponses(entities: List<SubscriptionEntity>): List<SubscriptionResponse> {
        return entities.map { toResponse(it) }
    }

    /**
     * Cria resposta de status de assinatura do usuário
     */
    fun toUserSubscriptionStatus(
        hasActiveSubscription: Boolean,
        activeSubscriptionCount: Long,
        currentSubscription: SubscriptionEntity?,
        allSubscriptions: List<SubscriptionEntity>
    ): UserSubscriptionStatusResponse {
        return UserSubscriptionStatusResponse(
            hasActiveSubscription = hasActiveSubscription,
            activeSubscriptionCount = activeSubscriptionCount,
            currentSubscription = currentSubscription?.let { toResponse(it) },
            allSubscriptions = toListResponses(allSubscriptions)
        )
    }
} 