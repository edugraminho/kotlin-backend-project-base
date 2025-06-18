package com.base.domain.model

import com.base.domain.enums.user.UserStatus
import java.time.LocalDateTime

/**
 * Representa um usuário do sistema.
 * Pode ter empresa própria ou ser apenas membro de empresas de terceiros.
 */
data class User(
    val id: Long? = null,
    val name: String,
    val email: String,
    val phone: String,
    val password: String,
    val ownedCompany: Company? = null,
    val profileImageUrl: String? = null,
    val status: UserStatus = UserStatus.PENDING,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Verifica se o usuário possui empresa própria
     */
    fun hasCompany(): Boolean = ownedCompany != null

    /**
     * Verifica se o usuário é o proprietário da empresa
     */
    fun isCompanyOwner(): Boolean = ownedCompany != null

    /**
     * Verifica se o usuário está ativo
     */
    fun isActive(): Boolean = status == UserStatus.ACTIVE

    /**
     * Verifica se é um super usuário (desenvolvedor/testador)
     */
    fun isSuperUser(): Boolean = status == UserStatus.SUPER_USER
}