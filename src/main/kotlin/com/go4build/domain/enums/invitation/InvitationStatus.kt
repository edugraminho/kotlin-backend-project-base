package com.base.domain.enums.invitation

/**
 * Status do convite
 */
enum class InvitationStatus {
    PENDING,    // Aguardando aceite
    ACCEPTED,   // Aceito
    REJECTED,   // Rejeitado
    EXPIRED,    // Expirado
    CANCELLED   // Cancelado
}