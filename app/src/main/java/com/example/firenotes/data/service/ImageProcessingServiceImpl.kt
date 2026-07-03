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
        // 1. Correct rotation/alignment: if vertical or horizontal, ensure proper landscape/portrait alignment
        var processed = if (bitmap.width > bitmap.height * 1.5) {
            // Document is probably rotated, rotate 90 degrees to make it portrait
            val matrix = Matrix().apply { postRotate(90f) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }

        // 2. Crop margins (simulating crop bounds detection by cropping 5% border padding if needed)
        val cropX = (processed.width * 0.02).toInt()
        val cropY = (processed.height * 0.02).toInt()
        val cropW = processed.width - (cropX * 2)
        val cropH = processed.height - (cropY * 2)
        if (cropW > 0 && cropH > 0) {
            processed = Bitmap.createBitmap(processed, cropX, cropY, cropW, cropH)
        }

        // 3. Contrast, sharpness enhancement and shadow reduction via ColorMatrix and Canvas
        val enhanced = Bitmap.createBitmap(processed.width, processed.height, processed.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(enhanced)
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }

        // High contrast matrix (factor = 1.3f) + shadow reduction offset
        val contrast = 1.3f
        val translate = -15f
        val cm = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, translate,
            0f, contrast, 0f, 0f, translate,
            0f, 0f, contrast, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(processed, 0f, 0f, paint)

        return enhanced
    }
}
