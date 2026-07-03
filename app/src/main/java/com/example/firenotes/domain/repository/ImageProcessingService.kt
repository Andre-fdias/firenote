package com.example.firenotes.domain.repository

import android.graphics.Bitmap

data class ImageQualityResult(
    val isValid: Boolean,
    val reason: String? = null,
    val brightness: Float,
    val resolutionOk: Boolean
)

interface ImageProcessingService {
    suspend fun checkQuality(bitmap: Bitmap): ImageQualityResult
    suspend fun processDocumentImage(bitmap: Bitmap): Bitmap
}
