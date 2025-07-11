package com.projectbasename.domain.model

import com.projectbasename.domain.enums.company.CompanyStatus
import com.projectbasename.domain.enums.company.CompanyType
import java.time.LocalDateTime

/**
 * Representa uma empresa no sistema.
 * Pode ser uma empresa real (BUSINESS) ou "empresa pessoal" (PERSONAL) para profissionais individuais.
 *
 * Regras:
 * - PERSONAL: dados replicados do User (name, document)
 * - BUSINESS: name e document obrigatórios e específicos da empresa
 */
data class Company(
    val id: Long? = null,
    val name: String,
    val document: String, // CNPJ para BUSINESS, CPF replicado para PERSONAL
    val email: String,
    val phone: String?,
    val address: String? = null, // Endereço em formato livre
    val companyType: CompanyType = CompanyType.PERSONAL,
    val owner: User,
    val activePlanId: Long? = null,
    val status: CompanyStatus = CompanyStatus.ACTIVE,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Verifica se é uma empresa pessoal (profissional individual)
     */
    fun isPersonal(): Boolean = companyType == CompanyType.PERSONAL

    /**
     * Verifica se é uma empresa de negócios
     */
    fun isBusiness(): Boolean = companyType == CompanyType.BUSINESS

    /**
     * Verifica se a empresa está ativa
     */
    fun isActive(): Boolean = status == CompanyStatus.ACTIVE

    /**
     * Verifica se possui dados completos da empresa (para BUSINESS)
     */
    fun hasCompleteBusinessData(): Boolean =
        isBusiness() && email.isNotBlank() && !phone.isNullOrBlank() && address != null

    /**
     * Verifica se o document é um CNPJ (14 dígitos)
     */
    fun hasCnpj(): Boolean = document.replace(Regex("[^0-9]"), "").length == 14

    /**
     * Valida se os dados estão consistentes com o tipo de empresa
     */
    fun isDataConsistent(): Boolean = when (companyType) {
        CompanyType.PERSONAL -> true // Para PERSONAL, dados são replicados do User
        CompanyType.BUSINESS -> name.isNotBlank() && hasCnpj()
    }

    fun hasActivePlan(): Boolean = activePlanId != null
}