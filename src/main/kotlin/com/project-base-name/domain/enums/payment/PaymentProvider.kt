package com.projectbasename.domain.enums.payment

/**
 * Enum que define os provedores de pagamento disponíveis no sistema
 */
enum class PaymentProvider {
    REVENUECAT;

    companion object {
        fun fromValue(value: String?): PaymentProvider? {
            return values().find { it.name.equals(value, ignoreCase = true) }
        }
    }
} 