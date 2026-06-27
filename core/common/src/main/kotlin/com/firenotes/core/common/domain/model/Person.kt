package com.firenotes.core.common.domain.model

import java.time.LocalDate

enum class PersonType {
    VICTIM,      // Vítima
    WITNESS,     // Testemunha
    DRIVER,      // Condutor
    INVOLVED     // Envolvido Geral
}

data class Person(
    val id: String,
    val occurrenceId: String,
    val type: PersonType,
    val name: String,
    val cpf: String?,
    val rg: String?,
    val cnh: String?,
    val birthDate: LocalDate?,
    val phone: String?,
    val address: String?,
    val observations: String?,
    val photos: List<Photo> = emptyList(),
    val documents: List<Document> = emptyList()
)
