package com.firenotes.features.occurrence.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.firenotes.features.occurrence.domain.usecase.GetOccurrenceByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OccurrenceDetailsViewModel @Inject constructor(
    private val getOccurrenceByIdUseCase: GetOccurrenceByIdUseCase
) : ViewModel() {

    private val _occurrenceId = MutableStateFlow<String?>(null)
    
    val uiState: StateFlow<OccurrenceDetailsUiState> = _occurrenceId
        .filterNotNull()
        .flatMapLatest { id ->
            getOccurrenceByIdUseCase(id).map { occurrence ->
                if (occurrence != null) {
                    OccurrenceDetailsUiState.Success(occurrence)
                } else {
                    OccurrenceDetailsUiState.Error("Ocorrência não encontrada.")
                }
            }
        }
        .catch { e ->
            emit(OccurrenceDetailsUiState.Error(e.message ?: "Erro desconhecido."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = OccurrenceDetailsUiState.Loading
        )

    fun loadOccurrence(id: String) {
        _occurrenceId.value = id
    }
}
