package com.base.application.security.oauth

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component

/**
 * Handler para sucesso no login OAuth2
 */
@Component
class OAuth2SuccessHandler : SimpleUrlAuthenticationSuccessHandler() {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        // TODO: Gerar JWT e redirecionar para aplicação mobile
        // TODO: Implementar deep link ou callback para mobile

        super.onAuthenticationSuccess(request, response, authentication)
    }
}
