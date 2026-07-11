package com.example.firenotes.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class WeatherInfo(
    val city: String,
    val temperature: Int,
    val condition: String,
    val conditionIcon: String,
    val humidity: Int,
    val windSpeed: Int,
    val precipitation: Int,
    val timestamp: Long = System.currentTimeMillis()
)
