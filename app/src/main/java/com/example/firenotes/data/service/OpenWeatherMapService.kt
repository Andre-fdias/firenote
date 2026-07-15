package com.example.firenotes.data.service

import android.content.Context
import android.location.Geocoder
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.firenotes.domain.model.WeatherInfo
import com.example.firenotes.domain.repository.WeatherService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.encodeToString
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import javax.inject.Inject

class OpenWeatherMapService @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context
) : WeatherService {

    companion object {
        private const val CACHE_EXPIRY_MINUTES = 30L
        private const val TAG = "FireWeather"
        private const val API_KEY = "f33fb834cc84e015b8b724a0befae3ac"
        private const val BASE_URL = "https://api.openweathermap.org/data/2.5/weather"

        private val WEATHER_CACHE_KEY = stringPreferencesKey("weather_cache")
        private val WEATHER_UPDATE_KEY = longPreferencesKey("weather_update")
    }

    override suspend fun getCurrentWeather(lat: Double, lon: Double): WeatherInfo {
        // 1. Verificar cache primeiro
        val cached = getCachedWeather()
        if (cached != null && !isCacheExpired(cached.timestamp)) {
            logD("✅ Usando cache: ${cached.city} - ${cached.temperature}°C")
            return cached
        }

        // 2. Buscar dados da API
        logD("🔄 Buscando dados da API OpenWeatherMap para lat=$lat, lon=$lon")
        
        try {
            val url = "$BASE_URL?lat=$lat&lon=$lon&appid=$API_KEY&units=metric&lang=pt_br"
            val response = fetchWeatherData(url)
            val weatherInfo = parseWeatherResponse(response, lat, lon)
            
            // 3. Salvar cache
            saveWeatherCache(weatherInfo)
            
            logD("✅ Clima atualizado: ${weatherInfo.city} - ${weatherInfo.temperature}°C")
            return weatherInfo
            
        } catch (e: Exception) {
            logE("❌ Erro ao buscar clima: ${e.message}")
            
            // 4. Fallback para cache mesmo expirado
            if (cached != null) {
                logD("⚠️ Usando cache expirado como fallback")
                return cached
            }
            
            // 5. Fallback para dados mockados
            return getMockWeather(lat, lon)
        }
    }

    override suspend fun getWeatherByCity(cityName: String): WeatherInfo {
        logD("🔍 Buscando clima para cidade no OpenWeatherMap: $cityName")
        
        try {
            val encodedCity = URLEncoder.encode(cityName, "UTF-8")
            val url = "$BASE_URL?q=$encodedCity&appid=$API_KEY&units=metric&lang=pt_br"
            val response = fetchWeatherData(url)
            val weatherInfo = parseWeatherResponse(response)
            
            // Salvar cache
            saveWeatherCache(weatherInfo)
            return weatherInfo
        } catch (e: Exception) {
            logE("❌ Erro ao buscar clima por cidade: ${e.message}")
            throw e
        }
    }

    override suspend fun getCachedWeather(): WeatherInfo? {
        return try {
            dataStore.data.first()[WEATHER_CACHE_KEY]?.let { json ->
                Json.decodeFromString<WeatherInfo>(json)
            }
        } catch (e: Exception) {
            logE("❌ Erro ao ler cache: ${e.message}")
            null
        }
    }

    override suspend fun saveWeatherCache(info: WeatherInfo) {
        try {
            val json = Json.encodeToString(info.copy(timestamp = System.currentTimeMillis()))
            dataStore.edit { preferences ->
                preferences[WEATHER_CACHE_KEY] = json
                preferences[WEATHER_UPDATE_KEY] = System.currentTimeMillis()
            }
            logD("✅ Cache salvo: ${info.city} - ${info.temperature}°C")
        } catch (e: Exception) {
            logE("❌ Erro ao salvar cache: ${e.message}")
        }
    }

    override suspend fun clearCache() {
        dataStore.edit { preferences ->
            preferences.remove(WEATHER_CACHE_KEY)
            preferences.remove(WEATHER_UPDATE_KEY)
        }
        logD("✅ Cache limpo")
    }

    // ============================================
    // MÉTODOS PRIVADOS
    // ============================================

    private fun isCacheExpired(timestamp: Long): Boolean {
        val diff = System.currentTimeMillis() - timestamp
        return diff > (CACHE_EXPIRY_MINUTES * 60 * 1000)
    }

    private suspend fun fetchWeatherData(url: String): String {
        return withContext(Dispatchers.IO) {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 10000 // 10 segundos
            connection.readTimeout = 10000
            connection.requestMethod = "GET"
            
            try {
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    throw IOException("HTTP error: $responseCode")
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    private suspend fun parseWeatherResponse(
        json: String, 
        fallbackLat: Double? = null, 
        fallbackLon: Double? = null
    ): WeatherInfo {
        val root = Json.parseToJsonElement(json).jsonObject
        
        // Extrair dados principais
        val mainObj = root["main"]?.jsonObject
        val temp = mainObj?.get("temp")?.jsonPrimitive?.content?.toDoubleOrNull() ?: 24.0
        val humidity = mainObj?.get("humidity")?.jsonPrimitive?.content?.toIntOrNull() ?: 68
        
        val windObj = root["wind"]?.jsonObject
        val windSpeedMps = windObj?.get("speed")?.jsonPrimitive?.content?.toDoubleOrNull() ?: 3.3
        val windSpeedKmh = (windSpeedMps * 3.6).toInt()
        
        val cloudsObj = root["clouds"]?.jsonObject
        val precipProb = cloudsObj?.get("all")?.jsonPrimitive?.content?.toIntOrNull() ?: 0

        // Obter nome da cidade e coordenadas
        val jsonName = root["name"]?.jsonPrimitive?.content
        val country = root["sys"]?.jsonObject?.get("country")?.jsonPrimitive?.content
        
        val coordObj = root["coord"]?.jsonObject
        val lat = coordObj?.get("lat")?.jsonPrimitive?.content?.toDoubleOrNull() ?: fallbackLat
        val lon = coordObj?.get("lon")?.jsonPrimitive?.content?.toDoubleOrNull() ?: fallbackLon

        val cityName = if (lat != null && lon != null) {
            geocodeReverse(lat, lon) ?: listOfNotNull(jsonName, country).joinToString(", ")
        } else {
            listOfNotNull(jsonName, country).joinToString(", ")
        }.ifBlank { "Localização desconhecida" }

        // Mapear weather id para condição
        val weatherObj = root["weather"]?.jsonArray?.firstOrNull()?.jsonObject
        val weatherId = weatherObj?.get("id")?.jsonPrimitive?.content?.toIntOrNull() ?: 800
        val description = weatherObj?.get("description")?.jsonPrimitive?.content?.replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
        } ?: "Céu limpo"

        val (_, icon) = mapOpenWeatherMapId(weatherId)

        return WeatherInfo(
            city = cityName,
            temperature = temp.toInt(),
            condition = description,
            conditionIcon = icon,
            humidity = humidity,
            windSpeed = windSpeedKmh,
            precipitation = precipProb,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun mapOpenWeatherMapId(id: Int): Pair<String, String> {
        return when (id) {
            in 200..299 -> Pair("Tempestade", "⚡")
            in 300..399 -> Pair("Garoa", "🌧️")
            in 500..599 -> Pair("Chuva", "🌧️")
            in 600..699 -> Pair("Neve", "❄️")
            in 700..799 -> Pair("Nevoeiro", "🌫️")
            800 -> Pair("Céu limpo", "☀️")
            801 -> Pair("Poucas nuvens", "🌤️")
            802 -> Pair("Nuvens dispersas", "⛅")
            803, 804 -> Pair("Nublado", "☁️")
            else -> Pair("Tempo firme", "☀️")
        }
    }

    private suspend fun geocodeReverse(lat: Double, lon: Double): String? {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                addresses?.firstOrNull()?.let { address ->
                    val city = address.locality ?: address.subAdminArea ?: ""
                    val state = address.adminArea ?: ""
                    if (city.isNotEmpty() && state.isNotEmpty()) {
                        "$city/$state"
                    } else {
                        address.featureName ?: "Localização desconhecida"
                    }
                }
            } catch (e: Exception) {
                logE("❌ Erro no geocoding reverso: ${e.message}")
                null
            }
        }
    }

    private fun getMockWeather(lat: Double, lon: Double): WeatherInfo {
        return WeatherInfo(
            city = "Sorocaba/SP",
            temperature = 24,
            condition = "Ensolarado",
            conditionIcon = "☀️",
            humidity = 68,
            windSpeed = 12,
            precipitation = 10,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun logD(message: String) = android.util.Log.d(TAG, message)
    private fun logE(message: String, throwable: Throwable? = null) = 
        android.util.Log.e(TAG, message, throwable)
}
