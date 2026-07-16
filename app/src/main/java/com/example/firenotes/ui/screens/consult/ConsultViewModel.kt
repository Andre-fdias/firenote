package com.example.firenotes.ui.screens.consult

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firenotes.domain.model.*
import com.example.firenotes.domain.repository.OcorrenciaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class ConsultFilters(
    val talao: String = "",
    val dataInicial: String = "",
    val dataFinal: String = "",
    val natureza: NaturezaOcorrencia? = null,
    val cidade: String = "",
    val bairro: String = "",
    val viatura: String = "",
    val militar: String = "",
    val placa: String = "",
    val cpf: String = "",
    val nome: String = "",
    val envolvido: String = "",
    val hospital: String = "",
    val status: String = "", // Aberto, Encerrado
    val usuario: String = "",
    val dataFiltro: String = ""
)

enum class ConsultSort {
    RECENTES,
    ANTIGAS,
    TALAO,
    VITIMAS,
    VEICULOS
}

data class ConsultUiState(
    val occurrences: List<Ocorrencia> = emptyList(),
    val filteredOccurrences: List<Ocorrencia> = emptyList(),
    val searchGlobal: String = "",
    val filters: ConsultFilters = ConsultFilters(),
    val sortBy: ConsultSort = ConsultSort.RECENTES,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedOccurrence: Ocorrencia? = null,
    val showDetailsDialog: Boolean = false
)

@HiltViewModel
class ConsultViewModel @Inject constructor(
    private val repository: OcorrenciaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConsultUiState())
    val uiState: StateFlow<ConsultUiState> = _uiState.asStateFlow()

    init {
        loadOccurrences()
    }

    fun loadOccurrences() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            repository.getOcorrencias()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
                }
                .collect { list ->
                    // Use pre-fetched list directly to avoid redundant N+1 queries
                    _uiState.update { state ->
                        state.copy(
                            occurrences = list,
                            isLoading = false
                        )
                    }
                    applyFiltersAndSort()
                }
        }
    }

    fun updateSearchGlobal(query: String) {
        _uiState.update { it.copy(searchGlobal = query) }
        applyFiltersAndSort()
    }

    fun updateFilters(filters: ConsultFilters) {
        _uiState.update { it.copy(filters = filters) }
        applyFiltersAndSort()
    }

    fun updateSort(sort: ConsultSort) {
        _uiState.update { it.copy(sortBy = sort) }
        applyFiltersAndSort()
    }

    fun selectOccurrence(occurrence: Ocorrencia) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getOcorrenciaById(occurrence.id ?: "").fold(
                onSuccess = { fullOcorrencia ->
                    _uiState.update { it.copy(selectedOccurrence = fullOcorrencia, showDetailsDialog = true, isLoading = false) }
                },
                onFailure = {
                    _uiState.update { it.copy(selectedOccurrence = occurrence, showDetailsDialog = true, isLoading = false) }
                }
            )
        }
    }

    fun dismissDetails() {
        _uiState.update { it.copy(showDetailsDialog = false, selectedOccurrence = null) }
    }

    fun deleteOccurrence(id: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            repository.deleteOcorrencia(id).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, showDetailsDialog = false, selectedOccurrence = null) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Falha ao deletar ocorrência: ${e.localizedMessage}") }
                }
            )
        }
    }

    fun duplicateOccurrence(occurrence: Ocorrencia, onDuplicated: (Ocorrencia) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getOcorrenciaById(occurrence.id ?: "").onSuccess { full ->
                // Create a duplicate draft without talão (protocolo), data, hora
                val clone = full.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    protocolo = "", // Clear talão
                    dataHora = java.time.Instant.now(),
                    viaturas = emptyList(),
                    vitimas = emptyList(),
                    veiculos = emptyList()
                )
                repository.createOcorrencia(clone).onSuccess { saved ->
                    val newOcorrenciaId = saved.id!!
                    
                    // Duplicate all related items
                    full.viaturas.forEach { v ->
                        repository.addViatura(v.copy(id = java.util.UUID.randomUUID().toString(), ocorrenciaId = newOcorrenciaId)).onSuccess { savedViatura ->
                            val newViaturaId = savedViatura.id!!
                            v.equipe.forEach { m ->
                                repository.addMilitar(m.copy(id = java.util.UUID.randomUUID().toString(), viaturaId = newViaturaId))
                            }
                        }
                    }

                    full.veiculos.forEach { vc ->
                        repository.addVeiculoEnvolvido(vc.copy(id = java.util.UUID.randomUUID().toString(), ocorrenciaId = newOcorrenciaId))
                    }

                    full.vitimas.forEach { vt ->
                        repository.addVitima(vt.copy(id = java.util.UUID.randomUUID().toString(), ocorrenciaId = newOcorrenciaId))
                    }

                    repository.getOcorrenciaById(newOcorrenciaId).onSuccess { fullySaved ->
                        _uiState.update { it.copy(isLoading = false) }
                        onDuplicated(fullySaved)
                    }.onFailure {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Erro ao carregar cópia da ocorrência.") }
                    }
                }.onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Falha ao salvar cópia: ${e.localizedMessage}") }
                }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Falha ao recuperar dados da ocorrência para duplicar.") }
            }
        }
    }

    private fun applyFiltersAndSort() {
        val state = _uiState.value
        var list = state.occurrences

        // 1. Global Search
        if (state.searchGlobal.isNotBlank()) {
            val q = state.searchGlobal.lowercase().trim()
            list = list.filter { o ->
                o.protocolo.lowercase().contains(q) ||
                o.historico?.lowercase()?.contains(q) == true ||
                o.rua?.lowercase()?.contains(q) == true ||
                o.cidade?.lowercase()?.contains(q) == true ||
                o.viaturas.any { v -> v.prefixo.lowercase().contains(q) || v.equipe.any { m -> m.nomeGuerra.lowercase().contains(q) || m.re.lowercase().contains(q) } } ||
                o.vitimas.any { vt -> vt.nome?.lowercase()?.contains(q) == true } ||
                o.veiculos.any { vc -> vc.placa?.lowercase()?.contains(q) == true || vc.modelo?.lowercase()?.contains(q) == true }
            }
        }

        // 2. Specific Filters
        val f = state.filters
        if (f.dataFiltro.isNotBlank()) {
            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val targetDate = try { java.time.LocalDate.parse(f.dataFiltro, formatter) } catch (e: Exception) { null }
            if (targetDate != null) {
                list = list.filter {
                    val occurrenceDate = it.dataHora.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    occurrenceDate == targetDate
                }
            }
        }
        if (f.natureza != null) {
            list = list.filter { it.natureza == f.natureza }
        }
        if (f.cidade.isNotBlank()) {
            list = list.filter { it.cidade?.equals(f.cidade, ignoreCase = true) == true }
        }
        if (f.bairro.isNotBlank()) {
            list = list.filter { it.bairro?.equals(f.bairro, ignoreCase = true) == true }
        }
        if (f.viatura.isNotBlank()) {
            list = list.filter { o -> o.viaturas.any { it.prefixo.equals(f.viatura, ignoreCase = true) } }
        }
        if (f.militar.isNotBlank()) {
            list = list.filter { o -> o.viaturas.any { v -> v.equipe.any { m -> m.re.contains(f.militar) } } }
        }
        if (f.placa.isNotBlank()) {
            list = list.filter { o -> o.veiculos.any { it.placa?.equals(f.placa, ignoreCase = true) == true } }
        }
        if (f.envolvido.isNotBlank()) {
            val q = f.envolvido.lowercase().trim()
            list = list.filter { o ->
                o.vitimas.any { it.nome.lowercase().contains(q) || it.cpf?.contains(q) == true }
            }
        }
        if (f.status.isNotBlank()) {
            val targetStatus = if (f.status.equals("Aberta", ignoreCase = true) || f.status.equals("Aberto", ignoreCase = true)) "ABERTA" else "ENCERRADA"
            list = list.filter { it.status == targetStatus }
        }

        // 3. Sorting
        list = when (state.sortBy) {
            ConsultSort.RECENTES -> list.sortedByDescending { it.dataHora }
            ConsultSort.ANTIGAS -> list.sortedBy { it.dataHora }
            ConsultSort.TALAO -> list.sortedBy { it.protocolo }
            ConsultSort.VITIMAS -> list.sortedByDescending { it.vitimas.size }
            ConsultSort.VEICULOS -> list.sortedByDescending { it.veiculos.size }
        }

        _uiState.update { it.copy(filteredOccurrences = list) }
    }

    fun importOccurrenceFromJson(jsonStr: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = Json.parseToJsonElement(jsonStr)
                val obj = json.jsonObject

                val protocolo = obj["protocolo"]?.jsonPrimitive?.content ?: ""
                val naturezaStr = obj["natureza"]?.jsonPrimitive?.content ?: "INDEFINIDA"
                val natureza = NaturezaOcorrencia.fromDescricao(naturezaStr)
                val latitude = obj["latitude"]?.jsonPrimitive?.doubleOrNull
                val longitude = obj["longitude"]?.jsonPrimitive?.doubleOrNull
                val dataHoraStr = obj["dataHora"]?.jsonPrimitive?.content ?: ""
                val dataHora = java.time.Instant.parse(dataHoraStr)
                val historico = obj["historico"]?.jsonPrimitive?.content
                val rua = obj["rua"]?.jsonPrimitive?.content
                val numero = obj["numero"]?.jsonPrimitive?.content
                val bairro = obj["bairro"]?.jsonPrimitive?.content
                val cidade = obj["cidade"]?.jsonPrimitive?.content
                val uf = obj["uf"]?.jsonPrimitive?.content
                val status = obj["status"]?.jsonPrimitive?.content ?: "ABERTA"

                val oId = java.util.UUID.randomUUID().toString()

                val ocorrencia = Ocorrencia(
                    id = oId,
                    protocolo = protocolo,
                    natureza = natureza,
                    latitude = latitude,
                    longitude = longitude,
                    dataHora = dataHora,
                    historico = historico,
                    rua = rua,
                    numero = numero,
                    bairro = bairro,
                    cidade = cidade,
                    uf = uf,
                    status = status
                )
                repository.createOcorrencia(ocorrencia).onSuccess { savedOcorrencia ->
                    obj["veiculos"]?.jsonArray?.forEach { vElement ->
                        val vObj = vElement.jsonObject
                        val placa = vObj["placa"]?.jsonPrimitive?.content ?: ""
                        val cor = vObj["cor"]?.jsonPrimitive?.content ?: ""
                        val chassi = vObj["chassi"]?.jsonPrimitive?.content ?: ""
                        val modelo = vObj["modelo"]?.jsonPrimitive?.content ?: ""
                        val marca = vObj["marca"]?.jsonPrimitive?.content ?: ""

                        repository.addVeiculoEnvolvido(
                            VeiculoEnvolvido(
                                id = java.util.UUID.randomUUID().toString(),
                                ocorrenciaId = oId,
                                placa = placa,
                                cor = cor,
                                chassi = chassi,
                                modelo = modelo,
                                marca = marca
                            )
                        )
                    }

                    obj["vitimas"]?.jsonArray?.forEach { vtElement ->
                        val vtObj = vtElement.jsonObject
                        val nome = vtObj["nome"]?.jsonPrimitive?.content ?: ""
                        val idade = vtObj["idade"]?.jsonPrimitive?.intOrNull
                        val destinoSocorro = vtObj["destinoSocorro"]?.jsonPrimitive?.content ?: ""
                        val quemSocorreu = vtObj["quemSocorreu"]?.jsonPrimitive?.content ?: ""
                        val resultadoOcorrencia = vtObj["resultadoOcorrencia"]?.jsonPrimitive?.content ?: ""
                        val hospitalDestino = vtObj["hospitalDestino"]?.jsonPrimitive?.content ?: ""
                        val nomeMedico = vtObj["nomeMedico"]?.jsonPrimitive?.content ?: ""
                        val crmMedico = vtObj["crmMedico"]?.jsonPrimitive?.content ?: ""
                        val cpf = vtObj["cpf"]?.jsonPrimitive?.content

                        var pessoaId: String? = null
                        if (!cpf.isNullOrBlank()) {
                            val pessoa = Pessoa(
                                id = java.util.UUID.randomUUID().toString(),
                                nome = nome,
                                cpf = cpf
                            )
                            repository.upsertPessoa(pessoa).onSuccess { p ->
                                pessoaId = p.id
                            }
                        }

                        repository.addVitima(
                            Vitima(
                                id = java.util.UUID.randomUUID().toString(),
                                ocorrenciaId = oId,
                                nome = nome,
                                idade = idade,
                                destinoSocorro = destinoSocorro,
                                quemSocorreu = quemSocorreu,
                                resultadoOcorrencia = resultadoOcorrencia,
                                hospitalDestino = hospitalDestino,
                                nomeMedico = nomeMedico,
                                crmMedico = crmMedico,
                                pessoaId = pessoaId
                            )
                        )
                    }

                    obj["viaturas"]?.jsonArray?.forEach { viatElement ->
                        val viatObj = viatElement.jsonObject
                        val prefixo = viatObj["prefixo"]?.jsonPrimitive?.content ?: ""
                        val tipo = viatObj["tipo"]?.jsonPrimitive?.content ?: ""
                        val unidade = viatObj["unidade"]?.jsonPrimitive?.content ?: ""
                        val kmSaida = viatObj["kmSaida"]?.jsonPrimitive?.intOrNull
                        val kmLocal = viatObj["kmLocal"]?.jsonPrimitive?.intOrNull
                        val kmRetorno = viatObj["kmRetorno"]?.jsonPrimitive?.intOrNull
                        val horaDespacho = viatObj["horaDespacho"]?.jsonPrimitive?.content ?: ""
                        val horaSaida = viatObj["horaSaida"]?.jsonPrimitive?.content ?: ""
                        val horaChegada = viatObj["horaChegada"]?.jsonPrimitive?.content ?: ""
                        val horaRetorno = viatObj["horaRetorno"]?.jsonPrimitive?.content ?: ""
                        val observacoes = viatObj["observacoes"]?.jsonPrimitive?.content ?: ""

                        val viatId = java.util.UUID.randomUUID().toString()
                        repository.addViatura(
                            Viatura(
                                id = viatId,
                                ocorrenciaId = oId,
                                prefixo = prefixo,
                                tipo = tipo,
                                unidade = unidade,
                                kmSaida = kmSaida,
                                kmLocal = kmLocal,
                                kmRetorno = kmRetorno,
                                horaDespacho = horaDespacho,
                                horaSaida = horaSaida,
                                horaChegada = horaChegada,
                                horaRetorno = horaRetorno,
                                observacoes = observacoes
                            )
                        ).onSuccess { savedViatura ->
                            viatObj["equipe"]?.jsonArray?.forEach { milElement ->
                                val milObj = milElement.jsonObject
                                val re = milObj["re"]?.jsonPrimitive?.content ?: ""
                                val nomeGuerra = milObj["nomeGuerra"]?.jsonPrimitive?.content ?: ""
                                val graduacao = milObj["graduacao"]?.jsonPrimitive?.content ?: ""
                                val funcao = milObj["funcao"]?.jsonPrimitive?.content ?: ""

                                repository.addMilitar(
                                    Militar(
                                        id = java.util.UUID.randomUUID().toString(),
                                        viaturaId = viatId,
                                        re = re,
                                        nomeGuerra = nomeGuerra,
                                        graduacao = graduacao,
                                        funcao = funcao
                                    )
                                )
                            }
                        }
                    }

                    // Importar apoios
                    obj["apoios"]?.jsonArray?.forEach { apElement ->
                        val apObj = apElement.jsonObject
                        val orgaoId = apObj["orgaoId"]?.jsonPrimitive?.content ?: ""
                        val orgaoSigla = apObj["orgaoSigla"]?.jsonPrimitive?.content ?: ""
                        val orgaoNome = apObj["orgaoNome"]?.jsonPrimitive?.content ?: ""
                        val viat = apObj["viatura"]?.jsonPrimitive?.content ?: ""
                        val enc = apObj["encarregado"]?.jsonPrimitive?.content ?: ""
                        val desc = apObj["descricaoOutros"]?.jsonPrimitive?.content ?: ""

                        repository.vincularOrgaoApoioDetalhado(
                            ocorrenciaId = oId,
                            orgaoId = orgaoId,
                            viatura = viat,
                            encarregado = enc
                        )
                    }

                    // Importar pessoas (não vítimas)
                    obj["pessoas"]?.jsonArray?.forEach { pElement ->
                        val pObj = pElement.jsonObject
                        val nome = pObj["nome"]?.jsonPrimitive?.content ?: ""
                        val cpf = pObj["cpf"]?.jsonPrimitive?.content
                        val rg = pObj["rg"]?.jsonPrimitive?.content
                        val reOrgao = pObj["rgOrgaoEmissor"]?.jsonPrimitive?.content
                        val rgUf = pObj["rgUf"]?.jsonPrimitive?.content
                        val nascimento = pObj["nascimento"]?.jsonPrimitive?.content
                        val naturalidade = pObj["naturalidade"]?.jsonPrimitive?.content
                        val nacionalidade = pObj["nacionalidade"]?.jsonPrimitive?.content
                        val filiacao = pObj["filiacao"]?.jsonPrimitive?.content
                        val sexo = pObj["sexo"]?.jsonPrimitive?.content
                        val telefone = pObj["telefone"]?.jsonPrimitive?.content
                        val email = pObj["email"]?.jsonPrimitive?.content
                        val logradouro = pObj["logradouro"]?.jsonPrimitive?.content
                        val numero = pObj["numero"]?.jsonPrimitive?.content
                        val bairro = pObj["bairro"]?.jsonPrimitive?.content
                        val cidade = pObj["cidade"]?.jsonPrimitive?.content
                        val uf = pObj["uf"]?.jsonPrimitive?.content
                        val cep = pObj["cep"]?.jsonPrimitive?.content

                        val pessoa = Pessoa(
                            id = java.util.UUID.randomUUID().toString(),
                            nome = nome,
                            cpf = cpf,
                            rg = rg,
                            rgOrgaoEmissor = reOrgao,
                            rgUf = rgUf,
                            nascimento = nascimento,
                            naturalidade = naturalidade,
                            nacionalidade = nacionalidade,
                            filiacao = filiacao,
                            sexo = sexo,
                            telefone = telefone,
                            email = email,
                            logradouro = logradouro,
                            numero = numero,
                            bairro = bairro,
                            cidade = cidade,
                            uf = uf,
                            cep = cep
                        )
                        repository.upsertPessoa(pessoa).onSuccess { p ->
                            // Para vincular a pessoa à ocorrência como envolvido (documento de identificação fictício)
                            val docId = java.util.UUID.randomUUID().toString()
                            val doc = com.example.firenotes.domain.model.Documento(
                                id = docId,
                                ocorrenciaId = oId,
                                pessoaId = p.id,
                                tipo = "IDENTIFICACAO",
                                numero = cpf ?: rg ?: "S/N",
                                urlImagem = "",
                                textoOcr = "",
                                dadosEstruturados = emptyMap()
                            )
                            repository.salvarPessoaEDocumento(p, doc)
                        }
                    }

                    onSuccess()
                }.onFailure { e ->
                    onError(e.localizedMessage ?: "Erro ao salvar ocorrência importada")
                }
            } catch (e: Exception) {
                onError("Estrutura JSON inválida: ${e.localizedMessage}")
            }
        }
    }
}
