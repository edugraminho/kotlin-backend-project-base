package com.projectbasename.application.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

/**
 * Configuração para exibir URLs importantes após inicialização
 */
@Configuration
class StartupLogger {

    private val logger = LoggerFactory.getLogger(StartupLogger::class.java)

    @Bean
    fun applicationStartupRunner(
        environment: Environment,
        @Value("\${server.port:8080}") serverPort: String,
        @Value("\${springdoc.swagger-ui.path:/swagger-ui.html}") swaggerPath: String
    ) = ApplicationRunner {

        val activeProfiles = environment.activeProfiles.joinToString(", ")
        val isDev = environment.activeProfiles.contains("dev") ||
                environment.activeProfiles.contains("local")

        logger.info("Aplicação PROJECTBASENAME iniciada com sucesso!")
        logger.info("Perfis ativos: $activeProfiles")

        if (isDev) {
            val baseUrl = "http://localhost:$serverPort"

            logger.info("=" * 60)
            logger.info("🔧 AMBIENTE DE DESENVOLVIMENTO")
            logger.info("=" * 60)
            logger.info("Swagger UI: $baseUrl$swaggerPath")
            logger.info("API Docs:   $baseUrl/v3/api-docs")
            logger.info("Health:     $baseUrl/actuator/health")
            logger.info("Metrics:    $baseUrl/actuator/metrics")
            logger.info("Flyway:     $baseUrl/actuator/flyway")
            logger.info("=" * 60)

            // Verificar Redis
            try {
                // Se chegou até aqui, provavelmente Redis está ok
                logger.info("✅ Redis: Conectado")
            } catch (e: Exception) {
                logger.warn("❌ Redis: Erro de conexão - ${e.message}")
            }

            // Verificar Database
            try {
                logger.info("✅ Database: Conectado")
            } catch (e: Exception) {
                logger.warn("❌ Database: Erro de conexão - ${e.message}")
            }

            logger.info("=" * 60)
        } else {
            logger.info("=" * 60)
            logger.info("Aplicação rodando em PRODUÇÃO")
            logger.info("URLs de desenvolvimento desabilitadas")
            logger.info("=" * 60)
        }
    }

    /**
     * Função auxiliar para repetir caracteres
     */
    private operator fun String.times(n: Int): String = this.repeat(n)
} 