package com.projectbasename.application.config

import com.projectbasename.domain.enums.payment.PaymentProvider
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuração geral para sistema de pagamentos
 */
@Configuration
@ConfigurationProperties(prefix = "payment")
data class PaymentConfig(
    var isProductionEnvironment: Boolean = false,
    var providerActivated: String = "REVENUECAT",
    var defaultCurrency: String = "BRL",
    var webhookRetries: Int = 3,
    var webhookTimeoutSeconds: Int = 30
) {
    
    /**
     * Retorna o provedor de pagamento ativo
     */
    fun getActiveProvider(): PaymentProvider {
        return PaymentProvider.fromValue(providerActivated) ?: PaymentProvider.REVENUECAT
    }
    
    /**
     * Verifica se está em ambiente de produção
     */
    fun isProduction(): Boolean = isProductionEnvironment
} 