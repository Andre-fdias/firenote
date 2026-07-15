package com.example.firenotes.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val themeFlow: Flow<String>
    val pinEnabledFlow: Flow<Boolean>
    val pinCodeFlow: Flow<String>
    val biometricEnabledFlow: Flow<Boolean>
    val lastCityFlow: Flow<String>
    val lastTempFlow: Flow<String>
    val lastUpdateFlow: Flow<Long>
    val languageFlow: Flow<String>
    val dateFormatFlow: Flow<String>
    val unitSystemFlow: Flow<String>

    suspend fun setTheme(theme: String)
    suspend fun setPinEnabled(enabled: Boolean)
    suspend fun setPinCode(pin: String)
    suspend fun setBiometricEnabled(enabled: Boolean)
    suspend fun saveWeatherCache(city: String, temp: String, timestamp: Long)
    suspend fun setLanguage(language: String)
    suspend fun setDateFormat(format: String)
    suspend fun setUnitSystem(system: String)
}
