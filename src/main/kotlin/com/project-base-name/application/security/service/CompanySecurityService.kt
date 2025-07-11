package com.projectbasename.application.security.service

import com.projectbasename.domain.enums.member.UserRole
import com.projectbasename.domain.repository.CompanyMemberRepository
import com.projectbasename.domain.service.CompanyService
import com.projectbasename.domain.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service

/**
 * Serviço de segurança para validação de permissões de empresa
 */
@Service
class CompanySecurityService(
    private val companyMemberRepository: CompanyMemberRepository,
    private val userService: UserService,
    private val companyService: CompanyService
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    /**
     * Verifica se o usuário autenticado é proprietário ou administrador da empresa
     */
    fun isOwnerOrAdmin(authentication: Authentication, companyId: Long): Boolean {
        val userId = getUserIdFromAuthentication(authentication)
        
        // Verificar se é proprietário da empresa
        val company = companyService.findCompanyEntityById(companyId)
        if (company.owner.id == userId) {
            return true
        }
        
        // Verificar se é membro com papel administrativo
        val member = companyMemberRepository.findByUserIdAndCompanyId(userId, companyId)
        return member?.let { 
            it.userRole in setOf(UserRole.OWNER, UserRole.ADMIN, UserRole.SUPER_USER) && 
            it.status.name == "ACTIVE"
        } ?: false
    }

    /**
     * Verifica se o usuário autenticado pode acessar a empresa
     */
    fun canAccessCompany(authentication: Authentication, companyId: Long): Boolean {
        val userId = getUserIdFromAuthentication(authentication)
        
        // Verificar se é proprietário da empresa
        val company = companyService.findCompanyEntityById(companyId)
        if (company.owner.id == userId) {
            return true
        }
        
        // Verificar se é membro ativo da empresa
        val member = companyMemberRepository.findByUserIdAndCompanyId(userId, companyId)
        return member?.let { it.status.name == "ACTIVE" } ?: false
    }

    /**
     * Verifica se o usuário autenticado pode atualizar outro usuário
     * Regras:
     * - SUPER_USER pode atualizar qualquer usuário
     * - Usuário pode atualizar a si mesmo
     * - OWNER/ADMIN só podem atualizar usuários da mesma empresa
     */
    fun canUpdateUser(authentication: Authentication, targetUserId: Long): Boolean {
        val currentUserId = getUserIdFromAuthentication(authentication)
        
        // Usuário pode atualizar a si mesmo
        if (currentUserId == targetUserId) {
            return true
        }
        
        // Verificar roles do usuário atual
        val currentUserRoles = getCurrentUserRoles(currentUserId)
        
        // SUPER_USER pode atualizar qualquer usuário
        if (UserRole.SUPER_USER in currentUserRoles) {
            return true
        }
        
        // Para OWNER/ADMIN, verificar se ambos usuários pertencem às mesmas empresas
        if (UserRole.OWNER in currentUserRoles || UserRole.ADMIN in currentUserRoles) {
            return usersShareCompany(currentUserId, targetUserId)
        }
        
        return false
    }

    /**
     * Verifica se dois usuários compartilham pelo menos uma empresa
     */
    private fun usersShareCompany(userId1: Long, userId2: Long): Boolean {
        val user1Companies = companyMemberRepository.findByUserId(userId1).map { it.company.id }
        val user2Companies = companyMemberRepository.findByUserId(userId2).map { it.company.id }
        
        // Verificar se há interseção entre as empresas
        return user1Companies.intersect(user2Companies.toSet()).isNotEmpty()
    }

    /**
     * Obtém os roles do usuário atual
     */
    private fun getCurrentUserRoles(userId: Long): Set<UserRole> {
        return companyMemberRepository.findByUserId(userId)
            .filter { it.status.name == "ACTIVE" }
            .map { it.userRole }
            .toSet()
    }

    /**
     * Extrai o ID do usuário da autenticação
     */
    private fun getUserIdFromAuthentication(authentication: Authentication): Long {
        val principal = authentication.principal
        log.debug("Getting user ID from principal type: ${principal?.javaClass?.simpleName}")
        
        return try {
            when (principal) {
                is Jwt -> {
                    // Com a configuração correta, o subject do JWT deve conter o userId
                    val userId = principal.subject
                    log.debug("JWT subject (userId): $userId")
                    userId.toLong()
                }
                is String -> {
                    // Para outros tipos de autenticação
                    log.debug("String principal: $principal")
                    principal.toLong()
                }
                else -> {
                    // Fallback - tentar extrair do toString()
                    val principalStr = principal.toString()
                    log.debug("Fallback principal toString(): $principalStr")
                    principalStr.toLong()
                }
            }
        } catch (e: NumberFormatException) {
            log.error("Falha ao converter principal para Long. Principal: $principal, Tipo: ${principal?.javaClass?.simpleName}", e)
            throw IllegalArgumentException("ID do usuário inválido na autenticação")
        } catch (e: Exception) {
            log.error("Erro inesperado ao obter ID do usuário. Principal: $principal", e)
            throw IllegalArgumentException("Erro na autenticação do usuário")
        }
    }
} 