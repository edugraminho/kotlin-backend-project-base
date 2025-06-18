package com.base.application.dto.company

import com.base.domain.enums.company.CompanyStatus
import com.base.domain.enums.company.CompanyType
import java.time.LocalDateTime

/**
 * DTO de resposta para empresa
 */
data class CompanyResponse(
    val id: Long,
    val name: String,
    val document: String,
    val email: String,
    val phone: String,
    val address: String?,
    val companyType: CompanyType,
    val ownerId: Long,
    val ownerName: String,
    val activePlanId: Long?,
    val status: CompanyStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

