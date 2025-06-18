package com.base.application.dto.company.member

import com.base.domain.enums.member.MemberStatus
import com.base.domain.enums.member.MemberType
import com.base.domain.enums.member.UserRole
import java.time.LocalDateTime

data class CompanyMemberResponse(
    val id: Long,
    val companyId: Long,
    val companyName: String,
    val userId: Long,
    val userName: String,
    val userEmail: String,
    val memberType: MemberType,
    val userRole: UserRole,
    val status: MemberStatus,
    val invitedBy: Long?,
    val joinedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) 