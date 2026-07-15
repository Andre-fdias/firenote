package com.example.firenotes.ui.screens.consult

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firenotes.domain.model.Ocorrencia
import com.example.firenotes.domain.repository.OcorrenciaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OccurrenceDetailsUiState(
    val occurrence: Ocorrencia? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class OccurrenceDetailsViewModel @Inject constructor(
    private val repository: OcorrenciaRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val occurrenceId: String? = savedStateHandle["occurrenceId"]

    private val _uiState = MutableStateFlow(OccurrenceDetailsUiState())
    val uiState: StateFlow<OccurrenceDetailsUiState> = _uiState.asStateFlow()

    init {
        loadOccurrence()
    }

    fun loadOccurrence() {
        val id = occurrenceId
        if (id.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "ID da ocorrência inválido ou não informado") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            repository.getOcorrenciaById(id).fold(
                onSuccess = { fullOcorrencia ->
                    _uiState.update { it.copy(occurrence = fullOcorrencia, isLoading = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Falha ao carregar ocorrência: ${e.localizedMessage}") }
                }
            )
        }
    }

    fun deleteOccurrence(onSuccess: () -> Unit) {
        val id = occurrenceId ?: return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            repository.deleteOcorrencia(id).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Falha ao deletar ocorrência: ${e.localizedMessage}") }
                }
            )
        }
    }
}
