package com.projectbasename.application.config

import com.amazon.sqs.javamessaging.ProviderConfiguration
import com.amazon.sqs.javamessaging.SQSConnectionFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.context.annotation.Profile
import org.springframework.jms.annotation.EnableJms
import org.springframework.jms.config.DefaultJmsListenerContainerFactory
import software.amazon.awssdk.services.sqs.SqsClient
import jakarta.jms.ConnectionFactory
import jakarta.jms.Session

/**
 * Configuração JMS simples para SQS
 */
@EnableJms
@Configuration
@DependsOn("awsConfig", "sqsClient")
@Profile("consumer")
class JmsConfig(
    private val sqsClient: SqsClient
) {

    @Value("\${queue.payment-webhook.concurrency:3}")
    private lateinit var concurrency: String

    @Bean
    fun amazonSQSConnectionFactory(): ConnectionFactory {
        val providerConfiguration = ProviderConfiguration()
        return SQSConnectionFactory(providerConfiguration, sqsClient)
    }

    @Bean
    fun jmsListenerContainerFactory(): DefaultJmsListenerContainerFactory {
        val factory = DefaultJmsListenerContainerFactory()
        factory.setConnectionFactory(amazonSQSConnectionFactory())
        factory.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE)
        factory.setConcurrency(concurrency)
        return factory
    }
} 