package com.base.application.security.oauth

import com.base.application.security.service.OAuth2UserService
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

/**
 * Serviço customizado para carregar usuários OAuth2
 */
@Service
class CustomOAuth2UserService(
    private val oauth2UserService: OAuth2UserService
) : DefaultOAuth2UserService() {

    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oauth2User = super.loadUser(userRequest)

        return oauth2UserService.processOAuth2User(userRequest, oauth2User)
    }
}
