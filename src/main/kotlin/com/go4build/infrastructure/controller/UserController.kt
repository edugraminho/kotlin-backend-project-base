package com.base.infrastructure.controller

import com.base.application.dto.user.CreateUserRequest
import com.base.application.dto.user.UpdateUserRequest
import com.base.application.dto.user.UserListResponse
import com.base.application.dto.user.UserResponse
import com.base.domain.service.UserService
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
 * Controller REST para operações com usuários
 */
@RestController
@RequestMapping("/v1/users")
@Tag(name = "Users", description = "Operações relacionadas a usuários")
class UserController(
    private val userService: UserService
) {

    @Operation(
        summary = "Criar novo usuário",
        description = "Cria um novo usuário no sistema com empresa pessoal ou de negócios"
    )
    @PostMapping
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
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.id == #id")
    fun getUserById(
        @Parameter(description = "ID do usuário")
        @PathVariable id: Long
    ): ResponseEntity<UserResponse> {
        val user = userService.findById(id)
        return ResponseEntity.ok(user)
    }


    @Operation(
        summary = "Atualizar usuário",
        description = "Atualiza os dados de um usuário existente"
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.id == #id")
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
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
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
    @PreAuthorize("hasRole('ADMIN')")
    fun getUserByEmail(
        @Parameter(description = "Email do usuário")
        @PathVariable email: String
    ): ResponseEntity<UserResponse> {
        val user = userService.findByEmail(email)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(user)
    }


    @Operation(
        summary = "Obter perfil do usuário logado",
        description = "Retorna os dados do usuário autenticado"
    )
    @GetMapping("/me")
    fun getCurrentUser(
        // TODO: Implementar após JWT
        // @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<UserResponse> {
        // Por enquanto retorna um exemplo
        // val userId = (user as CustomUserDetails).id
        // val currentUser = userService.findById(userId)
        // return ResponseEntity.ok(currentUser)

        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()
    }
}