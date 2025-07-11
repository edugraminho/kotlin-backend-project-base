package com.projectbasename.application.dto.auth

import com.projectbasename.application.dto.company.CompanyResponse
import com.projectbasename.application.dto.user.UserResponse

/**
 * Response de autenticação completa
 */
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val user: UserResponse,
    val activeCompany: CompanyResponse? = null,
    // Flags para indicar próximos passos
    val needsProfile: Boolean = false,      // UserType.OWNER sem empresa
    val hasInvitation: Boolean = false      // UserType.INVITED com convite pendente
)

/**
 * Response de login (antes da verificação)
 * 1. Com telefone: tempToken para verificação SMS
 * 2. Sem telefone: authResponse com login direto
 */
data class LoginResponse(
    val requiresVerification: Boolean,
    val verificationType: VerificationType,
    val message: String,
    
    // Fluxo SMS (com telefone)
    val tempToken: String? = null,
    val expiresIn: Long? = null,
    
    // Fluxo direto (sem telefone)
    val authResponse: AuthResponse? = null
)

/**
 * Response de registro
 */
data class RegisterResponse(
    val success: Boolean,
    val message: String,
    val tempToken: String,
    val expiresIn: Long
)

/**
 * Response de verificação SMS
 */
data class SmsVerificationResponse(
    val success: Boolean,
    val message: String,
    val expiresIn: Long
)

/**
 * Resultado de verificação SMS
 */
data class SmsVerificationResult(
    val success: Boolean,
    val message: String
)

/**
 * Response de logout
 */
data class LogoutResponse(
    val success: Boolean,
    val message: String = "Logout realizado com sucesso"
)

/**
 * Tipo de verificação
 */
enum class VerificationType {
    SMS,
    EMAIL
}