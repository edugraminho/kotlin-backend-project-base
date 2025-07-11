package com.projectbasename.infrastructure.controller

import com.projectbasename.domain.enums.payment.PaymentProvider
import com.projectbasename.domain.service.queue.PaymentQueueProducerService
import com.projectbasename.domain.service.payment.RevenueCatPaymentService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controller simples para webhooks de pagamento
 */
@RestController
@RequestMapping("/api/v1/payment/webhooks")
class PaymentWebhookController(
    private val paymentQueueProducerService: PaymentQueueProducerService,
    private val revenueCatPaymentService: RevenueCatPaymentService
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    /**
     * Webhook do RevenueCat
     */
    @PostMapping("/revenuecat")
    fun revenueCatWebhook(
        @RequestHeader("X-Signature") signature: String?,
        @RequestBody payload: String
    ): ResponseEntity<Map<String, String>> {
        
        log.info("Webhook RevenueCat recebido")
        
        try {
            if (paymentQueueProducerService.isSqsAvailable()) {
                paymentQueueProducerService.sendToPaymentWebhookQueue(
                    PaymentProvider.REVENUECAT, 
                    payload
                )
            } else {
                // Fallback síncrono
                revenueCatPaymentService.processNotification(payload)
            }

            return ResponseEntity.ok(mapOf("status" to "success"))

        } catch (e: Exception) {
            log.error("Erro ao processar webhook RevenueCat", e)
            return ResponseEntity.internalServerError()
                .body(mapOf("error" to "Internal server error"))
        }
    }
} 