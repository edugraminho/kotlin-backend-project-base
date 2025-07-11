package com.projectbasename.application.dto.payment

/**
 * DTO simples para mensagens da fila de pagamentos
 */
data class PaymentQueueDto(
    val provider: String,
    val data: String
) 