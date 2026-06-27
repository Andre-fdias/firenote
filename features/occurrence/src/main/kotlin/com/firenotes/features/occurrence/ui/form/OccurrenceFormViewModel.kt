package com.firenotes.features.occurrence.ui.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.firenotes.core.common.domain.model.Occurrence
import com.firenotes.core.common.domain.model.OccurrenceStatus
import com.firenotes.features.occurrence.domain.usecase.GetOccurrenceByIdUseCase
import com.firenotes.features.occurrence.domain.usecase.SaveOccurrenceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class OccurrenceFormViewModel @Inject constructor(
    private val saveOccurrenceUseCase: SaveOccurrenceUseCase,
    private val getOccurrenceByIdUseCase: GetOccurrenceByIdUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OccurrenceFormUiState())
    val uiState: StateFlow<OccurrenceFormUiState> = _uiState.asStateFlow()

    private var currentOccurrenceId: String = UUID.randomUUID().toString()
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun onEvent(event: OccurrenceFormEvent) {
        when (event) {
            is OccurrenceFormEvent.InternalNumberChanged -> _uiState.update { it.copy(internalNumber = event.value) }
            is OccurrenceFormEvent.NatureChanged -> _uiState.update { it.copy(nature = event.value) }
            is OccurrenceFormEvent.DateChanged -> _uiState.update { it.copy(date = event.value) }
            is OccurrenceFormEvent.DispatchTimeChanged -> _uiState.update { it.copy(dispatchTime = event.value) }
            is OccurrenceFormEvent.ArrivalTimeChanged -> _uiState.update { it.copy(arrivalTime = event.value) }
            is OccurrenceFormEvent.CompletionTimeChanged -> _uiState.update { it.copy(completionTime = event.value) }
            is OccurrenceFormEvent.ObservationsChanged -> _uiState.update { it.copy(observations = event.value) }
            is OccurrenceFormEvent.AddressChanged -> _uiState.update { it.copy(address = event.value) }
            is OccurrenceFormEvent.NumberChanged -> _uiState.update { it.copy(number = event.value) }
            is OccurrenceFormEvent.ComplementChanged -> _uiState.update { it.copy(complement = event.value) }
            is OccurrenceFormEvent.NeighborhoodChanged -> _uiState.update { it.copy(neighborhood = event.value) }
            is OccurrenceFormEvent.CityChanged -> _uiState.update { it.copy(city = event.value) }
            is OccurrenceFormEvent.StateChanged -> _uiState.update { it.copy(state = event.value) }
            is OccurrenceFormEvent.ZipCodeChanged -> _uiState.update { it.copy(zipCode = event.value) }
            is OccurrenceFormEvent.ReferencePointChanged -> _uiState.update { it.copy(referencePoint = event.value) }
            is OccurrenceFormEvent.LoadOccurrence -> loadOccurrence(event.id)
            is OccurrenceFormEvent.Save -> saveOccurrence()
        }
    }

    private fun loadOccurrence(id: String) {
        currentOccurrenceId = id
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            getOccurrenceByIdUseCase(id).collect { occurrence ->
                if (occurrence != null) {
                    _uiState.update {
                        it.copy(
                            internalNumber = occurrence.internalNumber,
                            nature = occurrence.nature,
                            date = occurrence.date.toString(),
                            dispatchTime = occurrence.dispatchTime.format(timeFormatter),
                            arrivalTime = occurrence.arrivalTime?.format(timeFormatter) ?: "",
                            completionTime = occurrence.completionTime?.format(timeFormatter) ?: "",
                            observations = occurrence.observations,
                            latitude = occurrence.latitude,
                            longitude = occurrence.longitude,
                            address = occurrence.address ?: "",
                            number = occurrence.number ?: "",
                            complement = occurrence.complement ?: "",
                            neighborhood = occurrence.neighborhood ?: "",
                            city = occurrence.city ?: "",
                            state = occurrence.state ?: "",
                            zipCode = occurrence.zipCode ?: "",
                            referencePoint = occurrence.referencePoint ?: "",
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Ocorrência não encontrada.") }
                }
            }
        }
    }

    private fun saveOccurrence() {
        val state = _uiState.value
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                // Conversão de Strings da interface para objetos de data/hora
                val parsedDate = if (state.date.isNotBlank()) LocalDate.parse(state.date, dateFormatter) else LocalDate.now()
                val parsedDispatchTime = if (state.dispatchTime.isNotBlank()) LocalTime.parse(state.dispatchTime, timeFormatter) else LocalTime.now()
                val parsedArrivalTime = if (state.arrivalTime.isNotBlank()) LocalTime.parse(state.arrivalTime, timeFormatter) else null
                val parsedCompletionTime = if (state.completionTime.isNotBlank()) LocalTime.parse(state.completionTime, timeFormatter) else null

                val occurrence = Occurrence(
                    id = currentOccurrenceId,
                    internalNumber = state.internalNumber,
                    date = parsedDate,
                    dispatchTime = parsedDispatchTime,
                    arrivalTime = parsedArrivalTime,
                    completionTime = parsedCompletionTime,
                    nature = state.nature,
                    observations = state.observations,
                    status = OccurrenceStatus.PENDING_SYNC,
                    latitude = state.latitude,
                    longitude = state.longitude,
                    address = state.address.takeIf { it.isNotBlank() },
                    number = state.number.takeIf { it.isNotBlank() },
                    complement = state.complement.takeIf { it.isNotBlank() },
                    neighborhood = state.neighborhood.takeIf { it.isNotBlank() },
                    city = state.city.takeIf { it.isNotBlank() },
                    state = state.state.takeIf { it.isNotBlank() },
                    zipCode = state.zipCode.takeIf { it.isNotBlank() },
                    referencePoint = state.referencePoint.takeIf { it.isNotBlank() }
                )

                val result = saveOccurrenceUseCase(occurrence)
                if (result.isSuccess) {
                    _uiState.update { it.copy(isLoading = false, isSaved = true) }
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Erro ao salvar a ocorrência."
                    _uiState.update { it.copy(isLoading = false, errorMessage = errorMsg) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Dados de data/hora inválidos. Use AAAA-MM-DD e HH:MM.") }
            }
        }
    }
}
