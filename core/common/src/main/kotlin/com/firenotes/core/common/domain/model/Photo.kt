package com.firenotes.core.common.domain.model

import java.time.LocalDateTime

enum class PhotoType {
    LOCAL,
    VEHICLE,
    DOCUMENT,
    PERSON,
    FREE
}

data class Photo(
    val id: String,
    val occurrenceId: String?,
    val personId: String?,
    val type: PhotoType,
    val filePath: String,
    val createdAt: LocalDateTime
)
