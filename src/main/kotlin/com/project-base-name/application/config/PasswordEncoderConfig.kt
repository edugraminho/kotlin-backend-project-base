package com.projectbasename.application.config


import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

/**
 * Configuração do PasswordEncoder para criptografia de senhas
 */
@Configuration
class PasswordEncoderConfig {

    /**
     * Bean do PasswordEncoder usando BCrypt
     * BCrypt é o padrão recomendado para hash de senhas
     */
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
}