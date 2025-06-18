package com.base.infrastructure.integration.twilio

import com.base.infrastructure.integration.twilio.dto.SendSmsDto
import com.base.infrastructure.integration.twilio.dto.SmsResponseDto
import com.twilio.Twilio
import com.twilio.exception.ApiException
import com.twilio.rest.api.v2010.account.Message
import com.twilio.security.RequestValidator
import com.twilio.type.PhoneNumber
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct
import javax.net.ssl.SSLPeerUnverifiedException

/**
 * Implementação técnica do provider SMS usando Twilio
 * Responsável apenas pela integração com a API do Twilio
 */
@Service
class TwilioSmsService(
    @Value("\${twilio.account.sid}") private val accountSid: String,
    @Value("\${twilio.auth.token}") private val authToken: String,
    @Value("\${twilio.sms.phone.number}") private val twilioFromSmsPhoneNumber: String,
    @Value("\${api.server.url}") private val apiServerUrl: String
) : TwilioSmsProvider {

    private val log = LoggerFactory.getLogger(this::class.java)

    @PostConstruct
    fun init() {
        try {
            Twilio.init(accountSid, authToken)
            log.info("Twilio SMS Provider inicializado com sucesso")
        } catch (e: Exception) {
            log.error("Erro ao inicializar Twilio SMS Provider", e)
            throw e
        }
    }

    /**
     * Implementação técnica de envio SMS via Twilio
     */
    override fun sendSms(phone: String, message: String): String {
        val formattedPhone = formatPhoneNumber(phone)

        try {
            log.debug("Enviando SMS via Twilio para: ${maskPhone(phone)}")

            val twilioMessage = Message.creator(
                PhoneNumber(formattedPhone),
                PhoneNumber(twilioFromSmsPhoneNumber),
                message
            ).setStatusCallback(getCallbackUrl())
                .create()

            log.info("SMS enviado via Twilio - SID: ${twilioMessage.sid}")
            return twilioMessage.sid

        } catch (e: ApiException) {
            val errorMessage = mapTwilioError(e)
            log.error("Erro da API Twilio: $errorMessage", e)
            throw TwilioApiException(errorMessage, e)

        } catch (e: SSLPeerUnverifiedException) {
            log.error("Erro de conexão SSL com Twilio", e)
            throw TwilioConnectionException("Erro de conexão SSL", e)

        } catch (e: Exception) {
            log.error("Erro inesperado na integração Twilio", e)
            throw TwilioIntegrationException("Erro inesperado", e)
        }
    }

    /**
     * Busca status de mensagem via API Twilio
     */
    override fun getMessageStatus(messageId: String): String {
        return try {
            val message = Message.fetcher(messageId).fetch()
            message.status.toString()
        } catch (e: ApiException) {
            log.error("Erro ao buscar status da mensagem Twilio: $messageId", e)
            throw TwilioApiException("Erro ao buscar status", e)
        } catch (e: Exception) {
            log.error("Erro inesperado ao buscar status da mensagem", e)
            throw TwilioIntegrationException("Erro inesperado", e)
        }
    }

    /**
     * Valida webhook do Twilio usando RequestValidator
     */
    override fun validateWebhook(params: Map<String, String>, expectedSignature: String): Boolean {
        return try {
            val validator = RequestValidator(authToken)
            val isValid = validator.validate(getCallbackUrl(), params, expectedSignature)

            log.debug("Validação de webhook Twilio: ${if (isValid) "válida" else "inválida"}")
            isValid
        } catch (e: Exception) {
            log.error("Erro ao validar webhook do Twilio", e)
            false
        }
    }

    /**
     * Método utilitário para envio com DTO específico
     */
    fun sendSmsWithDto(smsDto: SendSmsDto): SmsResponseDto {
        val messageId = sendSms(smsDto.phoneNumber, smsDto.message)
        val status = getMessageStatus(messageId)

        return SmsResponseDto(
            messageId = messageId,
            status = status,
            destination = smsDto.phoneNumber
        )
    }

    private fun formatPhoneNumber(phone: String): String {
        // Remove caracteres especiais e adiciona +55 se necessário
        val cleanPhone = phone.replace(Regex("[^0-9+]"), "")

        return when {
            cleanPhone.startsWith("+55") -> cleanPhone
            cleanPhone.startsWith("55") -> "+$cleanPhone"
            cleanPhone.length == 11 -> "+55$cleanPhone" // DDD + número
            cleanPhone.length == 10 -> "+55$cleanPhone" // DDD + número sem 9
            else -> cleanPhone
        }
    }

    private fun getCallbackUrl(): String {
        return "${apiServerUrl}twilio-webhooks/sms-status"
    }

    private fun mapTwilioError(e: ApiException): String {
        return when (e.code) {
            20003 -> "Erro de autenticação com Twilio"
            21211 -> "Número de telefone inválido"
            21408 -> "Conta Twilio sem permissão para enviar mensagens"
            21610 -> "Número de telefone não confirmado"
            21614 -> "Número de telefone inválido ou não pode receber mensagens"
            21617 -> "Número de telefone está na lista de bloqueio"
            else -> "Erro Twilio: ${e.message}"
        }
    }

    private fun maskPhone(phone: String): String {
        return if (phone.length >= 4) {
            "*".repeat(phone.length - 4) + phone.takeLast(4)
        } else phone
    }
}

/**
 * Exceções específicas para problemas técnicos do Twilio
 */
class TwilioApiException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class TwilioConnectionException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class TwilioIntegrationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)