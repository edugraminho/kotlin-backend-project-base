package com.projectbasename.domain.service.queue

import com.projectbasename.application.dto.payment.PaymentQueueDto
import com.projectbasename.domain.enums.payment.PaymentProvider
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest

/**
 * Serviço simples para enviar mensagens para SQS
 */
@Service
class PaymentQueueProducerService(
    private val sqsClient: SqsClient,
    private val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    @Value("\${queue.payment-webhook.url}")
    private lateinit var queueUrl: String

    /**
     * Envia mensagem para a fila SQS
     */
    fun sendToPaymentWebhookQueue(provider: PaymentProvider, payload: String) {
        try {
            val paymentQueueDto = PaymentQueueDto(
                provider = provider.name,
                data = payload
            )

            val messageBody = objectMapper.writeValueAsString(paymentQueueDto)
            
            val request = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(messageBody)
                .build()

            val response = sqsClient.sendMessage(request)
            log.info("Mensagem enviada para SQS: messageId=${response.messageId()}, provider=$provider")

        } catch (e: Exception) {
            log.error("Erro ao enviar mensagem para SQS: provider=$provider", e)
            throw e
        }
    }

    /**
     * Verifica se o SQS está disponível
     */
    fun isSqsAvailable(): Boolean {
        return try {
            sqsClient.listQueues()
            true
        } catch (e: Exception) {
            log.warn("SQS não disponível: ${e.message}")
            false
        }
    }
} 