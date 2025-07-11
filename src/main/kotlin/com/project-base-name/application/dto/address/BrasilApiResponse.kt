package com.projectbasename.application.dto.address

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * DTO para resposta da API BrasilAPI
 */
data class BrasilApiResponse(
    val cep: String,
    val state: String,
    val city: String,
    val neighborhood: String,
    val street: String,
    val service: String,
    val location: Location?,
    @JsonProperty("error")
    val error: Boolean = false
) {
    data class Location(
        val type: String,
        val coordinates: Coordinates
    ) {
        data class Coordinates(
            val longitude: String,
            val latitude: String
        )
    }
} 