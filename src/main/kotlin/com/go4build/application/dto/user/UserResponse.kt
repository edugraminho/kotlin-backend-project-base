package com.base.application.dto.user

import com.base.domain.enums.user.UserStatus
import java.time.LocalDateTime

/**
 * DTO de resposta para usuário
 */
data class UserResponse(
    val id: Long,
    val name: String,
    val email: String,
    val phone: String,
    val ownedCompanyId: Long?,
    val profileImageUrl: String?,
    val status: UserStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)