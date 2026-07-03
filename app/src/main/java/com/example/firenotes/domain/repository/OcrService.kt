package com.example.firenotes.domain.repository

import android.net.Uri

data class OcrField(
    val value: String,
    val confidence: Float, // 0.0 to 1.0
    val isPendingReview: Boolean = confidence < 0.80f
)

data class OcrDocumentResult(
    val tipo: String, // "CNH", "CIN", "RG", "CRLV", "OAB", etc.
    val rawText: String,
    val extractedFields: Map<String, String>, // Kept for 100% backward compatibility
    val fieldsWithConfidence: Map<String, OcrField> = emptyMap() // V4 confidence mappings
)

interface OcrService {
    suspend fun recognizeText(imageUri: Uri): Result<OcrDocumentResult>
}
