package com.projectbasename.domain.service

import com.projectbasename.domain.exception.BusinessException
import com.projectbasename.domain.exception.ExceptionType
import com.projectbasename.infrastructure.integration.twilio.TwilioSmsProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Serviço de domínio para SMS
 * Implementa regras de negócio para envio de mensagens SMS
 */
@Service
@Transactional
class SmsService(
    private val twilioSmsProvider: TwilioSmsProvider
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    /**
     * Envia SMS com validações de negócio
     * @param phone Número de telefone no formato +55XXXXXXXXXXX
     * @param message Conteúdo da mensagem
     * @return ID da mensagem enviada
     */
    fun sendSms(phone: String, message: String): String {
        validateBusinessRules(phone, message)

        return try {
            val messageId = twilioSmsProvider.sendSms(phone, message)
            log.info("SMS enviado com sucesso via domínio - ID: $messageId")
            messageId
        } catch (e: Exception) {
            log.error("Erro no serviço de domínio ao enviar SMS", e)
            throw BusinessException(ExceptionType.SMS_SEND_ERROR)
        }
    }

    /**
     * Verifica status de uma mensagem SMS
     * @param messageId ID da mensagem retornado pelo envio
     * @return Status atual da mensagem
     */
    fun getMessageStatus(messageId: String): String {
        validateMessageId(messageId)

        return try {
            twilioSmsProvider.getMessageStatus(messageId)
        } catch (e: Exception) {
            log.error("Erro ao buscar status da mensagem no domínio", e)
            throw BusinessException(ExceptionType.SMS_STATUS_ERROR)
        }
    }

    /**
     * Envia SMS de verificação com template padrão
     * @param phone Telefone do usuário
     * @param code Código de verificação
     * @param expirationMinutes Tempo de expiração em minutos
     * @return ID da mensagem
     */
    fun sendVerificationSms(phone: String, code: String, expirationMinutes: Long): String {
        val message = buildVerificationMessage(code, expirationMinutes)
        return sendSms(phone, message)
    }

    /**
     * Verifica se o número de telefone é válido para o Brasil
     */
    fun isValidBrazilianPhone(phone: String): Boolean {
        val cleanPhone = phone.replace(Regex("[^0-9+]"), "")

        return when {
            cleanPhone.startsWith("+55") && cleanPhone.length == 14 -> true // +55 + DDD + 9 dígitos
            cleanPhone.startsWith("+55") && cleanPhone.length == 13 -> true // +55 + DDD + 8 dígitos
            cleanPhone.startsWith("55") && cleanPhone.length == 13 -> true // 55 + DDD + 9 dígitos
            cleanPhone.startsWith("55") && cleanPhone.length == 12 -> true // 55 + DDD + 8 dígitos
            cleanPhone.length == 11 -> true // DDD + 9 dígitos
            cleanPhone.length == 10 -> true // DDD + 8 dígitos
            else -> false
        }
    }

    private fun validateBusinessRules(phone: String, message: String) {
        when {
            phone.isBlank() -> throw BusinessException(ExceptionType.INVALID_PHONE_FORMAT)
            message.isBlank() -> throw BusinessException(ExceptionType.INVALID_SMS_CONTENT)
            message.length > 160 -> throw BusinessException(ExceptionType.SMS_TOO_LONG)
            !isValidBrazilianPhone(phone) -> throw BusinessException(ExceptionType.INVALID_PHONE_FORMAT)
        }
    }

    private fun validateMessageId(messageId: String) {
        if (messageId.isBlank()) {
            throw BusinessException(ExceptionType.INVALID_MESSAGE_ID)
        }
    }

    private fun buildVerificationMessage(code: String, expirationMinutes: Long): String {
        return "ProjectBaseName - Seu código de verificação: $code. Válido por $expirationMinutes minutos."
    }
}