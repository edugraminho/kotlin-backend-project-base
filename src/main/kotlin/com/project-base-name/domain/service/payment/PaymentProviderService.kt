package com.projectbasename.domain.service.payment

import com.projectbasename.domain.enums.payment.PaymentProvider

/**
 * Interface base para serviços de provedores de pagamento
 * Define o contrato que todos os provedores devem implementar
 */
interface PaymentProviderService {
    
    /**
     * Retorna o provedor de pagamento que este serviço implementa
     */
    fun getProvider(): PaymentProvider
    
    /**
     * Processa notificação de webhook do provedor
     * 
     * @param webhookPayload Payload JSON recebido do webhook
     */
    fun processNotification(webhookPayload: String)
} 