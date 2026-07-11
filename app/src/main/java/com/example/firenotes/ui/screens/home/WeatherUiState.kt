package com.example.firenotes.ui.screens.home

import com.example.firenotes.domain.model.WeatherInfo

data class WeatherUiState(
    val city: String = "Carregando...",
    val condition: String = "Carregando...",
    val conditionIcon: String = "⏳",
    val temperature: String = "--°C",
    val humidity: String = "💧 --%",
    val windSpeed: String = "🌬️ -- km/h",
    val rainChance: String = "🌧️ --%",
    val isUpdating: Boolean = false,
    val error: String? = null
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
                error = null
            )
        }
    }
}
