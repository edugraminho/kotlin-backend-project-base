package com.base.application.dto.company

import com.base.domain.enums.member.MemberStatus
import com.base.domain.enums.member.MemberType
import java.time.LocalDateTime

data class CompanyMemberResponse(
    val id: Long,
    val companyId: Long,
    val companyName: String,
    val userId: Long,
    val userName: String,
    val userEmail: String,
    val memberType: MemberType,
    val status: MemberStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) 