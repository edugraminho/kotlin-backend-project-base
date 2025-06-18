package com.base.application.security.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.beans.factory.annotation.Value

/**
 * Configuração dos clientes OAuth2 (Google, Apple, Microsoft)
 */
@Configuration
class OAuth2ClientConfig(
    @Value("\${google.oauth.client-id}") private val googleClientId: String,
    @Value("\${google.oauth.client-secret}") private val googleClientSecret: String,
    @Value("\${apple.oauth.client-id}") private val appleClientId: String,
    @Value("\${apple.oauth.client-secret}") private val appleClientSecret: String,
    @Value("\${api.server.url}") private val serverUrl: String
) {

    /**
     * Repositório de registros de clientes OAuth2
     */
    @Bean
    fun clientRegistrationRepository(): ClientRegistrationRepository {
        return InMemoryClientRegistrationRepository(
            googleClientRegistration(),
            appleClientRegistration()
        )
    }

    /**
     * Configuração do cliente Google OAuth2
     */
    private fun googleClientRegistration(): ClientRegistration {
        return ClientRegistration.withRegistrationId("google")
            .clientId(googleClientId)
            .clientSecret(googleClientSecret)
            .clientAuthenticationMethod(org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("${serverUrl}login/oauth2/code/google")
            .scope("openid", "profile", "email")
            .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
            .tokenUri("https://www.googleapis.com/oauth2/v4/token")
            .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
            .userNameAttributeName("sub")
            .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
            .clientName("Google")
            .build()
    }

    /**
     * Configuração do cliente Apple OAuth2
     */
    private fun appleClientRegistration(): ClientRegistration {
        return ClientRegistration.withRegistrationId("apple")
            .clientId(appleClientId)
            .clientSecret(appleClientSecret)
            .clientAuthenticationMethod(org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_POST)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("${serverUrl}login/oauth2/code/apple")
            .scope("name", "email")
            .authorizationUri("https://appleid.apple.com/auth/authorize")
            .tokenUri("https://appleid.apple.com/auth/token")
            .jwkSetUri("https://appleid.apple.com/auth/keys")
            .clientName("Apple")
            .build()
    }
}
