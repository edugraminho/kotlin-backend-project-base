package com.base.infrastructure.integration.twilio

import com.base.infrastructure.integration.twilio.dto.SmsStatusDto
import com.base.infrastructure.integration.twilio.dto.TwilioSmsStatus
import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.*
import java.time.ZonedDateTime

/**
 * Controller para receber webhooks do Twilio
 * Adaptado do projeto anterior para nova estrutura
 */
@RestController
@RequestMapping("/twilio-webhooks")
@Tag(name = "Twilio Webhooks", description = "Endpoints para receber status de SMS do Twilio")
class TwilioWebhookController(
    private val twilioSmsProvider: TwilioSmsService
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    @Operation(
        summary = "Webhook para status de SMS",
        description = "Recebe atualizações de status de SMS enviados pelo Twilio"
    )
    @PostMapping("/sms-status", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun handleSmsStatus(
        @RequestParam paramMap: MultiValueMap<String, String>,
        @RequestHeader("X-Twilio-Signature") twilioSignature: String?
    ): ResponseEntity<Any> {

        try {
            val params = paramMap.toSingleValueMap()

            // Valida assinatura do Twilio
            val isValidSignature = twilioSmsProvider.validateWebhook(
                params,
                twilioSignature ?: ""
            )

            if (!isValidSignature) {
                log.warn("Webhook inválido recebido - assinatura não confere")
                return ResponseEntity.badRequest().build()
            }

            // Processa status do SMS
            val smsStatus = SmsStatusDto(
                messageId = params["MessageSid"],
                status = params["MessageStatus"],
                destination = params["To"],
                errorCode = params["ErrorCode"]?.toIntOrNull(),
                errorMessage = params["ErrorMessage"],
                updatedAt = ZonedDateTime.now()
            )

            processSmsStatus(smsStatus)

            return ResponseEntity.ok().build()

        } catch (e: Exception) {
            log.error("Erro ao processar webhook de status SMS", e)
            return ResponseEntity.internalServerError().build()
        }
    }

    @Hidden
    @PostMapping("/sms-response", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun handleSmsResponse(
        @RequestParam paramMap: MultiValueMap<String, String>,
        @RequestHeader("X-Twilio-Signature") twilioSignature: String?
    ): ResponseEntity<Any> {

        try {
            val params = paramMap.toSingleValueMap()

            val isValidSignature = twilioSmsProvider.validateWebhook(
                params,
                twilioSignature ?: ""
            )

            if (isValidSignature) {
                log.info("Resposta SMS recebida: ${params["From"]} -> ${params["Body"]}")
            }

            return ResponseEntity.ok().build()

        } catch (e: Exception) {
            log.error("Erro ao processar resposta SMS", e)
            return ResponseEntity.internalServerError().build()
        }
    }

    /**
     * Processa status de SMS e realiza logs apropriados
     */
    private fun processSmsStatus(smsStatus: SmsStatusDto) {
        val maskedDestination = maskPhone(smsStatus.destination ?: "")

        when (TwilioSmsStatus.fromValue(smsStatus.status ?: "")) {
            TwilioSmsStatus.DELIVERED -> {
                log.info("SMS entregue com sucesso - ID: ${smsStatus.messageId}, Destino: $maskedDestination")
            }

            TwilioSmsStatus.FAILED, TwilioSmsStatus.UNDELIVERED -> {
                log.error(
                    "Falha na entrega de SMS - ID: ${smsStatus.messageId}, " +
                            "Destino: $maskedDestination, " +
                            "Erro: ${smsStatus.errorCode} - ${smsStatus.errorMessage}"
                )
            }

            TwilioSmsStatus.SENT -> {
                log.info("SMS enviado - ID: ${smsStatus.messageId}, Destino: $maskedDestination")
            }

            TwilioSmsStatus.QUEUED, TwilioSmsStatus.SENDING -> {
                log.debug("SMS em processamento - ID: ${smsStatus.messageId}, Status: ${smsStatus.status}")
            }

            else -> {
                log.info("Status SMS recebido - ID: ${smsStatus.messageId}, Status: ${smsStatus.status}")
            }
        }

        // TODO: Implementar persistência de logs se necessário
        // Pode ser útil para auditoria e monitoramento
    }

    private fun maskPhone(phone: String): String {
        return if (phone.length >= 4) {
            "*".repeat(phone.length - 4) + phone.takeLast(4)
        } else phone
    }
}