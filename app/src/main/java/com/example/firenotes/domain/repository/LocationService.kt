package com.example.firenotes.domain.repository

data class AddressDetails(
    val rua: String = "",
    val numero: String = "",
    val bairro: String = "",
    val cidade: String = "",
    val uf: String = ""
)

interface LocationService {
    suspend fun getCurrentLocation(): Result<Pair<Double, Double>>
    suspend fun getAddressFromLocation(latitude: Double, longitude: Double): Result<AddressDetails>
}
