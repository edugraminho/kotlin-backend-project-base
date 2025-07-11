package com.projectbasename.domain.service

import com.projectbasename.application.dto.invitation.CreateInvitationRequest
import com.projectbasename.application.dto.invitation.InvitationResponse
import com.projectbasename.application.util.SecurityUtils
import com.projectbasename.domain.entity.InvitationEntity
import com.projectbasename.domain.enums.invitation.InvitationStatus
import com.projectbasename.domain.enums.invitation.InvitationType
import com.projectbasename.domain.enums.member.MemberType
import com.projectbasename.domain.enums.member.UserRole
import com.projectbasename.domain.exception.BusinessException
import com.projectbasename.domain.exception.ExceptionType
import com.projectbasename.domain.repository.CompanyMemberRepository
import com.projectbasename.domain.repository.InvitationRepository
import com.projectbasename.infrastructure.integration.email.EmailService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class InvitationService(
    private val invitationRepository: InvitationRepository,
    private val companyService: CompanyService,
    private val userService: UserService,
    private val companyMemberRepository: CompanyMemberRepository,
    private val companyMemberService: CompanyMemberService,
    private val emailService: EmailService,
    @Value("\${api.server.url:http://localhost:8080}") private val baseUrl: String
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    /**
     * Cria convite e envia email automaticamente
     */
    fun createInvitation(request: CreateInvitationRequest): InvitationResponse {
        // Obter ID do usuário autenticado
        val inviterId = SecurityUtils.getCurrentUserId()

        // Validar empresa
        val company = companyService.findCompanyEntityById(request.companyId)

        // Buscar dados do usuário que está convidando
        val inviter = userService.findUserEntityById(inviterId)

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
            inviterId = inviterId,
            invitationType = request.invitationType,
            token = generateToken(),
            expiresAt = LocalDateTime.now().plusDays(7) // 7 dias de validade
        )

        val savedInvitation = invitationRepository.save(invitation)

        // Enviar email de convite
        try {
            val emailResponse = emailService.sendInvitationEmail(
                recipientEmail = request.email,
                inviterName = inviter.name,
                companyName = company.name,
                inviteToken = savedInvitation.token,
                baseUrl = baseUrl
            )

            if (!emailResponse.success) {
                log.warn("Falha ao enviar email de convite: ${emailResponse.message}")
                // Não falhar a operação, apenas logar o erro
            } else {
                log.info("Email de convite enviado com sucesso para: ${maskEmail(request.email)}")
            }
        } catch (e: Exception) {
            log.error("Erro ao enviar email de convite", e)
            // Não falhar a operação, apenas logar o erro
        }

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

        // Verificar se o usuário autenticado é o mesmo do email do convite
        val currentUserId = SecurityUtils.getCurrentUserId()
        val currentUser = userService.findUserEntityById(currentUserId)

        if (currentUser.email != invitation.email) {
            throw BusinessException(ExceptionType.ACCESS_DENIED, "Este convite não é para você")
        }

        // Verificar se já não é membro da empresa
        val existingMember = companyMemberRepository.findByUserIdAndCompanyId(currentUserId, invitation.companyId)
        if (existingMember != null) {
            log.warn("Usuário ${currentUser.email} já é membro da empresa ${invitation.companyId}")
        } else {
            try {
                val inviter = userService.findUserEntityById(invitation.inviterId)
                val (memberType, userRole) = mapInvitationTypeToMemberTypeAndRole(invitation.invitationType)

                companyMemberService.addMember(
                    companyId = invitation.companyId,
                    userId = currentUserId,
                    memberType = memberType,
                    userRole = userRole,
                    invitedBy = inviter
                )

                log.info("Membro criado automaticamente para usuário ${maskEmail(currentUser.email)} na empresa ${invitation.companyId}")
            } catch (e: Exception) {
                log.error("Erro ao criar membro automaticamente para convite aceito", e)
            }
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

        // Verificar se o usuário autenticado é o mesmo do email do convite
        val currentUserId = SecurityUtils.getCurrentUserId()
        val currentUser = userService.findUserEntityById(currentUserId)

        if (currentUser.email != invitation.email) {
            throw BusinessException(ExceptionType.ACCESS_DENIED, "Este convite não é para você")
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

        // Verificar se o usuário autenticado tem permissão para cancelar
        val currentUserId = SecurityUtils.getCurrentUserId()
        val company = companyService.findCompanyEntityById(invitation.companyId)

        // Permitir se for o criador do convite, proprietário da empresa ou admin
        val canCancel = invitation.inviterId == currentUserId ||
                company.owner.id == currentUserId ||
                companyMemberRepository.findByUserIdAndCompanyId(currentUserId, invitation.companyId)?.let {
                    it.userRole in setOf(UserRole.OWNER, UserRole.ADMIN, UserRole.SUPER_USER) &&
                            it.status.name == "ACTIVE"
                } ?: false

        if (!canCancel) {
            throw BusinessException(ExceptionType.ACCESS_DENIED, "Sem permissão para cancelar este convite")
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
     * Reenvia convite por email
     */
    fun resendInvitation(id: Long): InvitationResponse {
        val invitation = invitationRepository.findById(id)
            .orElseThrow { BusinessException(ExceptionType.INVITATION_NOT_FOUND) }

        if (invitation.status != InvitationStatus.PENDING) {
            throw BusinessException(ExceptionType.INVITATION_INVALID_STATUS)
        }

        // Verificar se ainda está válido
        if (invitation.expiresAt.isBefore(LocalDateTime.now())) {
            invitation.status = InvitationStatus.EXPIRED
            invitationRepository.save(invitation)
            throw BusinessException(ExceptionType.INVITATION_EXPIRED)
        }

        // Buscar dados necessários
        val company = companyService.findCompanyEntityById(invitation.companyId)
        val inviter = userService.findUserEntityById(invitation.inviterId)

        // Reenviar email
        try {
            val emailResponse = emailService.sendInvitationEmail(
                recipientEmail = invitation.email,
                inviterName = inviter.name,
                companyName = company.name,
                inviteToken = invitation.token,
                baseUrl = baseUrl
            )

            if (!emailResponse.success) {
                log.warn("Falha ao reenviar email de convite: ${emailResponse.message}")
                throw BusinessException(ExceptionType.EMAIL_SEND_ERROR)
            }

            log.info("Email de convite reenviado com sucesso para: ${maskEmail(invitation.email)}")
        } catch (e: Exception) {
            log.error("Erro ao reenviar email de convite", e)
            throw BusinessException(ExceptionType.EMAIL_SEND_ERROR)
        }

        return mapToResponse(invitation)
    }

    /**
     * Mapeia tipo de convite para tipo de membro e role
     */
    private fun mapInvitationTypeToMemberTypeAndRole(invitationType: InvitationType): Pair<MemberType, UserRole> {
        return when (invitationType) {
            InvitationType.EMPLOYEE -> Pair(MemberType.INTERNAL, UserRole.EMPLOYEE)
            InvitationType.CLIENT -> Pair(MemberType.CLIENT, UserRole.CLIENT)
            InvitationType.SUPPLIER -> Pair(MemberType.SUPPLIER, UserRole.SUPPLIER)
        }
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

    /**
     * Mascara email para logs
     */
    private fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return email

        val username = parts[0]
        val domain = parts[1]

        val maskedUsername = when {
            username.length <= 2 -> username
            username.length <= 4 -> username.take(2) + "*".repeat(username.length - 2)
            else -> username.take(2) + "*".repeat(username.length - 4) + username.takeLast(2)
        }

        return "$maskedUsername@$domain"
    }
} 