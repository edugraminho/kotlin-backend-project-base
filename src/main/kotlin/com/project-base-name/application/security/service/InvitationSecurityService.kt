package com.projectbasename.application.security.service

import com.projectbasename.domain.repository.InvitationRepository
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service

/**
 * Serviço de segurança para validação de permissões de convites
 */
@Service
class InvitationSecurityService(
    private val invitationRepository: InvitationRepository,
    private val companySecurityService: CompanySecurityService
) {

    /**
     * Verifica se o usuário autenticado pode cancelar um convite
     */
    fun canCancelInvitation(authentication: Authentication, invitationId: Long): Boolean {
        val invitation = invitationRepository.findById(invitationId).orElse(null)
            ?: return false
        
        // Verificar se o usuário tem permissão na empresa do convite
        return companySecurityService.isOwnerOrAdmin(authentication, invitation.companyId)
    }
} 