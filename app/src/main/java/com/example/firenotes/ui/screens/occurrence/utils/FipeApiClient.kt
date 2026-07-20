package com.example.firenotes.ui.screens.occurrence.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

@Serializable
data class FipeBrand(
    val codigo: String,
    val nome: String
)

@Serializable
data class FipeModelsResponse(
    val modelos: List<FipeModel>
)

@Serializable
data class FipeModel(
    val codigo: String,
    val nome: String
)

@Serializable
data class FipeYear(
    val codigo: String,
    val nome: String
)

object FipeApiClient {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getBrands(): List<FipeBrand> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://parallelum.com.br/fipe/api/v1/carros/marcas")
            .build()
        
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            val body = response.body?.string() ?: ""
            json.decodeFromString<List<FipeBrand>>(body)
        }
    }

    suspend fun getModels(brandId: String): List<FipeModel> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://parallelum.com.br/fipe/api/v1/carros/marcas/$brandId/modelos")
            .build()
        
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            val body = response.body?.string() ?: ""
            val parsed = json.decodeFromString<FipeModelsResponse>(body)
            parsed.modelos
        }
    }

    suspend fun getYears(brandId: String, modelId: String): List<FipeYear> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://parallelum.com.br/fipe/api/v1/carros/marcas/$brandId/modelos/$modelId/anos")
            .build()
        
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            val body = response.body?.string() ?: ""
            json.decodeFromString<List<FipeYear>>(body)
        }
    }
}
