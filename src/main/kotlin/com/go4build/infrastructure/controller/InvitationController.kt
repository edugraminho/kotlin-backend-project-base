package com.base.infrastructure.controller

import com.base.application.dto.invitation.CreateInvitationRequest
import com.base.application.dto.invitation.InvitationResponse
import com.base.domain.service.InvitationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
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
    private val invitationService: InvitationService
) {

    @Operation(
        summary = "Criar convite",
        description = "Cria um novo convite para uma empresa"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Convite criado com sucesso"),
            ApiResponse(responseCode = "400", description = "Dados inválidos"),
            ApiResponse(responseCode = "409", description = "Convite já existe")
        ]
    )
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @companySecurityService.isOwnerOrAdmin(authentication, #request.companyId)")
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
    @PreAuthorize("hasRole('ADMIN') or @companySecurityService.isOwnerOrAdmin(authentication, #id)")
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
    @PreAuthorize("hasRole('ADMIN') or @companySecurityService.isOwnerOrAdmin(authentication, #companyId)")
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
    fun listPendingUserInvitations(
        @Parameter(description = "Email do usuário")
        @PathVariable email: String
    ): ResponseEntity<List<InvitationResponse>> {
        val invitations = invitationService.listPendingUserInvitations(email)
        return ResponseEntity.ok(invitations)
    }
} 