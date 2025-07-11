package com.projectbasename.application.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient

/**
 * Configuração AWS para SQS
 */
@Configuration
class AwsConfig {

    @Value("\${aws.region:us-east-1}")
    private lateinit var region: String

    @Value("\${aws.credentials.access-key:}")
    private lateinit var accessKeyId: String

    @Value("\${aws.credentials.secret-key:}")
    private lateinit var secretKey: String

    @Bean
    fun sqsClient(): SqsClient {
        return SqsClient.builder()
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretKey)
                )
            )
            .region(Region.of(region))
            .build()
    }
} 