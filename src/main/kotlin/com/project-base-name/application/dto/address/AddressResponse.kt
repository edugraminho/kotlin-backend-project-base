package com.projectbasename.application.dto.address

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * DTO de resposta para informações de endereço obtidas via CEP
 */
data class AddressResponse(
    val cep: String,
    val logradouro: String,
    val complemento: String?,
    val bairro: String,
    val localidade: String,
    val uf: String,
    val ibge: String?,
    val gia: String?,
    val ddd: String?,
    val siafi: String?,
    @JsonProperty("provider")
    val provider: String
) 