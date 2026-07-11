package com.example.firenotes.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firenotes.domain.model.Ocorrencia
import com.example.firenotes.domain.model.WeatherInfo
import com.example.firenotes.domain.repository.OcorrenciaRepository
import com.example.firenotes.domain.repository.LocationService
import com.example.firenotes.domain.repository.SettingsRepository
import com.example.firenotes.domain.repository.WeatherService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(val occurrences: List<Ocorrencia>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val weatherService: WeatherService,
    private val locationService: LocationService,
    private val repository: OcorrenciaRepository,
    private val settingsRepository: SettingsRepository,
    private val backupService: com.example.firenotes.data.service.BackupService,
    private val googleDriveBackupService: com.example.firenotes.data.service.GoogleDriveBackupService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _weatherState = MutableStateFlow(WeatherUiState())
    val weatherState: StateFlow<WeatherUiState> = _weatherState.asStateFlow()

    private val _isLoadingWeather = MutableStateFlow(false)
    val isLoadingWeather: StateFlow<Boolean> = _isLoadingWeather.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadOccurrences()
        triggerAutoBackups()
        loadWeather()
    }

    fun refreshAll() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadOccurrences()
            loadWeather()
            kotlinx.coroutines.delay(1000)
            _isRefreshing.value = false
        }
    }

    fun loadOccurrences() {
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch {
            repository.getOcorrencias()
                .catch { exception ->
                    _uiState.value = HomeUiState.Error(exception.message ?: "Erro desconhecido ao carregar ocorrências")
                }
                .collect { occurrences ->
                    _uiState.value = HomeUiState.Success(occurrences.sortedByDescending { it.dataHora })
                }
        }
    }

    fun loadWeather() {
        viewModelScope.launch {
            _isLoadingWeather.value = true
            
            try {
                // 1. Tentar cache primeiro
                val cached = weatherService.getCachedWeather()
                if (cached != null) {
                    logD("✅ Cache carregado: ${cached.city}")
                    _weatherState.value = WeatherUiState.fromWeatherInfo(cached)
                }

                // 2. Tentar localização
                val locationResult = locationService.getCurrentLocation()
                locationResult.onSuccess { (lat, lon) ->
                    logD("📍 Localização obtida: lat=$lat, lon=$lon")
                    fetchWeather(lat, lon)
                }.onFailure { error ->
                    logE("❌ Erro ao obter localização: ${error.message}")
                    
                    // Tentar usar última cidade salva
                    val lastCity = settingsRepository.lastCityFlow.first()
                    if (lastCity.isNotBlank()) {
                        logD("📍 Usando última cidade: $lastCity")
                        fetchWeatherByCity(lastCity)
                    } else {
                        // Fallback para dados mockados
                        useMockWeather()
                    }
                }
            } catch (e: Exception) {
                logE("❌ Erro ao carregar clima: ${e.message}")
                useMockWeather()
            } finally {
                _isLoadingWeather.value = false
            }
        }
    }

    fun fetchWeatherForCity(cityName: String) {
        if (cityName.isBlank()) return
        viewModelScope.launch {
            _isLoadingWeather.value = true
            try {
                fetchWeatherByCity(cityName)
            } catch (e: Exception) {
                logE("❌ Erro ao buscar clima por cidade: ${e.message}")
                useMockWeather()
            } finally {
                _isLoadingWeather.value = false
            }
        }
    }

    private suspend fun fetchWeather(lat: Double, lon: Double) {
        try {
            val weather = weatherService.getCurrentWeather(lat, lon)
            _weatherState.value = WeatherUiState.fromWeatherInfo(weather)
            logD("✅ Clima atualizado: ${weather.city} - ${weather.temperature}°C")
            // Save the last city so that on subsequent app launches we can fallback to it
            settingsRepository.saveWeatherCache(weather.city, "", System.currentTimeMillis())
        } catch (e: Exception) {
            logE("❌ Erro ao buscar clima: ${e.message}")
            useMockWeather()
        }
    }

    private suspend fun fetchWeatherByCity(cityName: String) {
        try {
            val weather = weatherService.getWeatherByCity(cityName)
            _weatherState.value = WeatherUiState.fromWeatherInfo(weather)
            logD("✅ Clima atualizado para cidade: ${weather.city}")
            // Save the last city so that on subsequent app launches we can fallback to it
            settingsRepository.saveWeatherCache(weather.city, "", System.currentTimeMillis())
        } catch (e: Exception) {
            logE("❌ Erro ao buscar clima por cidade: ${e.message}")
            useMockWeather()
        }
    }

    private fun useMockWeather() {
        val mock = WeatherInfo(
            city = "Sorocaba/SP",
            temperature = 24,
            condition = "Ensolarado",
            conditionIcon = "☀️",
            humidity = 68,
            windSpeed = 12,
            precipitation = 10,
            timestamp = System.currentTimeMillis()
        )
        _weatherState.value = WeatherUiState.fromWeatherInfo(mock)
        logD("✅ Usando dados mockados")
    }

    private fun triggerAutoBackups() {
        viewModelScope.launch {
            runCatching { backupService.checkAndTriggerAutoBackup() }

            runCatching {
                val account = googleDriveBackupService.getLastSignedInAccount()
                if (account != null) {
                    val token = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        com.google.android.gms.auth.GoogleAuthUtil.getToken(
                            context,
                            account.account ?: throw IllegalStateException("Conta sem e-mail do sistema"),
                            "oauth2:https://www.googleapis.com/auth/drive.appdata"
                        )
                    }
                    googleDriveBackupService.checkAndTriggerAutoDriveBackup(token)
                }
            }
        }
    }

    private fun logD(message: String) = android.util.Log.d("FireHome", message)
    private fun logE(message: String, throwable: Throwable? = null) = 
        android.util.Log.e("FireHome", message, throwable)
}
