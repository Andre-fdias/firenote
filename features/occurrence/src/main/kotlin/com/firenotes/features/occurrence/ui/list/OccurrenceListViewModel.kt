package com.firenotes.features.occurrence.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.firenotes.features.occurrence.domain.usecase.GetOccurrencesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class OccurrenceListViewModel @Inject constructor(
    private val getOccurrencesUseCase: GetOccurrencesUseCase
) : ViewModel() {

    val uiState: StateFlow<OccurrenceListUiState> = getOccurrencesUseCase()
        .map { occurrences ->
            OccurrenceListUiState.Success(occurrences)
        }
        .catch { e ->
            emit(OccurrenceListUiState.Error(e.message ?: "Erro desconhecido ao carregar ocorrências."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = OccurrenceListUiState.Loading
        )
}
