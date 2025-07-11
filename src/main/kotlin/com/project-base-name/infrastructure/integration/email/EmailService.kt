package com.projectbasename.infrastructure.integration.email

import com.projectbasename.infrastructure.integration.email.dto.EmailRequest
import com.projectbasename.infrastructure.integration.email.dto.EmailResponse

/**
 * Interface principal do serviço de email
 * Responsável pelo envio de emails com templates e sem templates
 */
interface EmailService {

    /**
     * Envia email simples com texto
     * @param request Dados do email a ser enviado
     * @return Resposta com status do envio
     */
    fun sendEmail(request: EmailRequest): EmailResponse

    /**
     * Envia email com template HTML
     * @param to Email do destinatário
     * @param subject Assunto do email
     * @param templateName Nome do template
     * @param variables Variáveis para o template
     * @return Resposta com status do envio
     */
    fun sendEmailWithTemplate(
        to: String,
        subject: String,
        templateName: String,
        variables: Map<String, Any>
    ): EmailResponse

    /**
     * Envia email de convite para novo usuário
     * @param recipientEmail Email do convidado
     * @param inviterName Nome de quem está convidando
     * @param companyName Nome da empresa
     * @param inviteToken Token do convite
     * @param baseUrl URL base da aplicação
     */
    fun sendInvitationEmail(
        recipientEmail: String,
        inviterName: String,
        companyName: String,
        inviteToken: String,
        baseUrl: String
    ): EmailResponse

    /**
     * Envia email de recuperação de senha
     * @param recipientEmail Email do usuário
     * @param userName Nome do usuário
     * @param resetToken Token de reset
     * @param baseUrl URL base da aplicação
     */
    fun sendPasswordResetEmail(
        recipientEmail: String,
        userName: String,
        resetToken: String,
        baseUrl: String
    ): EmailResponse

    /**
     * Envia código de verificação por email
     * @param recipientEmail Email do usuário
     * @param userName Nome do usuário
     * @param verificationCode Código de verificação
     * @param expirationMinutes Tempo de expiração em minutos
     */
    fun sendVerificationCodeEmail(
        recipientEmail: String,
        userName: String,
        verificationCode: String,
        expirationMinutes: Long
    ): EmailResponse
} 