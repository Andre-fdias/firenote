package com.example.firenotes.domain.ocr

import javax.inject.Inject

class FieldValidator @Inject constructor() {
    
    data class ValidationResult(
        val isValid: Boolean,
        val confidence: Int, // 0-100
        val message: String? = null
    )
    
    fun validateCPF(cpf: String?): ValidationResult {
        if (cpf == null) return ValidationResult(false, 0, "CPF não informado")
        
        val numeros = cpf.replace(Regex("[^0-9]"), "")
        
        if (numeros.length != 11) {
            return ValidationResult(false, 20, "CPF deve ter 11 dígitos")
        }
        
        if (numeros.all { it == numeros[0] }) {
            return ValidationResult(false, 10, "CPF inválido (digitos repetidos)")
        }
        
        // Validar dígitos verificadores
        var soma = 0
        for (i in 0..8) {
            soma += numeros[i].toString().toInt() * (10 - i)
        }
        var resto = soma % 11
        val digito1 = if (resto < 2) 0 else 11 - resto
        
        if (digito1 != numeros[9].toString().toInt()) {
            return ValidationResult(false, 30, "Dígito verificador inválido")
        }
        
        soma = 0
        for (i in 0..9) {
            soma += numeros[i].toString().toInt() * (11 - i)
        }
        resto = soma % 11
        val digito2 = if (resto < 2) 0 else 11 - resto
        
        if (digito2 != numeros[10].toString().toInt()) {
            return ValidationResult(false, 30, "Dígito verificador inválido")
        }
        
        return ValidationResult(true, 100, "CPF válido")
    }
    
    fun validateRG(rg: String?): ValidationResult {
        if (rg == null) return ValidationResult(false, 0, "RG não informado")
        
        val numeros = rg.replace(Regex("[^0-9]"), "")
        
        if (numeros.length !in 8..9) {
            return ValidationResult(false, 20, "RG deve ter 8 ou 9 dígitos")
        }
        
        return ValidationResult(true, 90, "RG válido")
    }
    
    fun validateData(data: String?): ValidationResult {
        if (data == null) return ValidationResult(false, 0, "Data não informada")
        
        val regex = Regex("""(\d{2})/(\d{2})/(\d{4})""")
        val match = regex.find(data) ?: return ValidationResult(false, 10, "Formato de data inválido")
        
        val dia = match.groupValues[1].toInt()
        val mes = match.groupValues[2].toInt()
        val ano = match.groupValues[3].toInt()
        
        if (ano < 1900 || ano > 2100) {
            return ValidationResult(false, 30, "Ano fora do intervalo")
        }
        
        if (mes !in 1..12) {
            return ValidationResult(false, 30, "Mês inválido")
        }
        
        val diasNoMes = when (mes) {
            2 -> if (ano % 4 == 0 && (ano % 100 != 0 || ano % 400 == 0)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
        
        if (dia !in 1..diasNoMes) {
            return ValidationResult(false, 30, "Dia inválido para o mês")
        }
        
        return ValidationResult(true, 100, "Data válida")
    }
    
    fun validatePlaca(placa: String?): ValidationResult {
        if (placa == null) return ValidationResult(false, 0, "Placa não informada")
        
        val placaLimpa = placa.uppercase()
        val padraoAntigo = Regex("""[A-Z]{3}-\d{4}""")
        val padraoMercosul = Regex("""[A-Z]{3}\d[A-Z]\d{2}""")
        
        if (padraoAntigo.matches(placaLimpa) || padraoMercosul.matches(placaLimpa)) {
            return ValidationResult(true, 95, "Placa válida")
        }
        
        return ValidationResult(false, 20, "Formato de placa inválido")
    }
}
