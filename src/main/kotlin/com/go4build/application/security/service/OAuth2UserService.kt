package com.base.application.security.service

import com.base.domain.service.UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

/**
 * Serviço para processar informações do usuário OAuth2
 * TODO: Implementar lógica de criação/atualização de usuário via OAuth2
 */
@Service
class OAuth2UserService(
    private val userService: UserService
) {

    /**
     * Processa usuário OAuth2 após login social
     */
    fun processOAuth2User(userRequest: OAuth2UserRequest, oauth2User: OAuth2User): OAuth2User {
        val registrationId = userRequest.clientRegistration.registrationId

        // TODO: Extrair informações do usuário baseado no provider
        // TODO: Criar ou atualizar usuário no sistema
        // TODO: Retornar OAuth2User customizado

        return oauth2User
    }
}
