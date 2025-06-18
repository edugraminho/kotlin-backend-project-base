package com.base.infrastructure.controller

import com.base.application.dto.company.member.CompanyMemberResponse
import com.base.domain.enums.member.MemberType
import com.base.domain.enums.member.UserRole
import com.base.domain.service.CompanyMemberService
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
 * Controller REST para operações com membros de empresas
 */
@RestController
@RequestMapping("/v1/companies/{companyId}/members")
@Tag(name = "Company Members", description = "Operações relacionadas a membros de empresas")
class CompanyMemberController(
    private val companyMemberService: CompanyMemberService
) {

    @Operation(
        summary = "Listar membros da empresa",
        description = "Lista todos os membros de uma empresa"
    )
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or @companySecurityService.canAccessCompany(authentication, #companyId)")
    fun listCompanyMembers(
        @Parameter(description = "ID da empresa")
        @PathVariable companyId: Long
    ): ResponseEntity<List<CompanyMemberResponse>> {
        val members = companyMemberService.listCompanyMembers(companyId)
        return ResponseEntity.ok(members)
    }

    @Operation(
        summary = "Buscar membro",
        description = "Retorna os dados de um membro específico"
    )
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @companySecurityService.canAccessCompany(authentication, #companyId)")
    fun getMember(
        @Parameter(description = "ID da empresa")
        @PathVariable companyId: Long,
        @Parameter(description = "ID do membro")
        @PathVariable id: Long
    ): ResponseEntity<CompanyMemberResponse> {
        val member = companyMemberService.findById(id)
        return ResponseEntity.ok(member)
    }

    @Operation(
        summary = "Adicionar membro",
        description = "Adiciona um novo membro à empresa"
    )
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @companySecurityService.isOwnerOrAdmin(authentication, #companyId)")
    fun addMember(
        @Parameter(description = "ID da empresa")
        @PathVariable companyId: Long,
        @Parameter(description = "ID do usuário")
        @RequestParam userId: Long,
        @Parameter(description = "Tipo de membro")
        @RequestParam memberType: MemberType,
        @Parameter(description = "Papel do usuário")
        @RequestParam userRole: UserRole
    ): ResponseEntity<CompanyMemberResponse> {
        val member = companyMemberService.addMember(companyId, userId, memberType, userRole)
        return ResponseEntity.status(HttpStatus.CREATED).body(member)
    }

    @Operation(
        summary = "Atualizar papel do membro",
        description = "Atualiza o papel de um membro na empresa"
    )
    @PutMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN') or @companySecurityService.isOwnerOrAdmin(authentication, #companyId)")
    fun updateMemberRole(
        @Parameter(description = "ID da empresa")
        @PathVariable companyId: Long,
        @Parameter(description = "ID do usuário")
        @PathVariable userId: Long,
        @Parameter(description = "Novo papel")
        @RequestParam newRole: UserRole
    ): ResponseEntity<CompanyMemberResponse> {
        val member = companyMemberService.updateMemberRole(companyId, userId, newRole)
        return ResponseEntity.ok(member)
    }

    @Operation(
        summary = "Remover membro",
        description = "Remove um membro da empresa"
    )
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @companySecurityService.isOwnerOrAdmin(authentication, #companyId)")
    fun removeMember(
        @Parameter(description = "ID da empresa")
        @PathVariable companyId: Long,
        @Parameter(description = "ID do usuário")
        @PathVariable userId: Long
    ): ResponseEntity<Unit> {
        companyMemberService.removeMember(companyId, userId)
        return ResponseEntity.noContent().build()
    }
} 