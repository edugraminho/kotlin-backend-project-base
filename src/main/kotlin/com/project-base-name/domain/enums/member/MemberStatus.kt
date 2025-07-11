package com.projectbasename.domain.enums.member

/**
 * Status do membro na empresa
 */
enum class MemberStatus {
    ACTIVE,     // Membro ativo
    INACTIVE,   // Membro inativo
    PENDING,    // Aguardando aprovação
    BLOCKED     // Membro bloqueado
}