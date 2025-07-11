package com.projectbasename.infrastructure.controller

import com.projectbasename.application.dto.auth.*
import com.projectbasename.application.security.service.AuthService
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
 * Gerencia login, registro e verificação SMS com suporte a UserType
 */
@RestController
@RequestMapping("/v1/auth")
@Tag(name = "Authentication", description = "Operações de autenticação e autorização")
class AuthController(
    private val authService: AuthService
) {

    @Operation(
        summary = "Registro básico de usuário",
        description = """
        Registra novo usuário com dados mínimos e envia SMS para ativação.
        - Sem invitationToken: UserType.OWNER (pode criar empresa própria)
        - Com invitationToken: UserType.INVITED (apenas membro de empresas)
        """
    )
    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<RegisterResponse> {
        val response = authService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @Operation(
        summary = "Ativar conta após registro",
        description = """
        Verifica código SMS e ativa conta criada. Response inclui flags:
        - needsProfile: true para UserType.OWNER (deve completar perfil)
        - hasInvitation: true para UserType.INVITED (pode aceitar convites)
        """
    )
    @PostMapping("/activate")
    fun activateAccount(@Valid @RequestBody request: VerifySmsRequest): ResponseEntity<AuthResponse> {
        val response = authService.verifySmsAndActivateAccount(request)
        return ResponseEntity.ok(response)
    }

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
        description = "Reenvia código SMS usando token temporário"
    )
    @PostMapping("/resend-code")
    fun resendCode(@Valid @RequestBody request: ResendCodeRequest): ResponseEntity<SmsVerificationResponse> {
        val response = authService.resendCode(request)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "Login via redes sociais",
        description = """
        Realiza login usando token de redes sociais (Google, Apple, Facebook).
        Valida o token diretamente com o provedor e cria/atualiza usuário automaticamente.
        Não requer verificação SMS adicional.
        """
    )
    @PostMapping("/social-login")
    fun socialLogin(@Valid @RequestBody request: SocialLoginRequest): ResponseEntity<AuthResponse> {
        val response = authService.socialLogin(request)
        return ResponseEntity.ok(response)
    }
}