package com.base.application.security.service

import com.base.application.dto.auth.*
import com.base.application.dto.user.UserResponse
import com.base.application.mapper.UserMapper
import com.base.application.security.verification.SmsCodeService
import com.base.domain.enums.member.UserRole
import com.base.domain.enums.user.UserStatus
import com.base.domain.exception.BusinessException
import com.base.domain.exception.ExceptionType
import com.base.domain.model.User
import com.base.domain.repository.CompanyMemberRepository
import com.base.domain.service.CompanyService
import com.base.domain.service.UserService
import com.base.infrastructure.cache.TokenCacheService
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.regex.Pattern

/**
 * Serviço principal de autenticação
 * Gerencia login, registro e verificação SMS/Email
 */
@Service
@Transactional
class AuthService(
    private val userService: UserService,
    private val companyService: CompanyService,
    private val jwtService: JwtService,
    private val smsCodeService: SmsCodeService,
    private val passwordEncoder: PasswordEncoder,
    private val companyMemberRepository: CompanyMemberRepository,
    private val tokenCacheService: TokenCacheService,
    @Value("\${jwt.access-token-expiration-seconds:86400}") private val tokenExpirationSeconds: Long,
    @Value("\${verification.sms.max-attempts:3}") private val maxSmsAttempts: Int
) {
    // Padrões de validação
    private val emailPattern = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)")
    private val phonePattern = Pattern.compile("^\\+?[1-9]\\d{1,14}")

    /**
     * Login com SMS - Primeira etapa
     * Valida credenciais e envia código SMS
     */
    fun login(request: LoginRequest): LoginResponse {
        validateLoginAttempts(request.email)
        val user = userService.findUserEntityByEmail(request.email)
            ?: throw BusinessException(ExceptionType.INVALID_CREDENTIALS)

        if (user.status == UserStatus.INACTIVE) throw BusinessException(ExceptionType.USER_INACTIVE)

        if (!passwordEncoder.matches(request.password, user.password)) {
            tokenCacheService.incrementLoginAttempts(request.email)
            throw BusinessException(ExceptionType.INVALID_CREDENTIALS)
        }

        val smsVerification = smsCodeService.generateAndSendCode(user.phone)
        tokenCacheService.resetLoginAttempts(request.email)

        val tempToken = jwtService.generateTempToken(user.id!!)

        return LoginResponse(
            requiresVerification = true,
            verificationType = VerificationType.SMS,
            message = "Código SMS enviado",
            tempToken = tempToken,
            expiresIn = smsVerification.expiresIn
        )
    }

    /**
     * Verifica código SMS e ativa conta
     */
    fun verifySmsAndActivateAccount(request: VerifySmsRequest): AuthResponse {
        val userId = jwtService.validateTempToken(request.tempToken)
        val user = userService.findUserEntityById(userId)

        // Verifica se usuário está pendente
        if (user.status != UserStatus.PENDING) {
            throw BusinessException(ExceptionType.INVALID_USER_STATUS, "Usuário já está ativo")
        }

        // Verifica código SMS
        val verificationResult = smsCodeService.verifyCode(user.phone, request.code)
        if (!verificationResult.success) {
            throw BusinessException(ExceptionType.INVALID_VERIFICATION_CODE)
        }

        // Ativa usuário
        userService.activateUser(userId)

        // Gera tokens de autenticação
        return generateAuthResponse(UserMapper.toModel(user), null)
    }

    /**
     * Verifica código SMS e completa login
     */
    fun verifySmsAndCompleteLogin(request: VerifySmsRequest): AuthResponse {
        val userId = jwtService.validateTempToken(request.tempToken)
        val user = userService.findUserEntityById(userId)
        val verificationResult = smsCodeService.verifyCode(user.phone, request.code)
        if (!verificationResult.success) {
            throw BusinessException(ExceptionType.INVALID_VERIFICATION_CODE)
        }
        return generateAuthResponse(UserMapper.toModel(user), request.activeCompanyId)
    }

    /**
     * Registro com verificação por email
     */
    fun register(request: RegisterRequest): RegisterResponse {
        validateEmail(request.email)
        validatePhone(request.phone)
        if (userService.existsByEmail(request.email)) {
            throw BusinessException(ExceptionType.EMAIL_ALREADY_EXISTS)
        }
        val user = userService.createUserPending(request)
        val smsVerification = smsCodeService.generateAndSendCode(user.phone)
        val tempToken = jwtService.generateTempToken(user.id!!)
        return RegisterResponse(
            success = true,
            message = "Usuário registrado. Código SMS enviado.",
            tempToken = tempToken,
            expiresIn = smsVerification.expiresIn
        )
    }

    /**
     * Refresh token - gera novos tokens
     */
    fun refresh(request: RefreshTokenRequest): AuthResponse {
        val userId = jwtService.validateRefreshToken(request.refreshToken)
        val user = userService.findUserEntityById(userId)
        if (user.status == UserStatus.INACTIVE) throw BusinessException(ExceptionType.USER_INACTIVE)


        // Revoga o refresh token atual e todos os tokens relacionados
        tokenCacheService.revokeToken(request.refreshToken, userId)

        return generateAuthResponse(UserMapper.toModel(user), request.activeCompanyId)
    }

    /**
     * Logout do usuário
     */
    fun logout(request: LogoutRequest) {
        val userId = jwtService.validateAccessToken(request.accessToken)
        // Revoga o access token e todos os tokens relacionados
        tokenCacheService.revokeToken(request.accessToken, userId)
    }

    /**
     * Reenvio de código SMS
     */
    fun resendCode(request: ResendCodeRequest): SmsVerificationResponse {
        val userId = jwtService.validateTempToken(request.tempToken)
        val user = userService.findUserEntityById(userId)
        return smsCodeService.generateAndSendCode(user.phone)
    }

    /**
     * Verifica se token está revogado
     */
    fun isTokenRevoked(token: String): Boolean {
        return tokenCacheService.isTokenRevoked(token)
    }

    /**
     * Gera resposta de autenticação com tokens
     */
    private fun generateAuthResponse(user: User, activeCompanyId: Long?): AuthResponse {
        val personalCompany = userService.findPersonalCompanyByUserId(user.id!!)
        val finalCompanyId = activeCompanyId ?: personalCompany?.id
        if (finalCompanyId != null) {
            val hasAccess = companyMemberRepository.existsByUserIdAndCompanyId(user.id!!, finalCompanyId)
            if (!hasAccess) throw BusinessException(ExceptionType.COMPANY_ACCESS_DENIED)
        }
        val userRoles = getUserRoles(user.id!!)
        val accessToken = jwtService.generateAccessToken(user.id!!, userRoles, finalCompanyId)
        val refreshToken = jwtService.generateRefreshToken(user.id!!)
        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = tokenExpirationSeconds,
            user = mapUserToResponse(user),
            activeCompany = finalCompanyId?.let { companyService.findById(it) }
        )
    }

    /**
     * Busca roles do usuário baseado nas empresas onde é membro
     */
    private fun getUserRoles(userId: Long): Set<UserRole> {
        val memberRoles = companyMemberRepository.findByUserId(userId)
            .map { it.userRole }
            .toSet()
        return memberRoles.ifEmpty { setOf(UserRole.EMPLOYEE) }
    }

    /**
     * Mapeia User domain para UserResponse
     */
    private fun mapUserToResponse(user: User): UserResponse {
        return UserResponse(
            id = user.id!!,
            name = user.name,
            email = user.email,
            phone = user.phone,
            ownedCompanyId = user.ownedCompany?.id,
            profileImageUrl = user.profileImageUrl,
            status = user.status,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt
        )
    }

    /**
     * Valida formato de email
     */
    private fun validateEmail(email: String) {
        if (!emailPattern.matcher(email).matches()) {
            throw BusinessException(ExceptionType.INVALID_EMAIL_FORMAT)
        }
    }

    /**
     * Valida formato de telefone
     */
    private fun validatePhone(phone: String) {
        if (!phonePattern.matcher(phone).matches()) {
            throw BusinessException(ExceptionType.INVALID_PHONE_FORMAT)
        }
    }

    /**
     * Verifica tentativas de login
     */
    private fun validateLoginAttempts(email: String) {
        if (tokenCacheService.isLoginLocked(email)) {
            throw BusinessException(ExceptionType.TOO_MANY_ATTEMPTS)
        }
    }
}