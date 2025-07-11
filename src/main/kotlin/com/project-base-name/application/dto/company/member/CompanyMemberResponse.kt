package com.projectbasename.application.dto.company.member

import com.projectbasename.domain.enums.member.MemberStatus
import com.projectbasename.domain.enums.member.MemberType
import com.projectbasename.domain.enums.member.UserRole
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