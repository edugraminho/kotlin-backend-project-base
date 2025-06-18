package com.base.application.mapper

import com.base.domain.entity.CompanyEntity
import com.base.domain.model.Company

object CompanyMapper {
    fun toModel(entity: CompanyEntity): Company {
        return Company(
            id = entity.id,
            name = entity.name,
            document = entity.document,
            email = entity.email,
            phone = entity.phone,
            address = entity.address,
            companyType = entity.companyType,
            owner = UserMapper.toModel(entity.owner),
            activePlanId = entity.activePlanId,
            status = entity.status,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(model: Company): CompanyEntity {
        return CompanyEntity(
            id = model.id,
            name = model.name,
            document = model.document,
            email = model.email,
            phone = model.phone,
            address = model.address,
            companyType = model.companyType,
            owner = UserMapper.toEntity(model.owner),
            activePlanId = model.activePlanId,
            status = model.status,
            createdAt = model.createdAt,
            updatedAt = model.updatedAt
        )
    }
} 