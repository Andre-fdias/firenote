package com.example.firenotes.data.service

import android.graphics.*
import com.example.firenotes.domain.repository.ImageQualityResult
import com.example.firenotes.domain.repository.ImageProcessingService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageProcessingServiceImpl @Inject constructor() : ImageProcessingService {

    override suspend fun checkQuality(bitmap: Bitmap): ImageQualityResult {
        val width = bitmap.width
        val height = bitmap.height
        val resolutionOk = width >= 800 && height >= 800

        // Analyze brightness (average luminance)
        var totalLuminance = 0L
        val sampleSize = 100 // sample 100 pixels to be performant
        val stepX = (width / 10).coerceAtLeast(1)
        val stepY = (height / 10).coerceAtLeast(1)
        var sampledPixels = 0

        for (x in 0 until width step stepX) {
            for (y in 0 until height step stepY) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                // Luminance formula
                val luminance = (0.299 * r + 0.587 * g + 0.114 * b).toLong()
                totalLuminance += luminance
                sampledPixels++
            }
        }

        val avgBrightness = if (sampledPixels > 0) totalLuminance.toFloat() / sampledPixels else 127f
        val tooDark = avgBrightness < 50f
        val tooBright = avgBrightness > 240f

        val isValid = resolutionOk && !tooDark && !tooBright
        val reason = when {
            !resolutionOk -> "Resolução muito baixa (${width}x${height}). Aproxime mais a câmera."
            tooDark -> "Ambiente muito escuro. Ligue o flash ou melhore a iluminação."
            tooBright -> "Excesso de luz/reflexo na foto."
            else -> null
        }

        return ImageQualityResult(
            isValid = isValid,
            reason = reason,
            brightness = avgBrightness,
            resolutionOk = resolutionOk
        )
    }

    override suspend fun processDocumentImage(bitmap: Bitmap): Bitmap {
        val isRotated = bitmap.width > bitmap.height * 1.5
        val rotated = if (isRotated) {
            val matrix = Matrix().apply { postRotate(90f) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }

        val cropX = (rotated.width * 0.02).toInt()
        val cropY = (rotated.height * 0.02).toInt()
        val cropW = rotated.width - (cropX * 2)
        val cropH = rotated.height - (cropY * 2)
        val cropped = if (cropW > 0 && cropH > 0) {
            val b = Bitmap.createBitmap(rotated, cropX, cropY, cropW, cropH)
            if (rotated != bitmap) {
                rotated.recycle()
            }
            b
        } else {
            rotated
        }

        val enhanced = Bitmap.createBitmap(cropped.width, cropped.height, cropped.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(enhanced)
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }

        val contrast = 1.3f
        val translate = -15f
        val cm = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, translate,
            0f, contrast, 0f, 0f, translate,
            0f, 0f, contrast, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(cropped, 0f, 0f, paint)

        if (cropped != bitmap) {
            cropped.recycle()
        }

        return enhanced
    }
}
