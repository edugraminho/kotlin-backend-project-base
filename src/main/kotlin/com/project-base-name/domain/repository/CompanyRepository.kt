package com.projectbasename.domain.repository

import com.projectbasename.domain.enums.company.CompanyStatus
import com.projectbasename.domain.enums.company.CompanyType
import com.projectbasename.domain.entity.CompanyEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Repositório para operações com empresas
 * Combina interface de domínio com implementação Spring Data JPA
 */
@Repository
interface CompanyRepository : JpaRepository<CompanyEntity, Long> {

    /**
     * Busca empresa por documento (CNPJ/CPF)
     */
    fun findByDocument(document: String): CompanyEntity?

    /**
     * Busca empresas por proprietário
     */
    fun findByOwnerId(ownerId: Long): List<CompanyEntity>

    /**
     * Busca empresa específica do proprietário por tipo
     */
    fun findByOwnerIdAndCompanyType(ownerId: Long, companyType: CompanyType): CompanyEntity?

    /**
     * Busca empresa pessoal do usuário
     */
    @Query("SELECT c FROM CompanyEntity c WHERE c.owner.id = :ownerId AND c.companyType = 'PERSONAL'")
    fun findPersonalCompanyByOwnerId(@Param("ownerId") ownerId: Long): CompanyEntity?

    /**
     * Lista empresas por tipo
     */
    fun findByCompanyType(companyType: CompanyType, pageable: Pageable): Page<CompanyEntity>

    /**
     * Lista empresas por status
     */
    fun findByStatus(status: CompanyStatus, pageable: Pageable): Page<CompanyEntity>

    /**
     * Busca empresas por nome (case insensitive)
     */
    fun findByNameContainingIgnoreCase(name: String, pageable: Pageable): Page<CompanyEntity>

    /**
     * Verifica se documento já existe
     */
    fun existsByDocument(document: String): Boolean

    /**
     * Lista empresas ativas
     */
    @Query("SELECT c FROM CompanyEntity c WHERE c.status = 'ACTIVE'")
    fun findActiveCompanies(pageable: Pageable): Page<CompanyEntity>

    /**
     * Conta empresas por tipo
     */
    fun countByCompanyType(companyType: CompanyType): Long

    /**
     * Lista empresas ativas por tipo
     */
    @Query(
        """
        SELECT c FROM CompanyEntity c 
        WHERE c.status = 'ACTIVE' 
        AND c.companyType = :companyType
    """
    )
    fun findActiveCompaniesByType(
        @Param("companyType") companyType: CompanyType,
        pageable: Pageable
    ): Page<CompanyEntity>

    /**
     * Busca empresas de negócio (não pessoais)
     */
    @Query("SELECT c FROM CompanyEntity c WHERE c.companyType != 'PERSONAL'")
    fun findBusinessCompanies(pageable: Pageable): Page<CompanyEntity>

    /**
     * Lista empresas por documento parcial (para busca)
     */
    fun findByDocumentContaining(document: String, pageable: Pageable): Page<CompanyEntity>

    @Query(
        """
        SELECT c FROM CompanyEntity c 
        WHERE c.status = 'ACTIVE' 
        AND c.owner.id = :ownerId
    """
    )
    fun findActiveCompaniesByOwnerId(
        @Param("ownerId") ownerId: Long,
        pageable: Pageable
    ): Page<CompanyEntity>
}