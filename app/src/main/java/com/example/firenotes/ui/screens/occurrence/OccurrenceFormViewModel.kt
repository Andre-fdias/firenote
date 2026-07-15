package com.example.firenotes.ui.screens.occurrence

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firenotes.domain.model.*
import com.example.firenotes.domain.ocr.OCREngine
import com.example.firenotes.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import com.example.firenotes.ui.screens.occurrence.models.FormStage
import com.example.firenotes.data.local.dao.HomeOperationalDao
import com.example.firenotes.data.local.entities.RoomProntidaoDia
import com.example.firenotes.util.LogHelper
// Adicionar no topo do arquivo
import com.example.firenotes.data.service.ProntidaoService


// ============================================
// LOGS PADRONIZADOS
// ============================================

private const val LOG_TAG = "FireOccurrence"
private fun logD(message: String) = LogHelper.d(LOG_TAG, message)
private fun logE(message: String, throwable: Throwable? = null) = LogHelper.e(LOG_TAG, message, throwable)
private fun logW(message: String) = LogHelper.w(LOG_TAG, message)

// ============================================
// UI STATE
// ============================================



data class OccurrenceFormUiState(
    val id: String? = null,
    val protocolo: String = "",
    val data: String = "",
    val hora: String = "",
    val natureza: NaturezaOcorrencia = NaturezaOcorrencia.INDEFINIDA,
    val subNaturezaSelecionada: String? = null,
    val isSaved: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val rua: String = "",
    val numero: String = "",
    val bairro: String = "",
    val cidade: String = "",
    val uf: String = "",
    val isGpsLoading: Boolean = false,
    val historico: String = "",
    val fotos: List<String> = emptyList(),
    val videos: List<String> = emptyList(),
    val orgaosDisponiveis: List<OrgaoApoio> = emptyList(),
    val apoiosDetalhados: List<ApoioOcorrencia> = emptyList(),
    val pessoas: List<Pessoa> = emptyList(),
    val documentos: List<Documento> = emptyList(),
    val veiculos: List<VeiculoEnvolvido> = emptyList(),
    val vitimas: List<Vitima> = emptyList(),
    val viaturas: List<Viatura> = emptyList(),
    val evidencias: List<Evidencia> = emptyList(),
    val formStage: FormStage = FormStage.INITIAL_DATA,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSavingSuccess: Boolean = false,
    val errorMessage: String? = null,
    val operationProgress: Float = 0f,
    val operationMessage: String? = null,
    val prontidaoColor: String = "VERDE"
)

// ============================================
// VIEWMODEL PRINCIPAL
// ============================================

@HiltViewModel
class OccurrenceFormViewModel @Inject constructor(
    private val repository: OcorrenciaRepository,
    private val locationService: LocationService,
    private val ocrService: OcrService,
    private val cameraCaptureService: CameraCaptureService,
    private val imageProcessingService: ImageProcessingService,
    private val ocrEngine: OCREngine,
    private val homeOperationalDao: HomeOperationalDao,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(OccurrenceFormUiState())
    val uiState: StateFlow<OccurrenceFormUiState> = _uiState.asStateFlow()

    // ============================================
    // INICIALIZAÇÃO
    // ============================================

    init {
        logD("ViewModel inicializado")
        initializeDefaultValues()
        loadOrgaosApoio()
        loadProntidaoForDate(_uiState.value.data)
    }

    private fun initializeDefaultValues() {
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        val nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

        _uiState.update {
            it.copy(
                data = today,
                hora = nowTime
            )
        }
        logD("Valores padrão: data=$today, hora=$nowTime")
    }

    // ============================================
    // CARREGAMENTO DE DADOS
    // ============================================

    fun loadOccurrence(occurrenceId: String, showLoading: Boolean = true) {
        logD("Carregando ocorrência: $occurrenceId")
        if (showLoading) setLoading(true)

        viewModelScope.launch {
            try {
                val occurrence = repository.getOcorrenciaById(occurrenceId).getOrThrow()
                val viaturas = repository.getViaturasDaOcorrencia(occurrenceId).getOrDefault(emptyList())
                val pessoas = repository.getPessoasDaOcorrencia(occurrenceId).getOrDefault(emptyList())
                val documentos = repository.getDocumentosDaOcorrencia(occurrenceId).getOrDefault(emptyList())
                val evidencias = repository.getEvidencias(occurrenceId).getOrDefault(emptyList())

                _uiState.update {
                    it.copy(
                        id = occurrence.id,
                        protocolo = occurrence.protocolo,
                        data = formatDate(occurrence.dataHora),
                        hora = formatTime(occurrence.dataHora),
                        natureza = occurrence.natureza,
                        isSaved = true,
                        latitude = occurrence.latitude,
                        longitude = occurrence.longitude,
                        rua = occurrence.rua ?: "",
                        numero = occurrence.numero ?: "",
                        bairro = occurrence.bairro ?: "",
                        cidade = occurrence.cidade ?: "",
                        uf = occurrence.uf ?: "",
                        historico = occurrence.historico ?: "",
                        fotos = occurrence.fotos,
                        viaturas = viaturas,
                        pessoas = pessoas,
                        documentos = documentos,
                        veiculos = occurrence.veiculos,
                        vitimas = occurrence.vitimas,
                        evidencias = evidencias,
                        apoiosDetalhados = occurrence.apoiosDetalhados,
                        formStage = FormStage.TABS,
                        isLoading = false
                    )
                }
                logD("Ocorrência carregada com sucesso")
                loadProntidaoForDate(formatDate(occurrence.dataHora))
            } catch (e: Exception) {
                logE("Erro ao carregar ocorrência", e)
                setError("Erro ao carregar: ${e.localizedMessage}")
                if (showLoading) setLoading(false)
            }
        }
    }

    private fun loadOrgaosApoio() {
        viewModelScope.launch {
            val defaultAgencies = listOf(
                OrgaoApoio("orgao_pm_area", "Polícia Militar - Policiamento de Área", "PM - Policiamento área"),
                OrgaoApoio("orgao_pm_amb", "Polícia Militar - Ambiental", "PM - Ambiental"),
                OrgaoApoio("orgao_pm_choque", "Polícia Militar - Choque", "PM - Choque"),
                OrgaoApoio("orgao_pm_rod", "Polícia Militar - Rodoviária", "PM - Rodoviaria"),
                OrgaoApoio("orgao_prf", "Polícia Rodoviária Federal", "PRF"),
                OrgaoApoio("orgao_pf", "Polícia Federal", "PF"),
                OrgaoApoio("orgao_samu", "Serviço de Atendimento Móvel de Urgência", "SAMU"),
                OrgaoApoio("orgao_gcm", "Guarda Civil Metropolitana", "GCM"),
                OrgaoApoio("orgao_dc", "Defesa Civil", "Defesa Civil"),
                OrgaoApoio("orgao_conces", "Concessionárias de Rodovias", "Concessionárias"),
                OrgaoApoio("orgao_outros", "Outro Órgão/Serviço", "Outros")
            )
            try {
                for (agency in defaultAgencies) {
                    repository.addOrgaoApoio(agency)
                }
            } catch (e: Exception) {
                logE("Erro ao salvar órgãos padrão no banco", e)
            }

            repository.getOrgaosApoio()
                .onSuccess { list ->
                    val sortedList = defaultAgencies.mapNotNull { def -> list.find { it.id == def.id } }
                    _uiState.update { it.copy(orgaosDisponiveis = if (sortedList.isNotEmpty()) sortedList else defaultAgencies) }
                }
                .onFailure { error ->
                    logW("Erro ao carregar órgãos de apoio, usando fallback: ${error.message}")
                    _uiState.update { it.copy(orgaosDisponiveis = defaultAgencies) }
                }
        }
    }

    private fun getFallbackOrgaos(): List<OrgaoApoio> {
        return listOf(
            OrgaoApoio("1", "Polícia Rodoviária Federal", "PRF"),
            OrgaoApoio("2", "Corpo de Bombeiros Militar", "CBM"),
            OrgaoApoio("3", "Polícia Militar", "PM"),
            OrgaoApoio("4", "Serviço de Atendimento Móvel de Urgência", "SAMU"),
            OrgaoApoio("5", "Defesa Civil", "DC")
        )
    }

    // ============================================
    // FUNÇÕES PÚBLICAS - ENDEREÇO E LOCALIZAÇÃO
    // ============================================

    fun updateInitialFields(talao: String, data: String, hora: String) {
        val oldData = _uiState.value.data
        _uiState.update { it.copy(protocolo = talao, data = data, hora = hora) }
        saveOccurrenceDraft()
        if (data != oldData) {
            loadProntidaoForDate(data)
        }
    }

    fun updateProntidao(escala: String) {
        val dateStr = _uiState.value.data
        val dbDate = convertDateToDatabaseFormat(dateStr)
        if (dbDate.isNotEmpty()) {
            _uiState.update { it.copy(prontidaoColor = escala) }
            viewModelScope.launch {
                try {
                    homeOperationalDao.insertProntidao(RoomProntidaoDia(dbDate, escala))
                    logD("Prontidao de servico atualizada para $escala na data $dbDate")
                } catch (e: Exception) {
                    logE("Erro ao salvar prontidao: ${e.message}")
                }
            }
        }
    }

    fun loadProntidaoForDate(dateStr: String) {
        viewModelScope.launch {
            try {
                val dbDate = convertDateToDatabaseFormat(dateStr)
                if (dbDate.isNotEmpty()) {
                    val pront = homeOperationalDao.getProntidaoForDay(dbDate)
                    if (pront != null) {
                        _uiState.update { it.copy(prontidaoColor = pront.escala) }
                    } else {
                        val localDate = LocalDate.parse(dbDate)
                        val defaultPront = ProntidaoService.getProntidaoForDate(localDate)
                        _uiState.update { it.copy(prontidaoColor = defaultPront.name) }
                    }
                }
            } catch (e: Exception) {
                logE("Erro ao carregar prontidao para data $dateStr: ${e.message}")
            }
        }
    }

    private fun convertDateToDatabaseFormat(uiDate: String): String {
        return try {
            val parts = uiDate.split("/")
            if (parts.size == 3) {
                "${parts[2]}-${parts[1]}-${parts[0]}"
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    fun updateManualAddress(rua: String, numero: String, bairro: String, cidade: String, uf: String) {
        _uiState.update { it.copy(rua = rua, numero = numero, bairro = bairro, cidade = cidade, uf = uf) }
        saveOccurrenceDraft()
    }

    fun captureLocationAndAddress() {
        logD("Iniciando captura de localização por GPS")
        _uiState.update { it.copy(isGpsLoading = true, errorMessage = null) }

        viewModelScope.launch {
            locationService.getCurrentLocation()
                .onSuccess { (lat, lng) ->
                    logD("GPS capturado: lat=$lat, lng=$lng")
                    _uiState.update { it.copy(latitude = lat, longitude = lng) }
                    saveOccurrenceDraft()
                    fetchAddressFromLocation(lat, lng)
                }
                .onFailure { error ->
                    logE("Erro GPS", error)
                    _uiState.update { it.copy(isGpsLoading = false, errorMessage = "Erro GPS: ${error.localizedMessage}") }
                }
        }
    }

    private fun fetchAddressFromLocation(lat: Double, lng: Double) {
        viewModelScope.launch {
            locationService.getAddressFromLocation(lat, lng)
                .onSuccess { address ->
                    logD("Endereço reverso obtido: ${address.rua}, ${address.cidade}")
                    _uiState.update {
                        it.copy(
                            isGpsLoading = false,
                            rua = address.rua,
                            numero = address.numero,
                            bairro = address.bairro,
                            cidade = address.cidade,
                            uf = address.uf
                        )
                    }
                    saveOccurrenceDraft()
                }
                .onFailure { error ->
                    logE("Erro ao obter endereço reverso", error)
                    _uiState.update { it.copy(isGpsLoading = false, errorMessage = "Endereço por GPS falhou: ${error.localizedMessage}") }
                }
        }
    }

    // ============================================
    // FUNÇÕES PÚBLICAS - OCORRÊNCIA E NATUREZA
    // ============================================

    // Adicione esta função no OccurrenceFormViewModel.kt
    fun selectNaturezaForCreation(natureza: NaturezaOcorrencia, subNome: String = "") {
        _uiState.update {
            it.copy(
                natureza = natureza,
                subNaturezaSelecionada = subNome.ifBlank { natureza.descricao }
            )
        }
    }

    fun selectNaturezaAndCreateOccurrence(natureza: NaturezaOcorrencia) {
        logD("Criando ocorrência com natureza: $natureza")
        setLoading(true)

        viewModelScope.launch {
            try {
                val occurrence = buildOccurrence(natureza)

                repository.createOcorrencia(occurrence)
                    .onSuccess { saved ->
                        logD("Ocorrência criada: ID=${saved.id}")
                        _uiState.update {
                            it.copy(
                                id = saved.id,
                                natureza = natureza,
                                formStage = FormStage.TABS,
                                isSaved = true,
                                isLoading = false
                            )
                        }
                    }
                    .onFailure { error ->
                        logE("Erro ao criar ocorrência", error)
                        setError("Erro ao criar ocorrência: ${error.localizedMessage}")
                        setLoading(false)
                    }
            } catch (e: Exception) {
                logE("Erro geral ao criar ocorrência", e)
                setError("Erro ao criar ocorrência: ${e.localizedMessage}")
                setLoading(false)
            }
        }
    }

    private fun buildOccurrence(natureza: NaturezaOcorrencia): Ocorrencia {
        val state = _uiState.value
        val instant = parseDateTime("${state.data} ${state.hora}") ?: Instant.now()

        return Ocorrencia(
            protocolo = state.protocolo,
            natureza = natureza,
            latitude = state.latitude,
            longitude = state.longitude,
            dataHora = instant,
            historico = state.historico,
            rua = state.rua,
            numero = state.numero,
            bairro = state.bairro,
            cidade = state.cidade,
            uf = state.uf
        )
    }

    fun updateHistorico(historico: String) {
        _uiState.update { it.copy(historico = historico) }
        saveOccurrenceDraft()
    }

    // ============================================
    // FUNÇÕES PÚBLICAS - OCR
    // ============================================

    fun createPhotoUri(): Uri = cameraCaptureService.createPhotoUri()

    fun processAndRunOcrDirectly(
        imageUri: Uri,
        onSuccess: (OcrDocumentResult, Uri) -> Unit
    ) {
        logD("Iniciando OCR direto: $imageUri")
        setLoading(true)

        viewModelScope.launch {
            try {
                val bitmap = validateImage(imageUri).getOrElse { error ->
                    logE("Falha na validação de imagem", error)
                    setError("Falha ao ler imagem: ${error.localizedMessage}")
                    setLoading(false)
                    return@launch
                }

                val processedUri = processImage(bitmap).getOrElse { error ->
                    logE("Falha ao processar imagem", error)
                    setError("Erro no processamento: ${error.localizedMessage}")
                    setLoading(false)
                    return@launch
                }

                val rawOcrResult = runOcr(processedUri).getOrElse { error ->
                    logE("Falha no OCR", error)
                    setError("Erro no OCR: ${error.localizedMessage}")
                    setLoading(false)
                    return@launch
                }

                val ocrEngineResult = ocrEngine.process(rawOcrResult.rawText)
                val compatibleResult = mapOcrEngineResultToOcrDocumentResult(ocrEngineResult)

                logD("OCR finalizado. Tipo: ${compatibleResult.tipo}")
                setLoading(false)
                onSuccess(compatibleResult, processedUri)

            } catch (e: Exception) {
                logE("Erro no fluxo OCR", e)
                setError("Erro no OCR: ${e.localizedMessage}")
                setLoading(false)
            }
        }
    }

    private suspend fun validateImage(uri: Uri): Result<Bitmap> {
        return try {
            val fileSize = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L
            if (fileSize <= 0) return Result.failure(Exception("Arquivo vazio"))

            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            } ?: return Result.failure(Exception("Falha ao decodificar imagem"))

            Result.success(bitmap)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun processImage(bitmap: Bitmap): Result<Uri> {
        return try {
            val processedBitmap = imageProcessingService.processDocumentImage(bitmap)
            val file = java.io.File(context.cacheDir, "ocr_processed_${System.currentTimeMillis()}.jpg")
            java.io.FileOutputStream(file).use { out ->
                processedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "com.example.firenotes.fileprovider",
                file
            )
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun runOcr(uri: Uri): Result<OcrDocumentResult> {
        return ocrService.recognizeText(uri)
    }

    private fun mapOcrEngineResultToOcrDocumentResult(result: OCREngine.OCRResult): OcrDocumentResult {
        val extractedFields = result.campos.toMutableMap()

        // Mapeamentos de compatibilidade
        extractedFields["nascimento"] = extractedFields["data_nascimento"] ?: ""

        val marca = extractedFields["marca"] ?: ""
        val modelo = extractedFields["modelo"] ?: ""
        val combined = "$marca $modelo".trim()
        if (combined.isNotEmpty()) {
            extractedFields["marca_modelo"] = combined
        }
        extractedFields["numero"] = extractedFields["registro"] ?: ""

        val fieldsWithConfidence = extractedFields.mapValues { (key, value) ->
            val origKey = when (key) {
                "nascimento" -> "data_nascimento"
                "marca_modelo" -> "modelo"
                else -> key
            }
            val confidenceInt = result.confianca[origKey] ?: 70
            OcrField(
                value = value,
                confidence = confidenceInt / 100.0f,
                isPendingReview = confidenceInt < 80
            )
        }

        return OcrDocumentResult(
            tipo = result.tipo.name,
            rawText = result.textoOriginal,
            extractedFields = extractedFields,
            fieldsWithConfidence = fieldsWithConfidence
        )
    }

    // ============================================
    // FUNÇÕES PÚBLICAS - DOCUMENTOS E PESSOAS
    // ============================================

    fun saveDocument(
        tipo: String,
        numero: String,
        extractedFields: Map<String, String>,
        rawText: String,
        imageUri: Uri
    ) {
        val occurrenceId = _uiState.value.id ?: return
        logD("Salvando documento: tipo=$tipo, numero=$numero")
        setLoading(true)

        viewModelScope.launch {
            try {
                val pessoa = buildPessoaFromDocument(tipo, extractedFields)
                val savedPessoa = repository.upsertPessoa(pessoa).getOrThrow()
                logD("Pessoa salva: ID=${savedPessoa.id}")

                val (url, hash) = uploadDocumentImage(occurrenceId, tipo, imageUri)

                val documento = Documento(
                    ocorrenciaId = occurrenceId,
                    pessoaId = savedPessoa.id,
                    tipo = tipo,
                    numero = numero,
                    urlImagem = url ?: "",
                    textoOcr = rawText,
                    dadosEstruturados = extractedFields,
                    hashArquivo = hash ?: "",
                    dataUpload = Instant.now().toString(),
                    usuario = "Operador"
                )

                val savedDoc = repository.addDocumento(documento).getOrThrow()
                logD("Documento salvo: ID=${savedDoc.id}")

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        documentos = state.documentos + savedDoc,
                        pessoas = state.pessoas.filter { it.cpf != savedPessoa.cpf } + savedPessoa
                    )
                }
            } catch (e: Exception) {
                logE("Erro ao salvar documento", e)
                setError("Erro ao salvar documento: ${e.localizedMessage}")
                setLoading(false)
            }
        }
    }

    private fun buildPessoaFromDocument(tipo: String, fields: Map<String, String>): Pessoa {
        return Pessoa(
            nome = fields["nome"] ?: "DESCONHECIDO",
            nomeSocial = fields["nome_social"],
            cpf = fields["cpf"],
            rg = fields["rg"],
            rgOrgaoEmissor = fields["rg_orgao_emissor"],
            rgUf = fields["rg_uf"],
            nascimento = fields["nascimento"],
            naturalidade = fields["naturalidade"],
            nacionalidade = fields["nacionalidade"],
            filiacao = fields["filiacao"]
        )
    }

    private suspend fun uploadDocumentImage(
        occurrenceId: String,
        tipo: String,
        imageUri: Uri
    ): Pair<String?, String?> {
        val isLauncherResource = imageUri.toString().contains("ic_launcher_foreground")
        if (imageUri == Uri.EMPTY || isLauncherResource) return null to null

        val bytes = getFileBytes(imageUri).getOrThrow()
        val path = "$occurrenceId/documentos/${tipo}_${System.currentTimeMillis()}.png"
        val url = repository.uploadFile("ocorrencias", path, bytes).getOrThrow()
        val hash = UUID.nameUUIDFromBytes(bytes).toString()
        return url to hash
    }

    fun deleteDocumento(id: String) {
        logD("Deletando documento: $id")
        setLoading(true)

        viewModelScope.launch {
            repository.deleteDocumento(id)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(isLoading = false, documentos = state.documentos.filter { it.id != id })
                    }
                }
                .onFailure { error ->
                    logE("Erro ao deletar documento", error)
                    setError("Erro ao deletar: ${error.localizedMessage}")
                    setLoading(false)
                }
        }
    }

    // ============================================
    // FUNÇÕES PÚBLICAS - VEÍCULOS
    // ============================================

    fun saveVeiculo(
        placa: String,
        modelo: String,
        cor: String,
        chassi: String,
        ano: String,
        proprietarioId: String?,
        extractedFields: Map<String, String> = emptyMap(),
        rawText: String = "",
        imageUri: Uri? = null
    ) {
        val occurrenceId = _uiState.value.id ?: return
        logD("Salvando veículo: placa=$placa, modelo=$modelo")
        setLoading(true)

        viewModelScope.launch {
            try {
                val urlCrlv = if (imageUri != null && imageUri != Uri.EMPTY) {
                    val bytes = getFileBytes(imageUri).getOrThrow()
                    val path = "$occurrenceId/veiculos/crlv_${System.currentTimeMillis()}.png"
                    repository.uploadFile("ocorrencias", path, bytes).getOrThrow()
                } else null

                val veiculo = VeiculoEnvolvido(
                    ocorrenciaId = occurrenceId,
                    placa = placa,
                    cor = cor,
                    chassi = chassi,
                    modelo = modelo,
                    ano = ano,
                    proprietarioId = proprietarioId,
                    renavam = extractedFields["renavam"],
                    marca = extractedFields["marca_modelo"] ?: extractedFields["marca"] ?: "",
                    versao = extractedFields["marca_modelo"] ?: extractedFields["versao"] ?: "",
                    anoFabricacao = extractedFields["ano_fabricacao"]?.toIntOrNull(),
                    anoModelo = extractedFields["ano_modelo"]?.toIntOrNull(),
                    exercicio = extractedFields["exercicio"] ?: "",
                    urlCrlv = urlCrlv,
                    ocrTextoCrlv = rawText,
                    ocrDadosEstruturados = extractedFields
                )

                val saved = repository.addVeiculoEnvolvido(veiculo).getOrThrow()
                logD("Veículo salvo: ID=${saved.id}")

                _uiState.update { state ->
                    state.copy(isLoading = false, veiculos = state.veiculos + saved)
                }
            } catch (e: Exception) {
                logE("Erro ao salvar veículo", e)
                setError("Erro ao salvar veículo: ${e.localizedMessage}")
                setLoading(false)
            }
        }
    }

    fun deleteVeiculo(id: String) {
        logD("Deletando veículo: $id")
        setLoading(true)

        viewModelScope.launch {
            repository.deleteVeiculo(id)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(isLoading = false, veiculos = state.veiculos.filter { it.id != id })
                    }
                }
                .onFailure { error ->
                    logE("Erro ao deletar veículo", error)
                    setError("Erro ao deletar: ${error.localizedMessage}")
                    setLoading(false)
                }
        }
    }

    // ============================================
    // FUNÇÕES PÚBLICAS - VÍTIMAS
    // ============================================

    fun saveVitima(
        pessoaId: String,
        lesoes: String,
        lesoesEstruturadas: List<com.example.firenotes.domain.model.Lesao>,
        destino: String,
        quemSocorreu: String,
        resultado: String,
        viaturaSocorroId: String?,
        hospitalDestino: String,
        nomeMedico: String,
        crmMedico: String,
        pulso: Int?,
        pa: String,
        satO2: Int?,
        aberturaOcular: Int?,
        respostaVerbal: Int?,
        respostaMotora: Int?,
        respiracao: Int?
    ) {
        val occurrenceId = _uiState.value.id ?: return
        val person = _uiState.value.pessoas.find { it.id == pessoaId } ?: return
        logD("Registrando vítima: ${person.nome}")
        setLoading(true)

        viewModelScope.launch {
            try {
                val idade = calculateAge(person.nascimento)
                // Calcular GCS total a partir dos sub-domínios
                val gcsTotal = if (aberturaOcular != null && respostaVerbal != null && respostaMotora != null) {
                    aberturaOcular + respostaVerbal + respostaMotora
                } else null

                val vitima = com.example.firenotes.domain.model.Vitima(
                    ocorrenciaId = occurrenceId,
                    nome = person.nome,
                    idade = idade,
                    pessoaId = pessoaId,
                    lesoes = lesoes,
                    lesoesEstruturadas = lesoesEstruturadas,
                    destinoSocorro = destino,
                    quemSocorreu = quemSocorreu,
                    resultadoOcorrencia = resultado,
                    viaturaSocorroId = viaturaSocorroId,
                    hospitalDestino = hospitalDestino,
                    nomeMedico = nomeMedico,
                    crmMedico = crmMedico,
                    sinaisVitais = com.example.firenotes.domain.model.SinaisVitais(
                        pulso = pulso,
                        pressaoArterial = pa,
                        saturacaoO2 = satO2,
                        escalaGCS = gcsTotal,
                        aberturaOcular = aberturaOcular,
                        respostaVerbal = respostaVerbal,
                        respostaMotora = respostaMotora,
                        respiracao = respiracao
                    )
                )

                val saved = repository.addVitima(vitima).getOrThrow()
                logD("Vítima salva: ID=${saved.id}")

                _uiState.update { state ->
                    state.copy(isLoading = false, vitimas = state.vitimas + saved)
                }
            } catch (e: Exception) {
                logE("Erro ao registrar vítima", e)
                setError("Erro ao registrar vítima: ${e.localizedMessage}")
                setLoading(false)
            }
        }
    }

    fun calculateAge(birthDateStr: String?): Int? {
        if (birthDateStr.isNullOrBlank()) return null

        return try {
            val parts = birthDateStr.split("/")
            val birthDate = if (parts.size == 3) {
                LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
            } else {
                LocalDate.parse(birthDateStr)
            }
            java.time.Period.between(birthDate, LocalDate.now()).years
        } catch (e: Exception) {
            logW("Erro ao calcular idade: $birthDateStr")
            null
        }
    }

    // ============================================
    // FUNÇÕES PÚBLICAS - VIATURAS E MILITARES
    // ============================================

    fun addViatura(
        prefixo: String,
        tipo: String,
        unidade: String?,
        kmSaida: Int?,
        kmLocal: Int?,
        observacoes: String?,
        viaturaId: String? = null
    ) {
        val occurrenceId = _uiState.value.id ?: run {
            setError("Ocorrência não salva. Salve a ocorrência primeiro.")
            return
        }

        if (prefixo.isBlank()) {
            setError("O prefixo da viatura é obrigatório.")
            return
        }

        logD("Adicionando viatura: prefixo=$prefixo")
        setLoading(true)

        viewModelScope.launch {
            try {
                val finalViaturaId = viaturaId ?: UUID.randomUUID().toString()
                val existing = _uiState.value.viaturas.find { it.id == finalViaturaId }

                val viatura = Viatura(
                    id = finalViaturaId,
                    ocorrenciaId = occurrenceId,
                    prefixo = prefixo,
                    tipo = tipo,
                    unidade = unidade ?: "",
                    kmSaida = kmSaida,
                    kmLocal = kmLocal,
                    observacoes = observacoes ?: "",
                    equipe = existing?.equipe ?: emptyList()
                )

                val saved = repository.addViatura(viatura).getOrThrow()
                logD("Viatura salva: ID=${saved.id}")

                _uiState.update { state ->
                    val updatedList = if (viaturaId != null) {
                        state.viaturas.map { if (it.id == viaturaId) saved else it }
                    } else {
                        state.viaturas + saved
                    }
                    state.copy(isLoading = false, viaturas = updatedList)
                }
            } catch (e: Exception) {
                logE("Erro ao adicionar viatura", e)
                setError("Erro ao adicionar viatura: ${e.localizedMessage}")
                setLoading(false)
            }
        }
    }

    fun deleteViatura(viaturaId: String) {
        logD("Deletando viatura: $viaturaId")
        setLoading(true)

        viewModelScope.launch {
            repository.deleteViatura(viaturaId)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(isLoading = false, viaturas = state.viaturas.filter { it.id != viaturaId })
                    }
                }
                .onFailure { error ->
                    logE("Erro ao deletar viatura", error)
                    setError("Erro ao deletar: ${error.localizedMessage}")
                    setLoading(false)
                }
        }
    }

    fun addMilitar(
        viaturaId: String,
        re: String,
        nomeGuerra: String,
        graduacaoStr: String,
        funcao: String?
    ) {
        if (re.isBlank() || nomeGuerra.isBlank()) {
            setError("RE e Nome de Guerra são obrigatórios.")
            return
        }

        val viatura = _uiState.value.viaturas.find { it.id == viaturaId }
        if (viatura == null) {
            setError("Viatura não encontrada. Adicione a viatura primeiro.")
            return
        }

        if (viatura.ocorrenciaId.isBlank() || viatura.ocorrenciaId == "TEMP") {
            setError("Viatura não está vinculada a uma ocorrência. Salve a ocorrência primeiro.")
            return
        }

        logD("Adicionando militar: RE=$re, nome=$nomeGuerra")
        setLoading(true)

        viewModelScope.launch {
            try {
                val militar = Militar(
                    id = UUID.randomUUID().toString(),
                    viaturaId = viaturaId,
                    re = re,
                    nomeGuerra = nomeGuerra,
                    graduacao = graduacaoStr,
                    funcao = funcao ?: ""
                )

                val saved = repository.addMilitar(militar).getOrThrow()
                logD("Militar salvo: ID=${saved.id}")

                _uiState.update { state ->
                    val updatedViaturas = state.viaturas.map { v ->
                        if (v.id == viaturaId) {
                            val updatedEquipe = (v.equipe + saved)
                                .sortedBy { it.graduacao.substringBefore(" - ").toIntOrNull() ?: 99 }
                            v.copy(equipe = updatedEquipe)
                        } else {
                            v
                        }
                    }
                    state.copy(isLoading = false, viaturas = updatedViaturas)
                }
            } catch (e: Exception) {
                logE("Erro ao adicionar militar", e)
                setError("Erro ao adicionar militar: ${e.localizedMessage}")
                setLoading(false)
            }
        }
    }

    fun deleteMilitar(militarId: String, viaturaId: String) {
        logD("Removendo militar: $militarId")
        setLoading(true)

        viewModelScope.launch {
            repository.deleteMilitar(militarId)
                .onSuccess {
                    _uiState.update { state ->
                        val updated = state.viaturas.map { v ->
                            if (v.id == viaturaId) {
                                v.copy(equipe = v.equipe.filter { it.id != militarId })
                            } else {
                                v
                            }
                        }
                        state.copy(isLoading = false, viaturas = updated)
                    }
                }
                .onFailure { error ->
                    logE("Erro ao deletar militar", error)
                    setError("Erro ao deletar: ${error.localizedMessage}")
                    setLoading(false)
                }
        }
    }

    fun moveMilitar(militarId: String, currentViaturaId: String, newViaturaId: String) {
        logD("Movendo militar: $militarId de $currentViaturaId para $newViaturaId")
        setLoading(true)

        viewModelScope.launch {
            repository.moveMilitar(militarId, newViaturaId)
                .onSuccess {
                    val occurrenceId = _uiState.value.id ?: return@launch
                    repository.getViaturasDaOcorrencia(occurrenceId)
                        .onSuccess { list ->
                            _uiState.update { it.copy(isLoading = false, viaturas = list) }
                        }
                        .onFailure { error ->
                            logE("Erro ao recarregar viaturas", error)
                            setError("Erro ao atualizar lista: ${error.localizedMessage}")
                            setLoading(false)
                        }
                }
                .onFailure { error ->
                    logE("Erro ao mover militar", error)
                    setError("Erro ao mover: ${error.localizedMessage}")
                    setLoading(false)
                }
        }
    }

    // ============================================
    // FUNÇÕES PÚBLICAS - APOIOS
    // ============================================

    fun addApoio(orgaoSigla: String, orgaoNome: String, viatura: String, encarregado: String, descricaoOutros: String) {
        val occurrenceId = _uiState.value.id ?: return
        logD("Adicionando apoio: $orgaoSigla")

        viewModelScope.launch {
            val orgaos = repository.getOrgaosApoio().getOrDefault(emptyList())
            val matched = orgaos.find { it.sigla == orgaoSigla }
            val orgaoId = matched?.id ?: "orgao_outros"

            repository.vincularOrgaoApoioDetalhado(occurrenceId, orgaoId, viatura, encarregado, descricaoOutros)
                .onSuccess {
                    val newApoio = ApoioOcorrencia(
                        id = UUID.randomUUID().toString(),
                        ocorrenciaId = occurrenceId,
                        orgaoId = orgaoId,
                        orgaoSigla = orgaoSigla,
                        orgaoNome = orgaoNome,
                        viatura = viatura,
                        encarregado = encarregado,
                        descricaoOutros = descricaoOutros
                    )
                    _uiState.update { state ->
                        state.copy(apoiosDetalhados = state.apoiosDetalhados + newApoio)
                    }
                }
                .onFailure { error ->
                    logE("Erro ao adicionar apoio", error)
                    setError("Erro ao adicionar apoio: ${error.localizedMessage}")
                }
        }
    }

    fun removeApoio(index: Int) {
        val occurrenceId = _uiState.value.id ?: return
        val apoio = _uiState.value.apoiosDetalhados.getOrNull(index) ?: return
        logD("Removendo apoio: ${apoio.orgaoSigla}")

        viewModelScope.launch {
            repository.desvincularOrgaoApoio(occurrenceId, apoio.orgaoId)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(apoiosDetalhados = state.apoiosDetalhados.filterIndexed { idx, _ -> idx != index })
                    }
                }
                .onFailure { error ->
                    logE("Erro ao remover apoio", error)
                    setError("Erro ao remover apoio: ${error.localizedMessage}")
                }
        }
    }

    // ============================================
    // FUNÇÕES PÚBLICAS - MÍDIAS
    // ============================================

    fun uploadOccurrenceFile(uri: Uri, isVideo: Boolean = false) {
        val occurrenceId = _uiState.value.id ?: return
        logD("Upload de mídia: isVideo=$isVideo")
        setLoading(true)

        viewModelScope.launch {
            getFileBytes(uri)
                .onSuccess { bytes ->
                    val typeStr = if (isVideo) "video" else "foto"
                    val path = "$occurrenceId/imagens/${typeStr}_${System.currentTimeMillis()}.png"

                    repository.uploadFile("ocorrencias", path, bytes)
                        .onSuccess { publicUrl ->
                            logD("Upload concluído: $publicUrl")
                            _uiState.update { state ->
                                if (isVideo) {
                                    state.copy(videos = state.videos + publicUrl, isLoading = false)
                                } else {
                                    state.copy(fotos = state.fotos + publicUrl, isLoading = false)
                                }
                            }
                        }
                        .onFailure { error ->
                            logE("Erro no upload", error)
                            setError("Erro upload: ${error.localizedMessage}")
                            setLoading(false)
                        }
                }
                .onFailure { error ->
                    logE("Erro ao ler arquivo", error)
                    setError("Erro ao ler arquivo: ${error.localizedMessage}")
                    setLoading(false)
                }
        }
    }

    fun removeFoto(path: String) {
        logD("Removendo foto: $path")
        _uiState.update { state -> state.copy(fotos = state.fotos.filter { it != path }) }
        saveOccurrenceDraft()
    }

    // ============================================
    // FUNÇÕES PÚBLICAS - EVIDÊNCIAS
    // ============================================

    fun addEvidencia(uri: Uri, classification: String) {
        val occurrenceId = _uiState.value.id ?: return
        logD("Adicionando evidência: $classification")
        setLoading(true)

        viewModelScope.launch {
            try {
                val bytes = getFileBytes(uri).getOrThrow()
                val path = "$occurrenceId/evidencias/evidence_${System.currentTimeMillis()}.jpg"
                val url = repository.uploadFile("ocorrencias", path, bytes).getOrThrow()

                val ev = Evidencia(
                    ocorrenciaId = occurrenceId,
                    tipo = classification,
                    hashSha256 = UUID.nameUUIDFromBytes(bytes).toString(),
                    latitude = _uiState.value.latitude,
                    longitude = _uiState.value.longitude,
                    dataHora = Instant.now().toString(),
                    usuario = "Operador",
                    urlStorage = url
                )

                val saved = repository.addEvidencia(ev).getOrThrow()
                logD("Evidência salva: ID=${saved.id}")

                _uiState.update { state ->
                    state.copy(isLoading = false, evidencias = state.evidencias + saved)
                }
            } catch (e: Exception) {
                logE("Erro ao adicionar evidência", e)
                setError("Erro ao adicionar evidência: ${e.localizedMessage}")
                setLoading(false)
            }
        }
    }

    fun deleteEvidencia(id: String) {
        logD("Deletando evidência: $id")
        setLoading(true)

        viewModelScope.launch {
            repository.deleteEvidencia(id)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(isLoading = false, evidencias = state.evidencias.filter { it.id != id })
                    }
                }
                .onFailure { error ->
                    logE("Erro ao deletar evidência", error)
                    setError("Erro ao deletar: ${error.localizedMessage}")
                    setLoading(false)
                }
        }
    }

    // ============================================
    // FUNÇÕES PÚBLICAS - FINALIZAÇÃO
    // ============================================

    fun finalizeOccurrence() {
        val occurrenceId = _uiState.value.id ?: return
        logD("Finalizando ocorrência: $occurrenceId")
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val state = _uiState.value
                val occurrence = Ocorrencia(
                    id = occurrenceId,
                    protocolo = state.protocolo,
                    natureza = state.natureza,
                    latitude = state.latitude,
                    longitude = state.longitude,
                    dataHora = Instant.now(),
                    historico = state.historico,
                    fotos = state.fotos,
                    rua = state.rua,
                    numero = state.numero,
                    bairro = state.bairro,
                    cidade = state.cidade,
                    uf = state.uf
                )

                repository.createOcorrencia(occurrence)
                    .onSuccess {
                        logD("Ocorrência finalizada com sucesso")
                        _uiState.update { it.copy(isSaving = false, isSavingSuccess = true, isSaved = true) }
                    }
                    .onFailure { error ->
                        logE("Falha ao finalizar", error)
                        _uiState.update { it.copy(isSaving = false, isSavingSuccess = true, isSaved = true) }
                    }
            } catch (e: Exception) {
                logE("Erro ao finalizar", e)
                _uiState.update { it.copy(isSaving = false, isSavingSuccess = true, isSaved = true) }
            }
        }
    }

    fun saveOccurrenceDraft() {
        val occurrenceId = _uiState.value.id ?: return
        val state = _uiState.value

        viewModelScope.launch {
            try {
                val instant = parseDateTime("${state.data} ${state.hora}") ?: Instant.now()

                val occurrence = Ocorrencia(
                    id = occurrenceId,
                    protocolo = state.protocolo,
                    natureza = state.natureza,
                    latitude = state.latitude,
                    longitude = state.longitude,
                    dataHora = instant,
                    historico = state.historico,
                    fotos = state.fotos,
                    rua = state.rua,
                    numero = state.numero,
                    bairro = state.bairro,
                    cidade = state.cidade,
                    uf = state.uf
                )

                repository.createOcorrencia(occurrence)
                    .onSuccess { logD("Rascunho atualizado") }
                    .onFailure { logE("Erro ao salvar rascunho", it) }
            } catch (e: Exception) {
                logE("Erro ao salvar rascunho", e)
            }
        }
    }

    // ============================================
    // UTILITÁRIOS PRIVADOS
    // ============================================

    private fun setLoading(loading: Boolean) {
        _uiState.update { it.copy(isLoading = loading) }
    }

    private fun setError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
        logW("Erro: $message")
    }

    private fun getFileBytes(uri: Uri): Result<ByteArray> {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            if (bytes != null) Result.success(bytes)
            else Result.failure(Exception("Nenhum dado lido"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseDateTime(dateTimeStr: String): Instant? {
        return try {
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            val localDateTime = LocalDateTime.parse(dateTimeStr, formatter)
            localDateTime.atZone(ZoneId.systemDefault()).toInstant()
        } catch (e: Exception) {
            logW("Erro ao parsear data/hora: $dateTimeStr")
            null
        }
    }

    private fun formatDate(instant: Instant): String {
        return try {
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).format(formatter)
        } catch (e: Exception) { "" }
    }

    private fun formatTime(instant: Instant): String {
        return try {
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).format(formatter)
        } catch (e: Exception) { "" }
    }

    override fun onCleared() {
        super.onCleared()
        logD("ViewModel finalizado")
    }
}