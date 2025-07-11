package com.projectbasename.application.dto.auth

import com.projectbasename.domain.enums.user.SocialNetworkType

/**
 * Informações do usuário extraídas do login social
 */
data class SocialUserInfo(
    val providerId: String,
    val name: String? = null,
    val email: String,
    val phone: String? = null,
    val completeName: String? = null,
    val socialNetworkType: SocialNetworkType,
)
