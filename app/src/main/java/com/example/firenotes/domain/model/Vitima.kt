package com.example.firenotes.domain.model

data class Vitima(
    val id: String? = null,
    val ocorrenciaId: String,
    val nome: String?,
    val idade: Int?,
    val lesoesAparentes: String?,
    val destinoSocorro: String? = null,
    val quemSocorreu: String? = null,
    val resultadoOcorrencia: String? = null,
    val sinaisVitais: SinaisVitais = SinaisVitais(),
    val pessoaId: String? = null,
    
    // V3 Socorro fields
    val viaturaSocorroId: String? = null,
    val hospitalDestino: String? = null,
    val transportadoPor: String? = null // "Viatura" or "Outro órgão"
)

data class SinaisVitais(
    val pulso: Int? = null,                // E.g., BPM (batimentos por minuto)
    val pressaoArterial: String? = null,  // E.g., "12/8" ou "120/80" mmHg
    val saturacaoO2: Int? = null,         // E.g., 98%
    val temperatura: Double? = null,      // E.g., 36.5°C
    val escalaGCS: Int? = null,           // Glasgow Coma Scale (3 a 15)
    val observacoesMedicas: String? = null
)
