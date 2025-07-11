package com.projectbasename.application.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

/**
 * Configuração do WebClient para integrações com APIs externas
 */
@Configuration
class WebClientConfig {

    @Bean
    fun webClient(): WebClient {
        val webClient = WebClient.builder()
            .codecs { configurer ->
                configurer.defaultCodecs().maxInMemorySize(1024 * 1024) // 1MB
            }
            .build()
        

        return webClient
    }

    @Bean
    fun webClientWithTimeout(): WebClient {
        return WebClient.builder()
            .codecs { configurer ->
                configurer.defaultCodecs().maxInMemorySize(1024 * 1024) // 1MB
            }
            .build()
    }
} 