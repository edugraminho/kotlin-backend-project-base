package com.projectbasename.infrastructure.controller

import com.projectbasename.application.dto.address.AddressResponse
import com.projectbasename.domain.service.AddressService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controller para busca de endereços por CEP
 */
@RestController
@RequestMapping("/v1/address")
@Tag(name = "Address", description = "Endpoints para busca de endereços por CEP")
class AddressController(
    private val addressService: AddressService
) {
    private val log = LoggerFactory.getLogger(this::class.java)
    


    @GetMapping("/cep/{cep}")
    @Operation(
        summary = "Buscar endereço por CEP",
        description = "Busca informações de endereço usando CEP com fallback entre APIs (ViaCEP e BrasilAPI)"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Endereço encontrado com sucesso",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = AddressResponse::class)
                    )
                ]
            ),
            ApiResponse(
                responseCode = "400",
                description = "CEP inválido"
            ),
            ApiResponse(
                responseCode = "404",
                description = "CEP não encontrado"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Erro interno do servidor"
            )
        ]
    )
    fun searchByCep(
        @Parameter(
            description = "CEP a ser consultado (formato: 12345678 ou 12345-678)",
            example = "01310930"
        )
        @PathVariable cep: String
    ): ResponseEntity<AddressResponse> {
        log.info("Buscando endereço para CEP: $cep")

        val address = addressService.searchByCep(cep)

        log.info("Endereço encontrado para CEP $cep: ${address.localidade}/${address.uf}")

        return ResponseEntity.ok(address)
    }

    @DeleteMapping("/cep/{cep}/cache")
    @Operation(
        summary = "Invalidar cache de CEP",
        description = "Remove o endereço do cache para forçar nova consulta nas APIs"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Cache invalidado com sucesso"
            ),
            ApiResponse(
                responseCode = "400",
                description = "CEP inválido"
            )
        ]
    )
    fun invalidateCache(
        @Parameter(
            description = "CEP para invalidar cache",
            example = "01310930"
        )
        @PathVariable cep: String
    ): ResponseEntity<Map<String, String>> {
        log.info("Invalidando cache para CEP: $cep")

        addressService.invalidateCache(cep)

        return ResponseEntity.ok(mapOf("message" to "Cache invalidado com sucesso"))
    }

    @DeleteMapping("/cache")
    @Operation(
        summary = "Invalidar cache de todos os CEPs",
        description = "Remove todos os endereços do cache"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Cache invalidado com sucesso"
            )
        ]
    )
    fun invalidateAllCache(): ResponseEntity<Map<String, String>> {
        log.info("Invalidando cache de todos os CEPs")

        addressService.invalidateAllCache()

        return ResponseEntity.ok(mapOf("message" to "Cache de endereços invalidado com sucesso"))
    }
} 