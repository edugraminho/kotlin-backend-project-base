package com.projectbasename.domain.model

import com.projectbasename.domain.enums.member.MemberStatus
import com.projectbasename.domain.enums.member.MemberType
import com.projectbasename.domain.enums.member.UserRole
import java.time.LocalDateTime

/**
 * Representa a vinculação de um usuário com uma empresa.
 * Define o papel e permissões do usuário dentro da empresa.
 */
data class CompanyMember(
    val id: Long? = null,
    val company: Company,
    val user: User,
    val memberType: MemberType,
    val userRole: UserRole,
    val status: MemberStatus = MemberStatus.PENDING,
    val invitedBy: User? = null,
    val joinedAt: LocalDateTime? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Verifica se o membro está ativo na empresa
     */
    fun isActive(): Boolean = status == MemberStatus.ACTIVE

    /**
     * Verifica se o membro está pendente na empresa
     */
    fun isPending(): Boolean = status == MemberStatus.PENDING

    /**
     * Verifica se o membro está bloqueado na empresa
     */
    fun isBlocked(): Boolean = status == MemberStatus.BLOCKED

    /**
     * Verifica se o membro está inativo na empresa
     */
    fun isInactive(): Boolean = status == MemberStatus.INACTIVE

    /**
     * Verifica se é membro interno (funcionário)
     */
    fun isInternal(): Boolean = memberType == MemberType.INTERNAL

    /**
     * Verifica se é cliente
     */
    fun isClient(): Boolean = memberType == MemberType.CLIENT

    /**
     * Verifica se é fornecedor
     */
    fun isSupplier(): Boolean = memberType == MemberType.SUPPLIER

    /**
     * Verifica se é parceiro
     */
    fun isPartner(): Boolean = memberType == MemberType.PARTNER

    /**
     * Verifica se é proprietário da empresa
     */
    fun isOwner(): Boolean = userRole == UserRole.OWNER

    /**
     * Verifica se é super usuário
     */
    fun isSuperUser(): Boolean = userRole == UserRole.SUPER_USER

    /**
     * Verifica se possui papel administrativo
     */
    fun hasAdminRole(): Boolean =
        userRole in setOf(UserRole.OWNER, UserRole.ADMIN, UserRole.SUPER_USER)

    /**
     * Verifica se pode acessar qualquer recurso (super user ou owner)
     */
    fun hasFullAccess(): Boolean = isSuperUser() || isOwner()
}