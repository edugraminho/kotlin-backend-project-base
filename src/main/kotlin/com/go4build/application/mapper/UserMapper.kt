package com.base.application.mapper

import com.base.domain.entity.UserEntity
import com.base.domain.model.User

/**
 * Mapper para converter entre UserEntity e User
 */
object UserMapper {

    /**
     * Converte UserEntity para User
     */
    fun toModel(entity: UserEntity): User {
        return User(
            id = entity.id,
            name = entity.name,
            email = entity.email,
            phone = entity.phone,
            password = entity.password,
            ownedCompany = entity.ownedCompany?.let { CompanyMapper.toModel(it) },
            profileImageUrl = entity.profileImageUrl,
            status = entity.status,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    /**
     * Converte User para UserEntity
     */
    fun toEntity(model: User): UserEntity {
        return UserEntity(
            id = model.id,
            name = model.name,
            email = model.email,
            phone = model.phone,
            password = model.password,
            status = model.status,
            profileImageUrl = model.profileImageUrl,
            createdAt = model.createdAt,
            updatedAt = model.updatedAt,
            ownedCompany = model.ownedCompany?.let { CompanyMapper.toEntity(it) },
        )
    }
} 