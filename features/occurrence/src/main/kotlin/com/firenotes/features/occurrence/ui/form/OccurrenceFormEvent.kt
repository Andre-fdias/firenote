package com.firenotes.features.occurrence.ui.form

sealed interface OccurrenceFormEvent {
    data class InternalNumberChanged(val value: String) : OccurrenceFormEvent
    data class NatureChanged(val value: String) : OccurrenceFormEvent
    data class DateChanged(val value: String) : OccurrenceFormEvent
    data class DispatchTimeChanged(val value: String) : OccurrenceFormEvent
    data class ArrivalTimeChanged(val value: String) : OccurrenceFormEvent
    data class CompletionTimeChanged(val value: String) : OccurrenceFormEvent
    data class ObservationsChanged(val value: String) : OccurrenceFormEvent
    data class AddressChanged(val value: String) : OccurrenceFormEvent
    data class NumberChanged(val value: String) : OccurrenceFormEvent
    data class ComplementChanged(val value: String) : OccurrenceFormEvent
    data class NeighborhoodChanged(val value: String) : OccurrenceFormEvent
    data class CityChanged(val value: String) : OccurrenceFormEvent
    data class StateChanged(val value: String) : OccurrenceFormEvent
    data class ZipCodeChanged(val value: String) : OccurrenceFormEvent
    data class ReferencePointChanged(val value: String) : OccurrenceFormEvent
    data object Save : OccurrenceFormEvent
    data class LoadOccurrence(val id: String) : OccurrenceFormEvent
}
