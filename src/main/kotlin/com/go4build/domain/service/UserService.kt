package com.base.domain.service

import com.base.application.dto.auth.RegisterRequest
import com.base.application.dto.user.CreateUserRequest
import com.base.application.dto.user.UpdateUserRequest
import com.base.application.dto.user.UserResponse
import com.base.application.mapper.UserMapper
import com.base.domain.entity.CompanyEntity
import com.base.domain.entity.CompanyMemberEntity
import com.base.domain.entity.UserEntity
import com.base.domain.enums.company.CompanyType
import com.base.domain.enums.member.MemberType
import com.base.domain.enums.member.UserRole
import com.base.domain.enums.user.UserStatus
import com.base.domain.exception.BusinessException
import com.base.domain.exception.ExceptionType
import com.base.domain.model.User
import com.base.domain.repository.CompanyMemberRepository
import com.base.domain.repository.CompanyRepository
import com.base.domain.repository.UserRepository
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
    private val passwordEncoder: PasswordEncoder
) {

    /**
     * Cria um novo usuário com empresa
     */
    fun createUser(request: CreateUserRequest): UserResponse {
        validateUserCreation(request)

        // Criar usuário
        val user = createUserFromRequest(request)
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
     * Busca usuário por ID
     */
    fun findById(id: Long): UserResponse {
        val user = findUserEntityById(id)
        return mapToResponse(user)
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
     * Verifica se email já existe
     */
    fun existsByEmail(email: String): Boolean {
        return userRepository.existsByEmail(email)
    }

    /**
     * Cria usuário pendente
     */
    fun createUserPending(request: RegisterRequest): UserEntity {
        val user = UserEntity(
            name = request.name,
            email = request.email,
            phone = request.phone,
            password = passwordEncoder.encode(request.password),
            status = UserStatus.PENDING,
        )
        return userRepository.save(user)
    }

    /**
     * Ativa usuário
     */
    fun activateUser(userId: Long): User {
        val user = findUserEntityById(userId)

        user.status = UserStatus.ACTIVE
        user.updateTimestamp()
        return UserMapper.toModel(userRepository.save(user))
    }

    /**
     * Busca empresa pessoal do usuário
     */
    fun findPersonalCompanyByUserId(userId: Long): CompanyEntity? {
        val user = findUserEntityById(userId)
        return user.ownedCompany
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
            if (request.companyDocument!!.replace(Regex("[^0-9]"), "").length != 14) {
                throw BusinessException(ExceptionType.INVALID_CNPJ)
            }
        }
    }

    /**
     * Cria usuário a partir do request
     */
    private fun createUserFromRequest(request: CreateUserRequest): UserEntity {
        return UserEntity(
            name = request.name,
            email = request.email,
            phone = request.phone,
            password = passwordEncoder.encode(request.password),
        )
    }

    /**
     * Cria empresa a partir do request
     */
    private fun createCompanyFromRequest(request: CreateUserRequest, owner: UserEntity): CompanyEntity {
        return when (request.companyType) {
            CompanyType.PERSONAL -> CompanyEntity(
                name = request.name,
                document = request.document,
                email = request.email,
                phone = request.phone,
                companyType = CompanyType.PERSONAL,
                owner = owner
            )

            CompanyType.BUSINESS -> CompanyEntity(
                name = request.companyName!!,
                document = request.companyDocument!!,
                email = request.companyEmail ?: request.email,
                phone = request.companyPhone ?: request.phone,
                address = request.companyAddress,
                companyType = CompanyType.BUSINESS,
                owner = owner
            )
        }
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