package com.projectbasename.application.util

import com.projectbasename.domain.exception.BusinessException
import com.projectbasename.domain.exception.ExceptionType
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt

/**
 * Utilitários para operações de segurança
 */
object SecurityUtils {

    private val log = LoggerFactory.getLogger(this::class.java)

    /**
     * Obtém o ID do usuário autenticado
     * @return ID do usuário autenticado
     * @throws BusinessException se não houver usuário autenticado
     */
    fun getCurrentUserId(): Long {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw BusinessException(ExceptionType.UNAUTHORIZED, "Usuário não autenticado")
        
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
            throw BusinessException(ExceptionType.UNAUTHORIZED, "ID do usuário inválido na autenticação")
        } catch (e: Exception) {
            log.error("Erro inesperado ao obter ID do usuário. Principal: $principal", e)
            throw BusinessException(ExceptionType.UNAUTHORIZED, "Erro na autenticação do usuário")
        }
    }

    /**
     * Verifica se há um usuário autenticado
     * @return true se há usuário autenticado, false caso contrário
     */
    fun isAuthenticated(): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication != null && authentication.isAuthenticated
    }
} 