package com.projectbasename.domain.enums.payment

/**
 * Enum que define os status possíveis de uma assinatura
 */
enum class SubscriptionStatus {
    ACTIVE,
    CANCELED,
    EXPIRED,
    PAUSED,
    PENDING,
    TRIALING,
    REFUNDED,
    CHARGEBACK;

    companion object {
        fun fromValue(value: String?): SubscriptionStatus? {
            return values().find { it.name.equals(value, ignoreCase = true) }
        }
    }
} 