package com.projectbasename.application.dto.address

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * DTO para resposta da API ViaCEP
 */
data class ViaCepResponse(
    val cep: String,
    val logradouro: String,
    val complemento: String,
    val bairro: String,
    val localidade: String,
    val uf: String,
    val ibge: String,
    val gia: String,
    val ddd: String,
    val siafi: String,
    @JsonProperty("erro")
    val erro: Boolean = false
) 