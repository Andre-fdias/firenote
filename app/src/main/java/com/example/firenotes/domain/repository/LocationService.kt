package com.example.firenotes.domain.repository

data class AddressInfo(
    val rua: String = "",
    val numero: String = "",
    val bairro: String = "",
    val cidade: String = "",
    val uf: String = "",
    val cep: String = "",
    val pais: String = "Brasil"
)

interface LocationService {
    suspend fun getCurrentLocation(): Result<Pair<Double, Double>>
    suspend fun getAddressFromLocation(lat: Double, lon: Double): Result<AddressInfo>
    suspend fun getCoordsFromCityName(cityName: String): Result<Pair<Double, Double>>
    suspend fun checkPermissions(): Boolean
    suspend fun requestPermissions(): Boolean
}
