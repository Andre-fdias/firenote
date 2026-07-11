package com.example.firenotes.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.example.firenotes.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    companion object {
        val KEY_THEME = stringPreferencesKey("theme")
        val KEY_PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val KEY_PIN_CODE = stringPreferencesKey("pin_code")
        val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        
        val KEY_LAST_CITY = stringPreferencesKey("ultima_cidade")
        val KEY_LAST_TEMP = stringPreferencesKey("ultima_temperatura")
        val KEY_LAST_UPDATE = longPreferencesKey("ultima_atualizacao")
    }

    override val themeFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_THEME] ?: "Automático"
    }

    override val pinEnabledFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_PIN_ENABLED] ?: false
    }

    override val pinCodeFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_PIN_CODE] ?: ""
    }

    override val biometricEnabledFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_BIOMETRIC_ENABLED] ?: false
    }

    override val lastCityFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_LAST_CITY] ?: "Sorocaba/SP"
    }

    override val lastTempFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_LAST_TEMP] ?: "24°C"
    }

    override val lastUpdateFlow: Flow<Long> = dataStore.data.map { preferences ->
        preferences[KEY_LAST_UPDATE] ?: 0L
    }

    override suspend fun setTheme(theme: String) {
        dataStore.edit { preferences ->
            preferences[KEY_THEME] = theme
        }
    }

    override suspend fun setPinEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_PIN_ENABLED] = enabled
        }
    }

    override suspend fun setPinCode(pin: String) {
        dataStore.edit { preferences ->
            preferences[KEY_PIN_CODE] = pin
        }
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_BIOMETRIC_ENABLED] = enabled
        }
    }

    override suspend fun saveWeatherCache(city: String, temp: String, timestamp: Long) {
        android.util.Log.d("FireTheme", "DataStore - Salvando cache de Clima: Cidade=$city, Temp=$temp, Timestamp=$timestamp")
        dataStore.edit { preferences ->
            preferences[KEY_LAST_CITY] = city
            preferences[KEY_LAST_TEMP] = temp
            preferences[KEY_LAST_UPDATE] = timestamp
        }
    }
}
