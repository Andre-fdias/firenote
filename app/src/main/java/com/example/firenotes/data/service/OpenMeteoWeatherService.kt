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
import java.util.Locale
import javax.inject.Inject

class OpenMeteoWeatherService @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context
) : WeatherService {

    companion object {
        private const val CACHE_EXPIRY_MINUTES = 30L
        private const val TAG = "FireWeather"
        private const val BASE_URL = "https://api.open-meteo.com/v1/forecast"
        private const val FALLBACK_URL = "https://wttr.in/{city}?format=%C+%t+%h+%w"

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
        logD("🔄 Buscando dados da API para lat=$lat, lon=$lon")
        
        try {
            val url = buildUrl(lat, lon)
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
        logD("🔍 Buscando clima para cidade: $cityName")
        
        try {
            // Usar geocoding para obter coordenadas
            val coords = geocodeCity(cityName)
            if (coords != null) {
                return getCurrentWeather(coords.first, coords.second)
            }
        } catch (e: Exception) {
            logE("❌ Erro ao geocodificar cidade: ${e.message}")
        }
        
        // Fallback: usar wttr.in
        try {
            val url = FALLBACK_URL.replace("{city}", cityName)
            val response = fetchWeatherData(url)
            return parseWttrResponse(response, cityName)
        } catch (e: Exception) {
            logE("❌ Erro ao buscar clima via wttr.in: ${e.message}")
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

    private fun buildUrl(lat: Double, lon: Double): String {
        return "$BASE_URL?latitude=$lat&longitude=$lon&" +
                "current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m&" +
                "hourly=precipitation_probability&" +
                "timezone=America/Sao_Paulo&" +
                "forecast_days=1"
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

    private suspend fun parseWeatherResponse(json: String, lat: Double, lon: Double): WeatherInfo {
        val root = Json.parseToJsonElement(json).jsonObject
        
        // Extrair dados
        val temp = root["current"]?.jsonObject?.get("temperature_2m")?.jsonPrimitive?.content?.toDoubleOrNull() ?: 24.0
        val humidity = root["current"]?.jsonObject?.get("relative_humidity_2m")?.jsonPrimitive?.content?.toIntOrNull() ?: 68
        val wind = root["current"]?.jsonObject?.get("wind_speed_10m")?.jsonPrimitive?.content?.toDoubleOrNull() ?: 12.0
        val weatherCode = root["current"]?.jsonObject?.get("weather_code")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        
        // Precipitação
        val precipProb = root["hourly"]?.jsonObject?.get("precipitation_probability")?.jsonArray
            ?.firstOrNull()?.jsonPrimitive?.content?.toIntOrNull() ?: 0

        // Geocodificar coordenadas para obter cidade
        val cityName = geocodeReverse(lat, lon) ?: "Localização desconhecida"

        // Mapear weather code para condição
        val (condition, icon) = mapWeatherCode(weatherCode)

        return WeatherInfo(
            city = cityName,
            temperature = temp.toInt(),
            condition = condition,
            conditionIcon = icon,
            humidity = humidity,
            windSpeed = wind.toInt(),
            precipitation = precipProb,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun parseWttrResponse(response: String, cityName: String): WeatherInfo {
        // Parse simples do wttr.in
        val parts = response.split("+")
        val condition = parts.getOrNull(0)?.trim() ?: "Ensolarado"
        val tempStr = parts.getOrNull(1)?.replace("°C", "")?.trim() ?: "24"
        val humidityStr = parts.getOrNull(2)?.replace("%", "")?.trim() ?: "68"
        val windStr = parts.getOrNull(3)?.replace("km/h", "")?.trim() ?: "12"

        return WeatherInfo(
            city = cityName,
            temperature = tempStr.toIntOrNull() ?: 24,
            condition = condition,
            conditionIcon = getConditionIcon(condition),
            humidity = humidityStr.toIntOrNull() ?: 68,
            windSpeed = windStr.toIntOrNull() ?: 12,
            precipitation = 0,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun mapWeatherCode(code: Int): Pair<String, String> {
        return when (code) {
            0 -> Pair("Céu limpo", "☀️")
            1, 2, 3 -> Pair("Parcialmente nublado", "⛅")
            45, 48 -> Pair("Nevoeiro", "🌫️")
            51, 53, 55 -> Pair("Garoa", "🌧️")
            61, 63, 65 -> Pair("Chuva", "🌧️")
            71, 73, 75 -> Pair("Neve", "❄️")
            80, 81, 82 -> Pair("Pancadas de chuva", "🌧️")
            95, 96, 99 -> Pair("Tempestade", "⚡")
            else -> Pair("Tempo firme", "☀️")
        }
    }

    private fun getConditionIcon(condition: String): String {
        return when {
            condition.contains("sun", ignoreCase = true) -> "☀️"
            condition.contains("rain", ignoreCase = true) -> "🌧️"
            condition.contains("cloud", ignoreCase = true) -> "⛅"
            condition.contains("snow", ignoreCase = true) -> "❄️"
            condition.contains("storm", ignoreCase = true) -> "⚡"
            else -> "☀️"
        }
    }

    private suspend fun geocodeCity(cityName: String): Pair<Double, Double>? {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(cityName, 1)
                addresses?.firstOrNull()?.let { address ->
                    address.latitude to address.longitude
                }
            } catch (e: Exception) {
                logE("❌ Erro no geocoding: ${e.message}")
                null
            }
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
