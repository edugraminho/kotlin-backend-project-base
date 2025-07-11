package com.projectbasename.infrastructure.controller

import com.projectbasename.application.dto.auth.PasswordResetRequest
import com.projectbasename.application.dto.auth.PasswordResetResponse
import com.projectbasename.application.dto.auth.ResetPasswordRequest
import com.projectbasename.domain.service.PasswordResetService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controller para operações de recuperação de senha
 */
@RestController
@RequestMapping("/v1/password-reset")
@Tag(name = "Password Reset", description = "APIs para recuperação de senha")
class PasswordResetController(
    private val passwordResetService: PasswordResetService
) {

    @Operation(
        summary = "Solicitar recuperação de senha",
        description = "Envia email com link para redefinir senha"
    )
    @PostMapping("/request")
    fun requestPasswordReset(
        @Valid @RequestBody request: PasswordResetRequest
    ): ResponseEntity<PasswordResetResponse> {
        val response = passwordResetService.requestPasswordReset(request)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "Validar token de recuperação",
        description = "Verifica se o token de recuperação é válido e não expirou"
    )
    @GetMapping("/validate/{token}")
    fun validateResetToken(
        @Parameter(description = "Token de recuperação")
        @PathVariable token: String
    ): ResponseEntity<Map<String, Boolean>> {
        val isValid = passwordResetService.validateResetToken(token)
        return ResponseEntity.ok(mapOf("valid" to isValid))
    }

    @Operation(
        summary = "Obter informações do token",
        description = "Retorna informações básicas do token para exibição na tela de reset"
    )
    @GetMapping("/info/{token}")
    fun getTokenInfo(
        @Parameter(description = "Token de recuperação")
        @PathVariable token: String
    ): ResponseEntity<Map<String, Any>> {
        val tokenInfo = passwordResetService.getTokenInfo(token)
        return ResponseEntity.ok(tokenInfo)
    }

    @Operation(
        summary = "Redefinir senha",
        description = "Redefine a senha do usuário usando token válido"
    )
    @PostMapping("/reset")
    fun resetPassword(
        @Valid @RequestBody request: ResetPasswordRequest
    ): ResponseEntity<PasswordResetResponse> {
        val response = passwordResetService.resetPassword(request)
        return ResponseEntity.ok(response)
    }
} 