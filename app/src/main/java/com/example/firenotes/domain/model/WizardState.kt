package com.example.firenotes.domain.model

import android.net.Uri

enum class WizardStep(val number: Int, val title: String) {
    INITIAL_DATA(1, "Dados Iniciais"),
    NATURE_SELECTION(2, "Natureza"),
    VIATURAS_EQUIPE(3, "Viaturas e Equipe"),
    BATCH_OCR(4, "Captura de Documentos"),
    VEICULOS(5, "Veículos"),
    VITIMAS(6, "Vítimas"),
    EVIDENCIAS(7, "Evidências da Cena"),
    HISTORICO(8, "Histórico Narrativo"),
    CHECKLIST_FINAL(9, "Checklist Final")
}

data class WizardState(
    // Step 1: Initial Data
    val id: String? = null,
    val protocolo: String = "",
    val data: String = "",
    val hora: String = "",
    val rua: String = "",
    val numero: String = "",
    val bairro: String = "",
    val cidade: String = "",
    val uf: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,

    // Step 2: Natureza
    val natureza: NaturezaOcorrencia = NaturezaOcorrencia.PESSOAL,

    // Step 3: Viaturas
    val viaturas: List<Viatura> = emptyList(),

    // Step 4: Batch OCR Images & Results
    val ocrQueueUris: List<Uri> = emptyList(),
    val isOcrProcessing: Boolean = false,
    val ocrCompletedResults: List<OcrBatchResult> = emptyList(),

    // Step 5: Veículos
    val veiculos: List<VeiculoEnvolvido> = emptyList(),

    // Step 6: Vítimas
    val vitimas: List<Vitima> = emptyList(),

    // Step 7: Evidências
    val evidencias: List<EvidenciaCena> = emptyList(),

    // Step 8: Histórico
    val historico: String = "",

    // UI state
    val currentStep: WizardStep = WizardStep.INITIAL_DATA,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isNightMode: Boolean = false
)

data class OcrBatchResult(
    val uri: Uri,
    val tipo: String, // CNH, CIN, RG, CRLV, etc.
    val rawText: String,
    val extractedFields: Map<String, String>,
    val fieldsWithConfidence: Map<String, Float>,
    val parsedPerson: Pessoa? = null,
    val parsedVehicle: VeiculoMaster? = null,
    val isDuplicate: Boolean = false
)

data class EvidenciaCena(
    val uri: Uri,
    val classification: String, // Documento, Veículo, Vítima, Local, Evidência, Outro
    val timestamp: String,
    val latitude: Double?,
    val longitude: Double?
)
