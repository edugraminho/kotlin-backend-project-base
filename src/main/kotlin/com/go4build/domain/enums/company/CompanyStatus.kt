package com.base.domain.enums.company

/**
 * Status da empresa no sistema
 */
enum class CompanyStatus {
    ACTIVE,     // Empresa ativa
    INACTIVE,   // Empresa inativa
    SUSPENDED,  // Empresa suspensa (problemas de pagamento, etc.)
    BLOCKED     // Empresa bloqueada
}