package com.base.application.dto.auth

import com.base.application.dto.company.CompanyResponse
import com.base.application.dto.user.UserResponse

/**
 * Response de autenticação completa
 */
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val user: UserResponse,
    val activeCompany: CompanyResponse? = null
)

/**
 * Response de login (antes da verificação)
 */
data class LoginResponse(
    val requiresVerification: Boolean,
    val verificationType: VerificationType,
    val message: String,
    val tempToken: String? = null,
    val expiresIn: Long? = null
)

/**
 * Response de registro
 */
data class RegisterResponse(
    val success: Boolean,
    val message: String,
    val tempToken: String? = null,
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

