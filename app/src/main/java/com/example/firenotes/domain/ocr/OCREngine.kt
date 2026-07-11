package com.example.firenotes.domain.ocr

import javax.inject.Inject

class OCREngine @Inject constructor(
    private val classifier: DocumentClassifier,
    private val extractor: FieldExtractor,
    private val validator: FieldValidator
) {
    
    data class OCRResult(
        val tipo: DocumentType,
        val campos: Map<String, String>,
        val confianca: Map<String, Int>,
        val validacoes: Map<String, FieldValidator.ValidationResult>,
        val textoOriginal: String
    )
    
    fun process(text: String): OCRResult {
        // 1. Classificar documento
        val tipo = classifier.classify(text)
        
        // 2. Extrair campos específicos
        val campos = when (tipo) {
            DocumentType.RG -> extractRG(text)
            DocumentType.CIN -> extractCIN(text)
            DocumentType.CNH -> extractCNH(text)
            DocumentType.CPF -> extractCPF(text)
            DocumentType.CRLV -> extractCRLV(text)
            DocumentType.OAB -> extractOAB(text)
            else -> extractGeneric(text)
        }
        
        // 3. Validar campos e calcular confiança
        val validacoes = mutableMapOf<String, FieldValidator.ValidationResult>()
        val confianca = mutableMapOf<String, Int>()
        
        campos.forEach { (key, value) ->
            val validation = when (key) {
                "cpf" -> validator.validateCPF(value)
                "rg" -> validator.validateRG(value)
                "data_nascimento", "validade" -> validator.validateData(value)
                "placa" -> validator.validatePlaca(value)
                else -> FieldValidator.ValidationResult(true, 70, "Valor extraído")
            }
            validacoes[key] = validation
            confianca[key] = validation.confidence
        }
        
        return OCRResult(
            tipo = tipo,
            campos = campos,
            confianca = confianca,
            validacoes = validacoes,
            textoOriginal = text
        )
    }
    
    private fun extractRG(text: String): Map<String, String> {
        val fields = listOf("nome", "rg", "cpf", "data_nascimento", "filiacao")
        return extractor.extract(text, fields)
    }
    
    private fun extractCIN(text: String): Map<String, String> {
        val fields = listOf("cpf", "nome", "data_nascimento", "filiacao")
        return extractor.extract(text, fields)
    }
    
    private fun extractCNH(text: String): Map<String, String> {
        val fields = listOf("nome", "cpf", "rg", "registro", "categorias", "data_nascimento", "filiacao", "validade")
        return extractor.extract(text, fields)
    }
    
    private fun extractCPF(text: String): Map<String, String> {
        val fields = listOf("nome", "cpf", "data_nascimento")
        return extractor.extract(text, fields)
    }
    
    private fun extractCRLV(text: String): Map<String, String> {
        val fields = listOf(
            "placa", "ano_fabricacao", "ano_modelo", "marca", "modelo", "versao",
            "cor", "motor", "proprietario_nome", "proprietario_cpf"
        )
        return extractor.extract(text, fields)
    }
    
    private fun extractOAB(text: String): Map<String, String> {
        val fields = listOf("nome", "numero", "uf")
        return extractor.extract(text, fields)
    }
    
    private fun extractGeneric(text: String): Map<String, String> {
        return extractor.extractGeneric(text)
    }
}
