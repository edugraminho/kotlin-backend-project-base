package com.base.infrastructure.controller

import com.base.application.dto.auth.*
import com.base.application.security.service.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Controller REST para operações de autenticação
 * Gerencia login, registro, verificação SMS
 */
@RestController
@RequestMapping("/v1/auth")
@Tag(name = "Authentication", description = "Operações de autenticação e autorização")
class AuthController(
    private val authService: AuthService
) {
    @Operation(
        summary = "Login com verificação SMS",
        description = "Realiza login e envia código SMS para verificação"
    )
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
        val response = authService.login(request)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "Verificar código SMS e completar login",
        description = "Verifica código SMS e retorna tokens de acesso"
    )
    @PostMapping("/verify-sms")
    fun verifySms(@Valid @RequestBody request: VerifySmsRequest): ResponseEntity<AuthResponse> {
        val response = authService.verifySmsAndCompleteLogin(request)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "Registro de novo usuário",
        description = "Cria nova conta e envia código SMS para ativação"
    )
    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<RegisterResponse> {
        val response = authService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @Operation(
        summary = "Ativar conta após registro",
        description = "Verifica código SMS e ativa a conta criada"
    )
    @PostMapping("/activate")
    fun activateAccount(@Valid @RequestBody request: VerifySmsRequest): ResponseEntity<AuthResponse> {
        val response = authService.verifySmsAndActivateAccount(request)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "Renovar access token",
        description = "Renova access token usando refresh token"
    )
    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<AuthResponse> {
        val response = authService.refresh(request)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "Logout",
        description = "Realiza logout e invalida tokens"
    )
    @PostMapping("/logout")
    fun logout(@Valid @RequestBody request: LogoutRequest): ResponseEntity<LogoutResponse> {
        authService.logout(request)
        return ResponseEntity.ok(LogoutResponse(true))
    }

    @Operation(
        summary = "Reenviar código de verificação",
        description = "Reenvia código SMS"
    )
    @PostMapping("/resend-code")
    fun resendCode(@Valid @RequestBody request: ResendCodeRequest): ResponseEntity<SmsVerificationResponse> {
        val response = authService.resendCode(request)
        return ResponseEntity.ok(response)
    }
}
