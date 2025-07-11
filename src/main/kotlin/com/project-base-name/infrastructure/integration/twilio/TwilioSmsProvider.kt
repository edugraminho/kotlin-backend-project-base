package com.projectbasename.infrastructure.integration.twilio

/**
 * Interface para provider de SMS via Twilio
 * Abstrai implementação técnica específica do Twilio
 */
interface TwilioSmsProvider {

    /**
     * Envia SMS via Twilio
     * @param phone Número de telefone formatado
     * @param message Conteúdo da mensagem
     * @return ID da mensagem Twilio (SID)
     */
    fun sendSms(phone: String, message: String): String

    /**
     * Busca status de mensagem SMS
     * @param messageId SID da mensagem Twilio
     * @return Status atual da mensagem
     */
    fun getMessageStatus(messageId: String): String

    /**
     * Valida webhook do Twilio
     * @param params Parâmetros recebidos
     * @param expectedSignature Assinatura esperada
     * @return true se válido
     */
    fun validateWebhook(params: Map<String, String>, expectedSignature: String): Boolean
}