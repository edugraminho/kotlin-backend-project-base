package com.projectbasename.application.dto.auth

import com.projectbasename.domain.enums.user.SocialNetworkType
import jakarta.validation.constraints.NotBlank

/**
 * Request para login via redes sociais
 */
data class SocialLoginRequest(
    @field:NotBlank(message = "Token é obrigatório")
    val token: String,
    
    val socialNetworkType: SocialNetworkType,
    
    // Para Apple, dados podem vir separados
    val receivedName: String? = null,
    val receivedEmail: String? = null
) 