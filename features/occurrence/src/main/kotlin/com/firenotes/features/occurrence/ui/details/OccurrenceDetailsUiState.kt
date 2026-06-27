package com.firenotes.features.occurrence.ui.details

import com.firenotes.core.common.domain.model.Occurrence

sealed interface OccurrenceDetailsUiState {
    data object Loading : OccurrenceDetailsUiState
    data class Success(val occurrence: Occurrence) : OccurrenceDetailsUiState
    data class Error(val message: String) : OccurrenceDetailsUiState
}
