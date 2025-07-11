package com.projectbasename.application.security.service

import com.projectbasename.application.dto.auth.*
import com.projectbasename.application.dto.user.UserResponse
import com.projectbasename.application.mapper.UserMapper
import com.projectbasename.application.security.verification.SmsCodeService
import com.projectbasename.domain.enums.member.UserRole
import com.projectbasename.domain.enums.user.UserStatus
import com.projectbasename.domain.enums.user.UserType
import com.projectbasename.domain.exception.BusinessException
import com.projectbasename.domain.exception.ExceptionType
import com.projectbasename.domain.model.User
import com.projectbasename.domain.repository.CompanyMemberRepository
import com.projectbasename.domain.repository.UserRepository
import com.projectbasename.domain.service.CompanyService
import com.projectbasename.domain.service.UserService
import com.projectbasename.infrastructure.cache.TokenCacheService
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import java.util.regex.Pattern

/**
 * Serviço principal de autenticação
 * Gerencia login, registro e verificação SMS/Email com UserType
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
    private val socialLoginService: SocialLoginService,
    private val userRepository: UserRepository,
    @Value("\${jwt.access-token-expiration-seconds:86400}") private val tokenExpirationSeconds: Long,
    @Value("\${verification.sms.max-attempts:3}") private val maxSmsAttempts: Int,
) {
    // Padrões de validação
    private val emailPattern = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)")
    private val phonePattern = Pattern.compile("^\\+?[1-9]\\d{1,14}")

    /**
     * Login com SMS - Primeira etapa
     * Se usuário não tem telefone, ou é SUPER_USER, login direto sem verificação SMS
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

        tokenCacheService.resetLoginAttempts(request.email)

        // Bypass SMS para SUPER_USER
        val memberRoles = companyMemberRepository.findByUserId(user.id!!)
            .map { it.userRole }
            .toSet()
            .ifEmpty { setOf(UserRole.EMPLOYEE) }
        if (UserRole.SUPER_USER in memberRoles) {
            val authResponse = generateAuthResponse(UserMapper.toModel(user), null)
            return LoginResponse(
                requiresVerification = false,
                verificationType = VerificationType.EMAIL,
                message = "Login realizado com sucesso",
                authResponse = authResponse
            )
        }

        // Se usuário não tem telefone, manter fluxo direto (sem SMS)
        return if (user.phone.isNullOrBlank()) {
            LoginResponse(
                requiresVerification = false,
                verificationType = VerificationType.EMAIL,
                message = "Login realizado com sucesso"
            )
        } else {
            // Fluxo normal com SMS
            val smsVerification = smsCodeService.generateAndSendCode(user.phone!!)
            val tempToken = jwtService.generateTempToken(user.id!!)

            LoginResponse(
                requiresVerification = true,
                verificationType = VerificationType.SMS,
                message = "Código SMS enviado",
                tempToken = tempToken,
                expiresIn = smsVerification.expiresIn,
            )
        }
    }

    /**
     * Verifica código SMS e ativa conta (registro)
     */
    fun verifySmsAndActivateAccount(request: VerifySmsRequest): AuthResponse {
        val userId = jwtService.validateTempToken(request.tempToken)
        val user = userService.findUserEntityById(userId)

        // Verifica se usuário está pendente
        if (user.status != UserStatus.PENDING) {
            throw BusinessException(ExceptionType.INVALID_USER_STATUS, "Usuário já está ativo")
        }

        // Verifica código SMS
        val verificationResult = smsCodeService.verifyCode(user.phone!!, request.code)
        if (!verificationResult.success) {
            throw BusinessException(ExceptionType.INVALID_VERIFICATION_CODE)
        }

        // Ativa usuário
        val activatedUser = userService.activateUser(userId)

        // Gera tokens de autenticação com flags baseadas em UserType
        return generateAuthResponseForActivation(activatedUser)
    }

    /**
     * Verifica código SMS e completa login
     */
    fun verifySmsAndCompleteLogin(request: VerifySmsRequest): AuthResponse {
        val userId = jwtService.validateTempToken(request.tempToken)
        val user = userService.findUserEntityById(userId)
        val verificationResult = smsCodeService.verifyCode(user.phone!!, request.code)
        if (!verificationResult.success) {
            throw BusinessException(ExceptionType.INVALID_VERIFICATION_CODE)
        }
        return generateAuthResponse(UserMapper.toModel(user), request.activeCompanyId)
    }

    /**
     * Registro básico com UserType automático
     * Telefone é OBRIGATÓRIO - sempre envia SMS para verificação
     * (Social login é diferente e não passa por aqui)
     */
    fun register(request: RegisterRequest): RegisterResponse {
        validateEmail(request.email)
        validatePhone(request.phone) // Sempre obrigatório no registro comum
        
        if (userService.existsByEmail(request.email)) {
            throw BusinessException(ExceptionType.EMAIL_ALREADY_EXISTS)
        }

        // UserType é determinado automaticamente no UserService
        val user = userService.createUserPending(request)
        val smsVerification = smsCodeService.generateAndSendCode(user.phone!!)
        val tempToken = jwtService.generateTempToken(user.id!!)
        
        val userTypeMessage = if (request.invitationToken != null) {
            "Usuário convidado registrado."
        } else {
            "Usuário independente registrado."
        }

        return RegisterResponse(
            success = true,
            message = "$userTypeMessage Código SMS enviado.",
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
        return smsCodeService.generateAndSendCode(user.phone!!)
    }

    /**
     * Login via redes sociais (Google, Apple, Facebook)
     * Baseado na implementação simples do projeto antigo
     */
    fun socialLogin(request: SocialLoginRequest): AuthResponse {
        // Valida token social e extrai informações do usuário
        val socialUserInfo = socialLoginService.validateSocialToken(request)

        // Busca usuário existente por email
        val existingUser = userService.findUserEntityByEmail(socialUserInfo.email)

        val userEntity =
            if (existingUser != null) {
                // Usuário já existe - atualiza informações se necessário
                updateUserFromSocialLogin(existingUser, socialUserInfo)
            } else {
                // Cria novo usuário com informações do social login
                createUserFromSocialLogin(socialUserInfo)
            }

        if (userEntity.status == UserStatus.INACTIVE) {
            throw BusinessException(ExceptionType.USER_INACTIVE)
        }

        // TODO: Associar provider social ao usuário se ainda não estiver associado

        return generateAuthResponse(UserMapper.toModel(userEntity), null)
    }

    /**
     * Verifica se token está revogado
     */
    fun isTokenRevoked(token: String): Boolean = tokenCacheService.isTokenRevoked(token)

    /**
     * Gera resposta de autenticação para ativação (com flags UserType)
     */
    private fun generateAuthResponseForActivation(user: User): AuthResponse {
        val userRoles = getUserRoles(user.id!!)
        val accessToken = jwtService.generateAccessToken(user.id!!, userRoles, null)
        val refreshToken = jwtService.generateRefreshToken(user.id!!)

        // Determinar flags baseadas no UserType
        val needsProfile = user.userType == UserType.OWNER
        val hasInvitation = user.userType == UserType.INVITED

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = tokenExpirationSeconds,
            user = mapUserToResponse(user),
            activeCompany = null, // Sem empresa na ativação
            needsProfile = needsProfile,
            hasInvitation = hasInvitation,
        )
    }

    /**
     * Gera resposta de autenticação com tokens (login normal)
     */
    private fun generateAuthResponse(
        user: User,
        activeCompanyId: Long?,
    ): AuthResponse {
        val personalCompany = userService.findPersonalCompanyByUserId(user.id!!)
        val finalCompanyId = activeCompanyId ?: personalCompany?.id

        if (finalCompanyId != null) {
            val hasAccess = companyMemberRepository.existsByUserIdAndCompanyId(user.id!!, finalCompanyId)
            if (!hasAccess) throw BusinessException(ExceptionType.COMPANY_ACCESS_DENIED)
        }

        val userRoles = getUserRoles(user.id!!)
        val accessToken = jwtService.generateAccessToken(user.id!!, userRoles, finalCompanyId)
        val refreshToken = jwtService.generateRefreshToken(user.id!!)

        // Determinar flags baseadas no UserType e estado atual
        val needsProfile = user.userType == UserType.OWNER && personalCompany == null
        val hasInvitation = user.userType == UserType.INVITED && hasPendingInvitations(user.id!!)

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = tokenExpirationSeconds,
            user = mapUserToResponse(user),
            activeCompany = finalCompanyId?.let { companyService.findById(it) },
            needsProfile = needsProfile,
            hasInvitation = hasInvitation,
        )
    }

    /**
     * Busca roles do usuário baseado nas empresas onde é membro
     */
    private fun getUserRoles(userId: Long): Set<UserRole> {
        val memberRoles =
            companyMemberRepository
                .findByUserId(userId)
                .map { it.userRole }
                .toSet()
        return memberRoles.ifEmpty { setOf(UserRole.EMPLOYEE) }
    }

    /**
     * Verifica se usuário tem convites pendentes
     */
    private fun hasPendingInvitations(userId: Long): Boolean {
        // TODO: Implementar verificação de convites pendentes
        return false
    }

    /**
     * Mapeia User domain para UserResponse
     */
    private fun mapUserToResponse(user: User): UserResponse {
        val personalCompany = userService.findPersonalCompanyByUserId(user.id!!)

        return UserResponse(
            id = user.id!!,
            name = user.name,
            email = user.email,
            phone = user.phone,
            ownedCompanyId = personalCompany?.id,
            profileImageUrl = user.profileImageUrl,
            status = user.status,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt,
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

    /**
     * Cria usuário a partir de informações do login social (não precisa verificação)
     */
    private fun createUserFromSocialLogin(socialUserInfo: SocialUserInfo): com.projectbasename.domain.entity.UserEntity {
        val user = com.projectbasename.domain.entity.UserEntity(
            name = socialUserInfo.name ?: socialUserInfo.completeName?.split(" ")?.firstOrNull() ?: "Usuário",
            email = socialUserInfo.email,
            phone = null,
            password = passwordEncoder.encode(UUID.randomUUID().toString()),
            status = UserStatus.ACTIVE,
            userType = UserType.OWNER
        )
        
        return userRepository.save(user)
    }

    /**
     * Atualiza usuário existente com informações do social login
     */
    private fun updateUserFromSocialLogin(
        existingUser: com.projectbasename.domain.entity.UserEntity,
        socialUserInfo: SocialUserInfo,
    ): com.projectbasename.domain.entity.UserEntity {
        // Atualiza nome se estiver em branco ou "null"
        existingUser.name = socialUserInfo.name ?: socialUserInfo.completeName?.split(" ")?.firstOrNull() ?: "Usuário"
        existingUser.updateTimestamp()
        return userRepository.save(existingUser)
    }
}
