package com.projectbasename.infrastructure.integration.cep

import com.projectbasename.application.dto.address.ViaCepResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * Serviço de integração com a API ViaCEP
 * Documentação: https://viacep.com.br/
 */
@Service
class ViaCepService(
    private val webClient: WebClient
) {
    private val log = LoggerFactory.getLogger(this::class.java)
    

    
    companion object {
        private const val BASE_URL = "https://viacep.com.br/ws"
        private const val TIMEOUT_SECONDS = 5L
    }

    /**
     * Busca endereço por CEP na API ViaCEP
     */
    fun searchByCep(cep: String): Mono<ViaCepResponse> {
        val cleanCep = cep.replace(Regex("[^0-9]"), "")
        
        return webClient.get()
            .uri("$BASE_URL/$cleanCep/json")
            .retrieve()
            .bodyToMono(ViaCepResponse::class.java)
            .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .doOnSuccess { response ->
                if (response.erro) {
                    log.warn("ViaCEP retornou erro para CEP: $cep")
                } else {
                    log.debug("ViaCEP encontrou endereço para CEP: $cep")
                }
            }
            .doOnError { error ->
                when (error) {
                    is WebClientResponseException.NotFound -> {
                        log.warn("CEP não encontrado na ViaCEP: $cep")
                    }
                    is WebClientResponseException -> {
                        log.error("Erro HTTP na ViaCEP para CEP $cep: ${error.statusCode}", error)
                    }
                    else -> {
                        log.error("Erro na integração com ViaCEP para CEP $cep", error)
                    }
                }
            }
    }
} 