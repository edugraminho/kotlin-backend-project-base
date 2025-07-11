package com.projectbasename.infrastructure.controller

import com.projectbasename.application.dto.invitation.CreateInvitationRequest
import com.projectbasename.application.dto.invitation.InvitationResponse
import com.projectbasename.application.security.service.PermissionService
import com.projectbasename.domain.service.InvitationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

/**
 * Controller REST para operações com convites
 */
@RestController
@RequestMapping("/v1/invitations")
@Tag(name = "Invitations", description = "Operações relacionadas a convites")
class InvitationController(
    private val invitationService: InvitationService,
    private val permissionService: PermissionService
) {

    @Operation(
        summary = "Criar convite",
        description = "Cria um novo convite para uma empresa: EMPLOYEE, CLIENT ou SUPPLIER"
    )
    @PostMapping
    @PreAuthorize("@permissionService.canManageInvitations(authentication, #request.companyId)")
    fun createInvitation(
        @Valid @RequestBody request: CreateInvitationRequest
    ): ResponseEntity<InvitationResponse> {
        val invitation = invitationService.createInvitation(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(invitation)
    }

    @Operation(
        summary = "Aceitar convite",
        description = "Aceita um convite usando o token"
    )
    @PostMapping("/{token}/accept")
    fun acceptInvitation(
        @Parameter(description = "Token do convite")
        @PathVariable token: String
    ): ResponseEntity<InvitationResponse> {
        val invitation = invitationService.acceptInvitation(token)
        return ResponseEntity.ok(invitation)
    }

    @Operation(
        summary = "Rejeitar convite",
        description = "Rejeita um convite usando o token"
    )
    @PostMapping("/{token}/reject")
    fun rejectInvitation(
        @Parameter(description = "Token do convite")
        @PathVariable token: String
    ): ResponseEntity<InvitationResponse> {
        val invitation = invitationService.rejectInvitation(token)
        return ResponseEntity.ok(invitation)
    }

    @Operation(
        summary = "Cancelar convite",
        description = "Cancela um convite pendente"
    )
    @PostMapping("/{id}/cancel")
    @PreAuthorize("@invitationSecurityService.canCancelInvitation(authentication, #id)")
    fun cancelInvitation(
        @Parameter(description = "ID do convite")
        @PathVariable id: Long
    ): ResponseEntity<InvitationResponse> {
        val invitation = invitationService.cancelInvitation(id)
        return ResponseEntity.ok(invitation)
    }

    @Operation(
        summary = "Buscar convite por token",
        description = "Retorna os dados de um convite pelo token"
    )
    @GetMapping("/token/{token}")
    fun getInvitationByToken(
        @Parameter(description = "Token do convite")
        @PathVariable token: String
    ): ResponseEntity<InvitationResponse> {
        val invitation = invitationService.findByToken(token)
        return ResponseEntity.ok(invitation)
    }

    @Operation(
        summary = "Listar convites pendentes da empresa",
        description = "Lista todos os convites pendentes de uma empresa"
    )
    @GetMapping("/company/{companyId}/pending")
    @PreAuthorize("@permissionService.canManageInvitations(authentication, #companyId)")
    fun listPendingCompanyInvitations(
        @Parameter(description = "ID da empresa")
        @PathVariable companyId: Long
    ): ResponseEntity<List<InvitationResponse>> {
        val invitations = invitationService.listPendingCompanyInvitations(companyId)
        return ResponseEntity.ok(invitations)
    }

    @Operation(
        summary = "Listar convites pendentes do usuário",
        description = "Lista todos os convites pendentes de um usuário"
    )
    @GetMapping("/user/{email}/pending")
    @PreAuthorize("@permissionService.hasAdminRole(authentication) or authentication.principal.toString() == @userService.findUserIdByEmail(#email).toString()")
    fun listPendingUserInvitations(
        @Parameter(description = "Email do usuário")
        @PathVariable email: String
    ): ResponseEntity<List<InvitationResponse>> {
        val invitations = invitationService.listPendingUserInvitations(email)
        return ResponseEntity.ok(invitations)
    }

    @Operation(
        summary = "Reenviar convite por email",
        description = "Reenvia email de convite para o destinatário"
    )
    @PostMapping("/{id}/resend")
    @PreAuthorize("@invitationSecurityService.canCancelInvitation(authentication, #id)")
    fun resendInvitation(
        @Parameter(description = "ID do convite")
        @PathVariable id: Long
    ): ResponseEntity<InvitationResponse> {
        val invitation = invitationService.resendInvitation(id)
        return ResponseEntity.ok(invitation)
    }
} 