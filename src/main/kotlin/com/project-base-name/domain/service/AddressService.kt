package com.projectbasename.domain.service

import com.projectbasename.application.dto.address.AddressResponse
import com.projectbasename.application.dto.address.BrasilApiResponse
import com.projectbasename.application.dto.address.ViaCepResponse
import com.projectbasename.domain.exception.BusinessException
import com.projectbasename.domain.exception.ExceptionType
import com.projectbasename.infrastructure.cache.SimpleCacheService
import com.projectbasename.infrastructure.integration.cep.BrasilApiService
import com.projectbasename.infrastructure.integration.cep.ViaCepService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * Serviço principal para busca de endereços por CEP
 * Implementa fallback entre APIs e cache Redis
 */
@Service
class AddressService(
    private val viaCepService: ViaCepService,
    private val brasilApiService: BrasilApiService,
    private val cacheService: SimpleCacheService
) {
    private val log = LoggerFactory.getLogger(this::class.java)
    
    companion object {
        private const val CACHE_TTL_HOURS = 24L
        private const val CEP_PATTERN = "^[0-9]{8}$"
    }

    /**
     * Busca endereço por CEP com fallback entre APIs e cache
     */
    fun searchByCep(cep: String): AddressResponse {
        // Valida formato do CEP
        val cleanCep = validateAndCleanCep(cep)
        
        // Tenta buscar no cache primeiro
        val cacheKey = "address:cep:$cleanCep"
        val cachedAddress = cacheService.get<AddressResponse>(cacheKey)
        if (cachedAddress != null) {
            log.debug("Endereço encontrado no cache para CEP: $cleanCep")
            return cachedAddress
        }

        // Busca nas APIs com fallback
        val address = searchInApis(cleanCep)
        
        // Salva no cache
        cacheService.set(cacheKey, address, Duration.ofHours(CACHE_TTL_HOURS))
        
        return address
    }

    /**
     * Busca endereço nas APIs com fallback (ViaCEP -> BrasilAPI)
     */
    private fun searchInApis(cep: String): AddressResponse {
        log.info("Buscando endereço para CEP: $cep")
        
        // Primeiro tenta ViaCEP
        try {
            val viaCepResponse = viaCepService.searchByCep(cep).block()
            
            if (viaCepResponse != null && !viaCepResponse.erro) {
                log.info("Endereço encontrado na ViaCEP para CEP: $cep")
                return viaCepResponse.toAddressResponse("viacep")
            }
        } catch (e: Exception) {
            log.warn("Erro na ViaCEP para CEP $cep, tentando BrasilAPI", e)
        }

        // Fallback para BrasilAPI
        try {
            val brasilApiResponse = brasilApiService.searchByCep(cep).block()
            
            if (brasilApiResponse != null && !brasilApiResponse.error) {
                log.info("Endereço encontrado na BrasilAPI para CEP: $cep")
                return brasilApiResponse.toAddressResponse("brasilapi")
            }
        } catch (e: Exception) {
            log.error("Erro na BrasilAPI para CEP $cep", e)
        }

        // Nenhuma API retornou resultado válido
        log.error("CEP não encontrado em nenhuma API: $cep")
        throw BusinessException(ExceptionType.RESOURCE_NOT_FOUND, "CEP não encontrado: $cep")
    }

    /**
     * Valida e limpa o formato do CEP
     */
    private fun validateAndCleanCep(cep: String): String {
        val cleanCep = cep.replace(Regex("[^0-9]"), "")
        
        if (!cleanCep.matches(Regex(CEP_PATTERN))) {
            throw BusinessException(ExceptionType.VALIDATION_ERROR, "CEP inválido: $cep")
        }
        
        return cleanCep
    }

    /**
     * Converte resposta da ViaCEP para AddressResponse
     */
    private fun ViaCepResponse.toAddressResponse(provider: String): AddressResponse {
        return AddressResponse(
            cep = this.cep,
            logradouro = this.logradouro,
            complemento = this.complemento.takeIf { it.isNotBlank() },
            bairro = this.bairro,
            localidade = this.localidade,
            uf = this.uf,
            ibge = this.ibge.takeIf { it.isNotBlank() },
            gia = this.gia.takeIf { it.isNotBlank() },
            ddd = this.ddd.takeIf { it.isNotBlank() },
            siafi = this.siafi.takeIf { it.isNotBlank() },
            provider = provider
        )
    }

    /**
     * Converte resposta da BrasilAPI para AddressResponse
     */
    private fun BrasilApiResponse.toAddressResponse(provider: String): AddressResponse {
        return AddressResponse(
            cep = this.cep,
            logradouro = this.street,
            complemento = null, // BrasilAPI não retorna complemento
            bairro = this.neighborhood,
            localidade = this.city,
            uf = this.state,
            ibge = null, // BrasilAPI não retorna IBGE
            gia = null, // BrasilAPI não retorna GIA
            ddd = null, // BrasilAPI não retorna DDD
            siafi = null, // BrasilAPI não retorna SIAFI
            provider = provider
        )
    }

    /**
     * Invalida cache de um CEP específico
     */
    fun invalidateCache(cep: String) {
        val cleanCep = validateAndCleanCep(cep)
        val cacheKey = "address:cep:$cleanCep"
        cacheService.evict(cacheKey)
        log.info("Cache invalidado para CEP: $cleanCep")
    }

    /**
     * Invalida cache de todos os CEPs
     */
    fun invalidateAllCache() {
        cacheService.evictPattern("address:cep:*")
        log.info("Cache de endereços invalidado")
    }
} 