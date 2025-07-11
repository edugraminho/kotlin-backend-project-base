package com.projectbasename.application.security.service

import com.projectbasename.application.config.RevenueCatConfig
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Serviço responsável pela validação de segurança dos webhooks
 * Implementa validação de assinatura para diferentes provedores
 */
@Service
class WebhookSecurityService(
    private val revenueCatConfig: RevenueCatConfig
) {
    
    private val log = LoggerFactory.getLogger(this::class.java)

    /**
     * Valida assinatura do webhook do RevenueCat
     * Baseado na documentação oficial: https://docs.revenuecat.com/docs/webhooks
     */
    fun validateRevenueCatSignature(payload: String, signature: String?): Boolean {
        if (signature.isNullOrBlank()) {
            log.warn("Assinatura RevenueCat ausente")
            return false
        }

        try {
            val signatureKey = revenueCatConfig.api.signatureKey
            if (signatureKey.isBlank()) {
                log.error("Chave de assinatura RevenueCat não configurada")
                return false
            }

            // Remove prefixo "Bearer " se presente
            val cleanSignature = signature.removePrefix("Bearer ")
            
            // Gera assinatura esperada usando HMAC-SHA256
            val expectedSignature = generateHmacSha256(payload, signatureKey)
            
            // Compara assinaturas de forma segura
            val isValid = MessageDigest.isEqual(
                expectedSignature.toByteArray(),
                cleanSignature.toByteArray()
            )
            
            if (!isValid) {
                log.warn("Assinatura RevenueCat inválida")
            }
            
            return isValid
            
        } catch (e: Exception) {
            log.error("Erro ao validar assinatura RevenueCat", e)
            return false
        }
    }

    /**
     * Gera assinatura HMAC-SHA256
     */
    private fun generateHmacSha256(data: String, key: String): String {
        val secretKeySpec = SecretKeySpec(key.toByteArray(), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(secretKeySpec)
        
        val hash = mac.doFinal(data.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Método público para testes gerarem assinaturas válidas
     */
    fun generateHmacSha256ForTest(data: String, key: String): String {
        return generateHmacSha256(data, key)
    }

    /**
     * Valida se o ambiente está configurado corretamente
     */
    fun validateEnvironment(provider: String, environment: String?): Boolean {
        return when (provider.uppercase()) {
            "REVENUECAT" -> {
                val configEnvironment = revenueCatConfig.api.environment
                environment == null || environment.equals(configEnvironment, ignoreCase = true)
            }
            else -> {
                log.warn("Provedor não reconhecido: $provider")
                false
            }
        }
    }
} 