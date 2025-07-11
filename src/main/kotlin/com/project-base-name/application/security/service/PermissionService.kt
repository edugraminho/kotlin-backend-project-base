package com.projectbasename.application.security.service

import com.projectbasename.domain.enums.member.UserRole
import com.projectbasename.domain.repository.CompanyMemberRepository
import com.projectbasename.domain.service.CompanyService
import com.projectbasename.domain.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service

/**
 * Serviço de permissões baseado na hierarquia de roles
 * Considera a hierarquia: SUPER_USER > OWNER > ADMIN > MANAGER > EMPLOYEE > CLIENT/SUPPLIER > GUEST
 */
@Service
class PermissionService(
    private val companyMemberRepository: CompanyMemberRepository,
    private val userService: UserService,
    private val companyService: CompanyService
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    /**
     * Verifica se o usuário tem role administrativo (SUPER_USER, OWNER, ADMIN)
     */
    fun hasAdminRole(authentication: Authentication): Boolean {
        val userId = getUserIdFromAuthentication(authentication)
        val userRoles = getUserRoles(userId)
        
        return userRoles.any { it in setOf(UserRole.SUPER_USER, UserRole.OWNER, UserRole.ADMIN) }
    }

    /**
     * Verifica se o usuário tem role de super usuário
     */
    fun hasSuperUserRole(authentication: Authentication): Boolean {
        val userId = getUserIdFromAuthentication(authentication)
        val userRoles = getUserRoles(userId)
        
        return UserRole.SUPER_USER in userRoles
    }

    /**
     * Verifica se o usuário é proprietário de alguma empresa
     */
    fun isOwnerOfAnyCompany(authentication: Authentication): Boolean {
        val userId = getUserIdFromAuthentication(authentication)
        val userRoles = getUserRoles(userId)
        
        return UserRole.OWNER in userRoles
    }

    /**
     * Verifica se o usuário é proprietário da empresa específica
     */
    fun isOwnerOfCompany(authentication: Authentication, companyId: Long): Boolean {
        val userId = getUserIdFromAuthentication(authentication)
        
        // Verificar se é proprietário da empresa
        val company = companyService.findCompanyEntityById(companyId)
        if (company.owner.id == userId) {
            return true
        }
        
        // Verificar se tem role OWNER na empresa
        val member = companyMemberRepository.findByUserIdAndCompanyId(userId, companyId)
        return member?.let { 
            it.userRole == UserRole.OWNER && it.status.name == "ACTIVE"
        } ?: false
    }

    /**
     * Verifica se o usuário tem acesso administrativo à empresa (OWNER ou ADMIN)
     */
    fun hasCompanyAdminAccess(authentication: Authentication, companyId: Long): Boolean {
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
     * Verifica se o usuário pode gerenciar assinaturas
     * OWNER e ADMIN podem gerenciar assinaturas de suas empresas
     * SUPER_USER pode gerenciar todas as assinaturas
     */
    fun canManageSubscriptions(authentication: Authentication, companyId: Long? = null): Boolean {
        val userId = getUserIdFromAuthentication(authentication)
        val userRoles = getUserRoles(userId)
        
        // SUPER_USER pode gerenciar todas as assinaturas
        if (UserRole.SUPER_USER in userRoles) {
            return true
        }
        
        // Se não especificou empresa, verificar se tem role administrativo
        if (companyId == null) {
            return userRoles.any { it in setOf(UserRole.OWNER, UserRole.ADMIN) }
        }
        
        // Verificar acesso administrativo à empresa específica
        return hasCompanyAdminAccess(authentication, companyId)
    }

    /**
     * Verifica se o usuário pode gerenciar convites
     * OWNER e ADMIN podem gerenciar convites de suas empresas
     * SUPER_USER pode gerenciar todos os convites
     */
    fun canManageInvitations(authentication: Authentication, companyId: Long? = null): Boolean {
        val userId = getUserIdFromAuthentication(authentication)
        val userRoles = getUserRoles(userId)
        
        // SUPER_USER pode gerenciar todos os convites
        if (UserRole.SUPER_USER in userRoles) {
            return true
        }
        
        // Se não especificou empresa, verificar se tem role administrativo
        if (companyId == null) {
            return userRoles.any { it in setOf(UserRole.OWNER, UserRole.ADMIN) }
        }
        
        // Verificar acesso administrativo à empresa específica
        return hasCompanyAdminAccess(authentication, companyId)
    }

    /**
     * Verifica se o usuário pode acessar recursos administrativos do sistema
     * Apenas SUPER_USER e OWNER podem acessar recursos administrativos
     */
    fun canAccessSystemAdmin(authentication: Authentication): Boolean {
        val userId = getUserIdFromAuthentication(authentication)
        val userRoles = getUserRoles(userId)
        
        return userRoles.any { it in setOf(UserRole.SUPER_USER, UserRole.OWNER) }
    }

    /**
     * Obtém os roles do usuário
     */
    private fun getUserRoles(userId: Long): Set<UserRole> {
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
        
        return try {
            when (principal) {
                is org.springframework.security.oauth2.jwt.Jwt -> {
                    principal.subject.toLong()
                }
                is String -> {
                    principal.toLong()
                }
                else -> {
                    principal.toString().toLong()
                }
            }
        } catch (e: Exception) {
            log.error("Erro ao obter ID do usuário da autenticação", e)
            throw IllegalArgumentException("ID do usuário inválido na autenticação")
        }
    }
} 