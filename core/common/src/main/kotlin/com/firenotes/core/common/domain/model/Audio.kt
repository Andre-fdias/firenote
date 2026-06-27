package com.firenotes.core.common.domain.model

import java.time.LocalDateTime

data class Audio(
    val id: String,
    val occurrenceId: String,
    val filePath: String,
    val durationMs: Long,
    val createdAt: LocalDateTime
)
