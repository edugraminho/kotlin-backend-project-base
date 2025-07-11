package com.projectbasename.domain.service

import com.projectbasename.application.dto.company.CompanyResponse
import com.projectbasename.application.dto.company.CreateCompanyRequest
import com.projectbasename.application.dto.company.UpdateCompanyRequest
import com.projectbasename.domain.entity.CompanyEntity
import com.projectbasename.domain.entity.CompanyMemberEntity
import com.projectbasename.domain.enums.member.MemberType
import com.projectbasename.domain.enums.member.UserRole
import com.projectbasename.domain.exception.BusinessException
import com.projectbasename.domain.exception.ExceptionType
import com.projectbasename.domain.repository.CompanyMemberRepository
import com.projectbasename.domain.repository.CompanyRepository
import com.projectbasename.domain.repository.UserRepository
import com.projectbasename.infrastructure.cache.SimpleCacheService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Serviço de domínio para operações com empresas
 */
@Service
@Transactional
class CompanyService(
    private val companyRepository: CompanyRepository,
    private val userRepository: UserRepository,
    private val companyMemberRepository: CompanyMemberRepository,
    private val cacheService: SimpleCacheService
) {

    /**
     * Busca empresa por ID com cache
     */
    fun findById(id: Long): CompanyResponse {
        return cacheService.getOrSet(
            key = cacheService.companyKey(id),
            ttl = SimpleCacheService.COMPANY_TTL
        ) {
            val company = findCompanyEntityById(id)
            mapToResponse(company)
        }
    }

    /**
     * Busca entidade por ID
     */
    fun findCompanyEntityById(id: Long): CompanyEntity {
        return companyRepository.findById(id)
            .orElseThrow { BusinessException(ExceptionType.COMPANY_NOT_FOUND) }
    }

    /**
     * Cria empresa
     */
    fun createCompany(request: CreateCompanyRequest): CompanyResponse {
        val owner = userRepository.findById(request.ownerId)
            .orElseThrow { BusinessException(ExceptionType.USER_NOT_FOUND) }

        // Verificar se usuário já possui empresa
        if (owner.ownedCompany != null) {
            throw BusinessException(ExceptionType.USER_ALREADY_HAS_COMPANY)
        }

        val company = CompanyEntity(
            name = request.name,
            document = request.document,
            email = request.email,
            phone = request.phone,
            address = request.address,
            companyType = request.companyType,
            owner = owner
        )

        val savedCompany = companyRepository.save(company)

        // Atualizar usuário com empresa própria
        owner.ownedCompany = savedCompany
        userRepository.save(owner)

        // Criar membro como OWNER
        val companyMember = CompanyMemberEntity(
            user = owner,
            company = savedCompany,
            memberType = MemberType.INTERNAL,
            userRole = UserRole.OWNER
        )
        companyMemberRepository.save(companyMember)

        return mapToResponse(savedCompany)
    }

    /**
     * Atualiza empresa com invalidação de cache
     */
    fun updateCompany(id: Long, request: UpdateCompanyRequest): CompanyResponse {
        val company = findCompanyEntityById(id)

        request.name?.let { company.name = it }
        request.document?.let { company.document = it }
        request.email?.let { company.email = it }
        request.phone?.let { company.phone = it }
        request.address?.let { company.address = it }
        company.updateTimestamp()

        val updatedCompany = companyRepository.save(company)
        val response = mapToResponse(updatedCompany)

        // Invalida cache da empresa e listas relacionadas
        cacheService.evict(cacheService.companyKey(id))
        cacheService.evictPattern("user_companies:*")

        return response
    }

    /**
     * Deleta empresa com invalidação de cache
     */
    fun deleteCompany(id: Long) {
        val company = findCompanyEntityById(id)
        
        // Remover referência da empresa no usuário
        company.owner.ownedCompany = null
        userRepository.save(company.owner)

        companyRepository.delete(company)

        // Invalida cache da empresa deletada
        cacheService.evict(cacheService.companyKey(id))
        cacheService.evictPattern("user_companies:*")
    }

    /**
     * Lista empresas do usuário com cache
     */
    fun listUserCompanies(userId: Long): List<CompanyResponse> {
        return cacheService.getOrSet(
            key = cacheService.userCompaniesKey(userId),
            ttl = SimpleCacheService.LIST_TTL
        ) {
            val user = userRepository.findById(userId)
                .orElseThrow { BusinessException(ExceptionType.USER_NOT_FOUND) }

            val companies = mutableListOf<CompanyEntity>()

            // Adicionar empresa própria se existir
            user.ownedCompany?.let { companies.add(it) }

            // Adicionar empresas onde é membro
            companies.addAll(user.companyMemberships.map { it.company })

            companies.map { mapToResponse(it) }
        }
    }

    /**
     * Lista todas as empresas com paginação
     */
    @Transactional(readOnly = true)
    fun findAll(pageable: Pageable): Page<CompanyResponse> {
        return companyRepository.findAll(pageable)
            .map { mapToResponse(it) }
    }

    /**
     * Busca empresas por nome
     */
    @Transactional(readOnly = true)
    fun findByName(name: String, pageable: Pageable): Page<CompanyResponse> {
        return companyRepository.findByNameContainingIgnoreCase(name, pageable)
            .map { mapToResponse(it) }
    }

    /**
     * Lista empresas que um usuário participa
     */
    @Transactional(readOnly = true)
    fun findCompaniesByUserId(userId: Long): List<CompanyResponse> {
        val user = userRepository.findById(userId)
            .orElseThrow { BusinessException(ExceptionType.USER_NOT_FOUND) }

        return user.companyMemberships
            .map { it.company }
            .map { mapToResponse(it) }
    }

    /**
     * Busca empresa pessoal do usuário
     */
    @Transactional(readOnly = true)
    fun findPersonalCompanyByUserId(userId: Long): CompanyResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { BusinessException(ExceptionType.USER_NOT_FOUND) }

        val company = user.ownedCompany
            ?: throw BusinessException(ExceptionType.COMPANY_NOT_FOUND)

        return mapToResponse(company)
    }

    /**
     * Mapeia entidade para DTO de resposta
     */
    private fun mapToResponse(company: CompanyEntity): CompanyResponse {
        return CompanyResponse(
            id = company.id!!,
            name = company.name,
            document = company.document,
            email = company.email,
            phone = company.phone,
            address = company.address,
            companyType = company.companyType,
            ownerId = company.owner.id!!,
            ownerName = company.owner.name,
            activePlanId = company.activePlanId,
            status = company.status,
            createdAt = company.createdAt,
            updatedAt = company.updatedAt
        )
    }
}