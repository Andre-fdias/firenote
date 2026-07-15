package com.example.firenotes.ui.screens.home

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firenotes.domain.model.Ocorrencia
import com.example.firenotes.domain.model.WeatherInfo
import com.example.firenotes.domain.repository.OcorrenciaRepository
import com.example.firenotes.domain.repository.LocationService
import com.example.firenotes.domain.repository.SettingsRepository
import com.example.firenotes.domain.repository.WeatherService
import com.example.firenotes.data.local.dao.HomeOperationalDao
import com.example.firenotes.data.local.entities.RoomTarefa
import com.example.firenotes.data.local.entities.RoomEventoAgenda
import com.example.firenotes.data.local.entities.RoomProntidaoDia
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

// ============================================
// UI STATE SEALED CLASS
// ============================================

sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(val occurrences: List<Ocorrencia>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

// ============================================
// HOME VIEW MODEL
// ============================================

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val weatherService: WeatherService,
    private val locationService: LocationService,
    private val repository: OcorrenciaRepository,
    private val settingsRepository: SettingsRepository,
    private val backupService: com.example.firenotes.data.service.BackupService,
    private val googleDriveBackupService: com.example.firenotes.data.service.GoogleDriveBackupService,
    private val homeOperationalDao: HomeOperationalDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // ============================================
    // UI STATES - OCORRÊNCIAS
    // ============================================

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // ============================================
    // UI STATES - CLIMA
    // ============================================

    private val _weatherState = MutableStateFlow(WeatherUiState())
    val weatherState: StateFlow<WeatherUiState> = _weatherState.asStateFlow()

    private val _isLoadingWeather = MutableStateFlow(false)
    val isLoadingWeather: StateFlow<Boolean> = _isLoadingWeather.asStateFlow()

    // ============================================
    // UI STATES - REFRESH
    // ============================================

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // ============================================
    // UI STATES - CALENDÁRIO
    // ============================================

    private val _selectedDate = MutableStateFlow<LocalDate>(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _currentMonth = MutableStateFlow<LocalDate>(LocalDate.now().withDayOfMonth(1))
    val currentMonth: StateFlow<LocalDate> = _currentMonth.asStateFlow()

    // ============================================
    // UI STATES - DADOS LOCAIS (TAREFAS, EVENTOS, PRONTIDÃO)
    // ============================================

    private val _allTarefas = MutableStateFlow<List<RoomTarefa>>(emptyList())
    val allTarefas: StateFlow<List<RoomTarefa>> = _allTarefas.asStateFlow()

    private val _allEventos = MutableStateFlow<List<RoomEventoAgenda>>(emptyList())
    val allEventos: StateFlow<List<RoomEventoAgenda>> = _allEventos.asStateFlow()

    private val _allProntidoes = MutableStateFlow<List<RoomProntidaoDia>>(emptyList())
    val allProntidoes: StateFlow<List<RoomProntidaoDia>> = _allProntidoes.asStateFlow()

    // ============================================
    // CONSTANTES DE CACHE
    // ============================================

    private var lastWeatherFetchTime = 0L
    private val WEATHER_CACHE_DURATION = 5 * 60 * 1000L // 5 minutos

    // ============================================
    // INICIALIZAÇÃO
    // ============================================

    init {
        Log.d(TAG, "🚀 Inicializando HomeViewModel")
        loadOccurrences()
        triggerAutoBackups()
        loadWeather()
        observeHomeData()
    }

    // ============================================
    // OBSERVAÇÃO DE DADOS LOCAIS
    // ============================================

    private fun observeHomeData() {
        viewModelScope.launch {
            homeOperationalDao.getAllTarefasFlow().collect {
                _allTarefas.value = it
                Log.d(TAG, "📋 Tarefas atualizadas: ${it.size}")
            }
        }
        viewModelScope.launch {
            homeOperationalDao.getAllEventosFlow().collect {
                _allEventos.value = it
                Log.d(TAG, "📅 Eventos atualizados: ${it.size}")
            }
        }
        viewModelScope.launch {
            homeOperationalDao.getAllProntidoesFlow().collect {
                _allProntidoes.value = it
                Log.d(TAG, "🚒 Prontidões atualizadas: ${it.size}")
            }
        }
    }

    // ============================================
    // NAVEGAÇÃO DO CALENDÁRIO
    // ============================================

    fun selectDate(date: LocalDate) {
        Log.d(TAG, "📆 Data selecionada: $date")
        _selectedDate.value = date
    }

    fun nextMonth() {
        val newMonth = _currentMonth.value.plusMonths(1)
        Log.d(TAG, "📆 Próximo mês: $newMonth")
        _currentMonth.value = newMonth
    }

    fun previousMonth() {
        val newMonth = _currentMonth.value.minusMonths(1)
        Log.d(TAG, "📆 Mês anterior: $newMonth")
        _currentMonth.value = newMonth
    }

    // ============================================
    // CRUD - TAREFAS
    // ============================================

    fun addTarefa(
        titulo: String,
        data: LocalDate,
        descricao: String? = null,
        prioridade: Prioridade = Prioridade.MEDIA,
        categoria: String = "Operacional"
    ) {
        if (titulo.isBlank()) {
            Log.w(TAG, "⚠️ Tentativa de adicionar tarefa com título vazio")
            return
        }

        viewModelScope.launch {
            try {
                val novaTarefa = RoomTarefa(
                    id = UUID.randomUUID().toString(),
                    titulo = titulo.trim(),
                    descricao = descricao?.trim(),
                    concluida = false,
                    data = data.toString(),
                    categoria = categoria,
                    prioridade = prioridade.name,
                    criadoEm = System.currentTimeMillis(),
                    concluidoEm = null
                )
                homeOperationalDao.insertTarefa(novaTarefa)
                Log.d(TAG, "✅ Tarefa adicionada: ${novaTarefa.titulo}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro ao adicionar tarefa: ${e.message}", e)
            }
        }
    }

    fun toggleTarefa(tarefa: RoomTarefa) {
        viewModelScope.launch {
            try {
                val updated = tarefa.copy(
                    concluida = !tarefa.concluida,
                    concluidoEm = if (!tarefa.concluida) System.currentTimeMillis() else null
                )
                homeOperationalDao.updateTarefa(updated)
                Log.d(TAG, "🔄 Tarefa toggled: ${tarefa.titulo} -> ${updated.concluida}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro ao toggle tarefa: ${e.message}", e)
            }
        }
    }

    fun deleteTarefa(id: String) {
        viewModelScope.launch {
            try {
                homeOperationalDao.deleteTarefa(id)
                Log.d(TAG, "🗑️ Tarefa deletada: $id")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro ao deletar tarefa: ${e.message}", e)
            }
        }
    }

    // ============================================
    // CRUD - EVENTOS
    // ============================================

    fun addEvento(
        titulo: String,
        descricao: String?,
        data: LocalDate,
        horaInicio: String?,
        horaFim: String?,
        tipo: com.example.firenotes.ui.screens.agenda.TipoEvento? = null
    ) {
        if (titulo.isBlank()) {
            Log.w(TAG, "⚠️ Tentativa de adicionar evento com título vazio")
            return
        }

        viewModelScope.launch {
            try {
                val novoEvento = com.example.firenotes.data.local.entities.RoomEventoAgenda(
                    id = UUID.randomUUID().toString(),
                    titulo = titulo.trim(),
                    descricao = descricao?.trim(),
                    data = data.toString(),
                    horaInicio = horaInicio,
                    horaFim = horaFim,
                    tipo = tipo?.name
                )
                homeOperationalDao.insertEvento(novoEvento)
                Log.d(TAG, "✅ Evento adicionado: ${novoEvento.titulo}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro ao adicionar evento: ${e.message}", e)
            }
        }
    }

    fun updateTarefa(tarefa: com.example.firenotes.data.local.entities.RoomTarefa) {
        viewModelScope.launch {
            try {
                homeOperationalDao.updateTarefa(tarefa)
                Log.d(TAG, "✅ Tarefa atualizada: ${tarefa.titulo}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro ao atualizar tarefa: ${e.message}", e)
            }
        }
    }

    fun updateEvento(evento: com.example.firenotes.data.local.entities.RoomEventoAgenda) {
        viewModelScope.launch {
            try {
                homeOperationalDao.updateEvento(evento)
                Log.d(TAG, "✅ Evento atualizado: ${evento.titulo}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro ao atualizar evento: ${e.message}", e)
            }
        }
    }

    fun deleteEvento(id: String) {
        viewModelScope.launch {
            try {
                homeOperationalDao.deleteEvento(id)
                Log.d(TAG, "🗑️ Evento deletado: $id")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro ao deletar evento: ${e.message}", e)
            }
        }
    }

    // ============================================
    // CRUD - PRONTIDÃO
    // ============================================

    fun setProntidaoDia(data: LocalDate, escala: String) {
        viewModelScope.launch {
            try {
                homeOperationalDao.insertProntidao(RoomProntidaoDia(data.toString(), escala))
                Log.d(TAG, "🚒 Prontidão definida: $data -> $escala")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro ao definir prontidão: ${e.message}", e)
            }
        }
    }

    // ============================================
    // REFRESH
    // ============================================

    fun refreshAll() {
        Log.d(TAG, "🔄 Refreshing all data...")
        viewModelScope.launch {
            _isRefreshing.value = true
            loadOccurrences()
            loadWeather(forceRefresh = true)
            kotlinx.coroutines.delay(1000)
            _isRefreshing.value = false
            Log.d(TAG, "✅ Refresh completo")
        }
    }

    // ============================================
    // CARREGAMENTO DE OCORRÊNCIAS
    // ============================================

    fun loadOccurrences() {
        Log.d(TAG, "📥 Carregando ocorrências...")
        _uiState.value = HomeUiState.Loading

        viewModelScope.launch {
            repository.getOcorrencias()
                .catch { exception ->
                    val msg = exception.message ?: "Erro desconhecido ao carregar ocorrências"
                    Log.e(TAG, "❌ Erro ao carregar ocorrências: $msg", exception)
                    _uiState.value = HomeUiState.Error(msg)
                }
                .collect { occurrences ->
                    val sorted = occurrences.sortedByDescending { it.dataHora }
                    Log.d(TAG, "✅ Ocorrências carregadas: ${sorted.size}")
                    _uiState.value = HomeUiState.Success(sorted)
                }
        }
    }

    // ============================================
    // CARREGAMENTO DE CLIMA
    // ============================================

    fun loadWeather(forceRefresh: Boolean = false) {
        val now = System.currentTimeMillis()

        if (!forceRefresh && now - lastWeatherFetchTime < WEATHER_CACHE_DURATION) {
            Log.d(TAG, "⏳ Usando cache de clima (${(now - lastWeatherFetchTime) / 1000}s atrás)")
            return
        }

        Log.d(TAG, "🌤️ Carregando clima... (forceRefresh=$forceRefresh)")

        viewModelScope.launch {
            _isLoadingWeather.value = true
            lastWeatherFetchTime = now

            try {
                // 1. Tentar cache primeiro
                val cached = weatherService.getCachedWeather()
                if (cached != null && !forceRefresh) {
                    Log.d(TAG, "✅ Cache carregado: ${cached.city}")
                    _weatherState.value = WeatherUiState.fromWeatherInfo(cached)
                }

                // 2. Tentar localização
                val locationResult = locationService.getCurrentLocation()
                locationResult.onSuccess { (lat, lon) ->
                    Log.d(TAG, "📍 Localização obtida: lat=$lat, lon=$lon")
                    fetchWeather(lat, lon)
                }.onFailure { error ->
                    Log.e(TAG, "❌ Erro ao obter localização: ${error.message}")

                    // 3. Tentar usar última cidade salva
                    val lastCity = settingsRepository.lastCityFlow.first()
                    if (lastCity.isNotBlank()) {
                        Log.d(TAG, "📍 Usando última cidade: $lastCity")
                        fetchWeatherByCity(lastCity)
                    } else {
                        // 4. Fallback para dados mockados
                        useMockWeather()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro ao carregar clima: ${e.message}", e)
                useMockWeather()
            } finally {
                _isLoadingWeather.value = false
            }
        }
    }

    fun fetchWeatherForCity(cityName: String) {
        if (cityName.isBlank()) {
            Log.w(TAG, "⚠️ Tentativa de buscar clima com cidade vazia")
            return
        }

        Log.d(TAG, "🔍 Buscando clima para cidade: $cityName")

        viewModelScope.launch {
            _isLoadingWeather.value = true
            try {
                fetchWeatherByCity(cityName)
                lastWeatherFetchTime = System.currentTimeMillis()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro ao buscar clima por cidade: ${e.message}", e)
                useMockWeather()
            } finally {
                _isLoadingWeather.value = false
            }
        }
    }

    // ============================================
    // MÉTODOS PRIVADOS DE CLIMA
    // ============================================

    private suspend fun fetchWeather(lat: Double, lon: Double) {
        try {
            val weather = weatherService.getCurrentWeather(lat, lon)
            _weatherState.value = WeatherUiState.fromWeatherInfo(weather)
            Log.d(TAG, "✅ Clima atualizado: ${weather.city} - ${weather.temperature}°C")
            settingsRepository.saveWeatherCache(weather.city, "", System.currentTimeMillis())
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao buscar clima por coordenadas: ${e.message}", e)

            // Tentar fallback por cidade se falhar
            val lastCity = settingsRepository.lastCityFlow.first()
            if (lastCity.isNotBlank()) {
                Log.d(TAG, "📍 Fallback para última cidade: $lastCity")
                fetchWeatherByCity(lastCity)
            } else {
                useMockWeather()
            }
        }
    }

    private suspend fun fetchWeatherByCity(cityName: String) {
        try {
            val weather = weatherService.getWeatherByCity(cityName)
            _weatherState.value = WeatherUiState.fromWeatherInfo(weather)
            Log.d(TAG, "✅ Clima atualizado para cidade: ${weather.city} - ${weather.temperature}°C")
            settingsRepository.saveWeatherCache(weather.city, "", System.currentTimeMillis())
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao buscar clima por cidade '$cityName': ${e.message}", e)
            useMockWeather()
        }
    }

    private fun useMockWeather() {
        Log.d(TAG, "📊 Usando dados mockados de clima")

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
    }

    // ============================================
    // BACKUP AUTOMÁTICO
    // ============================================

    private fun triggerAutoBackups() {
        Log.d(TAG, "💾 Iniciando backup automático...")

        viewModelScope.launch {
            // Backup local
            runCatching {
                backupService.checkAndTriggerAutoBackup()
                Log.d(TAG, "✅ Backup local concluído")
            }.onFailure { e ->
                Log.e(TAG, "❌ Erro no backup local: ${e.message}", e)
            }

            // Backup Google Drive
            runCatching {
                val account = googleDriveBackupService.getLastSignedInAccount()
                if (account != null) {
                    Log.d(TAG, "📤 Conta Google Drive encontrada: ${account.account}")
                    val token = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        com.google.android.gms.auth.GoogleAuthUtil.getToken(
                            context,
                            account.account ?: throw IllegalStateException("Conta sem e-mail do sistema"),
                            "oauth2:https://www.googleapis.com/auth/drive.appdata"
                        )
                    }
                    googleDriveBackupService.checkAndTriggerAutoDriveBackup(token)
                    Log.d(TAG, "✅ Backup Google Drive concluído")
                } else {
                    Log.d(TAG, "ℹ️ Nenhuma conta Google Drive encontrada")
                }
            }.onFailure { e ->
                Log.e(TAG, "❌ Erro no backup Google Drive: ${e.message}", e)
            }
        }
    }

    fun deleteOccurrence(id: String) {
        viewModelScope.launch {
            repository.deleteOcorrencia(id).onSuccess {
                loadOccurrences()
            }.onFailure {
                Log.e("HomeVM", "Erro ao excluir ocorrência: ${it.message}", it)
            }
        }
    }

    fun duplicateOccurrence(ocorrencia: Ocorrencia, onDuplicated: (Ocorrencia) -> Unit) {
        viewModelScope.launch {
            val duplicated = ocorrencia.copy(
                id = java.util.UUID.randomUUID().toString(),
                protocolo = ocorrencia.protocolo + " (Cópia)",
                dataHora = java.time.Instant.now()
            )
            repository.createOcorrencia(duplicated).onSuccess { saved ->
                loadOccurrences()
                onDuplicated(saved)
            }.onFailure {
                Log.e("HomeVM", "Erro ao duplicar ocorrência: ${it.message}", it)
            }
        }
    }

    // ============================================
    // COMPANION OBJECT
    // ============================================

    companion object {
        private const val TAG = "FireHomeViewModel"
    }
}