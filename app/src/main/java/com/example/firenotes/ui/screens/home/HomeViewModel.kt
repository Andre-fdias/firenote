package com.example.firenotes.ui.screens.home

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firenotes.domain.model.Ocorrencia
import com.example.firenotes.domain.repository.OcorrenciaRepository
import com.example.firenotes.domain.repository.LocationService
import com.example.firenotes.domain.repository.SettingsRepository
import com.example.firenotes.domain.repository.CalendarRepository
import com.example.firenotes.domain.model.EscalaConfig
import com.example.firenotes.domain.model.EquipeConfig
import com.example.firenotes.domain.calendar.ScaleEngine
import com.example.firenotes.data.local.dao.HomeOperationalDao
import com.example.firenotes.data.local.entities.RoomTarefa
import com.example.firenotes.data.local.entities.RoomTarefaComSubtarefas
import com.example.firenotes.data.local.entities.RoomEventoAgenda
import com.example.firenotes.data.local.entities.RoomEventoComLembretes
import com.example.firenotes.data.local.entities.RoomProntidaoDia
import com.example.firenotes.domain.model.SubtarefaInput
import com.example.firenotes.data.local.entities.RoomSubtarefa
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
    private val calendarRepository: CalendarRepository,
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

    private val _selectedEscalaFilter = MutableStateFlow<String?>(null) // null = Todas, "NONE" = Nenhuma
    val selectedEscalaFilter: StateFlow<String?> = _selectedEscalaFilter.asStateFlow()

    private val _availableEscalas = MutableStateFlow<List<EscalaConfig>>(emptyList())
    val availableEscalas: StateFlow<List<EscalaConfig>> = _availableEscalas.asStateFlow()

    private val _previewDays = MutableStateFlow<Map<LocalDate, Map<Int, List<EquipeConfig>>>>(emptyMap())
    val previewDays: StateFlow<Map<LocalDate, Map<Int, List<EquipeConfig>>>> = _previewDays.asStateFlow()

    fun setEscalaFilter(filter: String?) {
        viewModelScope.launch {
            settingsRepository.setActiveCalendarFilter(filter ?: "Todos")
        }
    }


    // UI STATES - CALENDÁRIO
    // ============================================

    private val _selectedDate = MutableStateFlow<LocalDate>(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _currentMonth = MutableStateFlow<LocalDate>(LocalDate.now().withDayOfMonth(1))
    val currentMonth: StateFlow<LocalDate> = _currentMonth.asStateFlow()

    // ============================================
    // UI STATES - DADOS LOCAIS (TAREFAS, EVENTOS, PRONTIDÃO)
    // ============================================

    private val _allTarefas = MutableStateFlow<List<RoomTarefaComSubtarefas>>(emptyList())
    val allTarefas: StateFlow<List<RoomTarefaComSubtarefas>> = _allTarefas.asStateFlow()

    private val _allEventos = MutableStateFlow<List<RoomEventoComLembretes>>(emptyList())
    val allEventos: StateFlow<List<RoomEventoComLembretes>> = _allEventos.asStateFlow()

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
        
        viewModelScope.launch {
            settingsRepository.activeCalendarFilterFlow.collect { filter ->
                _selectedEscalaFilter.value = if (filter == "Todos") null else filter
            }
        }

        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                _selectedEscalaFilter,
                _currentMonth,
                calendarRepository.getEscalasFlow(),
                calendarRepository.getEquipesFlow()
            ) { filter, month, scales, teams ->
                _availableEscalas.value = scales
                if (filter == "NONE") {
                    emptyMap()
                } else {
                    val filteredScales = if (filter == null) scales else scales.filter { it.id == filter }
                    val filteredTeams = if (filter == null) teams else teams.filter { it.escalaId == filter }
                    ScaleEngine.getPrecomputedMonthScales(month, filteredScales, filteredTeams)
                }
            }.collect {
                _previewDays.value = it
            }
        }
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
        hora: String? = null,
        lembretesMinutos: List<Int> = emptyList(),
        subtarefas: List<SubtarefaInput> = emptyList(),
        escalaId: String? = null,
        corHex: String = "#10B981"
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
                    hora = hora,
                    escalaId = escalaId,
                    cor = corHex
                )
                homeOperationalDao.insertTarefa(novaTarefa)
                
                if (subtarefas.isNotEmpty()) {
                    val roomSubtarefas = resolveParentIds(subtarefas, novaTarefa.id)
                    homeOperationalDao.insertSubtarefas(roomSubtarefas)
                }

                if (lembretesMinutos.isNotEmpty()) {
                    val roomLembretes = lembretesMinutos.map { min ->
                        com.example.firenotes.data.local.entities.RoomLembrete(
                            id = UUID.randomUUID().toString(),
                            referenciaId = novaTarefa.id,
                            tipoReferencia = "TAREFA",
                            minutosAntes = min
                        )
                    }
                    homeOperationalDao.insertLembretes(roomLembretes)
                }

                if (novaTarefa.hora != null) {
                    val lembretesParaAgendar = if (lembretesMinutos.isEmpty()) listOf(0) else lembretesMinutos
                    lembretesParaAgendar.forEach { min ->
                        com.example.firenotes.util.NotificationScheduler.schedule(
                            context,
                            novaTarefa.id,
                            "Tarefa Agendada",
                            "${novaTarefa.titulo} (${novaTarefa.categoria})",
                            novaTarefa.data,
                            novaTarefa.hora,
                            min
                        )
                    }
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
        tipo: com.example.firenotes.domain.model.TipoEvento? = null,
        local: String? = null,
        lembretesMinutos: List<Int> = emptyList(),
        escalaId: String? = null,
        corHex: String = "#3B82F6"
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
                    tipo = tipo?.name,
                    local = local,
                    escalaId = escalaId,
                    cor = corHex
                )
                homeOperationalDao.insertEvento(novoEvento)
                
                if (lembretesMinutos.isNotEmpty()) {
                    val roomLembretes = lembretesMinutos.map { min ->
                        com.example.firenotes.data.local.entities.RoomLembrete(
                            id = UUID.randomUUID().toString(),
                            referenciaId = novoEvento.id,
                            tipoReferencia = "EVENTO",
                            minutosAntes = min
                        )
                    }
                    homeOperationalDao.insertLembretes(roomLembretes)
                }
                if (novoEvento.horaInicio != null) {
                    val lembretesParaAgendar = if (lembretesMinutos.isEmpty()) listOf(0) else lembretesMinutos
                    lembretesParaAgendar.forEach { min ->
                        com.example.firenotes.util.NotificationScheduler.schedule(
                            context,
                            novoEvento.id,
                            "Evento Agendado",
                            "${novoEvento.titulo} (Inicio: ${novoEvento.horaInicio})",
                            novoEvento.data,
                            novoEvento.horaInicio,
                            min
                        )
                    }
                }
                Log.d(TAG, "✅ Evento adicionado: ${novoEvento.titulo}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro ao adicionar evento: ${e.message}", e)
            }
        }
    }

    fun updateTarefa(
        tarefa: com.example.firenotes.data.local.entities.RoomTarefa,
        lembretesMinutos: List<Int> = emptyList(),
        subtarefas: List<SubtarefaInput> = emptyList()
    ) {
        viewModelScope.launch {
            try {
                homeOperationalDao.updateTarefa(tarefa)
                
                homeOperationalDao.deleteSubtarefasByTarefa(tarefa.id)
                if (subtarefas.isNotEmpty()) {
                    val roomSubtarefas = resolveParentIds(subtarefas, tarefa.id)
                    homeOperationalDao.insertSubtarefas(roomSubtarefas)
                }

                homeOperationalDao.deleteLembretesByReferencia(tarefa.id)
                if (lembretesMinutos.isNotEmpty()) {
                    val roomLembretes = lembretesMinutos.map { min ->
                        com.example.firenotes.data.local.entities.RoomLembrete(
                            id = UUID.randomUUID().toString(),
                            referenciaId = tarefa.id,
                            tipoReferencia = "TAREFA",
                            minutosAntes = min
                        )
                    }
                    homeOperationalDao.insertLembretes(roomLembretes)
                }
                com.example.firenotes.util.NotificationScheduler.cancel(context, tarefa.id)
                if (!tarefa.concluida && tarefa.hora != null) {
                    val lembretesParaAgendar = if (lembretesMinutos.isEmpty()) listOf(0) else lembretesMinutos
                    lembretesParaAgendar.forEach { min ->
                        com.example.firenotes.util.NotificationScheduler.schedule(
                            context,
                            tarefa.id,
                            "Tarefa Agendada",
                            "${tarefa.titulo} (${tarefa.categoria})",
                            tarefa.data,
                            tarefa.hora,
                            min
                        )
                    }
                }
                Log.d(TAG, "✅ Tarefa atualizada: ${tarefa.titulo}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro ao atualizar tarefa: ${e.message}", e)
            }
        }
    }

    fun updateEvento(
        evento: com.example.firenotes.data.local.entities.RoomEventoAgenda,
        lembretesMinutos: List<Int> = emptyList()
    ) {
        viewModelScope.launch {
            try {
                homeOperationalDao.updateEvento(evento)
                
                homeOperationalDao.deleteLembretesByReferencia(evento.id)
                if (lembretesMinutos.isNotEmpty()) {
                    val roomLembretes = lembretesMinutos.map { min ->
                        com.example.firenotes.data.local.entities.RoomLembrete(
                            id = UUID.randomUUID().toString(),
                            referenciaId = evento.id,
                            tipoReferencia = "EVENTO",
                            minutosAntes = min
                        )
                    }
                    homeOperationalDao.insertLembretes(roomLembretes)
                }
                com.example.firenotes.util.NotificationScheduler.cancel(context, evento.id)
                if (evento.horaInicio != null) {
                    val lembretesParaAgendar = if (lembretesMinutos.isEmpty()) listOf(0) else lembretesMinutos
                    lembretesParaAgendar.forEach { min ->
                        com.example.firenotes.util.NotificationScheduler.schedule(
                            context,
                            evento.id,
                            "Evento Agendado",
                            "${evento.titulo} (Inicio: ${evento.horaInicio})",
                            evento.data,
                            evento.horaInicio,
                            min
                        )
                    }
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

    suspend fun getSubtarefasByTarefa(tarefaId: String): List<RoomSubtarefa> {
        return homeOperationalDao.getSubtarefasByTarefa(tarefaId)
    }

    suspend fun getLembretesByReferencia(referenciaId: String): List<Int> {
        return homeOperationalDao.getLembretesByReferencia(referenciaId)
            .map { it.minutosAntes }
    }

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

    fun toggleSubtarefa(subtarefa: RoomSubtarefa) {
        viewModelScope.launch {
            try {
                val updated = subtarefa.copy(concluida = !subtarefa.concluida)
                homeOperationalDao.updateSubtarefa(updated)
            } catch (e: Exception) {
                Log.e("HomeVM", "Erro ao toggle subtarefa: ${e.message}", e)
            }
        }
    }

    fun updateSubtarefaTitle(subtarefa: RoomSubtarefa, newTitle: String) {
        viewModelScope.launch {
            try {
                val updated = subtarefa.copy(titulo = newTitle)
                homeOperationalDao.updateSubtarefa(updated)
            } catch (e: Exception) {
                Log.e("HomeVM", "Erro ao atualizar titulo subtarefa: ${e.message}", e)
            }
        }
    }

    fun deleteSubtarefa(subtarefa: RoomSubtarefa) {
        viewModelScope.launch {
            try {
                homeOperationalDao.deleteSubtarefa(subtarefa)
            } catch (e: Exception) {
                Log.e("HomeVM", "Erro ao deletar subtarefa: ${e.message}", e)
            }
        }
    }

    private fun resolveParentIds(inputs: List<SubtarefaInput>, tarefaId: String): List<RoomSubtarefa> {
        val result = mutableListOf<RoomSubtarefa>()
        val activeParents = mutableMapOf<Int, String>()
        inputs.forEach { input ->
            val parentId = if (input.level > 0) activeParents[input.level - 1] else null
            val roomSub = RoomSubtarefa(
                id = input.id,
                tarefaId = tarefaId,
                titulo = input.titulo,
                concluida = input.concluida,
                parentId = parentId
            )
            result.add(roomSub)
            activeParents[input.level] = input.id
        }
        return result
    }

    // ============================================
    // COMPANION OBJECT
    // ============================================

    companion object {
        private const val TAG = "FireHomeViewModel"
    }
}