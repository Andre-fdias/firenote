package com.firenotes.features.occurrence.ui.form

data class OccurrenceFormUiState(
    val internalNumber: String = "",
    val nature: String = "",
    val date: String = "", // ISO Format yyyy-MM-dd
    val dispatchTime: String = "", // HH:mm
    val arrivalTime: String = "", // HH:mm
    val completionTime: String = "", // HH:mm
    val observations: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String = "",
    val number: String = "",
    val complement: String = "",
    val neighborhood: String = "",
    val city: String = "",
    val state: String = "",
    val zipCode: String = "",
    val referencePoint: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)
