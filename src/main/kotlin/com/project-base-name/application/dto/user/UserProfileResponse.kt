package com.projectbasename.application.dto.user

/**
 * Response do perfil do usuário logado
 */
data class UserProfileResponse(
    val user: UserResponse,
    val needsProfile: Boolean,
    val canInviteUsers: Boolean,
    val userType: com.projectbasename.domain.enums.user.UserType
) 