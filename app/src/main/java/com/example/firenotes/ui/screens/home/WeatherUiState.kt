package com.example.firenotes.ui.screens.home

import com.example.firenotes.domain.model.WeatherInfo
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class WeatherUiState(
    val city: String = "Carregando...",
    val condition: String = "Carregando...",
    val conditionIcon: String = "⏳",
    val temperature: String = "--°C",
    val humidity: String = "💧 --%",
    val windSpeed: String = "🌬️ -- km/h",
    val rainChance: String = "🌧️ --%",
    val isUpdating: Boolean = false,
    val error: String? = null,
    val lastUpdated: Long = 0L
) {
    companion object {
        fun fromWeatherInfo(info: WeatherInfo): WeatherUiState {
            return WeatherUiState(
                city = info.city,
                condition = info.condition,
                conditionIcon = info.conditionIcon,
                temperature = "${info.temperature}°C",
                humidity = "💧 ${info.humidity}%",
                windSpeed = "🌬️ ${info.windSpeed} km/h",
                rainChance = "🌧️ ${info.precipitation}%",
                isUpdating = false,
                error = null,
                lastUpdated = info.timestamp
            )
        }

        fun error(message: String): WeatherUiState {
            return WeatherUiState(
                error = message,
                isUpdating = false
            )
        }
    }

    fun isStale(): Boolean {
        val now = System.currentTimeMillis()
        return lastUpdated > 0 && (now - lastUpdated) > 30 * 60 * 1000 // 30 minutos
    }

    fun getTimeAgo(): String {
        if (lastUpdated == 0L) return "Nunca atualizado"
        val diff = System.currentTimeMillis() - lastUpdated
        return when {
            diff < 60_000 -> "Agora"
            diff < 3_600_000 -> {
                val mins = diff / 60_000
                if (mins == 1L) "Há 1 min" else "Há $mins min"
            }
            else -> {
                // Mostra o horário local da última atualização
                val localTime = Instant.ofEpochMilli(lastUpdated)
                    .atZone(ZoneId.systemDefault())
                    .toLocalTime()
                    .format(DateTimeFormatter.ofPattern("HH:mm"))
                "Atualizado $localTime"
            }
        }
    }
}