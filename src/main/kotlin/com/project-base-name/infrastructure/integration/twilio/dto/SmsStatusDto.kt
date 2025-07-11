package com.projectbasename.infrastructure.integration.twilio.dto


import java.time.ZonedDateTime

/**
 * DTO para status de SMS recebido via webhook do Twilio
 */
data class SmsStatusDto(
    val messageId: String?,
    val status: String?,
    val destination: String?,
    val errorCode: Int? = null,
    val errorMessage: String? = null,
    val updatedAt: ZonedDateTime = ZonedDateTime.now()
)

/**
 * DTO para envio de SMS
 */
data class SendSmsDto(
    val phoneNumber: String,
    val message: String
)

/**
 * DTO para resposta de envio de SMS
 */
data class SmsResponseDto(
    val messageId: String,
    val status: String,
    val destination: String
)

/**
 * Enum para status de SMS do Twilio
 */
enum class TwilioSmsStatus(val value: String) {
    QUEUED("queued"),
    SENDING("sending"),
    SENT("sent"),
    DELIVERED("delivered"),
    UNDELIVERED("undelivered"),
    FAILED("failed"),
    RECEIVED("received");

    companion object {
        fun fromValue(value: String): TwilioSmsStatus? {
            return values().find { it.value.equals(value, ignoreCase = true) }
        }
    }
}