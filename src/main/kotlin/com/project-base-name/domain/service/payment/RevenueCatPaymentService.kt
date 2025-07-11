package com.projectbasename.domain.service.payment

import com.projectbasename.application.dto.payment.revenuecat.RevenueCatEventType
import com.projectbasename.application.dto.payment.revenuecat.RevenueCatWebhookDto
import com.projectbasename.domain.enums.payment.EnvironmentType
import com.projectbasename.domain.enums.payment.PaymentProvider
import com.projectbasename.domain.enums.payment.SubscriptionStatus
import com.projectbasename.domain.exception.BusinessException
import com.projectbasename.domain.exception.ExceptionType
import com.projectbasename.domain.service.SubscriptionService
import com.projectbasename.domain.service.CompanyService
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Serviço responsável pelo processamento de webhooks do RevenueCat
 * Implementa a lógica de negócio específica para eventos do RevenueCat
 */
@Service
class RevenueCatPaymentService(
    private val subscriptionService: SubscriptionService,
    private val companyService: CompanyService,
    private val objectMapper: ObjectMapper
) : PaymentProviderService {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun getProvider(): PaymentProvider = PaymentProvider.REVENUECAT

    /**
     * Processa notificação de webhook do RevenueCat
     */
    @Transactional
    override fun processNotification(webhookPayload: String) {
        log.info("Iniciando processamento de webhook RevenueCat")
        log.debug("Payload recebido: $webhookPayload")

        try {
            val webhookDto = objectMapper.readValue(webhookPayload, RevenueCatWebhookDto::class.java)
            processRevenueCatEvent(webhookDto)
        } catch (e: Exception) {
            log.error("Erro ao processar webhook RevenueCat", e)
            throw BusinessException(ExceptionType.PAYMENT_PROCESSING_ERROR, "Falha ao processar webhook: ${e.message}")
        }
    }

    /**
     * Processa evento específico do RevenueCat
     */
    private fun processRevenueCatEvent(webhookDto: RevenueCatWebhookDto) {
        val event = webhookDto.event
        val eventType = RevenueCatEventType.fromValue(event.type)
        
        log.info("Processando evento RevenueCat: tipo=${event.type}, id=${event.id}")

        // Extrai atributos importantes
        val integrationCode = event.subscriberAttributes?.get("integrationCode")?.value
        val customerId = event.subscriberAttributes?.get("customerId")?.value

        if (integrationCode.isNullOrBlank()) {
            log.warn("Webhook sem integrationCode, ignorando evento: ${event.id}")
            return
        }

        // Busca ou cria assinatura conforme necessário
        val subscription = getOrCreateSubscription(integrationCode, event, customerId)
        
        // Processa baseado no tipo de evento
        when (eventType) {
            RevenueCatEventType.INITIAL_PURCHASE,
            RevenueCatEventType.RENEWAL,
            RevenueCatEventType.UNCANCELLATION,
            RevenueCatEventType.TEMPORARY_ENTITLEMENT_GRANT -> {
                processActivationEvent(subscription, event, customerId)
            }

            RevenueCatEventType.CANCELLATION,
            RevenueCatEventType.EXPIRATION -> {
                processCancellationEvent(subscription, event, customerId)
            }

            RevenueCatEventType.SUBSCRIPTION_PAUSED -> {
                processPauseEvent(subscription, event)
            }

            RevenueCatEventType.PRODUCT_CHANGE -> {
                processProductChangeEvent(subscription, event)
            }

            RevenueCatEventType.TEST -> {
                log.info("Evento de teste recebido, ignorando: ${event.id}")
                return // Não processa mais nada para eventos de teste
            }

            else -> {
                log.info("Tipo de evento não processado: $eventType para evento ${event.id}")
                return // Não processa mais nada para eventos não suportados
            }
        }

        // Atualiza informações gerais da assinatura
        updateSubscriptionFromEvent(subscription, event)
        
        log.info("Evento processado com sucesso: ${event.id} - Assinatura ID: ${subscription.id}")
    }

    /**
     * Busca assinatura existente ou cria nova se necessário
     */
    private fun getOrCreateSubscription(
        integrationCode: String,
        event: com.projectbasename.application.dto.payment.revenuecat.RevenueCatEventDto,
        customerId: String?
    ): com.projectbasename.domain.entity.SubscriptionEntity {
        
        // Primeiro tenta buscar assinatura existente
        val existingSubscription = subscriptionService.findByIntegrationCode(integrationCode)
        
        if (existingSubscription != null) {
            log.info("Assinatura existente encontrada: ID=${existingSubscription.id}, IntegrationCode=$integrationCode")
            return existingSubscription
        }
        
        // Se não existe, cria nova assinatura (primeira compra)
        log.info("Criando nova assinatura para primeira compra: IntegrationCode=$integrationCode")
        
        val userId = customerId?.toLongOrNull()
        if (userId == null) {
            log.error("CustomerId inválido ou ausente: $customerId, não é possível criar assinatura")
            throw BusinessException(ExceptionType.PAYMENT_PROCESSING_ERROR, "CustomerId inválido para criação de assinatura")
        }
        
        // Busca empresa do usuário
        val companyId = getCompanyIdForUser(userId)
        
        // Determina status inicial baseado no tipo de evento
        val initialStatus = when (RevenueCatEventType.fromValue(event.type)) {
            RevenueCatEventType.INITIAL_PURCHASE -> SubscriptionStatus.ACTIVE
            RevenueCatEventType.RENEWAL -> SubscriptionStatus.ACTIVE
            RevenueCatEventType.UNCANCELLATION -> SubscriptionStatus.ACTIVE
            else -> SubscriptionStatus.PENDING
        }
        
        // Cria nova assinatura
        val newSubscription = subscriptionService.createSubscription(
            userId = userId,
            companyId = companyId,
            provider = PaymentProvider.REVENUECAT,
            status = initialStatus,
            environmentType = EnvironmentType.fromValue(event.environment),
            integrationCode = integrationCode,
            productId = event.productId,
            price = event.price,
            currency = event.currency
        )
        
        log.info("Nova assinatura criada com sucesso: ID=${newSubscription.id}, User=$userId, Status=$initialStatus")
        
        return newSubscription
    }

    /**
     * Busca o ID da empresa do usuário
     */
    private fun getCompanyIdForUser(userId: Long): Long? {
        return try {
            // Busca a empresa onde o usuário é owner
            val companies = companyService.findCompaniesByUserId(userId)
            companies.firstOrNull()?.id
        } catch (e: Exception) {
            log.warn("Erro ao buscar empresa do usuário $userId, criando assinatura sem empresa", e)
            null
        }
    }

    /**
     * Processa eventos de ativação (compra, renovação, reativação)
     */
    private fun processActivationEvent(
        subscription: com.projectbasename.domain.entity.SubscriptionEntity,
        event: com.projectbasename.application.dto.payment.revenuecat.RevenueCatEventDto,
        customerId: String?
    ) {
        log.info("Processando evento de ativação para assinatura ${subscription.id}")

        // Se a assinatura estava cancelada, pode ter sido uma reativação
        if (subscription.status == SubscriptionStatus.CANCELED) {
            // Se há customerId diferente, pode ser transferência para novo usuário
            customerId?.let { newCustomerId ->
                if (newCustomerId != subscription.user.id.toString()) {
                    handleUserTransfer(subscription, newCustomerId.toLong())
                }
            }
        }

        // Ativa a assinatura
        subscription.activate()
        
        // Atualiza data de expiração se fornecida
        event.expirationAtMs?.let { expirationMs ->
            subscription.expirationDate = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(expirationMs),
                java.time.ZoneId.systemDefault()
            )
        }

        log.info("Assinatura ativada: ID=${subscription.id}, Status=${subscription.status}")
    }

    /**
     * Processa eventos de cancelamento ou expiração
     */
    private fun processCancellationEvent(
        subscription: com.projectbasename.domain.entity.SubscriptionEntity,
        event: com.projectbasename.application.dto.payment.revenuecat.RevenueCatEventDto,
        customerId: String?
    ) {
        log.info("Processando evento de cancelamento para assinatura ${subscription.id}")

        if (subscription.status == SubscriptionStatus.CANCELED) {
            log.info("Assinatura já cancelada, ignorando evento: ${subscription.id}")
            return
        }

        // Cancela a assinatura
        subscription.cancel()

        // Cria assinatura gratuita se necessário
        val userId = subscription.user.id!!
        val companyId = subscription.company?.id
        
        if (companyId != null) {
            try {
                subscriptionService.createFreeSubscription(userId, companyId)
                log.info("Assinatura gratuita criada para usuário $userId após cancelamento")
            } catch (e: Exception) {
                log.error("Erro ao criar assinatura gratuita após cancelamento", e)
            }
        }

        log.info("Assinatura cancelada: ID=${subscription.id}")
    }

    /**
     * Processa evento de pausa
     */
    private fun processPauseEvent(
        subscription: com.projectbasename.domain.entity.SubscriptionEntity,
        event: com.projectbasename.application.dto.payment.revenuecat.RevenueCatEventDto
    ) {
        log.info("Processando evento de pausa para assinatura ${subscription.id}")
        
        subscription.status = SubscriptionStatus.PAUSED
        subscription.updateTimestamp()
        
        log.info("Assinatura pausada: ID=${subscription.id}")
    }

    /**
     * Processa mudança de produto
     */
    private fun processProductChangeEvent(
        subscription: com.projectbasename.domain.entity.SubscriptionEntity,
        event: com.projectbasename.application.dto.payment.revenuecat.RevenueCatEventDto
    ) {
        log.info("Processando mudança de produto para assinatura ${subscription.id}")
        
        event.productId?.let { newProductId ->
            subscription.productId = newProductId
            log.info("Produto alterado para: $newProductId na assinatura ${subscription.id}")
        }
        
        event.price?.let { newPrice ->
            subscription.price = newPrice
            log.info("Preço alterado para: $newPrice na assinatura ${subscription.id}")
        }
    }

    /**
     * Atualiza informações gerais da assinatura a partir do evento
     */
    private fun updateSubscriptionFromEvent(
        subscription: com.projectbasename.domain.entity.SubscriptionEntity,
        event: com.projectbasename.application.dto.payment.revenuecat.RevenueCatEventDto
    ) {
        // Atualiza ambiente
        subscription.environmentType = EnvironmentType.fromValue(event.environment)
        
        // Atualiza original transaction ID se não existir
        if (subscription.originalTransactionId.isNullOrBlank()) {
            subscription.originalTransactionId = event.originalTransactionId
        }
        
        // Atualiza informações de preço e moeda
        event.price?.let { subscription.price = it }
        event.currency?.let { subscription.currency = it }
        event.periodType?.let { subscription.periodType = it }
        
        // Salva as alterações
        subscriptionService.updateSubscription(subscription)
    }

    /**
     * Lida com transferência de usuário
     */
    private fun handleUserTransfer(subscription: com.projectbasename.domain.entity.SubscriptionEntity, newUserId: Long) {
        log.info("Detectada transferência de usuário para assinatura ${subscription.id}: ${subscription.user.id} -> $newUserId")
        
        // Aqui você pode implementar a lógica de transferência se necessário
        // Por enquanto, apenas loga a ocorrência
        // Em um cenário real, pode ser necessário criar uma nova assinatura para o novo usuário
    }
} 