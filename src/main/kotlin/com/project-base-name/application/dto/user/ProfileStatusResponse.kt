package com.projectbasename.application.dto.user

/**
 * Response do status do perfil
 */
data class ProfileStatusResponse(
    val needsProfile: Boolean,
    val canCreateCompany: Boolean,
    val canInviteUsers: Boolean,
    val userType: com.projectbasename.domain.enums.user.UserType,
    val hasCompany: Boolean
) 