package com.projectbasename.domain.model

import com.projectbasename.domain.enums.invitation.InvitationStatus
import com.projectbasename.domain.enums.member.MemberType
import com.projectbasename.domain.enums.member.Permission
import com.projectbasename.domain.enums.member.UserRole
import java.time.LocalDateTime
import java.util.*

/**
 * Representa um convite para um usuário participar de uma empresa.
 * Usado para onboarding simplificado de clientes, fornecedores, etc.
 */
data class Invitation(
    val id: Long? = null,
    val email: String,
    val companyId: Long,
    val invitedBy: Long, // Usuário que fez o convite
    val memberType: MemberType,
    val userRole: UserRole,
    val specificPermissions: Set<Permission> = emptySet(),
    val token: String = UUID.randomUUID().toString(),
    val expiresAt: LocalDateTime = LocalDateTime.now().plusDays(7), // 7 dias para aceitar
    val status: InvitationStatus = InvitationStatus.PENDING,
    val acceptedAt: LocalDateTime? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Verifica se o convite ainda está válido (não expirado e pendente)
     */
    fun isValid(): Boolean = status == InvitationStatus.PENDING && !isExpired()

    /**
     * Verifica se o convite expirou
     */
    fun isExpired(): Boolean = LocalDateTime.now().isAfter(expiresAt)

    /**
     * Verifica se o convite foi aceito
     */
    fun isAccepted(): Boolean = status == InvitationStatus.ACCEPTED

    /**
     * Marca o convite como aceito
     */
    fun accept(): Invitation = copy(
        status = InvitationStatus.ACCEPTED,
        acceptedAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    /**
     * Cancela o convite
     */
    fun cancel(): Invitation = copy(
        status = InvitationStatus.CANCELLED,
        updatedAt = LocalDateTime.now()
    )
}
