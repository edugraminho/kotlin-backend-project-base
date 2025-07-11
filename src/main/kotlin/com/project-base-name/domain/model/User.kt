package com.projectbasename.domain.model

import com.projectbasename.domain.enums.user.UserStatus
import com.projectbasename.domain.enums.user.UserType
import java.time.LocalDateTime

/**
 * Representa um usuário do sistema.
 * OWNER pode ter empresa própria, INVITED são apenas membros de empresas de terceiros.
 */
data class User(
    val id: Long? = null,
    val name: String,
    val email: String,
    val phone: String?,
    val password: String,
    val status: UserStatus = UserStatus.PENDING,
    val userType: UserType = UserType.OWNER,
    val profileImageUrl: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    /**
     * Verifica se o usuário está ativo
     */
    fun isActive(): Boolean = status == UserStatus.ACTIVE

    /**
     * Verifica se é um super usuário (desenvolvedor/testador)
     */
    fun isSuperUser(): Boolean = status == UserStatus.SUPER_USER

    /**
     * Verifica se o usuário pode criar empresa própria
     */
    fun canCreateCompany(): Boolean = userType == UserType.OWNER

    /**
     * Verifica se o usuário pode convidar outros usuários
     */
    fun canInviteUsers(): Boolean = userType == UserType.OWNER

    /**
     * Verifica se deve completar perfil obrigatoriamente
     */
    fun mustCompleteProfile(): Boolean =
        userType == UserType.OWNER &&
            status == UserStatus.ACTIVE

    /**
     * Verifica se é um usuário convidado (não cria empresa própria)
     */
    fun isInvitedUser(): Boolean = userType == UserType.INVITED
}
