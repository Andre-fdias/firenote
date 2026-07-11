package com.example.firenotes.domain.ocr

import javax.inject.Inject

class FieldExtractor @Inject constructor() {
    
    // Regex para cada campo
    private val regexPatterns = mapOf(
        "nome" to Regex("""(?:NOME|NOME COMPLETO)[\s:]+([A-Z][A-Z\s]+)""", RegexOption.IGNORE_CASE),
        "cpf" to Regex("""(\d{3}\.\d{3}\.\d{3}-\d{2}|\d{11})"""),
        "rg" to Regex("""(\d{2}\.\d{3}\.\d{3}-\d{1}|\d{8,9}-\d{1})"""),
        "data_nascimento" to Regex("""(?:NASC|NASCIMENTO)[\s:]+(\d{2}/\d{2}/\d{4})""", RegexOption.IGNORE_CASE),
        "filiacao" to Regex("""(?:FILIAÇÃO|FILHO|FILHA)[\s:]+([A-Z][A-Z\s]+)""", RegexOption.IGNORE_CASE),
        "registro" to Regex("""(?:REGISTRO|Nº)[\s:]+(\d{9,11})""", RegexOption.IGNORE_CASE),
        "categorias" to Regex("""(?:CATEGORIA|CAT)[\s:]+([A-E][,\s]*[A-E]*)""", RegexOption.IGNORE_CASE),
        "validade" to Regex("""(?:VALIDADE|VAL)[\s:]+(\d{2}/\d{2}/\d{4})""", RegexOption.IGNORE_CASE),
        "placa" to Regex("""([A-Z]{3}-\d{4}|[A-Z]{3}\d[A-Z]\d{2})""", RegexOption.IGNORE_CASE),
        "ano_fabricacao" to Regex("""(?:ANO FABRICAÇÃO|ANO FAB)[\s:]+(\d{4})""", RegexOption.IGNORE_CASE),
        "ano_modelo" to Regex("""(?:ANO MODELO)[\s:]+(\d{4})""", RegexOption.IGNORE_CASE),
        "marca" to Regex("""(?:MARCA)[\s:]+([A-Z][A-Z\s]+)""", RegexOption.IGNORE_CASE),
        "modelo" to Regex("""(?:MODELO)[\s:]+([A-Z][A-Z\s]+)""", RegexOption.IGNORE_CASE),
        "versao" to Regex("""(?:VERSÃO)[\s:]+([A-Z][A-Z\s]+)""", RegexOption.IGNORE_CASE),
        "cor" to Regex("""(?:COR)[\s:]+([A-Z][A-Z\s]+)""", RegexOption.IGNORE_CASE),
        "motor" to Regex("""(?:MOTOR)[\s:]+([A-Z0-9]+)""", RegexOption.IGNORE_CASE),
        "proprietario_nome" to Regex("""(?:PROPRIETÁRIO|NOME)[\s:]+([A-Z][A-Z\s]+)""", RegexOption.IGNORE_CASE),
        "proprietario_cpf" to Regex("""(?:PROPRIETÁRIO CPF|CPF)[\s:]+(\d{3}\.\d{3}\.\d{3}-\d{2}|\d{11})""", RegexOption.IGNORE_CASE)
    )
    
    fun extract(text: String, fields: List<String>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        
        fields.forEach { field ->
            val pattern = regexPatterns[field]
            if (pattern != null) {
                pattern.find(text)?.let { match ->
                    result[field] = match.groupValues[1].trim()
                }
            }
        }
        
        return result
    }
    
    fun extractGeneric(text: String): Map<String, String> {
        // Extração genérica para campos que podem aparecer em qualquer lugar
        val result = mutableMapOf<String, String>()
        
        // Nome - tentar várias formas
        val nomeRegex = Regex("""([A-Z]{2,}(?:\s+[A-Z]{2,})+)""")
        nomeRegex.find(text)?.let { 
            result["nome"] = it.groupValues[1].trim()
        }
        
        // CPF
        regexPatterns["cpf"]?.find(text)?.let {
            result["cpf"] = it.groupValues[1].trim()
        }
        
        // RG
        regexPatterns["rg"]?.find(text)?.let {
            result["rg"] = it.groupValues[1].trim()
        }
        
        // Data
        val dataRegex = Regex("""\d{2}/\d{2}/\d{4}""")
        val dataMatches = dataRegex.findAll(text).toList()
        if (dataMatches.isNotEmpty()) {
            result["data_nascimento"] = dataMatches.first().value
        }
        
        return result
    }
}
