package com.projectbasename.infrastructure.integration.aws.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client

/**
 * Configuração do cliente AWS S3
 * Utiliza as credenciais configuradas no application.yml
 */
@Configuration
@EnableConfigurationProperties(AwsProperties::class, S3StorageProperties::class)
class AwsS3Config(
    private val awsProperties: AwsProperties
) {

    @Bean
    fun s3Client(): S3Client {
        val credentials = AwsBasicCredentials.create(
            awsProperties.credentials.accessKey,
            awsProperties.credentials.secretKey
        )

        return S3Client.builder()
            .region(Region.of(awsProperties.region))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .build()
    }
}

/**
 * Propriedades de configuração do AWS extraídas do application.yml
 */
@ConfigurationProperties(prefix = "aws")
data class AwsProperties(
    val region: String,
    val credentials: AwsCredentials
)

/**
 * Credenciais AWS
 */
data class AwsCredentials(
    val accessKey: String,
    val secretKey: String
)

/**
 * Propriedades de configuração do Storage S3
 */
@ConfigurationProperties(prefix = "storage.s3")
data class S3StorageProperties(
    val bucket: String,
    val region: String,
    val accessKeyId: String,
    val secretAccessKey: String
) 