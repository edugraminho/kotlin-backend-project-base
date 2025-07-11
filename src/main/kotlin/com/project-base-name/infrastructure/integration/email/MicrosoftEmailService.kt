package com.projectbasename.infrastructure.integration.email

import com.projectbasename.domain.exception.BusinessException
import com.projectbasename.domain.exception.ExceptionType
import com.projectbasename.infrastructure.integration.email.dto.EmailRequest
import com.projectbasename.infrastructure.integration.email.dto.EmailResponse
import jakarta.mail.internet.InternetAddress
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import java.time.LocalDateTime

/**
 * Implementação do serviço de email usando Microsoft 365 SMTP
 * Responsável pelo envio técnico de emails
 */
@Service
class MicrosoftEmailService(
    private val mailSender: JavaMailSender,
    private val templateEngine: TemplateEngine,
    @Value("\${app.mail.from}") private val fromEmail: String
) : EmailService {

    private val log = LoggerFactory.getLogger(this::class.java)

    companion object {
        private const val FROM_NAME = "ProjectBaseName"
        private const val BASE_URL_VAR = "baseUrl"
        private const val CURRENT_YEAR_VAR = "currentYear"
    }

    /**
     * Envia email simples
     */
    override fun sendEmail(request: EmailRequest): EmailResponse {
        return try {
            val mimeMessage = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(mimeMessage, true, "UTF-8")

            helper.setFrom(InternetAddress(fromEmail, FROM_NAME))
            helper.setTo(request.to)
            helper.setSubject(request.subject)
            helper.setText(request.body, request.isHtml)

            // Configurar emails adicionais se fornecidos
            request.replyTo?.let { helper.setReplyTo(it) }
            request.cc?.takeIf { it.isNotEmpty() }?.let { helper.setCc(it.toTypedArray()) }
            request.bcc?.takeIf { it.isNotEmpty() }?.let { helper.setBcc(it.toTypedArray()) }

            mailSender.send(mimeMessage)

            log.info("Email enviado com sucesso para: ${maskEmail(request.to)}")
            
            EmailResponse(
                success = true,
                message = "Email enviado com sucesso",
                recipient = request.to,
                sentAt = LocalDateTime.now()
            )

        } catch (e: Exception) {
            log.error("Erro ao enviar email para ${maskEmail(request.to)}: ${e.message}", e)
            
            EmailResponse(
                success = false,
                message = "Erro ao enviar email",
                recipient = request.to,
                errorDetails = e.message,
                sentAt = LocalDateTime.now()
            )
        }
    }

    /**
     * Envia email com template HTML
     */
    override fun sendEmailWithTemplate(
        to: String,
        subject: String,
        templateName: String,
        variables: Map<String, Any>
    ): EmailResponse {
        return try {
            val context = Context().apply {
                // Adiciona variáveis padrão
                setVariable(CURRENT_YEAR_VAR, LocalDateTime.now().year)
                
                // Adiciona variáveis customizadas
                variables.forEach { (key, value) ->
                    setVariable(key, value)
                }
            }

            val htmlContent = templateEngine.process(templateName, context)

            val emailRequest = EmailRequest(
                to = to,
                subject = subject,
                body = htmlContent,
                isHtml = true
            )

            sendEmail(emailRequest)

        } catch (e: Exception) {
            log.error("Erro ao processar template '$templateName' para ${maskEmail(to)}: ${e.message}", e)
            throw BusinessException(ExceptionType.EMAIL_TEMPLATE_ERROR)
        }
    }

    /**
     * Envia email de convite
     */
    override fun sendInvitationEmail(
        recipientEmail: String,
        inviterName: String,
        companyName: String,
        inviteToken: String,
        baseUrl: String
    ): EmailResponse {
        val variables = mapOf(
            "inviterName" to inviterName,
            "companyName" to companyName,
            "inviteToken" to inviteToken,
            "acceptUrl" to "$baseUrl/api/v1/invitations/accept/$inviteToken",
            BASE_URL_VAR to baseUrl
        )

        return sendEmailWithTemplate(
            to = recipientEmail,
            subject = "Convite para fazer parte da $companyName - ProjectBaseName",
            templateName = "email/invitation",
            variables = variables
        )
    }

    /**
     * Envia email de recuperação de senha
     */
    override fun sendPasswordResetEmail(
        recipientEmail: String,
        userName: String,
        resetToken: String,
        baseUrl: String
    ): EmailResponse {
        val variables = mapOf(
            "userName" to userName,
            "resetToken" to resetToken,
            "resetUrl" to "$baseUrl/reset-password?token=$resetToken",
            BASE_URL_VAR to baseUrl
        )

        return sendEmailWithTemplate(
            to = recipientEmail,
            subject = "Recuperação de senha - ProjectBaseName",
            templateName = "email/password-reset",
            variables = variables
        )
    }

    /**
     * Envia código de verificação por email
     */
    override fun sendVerificationCodeEmail(
        recipientEmail: String,
        userName: String,
        verificationCode: String,
        expirationMinutes: Long
    ): EmailResponse {
        val variables = mapOf(
            "userName" to userName,
            "verificationCode" to verificationCode,
            "expirationMinutes" to expirationMinutes
        )

        return sendEmailWithTemplate(
            to = recipientEmail,
            subject = "Código de verificação - ProjectBaseName",
            templateName = "email/verification-code",
            variables = variables
        )
    }

    /**
     * Mascara email para logs de segurança
     */
    private fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return email

        val username = parts[0]
        val domain = parts[1]

        val maskedUsername = when {
            username.length <= 2 -> username
            username.length <= 4 -> username.take(2) + "*".repeat(username.length - 2)
            else -> username.take(2) + "*".repeat(username.length - 4) + username.takeLast(2)
        }

        return "$maskedUsername@$domain"
    }
} 