package com.projectbasename.application.dto.payment.revenuecat

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.Instant

/**
 * DTO principal para receber webhooks do RevenueCat
 * Baseado na documentação oficial do RevenueCat
 */
data class RevenueCatWebhookDto(
    @JsonProperty("api_version")
    val apiVersion: String,
    val event: RevenueCatEventDto
)

/**
 * DTO que representa um evento do RevenueCat
 */
data class RevenueCatEventDto(
    val id: String,
    val type: String,
    val environment: String,
    
    @JsonProperty("app_id")
    val appId: String? = null,
    
    @JsonProperty("app_user_id")
    val appUserId: String? = null,
    
    @JsonProperty("original_app_user_id")
    val originalAppUserId: String? = null,
    
    @JsonProperty("original_transaction_id")
    val originalTransactionId: String? = null,
    
    @JsonProperty("transaction_id")
    val transactionId: String? = null,
    
    @JsonProperty("product_id")
    val productId: String? = null,
    
    @JsonProperty("event_timestamp_ms")
    val eventTimestampMs: Long? = null,
    
    @JsonProperty("purchased_at_ms")
    val purchasedAtMs: Long? = null,
    
    @JsonProperty("expiration_at_ms")
    val expirationAtMs: Long? = null,
    
    val price: BigDecimal? = null,
    val currency: String? = null,
    
    @JsonProperty("country_code")
    val countryCode: String? = null,
    
    val store: String? = null,
    
    @JsonProperty("entitlement_ids")
    val entitlementIds: List<String>? = null,
    
    @JsonProperty("period_type")
    val periodType: String? = null,
    
    @JsonProperty("subscriber_attributes")
    val subscriberAttributes: Map<String, SubscriberAttributeDto>? = null,
    
    val aliases: List<String>? = null,
    
    @JsonProperty("is_family_share")
    val isFamilyShare: Boolean? = null,
    
    @JsonProperty("offer_code")
    val offerCode: String? = null,
    
    @JsonProperty("presented_offering_id")
    val presentedOfferingId: String? = null
) {
    /**
     * Converte timestamp para Instant
     */
    fun getEventTimestamp(): Instant? = eventTimestampMs?.let { Instant.ofEpochMilli(it) }
    
    /**
     * Converte timestamp de compra para Instant
     */
    fun getPurchasedAt(): Instant? = purchasedAtMs?.let { Instant.ofEpochMilli(it) }
    
    /**
     * Converte timestamp de expiração para Instant
     */
    fun getExpirationAt(): Instant? = expirationAtMs?.let { Instant.ofEpochMilli(it) }
}

/**
 * DTO para atributos de subscriber do RevenueCat
 */
data class SubscriberAttributeDto(
    val value: String,
    @JsonProperty("updated_at_ms")
    val updatedAtMs: Long
) {
    fun getUpdatedAt(): Instant = Instant.ofEpochMilli(updatedAtMs)
}

/**
 * Enum para tipos de eventos do RevenueCat
 */
enum class RevenueCatEventType {
    // Evento de teste através do dashboard
    TEST,
    
    // Nova assinatura comprada
    INITIAL_PURCHASE,
    
    // Renovação de assinatura existente
    RENEWAL,
    
    // Assinatura cancelada ou reembolsada
    CANCELLATION,
    
    // Assinatura cancelada foi reabilitada
    UNCANCELLATION,
    
    // Compra que não renova automaticamente
    NON_RENEWING_PURCHASE,
    
    // Assinatura pausada no final do período
    SUBSCRIPTION_PAUSED,
    
    // Assinatura expirou
    EXPIRATION,
    
    // Problema ao cobrar o assinante
    BILLING_ISSUE,
    
    // Mudança de produto da assinatura
    PRODUCT_CHANGE,
    
    // Transferência entre usuários
    TRANSFER,
    
    // Assinatura estendida
    SUBSCRIPTION_EXTENDED,
    
    // Concessão temporária de direito
    TEMPORARY_ENTITLEMENT_GRANT,
    
    // Nova fatura emitida (Web Billing)
    INVOICE_ISSUANCE,
    
    // Evento desconhecido
    UNKNOWN;

    companion object {
        fun fromValue(value: String?): RevenueCatEventType {
            return values().find { it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
        }
    }
} 