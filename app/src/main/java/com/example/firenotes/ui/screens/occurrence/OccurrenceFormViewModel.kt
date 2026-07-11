package com.example.firenotes.ui.screens.occurrence

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firenotes.domain.model.*
import com.example.firenotes.domain.ocr.DocumentType
import com.example.firenotes.domain.ocr.OCREngine
import com.example.firenotes.domain.repository.CameraCaptureService
import com.example.firenotes.domain.repository.ImageProcessingService
import com.example.firenotes.domain.repository.LocationService
import com.example.firenotes.domain.repository.OcorrenciaRepository
import com.example.firenotes.domain.repository.OcrDocumentResult
import com.example.firenotes.domain.repository.OcrField
import com.example.firenotes.domain.repository.OcrService
import com.example.firenotes.ui.designsystem.colors.FireColors
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

// ============================================
// LOGS PADRONIZADOS (Padrão FireNotes)
// ============================================

private const val LOG_TAG = "FireOccurrence"

private fun logD(message: String) = android.util.Log.d(LOG_TAG, message)
private fun logE(message: String, throwable: Throwable? = null) = 
    android.util.Log.e(LOG_TAG, message, throwable)
private fun logW(message: String) = android.util.Log.w(LOG_TAG, message)
private fun logI(message: String) = android.util.Log.i(LOG_TAG, message)

// ============================================
// UI STATE E ESTÁGIOS DO FORMULÁRIO
// ============================================

enum class FormStage {
    INITIAL_DATA,
    NATURE_SELECTION,
    TABS
}

data class OccurrenceFormUiState(
    val id: String? = null, // Database occurrence UUID
    val protocolo: String = "", // Talão number
    val data: String = "", // dd/MM/yyyy
    val hora: String = "", // HH:mm
    val natureza: NaturezaOcorrencia = NaturezaOcorrencia.PESSOAL,
    val latitude: Double? = null,
    val longitude: Double? = null,
    
    // Address
    val rua: String = "",
    val numero: String = "",
    val bairro: String = "",
    val cidade: String = "",
    val uf: String = "",
    val isGpsLoading: Boolean = false,
    
    // Form Content
    val historico: String = "",
    val fotos: List<String> = emptyList(), // URLs of uploaded photos
    val videos: List<String> = emptyList(), // URLs of uploaded videos
    
    // Support Details
    val orgaosDisponiveis: List<OrgaoApoio> = emptyList(),
    val apoiosDetalhados: List<ApoioOcorrencia> = emptyList(),
    
    // Entities
    val pessoas: List<Pessoa> = emptyList(),
    val documentos: List<Documento> = emptyList(),
    val veiculos: List<VeiculoEnvolvido> = emptyList(),
    val vitimas: List<Vitima> = emptyList(),
    val viaturas: List<Viatura> = emptyList(),
    val evidencias: List<Evidencia> = emptyList(),
    
    // UI state variables
    val formStage: FormStage = FormStage.INITIAL_DATA,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSavingSuccess: Boolean = false,
    val errorMessage: String? = null,
    val operationProgress: Float = 0f,
    val operationMessage: String? = null
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
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(OccurrenceFormUiState())
    val uiState: StateFlow<OccurrenceFormUiState> = _uiState.asStateFlow()

    init {
        logD("ViewModel inicializado")
        initializeDefaultValues()
        loadOrgaosApoio()
    }

    // ============================================
    // INICIALIZAÇÃO E CARREGAMENTO
    // ============================================

    private fun initializeDefaultValues() {
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        val nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        
        _uiState.update { 
            it.copy(
                data = today,
                hora = nowTime
            )
        }
        logD("Valores padrão inicializados: data=$today, hora=$nowTime")
    }

    fun loadOccurrence(occurrenceId: String) {
        logD("Carregando ocorrência: $occurrenceId")
        setLoading(true)
        
        viewModelScope.launch {
            try {
                val occurrence = repository.getOcorrenciaById(occurrenceId).getOrThrow()
                
                val viaturas = repository.getViaturasDaOcorrencia(occurrenceId)
                    .getOrDefault(emptyList())
                
                val pessoas = repository.getPessoasDaOcorrencia(occurrenceId)
                    .getOrDefault(emptyList())
                
                val documentos = repository.getDocumentosDaOcorrencia(occurrenceId)
                    .getOrDefault(emptyList())
                
                val evidencias = repository.getEvidencias(occurrenceId)
                    .getOrDefault(emptyList())

                logD("Dados carregados: viaturas=${viaturas.size}, pessoas=${pessoas.size}, documentos=${documentos.size}")

                _uiState.update {
                    it.copy(
                        id = occurrence.id,
                        protocolo = occurrence.protocolo,
                        data = formatDate(occurrence.dataHora),
                        hora = formatTime(occurrence.dataHora),
                        natureza = occurrence.natureza,
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
            } catch (e: Exception) {
                logE("Erro ao carregar ocorrência", e)
                setError("Erro ao carregar ocorrência: ${e.localizedMessage}")
                setLoading(false)
            }
        }
    }

    private fun loadOrgaosApoio() {
        viewModelScope.launch {
            repository.getOrgaosApoio()
                .onSuccess { list ->
                    _uiState.update { it.copy(orgaosDisponiveis = list) }
                    logD("Órgãos de apoio carregados: ${list.size}")
                }
                .onFailure { error ->
                    logW("Erro ao carregar órgãos de apoio, usando fallback: ${error.message}")
                    val fallbackList = listOf(
                        OrgaoApoio("1", "Polícia Rodoviária Federal", "PRF"),
                        OrgaoApoio("2", "Corpo de Bombeiros Militar", "CBM"),
                        OrgaoApoio("3", "Polícia Militar", "PM"),
                        OrgaoApoio("4", "Serviço de Atendimento Móvel de Urgência", "SAMU"),
                        OrgaoApoio("5", "Defesa Civil", "DC")
                    )
                    _uiState.update { it.copy(orgaosDisponiveis = fallbackList) }
                }
        }
    }

    // ============================================
    // ENDEREÇO E LOCALIZAÇÃO
    // ============================================

    fun updateInitialFields(talao: String, data: String, hora: String) {
        _uiState.update { 
            it.copy(
                protocolo = talao,
                data = data,
                hora = hora
            )
        }
        saveOccurrenceDraft()
    }

    fun updateManualAddress(rua: String, numero: String, bairro: String, cidade: String, uf: String) {
        _uiState.update {
            it.copy(
                rua = rua,
                numero = numero,
                bairro = bairro,
                cidade = cidade,
                uf = uf
            )
        }
        saveOccurrenceDraft()
    }

    fun captureLocationAndAddress() {
        logD("Iniciando captura de localização por GPS")
        _uiState.update { it.copy(isGpsLoading = true, errorMessage = null) }
        
        viewModelScope.launch {
            locationService.getCurrentLocation()
                .onSuccess { pair ->
                    val lat = pair.first
                    val lng = pair.second
                    logD("Localização GPS capturada com sucesso: lat=$lat, lng=$lng")
                    _uiState.update { 
                        it.copy(
                            latitude = lat,
                            longitude = lng
                        )
                    }
                    saveOccurrenceDraft()
                    fetchAddressFromLocation(lat, lng)
                }
                .onFailure { error ->
                    logE("Erro GPS ao tentar capturar localização", error)
                    _uiState.update {
                        it.copy(
                            isGpsLoading = false,
                            errorMessage = "Erro GPS: ${error.localizedMessage}"
                        )
                    }
                }
        }
    }

    private fun fetchAddressFromLocation(lat: Double, lng: Double) {
        viewModelScope.launch {
            locationService.getAddressFromLocation(lat, lng)
                .onSuccess { address ->
                    logD("Endereço reverso obtido com sucesso: ${address.rua}, ${address.cidade}")
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
                    logE("Erro ao obter endereço reverso a partir da localização", error)
                    _uiState.update {
                        it.copy(
                            isGpsLoading = false,
                            errorMessage = "Endereço por GPS falhou: ${error.localizedMessage}"
                        )
                    }
                }
        }
    }

    fun validateAndProceedToNature() {
        val state = _uiState.value
        when {
            state.protocolo.isBlank() -> {
                setError("O número do talão é obrigatório.")
            }
            state.data.isBlank() -> {
                setError("A data é obrigatória.")
            }
            state.hora.isBlank() -> {
                setError("A hora é obrigatória.")
            }
            else -> {
                logD("Dados iniciais validados com sucesso. Avançando para seleção de natureza.")
                _uiState.update { 
                    it.copy(
                        formStage = FormStage.NATURE_SELECTION,
                        errorMessage = null
                    )
                }
            }
        }
    }

    // ============================================
    // GESTÃO DE OCORRÊNCIA E NATUREZA
    // ============================================

    fun selectNaturezaAndCreateOccurrence(natureza: NaturezaOcorrencia) {
        logD("Selecionando natureza de ocorrência: $natureza")
        setLoading(true)
        
        viewModelScope.launch {
            try {
                val occurrence = buildOccurrence(natureza)
                
                repository.createOcorrencia(occurrence)
                    .onSuccess { saved ->
                        logD("Ocorrência criada e salva no DB local: ID=${saved.id}")
                        _uiState.update {
                            it.copy(
                                id = saved.id,
                                natureza = natureza,
                                formStage = FormStage.TABS,
                                isLoading = false
                            )
                        }
                    }
                    .onFailure { error ->
                        logE("Erro ao salvar nova ocorrência no DB", error)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Erro ao criar ocorrência: ${error.localizedMessage}"
                            )
                        }
                    }
            } catch (e: Exception) {
                logE("Erro geral ao instanciar ou salvar ocorrência", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Erro ao criar ocorrência: ${e.localizedMessage}"
                    )
                }
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
    // SISTEMA E PROCESSO OCR (INTELIGENTE & V4 COMPATIBLE)
    // ============================================

    fun createPhotoUri(): Uri {
        return cameraCaptureService.createPhotoUri()
    }

    fun scanDocumentOcr(imageUri: Uri, onResult: (OcrDocumentResult) -> Unit) {
        logD("Iniciando escaneamento OCR simples: $imageUri")
        setLoading(true)
        
        viewModelScope.launch {
            ocrService.recognizeText(imageUri)
                .onSuccess { rawResult ->
                    logD("Texto bruto OCR reconhecido com sucesso. Processando com OCREngine.")
                    
                    // 1. Process with OCREngine
                    val processedResult = ocrEngine.process(rawResult.rawText)
                    
                    // 2. Map OCREngine result to backward compatible OcrDocumentResult
                    val mappedResult = mapOcrEngineResultToOcrDocumentResult(processedResult)
                    
                    setLoading(false)
                    onResult(mappedResult)
                }
                .onFailure { error ->
                    logE("Falha no escaneamento OCR simples", error)
                    setError("OCR Error: ${error.localizedMessage}")
                    setLoading(false)
                }
        }
    }

    fun checkAndProcessOcrImage(
        imageUri: Uri,
        onQualityIssue: (String) -> Unit,
        onSuccess: (OcrDocumentResult, Uri) -> Unit
    ) {
        logD("Iniciando checkAndProcessOcrImage com verificação de qualidade: $imageUri")
        setLoading(true)
        
        viewModelScope.launch {
            try {
                // 1. Validar imagem
                val bitmap = validateImage(imageUri).getOrElse { error ->
                    logE("Falha ao validar imagem para OCR", error)
                    setError("Falha ao ler imagem: ${error.localizedMessage}")
                    setLoading(false)
                    return@launch
                }
                
                // 2. Verificar qualidade
                val quality = imageProcessingService.checkQuality(bitmap)
                if (!quality.isValid) {
                    logW("Qualidade inadequada detectada: ${quality.reason}")
                    setLoading(false)
                    onQualityIssue(quality.reason ?: "Baixa qualidade detectada na imagem.")
                    return@launch
                }
                
                // 3. Processar imagem (perspectiva, contraste, recortes)
                val processedUri = processImage(bitmap).getOrElse { error ->
                    logE("Falha no processamento de imagem (Filtros)", error)
                    setError("Erro no processamento da imagem: ${error.localizedMessage}")
                    setLoading(false)
                    return@launch
                }
                
                // 4. Executar OCR
                val rawOcrResult = runOcr(processedUri).getOrElse { error ->
                    logE("Falha no serviço de reconhecimento de texto OCR", error)
                    setError("Erro no OCR: ${error.localizedMessage}")
                    setLoading(false)
                    return@launch
                }
                
                // 5. Rodar motor inteligente de extração & classificação
                val ocrEngineResult = ocrEngine.process(rawOcrResult.rawText)
                val compatibleResult = mapOcrEngineResultToOcrDocumentResult(ocrEngineResult)
                
                logD("Processamento OCR completo. Tipo identificado: ${compatibleResult.tipo}")
                setLoading(false)
                onSuccess(compatibleResult, processedUri)
                
            } catch (e: Exception) {
                logE("Erro geral no fluxo de validação e processamento OCR", e)
                setError("Erro no fluxo OCR: ${e.localizedMessage}")
                setLoading(false)
            }
        }
    }

    fun processAndRunOcrDirectly(
        imageUri: Uri,
        onSuccess: (OcrDocumentResult, Uri) -> Unit
    ) {
        logD("Iniciando OCR direto (sem checagem de qualidade): $imageUri")
        setLoading(true)
        
        viewModelScope.launch {
            try {
                val bitmap = validateImage(imageUri).getOrElse { error ->
                    logE("Falha na validação de imagem direta", error)
                    setError("Falha ao ler imagem: ${error.localizedMessage}")
                    setLoading(false)
                    return@launch
                }
                
                val processedUri = processImage(bitmap).getOrElse { error ->
                    logE("Falha ao processar filtros na imagem direta", error)
                    setError("Erro no processamento: ${error.localizedMessage}")
                    setLoading(false)
                    return@launch
                }
                
                val rawOcrResult = runOcr(processedUri).getOrElse { error ->
                    logE("Falha na leitura direta de OCR", error)
                    setError("Erro no OCR: ${error.localizedMessage}")
                    setLoading(false)
                    return@launch
                }
                
                val ocrEngineResult = ocrEngine.process(rawOcrResult.rawText)
                val compatibleResult = mapOcrEngineResultToOcrDocumentResult(ocrEngineResult)
                
                logD("OCR direto finalizado com sucesso. Tipo: ${compatibleResult.tipo}")
                setLoading(false)
                onSuccess(compatibleResult, processedUri)
                
            } catch (e: Exception) {
                logE("Erro no fluxo direto de processamento OCR", e)
                setError("Erro no fluxo OCR direto: ${e.localizedMessage}")
                setLoading(false)
            }
        }
    }

    private suspend fun validateImage(uri: Uri): Result<Bitmap> {
        return try {
            val fileSize = try {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L
            } catch (e: Exception) {
                0L
            }
            
            if (fileSize <= 0) {
                return Result.failure(Exception("O arquivo de imagem está vazio ou não existe."))
            }
            
            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return Result.failure(Exception("Falha ao decodificar imagem."))
            
            Result.success(bitmap)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun processImage(bitmap: Bitmap): Result<Uri> {
        return try {
            val processedBitmap = imageProcessingService.processDocumentImage(bitmap)
            
            val processedFile = java.io.File(
                context.cacheDir,
                "camera_capture_processed_${System.currentTimeMillis()}.jpg"
            )
            
            java.io.FileOutputStream(processedFile).use { out ->
                processedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            
            val processedUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "com.example.firenotes.fileprovider",
                processedFile
            )
            
            Result.success(processedUri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun runOcr(uri: Uri): Result<OcrDocumentResult> {
        return ocrService.recognizeText(uri)
    }

    private fun mapOcrEngineResultToOcrDocumentResult(result: OCREngine.OCRResult): OcrDocumentResult {
        val extractedFields = result.campos.toMutableMap()
        
        // Add compatibility mappings for the dialog UI
        if (extractedFields.containsKey("data_nascimento")) {
            extractedFields["nascimento"] = extractedFields["data_nascimento"] ?: ""
        }
        
        if (extractedFields.containsKey("marca") || extractedFields.containsKey("modelo")) {
            val marca = extractedFields["marca"] ?: ""
            val modelo = extractedFields["modelo"] ?: ""
            val combined = "$marca $modelo".trim()
            if (combined.isNotEmpty()) {
                extractedFields["marca_modelo"] = combined
            }
        }
        
        if (extractedFields.containsKey("registro")) {
            extractedFields["numero"] = extractedFields["registro"] ?: ""
        }
        
        val fieldsWithConfidence = extractedFields.mapValues { (key, value) ->
            val origKey = when (key) {
                "nascimento" -> "data_nascimento"
                "marca_modelo" -> "modelo"
                else -> key
            }
            val confidenceInt = result.confianca[origKey] ?: 70
            val confidenceFloat = confidenceInt / 100.0f
            OcrField(
                value = value,
                confidence = confidenceFloat,
                isPendingReview = confidenceFloat < 0.80f
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
    // GESTÃO DE DOCUMENTOS E PESSOAS
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
                // 1. Criar/Atualizar pessoa vinculada ao documento
                val nome = extractedFields["nome"] ?: "DESCONHECIDO"
                val cpf = extractedFields["cpf"]
                val rg = extractedFields["rg"]
                val rgOrgaoEmissor = extractedFields["rg_orgao_emissor"]
                val rgUf = extractedFields["rg_uf"]
                val nascimento = extractedFields["nascimento"]
                val naturalidade = extractedFields["naturalidade"]
                val nacionalidade = extractedFields["nacionalidade"]
                val filiacao = extractedFields["filiacao"]
                val nomeSocial = extractedFields["nome_social"]

                val pessoa = Pessoa(
                    nome = nome,
                    nomeSocial = nomeSocial,
                    cpf = cpf,
                    rg = rg,
                    rgOrgaoEmissor = rgOrgaoEmissor,
                    rgUf = rgUf,
                    nascimento = nascimento,
                    naturalidade = naturalidade,
                    nacionalidade = nacionalidade,
                    filiacao = filiacao
                )

                val savedPessoa = repository.upsertPessoa(pessoa).getOrThrow()
                logD("Pessoa vinculada salva/atualizada: ID=${savedPessoa.id}")
                
                // 2. Fazer upload da imagem associada se aplicável
                val isLauncherResource = imageUri.toString().contains("ic_launcher_foreground")
                val uploadResult = if (imageUri != Uri.EMPTY && !isLauncherResource) {
                    val bytes = getFileBytes(imageUri).getOrThrow()
                    val path = "$occurrenceId/documentos/${tipo}_${System.currentTimeMillis()}.png"
                    val url = repository.uploadFile("ocorrencias", path, bytes).getOrThrow()
                    val hash = java.util.UUID.nameUUIDFromBytes(bytes).toString()
                    Result.success(url to hash)
                } else {
                    Result.success(null)
                }
                
                val (url, hash) = uploadResult.getOrNull() ?: (null to null)
                
                // 3. Persistir metadados do documento
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
                logPersistenceSuccess("Documento", "Salvo com sucesso: ID=${savedDoc.id}")
                
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        documentos = state.documentos + savedDoc,
                        pessoas = state.pessoas.filter { it.cpf != savedPessoa.cpf } + savedPessoa
                    )
                }
                
            } catch (e: Exception) {
                logE("Erro ao salvar documento e associar pessoa", e)
                setError("Erro ao salvar documento: ${e.localizedMessage}")
                setLoading(false)
            }
        }
    }

    fun deleteDocumento(id: String) {
        logD("Deletando documento: $id")
        setLoading(true)
        
        viewModelScope.launch {
            repository.deleteDocumento(id)
                .onSuccess {
                    logPersistenceSuccess("Documento", "Removido ID=$id")
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            documentos = state.documentos.filter { it.id != id }
                        )
                    }
                }
                .onFailure { error ->
                    logE("Erro ao remover documento do repositório", error)
                    setError("Erro ao deletar documento: ${error.localizedMessage}")
                    setLoading(false)
                }
        }
    }

    // ============================================
    // GESTÃO DE VEÍCULOS
    // ============================================

    fun saveVeiculo(
        placa: String,
        modelo: String,
        cor: String,
        chassi: String,
        ano: Int?,
        proprietarioId: String?,
        extractedFields: Map<String, String> = emptyMap(),
        rawText: String = "",
        imageUri: Uri? = null
    ) {
        val occurrenceId = _uiState.value.id ?: return
        logD("Salvando veículo envolvido: placa=$placa, modelo=$modelo")
        setLoading(true)
        
        viewModelScope.launch {
            try {
                var urlCrlv: String? = null
                if (imageUri != null && imageUri != Uri.EMPTY) {
                    val bytes = getFileBytes(imageUri).getOrThrow()
                    val path = "$occurrenceId/veiculos/crlv_${System.currentTimeMillis()}.png"
                    urlCrlv = repository.uploadFile("ocorrencias", path, bytes).getOrThrow()
                    logD("Upload de imagem CRLV realizado com sucesso: $urlCrlv")
                }
                
                val veiculo = VeiculoEnvolvido(
                    ocorrenciaId = occurrenceId,
                    placa = placa,
                    cor = cor,
                    chassi = chassi,
                    modelo = modelo,
                    ano = ano,
                    proprietarioId = proprietarioId,
                    renavam = extractedFields["renavam"],
                    monobloco = extractedFields["chassi"],
                    especie = extractedFields["especie"],
                    tipoVeiculo = extractedFields["tipo_veiculo"],
                    carroceria = extractedFields["carroceria"],
                    marca = extractedFields["marca_modelo"] ?: extractedFields["marca"],
                    versao = extractedFields["marca_modelo"] ?: extractedFields["versao"],
                    anoFabricacao = extractedFields["ano_fabricacao"]?.toIntOrNull(),
                    anoModelo = extractedFields["ano_modelo"]?.toIntOrNull(),
                    categoriaVeiculo = extractedFields["categoria_veiculo"],
                    exercicio = extractedFields["exercicio"],
                    urlCrlv = urlCrlv,
                    ocrTextoCrlv = rawText,
                    ocrDadosEstruturados = extractedFields
                )
                
                val saved = repository.addVeiculoEnvolvido(veiculo).getOrThrow()
                logPersistenceSuccess("Veículo", "Salvo ID=${saved.id}, placa=${saved.placa}")
                
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        veiculos = state.veiculos + saved
                    )
                }
                
            } catch (e: Exception) {
                logE("Erro ao salvar veículo envolvido", e)
                setError("Erro ao salvar veículo: ${e.localizedMessage}")
                setLoading(false)
            }
        }
    }

    fun deleteVeiculo(id: String) {
        logD("Deletando veículo envolvido: $id")
        setLoading(true)
        
        viewModelScope.launch {
            repository.deleteVeiculo(id)
                .onSuccess {
                    logPersistenceSuccess("Veículo", "Removido ID=$id")
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            veiculos = state.veiculos.filter { it.id != id }
                        )
                    }
                }
                .onFailure { error ->
                    logE("Erro ao deletar veículo do repositório", error)
                    setError("Erro ao deletar veículo: ${error.localizedMessage}")
                    setLoading(false)
                }
        }
    }

    // ============================================
    // GESTÃO DE VÍTIMAS
    // ============================================

    fun saveVitima(
        pessoaId: String,
        lesoes: String,
        destino: String,
        quemSocorreu: String,
        resultado: String,
        pulso: Int?,
        pa: String,
        satO2: Int?,
        temp: Double?,
        gcs: Int?,
        viaturaSocorroId: String?,
        hospitalDestino: String?,
        transportadoPor: String?
    ) {
        val occurrenceId = _uiState.value.id ?: return
        val person = _uiState.value.pessoas.find { it.id == pessoaId } ?: return
        logD("Registrando nova vítima: ${person.nome}")
        setLoading(true)
        
        viewModelScope.launch {
            try {
                val idade = calculateAge(person.nascimento)
                
                val vitima = Vitima(
                    ocorrenciaId = occurrenceId,
                    nome = person.nome,
                    idade = idade,
                    lesoesAparentes = lesoes,
                    destinoSocorro = destino,
                    quemSocorreu = quemSocorreu,
                    resultadoOcorrencia = resultado,
                    sinaisVitais = SinaisVitais(
                        pulso = pulso,
                        pressaoArterial = pa,
                        saturacaoO2 = satO2,
                        temperatura = temp,
                        escalaGCS = gcs
                    ),
                    pessoaId = pessoaId,
                    viaturaSocorroId = viaturaSocorroId,
                    hospitalDestino = hospitalDestino,
                    transportadoPor = transportadoPor
                )
                
                val saved = repository.addVitima(vitima).getOrThrow()
                logPersistenceSuccess("Vítima", "Salva ID=${saved.id}")
                
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        vitimas = state.vitimas + saved
                    )
                }
                
            } catch (e: Exception) {
                logE("Erro ao registrar vítima envolvida", e)
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
            logW("Erro ao calcular idade do formato: $birthDateStr")
            null
        }
    }

    // ============================================
    // GESTÃO DE VIATURAS E EQUIPES (MILITARES)
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
        val occurrenceId = _uiState.value.id
        if (occurrenceId == null) {
            setError("Ocorrência não salva. Salve a ocorrência primeiro.")
            return
        }
        
        if (prefixo.isBlank()) {
            setError("O prefixo da viatura é obrigatório.")
            return
        }
        
        logD("Adicionando/Atualizando viatura: prefixo=$prefixo, tipo=$tipo")
        setLoading(true)
        
        viewModelScope.launch {
            try {
                val finalViaturaId = viaturaId ?: UUID.randomUUID().toString()
                val existingViatura = _uiState.value.viaturas.find { it.id == finalViaturaId }
                
                val viatura = Viatura(
                    id = finalViaturaId,
                    ocorrenciaId = occurrenceId,
                    prefixo = prefixo,
                    tipo = tipo,
                    unidade = unidade,
                    kmSaida = kmSaida,
                    kmLocal = kmLocal,
                    observacoes = observacoes,
                    equipe = existingViatura?.equipe ?: emptyList()
                )
                
                val saved = repository.addViatura(viatura).getOrThrow()
                logPersistenceSuccess("Viatura", "Salva com sucesso: ID=${saved.id}")
                
                _uiState.update { state ->
                    val updatedList = if (viaturaId != null) {
                        state.viaturas.map { if (it.id == viaturaId) saved else it }
                    } else {
                        state.viaturas + saved
                    }
                    state.copy(
                        isLoading = false,
                        viaturas = updatedList
                    )
                }
            } catch (e: Exception) {
                logE("Erro ao adicionar viatura operacional", e)
                setError("Erro ao adicionar viatura: ${e.localizedMessage}")
                setLoading(false)
            }
        }
    }

    fun salvarViaturaComMilitares(
        prefixo: String,
        tipo: String,
        unidade: String?,
        kmSaida: Int?,
        kmLocal: Int?,
        observacoes: String?,
        militares: List<Militar>
    ) {
        val occurrenceId = _uiState.value.id
        if (occurrenceId == null) {
            setError("Ocorrência não foi salva. Salve a ocorrência primeiro.")
            return
        }
        
        if (prefixo.isBlank()) {
            setError("O prefixo da viatura é obrigatório.")
            return
        }
        
        logD("🔄 Salvando viatura com ${militares.size} militares...")
        setLoading(true)
        
        viewModelScope.launch {
            try {
                val viaturaId = UUID.randomUUID().toString()
                val viatura = Viatura(
                    id = viaturaId,
                    ocorrenciaId = occurrenceId,
                    prefixo = prefixo,
                    tipo = tipo,
                    unidade = unidade,
                    kmSaida = kmSaida,
                    kmLocal = kmLocal,
                    observacoes = observacoes
                )
                
                val saved = repository.salvarViaturaComMilitares(viatura, militares).getOrThrow()
                logPersistenceSuccess("Viatura com Militares", "Salva com sucesso: ID=${saved.id}")
                
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        viaturas = state.viaturas + saved
                    )
                }
            } catch (e: Exception) {
                logE("Erro ao salvar viatura com militares", e)
                setError("Erro ao salvar viatura: ${e.localizedMessage}")
                setLoading(false)
            }
        }
    }

    fun deleteViatura(viaturaId: String) {
        logD("Deletando viatura operacional: $viaturaId")
        setLoading(true)
        
        viewModelScope.launch {
            repository.deleteViatura(viaturaId)
                .onSuccess {
                    logPersistenceSuccess("Viatura", "Removida ID=$viaturaId")
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            viaturas = state.viaturas.filter { it.id != viaturaId }
                        )
                    }
                }
                .onFailure { error ->
                    logE("Erro ao remover viatura do repositório", error)
                    setError("Erro ao deletar viatura: ${error.localizedMessage}")
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
            logE("❌ Viatura não encontrada: $viaturaId")
            logD("📋 Viaturas disponíveis: ${_uiState.value.viaturas.map { it.id }}")
            setError("Viatura não encontrada. Por favor, adicione a viatura primeiro.")
            return
        }
        
        if (viatura.ocorrenciaId.isBlank() || viatura.ocorrenciaId == "TEMP") {
            logE("❌ Viatura com ocorrenciaId inválido: ${viatura.ocorrenciaId}")
            setError("Viatura não está vinculada a uma ocorrência. Salve a ocorrência primeiro.")
            return
        }
        
        logD("========================================")
        logD("🔄 INICIANDO SALVAMENTO DE MILITAR")
        logD("📋 RE: $re")
        logD("📋 Nome: $nomeGuerra")
        logD("📋 Viatura ID: $viaturaId")
        logD("📋 Função: $funcao")
        logD("========================================")
        
        setLoading(true)
        
        viewModelScope.launch {
            try {
                val graduacao = GraduacaoMilitar.fromDescricao(graduacaoStr)
                val militarId = UUID.randomUUID().toString()
                
                val militar = Militar(
                    id = militarId,
                    viaturaId = viaturaId,
                    re = re,
                    nomeGuerra = nomeGuerra,
                    graduacao = graduacao,
                    funcao = funcao
                )
                
                val saved = repository.addMilitar(militar).getOrThrow()
                logPersistenceSuccess("Militar", "Salvo na equipe da viatura: ID=${saved.id}")
                
                _uiState.update { state ->
                    val updatedViaturas = state.viaturas.map { v ->
                        if (v.id == viaturaId) {
                            val updatedEquipe = (v.equipe + saved)
                                .sortedByDescending { it.graduacao.hierarquia }
                            v.copy(equipe = updatedEquipe)
                        } else {
                            v
                        }
                    }
                    state.copy(
                        isLoading = false,
                        viaturas = updatedViaturas
                    )
                }
            } catch (e: Exception) {
                logE("Erro ao registrar militar na equipe", e)
                setError("Erro ao adicionar militar: ${e.localizedMessage}")
                setLoading(false)
            }
        }
    }

    fun deleteMilitar(militarId: String, viaturaId: String) {
        logD("Removendo militar da equipe: militarId=$militarId, viaturaId=$viaturaId")
        setLoading(true)
        
        viewModelScope.launch {
            repository.deleteMilitar(militarId)
                .onSuccess {
                    logPersistenceSuccess("Militar", "Removido ID=$militarId")
                    _uiState.update { state ->
                        val updatedViaturas = state.viaturas.map { viatura ->
                            if (viatura.id == viaturaId) {
                                viatura.copy(
                                    equipe = viatura.equipe.filter { it.id != militarId }
                                )
                            } else {
                                viatura
                            }
                        }
                        state.copy(
                            isLoading = false,
                            viaturas = updatedViaturas
                        )
                    }
                }
                .onFailure { error ->
                    logE("Erro ao deletar militar", error)
                    setError("Erro ao deletar militar: ${error.localizedMessage}")
                    setLoading(false)
                }
        }
    }

    fun moveMilitar(militarId: String, currentViaturaId: String, newViaturaId: String) {
        logD("Movendo militar ($militarId) de viatura: de=$currentViaturaId para=$newViaturaId")
        setLoading(true)
        
        viewModelScope.launch {
            repository.moveMilitar(militarId, newViaturaId)
                .onSuccess {
                    val occurrenceId = _uiState.value.id ?: return@launch
                    repository.getViaturasDaOcorrencia(occurrenceId)
                        .onSuccess { list ->
                            logPersistenceSuccess("Militar", "Movido com sucesso")
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    viaturas = list
                                )
                            }
                        }
                        .onFailure { error ->
                            logE("Militar movido mas falha ao recarregar viaturas", error)
                            setError("Erro ao atualizar lista de viaturas: ${error.localizedMessage}")
                            setLoading(false)
                        }
                }
                .onFailure { error ->
                    logE("Erro ao mover militar no repositório", error)
                    setError("Erro ao mover militar: ${error.localizedMessage}")
                    setLoading(false)
                }
        }
    }

    // ============================================
    // GESTÃO DE APOIOS E MÍDIAS
    // ============================================

    fun addApoio(orgao: OrgaoApoio, viatura: String, encarregado: String) {
        val occurrenceId = _uiState.value.id ?: return
        logD("Adicionando apoio à ocorrência: ${orgao.sigla}, viatura=$viatura")
        
        viewModelScope.launch {
            repository.vincularOrgaoApoioDetalhado(occurrenceId, orgao.id, viatura, encarregado)
                .onSuccess {
                    logD("Órgão de apoio vinculado com sucesso")
                    val newApoio = ApoioOcorrencia(orgao, viatura, encarregado)
                    _uiState.update { state ->
                        state.copy(apoiosDetalhados = state.apoiosDetalhados + newApoio)
                    }
                }
                .onFailure { error ->
                    logE("Erro ao vincular órgão de apoio detalhado", error)
                    setError("Erro ao adicionar apoio: ${error.localizedMessage}")
                }
        }
    }

    fun removeApoio(index: Int) {
        val occurrenceId = _uiState.value.id ?: return
        val apoio = _uiState.value.apoiosDetalhados.getOrNull(index) ?: return
        logD("Removendo apoio da ocorrência: ${apoio.orgao.sigla}")
        
        viewModelScope.launch {
            repository.desvincularOrgaoApoio(occurrenceId, apoio.orgao.id)
                .onSuccess {
                    logD("Órgão de apoio desvinculado com sucesso")
                    _uiState.update { state ->
                        state.copy(
                            apoiosDetalhados = state.apoiosDetalhados
                                .filterIndexed { idx, _ -> idx != index }
                        )
                    }
                }
                .onFailure { error ->
                    logE("Erro ao desvincular apoio", error)
                    setError("Erro ao remover apoio: ${error.localizedMessage}")
                }
        }
    }

    fun uploadOccurrenceFile(uri: Uri, isVideo: Boolean = false) {
        val occurrenceId = _uiState.value.id ?: return
        logD("Iniciando upload de anexo de mídia: isVideo=$isVideo, URI=$uri")
        setLoading(true)
        
        viewModelScope.launch {
            getFileBytes(uri)
                .onSuccess { bytes ->
                    val typeStr = if (isVideo) "video" else "foto"
                    val path = "$occurrenceId/imagens/${typeStr}_${System.currentTimeMillis()}.png"
                    
                    repository.uploadFile("ocorrencias", path, bytes)
                        .onSuccess { publicUrl ->
                            logD("Mídia enviada para o storage com sucesso: $publicUrl")
                            _uiState.update { state ->
                                if (isVideo) {
                                    state.copy(videos = state.videos + publicUrl, isLoading = false)
                                } else {
                                    state.copy(fotos = state.fotos + publicUrl, isLoading = false)
                                }
                            }
                        }
                        .onFailure { error ->
                            logE("Erro no envio do binário da mídia", error)
                            setError("Erro upload: ${error.localizedMessage}")
                            setLoading(false)
                        }
                }
                .onFailure { error ->
                    logE("Erro ao ler os bytes do URI da mídia", error)
                    setError("Erro ao ler arquivo: ${error.localizedMessage}")
                    setLoading(false)
                }
        }
    }

    fun removeFoto(path: String) {
        logD("Removendo foto da lista local da ocorrência: $path")
        _uiState.update { state ->
            state.copy(fotos = state.fotos.filter { it != path })
        }
        saveOccurrenceDraft()
    }

    // ============================================
    // GESTÃO DE EVIDÊNCIAS (CUSTÓDIA)
    // ============================================

    fun addEvidencia(uri: Uri, classification: String) {
        val occurrenceId = _uiState.value.id ?: return
        logD("Adicionando evidência classificada: $classification")
        setLoading(true)
        
        viewModelScope.launch {
            try {
                val bytes = getFileBytes(uri).getOrThrow()
                val path = "$occurrenceId/evidencias/evidence_${System.currentTimeMillis()}.jpg"
                val url = repository.uploadFile("ocorrencias", path, bytes).getOrThrow()
                logD("Arquivo de evidência enviado: $url")
                
                val ev = Evidencia(
                    ocorrenciaId = occurrenceId,
                    tipo = classification,
                    hashSha256 = java.util.UUID.nameUUIDFromBytes(bytes).toString(),
                    latitude = _uiState.value.latitude,
                    longitude = _uiState.value.longitude,
                    dataHora = Instant.now().toString(),
                    usuario = "Operador",
                    urlStorage = url
                )
                
                val saved = repository.addEvidencia(ev).getOrThrow()
                logPersistenceSuccess("Evidência", "Salva ID=${saved.id}")
                
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        evidencias = state.evidencias + saved
                    )
                }
                
            } catch (e: Exception) {
                logE("Erro ao registrar evidência", e)
                setError("Erro ao adicionar evidência: ${e.localizedMessage}")
                setLoading(false)
            }
        }
    }

    fun deleteEvidencia(id: String) {
        logD("Deletando registro de evidência: $id")
        setLoading(true)
        
        viewModelScope.launch {
            repository.deleteEvidencia(id)
                .onSuccess {
                    logPersistenceSuccess("Evidência", "Removida ID=$id")
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            evidencias = state.evidencias.filter { it.id != id }
                        )
                    }
                }
                .onFailure { error ->
                    logE("Erro ao deletar evidência do repositório", error)
                    setError("Erro ao deletar evidência: ${error.localizedMessage}")
                    setLoading(false)
                }
        }
    }

    // ============================================
    // FINALIZAÇÃO E PERSISTÊNCIA DE RASCUNHO
    // ============================================

    fun finalizeOccurrence() {
        val occurrenceId = _uiState.value.id ?: return
        val state = _uiState.value
        logD("Solicitando finalização e fechamento da ocorrência: $occurrenceId")
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
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
                        logD("Ocorrência finalizada e persistida localmente com sucesso")
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                isSavingSuccess = true
                            )
                        }
                    }
                    .onFailure { error ->
                        logE("Falha ao salvar versão final da ocorrência", error)
                        // Mesmo que falhe, consideramos salvo porque os detalhes já estão em tabelas separadas do SQLite
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                isSavingSuccess = true
                            )
                        }
                    }
            } catch (e: Exception) {
                logE("Erro geral ao finalizar ocorrência", e)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isSavingSuccess = true
                    )
                }
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
                    .onSuccess {
                        logPersistenceSuccess("Ocorrência", "Rascunho atualizado ID=$occurrenceId")
                    }
                    .onFailure { error ->
                        logPersistenceError("Ocorrência", "saveOccurrenceDraft", error)
                    }
            } catch (e: Exception) {
                logPersistenceError("Ocorrência", "saveOccurrenceDraftOuter", e)
            }
        }
    }

    // ============================================
    // UTILITÁRIOS AUXILIARES
    // ============================================

    private fun setLoading(loading: Boolean) {
        _uiState.update { it.copy(isLoading = loading) }
    }

    private fun setError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
        logW("Erro de UI definido: $message")
    }

    private fun getFileBytes(uri: Uri): Result<ByteArray> {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            if (bytes != null) {
                Result.success(bytes)
            } else {
                Result.failure(Exception("Nenhum dado lido do stream de entrada."))
            }
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
            logW("Erro ao formatar string de data/hora: $dateTimeStr")
            null
        }
    }

    private fun formatDate(instant: Instant): String {
        return try {
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).format(formatter)
        } catch (e: Exception) {
            ""
        }
    }

    private fun formatTime(instant: Instant): String {
        return try {
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).format(formatter)
        } catch (e: Exception) {
            ""
        }
    }

    private fun logPersistenceSuccess(tag: String, message: String) {
        android.util.Log.d("FireNotes", "Persistência - $tag: $message")
    }

    private fun logPersistenceError(tag: String, method: String, error: Throwable) {
        val stackTraceStr = android.util.Log.getStackTraceString(error)
        android.util.Log.e("FireNotes", "Persistência - ERRO em $method [${error.javaClass.simpleName}]: ${error.message}\n$stackTraceStr")
    }

    override fun onCleared() {
        super.onCleared()
        logD("ViewModel finalizado (onCleared)")
    }
}
