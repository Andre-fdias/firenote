package com.example.firenotes.domain.model

data class Evidencia(
    val id: String? = null,
    val ocorrenciaId: String,
    val tipo: String, // Imagem, Vídeo, Áudio, Documento, OCR, Croqui
    val hashSha256: String,
    val latitude: Double?,
    val longitude: Double?,
    val dataHora: String,
    val usuario: String?,
    val urlStorage: String,
    val miniaturaUrl: String? = null,
    val ocrBruto: String? = null,
    val jsonOcr: Map<String, String> = emptyMap()
)
