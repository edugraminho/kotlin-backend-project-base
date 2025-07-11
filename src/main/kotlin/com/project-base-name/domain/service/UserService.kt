package com.projectbasename.domain.service

import com.projectbasename.application.dto.auth.RegisterRequest
import com.projectbasename.application.dto.company.CompanyResponse
import com.projectbasename.application.dto.user.*
import com.projectbasename.application.mapper.UserMapper
import com.projectbasename.domain.entity.CompanyEntity
import com.projectbasename.domain.entity.CompanyMemberEntity
import com.projectbasename.domain.entity.UserEntity
import com.projectbasename.domain.enums.company.CompanyType
import com.projectbasename.domain.enums.member.MemberType
import com.projectbasename.domain.enums.member.UserRole
import com.projectbasename.domain.enums.user.UserStatus
import com.projectbasename.domain.enums.user.UserType
import com.projectbasename.domain.exception.BusinessException
import com.projectbasename.domain.exception.ExceptionType
import com.projectbasename.domain.model.User
import com.projectbasename.domain.repository.CompanyMemberRepository
import com.projectbasename.domain.repository.CompanyRepository
import com.projectbasename.domain.repository.UserRepository
import com.projectbasename.infrastructure.cache.SimpleCacheService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Serviço de domínio para operações com usuários
 */
@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
    private val companyRepository: CompanyRepository,
    private val companyMemberRepository: CompanyMemberRepository,
    private val passwordEncoder: PasswordEncoder,
    private val cacheService: SimpleCacheService
) {

    /**
     * Cria um novo usuário com empresa (fluxo completo - admin/import)
     */
    fun createUser(request: CreateUserRequest): UserResponse {
        validateUserCreation(request)

        // Criar usuário como OWNER (pode criar empresa)
        val user = createUserFromRequest(request, UserType.OWNER)
        val savedUser = userRepository.save(user)

        // Criar empresa
        val company = createCompanyFromRequest(request, savedUser)
        val savedCompany = companyRepository.save(company)

        // Atualizar usuário com empresa própria
        savedUser.ownedCompany = savedCompany
        val finalUser = userRepository.save(savedUser)

        // Criar membro como OWNER
        val companyMember = CompanyMemberEntity(
            user = finalUser,
            company = savedCompany,
            memberType = MemberType.INTERNAL,
            userRole = UserRole.OWNER
        )
        companyMemberRepository.save(companyMember)

        return mapToResponse(finalUser)
    }

    /**
     * Busca usuário por ID com cache
     */
    fun findById(id: Long): UserResponse {
        return cacheService.getOrSet(
            key = cacheService.userKey(id),
            ttl = SimpleCacheService.USER_TTL
        ) {
            val user = findUserEntityById(id)
            mapToResponse(user)
        }
    }

    /**
     * Busca usuário por email
     */
    fun findByEmail(email: String): UserResponse {
        val user = findUserEntityByEmail(email)
            ?: throw BusinessException(ExceptionType.USER_NOT_FOUND)
        return mapToResponse(user)
    }

    /**
     * Busca entidade por ID
     */
    fun findUserEntityById(id: Long): UserEntity {
        return userRepository.findById(id)
            .orElseThrow { BusinessException(ExceptionType.USER_NOT_FOUND) }
    }

    /**
     * Busca entidade por email
     */
    fun findUserEntityByEmail(email: String): UserEntity? {
        return userRepository.findByEmail(email)
    }

    /**
     * Busca ID do usuário por email
     */
    fun findUserIdByEmail(email: String): Long? {
        return userRepository.findByEmail(email)?.id
    }

    /**
     * Verifica se email já existe
     */
    fun existsByEmail(email: String): Boolean {
        return userRepository.existsByEmail(email)
    }

    /**
     * Cria usuário pendente de ativação (registro básico)
     */
    fun createUserPending(request: RegisterRequest): UserEntity {
        // Determina UserType baseado na presença de invitationToken
        val userType = if (request.invitationToken != null) {
            UserType.INVITED
        } else {
            UserType.OWNER
        }

        val user = UserEntity(
            name = request.name,
            email = request.email,
            phone = request.phone,
            password = passwordEncoder.encode(request.password),
            status = UserStatus.PENDING,
            userType = userType
        )
        return userRepository.save(user)
    }

    /**
     * Ativa usuário após verificação SMS
     */
    fun activateUser(userId: Long): User {
        val user = findUserEntityById(userId)

        user.status = UserStatus.ACTIVE
        user.updateTimestamp()
        return UserMapper.toModel(userRepository.save(user))
    }

    /**
     * Completa perfil do usuário (cria empresa se OWNER)
     */
    fun completeProfile(
        userId: Long,
        request: com.projectbasename.application.dto.user.CompleteProfileRequest
    ): ProfileResponse {
        val user = findUserEntityById(userId)

        // Validar se pode completar perfil
        if (user.userType != UserType.OWNER) {
            throw BusinessException(ExceptionType.OPERATION_NOT_ALLOWED, "Apenas usuários OWNER podem completar perfil")
        }

        if (user.status != UserStatus.ACTIVE) {
            throw BusinessException(ExceptionType.INVALID_USER_STATUS)
        }

        if (user.ownedCompany != null) {
            throw BusinessException(ExceptionType.PROFILE_ALREADY_COMPLETE)
        }

        // Validar telefone obrigatório para usuários de login social sem telefone
        if (user.phone == null && request.phone == null) {
            throw BusinessException(
                ExceptionType.PHONE_REQUIRED,
                "Telefone é obrigatório para completar o perfil"
            )
        }

        // Atualizar telefone se fornecido (importante para usuários de login social)
        if (request.phone != null && request.phone != user.phone) {
            user.phone = request.phone
        }

        // Criar empresa obrigatória
        val company = createCompanyFromProfileRequest(request, user)
        val savedCompany = companyRepository.save(company)

        // Vincular empresa ao usuário
        user.ownedCompany = savedCompany
        val updatedUser = userRepository.save(user)

        // Criar membership como OWNER
        val companyMember = CompanyMemberEntity(
            user = updatedUser,
            company = savedCompany,
            memberType = MemberType.INTERNAL,
            userRole = UserRole.OWNER
        )
        companyMemberRepository.save(companyMember)

        return ProfileResponse(
            user = mapToResponse(updatedUser),
            company = CompanyResponse.from(savedCompany),
            isComplete = true,
            needsCompany = false
        )
    }

    /**
     * Busca empresa pessoal do usuário
     */
    fun findPersonalCompanyByUserId(userId: Long): CompanyEntity? {
        return companyRepository.findPersonalCompanyByOwnerId(userId)
    }

    /**
     * Atualiza a foto do perfil do usuário
     */
    @Transactional
    fun updateProfileImage(userId: Long, profileImageUrl: String): UserResponse {
        val user = findUserEntityById(userId)

        user.profileImageUrl = profileImageUrl
        user.updateTimestamp()

        val updatedUser = userRepository.save(user)

        // Invalida cache da foto de perfil
        cacheService.evict(cacheService.userKey(userId))

        return mapToResponse(updatedUser)
    }

    /**
     * Atualiza usuário
     */
    fun updateUser(id: Long, request: UpdateUserRequest): UserResponse {
        val user = findUserEntityById(id)

        // Validar email único se for alterado
        request.email?.let { newEmail ->
            if (newEmail != user.email && existsByEmail(newEmail)) {
                throw BusinessException(ExceptionType.EMAIL_ALREADY_EXISTS)
            }
        }

        // Atualizar campos
        request.name?.let { user.name = it }
        request.email?.let { user.email = it }
        request.phone?.let { user.phone = it }
        request.profileImageUrl?.let { user.profileImageUrl = it }
        user.updateTimestamp()

        val savedUser = userRepository.save(user)

        // Invalida cache do usuário
        cacheService.evict(cacheService.userKey(id))

        return mapToResponse(savedUser)
    }

    /**
     * Lista usuários com paginação
     */
    fun findAll(pageable: Pageable): Page<UserResponse> {
        return userRepository.findAll(pageable)
            .map { mapToResponse(it) }
    }

    /**
     * Busca usuários por nome
     */
    fun findByName(name: String, pageable: Pageable): Page<UserResponse> {
        return userRepository.findByNameContainingIgnoreCase(name, pageable)
            .map { mapToResponse(it) }
    }

    /*
     * Usado internamente pelos serviços para salvar alterações diretas na entidade
     */
    fun updateUserEntity(user: UserEntity): UserEntity {
        return userRepository.save(user)
    }

    /**
     * Valida dados para criação de usuário
     */
    private fun validateUserCreation(request: CreateUserRequest) {
        // Validar email único
        if (existsByEmail(request.email)) {
            throw BusinessException(ExceptionType.EMAIL_ALREADY_EXISTS)
        }

        // Validar CNPJ para empresa BUSINESS
        if (request.companyType == CompanyType.BUSINESS) {
            if (request.companyName.isNullOrBlank()) {
                throw BusinessException(ExceptionType.COMPANY_NAME_REQUIRED)
            }
            if (request.companyDocument.isNullOrBlank()) {
                throw BusinessException(ExceptionType.COMPANY_DOCUMENT_REQUIRED)
            }
            if (request.companyDocument.replace(Regex("[^0-9]"), "").length != 14) {
                throw BusinessException(ExceptionType.INVALID_CNPJ)
            }
        }
    }

    /**
     * Cria usuário a partir do request
     */
    private fun createUserFromRequest(request: CreateUserRequest, userType: UserType): UserEntity {
        return UserEntity(
            name = request.name,
            email = request.email,
            phone = request.phone,
            password = passwordEncoder.encode(request.password),
            userType = userType,
            status = UserStatus.ACTIVE
        )
    }

    /**
     * Cria empresa a partir do request (fluxo completo)
     */
    private fun createCompanyFromRequest(request: CreateUserRequest, owner: UserEntity): CompanyEntity {
        return when (request.companyType) {
            CompanyType.PERSONAL -> CompanyEntity(
                name = request.name,
                document = cleanDocument(request.document),
                email = request.email,
                phone = request.phone,
                address = request.companyAddress ?: "",
                companyType = CompanyType.PERSONAL,
                owner = owner
            )

            CompanyType.BUSINESS -> CompanyEntity(
                name = request.companyName!!,
                document = cleanDocument(request.companyDocument!!),
                email = request.companyEmail ?: request.email,
                phone = request.companyPhone ?: request.phone,
                address = request.companyAddress ?: "",
                companyType = CompanyType.BUSINESS,
                owner = owner
            )
        }
    }

    /**
     * Cria empresa a partir do complete profile request
     */
    private fun createCompanyFromProfileRequest(request: CompleteProfileRequest, owner: UserEntity): CompanyEntity {
        val phoneToUse = owner.phone!!

        return when (request.companyType) {
            CompanyType.PERSONAL -> CompanyEntity(
                name = owner.name, // Usa nome do usuário
                document = cleanDocument(request.document),
                email = owner.email, // Usa email do usuário
                phone = phoneToUse,
                address = request.address ?: "",
                companyType = CompanyType.PERSONAL,
                owner = owner
            )

            CompanyType.BUSINESS -> {
                // Para BUSINESS, nome da empresa é obrigatório
                if (request.name.isNullOrBlank()) {
                    throw BusinessException(
                        ExceptionType.COMPANY_NAME_REQUIRED,
                        "Nome da empresa é obrigatório para tipo BUSINESS"
                    )
                }

                CompanyEntity(
                    name = request.name,
                    document = cleanDocument(request.document),
                    email = request.email ?: owner.email, // Fallback para email do usuário
                    phone = request.phone ?: phoneToUse, // Fallback para telefone do usuário
                    address = request.address ?: "",
                    companyType = CompanyType.BUSINESS,
                    owner = owner
                )
            }
        }
    }

    /**
     * Remove formatação de documentos (CPF/CNPJ)
     * "11.222.333/0001-44" → "11222333000144"
     */
    private fun cleanDocument(document: String): String {
        return document.replace(Regex("[^0-9]"), "")
    }

    /**
     * Mapeia entidade para DTO de resposta
     */
    private fun mapToResponse(user: UserEntity): UserResponse {
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
}
