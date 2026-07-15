package com.example.firenotes.ui.screens.occurrence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firenotes.domain.model.*
import com.example.firenotes.domain.repository.OcorrenciaRepository
import com.example.firenotes.domain.repository.OcrService
import com.example.firenotes.domain.repository.CameraCaptureService
import com.example.firenotes.ui.screens.occurrence.document.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PersonIdentificationViewModel @Inject constructor(
    private val repository: OcorrenciaRepository,
    private val ocrService: OcrService,
    private val cameraCaptureService: CameraCaptureService
) : ViewModel() {

    var testScope: kotlinx.coroutines.CoroutineScope? = null
    private val scope: kotlinx.coroutines.CoroutineScope
        get() = testScope ?: viewModelScope

    fun createPhotoUri(): android.net.Uri {
        return cameraCaptureService.createPhotoUri()
    }

    private val _state = MutableStateFlow(PersonIdentificationUiState())
    val state: StateFlow<PersonIdentificationUiState> = _state.asStateFlow()

    fun setOccurrenceId(id: String) {
        _state.update { it.copy(occurrenceId = id) }
    }

    fun resetSelection() {
        _state.update {
            PersonIdentificationUiState(
                occurrenceId = it.occurrenceId
            )
        }
    }

    fun selectDocumentType(type: DocumentType) {
        _state.update {
            it.copy(
                selectedType = type,
                validationErrors = emptyMap(),
                ocrText = null,
                isSavedSuccessfully = false
            )
        }
    }

    fun updateRgState(rgState: RgDocumentState) {
        _state.update { it.copy(rgState = rgState) }
    }

    fun updateCinState(cinState: CinDocumentState) {
        _state.update { it.copy(cinState = cinState) }
    }

    fun updateCnhState(cnhState: CnhDocumentState) {
        _state.update { it.copy(cnhState = cnhState) }
    }

    fun updateCpfState(cpfState: CpfDocumentState) {
        _state.update { it.copy(cpfState = cpfState) }
    }

    fun updateCrlvState(crlvState: CrlvDocumentState) {
        _state.update { it.copy(crlvState = crlvState) }
    }

    fun updateOabState(oabState: OabDocumentState) {
        _state.update { it.copy(oabState = oabState) }
    }

    fun updateTelefone(value: String) {
        _state.update { it.copy(telefone = value) }
    }

    fun updateEmail(value: String) {
        _state.update { it.copy(email = value) }
    }

    fun processOcr(imageUri: android.net.Uri) {
        _state.update { it.copy(isOcrProcessing = true) }
        scope.launch {
            val result = ocrService.recognizeText(imageUri)
            result.onSuccess { ocrResult ->
                _state.update { state ->
                    when (state.selectedType) {
                        DocumentType.RG -> state.copy(
                            rgState = RgParser.parse(ocrResult),
                            ocrText = ocrResult.rawText,
                            isOcrProcessing = false
                        )
                        DocumentType.CIN -> state.copy(
                            cinState = CinParser.parse(ocrResult),
                            ocrText = ocrResult.rawText,
                            isOcrProcessing = false
                        )
                        DocumentType.CNH -> state.copy(
                            cnhState = CnhParser.parse(ocrResult),
                            ocrText = ocrResult.rawText,
                            isOcrProcessing = false
                        )
                        DocumentType.CPF -> state.copy(
                            cpfState = CpfParser.parse(ocrResult),
                            ocrText = ocrResult.rawText,
                            isOcrProcessing = false
                        )
                        DocumentType.CRLV -> state.copy(
                            crlvState = CrlvParser.parse(ocrResult),
                            ocrText = ocrResult.rawText,
                            isOcrProcessing = false
                        )
                        DocumentType.OAB -> state.copy(
                            oabState = OabParser.parse(ocrResult),
                            ocrText = ocrResult.rawText,
                            isOcrProcessing = false
                        )
                        null -> state.copy(isOcrProcessing = false)
                    }
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isOcrProcessing = false,
                        validationErrors = mapOf("ocr" to (error.localizedMessage ?: "Falha no processamento OCR"))
                    )
                }
            }
        }
    }

    fun clearForm() {
        _state.update { state ->
            when (state.selectedType) {
                DocumentType.RG -> state.copy(rgState = RgDocumentState(), validationErrors = emptyMap(), ocrText = null)
                DocumentType.CIN -> state.copy(cinState = CinDocumentState(), validationErrors = emptyMap(), ocrText = null)
                DocumentType.CNH -> state.copy(cnhState = CnhDocumentState(), validationErrors = emptyMap(), ocrText = null)
                DocumentType.CPF -> state.copy(cpfState = CpfDocumentState(), validationErrors = emptyMap(), ocrText = null)
                DocumentType.CRLV -> state.copy(crlvState = CrlvDocumentState(), validationErrors = emptyMap(), ocrText = null)
                DocumentType.OAB -> state.copy(oabState = OabDocumentState(), validationErrors = emptyMap(), ocrText = null)
                null -> state
            }
        }
    }

    fun validateForm(): Boolean {
        val errors = mutableMapOf<String, String>()
        val state = _state.value
        
        when (state.selectedType) {
            DocumentType.RG -> {
                val doc = state.rgState
                if (doc.nome.isBlank()) errors["nome"] = "Campo obrigatório"
                if (doc.rg.isBlank()) errors["rg"] = "Campo obrigatório"
                if (doc.uf.length != 2) errors["uf"] = "UF deve ter 2 letras"
                if (doc.cpf.isNotBlank() && !isValidCpf(doc.cpf)) errors["cpf"] = "CPF inválido"
                if (doc.nascimento.isNotBlank() && !isValidDate(doc.nascimento)) errors["nascimento"] = "Data inválida (Use DD/MM/AAAA)"
                if (doc.dataExpedicao.isNotBlank() && !isValidDate(doc.dataExpedicao)) errors["dataExpedicao"] = "Data inválida (Use DD/MM/AAAA)"
            }
            DocumentType.CIN -> {
                val doc = state.cinState
                if (doc.nome.isBlank()) errors["nome"] = "Campo obrigatório"
                if (doc.cpf.isBlank()) errors["cpf"] = "Campo obrigatório"
                if (doc.cpf.isNotBlank() && !isValidCpf(doc.cpf)) errors["cpf"] = "CPF inválido"
                if (doc.nascimento.isNotBlank() && !isValidDate(doc.nascimento)) errors["nascimento"] = "Data inválida (Use DD/MM/AAAA)"
            }
            DocumentType.CNH -> {
                val doc = state.cnhState
                if (doc.nome.isBlank()) errors["nome"] = "Campo obrigatório"
                if (doc.cpf.isBlank()) errors["cpf"] = "Campo obrigatório"
                if (doc.cpf.isNotBlank() && !isValidCpf(doc.cpf)) errors["cpf"] = "CPF inválido"
                if (doc.registro.isBlank()) errors["registro"] = "Campo obrigatório"
                if (doc.nascimento.isNotBlank() && !isValidDate(doc.nascimento)) errors["nascimento"] = "Data inválida (Use DD/MM/AAAA)"
            }
            DocumentType.CPF -> {
                val doc = state.cpfState
                if (doc.nome.isBlank()) errors["nome"] = "Campo obrigatório"
                if (doc.cpf.isBlank()) errors["cpf"] = "Campo obrigatório"
                if (doc.cpf.isNotBlank() && !isValidCpf(doc.cpf)) errors["cpf"] = "CPF inválido"
                if (doc.nascimento.isNotBlank() && !isValidDate(doc.nascimento)) errors["nascimento"] = "Data inválida (Use DD/MM/AAAA)"
            }
            DocumentType.CRLV -> {
                val doc = state.crlvState
                if (doc.placa.isBlank()) errors["placa"] = "Campo obrigatório"
                if (doc.placa.isNotBlank() && !isValidPlaca(doc.placa)) errors["placa"] = "Placa inválida"
                if (doc.renavam.isNotBlank() && (doc.renavam.length < 9 || doc.renavam.length > 11)) errors["renavam"] = "RENAVAM inválido"
                if (doc.cpfProprietario.isNotBlank() && !isValidCpf(doc.cpfProprietario)) errors["cpfProprietario"] = "CPF inválido"
            }
            DocumentType.OAB -> {
                val doc = state.oabState
                if (doc.nome.isBlank()) errors["nome"] = "Campo obrigatório"
                if (doc.numero.isBlank()) errors["numero"] = "Campo obrigatório"
                if (doc.uf.length != 2) errors["uf"] = "UF deve ter 2 letras"
            }
            null -> {}
        }
        
        _state.update { it.copy(validationErrors = errors) }
        return errors.isEmpty()
    }

    fun saveDocument(onCompleted: () -> Unit) {
        if (!validateForm()) return

        _state.update { it.copy(isSaving = true) }
        scope.launch {
            val state = _state.value
            val type = state.selectedType ?: return@launch
            val docId = UUID.randomUUID().toString()

            val mainNumber: String
            val values: Map<String, String>

            when (type) {
                DocumentType.RG -> {
                    mainNumber = state.rgState.rg
                    values = mapOf(
                        "nome" to state.rgState.nome,
                        "rg" to state.rgState.rg,
                        "cpf" to state.rgState.cpf,
                        "nascimento" to state.rgState.nascimento,
                        "mae" to state.rgState.mae,
                        "naturalidade" to state.rgState.naturalidade,
                        "orgaoExpedidor" to state.rgState.orgaoExpedidor,
                        "dataExpedicao" to state.rgState.dataExpedicao,
                        "uf" to state.rgState.uf
                    )
                }
                DocumentType.CIN -> {
                    mainNumber = state.cinState.cpf
                    values = mapOf(
                        "cpf" to state.cinState.cpf,
                        "nome" to state.cinState.nome,
                        "nascimento" to state.cinState.nascimento,
                        "pai" to state.cinState.pai,
                        "mae" to state.cinState.mae,
                        "sexo" to state.cinState.sexo,
                        "nacionalidade" to state.cinState.nacionalidade,
                        "naturalidade" to state.cinState.naturalidade,
                        "orgao" to state.cinState.orgao,
                        "expedicao" to state.cinState.expedicao,
                        "validade" to state.cinState.validade
                    )
                }
                DocumentType.CNH -> {
                    mainNumber = state.cnhState.registro
                    values = mapOf(
                        "nome" to state.cnhState.nome,
                        "cpf" to state.cnhState.cpf,
                        "registro" to state.cnhState.registro,
                        "categoria" to state.cnhState.categoria,
                        "nascimento" to state.cnhState.nascimento,
                        "filiacao" to state.cnhState.filiacao,
                        "primeiraHabilitacao" to state.cnhState.primeiraHabilitacao,
                        "validade" to state.cnhState.validade
                    )
                }
                DocumentType.CPF -> {
                    mainNumber = state.cpfState.cpf
                    values = mapOf(
                        "nome" to state.cpfState.nome,
                        "cpf" to state.cpfState.cpf,
                        "nascimento" to state.cpfState.nascimento,
                        "filiacao" to state.cpfState.filiacao,
                        "situacao" to state.cpfState.situacao,
                        "dataInscricao" to state.cpfState.dataInscricao
                    )
                }
                DocumentType.CRLV -> {
                    mainNumber = state.crlvState.placa
                    values = mapOf(
                        "placa" to state.crlvState.placa,
                        "marca" to state.crlvState.marca,
                        "modelo" to state.crlvState.modelo,
                        "versao" to state.crlvState.versao,
                        "anoFabricacao" to state.crlvState.anoFabricacao,
                        "anoModelo" to state.crlvState.anoModelo,
                        "cor" to state.crlvState.cor,
                        "motor" to state.crlvState.motor,
                        "renavam" to state.crlvState.renavam,
                        "chassi" to state.crlvState.chassi,
                        "proprietario" to state.crlvState.proprietario,
                        "cpfProprietario" to state.crlvState.cpfProprietario
                    )
                }
                DocumentType.OAB -> {
                    mainNumber = state.oabState.numero
                    values = mapOf(
                        "nome" to state.oabState.nome,
                        "numero" to state.oabState.numero,
                        "uf" to state.oabState.uf,
                        "expedicao" to state.oabState.expedicao
                    )
                }
            }

            val pessoa: Pessoa
            when (type) {
                DocumentType.RG -> {
                    pessoa = Pessoa(
                        nome = state.rgState.nome,
                        cpf = state.rgState.cpf,
                        rg = state.rgState.rg,
                        rgOrgaoEmissor = state.rgState.orgaoExpedidor,
                        rgUf = state.rgState.uf,
                        nascimento = state.rgState.nascimento,
                        naturalidade = state.rgState.naturalidade,
                        filiacao = state.rgState.mae,
                        telefone = state.telefone.takeIf { it.isNotBlank() },
                        email = state.email.takeIf { it.isNotBlank() }
                    )
                }
                DocumentType.CIN -> {
                    pessoa = Pessoa(
                        nome = state.cinState.nome,
                        cpf = state.cinState.cpf,
                        nascimento = state.cinState.nascimento,
                        filiacao = state.cinState.mae,
                        sexo = state.cinState.sexo,
                        nacionalidade = state.cinState.nacionalidade,
                        naturalidade = state.cinState.naturalidade,
                        rgOrgaoEmissor = state.cinState.orgao,
                        telefone = state.telefone.takeIf { it.isNotBlank() },
                        email = state.email.takeIf { it.isNotBlank() }
                    )
                }
                DocumentType.CNH -> {
                    pessoa = Pessoa(
                        nome = state.cnhState.nome,
                        cpf = state.cnhState.cpf,
                        nascimento = state.cnhState.nascimento,
                        filiacao = state.cnhState.filiacao,
                        telefone = state.telefone.takeIf { it.isNotBlank() },
                        email = state.email.takeIf { it.isNotBlank() }
                    )
                }
                DocumentType.CPF -> {
                    pessoa = Pessoa(
                        nome = state.cpfState.nome,
                        cpf = state.cpfState.cpf,
                        nascimento = state.cpfState.nascimento,
                        filiacao = state.cpfState.filiacao,
                        telefone = state.telefone.takeIf { it.isNotBlank() },
                        email = state.email.takeIf { it.isNotBlank() }
                    )
                }
                DocumentType.CRLV -> {
                    pessoa = Pessoa(
                        nome = state.crlvState.proprietario,
                        cpf = state.crlvState.cpfProprietario,
                        telefone = state.telefone.takeIf { it.isNotBlank() },
                        email = state.email.takeIf { it.isNotBlank() }
                    )
                }
                DocumentType.OAB -> {
                    pessoa = Pessoa(
                        nome = state.oabState.nome,
                        telefone = state.telefone.takeIf { it.isNotBlank() },
                        email = state.email.takeIf { it.isNotBlank() }
                    )
                }
            }

            val doc = Documento(
                id = docId,
                ocorrenciaId = state.occurrenceId,
                pessoaId = null,
                tipo = type.name,
                numero = mainNumber,
                dadosEstruturados = values,
                textoOcr = state.ocrText,
                dataUpload = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            )

            repository.salvarPessoaEDocumento(pessoa, doc)
                .onSuccess {
                    _state.update { it.copy(isSaving = false, isSavedSuccessfully = true) }
                    onCompleted()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isSaving = false,
                            validationErrors = mapOf("global" to (error.localizedMessage ?: "Erro ao salvar no banco"))
                        )
                    }
                }
        }
    }

    private fun isValidCpf(cpf: String): Boolean {
        val cleanCpf = cpf.replace(Regex("[.-]"), "")
        if (cleanCpf.length != 11 || cleanCpf.all { it == cleanCpf[0] }) return false
        val numbers = cleanCpf.map { it.toString().toInt() }
        
        var sum = 0
        for (i in 0 until 9) sum += numbers[i] * (10 - i)
        var dv1 = 11 - (sum % 11)
        if (dv1 > 9) dv1 = 0
        if (numbers[9] != dv1) return false
        
        sum = 0
        for (i in 0 until 10) sum += numbers[i] * (11 - i)
        var dv2 = 11 - (sum % 11)
        if (dv2 > 9) dv2 = 0
        return numbers[10] == dv2
    }

    private fun isValidDate(date: String): Boolean {
        return date.matches(Regex("^\\d{2}/\\d{2}/\\d{4}$"))
    }

    private fun isValidPlaca(placa: String): Boolean {
        val clean = placa.uppercase().replace(" ", "")
        return clean.matches(Regex("^[A-Z]{3}\\d{4}$")) || clean.matches(Regex("^[A-Z]{3}\\d[A-Z]\\d{2}$"))
    }
}

data class PersonIdentificationUiState(
    val selectedType: DocumentType? = DocumentType.RG,
    val occurrenceId: String = "",
    val isOcrProcessing: Boolean = false,
    val isSaving: Boolean = false,
    val isSavedSuccessfully: Boolean = false,
    val ocrText: String? = null,
    val validationErrors: Map<String, String> = emptyMap(),
    val rgState: RgDocumentState = RgDocumentState(),
    val cinState: CinDocumentState = CinDocumentState(),
    val cnhState: CnhDocumentState = CnhDocumentState(),
    val cpfState: CpfDocumentState = CpfDocumentState(),
    val crlvState: CrlvDocumentState = CrlvDocumentState(),
    val oabState: OabDocumentState = OabDocumentState(),
    val telefone: String = "",
    val email: String = ""
)
