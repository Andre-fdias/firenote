package com.example.firenotes.ui.screens.wizard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firenotes.domain.model.*
import com.example.firenotes.domain.repository.*
import com.example.firenotes.data.service.OccurrenceNarrativeService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class WizardViewModel @Inject constructor(
    private val repository: OcorrenciaRepository,
    private val locationService: LocationService,
    private val ocrService: OcrService,
    private val cameraCaptureService: CameraCaptureService,
    private val imageProcessingService: ImageProcessingService,
    private val narrativeService: OccurrenceNarrativeService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(WizardState())
    val uiState: StateFlow<WizardState> = _uiState.asStateFlow()

    private val validator = WizardValidator()
    private val sharedPrefs by lazy {
        context.getSharedPreferences("fire_notes_wizard_prefs", Context.MODE_PRIVATE)
    }

    init {
        // Recover state if available
        recoverWizardState()
        
        // Setup initial date and time if empty
        if (_uiState.value.data.isBlank()) {
            val today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            val nowTime = java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            _uiState.update { it.copy(data = today, hora = nowTime) }
        }
    }

    // --- State recovery & persistence ---

    private fun recoverWizardState() {
        val savedJson = sharedPrefs.getString("saved_wizard_state", null)
        if (!savedJson.isNullOrBlank()) {
            try {
                // Recover basic text fields to continue where left off
                val parts = savedJson.split("|||")
                if (parts.size >= 8) {
                    _uiState.update { it.copy(
                        protocolo = parts[0],
                        data = parts[1],
                        hora = parts[2],
                        rua = parts[3],
                        numero = parts[4],
                        bairro = parts[5],
                        cidade = parts[6],
                        uf = parts[7],
                        currentStep = WizardStep.entries.getOrNull(parts.getOrNull(8)?.toIntOrNull() ?: 0) ?: WizardStep.INITIAL_DATA
                    ) }
                }
            } catch (e: Exception) {
                // Fallback silently if format parsing fails
            }
        }
    }

    private fun saveWizardStateToDisk() {
        val state = _uiState.value
        val serialized = "${state.protocolo}|||${state.data}|||${state.hora}|||${state.rua}|||${state.numero}|||${state.bairro}|||${state.cidade}|||${state.uf}|||${state.currentStep.ordinal}"
        sharedPrefs.edit().putString("saved_wizard_state", serialized).apply()
    }

    private fun clearWizardStateFromDisk() {
        sharedPrefs.edit().remove("saved_wizard_state").apply()
    }

    // --- Navigation (WizardNavigator / WizardController) ---

    fun nextStep() {
        val current = _uiState.value.currentStep
        val steps = WizardStep.values()
        if (current.ordinal < steps.lastIndex) {
            goToStep(steps[current.ordinal + 1])
        }
    }

    fun prevStep() {
        val current = _uiState.value.currentStep
        val steps = WizardStep.values()
        if (current.ordinal > 0) {
            goToStep(steps[current.ordinal - 1])
        }
    }

    fun goToStep(step: WizardStep) {
        _uiState.update { it.copy(currentStep = step) }
        saveWizardStateToDisk()
        
        // If transitioning to Step 8 (Histórico) and it is blank, generate it automatically
        if (step == WizardStep.HISTORICO && _uiState.value.historico.isBlank()) {
            generateOccurrenceNarrative()
        }
    }

    // --- Actions ---

    fun toggleNightMode() {
        _uiState.update { it.copy(isNightMode = !it.isNightMode) }
    }

    fun updateInitialData(protocolo: String, data: String, hora: String) {
        _uiState.update { it.copy(protocolo = protocolo, data = data, hora = hora) }
        saveWizardStateToDisk()
    }

    fun updateAddress(rua: String, numero: String, bairro: String, cidade: String, uf: String) {
        _uiState.update { it.copy(rua = rua, numero = numero, bairro = bairro, cidade = cidade, uf = uf) }
        saveWizardStateToDisk()
    }

    fun captureLocation() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            locationService.getCurrentLocation().fold(
                onSuccess = { location ->
                    val (lat, lng) = location
                    _uiState.update { it.copy(latitude = lat, longitude = lng) }
                    locationService.getAddressFromLocation(lat, lng).fold(
                        onSuccess = { address ->
                            _uiState.update { it.copy(
                                rua = address.rua,
                                numero = address.numero,
                                bairro = address.bairro,
                                cidade = address.cidade,
                                uf = address.uf,
                                isLoading = false
                            ) }
                            saveWizardStateToDisk()
                        },
                        onFailure = {
                            _uiState.update { it.copy(isLoading = false) }
                        }
                    )
                },
                onFailure = {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Falha ao obter GPS.") }
                }
            )
        }
    }

    fun selectNatureza(natureza: NaturezaOcorrencia) {
        _uiState.update { it.copy(natureza = natureza) }
        nextStep()
    }

    // --- V3 Viaturas and Militares ---

    fun createPhotoUri(): Uri {
        return cameraCaptureService.createPhotoUri()
    }

    fun addViatura(prefixo: String, tipo: String, kmSaida: Int?, kmLocal: Int?) {
        val newList = _uiState.value.viaturas + Viatura(
            id = UUID.randomUUID().toString(),
            ocorrenciaId = _uiState.value.id ?: "TEMP",
            prefixo = prefixo,
            tipo = tipo,
            kmSaida = kmSaida,
            kmLocal = kmLocal
        )
        _uiState.update { it.copy(viaturas = newList) }
    }

    fun addMilitarToViatura(viaturaId: String, re: String, nomeGuerra: String, graduacao: GraduacaoMilitar, funcao: String?) {
        val updated = _uiState.value.viaturas.map { viatura ->
            if (viatura.id == viaturaId) {
                // Check unique RE
                if (viatura.equipe.any { it.re == re }) return
                viatura.copy(
                    equipe = viatura.equipe + Militar(
                        id = UUID.randomUUID().toString(),
                        viaturaId = viaturaId,
                        re = re,
                        nomeGuerra = nomeGuerra,
                        graduacao = graduacao,
                        funcao = funcao
                    )
                )
            } else viatura
        }
        _uiState.update { it.copy(viaturas = updated) }
    }

    // --- V4 Batch OCR Capture & Auto-Association ---

    fun addPhotoToOcrQueue(uri: Uri) {
        _uiState.update { it.copy(ocrQueueUris = it.ocrQueueUris + uri) }
    }

    fun processOcrBatch(onComplete: () -> Unit) {
        val queue = _uiState.value.ocrQueueUris
        if (queue.isEmpty()) {
            onComplete()
            return
        }

        _uiState.update { it.copy(isOcrProcessing = true, isLoading = true) }
        viewModelScope.launch {
            val results = mutableListOf<OcrBatchResult>()
            queue.forEach { uri ->
                try {
                    // 1. Decode & Treat image
                    val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }

                    if (bitmap != null) {
                        val treatedBitmap = imageProcessingService.processDocumentImage(bitmap)
                        val treatedFile = File(context.cacheDir, "ocr_treated_${UUID.randomUUID()}.jpg")
                        FileOutputStream(treatedFile).use { out ->
                            treatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                        }

                        val treatedUri = FileProvider.getUriForFile(context, "com.example.firenotes.fileprovider", treatedFile)

                        // 2. Recognize text
                        ocrService.recognizeText(treatedUri).onSuccess { result ->
                            // Check duplicates
                            val cpf = result.extractedFields["cpf"]
                            val isDuplicate = results.any { it.extractedFields["cpf"] == cpf }

                            // Map parsed person details
                            val parsedPerson = if (result.tipo != "CRLV" && !cpf.isNullOrBlank()) {
                                Pessoa(
                                    id = UUID.randomUUID().toString(),
                                    nome = result.extractedFields["nome"] ?: "DOCUMENTO EXTRAÍDO",
                                    cpf = cpf,
                                    rg = result.extractedFields["rg"],
                                    nascimento = result.extractedFields["nascimento"],
                                    filiacao = result.extractedFields["filiacao"],
                                    cidade = _uiState.value.cidade,
                                    uf = _uiState.value.uf
                                )
                            } else null

                            // Map parsed vehicle details
                            val parsedVehicle = if (result.tipo == "CRLV") {
                                val placa = result.extractedFields["placa"] ?: "N/D"
                                VeiculoMaster(
                                    id = UUID.randomUUID().toString(),
                                    placa = placa,
                                    renavam = result.extractedFields["renavam"],
                                    chassi = result.extractedFields["chassi"],
                                    marca = result.extractedFields["marca_modelo"],
                                    modelo = result.extractedFields["marca_modelo"],
                                    cor = result.extractedFields["cor"],
                                    anoModelo = result.extractedFields["ano_modelo"]?.toIntOrNull(),
                                    status = "Ativo"
                                )
                            } else null

                            results.add(
                                OcrBatchResult(
                                    uri = treatedUri,
                                    tipo = result.tipo,
                                    rawText = result.rawText,
                                    extractedFields = result.extractedFields,
                                    fieldsWithConfidence = result.fieldsWithConfidence.mapValues { it.value.confidence },
                                    parsedPerson = parsedPerson,
                                    parsedVehicle = parsedVehicle,
                                    isDuplicate = isDuplicate
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    // Skip failed OCR items silently to prevent flow blockage
                }
            }

            // Execute V5 Auto Associations
            // CNH & CRLV match: if CRLV owner/fields match a CNH person CPF, associate them automatically!
            val updatedVehicles = mutableListOf<VeiculoEnvolvido>()
            results.forEach { res ->
                res.parsedVehicle?.let { vMaster ->
                    // Try to find matching owner in processed persons
                    val matchedOwner = results.find { it.parsedPerson != null && it.extractedFields["nome"]?.contains(res.extractedFields["proprietario"] ?: "-", ignoreCase = true) == true }?.parsedPerson
                    updatedVehicles.add(
                        VeiculoEnvolvido(
                            id = UUID.randomUUID().toString(),
                            ocorrenciaId = _uiState.value.id ?: "TEMP",
                            veiculoMasterId = vMaster.id,
                            proprietarioId = matchedOwner?.id,
                            placa = vMaster.placa,
                            cor = vMaster.cor,
                            chassi = vMaster.chassi,
                            modelo = vMaster.modelo,
                            ano = vMaster.anoModelo,
                            renavam = vMaster.renavam
                        )
                    )
                }
            }

            _uiState.update { state ->
                state.copy(
                    ocrCompletedResults = results,
                    ocrQueueUris = emptyList(),
                    isOcrProcessing = false,
                    isLoading = false,
                    veiculos = state.veiculos + updatedVehicles
                )
            }
            onComplete()
        }
    }

    // --- Step 5: Veículos ---

    fun addManualVehicle(placa: String, modelo: String, cor: String, chassi: String, ano: Int?) {
        val newV = VeiculoEnvolvido(
            id = UUID.randomUUID().toString(),
            ocorrenciaId = _uiState.value.id ?: "TEMP",
            placa = placa,
            modelo = modelo,
            cor = cor,
            chassi = chassi,
            ano = ano
        )
        _uiState.update { it.copy(veiculos = it.veiculos + newV) }
    }

    fun associateOwnerToVehicle(vehicleId: String, personId: String) {
        val updated = _uiState.value.veiculos.map {
            if (it.id == vehicleId) it.copy(proprietarioId = personId) else it
        }
        _uiState.update { it.copy(veiculos = updated) }
    }

    fun associateDriverToVehicle(vehicleId: String, personId: String) {
        val updated = _uiState.value.veiculos.map {
            if (it.id == vehicleId) it.copy(condutorId = personId) else it
        }
        _uiState.update { it.copy(veiculos = updated) }
    }

    // --- Step 6: Vítimas ---

    fun addVictim(
        personId: String,
        lesoes: String,
        glasgow: Int?,
        pa: String,
        pulso: Int?,
        satO2: Int?,
        temp: Double?,
        hospital: String?,
        viaturaSocorroId: String?,
        resultado: String?
    ) {
        val person = getParsedPersons().find { it.id == personId }
        val birthDateStr = person?.nascimento
        val age = if (birthDateStr != null) {
            try {
                val parts = birthDateStr.split("/")
                val birthDate = if (parts.size == 3) {
                    LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
                } else {
                    LocalDate.parse(birthDateStr)
                }
                java.time.Period.between(birthDate, LocalDate.now()).years
            } catch (e: java.lang.Exception) {
                null
            }
        } else null

        val newV = Vitima(
            id = UUID.randomUUID().toString(),
            ocorrenciaId = _uiState.value.id ?: "TEMP",
            nome = person?.nome ?: "Desconhecido",
            idade = age,
            lesoesAparentes = lesoes,
            sinaisVitais = SinaisVitais(pulso = pulso, pressaoArterial = pa, saturacaoO2 = satO2, temperatura = temp, escalaGCS = glasgow),
            pessoaId = personId,
            viaturaSocorroId = viaturaSocorroId,
            hospitalDestino = hospital,
            resultadoOcorrencia = resultado
        )
        _uiState.update { it.copy(vitimas = it.vitimas + newV) }
    }

    fun getParsedPersons(): List<Pessoa> {
        return _uiState.value.ocrCompletedResults.mapNotNull { it.parsedPerson }
    }

    // --- Step 7: Evidências ---

    fun addEvidenciaCena(uri: Uri, classification: String) {
        val newEv = EvidenciaCena(
            uri = uri,
            classification = classification,
            timestamp = Instant.now().toString(),
            latitude = _uiState.value.latitude,
            longitude = _uiState.value.longitude
        )
        _uiState.update { it.copy(evidencias = it.evidencias + newEv) }
    }

    // --- Step 8: Histórico ---

    fun updateHistorico(text: String) {
        _uiState.update { it.copy(historico = text) }
    }

    fun generateOccurrenceNarrative() {
        val state = _uiState.value
        val text = narrativeService.generateNarrative(
            natureza = state.natureza,
            veiculos = state.veiculos,
            vitimas = state.vitimas,
            viaturas = state.viaturas,
            apoios = emptyList(),
            dataHora = "${state.data} ${state.hora}",
            endereco = "${state.rua}, ${state.numero} - ${state.cidade}/${state.uf}",
            resultado = "Atendimento finalizado no local"
        )
        _uiState.update { it.copy(historico = text) }
    }

    // --- Step 9: Finalize & Submit ---

    fun submitWizardOccurrence(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (!validator.isWizardComplete(state)) {
            _uiState.update { it.copy(errorMessage = "Pendências obrigatórias identificadas no checklist final.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                // 1. Create main occurrence
                val occurrence = Ocorrencia(
                    id = UUID.randomUUID().toString(),
                    protocolo = state.protocolo,
                    natureza = state.natureza,
                    latitude = state.latitude,
                    longitude = state.longitude,
                    dataHora = Instant.now(),
                    historico = state.historico,
                    rua = state.rua,
                    numero = state.numero,
                    bairro = state.bairro,
                    cidade = state.cidade,
                    uf = state.uf
                )
                repository.createOcorrencia(occurrence).getOrThrow()

                // 2. Insert Persons & Documents
                state.ocrCompletedResults.forEach { result ->
                    result.parsedPerson?.let { person ->
                        repository.upsertPessoa(person).getOrThrow()

                        // Create Document
                        val doc = Documento(
                            ocorrenciaId = occurrence.id!!,
                            pessoaId = person.id,
                            tipo = result.tipo,
                            numero = result.extractedFields["registro"] ?: result.extractedFields["rg"] ?: "N/D",
                            urlImagem = "LocalUri: ${result.uri}",
                            textoOcr = result.rawText,
                            dadosEstruturados = result.extractedFields,
                            hashArquivo = UUID.randomUUID().toString(),
                            dataUpload = Instant.now().toString(),
                            usuario = "Operador"
                        )
                        repository.addDocumento(doc).getOrThrow()
                    }
                }

                // 3. Insert Viaturas and Militares
                state.viaturas.forEach { viatura ->
                    val savedV = repository.addViatura(viatura.copy(ocorrenciaId = occurrence.id!!)).getOrThrow()
                    viatura.equipe.forEach { militar ->
                        repository.addMilitar(militar.copy(viaturaId = savedV.id!!)).getOrThrow()
                    }
                }

                // 4. Insert Veículos
                state.veiculos.forEach { veiculo ->
                    repository.addVeiculoEnvolvido(veiculo.copy(ocorrenciaId = occurrence.id!!)).getOrThrow()
                }

                // 5. Insert Vítimas
                state.vitimas.forEach { vitima ->
                    repository.addVitima(vitima.copy(ocorrenciaId = occurrence.id!!)).getOrThrow()
                }

                // 6. Insert Evidencias
                state.evidencias.forEach { evidence ->
                    val fileBytes = getFileBytes(evidence.uri)
                    var storageUrl = "LocalUri: ${evidence.uri}"
                    if (fileBytes != null) {
                        val path = "${occurrence.id}/evidencias/evidence_${System.currentTimeMillis()}.jpg"
                        repository.uploadFile("ocorrencias", path, fileBytes).onSuccess { url ->
                            storageUrl = url
                        }
                    }
                    val ev = Evidencia(
                        ocorrenciaId = occurrence.id!!,
                        tipo = evidence.classification,
                        hashSha256 = UUID.randomUUID().toString(),
                        latitude = evidence.latitude,
                        longitude = evidence.longitude,
                        dataHora = evidence.timestamp,
                        usuario = "Operador",
                        urlStorage = storageUrl
                    )
                    repository.addEvidencia(ev).getOrThrow()
                }

                // 7. Write Timeline Event
                repository.addTimelineEvent(
                    TimelineEvent(
                        ocorrenciaId = occurrence.id!!,
                        evento = "Encerramento",
                        descricao = "Ocorrência encerrada via Modo Assistido",
                        dataHora = Instant.now().toString()
                    )
                ).getOrThrow()

                // 8. Write Audit Log
                repository.logAudit(
                    AuditLog(
                        ocorrenciaId = occurrence.id,
                        usuario = "Operador",
                        dataHora = Instant.now().toString(),
                        tabelaAlterada = "ocorrencias",
                        campoAlterado = "status",
                        valorAnterior = "Aberto",
                        valorNovo = "Encerrado"
                    )
                ).getOrThrow()

                // Clear recovery state
                clearWizardStateFromDisk()
                _uiState.update { it.copy(isLoading = false) }
                onSuccess()

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Erro ao finalizar ocorrência: ${e.localizedMessage}") }
            }
        }
    }

    private fun getFileBytes(uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.readBytes()
            }
        } catch (e: Exception) {
            null
        }
    }
}
