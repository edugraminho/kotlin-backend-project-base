package com.projectbasename.application.mapper

import com.projectbasename.domain.entity.UserEntity
import com.projectbasename.domain.model.User

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
            status = entity.status,
            userType = entity.userType,
            profileImageUrl = entity.profileImageUrl,
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
            userType = model.userType,
            profileImageUrl = model.profileImageUrl,
            createdAt = model.createdAt,
            updatedAt = model.updatedAt
        )
    }
}