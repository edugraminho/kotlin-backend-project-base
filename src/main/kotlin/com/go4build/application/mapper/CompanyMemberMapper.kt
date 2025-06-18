package com.base.application.mapper

import com.base.application.dto.company.member.CompanyMemberResponse
import com.base.domain.entity.CompanyMemberEntity
import com.base.domain.model.CompanyMember
import java.time.LocalDateTime

object CompanyMemberMapper {
    fun toModel(entity: CompanyMemberEntity): CompanyMember {
        return CompanyMember(
            id = entity.id,
            company = CompanyMapper.toModel(entity.company),
            user = UserMapper.toModel(entity.user),
            memberType = entity.memberType,
            userRole = entity.userRole,
            status = entity.status,
            invitedBy = entity.invitedBy?.let { UserMapper.toModel(it) },
            joinedAt = entity.joinedAt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(model: CompanyMember): CompanyMemberEntity {
        return CompanyMemberEntity(
            id = model.id,
            company = CompanyMapper.toEntity(model.company),
            user = UserMapper.toEntity(model.user),
            memberType = model.memberType,
            userRole = model.userRole,
            status = model.status,
            invitedBy = model.invitedBy?.let { UserMapper.toEntity(it) },
            joinedAt = model.joinedAt,
            createdAt = model.createdAt,
            updatedAt = model.updatedAt
        )
    }

    fun toResponse(model: CompanyMember): CompanyMemberResponse {
        return CompanyMemberResponse(
            id = model.id!!,
            companyId = model.company.id!!,
            companyName = model.company.name,
            userId = model.user.id!!,
            userName = model.user.name,
            userEmail = model.user.email,
            memberType = model.memberType,
            userRole = model.userRole,
            status = model.status,
            invitedBy = model.invitedBy?.id,
            joinedAt = model.joinedAt,
            createdAt = model.createdAt,
            updatedAt = model.updatedAt
        )
    }
} 