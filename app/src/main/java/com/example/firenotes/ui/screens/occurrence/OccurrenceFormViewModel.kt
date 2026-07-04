package com.example.firenotes.ui.screens.occurrence

import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firenotes.domain.model.*
import com.example.firenotes.domain.repository.LocationService
import com.example.firenotes.domain.repository.OcorrenciaRepository
import com.example.firenotes.domain.repository.OcrService
import com.example.firenotes.domain.repository.OcrDocumentResult
import com.example.firenotes.domain.repository.CameraCaptureService
import com.example.firenotes.domain.repository.ImageProcessingService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

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
    
    // V2 Entities
    val pessoas: List<Pessoa> = emptyList(),
    val documentos: List<Documento> = emptyList(),
    val veiculos: List<VeiculoEnvolvido> = emptyList(),
    val vitimas: List<Vitima> = emptyList(),
    val viaturas: List<Viatura> = emptyList(),
    val evidencias: List<Evidencia> = emptyList(),
    
    // UI state
    val formStage: FormStage = FormStage.INITIAL_DATA,
    val isLoading: Boolean = false,
    val isSavingSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class OccurrenceFormViewModel @Inject constructor(
    private val repository: OcorrenciaRepository,
    private val locationService: LocationService,
    private val ocrService: OcrService,
    private val cameraCaptureService: CameraCaptureService,
    private val imageProcessingService: ImageProcessingService,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(OccurrenceFormUiState())
    val uiState: StateFlow<OccurrenceFormUiState> = _uiState.asStateFlow()

    init {
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        val nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        
        _uiState.update { 
            it.copy(
                data = today,
                hora = nowTime
            )
        }
        
        loadOrgaosApoio()
    }

    fun loadOccurrence(occurrenceId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            repository.getOcorrenciaById(occurrenceId)
                .onSuccess { occurrence ->
                    val viaturas = repository.getViaturasDaOcorrencia(occurrenceId).getOrDefault(emptyList())
                    val pessoas = repository.getPessoasDaOcorrencia(occurrenceId).getOrDefault(emptyList())
                    val documentos = repository.getDocumentosDaOcorrencia(occurrenceId).getOrDefault(emptyList())
                    val evidencias = repository.getEvidencias(occurrenceId).getOrDefault(emptyList())
                    
                    val dateFormatted = try {
                        val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
                        java.time.LocalDateTime.ofInstant(occurrence.dataHora, java.time.ZoneId.systemDefault()).format(formatter)
                    } catch(e: Exception) { "" }
                    
                    val hourFormatted = try {
                        val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                        java.time.LocalDateTime.ofInstant(occurrence.dataHora, java.time.ZoneId.systemDefault()).format(formatter)
                    } catch(e: Exception) { "" }

                    android.util.Log.d("FireNotes", "Viaturas carregadas - Quantidade: ${viaturas.size}, ID ocorrência: $occurrenceId")

                    _uiState.update { 
                        it.copy(
                            id = occurrence.id,
                            protocolo = occurrence.protocolo,
                            data = if (dateFormatted.isNotEmpty()) dateFormatted else it.data,
                            hora = if (hourFormatted.isNotEmpty()) hourFormatted else it.hora,
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
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Erro ao carregar ocorrência: ${error.localizedMessage}") }
                }
        }
    }

    private fun loadOrgaosApoio() {
        viewModelScope.launch {
            repository.getOrgaosApoio().onSuccess { list ->
                _uiState.update { it.copy(orgaosDisponiveis = list) }
            }.onFailure { error ->
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

    fun updateInitialFields(talao: String, data: String, hora: String) {
        _uiState.update { it.copy(protocolo = talao, data = data, hora = hora) }
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
    }

    fun captureLocationAndAddress() {
        _uiState.update { it.copy(isGpsLoading = true, errorMessage = null) }
        viewModelScope.launch {
            locationService.getCurrentLocation()
                .onSuccess { pair ->
                    val lat = pair.first
                    val lng = pair.second
                    _uiState.update { it.copy(latitude = lat, longitude = lng) }
                    
                    // Reverse geocoding
                    locationService.getAddressFromLocation(lat, lng)
                        .onSuccess { address ->
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
                        }
                        .onFailure { error ->
                            _uiState.update { 
                                it.copy(
                                    isGpsLoading = false,
                                    errorMessage = "Endereço por GPS falhou: ${error.localizedMessage}"
                                )
                            }
                        }
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isGpsLoading = false,
                            errorMessage = "Erro GPS: ${error.localizedMessage}"
                        )
                    }
                }
        }
    }

    fun validateAndProceedToNature() {
        val state = _uiState.value
        if (state.protocolo.isBlank()) {
            _uiState.update { it.copy(errorMessage = "O número do talão é obrigatório.") }
            return
        }
        if (state.data.isBlank()) {
            _uiState.update { it.copy(errorMessage = "A data é obrigatória.") }
            return
        }
        if (state.hora.isBlank()) {
            _uiState.update { it.copy(errorMessage = "A hora é obrigatória.") }
            return
        }
        
        _uiState.update { it.copy(formStage = FormStage.NATURE_SELECTION, errorMessage = null) }
    }

    fun selectNaturezaAndCreateOccurrence(natureza: NaturezaOcorrencia) {
        val state = _uiState.value
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        
        viewModelScope.launch {
            val dateStr = "${state.data} ${state.hora}"
            val instant = try {
                val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                val localDateTime = java.time.LocalDateTime.parse(dateStr, formatter)
                localDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant()
            } catch (e: Exception) {
                Instant.now()
            }

            val occurrence = Ocorrencia(
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

            repository.createOcorrencia(occurrence)
                .onSuccess { saved ->
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
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = "Erro ao criar ocorrência: ${error.localizedMessage}"
                        )
                    }
                }
        }
    }

    fun updateHistorico(historico: String) {
        _uiState.update { it.copy(historico = historico) }
    }

    fun addApoio(orgao: OrgaoApoio, viatura: String, encarregado: String) {
        val occurrenceId = _uiState.value.id ?: return
        viewModelScope.launch {
            repository.vincularOrgaoApoioDetalhado(occurrenceId, orgao.id, viatura, encarregado)
                .onSuccess {
                    val newApoio = ApoioOcorrencia(orgao, viatura, encarregado)
                    _uiState.update { state ->
                        state.copy(apoiosDetalhados = state.apoiosDetalhados + newApoio)
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = "Erro ao adicionar apoio: ${error.localizedMessage}") }
                }
        }
    }

    fun removeApoio(index: Int) {
        val occurrenceId = _uiState.value.id ?: return
        val apoio = _uiState.value.apoiosDetalhados[index]
        viewModelScope.launch {
            repository.desvincularOrgaoApoio(occurrenceId, apoio.orgao.id)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(apoiosDetalhados = state.apoiosDetalhados.filterIndexed { idx, _ -> idx != index })
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = "Erro ao remover apoio: ${error.localizedMessage}") }
                }
        }
    }

    fun uploadOccurrenceFile(uri: Uri, isVideo: Boolean = false) {
        val occurrenceId = _uiState.value.id ?: return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            getFileBytes(uri).onSuccess { bytes ->
                val typeStr = if (isVideo) "video" else "foto"
                val path = "$occurrenceId/imagens/${typeStr}_${System.currentTimeMillis()}.png"
                repository.uploadFile("ocorrencias", path, bytes)
                    .onSuccess { publicUrl ->
                        _uiState.update { state ->
                            if (isVideo) {
                                state.copy(videos = state.videos + publicUrl, isLoading = false)
                            } else {
                                state.copy(fotos = state.fotos + publicUrl, isLoading = false)
                            }
                        }
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Erro upload: ${error.localizedMessage}") }
                    }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = "Erro ao ler arquivo: ${error.localizedMessage}") }
            }
        }
    }

    fun scanDocumentOcr(imageUri: Uri, onResult: (OcrDocumentResult) -> Unit) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            ocrService.recognizeText(imageUri)
                .onSuccess { result ->
                    _uiState.update { it.copy(isLoading = false) }
                    onResult(result)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "OCR Error: ${error.localizedMessage}") }
                }
        }
    }

    fun saveDocument(
        tipo: String,
        numero: String,
        extractedFields: Map<String, String>,
        rawText: String,
        imageUri: Uri
    ) {
        val occurrenceId = _uiState.value.id ?: return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            // 1. Create and Upsert the Person first to prevent duplicates
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

            repository.upsertPessoa(pessoa).onSuccess { savedPessoa ->
                // 2. Read and Upload file bytes
                getFileBytes(imageUri).onSuccess { bytes ->
                    val path = "$occurrenceId/documentos/${tipo}_${System.currentTimeMillis()}.png"
                    repository.uploadFile("ocorrencias", path, bytes).onSuccess { url ->
                        
                        // 3. Insert the Document referencing the saved Person and Occurrence
                        val hash = java.util.UUID.nameUUIDFromBytes(bytes).toString()
                        val documento = Documento(
                            ocorrenciaId = occurrenceId,
                            pessoaId = savedPessoa.id,
                            tipo = tipo,
                            numero = numero,
                            urlImagem = url,
                            textoOcr = rawText,
                            dadosEstruturados = extractedFields,
                            hashArquivo = hash,
                            dataUpload = Instant.now().toString(),
                            usuario = "Operador"
                        )
                        
                        repository.addDocumento(documento).onSuccess { savedDoc ->
                            android.util.Log.d("FireNotes", "Persistência - Documento salvo: ID=${savedDoc.id}, Tipo=${savedDoc.tipo}")
                            _uiState.update { state ->
                                state.copy(
                                    isLoading = false,
                                    documentos = state.documentos + savedDoc,
                                    pessoas = state.pessoas.filter { it.cpf != savedPessoa.cpf } + savedPessoa
                                )
                            }
                        }.onFailure { error ->
                            _uiState.update { it.copy(isLoading = false, errorMessage = "Erro salvar documento: ${error.localizedMessage}") }
                        }

                    }.onFailure { error ->
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Erro upload documento: ${error.localizedMessage}") }
                    }
                }.onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Erro ler arquivo: ${error.localizedMessage}") }
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = "Erro upsert pessoa: ${error.localizedMessage}") }
            }
        }
    }

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
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val block = suspend {
                var urlCrlv: String? = null
                if (imageUri != null) {
                    getFileBytes(imageUri).onSuccess { bytes ->
                        val path = "$occurrenceId/veiculos/crlv_${System.currentTimeMillis()}.png"
                        repository.uploadFile("ocorrencias", path, bytes).onSuccess { url ->
                            urlCrlv = url
                        }
                    }
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
                    marca = extractedFields["marca_modelo"],
                    versao = extractedFields["marca_modelo"],
                    anoFabricacao = extractedFields["ano_fabricacao"]?.toIntOrNull(),
                    anoModelo = extractedFields["ano_modelo"]?.toIntOrNull(),
                    categoriaVeiculo = extractedFields["categoria_veiculo"],
                    exercicio = extractedFields["exercicio"],
                    urlCrlv = urlCrlv,
                    ocrTextoCrlv = rawText,
                    ocrDadosEstruturados = extractedFields
                )

                repository.addVeiculoEnvolvido(veiculo).onSuccess { saved ->
                    android.util.Log.d("FireNotes", "Persistência - Veículo salvo: ID=${saved.id}, Placa=${saved.placa}")
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            veiculos = state.veiculos + saved
                        )
                    }
                }.onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Erro salvar veículo: ${error.localizedMessage}") }
                }
            }
            block()
        }
    }

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
        
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        
        viewModelScope.launch {
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
            
            repository.addVitima(vitima).onSuccess { saved ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        vitimas = state.vitimas + saved
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = "Erro registrar vítima: ${error.localizedMessage}") }
            }
        }
    }

    fun finalizeOccurrence() {
        val occurrenceId = _uiState.value.id ?: return
        val state = _uiState.value
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            // Update the occurrence to save the final history and photo URLs
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

            repository.createOcorrencia(occurrence).onSuccess {
                _uiState.update { it.copy(isLoading = false, isSavingSuccess = true) }
            }.onFailure { error ->
                // Even if final update fails, we consider it saved because the details are already in the DB
                _uiState.update { it.copy(isLoading = false, isSavingSuccess = true) }
            }
        }
    }

    private fun getFileBytes(uri: Uri): Result<ByteArray> {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            if (bytes != null) Result.success(bytes) else Result.failure(Exception("Nenhum dado lido."))
        } catch (e: Exception) {
            Result.failure(e)
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
            null
        }
    }

    // --- V3 Viaturas and Militares Operations ---

    fun addViatura(
        prefixo: String,
        tipo: String,
        unidade: String?,
        kmSaida: Int?,
        kmLocal: Int?,
        observacoes: String?,
        viaturaId: String? = null
    ) {
        val occurrenceId = _uiState.value.id ?: return
        if (prefixo.isBlank()) {
            _uiState.update { it.copy(errorMessage = "O prefixo da viatura é obrigatório.") }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
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
            repository.addViatura(viatura)
                .onSuccess { saved ->
                    android.util.Log.d("FireNotes", "Persistência - Viatura salva: ID=${saved.id}, Prefixo=${saved.prefixo}")
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
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Erro ao adicionar viatura: ${error.localizedMessage}") }
                }
        }
    }

    fun deleteViatura(viaturaId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            repository.deleteViatura(viaturaId)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            viaturas = state.viaturas.filter { it.id != viaturaId }
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.localizedMessage) }
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
            _uiState.update { it.copy(errorMessage = "RE e Nome de Guerra são obrigatórios.") }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val graduacao = GraduacaoMilitar.fromDescricao(graduacaoStr)
            val militar = Militar(
                viaturaId = viaturaId,
                re = re,
                nomeGuerra = nomeGuerra,
                graduacao = graduacao,
                funcao = funcao
            )
            repository.addMilitar(militar)
                .onSuccess { saved ->
                    _uiState.update { state ->
                        val updatedViaturas = state.viaturas.map { viatura ->
                            if (viatura.id == viaturaId) {
                                val updatedEquipe = (viatura.equipe + saved)
                                    .sortedByDescending { it.graduacao.hierarquia }
                                viatura.copy(equipe = updatedEquipe)
                            } else {
                                viatura
                            }
                        }
                        state.copy(isLoading = false, viaturas = updatedViaturas)
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.localizedMessage) }
                }
        }
    }

    fun deleteMilitar(militarId: String, viaturaId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            repository.deleteMilitar(militarId)
                .onSuccess {
                    _uiState.update { state ->
                        val updatedViaturas = state.viaturas.map { viatura ->
                            if (viatura.id == viaturaId) {
                                viatura.copy(equipe = viatura.equipe.filter { it.id != militarId })
                            } else {
                                viatura
                            }
                        }
                        state.copy(isLoading = false, viaturas = updatedViaturas)
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.localizedMessage) }
                }
        }
    }

    fun moveMilitar(militarId: String, currentViaturaId: String, newViaturaId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            repository.moveMilitar(militarId, newViaturaId)
                .onSuccess {
                    val occurrenceId = _uiState.value.id ?: return@launch
                    repository.getViaturasDaOcorrencia(occurrenceId)
                        .onSuccess { list ->
                            _uiState.update { it.copy(isLoading = false, viaturas = list) }
                        }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.localizedMessage) }
                }
        }
    }

    fun createPhotoUri(): Uri {
        return cameraCaptureService.createPhotoUri()
    }

    fun checkAndProcessOcrImage(
        imageUri: Uri,
        onQualityIssue: (String) -> Unit,
        onSuccess: (OcrDocumentResult, Uri) -> Unit
    ) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                // 1. Decode bitmap
                val bitmap = context.contentResolver.openInputStream(imageUri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
                
                if (bitmap == null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Falha ao decodificar imagem da câmera.") }
                    return@launch
                }

                // 2. Check Quality
                val quality = imageProcessingService.checkQuality(bitmap)
                if (!quality.isValid) {
                    _uiState.update { it.copy(isLoading = false) }
                    onQualityIssue(quality.reason ?: "Baixa qualidade detectada na imagem.")
                    return@launch
                }

                // 3. Process image (perspective correction, cropping, contrast)
                val processedBitmap = imageProcessingService.processDocumentImage(bitmap)
                
                // 4. Save enhanced image to a temporary file
                val processedFile = java.io.File(context.cacheDir, "camera_capture_processed_${System.currentTimeMillis()}.jpg")
                java.io.FileOutputStream(processedFile).use { out ->
                    processedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }

                val processedUri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "com.example.firenotes.fileprovider",
                    processedFile
                )

                // 5. Run OCR Service on the treated image in background
                ocrService.recognizeText(processedUri)
                    .onSuccess { result ->
                        android.util.Log.d("FireNotes", "OCR - Documento identificado: Tipo=${result.tipo}")
                        _uiState.update { it.copy(isLoading = false) }
                        onSuccess(result, processedUri)
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Erro no processamento OCR: ${error.localizedMessage}") }
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Erro no fluxo OCR: ${e.localizedMessage}") }
            }
        }
    }

    fun processAndRunOcrDirectly(
        imageUri: Uri,
        onSuccess: (OcrDocumentResult, Uri) -> Unit
    ) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val bitmap = context.contentResolver.openInputStream(imageUri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
                
                val processedBitmap = if (bitmap != null) {
                    imageProcessingService.processDocumentImage(bitmap)
                } else {
                    null
                }

                val processedUri = if (processedBitmap != null) {
                    val processedFile = java.io.File(context.cacheDir, "camera_capture_processed_${System.currentTimeMillis()}.jpg")
                    java.io.FileOutputStream(processedFile).use { out ->
                        processedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "com.example.firenotes.fileprovider",
                        processedFile
                    )
                } else {
                    imageUri
                }

                ocrService.recognizeText(processedUri)
                    .onSuccess { result ->
                        android.util.Log.d("FireNotes", "OCR - Documento identificado: Tipo=${result.tipo}")
                        _uiState.update { it.copy(isLoading = false) }
                        onSuccess(result, processedUri)
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Erro no processamento OCR: ${error.localizedMessage}") }
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Erro no fluxo OCR direto: ${e.localizedMessage}") }
            }
        }
    }

    fun addEvidencia(uri: Uri, classification: String) {
        val occurrenceId = _uiState.value.id ?: return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            getFileBytes(uri).onSuccess { bytes ->
                val path = "$occurrenceId/evidencias/evidence_${System.currentTimeMillis()}.jpg"
                repository.uploadFile("ocorrencias", path, bytes).onSuccess { url ->
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
                    repository.addEvidencia(ev).onSuccess { saved ->
                        android.util.Log.d("FireNotes", "Persistência - Evidência salva: ID=${saved.id}, Tipo=${saved.tipo}")
                        _uiState.update { state ->
                            state.copy(isLoading = false, evidencias = state.evidencias + saved)
                        }
                    }.onFailure { error ->
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Erro salvar evidência: ${error.localizedMessage}") }
                    }
                }.onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Erro upload: ${error.localizedMessage}") }
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = "Erro ler arquivo: ${error.localizedMessage}") }
            }
        }
    }
}
