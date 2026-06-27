package com.firenotes.core.common.domain.model

import java.time.LocalDateTime

enum class DocumentType {
    RG,
    CNH,
    CRLV,
    CPF
}

data class Document(
    val id: String,
    val occurrenceId: String?,
    val personId: String?,
    val type: DocumentType,
    val filePath: String,
    val rawText: String?,
    val parsedData: String?, // Representação JSON dos dados extraídos do OCR
    val createdAt: LocalDateTime
)
