package com.firenotes.core.common.domain.model

import java.time.LocalDate
import java.time.LocalTime

enum class OccurrenceStatus {
    PENDING_SYNC,
    SYNCED
}

data class Occurrence(
    val id: String,
    val internalNumber: String,
    val date: LocalDate,
    val dispatchTime: LocalTime,
    val arrivalTime: LocalTime?,
    val completionTime: LocalTime?,
    val nature: String,
    val observations: String,
    val status: OccurrenceStatus,
    val latitude: Double?,
    val longitude: Double?,
    val address: String?,
    val number: String?,
    val complement: String?,
    val neighborhood: String?,
    val city: String?,
    val state: String?,
    val zipCode: String?,
    val referencePoint: String?,
    val photos: List<Photo> = emptyList(),
    val audios: List<Audio> = emptyList(),
    val documents: List<Document> = emptyList(),
    val vehicles: List<Vehicle> = emptyList(),
    val people: List<Person> = emptyList()
)
