package com.base.application.security.config

import com.base.application.security.oauth.CustomOAuth2UserService
import com.base.application.security.oauth.OAuth2SuccessHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain

/**
 * Configuração de segurança OAuth2 Resource Server
 * Responsável por configurar autenticação JWT + OAuth2 social login
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig {

    /**
     * Configura a cadeia de filtros de segurança
     */
    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        jwtDecoder: JwtDecoder,
        oauth2UserService: CustomOAuth2UserService,
        oauth2SuccessHandler: OAuth2SuccessHandler
    ): SecurityFilterChain {

        return http
            .csrf { it.disable() }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }

            // OAuth2 Resource Server (JWT validation)
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.decoder(jwtDecoder)
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())
                }
            }

            // OAuth2 Client (Social Login)
            .oauth2Client { }
            .oauth2Login { oauth2 ->
                oauth2
                    .userInfoEndpoint { userInfo ->
                        userInfo.userService(oauth2UserService)
                    }
                    .successHandler(oauth2SuccessHandler)
                    .failureUrl("/auth/login?error=oauth2")
            }

            .authorizeHttpRequests { auth ->
                auth
                    // Public endpoints
                    .requestMatchers("/v1/auth/**").permitAll()
                    .requestMatchers("/v1/users").permitAll()
                    .requestMatchers("/oauth2/**").permitAll()
                    .requestMatchers("/login/oauth2/**").permitAll()
                    .requestMatchers("/actuator/**").permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                    // Protected endpoints
                    .anyRequest().authenticated()
            }

            .exceptionHandling { exceptions ->
                exceptions.authenticationEntryPoint(jwtAuthenticationEntryPoint())
            }

            .build()
    }

    /**
     * Conversor de autenticação JWT customizado
     */
    @Bean
    fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
        val converter = JwtAuthenticationConverter()
        converter.setJwtGrantedAuthoritiesConverter { jwt ->
            val roles = jwt.getClaimAsStringList("roles") ?: emptyList()
            roles.map { SimpleGrantedAuthority("ROLE_$it") }
        }
        converter.setPrincipalClaimName("userId")
        return converter
    }

    /**
     * Entry point para erros de autenticação JWT
     */
    @Bean
    fun jwtAuthenticationEntryPoint(): AuthenticationEntryPoint {
        return BearerTokenAuthenticationEntryPoint()
    }
}
