package com.projectbasename.infrastructure.controller

import com.projectbasename.application.dto.user.*
import com.projectbasename.application.security.service.CompanySecurityService
import com.projectbasename.application.security.service.JwtService
import com.projectbasename.application.security.service.PermissionService
import com.projectbasename.domain.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

/**
 * Controller REST para operações com usuários
 */
@RestController
@RequestMapping("/v1/users")
@Tag(name = "Users", description = "Operações relacionadas a usuários")
class UserController(
    private val userService: UserService,
    private val jwtService: JwtService,
    private val companySecurityService: CompanySecurityService,
    private val permissionService: PermissionService
) {

    @Operation(
        summary = "Completar perfil do usuário",
        description = "Completa o perfil do usuário após ativação (obrigatório para UserType.OWNER)"
    )
    @PostMapping("/complete-profile")
    @PreAuthorize("isAuthenticated()")
    fun completeProfile(
        @Valid @RequestBody request: CompleteProfileRequest,
        @RequestHeader("Authorization") authHeader: String
    ): ResponseEntity<ProfileResponse?> {
        val token = authHeader.substring(7)
        val userId = jwtService.validateAccessToken(token)

        val response = userService.completeProfile(userId, request)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "Criar novo usuário",
        description = "Cria um novo usuário completo no sistema - apenas para administradores ou donos de empresa"
    )
    @PostMapping
    @PreAuthorize("@permissionService.hasAdminRole(authentication)")
    fun createUser(
        @Valid @RequestBody request: CreateUserRequest
    ): ResponseEntity<UserResponse> {
        val user = userService.createUser(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(user)
    }

    @Operation(
        summary = "Buscar usuário por ID",
        description = "Retorna os dados de um usuário específico"
    )
    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasAdminRole(authentication) or authentication.principal.toString() == #id.toString()")
    fun getUserById(
        @Parameter(description = "ID do usuário")
        @PathVariable id: Long
    ): ResponseEntity<UserResponse> {
        val user = userService.findById(id)
        return ResponseEntity.ok(user)
    }

    @Operation(
        summary = "Atualizar usuário",
        description = "Atualiza os dados de um usuário existente (com controle de empresa)"
    )
    @PutMapping("/{id}")
    @PreAuthorize("@companySecurityService.canUpdateUser(authentication, #id)")
    fun updateUser(
        @Parameter(description = "ID do usuário")
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateUserRequest
    ): ResponseEntity<UserResponse> {
        val user = userService.updateUser(id, request)
        return ResponseEntity.ok(user)
    }

    @Operation(
        summary = "Listar usuários",
        description = "Lista todos os usuários com paginação e filtro opcional por nome"
    )
    @GetMapping
    @PreAuthorize("@permissionService.hasAdminRole(authentication)")
    fun getUsers(
        @Parameter(description = "Filtro por nome (opcional)")
        @RequestParam(required = false) name: String?,
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<UserListResponse> {
        val users = if (!name.isNullOrBlank()) {
            userService.findByName(name, pageable)
        } else {
            userService.findAll(pageable)
        }

        val response = UserListResponse.from(users)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "Buscar usuário por email",
        description = "Retorna os dados de um usuário pelo email"
    )
    @GetMapping("/email/{email}")
    @PreAuthorize("@permissionService.hasAdminRole(authentication)")
    fun getUserByEmail(
        @Parameter(description = "Email do usuário")
        @PathVariable email: String
    ): ResponseEntity<UserResponse> {
        val user = userService.findByEmail(email)
        return ResponseEntity.ok(user)
    }

    @Operation(
        summary = "Obter perfil do usuário logado",
        description = "Retorna os dados do usuário autenticado com informações sobre necessidade de completar perfil"
    )
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    fun getCurrentUser(
        @RequestHeader("Authorization") authHeader: String
    ): ResponseEntity<UserProfileResponse> {
        val token = authHeader.substring(7) // Remove "Bearer "
        val userId = jwtService.validateAccessToken(token)

        val user = userService.findById(userId)
        val userEntity = userService.findUserEntityById(userId)

        val response = UserProfileResponse(
            user = user,
            needsProfile = userEntity.mustCompleteProfile(),
            canInviteUsers = userEntity.canInviteUsers(),
            userType = userEntity.userType
        )

        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "Verificar status do perfil",
        description = "Verifica se o usuário precisa completar o perfil"
    )
    @GetMapping("/profile-status")
    @PreAuthorize("isAuthenticated()")
    fun getProfileStatus(
        @RequestHeader("Authorization") authHeader: String
    ): ResponseEntity<ProfileStatusResponse> {
        val token = authHeader.substring(7) // Remove "Bearer "
        val userId = jwtService.validateAccessToken(token)

        val userEntity = userService.findUserEntityById(userId)
        val personalCompany = userService.findPersonalCompanyByUserId(userId)

        val response = ProfileStatusResponse(
            needsProfile = userEntity.mustCompleteProfile(),
            canCreateCompany = userEntity.canCreateCompany(),
            canInviteUsers = userEntity.canInviteUsers(),
            userType = userEntity.userType,
            hasCompany = personalCompany != null
        )

        return ResponseEntity.ok(response)
    }
}