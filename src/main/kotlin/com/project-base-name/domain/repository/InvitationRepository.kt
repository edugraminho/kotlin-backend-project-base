package com.projectbasename.domain.repository

import com.projectbasename.domain.entity.InvitationEntity
import com.projectbasename.domain.enums.invitation.InvitationStatus
import com.projectbasename.domain.enums.invitation.InvitationType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Repositório para operações com convites
 * Combina interface de domínio com implementação Spring Data JPA
 */
@Repository
interface InvitationRepository : JpaRepository<InvitationEntity, Long> {

    /**
     * Busca convite por token
     */
    fun findByToken(token: String): InvitationEntity?

    fun findByEmailAndStatus(email: String, status: InvitationStatus): List<InvitationEntity>

    fun findByCompanyIdAndStatus(companyId: Long, status: InvitationStatus): List<InvitationEntity>

    fun existsByEmailAndCompanyIdAndInvitationTypeAndStatus(
        email: String,
        companyId: Long,
        invitationType: InvitationType,
        status: InvitationStatus
    ): Boolean
}