package com.projectbasename.infrastructure.integration.cep

import com.projectbasename.application.dto.address.BrasilApiResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * Serviço de integração com a API BrasilAPI
 * Documentação: https://brasilapi.com.br/docs
 */
@Service
class BrasilApiService(
    private val webClient: WebClient
) {
    private val log = LoggerFactory.getLogger(this::class.java)
    

    
    companion object {
        private const val BASE_URL = "https://brasilapi.com.br/api/cep/v2"
        private const val TIMEOUT_SECONDS = 5L
    }

    /**
     * Busca endereço por CEP na API BrasilAPI
     */
    fun searchByCep(cep: String): Mono<BrasilApiResponse> {
        val cleanCep = cep.replace(Regex("[^0-9]"), "")
        
        return webClient.get()
            .uri("$BASE_URL/$cleanCep")
            .retrieve()
            .bodyToMono(BrasilApiResponse::class.java)
            .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .doOnSuccess { response ->
                if (response.error) {
                    log.warn("BrasilAPI retornou erro para CEP: $cep")
                } else {
                    log.debug("BrasilAPI encontrou endereço para CEP: $cep")
                }
            }
            .doOnError { error ->
                when (error) {
                    is WebClientResponseException.NotFound -> {
                        log.warn("CEP não encontrado na BrasilAPI: $cep")
                    }
                    is WebClientResponseException -> {
                        log.error("Erro HTTP na BrasilAPI para CEP $cep: ${error.statusCode}", error)
                    }
                    else -> {
                        log.error("Erro na integração com BrasilAPI para CEP $cep", error)
                    }
                }
            }
    }
} 