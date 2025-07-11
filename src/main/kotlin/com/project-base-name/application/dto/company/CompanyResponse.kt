package com.projectbasename.application.dto.company

import com.projectbasename.domain.entity.CompanyEntity
import com.projectbasename.domain.enums.company.CompanyStatus
import com.projectbasename.domain.enums.company.CompanyType
import java.time.LocalDateTime

/**
 * DTO de resposta para empresa
 */
data class CompanyResponse(
    val id: Long,
    val name: String,
    val document: String,
    val email: String,
    val phone: String?,
    val address: String?,
    val companyType: CompanyType,
    val ownerId: Long,
    val ownerName: String,
    val activePlanId: Long?,
    val status: CompanyStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        /**
         * Converte CompanyEntity para CompanyResponse
         */
        fun from(entity: CompanyEntity): CompanyResponse {
            return CompanyResponse(
                id = entity.id!!,
                name = entity.name,
                document = entity.document,
                email = entity.email,
                phone = entity.phone,
                address = entity.address,
                companyType = entity.companyType,
                ownerId = entity.owner.id!!,
                ownerName = entity.owner.name,
                activePlanId = entity.activePlanId,
                status = entity.status,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }
}