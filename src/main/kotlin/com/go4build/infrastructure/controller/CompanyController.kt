package com.base.infrastructure.controller

import com.base.application.dto.company.CompanyListResponse
import com.base.application.dto.company.CompanyResponse
import com.base.application.dto.company.UpdateCompanyRequest
import com.base.domain.service.CompanyService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

/**
 * Controller REST para operações com empresas
 */
@RestController
@RequestMapping("/v1/companies")
@Tag(name = "Companies", description = "Operações relacionadas a empresas")
class CompanyController(
    private val companyService: CompanyService
) {

//    @Operation(
//        summary = "Criar empresa de negócios",
//        description = "Cria uma nova empresa do tipo BUSINESS para usuário existente"
//    )
//    @PostMapping
//    @PreAuthorize("hasRole('USER')")
//    fun createBusinessCompany(
//        @Valid @RequestBody request: CreateBusinessCompanyRequest
//    ): ResponseEntity<CompanyResponse> {
//        // Por enquanto usando ID fixo, implementar após JWT
//        val userId = 1L // Temporário
//
//        val company = companyService.createBusinessCompany(userId, request)
//        return ResponseEntity.status(HttpStatus.CREATED).body(company)
//    }

    @Operation(
        summary = "Buscar empresa por ID",
        description = "Retorna os dados de uma empresa específica"
    )
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @companySecurityService.canAccessCompany(authentication, #id)")
    fun getCompanyById(
        @Parameter(description = "ID da empresa")
        @PathVariable id: Long
    ): ResponseEntity<CompanyResponse> {
        val company = companyService.findById(id)
        return ResponseEntity.ok(company)
    }

    @Operation(
        summary = "Atualizar empresa",
        description = "Atualiza os dados de uma empresa existente"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Empresa atualizada com sucesso"),
            ApiResponse(responseCode = "404", description = "Empresa não encontrada")
        ]
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @companySecurityService.isOwnerOrAdmin(authentication, #id)")
    fun updateCompany(
        @Parameter(description = "ID da empresa")
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateCompanyRequest
    ): ResponseEntity<CompanyResponse> {
        val company = companyService.updateCompany(id, request)
        return ResponseEntity.ok(company)
    }

    @Operation(
        summary = "Listar empresas",
        description = "Lista empresas com paginação (apenas para admins)"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Lista de empresas retornada com sucesso")
        ]
    )
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_USER')")
    fun getCompanies(
        @Parameter(description = "Filtro por nome (opcional)")
        @RequestParam(required = false) name: String?,
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<CompanyListResponse> {
        val companies = if (!name.isNullOrBlank()) {
            companyService.findByName(name, pageable)
        } else {
            companyService.findAll(pageable)
        }

        val response = CompanyListResponse.from(companies)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "Listar minhas empresas",
        description = "Lista empresas onde o usuário logado é membro"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Lista de empresas do usuário")
        ]
    )
    @GetMapping("/my-companies")
    @PreAuthorize("hasRole('USER')")
    fun getMyCompanies(): ResponseEntity<List<CompanyResponse>> {
        // Por enquanto usando ID fixo, implementar após JWT
        val userId = 1L // Temporário

        val companies = companyService.findCompaniesByUserId(userId)
        return ResponseEntity.ok(companies)
    }

    @Operation(
        summary = "Obter empresa pessoal",
        description = "Retorna a empresa pessoal do usuário logado"
    )
    @GetMapping("/personal")
    @PreAuthorize("hasRole('USER')")
    fun getPersonalCompany(
        // TODO: Implementar após JWT
        // @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<CompanyResponse> {
        // Por enquanto retorna not implemented
        // val userId = (user as CustomUserDetails).id
        // val company = companyService.findPersonalCompanyByUserId(userId)
        // return ResponseEntity.ok(company)

        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()
    }
}