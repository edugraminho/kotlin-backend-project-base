package com.projectbasename.domain.service.queue

import com.projectbasename.application.dto.payment.PaymentQueueDto
import com.projectbasename.domain.enums.payment.PaymentProvider
import com.projectbasename.domain.service.payment.RevenueCatPaymentService
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Service
import jakarta.jms.JMSException
import jakarta.jms.TextMessage

/**
 * Serviço simples para consumir mensagens da SQS
 */
@Service
@Profile("consumer")
class PaymentQueueConsumerService(
    private val revenueCatPaymentService: RevenueCatPaymentService,
    private val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    /**
     * Listener para mensagens da fila de pagamentos
     */
    @JmsListener(
        destination = "\${queue.payment-webhook.name}",
        containerFactory = "jmsListenerContainerFactory"
    )
    @Throws(JMSException::class)
    fun receivePaymentWebhookMessage(textMessage: TextMessage) {
        val messageId = textMessage.jmsMessageID
        
        log.info("Processando mensagem SQS: messageId=$messageId")
        
        try {
            val paymentQueueDto = objectMapper.readValue(
                textMessage.text, 
                PaymentQueueDto::class.java
            )
            
            when (PaymentProvider.fromValue(paymentQueueDto.provider)) {
                PaymentProvider.REVENUECAT -> {
                    revenueCatPaymentService.processNotification(paymentQueueDto.data)
                    log.info("Mensagem RevenueCat processada: messageId=$messageId")
                }
                else -> {
                    log.warn("Provedor não suportado: ${paymentQueueDto.provider}")
                }
            }
            
            textMessage.acknowledge()
            
        } catch (e: Exception) {
            log.error("Erro ao processar mensagem SQS: messageId=$messageId", e)
            throw JMSException("Erro no processamento: ${e.message}")
        }
    }
} 