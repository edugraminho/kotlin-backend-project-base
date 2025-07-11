package com.projectbasename.application.security.config

import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import javax.crypto.spec.SecretKeySpec

/**
 * Configuração do JWT Decoder/Encoder
 * Responsável por validar e gerar tokens JWT
 */
@Configuration
class JwtDecoderConfig(
    @Value("\${jwt.secret}") private val jwtSecret: String
) {

    /**
     * Bean para decodificar e validar JWTs
     */
    @Bean
    fun jwtDecoder(): JwtDecoder {
        val secretKey = SecretKeySpec(jwtSecret.toByteArray(), "HmacSHA256")
        return NimbusJwtDecoder.withSecretKey(secretKey)
            .macAlgorithm(MacAlgorithm.HS256)
            .build()
    }

    /**
     * Bean para codificar e gerar JWTs
     */
    @Bean
    fun jwtEncoder(): JwtEncoder {
        val secretKey = SecretKeySpec(jwtSecret.toByteArray(), "HmacSHA256")
        val immutableSecret = ImmutableSecret<SecurityContext>(secretKey)
        return NimbusJwtEncoder(immutableSecret)
    }
}
