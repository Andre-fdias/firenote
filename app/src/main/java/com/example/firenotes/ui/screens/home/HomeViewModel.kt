package com.example.firenotes.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firenotes.domain.model.Ocorrencia
import com.example.firenotes.domain.repository.OcorrenciaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(val occurrences: List<Ocorrencia>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: OcorrenciaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadOccurrences()
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
}
