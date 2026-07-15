package com.example.firenotes.ui.screens.occurrence.document

import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping

data class RgDocumentState(
    val nome: String = "",
    val rg: String = "",
    val cpf: String = "",
    val nascimento: String = "", // Keeping DD/MM/AAAA string representation for simpler textfield binding
    val mae: String = "",
    val naturalidade: String = "",
    val orgaoExpedidor: String = "",
    val dataExpedicao: String = "",
    val uf: String = ""
)

data class CinDocumentState(
    val cpf: String = "",
    val nome: String = "",
    val nascimento: String = "",
    val pai: String = "",
    val mae: String = "",
    val sexo: String = "",
    val nacionalidade: String = "",
    val naturalidade: String = "",
    val orgao: String = "",
    val expedicao: String = "",
    val validade: String = ""
)

data class CnhDocumentState(
    val nome: String = "",
    val cpf: String = "",
    val registro: String = "",
    val categoria: String = "",
    val nascimento: String = "",
    val filiacao: String = "",
    val primeiraHabilitacao: String = "",
    val validade: String = ""
)

data class CpfDocumentState(
    val nome: String = "",
    val cpf: String = "",
    val nascimento: String = "",
    val filiacao: String = "",
    val situacao: String = "",
    val dataInscricao: String = ""
)

data class CrlvDocumentState(
    val placa: String = "",
    val marca: String = "",
    val modelo: String = "",
    val versao: String = "",
    val anoFabricacao: String = "",
    val anoModelo: String = "",
    val cor: String = "",
    val motor: String = "",
    val renavam: String = "",
    val chassi: String = "",
    val proprietario: String = "",
    val cpfProprietario: String = ""
)

data class OabDocumentState(
    val nome: String = "",
    val numero: String = "",
    val uf: String = "",
    val expedicao: String = ""
)

fun formatCpf(input: String): String {
    val clean = input.filter { it.isDigit() }.take(11)
    val sb = java.lang.StringBuilder()
    for (i in clean.indices) {
        sb.append(clean[i])
        if (i == 2 || i == 5) {
            if (i < clean.lastIndex) {
                sb.append('.')
            }
        } else if (i == 8) {
            if (i < clean.lastIndex) {
                sb.append('-')
            }
        }
    }
    return sb.toString()
}

fun formatTelefone(input: String, isCelular: Boolean): String {
    val clean = input.filter { it.isDigit() }
    if (clean.isEmpty()) return ""
    val maxDigits = if (isCelular) 11 else 10
    val digits = clean.take(maxDigits)
    
    val sb = java.lang.StringBuilder()
    sb.append("(")
    if (digits.length >= 2) {
        sb.append(digits.substring(0, 2))
        sb.append(")")
        if (digits.length > 2) {
            if (isCelular) {
                val firstPart = digits.substring(2, minOf(7, digits.length))
                sb.append(firstPart)
                if (digits.length > 7) {
                    sb.append("-")
                    sb.append(digits.substring(7))
                }
            } else {
                val firstPart = digits.substring(2, minOf(6, digits.length))
                sb.append(firstPart)
                if (digits.length > 6) {
                    sb.append("-")
                    sb.append(digits.substring(6))
                }
            }
        }
    } else {
        sb.append(digits)
    }
    return sb.toString()
}

class CpfVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val clean = text.text.filter { it.isDigit() }.take(11)
        val out = StringBuilder()
        for (i in clean.indices) {
            out.append(clean[i])
            if (i == 2 || i == 5) out.append('.')
            if (i == 8) out.append('-')
        }
        
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                var dots = 0
                var hyphens = 0
                if (offset > 2) dots++
                if (offset > 5) dots++
                if (offset > 8) hyphens++
                return minOf(offset + dots + hyphens, out.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                var dots = 0
                var hyphens = 0
                if (offset > 3) dots++
                if (offset > 7) dots++
                if (offset > 11) hyphens++
                return minOf(offset - dots - hyphens, clean.length)
            }
        }
        
        return TransformedText(AnnotatedString(out.toString()), offsetMapping)
    }
}

class TelefoneVisualTransformation(val isCelular: Boolean) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val clean = text.text.filter { it.isDigit() }.take(if (isCelular) 11 else 10)
        val out = StringBuilder()
        if (clean.isNotEmpty()) {
            out.append("(")
            val len = clean.length
            if (len >= 2) {
                out.append(clean.substring(0, 2))
                out.append(") ")
                if (isCelular) {
                    if (len > 2) {
                        out.append(clean.substring(2, minOf(7, len)))
                    }
                    if (len > 7) {
                        out.append("-")
                        out.append(clean.substring(7))
                    }
                } else {
                    if (len > 2) {
                        out.append(clean.substring(2, minOf(6, len)))
                    }
                    if (len > 6) {
                        out.append("-")
                        out.append(clean.substring(6))
                    }
                }
            } else {
                out.append(clean)
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                var added = 1
                if (offset >= 2) added += 2
                if (isCelular) {
                    if (offset > 7) added++
                } else {
                    if (offset > 6) added++
                }
                return minOf(offset + added, out.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 1) return 0
                var removed = 1
                if (offset >= 4) removed += 2
                val isHyphenPresent = out.contains("-")
                val hyphenIndex = out.indexOf("-")
                if (isHyphenPresent && offset > hyphenIndex) removed++
                return minOf(offset - removed, clean.length)
            }
        }

        return TransformedText(AnnotatedString(out.toString()), offsetMapping)
    }
}
