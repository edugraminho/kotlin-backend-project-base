package com.base.domain.service

import com.base.application.dto.invitation.CreateInvitationRequest
import com.base.application.dto.invitation.InvitationResponse
import com.base.domain.entity.InvitationEntity
import com.base.domain.enums.invitation.InvitationStatus
import com.base.domain.exception.BusinessException
import com.base.domain.exception.ExceptionType
import com.base.domain.repository.InvitationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class InvitationService(
    private val invitationRepository: InvitationRepository,
    private val companyService: CompanyService,
    private val userService: UserService
) {

    /**
     * Cria convite
     */
    fun createInvitation(request: CreateInvitationRequest): InvitationResponse {
        // Validar empresa
        val company = companyService.findCompanyEntityById(request.companyId)

        // Validar convite duplicado
        if (invitationRepository.existsByEmailAndCompanyIdAndInvitationTypeAndStatus(
                request.email,
                request.companyId,
                request.invitationType,
                InvitationStatus.PENDING
            )
        ) {
            throw BusinessException(ExceptionType.INVITATION_ALREADY_EXISTS)
        }

        val invitation = InvitationEntity(
            email = request.email,
            companyId = request.companyId,
            inviterId = request.inviterId,
            invitationType = request.invitationType,
            token = generateToken(),
            expiresAt = LocalDateTime.now().plusDays(7) // 7 dias de validade
        )

        val savedInvitation = invitationRepository.save(invitation)
        return mapToResponse(savedInvitation)
    }

    /**
     * Aceita convite
     */
    fun acceptInvitation(token: String): InvitationResponse {
        val invitation = invitationRepository.findByToken(token)
            ?: throw BusinessException(ExceptionType.INVITATION_NOT_FOUND)

        if (invitation.status != InvitationStatus.PENDING) {
            throw BusinessException(ExceptionType.INVITATION_INVALID_STATUS)
        }

        if (invitation.expiresAt.isBefore(LocalDateTime.now())) {
            invitation.status = InvitationStatus.EXPIRED
            invitationRepository.save(invitation)
            throw BusinessException(ExceptionType.INVITATION_EXPIRED)
        }

        invitation.status = InvitationStatus.ACCEPTED
        val updatedInvitation = invitationRepository.save(invitation)

        return mapToResponse(updatedInvitation)
    }

    /**
     * Rejeita convite
     */
    fun rejectInvitation(token: String): InvitationResponse {
        val invitation = invitationRepository.findByToken(token)
            ?: throw BusinessException(ExceptionType.INVITATION_NOT_FOUND)

        if (invitation.status != InvitationStatus.PENDING) {
            throw BusinessException(ExceptionType.INVITATION_INVALID_STATUS)
        }

        invitation.status = InvitationStatus.REJECTED
        val updatedInvitation = invitationRepository.save(invitation)

        return mapToResponse(updatedInvitation)
    }

    /**
     * Cancela convite
     */
    fun cancelInvitation(id: Long): InvitationResponse {
        val invitation = invitationRepository.findById(id)
            .orElseThrow { BusinessException(ExceptionType.INVITATION_NOT_FOUND) }

        if (invitation.status != InvitationStatus.PENDING) {
            throw BusinessException(ExceptionType.INVITATION_INVALID_STATUS)
        }

        invitation.status = InvitationStatus.CANCELLED
        val updatedInvitation = invitationRepository.save(invitation)

        return mapToResponse(updatedInvitation)
    }

    /**
     * Busca convite por token
     */
    fun findByToken(token: String): InvitationResponse {
        val invitation = invitationRepository.findByToken(token)
            ?: throw BusinessException(ExceptionType.INVITATION_NOT_FOUND)
        return mapToResponse(invitation)
    }

    /**
     * Lista convites pendentes da empresa
     */
    fun listPendingCompanyInvitations(companyId: Long): List<InvitationResponse> {
        return invitationRepository.findByCompanyIdAndStatus(companyId, InvitationStatus.PENDING)
            .map { mapToResponse(it) }
    }

    /**
     * Lista convites pendentes do usuário
     */
    fun listPendingUserInvitations(email: String): List<InvitationResponse> {
        return invitationRepository.findByEmailAndStatus(email, InvitationStatus.PENDING)
            .map { mapToResponse(it) }
    }

    /**
     * Gera token único para convite
     */
    private fun generateToken(): String {
        return UUID.randomUUID().toString()
    }

    /**
     * Mapeia entidade para DTO de resposta
     */
    private fun mapToResponse(invitation: InvitationEntity): InvitationResponse {
        val company = companyService.findCompanyEntityById(invitation.companyId)
        val inviter = userService.findUserEntityById(invitation.inviterId)

        return InvitationResponse(
            id = invitation.id!!,
            email = invitation.email,
            companyId = company.id!!,
            companyName = company.name,
            inviterId = inviter.id!!,
            inviterName = inviter.name,
            invitationType = invitation.invitationType,
            status = invitation.status,
            token = invitation.token,
            expiresAt = invitation.expiresAt,
            createdAt = invitation.createdAt,
            updatedAt = invitation.updatedAt
        )
    }
} 