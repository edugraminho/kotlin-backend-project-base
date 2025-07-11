package com.projectbasename.infrastructure.controller

import com.projectbasename.application.security.service.WebhookSecurityService
import com.projectbasename.domain.enums.payment.PaymentProvider
import com.projectbasename.domain.service.payment.RevenueCatPaymentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controller responsável por receber webhooks de pagamento de diferentes provedores
 */
@RestController
@RequestMapping("/v1/payments")
@Tag(name = "Pagamentos", description = "Endpoints para gestão de pagamentos e assinaturas")
class PaymentController(
    private val revenueCatPaymentService: RevenueCatPaymentService,
    private val webhookSecurityService: WebhookSecurityService
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    @Operation(
        summary = "Webhook do RevenueCat",
        description = "Endpoint para receber notificações de webhook do RevenueCat sobre eventos de assinatura"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Webhook processado com sucesso"),
        ApiResponse(responseCode = "400", description = "Dados inválidos no webhook"),
        ApiResponse(responseCode = "401", description = "Assinatura inválida"),
        ApiResponse(responseCode = "500", description = "Erro interno no processamento")
    ])
    @PostMapping("/webhook/revenuecat")
    fun revenueCatWebhook(
        @RequestBody webhookPayload: String,
        @RequestHeader("Authorization") authorization: String? = null
    ): ResponseEntity<Map<String, Any>> {
        
        log.info("Webhook RevenueCat recebido")
        log.debug("Headers Authorization: ${authorization?.take(20)}...")
        
        return try {
            // Valida assinatura do webhook
            if (!webhookSecurityService.validateRevenueCatSignature(webhookPayload, authorization)) {
                log.warn("Assinatura RevenueCat inválida")
                return ResponseEntity.status(401).body(mapOf<String, Any>(
                    "success" to false,
                    "message" to "Assinatura inválida",
                    "timestamp" to System.currentTimeMillis()
                ))
            }
            
            // Processa o webhook
            processWebhook(PaymentProvider.REVENUECAT, webhookPayload)
            
            log.info("Webhook RevenueCat processado com sucesso")
            
            ResponseEntity.ok(mapOf<String, Any>(
                "success" to true,
                "message" to "Webhook processado com sucesso",
                "timestamp" to System.currentTimeMillis()
            ))
            
        } catch (e: Exception) {
            log.error("Erro ao processar webhook RevenueCat", e)
            
            ResponseEntity.status(500).body(mapOf<String, Any>(
                "success" to false,
                "message" to "Erro interno no processamento",
                "error" to (e.message ?: "Erro desconhecido"),
                "timestamp" to System.currentTimeMillis()
            ))
        }
    }

    /**
     * Processa webhook de forma síncrona
     */
    private fun processWebhook(provider: PaymentProvider, payload: String) {
        when (provider) {
            PaymentProvider.REVENUECAT -> {
                revenueCatPaymentService.processNotification(payload)
            }
        }
    }
} 