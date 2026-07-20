package com.example.firenotes.ui.screens.home

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firenotes.domain.model.Ocorrencia
import com.example.firenotes.domain.repository.OcorrenciaRepository
import com.example.firenotes.domain.repository.LocationService
import com.example.firenotes.domain.repository.SettingsRepository
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

    private val _currentCity = MutableStateFlow<String>("...")
    val currentCity: StateFlow<String> = _currentCity.asStateFlow()

    private val _hasDismissedAlertsThisSession = MutableStateFlow(false)
    val hasDismissedAlertsThisSession: StateFlow<Boolean> = _hasDismissedAlertsThisSession.asStateFlow()

    fun dismissAlertsPermanently() {
        _hasDismissedAlertsThisSession.value = true
    }

    // ============================================
    // INICIALIZAÇÃO
    // ============================================

    init {
        Log.d(TAG, "🚀 Inicializando HomeViewModel")
        loadOccurrences()
        triggerAutoBackups()
        observeHomeData()
        fetchCity()
    }

    fun fetchCity() {
        viewModelScope.launch {
            try {
                if (locationService.checkPermissions()) {
                    locationService.getCurrentLocation().onSuccess { coords ->
                        locationService.getAddressFromLocation(coords.first, coords.second).onSuccess { address ->
                            if (address.cidade.isNotEmpty()) {
                                _currentCity.value = address.cidade
                            } else {
                                _currentCity.value = "Local não informado"
                            }
                        }.onFailure {
                            _currentCity.value = "Local não informado"
                        }
                    }.onFailure {
                        _currentCity.value = "Local não informado"
                    }
                } else {
                    _currentCity.value = "Local não informado"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao buscar cidade: ${e.message}")
                _currentCity.value = "Local não informado"
            }
        }
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
        categoria: String = "Operacional",
        hora: String? = null
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
                    concluidoEm = null,
                    hora = hora
                )
                homeOperationalDao.insertTarefa(novaTarefa)
                if (novaTarefa.hora != null) {
                    com.example.firenotes.util.NotificationScheduler.schedule(
                        context,
                        novaTarefa.id,
                        "Tarefa Agendada",
                        "${novaTarefa.titulo} (${novaTarefa.categoria})",
                        novaTarefa.data,
                        novaTarefa.hora
                    )
                }
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
                if (updated.concluida) {
                    com.example.firenotes.util.NotificationScheduler.cancel(context, updated.id)
                } else if (updated.hora != null) {
                    com.example.firenotes.util.NotificationScheduler.schedule(
                        context,
                        updated.id,
                        "Tarefa Agendada",
                        "${updated.titulo} (${updated.categoria})",
                        updated.data,
                        updated.hora
                    )
                }
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
                com.example.firenotes.util.NotificationScheduler.cancel(context, id)
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
                if (novoEvento.horaInicio != null) {
                    com.example.firenotes.util.NotificationScheduler.schedule(
                        context,
                        novoEvento.id,
                        "Evento Agendado",
                        "${novoEvento.titulo} (Inicio: ${novoEvento.horaInicio})",
                        novoEvento.data,
                        novoEvento.horaInicio
                    )
                }
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
                com.example.firenotes.util.NotificationScheduler.cancel(context, tarefa.id)
                if (!tarefa.concluida && tarefa.hora != null) {
                    com.example.firenotes.util.NotificationScheduler.schedule(
                        context,
                        tarefa.id,
                        "Tarefa Agendada",
                        "${tarefa.titulo} (${tarefa.categoria})",
                        tarefa.data,
                        tarefa.hora
                    )
                }
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
                com.example.firenotes.util.NotificationScheduler.cancel(context, evento.id)
                if (evento.horaInicio != null) {
                    com.example.firenotes.util.NotificationScheduler.schedule(
                        context,
                        evento.id,
                        "Evento Agendado",
                        "${evento.titulo} (Inicio: ${evento.horaInicio})",
                        evento.data,
                        evento.horaInicio
                    )
                }
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
                com.example.firenotes.util.NotificationScheduler.cancel(context, id)
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