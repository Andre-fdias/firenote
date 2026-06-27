package com.firenotes.core.common.domain.model

import java.time.LocalTime

data class Vehicle(
    val id: String,
    val occurrenceId: String,
    val prefix: String,
    val kmDeparture: Double,
    val kmArrival: Double?,
    val kmReturn: Double?,
    val timeDeparture: LocalTime,
    val timeArrival: LocalTime?,
    val timeReturn: LocalTime?,
    val observations: String,
    val militaryList: List<Military> = emptyList()
)
