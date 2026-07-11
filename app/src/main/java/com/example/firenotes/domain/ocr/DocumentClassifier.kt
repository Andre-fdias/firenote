package com.example.firenotes.domain.ocr

import javax.inject.Inject

class DocumentClassifier @Inject constructor() {
    
    private val patterns = mapOf(
        "RG" to listOf(
            "registro geral",
            "carteira de identidade",
            "rg"
        ),
        "CIN" to listOf(
            "carteira de identidade nacional",
            "cin",
            "identidade nacional"
        ),
        "CNH" to listOf(
            "carteira nacional de habilitação",
            "cnh",
            "habilitação",
            "driving licence"
        ),
        "CPF" to listOf(
            "cadastro de pessoas físicas",
            "cpf",
            "receita federal"
        ),
        "CRLV" to listOf(
            "certificado de registro",
            "licenciamento",
            "crlv",
            "renavam"
        ),
        "OAB" to listOf(
            "ordem dos advogados",
            "oab"
        )
    )
    
    fun classify(text: String): DocumentType {
        val normalizedText = text.lowercase()
        
        // Verificar cada padrão
        val matches = patterns.mapValues { (_, keywords) ->
            keywords.count { keyword -> normalizedText.contains(keyword) }
        }
        
        // Encontrar o documento com mais correspondências
        val bestMatch = matches.maxByOrNull { it.value }
        
        return if (bestMatch != null && bestMatch.value > 0) {
            when (bestMatch.key) {
                "RG" -> DocumentType.RG
                "CIN" -> DocumentType.CIN
                "CNH" -> DocumentType.CNH
                "CPF" -> DocumentType.CPF
                "CRLV" -> DocumentType.CRLV
                "OAB" -> DocumentType.OAB
                else -> DocumentType.UNKNOWN
            }
        } else {
            DocumentType.UNKNOWN
        }
    }
}

enum class DocumentType {
    RG, CIN, CNH, CPF, CRLV, OAB, UNKNOWN
}
