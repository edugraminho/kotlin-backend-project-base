package com.projectbasename.application.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import java.util.*

/**
 * Configuração de email para Microsoft 365
 */
@Configuration
class EmailConfig(
    @Value("\${app.mail.host}") private val host: String,
    @Value("\${app.mail.port}") private val port: Int,
    @Value("\${app.mail.username}") private val username: String,
    @Value("\${app.mail.password}") private val password: String
) {

    /**
     * Configuração do JavaMailSender para Microsoft 365
     */
    @Bean
    fun javaMailSender(): JavaMailSender {
        val mailSender = JavaMailSenderImpl().apply {
            host = this@EmailConfig.host
            port = this@EmailConfig.port
            username = this@EmailConfig.username
            password = this@EmailConfig.password
            defaultEncoding = "UTF-8"
        }

        // Propriedades específicas para Microsoft 365
        mailSender.javaMailProperties = Properties().apply {
            setProperty("mail.smtp.auth", "true")
            setProperty("mail.smtp.starttls.enable", "true")
            setProperty("mail.smtp.ssl.enable", "false")
            setProperty("mail.smtp.ssl.protocols", "TLSv1.2")
            setProperty("mail.smtp.connectiontimeout", "60000")
            setProperty("mail.smtp.timeout", "60000")
            setProperty("mail.smtp.writetimeout", "60000")
            setProperty("mail.debug", "false") // Para debug, alterar para true
        }

        return mailSender
    }
} 