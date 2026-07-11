package com.example.firenotes.data.service

import android.content.Context
import android.net.Uri
import com.example.firenotes.data.service.ocr.DocumentClassifier
import com.example.firenotes.data.service.ocr.RegexExtractor
import com.example.firenotes.data.service.ocr.OCRConfidenceCalculator
import com.example.firenotes.domain.repository.OcrDocumentResult
import com.example.firenotes.domain.repository.OcrField
import com.example.firenotes.domain.repository.OcrService
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class OcrServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : OcrService {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    override suspend fun recognizeText(imageUri: Uri): Result<OcrDocumentResult> = suspendCancellableCoroutine { continuation ->
        android.util.Log.d("FireOCR", "Iniciando reconhecimento de texto para imagem: $imageUri")
        try {
            val image = InputImage.fromFilePath(context, imageUri)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    if (continuation.isActive) {
                        val rawText = visionText.text
                        android.util.Log.d("FireOCR", "Texto extraído com sucesso pelo ML Kit (Dados Sensíveis Omitidos)")

                        val tipo = DocumentClassifier.classify(rawText)
                        android.util.Log.d("FireOCR", "Documento classificado como: $tipo")

                        val fields = parseDocumentFields(tipo, rawText)

                        val fieldsWithConfidence = fields.mapValues { (key, value) ->
                            val confidence = OCRConfidenceCalculator.calculate(key, value)
                            OcrField(value, confidence)
                        }

                        // Generate the requested JSON payload containing OCR text, confidence, image, date, hour
                        val now = LocalDateTime.now()
                        val dateStr = now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        val timeStr = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"))

                        val enrichedFields = fields.toMutableMap()

                        val jsonBuilder = StringBuilder()
                        jsonBuilder.append("{")
                        jsonBuilder.append("\"tipo_documento\":\"$tipo\",")
                        jsonBuilder.append("\"metadata\":{")
                        jsonBuilder.append("\"data\":\"$dateStr\",")
                        jsonBuilder.append("\"hora\":\"$timeStr\",")
                        jsonBuilder.append("\"imagem\":\"${imageUri}\"")
                        jsonBuilder.append("},")
                        jsonBuilder.append("\"dados_extraidos\":{")
                        fields.entries.forEachIndexed { i, entry ->
                            jsonBuilder.append("\"${entry.key}\":\"${entry.value.replace("\"", "\\\"")}\"")
                            if (i < fields.size - 1) jsonBuilder.append(",")
                        }
                        jsonBuilder.append("},")
                        jsonBuilder.append("\"confianca\":{")
                        fieldsWithConfidence.entries.forEachIndexed { i, entry ->
                            jsonBuilder.append("\"${entry.key}\":${(entry.value.confidence * 100).toInt()}")
                            if (i < fieldsWithConfidence.size - 1) jsonBuilder.append(",")
                        }
                        jsonBuilder.append("}")
                        jsonBuilder.append("}")
                        val jsonString = jsonBuilder.toString()

                        android.util.Log.d("FireOCR", "Payload JSON estruturado gerado com sucesso.")
                        enrichedFields["json_completo"] = jsonString

                        continuation.resume(Result.success(OcrDocumentResult(tipo, rawText, enrichedFields, fieldsWithConfidence)))
                    }
                }
                .addOnFailureListener { exception ->
                    android.util.Log.e("FireOCR", "Erro no processamento do ML Kit OCR: ${exception.message}", exception)
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(exception))
                    }
                }
        } catch (e: Exception) {
            android.util.Log.e("FireOCR", "Erro catastrófico no pipeline de OCR: ${e.message}", e)
            if (continuation.isActive) {
                continuation.resume(Result.failure(e))
            }
        }
    }

    private fun parseDocumentFields(tipo: String, text: String): Map<String, String> {
        val fields = mutableMapOf<String, String>()
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }

        // Common defaults
        val cpfFound = RegexExtractor.findCpf(text)
        if (cpfFound.isNotBlank()) fields["cpf"] = cpfFound

        val datesFound = RegexExtractor.findDates(text)
        if (datesFound.isNotEmpty()) {
            fields["nascimento"] = datesFound[0]
        }

        when (tipo) {
            "RG" -> {
                fields["nome"] = RegexExtractor.findLineAfter(lines, "NOME") ?: RegexExtractor.findFirstProperName(lines)
                fields["rg"] = RegexExtractor.findRg(text)
                fields["filiacao"] = RegexExtractor.findLineAfter(lines, "FILIAÇÃO") ?: RegexExtractor.findLineAfter(lines, "FILIACAO") ?: ""
                if (datesFound.size > 1) {
                    fields["data_expedicao"] = datesFound[1]
                }
            }
            "CIN" -> {
                fields["nome"] = RegexExtractor.findLineAfter(lines, "NOME") ?: RegexExtractor.findFirstProperName(lines)
                fields["filiacao"] = RegexExtractor.findLineAfter(lines, "FILIAÇÃO") ?: RegexExtractor.findLineAfter(lines, "FILIACAO") ?: ""
            }
            "CNH" -> {
                fields["nome"] = RegexExtractor.findLineAfter(lines, "NOME") ?: RegexExtractor.findFirstProperName(lines)
                fields["rg"] = RegexExtractor.findRg(text)
                fields["registro"] = RegexExtractor.findPattern(text, RegexExtractor.REGISTRO_CNH_PATTERN)
                fields["categoria"] = RegexExtractor.findPattern(text, RegexExtractor.CATEGORIA_CNH_PATTERN)
                fields["filiacao"] = RegexExtractor.findLineAfter(lines, "FILIAÇÃO") ?: RegexExtractor.findLineAfter(lines, "FILIACAO") ?: ""
                if (datesFound.size > 1) {
                    fields["validade"] = datesFound[1]
                }
            }
            "CPF" -> {
                fields["nome"] = RegexExtractor.findLineAfter(lines, "NOME") ?: RegexExtractor.findFirstProperName(lines)
            }
            "CRLV" -> {
                fields["placa"] = RegexExtractor.findPlaca(text)
                fields["renavam"] = RegexExtractor.findRenavam(text)
                fields["chassi"] = RegexExtractor.findChassi(text)
                fields["marca_modelo"] = RegexExtractor.findLineAfter(lines, "MARCA/MODELO") ?: ""
                fields["cor"] = RegexExtractor.findLineAfter(lines, "COR") ?: ""
                fields["motor"] = RegexExtractor.findLineAfter(lines, "MOTOR") ?: RegexExtractor.findPattern(text, Regex("\\b[A-Z0-9]{9,15}\\b"))
                fields["ano_fabricacao"] = RegexExtractor.findPattern(text, Regex("FABR?.?.?:?\\s*([0-9]{4})"))
                fields["ano_modelo"] = RegexExtractor.findPattern(text, Regex("MOD.?.?:?\\s*([0-9]{4})"))
                fields["proprietario"] = RegexExtractor.findLineAfter(lines, "NOME") ?: RegexExtractor.findFirstProperName(lines)

                val allCpfs = RegexExtractor.CPF_PATTERN.findAll(text).map { it.value.replace(Regex("[.-]"), "") }.toList()
                if (allCpfs.size > 1) {
                    fields["cpf_proprietario"] = allCpfs[1]
                } else if (allCpfs.size == 1) {
                    fields["cpf_proprietario"] = allCpfs[0]
                }
            }
            "OAB" -> {
                fields["nome"] = RegexExtractor.findLineAfter(lines, "NOME") ?: RegexExtractor.findFirstProperName(lines)
                fields["numero"] = RegexExtractor.findPattern(text, RegexExtractor.NUMERO_OAB_PATTERN)
                fields["uf"] = RegexExtractor.findPattern(text, RegexExtractor.UF_PATTERN)
            }
            else -> {
                fields["nome"] = RegexExtractor.findLineAfter(lines, "NOME") ?: RegexExtractor.findFirstProperName(lines)
            }
        }
        return fields
    }
}
