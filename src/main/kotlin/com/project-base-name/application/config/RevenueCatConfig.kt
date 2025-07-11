package com.projectbasename.application.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuração do RevenueCat para processamento de pagamentos
 */
@Configuration
@ConfigurationProperties(prefix = "revenuecat")
data class RevenueCatConfig(
    var api: ApiConfig = ApiConfig()
) {
    
    data class ApiConfig(
        var signatureKey: String = "",
        var publicKey: String = "",
        var webhookUrl: String = "",
        var environment: String = "sandbox"
    )
    
    /**
     * Verifica se está em ambiente de produção
     */
    fun isProduction(): Boolean {
        return api.environment.equals("production", ignoreCase = true)
    }
    
    /**
     * Valida se as configurações estão completas
     */
    fun isValid(): Boolean {
        return api.signatureKey.isNotBlank()
    }
} 