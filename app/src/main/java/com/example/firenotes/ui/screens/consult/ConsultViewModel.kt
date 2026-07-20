package com.example.firenotes.ui.screens.consult

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firenotes.domain.model.*
import com.example.firenotes.domain.repository.OcorrenciaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class ConsultFilters(
    val talao: String = "",
    val dataInicial: String = "",
    val dataFinal: String = "",
    val natureza: NaturezaOcorrencia? = null,
    val cidade: String = "",
    val bairro: String = "",
    val viatura: String = "",
    val militar: String = "",
    val placa: String = "",
    val cpf: String = "",
    val nome: String = "",
    val envolvido: String = "",
    val hospital: String = "",
    val status: String = "", // Aberto, Encerrado
    val usuario: String = "",
    val dataFiltro: String = ""
)

enum class ConsultSort {
    RECENTES,
    ANTIGAS,
    TALAO,
    VITIMAS,
    VEICULOS
}

data class ConsultUiState(
    val occurrences: List<Ocorrencia> = emptyList(),
    val filteredOccurrences: List<Ocorrencia> = emptyList(),
    val searchGlobal: String = "",
    val filters: ConsultFilters = ConsultFilters(),
    val sortBy: ConsultSort = ConsultSort.RECENTES,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedOccurrence: Ocorrencia? = null,
    val showDetailsDialog: Boolean = false
)

@HiltViewModel
class ConsultViewModel @Inject constructor(
    private val repository: OcorrenciaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConsultUiState())
    val uiState: StateFlow<ConsultUiState> = _uiState.asStateFlow()

    init {
        loadOccurrences()
    }

    fun loadOccurrences() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            repository.getOcorrencias()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
                }
                .collect { list ->
                    // Use pre-fetched list directly to avoid redundant N+1 queries
                    _uiState.update { state ->
                        state.copy(
                            occurrences = list,
                            isLoading = false
                        )
                    }
                    applyFiltersAndSort()
                }
        }
    }

    fun updateSearchGlobal(query: String) {
        _uiState.update { it.copy(searchGlobal = query) }
        applyFiltersAndSort()
    }

    fun updateFilters(filters: ConsultFilters) {
        _uiState.update { it.copy(filters = filters) }
        applyFiltersAndSort()
    }

    fun updateSort(sort: ConsultSort) {
        _uiState.update { it.copy(sortBy = sort) }
        applyFiltersAndSort()
    }

    fun selectOccurrence(occurrence: Ocorrencia) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getOcorrenciaById(occurrence.id ?: "").fold(
                onSuccess = { fullOcorrencia ->
                    _uiState.update { it.copy(selectedOccurrence = fullOcorrencia, showDetailsDialog = true, isLoading = false) }
                },
                onFailure = {
                    _uiState.update { it.copy(selectedOccurrence = occurrence, showDetailsDialog = true, isLoading = false) }
                }
            )
        }
    }

    fun dismissDetails() {
        _uiState.update { it.copy(showDetailsDialog = false, selectedOccurrence = null) }
    }

    fun deleteOccurrence(id: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            repository.deleteOcorrencia(id).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, showDetailsDialog = false, selectedOccurrence = null) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Falha ao deletar ocorrência: ${e.localizedMessage}") }
                }
            )
        }
    }

    fun duplicateOccurrence(occurrence: Ocorrencia, onDuplicated: (Ocorrencia) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getOcorrenciaById(occurrence.id ?: "").onSuccess { full ->
                // Create a duplicate draft without talão (protocolo), data, hora
                val clone = full.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    protocolo = "", // Clear talão
                    dataHora = java.time.Instant.now(),
                    viaturas = emptyList(),
                    vitimas = emptyList(),
                    veiculos = emptyList()
                )
                repository.createOcorrencia(clone).onSuccess { saved ->
                    val newOcorrenciaId = saved.id!!
                    
                    // Duplicate all related items
                    full.viaturas.forEach { v ->
                        repository.addViatura(v.copy(id = java.util.UUID.randomUUID().toString(), ocorrenciaId = newOcorrenciaId)).onSuccess { savedViatura ->
                            val newViaturaId = savedViatura.id!!
                            v.equipe.forEach { m ->
                                repository.addMilitar(m.copy(id = java.util.UUID.randomUUID().toString(), viaturaId = newViaturaId))
                            }
                        }
                    }

                    full.veiculos.forEach { vc ->
                        repository.addVeiculoEnvolvido(vc.copy(id = java.util.UUID.randomUUID().toString(), ocorrenciaId = newOcorrenciaId))
                    }

                    full.vitimas.forEach { vt ->
                        repository.addVitima(vt.copy(id = java.util.UUID.randomUUID().toString(), ocorrenciaId = newOcorrenciaId))
                    }

                    repository.getOcorrenciaById(newOcorrenciaId).onSuccess { fullySaved ->
                        _uiState.update { it.copy(isLoading = false) }
                        onDuplicated(fullySaved)
                    }.onFailure {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Erro ao carregar cópia da ocorrência.") }
                    }
                }.onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Falha ao salvar cópia: ${e.localizedMessage}") }
                }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Falha ao recuperar dados da ocorrência para duplicar.") }
            }
        }
    }

    private fun applyFiltersAndSort() {
        val state = _uiState.value
        var list = state.occurrences

        // 1. Global Search
        if (state.searchGlobal.isNotBlank()) {
            val q = state.searchGlobal.lowercase().trim()
            list = list.filter { o ->
                o.protocolo.lowercase().contains(q) ||
                o.historico?.lowercase()?.contains(q) == true ||
                o.rua?.lowercase()?.contains(q) == true ||
                o.cidade?.lowercase()?.contains(q) == true ||
                o.viaturas.any { v -> v.prefixo.lowercase().contains(q) || v.equipe.any { m -> m.nomeGuerra.lowercase().contains(q) || m.re.lowercase().contains(q) } } ||
                o.vitimas.any { vt -> vt.nome?.lowercase()?.contains(q) == true } ||
                o.veiculos.any { vc -> vc.placa?.lowercase()?.contains(q) == true || vc.modelo?.lowercase()?.contains(q) == true }
            }
        }

        // 2. Specific Filters
        val f = state.filters
        if (f.dataFiltro.isNotBlank()) {
            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val targetDate = try { java.time.LocalDate.parse(f.dataFiltro, formatter) } catch (e: Exception) { null }
            if (targetDate != null) {
                list = list.filter {
                    val occurrenceDate = it.dataHora.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    occurrenceDate == targetDate
                }
            }
        }
        if (f.natureza != null) {
            list = list.filter { it.natureza == f.natureza }
        }
        if (f.cidade.isNotBlank()) {
            list = list.filter { it.cidade?.equals(f.cidade, ignoreCase = true) == true }
        }
        if (f.bairro.isNotBlank()) {
            list = list.filter { it.bairro?.equals(f.bairro, ignoreCase = true) == true }
        }
        if (f.viatura.isNotBlank()) {
            list = list.filter { o -> o.viaturas.any { it.prefixo.equals(f.viatura, ignoreCase = true) } }
        }
        if (f.militar.isNotBlank()) {
            list = list.filter { o -> o.viaturas.any { v -> v.equipe.any { m -> m.re.contains(f.militar) } } }
        }
        if (f.placa.isNotBlank()) {
            list = list.filter { o -> o.veiculos.any { it.placa?.equals(f.placa, ignoreCase = true) == true } }
        }
        if (f.envolvido.isNotBlank()) {
            val q = f.envolvido.lowercase().trim()
            list = list.filter { o ->
                o.vitimas.any { it.nome.lowercase().contains(q) || it.cpf?.contains(q) == true }
            }
        }
        if (f.status.isNotBlank()) {
            val targetStatus = if (f.status.equals("Aberta", ignoreCase = true) || f.status.equals("Aberto", ignoreCase = true)) "ABERTA" else "ENCERRADA"
            list = list.filter { it.status == targetStatus }
        }

        // 3. Sorting
        list = when (state.sortBy) {
            ConsultSort.RECENTES -> list.sortedByDescending { it.dataHora }
            ConsultSort.ANTIGAS -> list.sortedBy { it.dataHora }
            ConsultSort.TALAO -> list.sortedBy { it.protocolo }
            ConsultSort.VITIMAS -> list.sortedByDescending { it.vitimas.size }
            ConsultSort.VEICULOS -> list.sortedByDescending { it.veiculos.size }
        }

        _uiState.update { it.copy(filteredOccurrences = list) }
    }

    fun importOccurrenceFromJson(jsonStr: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            com.example.firenotes.util.JsonImportHelper.importOccurrenceFromJson(jsonStr, repository).fold(
                onSuccess = {
                    onSuccess()
                },
                onFailure = { e ->
                    onError(e.localizedMessage ?: "Erro ao salvar ocorrência importada")
                }
            )
        }
    }
}
