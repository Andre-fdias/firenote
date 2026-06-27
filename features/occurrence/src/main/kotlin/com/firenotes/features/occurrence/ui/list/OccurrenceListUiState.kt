package com.firenotes.features.occurrence.ui.list

import com.firenotes.core.common.domain.model.Occurrence

sealed interface OccurrenceListUiState {
    data object Loading : OccurrenceListUiState
    data class Success(val occurrences: List<Occurrence>) : OccurrenceListUiState
    data class Error(val message: String) : OccurrenceListUiState
}
