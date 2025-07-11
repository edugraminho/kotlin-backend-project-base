package com.projectbasename.domain.enums.payment

/**
 * Enum que define os tipos de ambiente para pagamentos
 * 
 * SANDBOX = Ambiente de desenvolvimento/teste (não são cobranças reais)
 * PRODUCTION = Ambiente de produção (cobranças reais)
 */
enum class EnvironmentType {
    SANDBOX,    // Desenvolvimento/Teste - sem cobrança real
    PRODUCTION; // Produção - cobrança real

    companion object {
        fun fromValue(value: String?): EnvironmentType {
            return when (value?.lowercase()) {
                "sandbox" -> SANDBOX
                "production" -> PRODUCTION
                else -> SANDBOX // Default para sandbox por segurança
            }
        }
    }
} 