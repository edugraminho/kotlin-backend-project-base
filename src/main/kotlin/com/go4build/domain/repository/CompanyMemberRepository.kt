package com.base.domain.repository

import com.base.domain.enums.member.MemberStatus
import com.base.domain.enums.member.MemberType
import com.base.domain.enums.member.UserRole
import com.base.domain.entity.CompanyMemberEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Repositório para operações com membros de empresa
 * Combina interface de domínio com implementação Spring Data JPA
 */
@Repository
interface CompanyMemberRepository : JpaRepository<CompanyMemberEntity, Long> {

    /**
     * Busca membro específico por usuário e empresa
     */
    fun findByUserIdAndCompanyId(userId: Long, companyId: Long): CompanyMemberEntity?

    /**
     * Lista todas as empresas que um usuário participa
     */
    fun findByUserId(userId: Long): List<CompanyMemberEntity>

    /**
     * Lista todos os membros de uma empresa
     */
    fun findByCompanyId(companyId: Long, pageable: Pageable): Page<CompanyMemberEntity>

    /**
     * Lista membros ativos de uma empresa
     */
    fun findByCompanyIdAndStatus(
        companyId: Long,
        status: MemberStatus,
        pageable: Pageable
    ): Page<CompanyMemberEntity>

    /**
     * Lista membros por tipo
     */
    fun findByCompanyIdAndMemberType(
        companyId: Long,
        memberType: MemberType,
        pageable: Pageable
    ): Page<CompanyMemberEntity>

    /**
     * Lista membros por role
     */
    fun findByCompanyIdAndUserRole(
        companyId: Long,
        userRole: UserRole,
        pageable: Pageable
    ): Page<CompanyMemberEntity>

    /**
     * Busca proprietário da empresa
     */
    @Query("SELECT cm FROM CompanyMemberEntity cm WHERE cm.company.id = :companyId AND cm.userRole = 'OWNER'")
    fun findOwnerByCompanyId(@Param("companyId") companyId: Long): CompanyMemberEntity?

    /**
     * Lista empresas onde usuário é cliente
     */
    fun findByUserIdAndMemberType(userId: Long, memberType: MemberType): List<CompanyMemberEntity>

    /**
     * Lista empresas onde usuário é fornecedor
     */
    fun findByUserIdAndUserRole(userId: Long, userRole: UserRole): List<CompanyMemberEntity>

    /**
     * Verifica se usuário já é membro da empresa
     */
    fun existsByUserIdAndCompanyId(userId: Long, companyId: Long): Boolean

    /**
     * Verifica se usuário é membro de alguma empresa
     */
    fun existsByUserId(userId: Long): Boolean

    /**
     * Conta membros ativos de uma empresa
     */
    fun countByCompanyIdAndStatus(companyId: Long, status: MemberStatus): Long

    /**
     * Lista membros internos ativos de uma empresa
     */
    @Query(
        """
        SELECT cm FROM CompanyMemberEntity cm 
        WHERE cm.company.id = :companyId 
        AND cm.memberType = 'INTERNAL' 
        AND cm.status = 'ACTIVE'
    """
    )
    fun findActiveInternalMembers(
        @Param("companyId") companyId: Long,
        pageable: Pageable
    ): Page<CompanyMemberEntity>

    /**
     * Lista membros externos (clientes/fornecedores) de uma empresa
     */
    @Query(
        """
        SELECT cm FROM CompanyMemberEntity cm 
        WHERE cm.company.id = :companyId 
        AND cm.memberType IN ('CLIENT', 'SUPPLIER')
        AND cm.status = 'ACTIVE'
    """
    )
    fun findActiveExternalMembers(
        @Param("companyId") companyId: Long,
        pageable: Pageable
    ): Page<CompanyMemberEntity>

    /**
     * Busca administradores de uma empresa
     */
    @Query(
        """
        SELECT cm FROM CompanyMemberEntity cm 
        WHERE cm.company.id = :companyId 
        AND cm.userRole IN ('OWNER', 'ADMIN')
        AND cm.status = 'ACTIVE'
    """
    )
    fun findAdministrators(@Param("companyId") companyId: Long): List<CompanyMemberEntity>

    fun findByCompanyId(companyId: Long): List<CompanyMemberEntity>
}