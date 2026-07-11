package com.example.firenotes.domain.repository

import com.example.firenotes.domain.model.WeatherInfo

interface WeatherService {
    suspend fun getCurrentWeather(lat: Double, lon: Double): WeatherInfo
    suspend fun getWeatherByCity(cityName: String): WeatherInfo
    suspend fun getCachedWeather(): WeatherInfo?
    suspend fun saveWeatherCache(info: WeatherInfo)
    suspend fun clearCache()
}
