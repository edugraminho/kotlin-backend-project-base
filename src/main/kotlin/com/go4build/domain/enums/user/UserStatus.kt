package com.base.domain.enums.user

/**
 * Status geral do usuário no sistema
 */
enum class UserStatus {
    ACTIVE,     // Usuário ativo
    INACTIVE,   // Usuário inativo
    BLOCKED,    // Usuário bloqueado
    PENDING,    // Aguardando confirmação
    SUPER_USER  // Super usuário (desenvolvedores, testadores, suporte)
}