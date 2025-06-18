package com.base.domain.repository

import com.base.domain.entity.UserEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Repositório para operações com usuários
 * Combina interface de domínio com implementação Spring Data JPA
 */
@Repository
interface UserRepository : JpaRepository<UserEntity, Long> {

    /**
     * Busca usuário por email
     */
    fun findByEmail(email: String): UserEntity?

    /**
     * Verifica se email já existe
     */
    fun existsByEmail(email: String): Boolean

    /**
     * Busca usuários por nome (case insensitive)
     */
    fun findByNameContainingIgnoreCase(name: String, pageable: Pageable): Page<UserEntity>

}
