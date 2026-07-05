package com.example.firenotes.data.service

import android.content.Context
import android.net.Uri
import com.example.firenotes.domain.repository.OcrDocumentResult
import com.example.firenotes.domain.repository.OcrField
import com.example.firenotes.domain.repository.OcrService
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
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
        try {
            val image = InputImage.fromFilePath(context, imageUri)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    if (continuation.isActive) {
                        val rawText = visionText.text
                        val tipo = detectDocumentType(rawText)
                        val fields = parseDocumentFields(tipo, rawText)
                        
                        val fieldsWithConfidence = fields.mapValues { (key, value) ->
                            val confidence = calculateConfidence(key, value)
                            OcrField(value, confidence)
                        }

                        continuation.resume(Result.success(OcrDocumentResult(tipo, rawText, fields, fieldsWithConfidence)))
                    }
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(exception))
                    }
                }
        } catch (e: Exception) {
            if (continuation.isActive) {
                continuation.resume(Result.failure(e))
            }
        }
    }

    private fun detectDocumentType(text: String): String {
        val upperText = text.uppercase()
        return when {
            upperText.contains("RENAVAM") || upperText.contains("CRLV") || upperText.contains("LICENCIAMENTO") -> "CRLV"
            upperText.contains("CNH") || upperText.contains("HABILITAÇÃO") || upperText.contains("CARTEIRA NACIONAL DE HABILITACAO") -> "CNH"
            upperText.contains("CIN") || upperText.contains("IDENTIDADE NACIONAL") -> "CIN"
            upperText.contains("RG") || upperText.contains("REGISTRO GERAL") || upperText.contains("SSP") -> "RG"
            upperText.contains("OAB") || upperText.contains("ADVOGADO") -> "OAB"
            upperText.contains("CRM") || upperText.contains("MEDICINA") || upperText.contains("MÉDICO") -> "CRM"
            upperText.contains("CREA") || upperText.contains("ENGENHARIA") -> "CREA"
            upperText.contains("COREN") || upperText.contains("ENFERMAGEM") -> "COREN"
            upperText.contains("CRP") || upperText.contains("PSICOLOGIA") -> "CRP"
            upperText.contains("CRQ") || upperText.contains("QUIMICA") -> "CRQ"
            upperText.contains("CRBIO") || upperText.contains("BIOLOGIA") -> "CRBIO"
            else -> "OUTROS"
        }
    }

    private fun parseDocumentFields(tipo: String, text: String): Map<String, String> {
        val fields = mutableMapOf<String, String>()
        fields["tipo"] = tipo
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }

        // General helpers for common fields
        val cpfRegex = Regex("\\b[0-9]{3}\\.?[0-9]{3}\\.?[0-9]{3}-?[0-9]{2}\\b")
        cpfRegex.find(text)?.let { fields["cpf"] = it.value.replace(Regex("[.-]"), "") }

        val nascimentoRegex = Regex("\\b([0-9]{2})/([0-9]{2})/([0-9]{4})\\b")
        val birthDates = nascimentoRegex.findAll(text).map { it.value }.toList()
        if (birthDates.isNotEmpty()) {
            fields["nascimento"] = birthDates[0] // Default first date found is usually birth date
        }

        when (tipo) {
            "CNH" -> {
                fields["nome"] = findLineAfter(lines, "NOME") ?: findFirstProperName(lines)
                fields["rg"] = findPattern(text, Regex("\\b[0-9]{1,2}\\.?[0-9]{3}\\.?[0-9]{3}-?[0-9X]\\b"))
                fields["rg_orgao_emissor"] = "SSP"
                fields["registro"] = findPattern(text, Regex("\\b[0-9]{9,11}\\b"))
                fields["categoria"] = findPattern(text, Regex("\\b(A|B|C|D|E|AB|AC|AD|AE)\\b"))
                fields["validade"] = if (birthDates.size > 1) birthDates[1] else findPattern(text, Regex("VAL.?.?DADE:?\\s*([0-9/]{10})"))
                fields["filiacao"] = findLineAfter(lines, "FILIAÇÃO") ?: ""
            }
            "CIN" -> {
                fields["nome"] = findLineAfter(lines, "NOME") ?: findFirstProperName(lines)
                fields["nome_social"] = findLineAfter(lines, "NOME SOCIAL") ?: ""
                fields["naturalidade"] = findLineAfter(lines, "NATURALIDADE") ?: ""
                fields["nacionalidade"] = "Brasileira"
                fields["filiacao"] = findLineAfter(lines, "FILIAÇÃO") ?: ""
            }
            "RG" -> {
                fields["rg"] = findPattern(text, Regex("\\b[0-9]{1,2}\\.?[0-9]{3}\\.?[0-9]{3}-?[0-9X]\\b"))
                fields["rg_orgao_emissor"] = findPattern(text, Regex("\\b(SSP|DETRAN|IFP|ssp|detran|ifp)\\b")) ?: "SSP"
                fields["rg_uf"] = findPattern(text, Regex("\\b(SP|RJ|MG|ES|PR|SC|RS|BA|PE|CE|DF|GO|MT|MS|AL|SE|PB|RN|MA|PI|PA|AM|AC|RO|RR|AP|TO)\\b")) ?: "SP"
                fields["nome"] = findLineAfter(lines, "NOME") ?: findFirstProperName(lines)
                fields["filiacao"] = findLineAfter(lines, "FILIAÇÃO") ?: ""
                fields["data_expedicao"] = if (birthDates.size > 1) birthDates[1] else ""
            }
            "CRLV" -> {
                fields["placa"] = findPattern(text, Regex("[A-Z]{3}[0-9][A-Z0-9][0-9]{2}"))
                fields["renavam"] = findPattern(text, Regex("\\b[0-9]{9,11}\\b"))
                fields["chassi"] = findPattern(text, Regex("\\b[A-HJ-NPR-Z0-9]{17}\\b"))
                fields["marca_modelo"] = findLineAfter(lines, "MARCA/MODELO") ?: ""
                fields["cor"] = findLineAfter(lines, "COR") ?: ""
                fields["ano_fabricacao"] = findPattern(text, Regex("FABR?.?.?:?\\s*([0-9]{4})")) ?: "2020"
                fields["ano_modelo"] = findPattern(text, Regex("MOD.?.?:?\\s*([0-9]{4})")) ?: "2020"
                fields["exercicio"] = findPattern(text, Regex("EXERC.?.?CIO:?\\s*([0-9]{4})")) ?: "2026"
                fields["proprietario"] = findLineAfter(lines, "NOME") ?: findFirstProperName(lines)
            }
            else -> { // Professional card (OAB, CREA, etc.) or OUTROS
                fields["nome"] = findLineAfter(lines, "NOME") ?: findFirstProperName(lines)
                fields["profissao"] = tipo
                fields["registro"] = findPattern(text, Regex("\\b[0-9]{4,8}\\b"))
                fields["conselho"] = tipo
                fields["rg"] = findPattern(text, Regex("\\b[0-9]{1,2}\\.?[0-9]{3}\\.?[0-9]{3}-?[0-9X]\\b"))
                fields["filiacao"] = findLineAfter(lines, "FILIAÇÃO") ?: ""
            }
        }
        return fields
    }

    private fun calculateConfidence(key: String, value: String): Float {
        if (value.isBlank()) return 0.0f
        return when (key) {
            "cpf" -> if (value.length == 11) 0.95f else 0.75f // Flag invalid lengths
            "placa" -> if (value.length == 7) 0.92f else 0.65f
            "chassi" -> if (value.length == 17) 0.90f else 0.70f
            "nascimento" -> if (value.contains("/")) 0.88f else 0.50f
            else -> 0.85f // Default safe confidence
        }
    }

    private fun findPattern(text: String, regex: Regex): String {
        return regex.find(text)?.value ?: ""
    }

    private fun findLineAfter(lines: List<String>, keyword: String): String? {
        lines.forEachIndexed { index, line ->
            if (line.contains(keyword, ignoreCase = true) && index + 1 < lines.size) {
                return lines[index + 1].trim()
            }
        }
        return null
    }

    private fun findFirstProperName(lines: List<String>): String {
        return lines.find { line ->
            line.length > 8 && line.all { it.isLetter() || it.isWhitespace() } && line == line.uppercase()
        } ?: lines.firstOrNull() ?: ""
    }
}
