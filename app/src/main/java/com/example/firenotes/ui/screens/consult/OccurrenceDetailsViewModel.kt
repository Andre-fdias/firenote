package com.example.firenotes.ui.screens.consult

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.net.Uri
import java.io.File
import com.example.firenotes.domain.model.Ocorrencia
import com.example.firenotes.domain.model.Evidencia
import com.example.firenotes.domain.model.Documento
import com.example.firenotes.domain.repository.OcorrenciaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OccurrenceDetailsUiState(
    val occurrence: Ocorrencia? = null,
    val evidencias: List<Evidencia> = emptyList(),
    val pessoas: List<com.example.firenotes.domain.model.Pessoa> = emptyList(),
    val documentos: List<Documento> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class OccurrenceDetailsViewModel @Inject constructor(
    private val repository: OcorrenciaRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val occurrenceId: String? = savedStateHandle["occurrenceId"]

    private val _uiState = MutableStateFlow(OccurrenceDetailsUiState())
    val uiState: StateFlow<OccurrenceDetailsUiState> = _uiState.asStateFlow()

    init {
        loadOccurrence()
    }

    fun loadOccurrence() {
        val id = occurrenceId
        if (id.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "ID da ocorrência inválido ou não informado") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val occurrenceResult = repository.getOcorrenciaById(id)
            val evidenciasResult = repository.getEvidencias(id)
            val pessoasResult = repository.getPessoasDaOcorrencia(id)
            val documentosResult = repository.getDocumentosDaOcorrencia(id)
            
            if (occurrenceResult.isSuccess) {
                val fullOcorrencia = occurrenceResult.getOrThrow()
                val evidencias = evidenciasResult.getOrDefault(emptyList())
                val pessoas = pessoasResult.getOrDefault(emptyList())
                val documentos = documentosResult.getOrDefault(emptyList())
                _uiState.update { it.copy(
                    occurrence = fullOcorrencia, 
                    evidencias = evidencias, 
                    pessoas = pessoas,
                    documentos = documentos,
                    isLoading = false
                ) }
            } else {
                val e = occurrenceResult.exceptionOrNull()
                _uiState.update { it.copy(isLoading = false, errorMessage = "Falha ao carregar ocorrência: ${e?.localizedMessage}") }
            }
        }
    }

    fun deleteOccurrence(onSuccess: () -> Unit) {
        val id = occurrenceId ?: return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            repository.deleteOcorrencia(id).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Falha ao deletar ocorrência: ${e.localizedMessage}") }
                }
            )
        }
    }

    fun updateOccurrenceStatus(newStatus: String) {
        val o = _uiState.value.occurrence ?: return
        val updated = o.copy(status = newStatus)
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            repository.createOcorrencia(updated).fold(
                onSuccess = { saved ->
                    _uiState.update { it.copy(occurrence = saved, isLoading = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Falha ao atualizar status: ${e.localizedMessage}") }
                }
            )
        }
    }

    fun exportOccurrenceJson(context: Context, onCompleted: (Uri) -> Unit) {
        val o = _uiState.value.occurrence ?: return
        viewModelScope.launch {
            try {
                val json = buildString {
                    append("{")
                    append("\"protocolo\":\"${o.protocolo}\",")
                    append("\"natureza\":\"${o.natureza.name}\",")
                    append("\"latitude\":${o.latitude},")
                    append("\"longitude\":${o.longitude},")
                    append("\"dataHora\":\"${o.dataHora}\",")
                    append("\"historico\":\"${o.historico?.replace("\"", "\\\"")?.replace("\n", "\\n") ?: ""}\",")
                    append("\"rua\":\"${o.rua ?: ""}\",")
                    append("\"numero\":\"${o.numero ?: ""}\",")
                    append("\"bairro\":\"${o.bairro ?: ""}\",")
                    append("\"cidade\":\"${o.cidade ?: ""}\",")
                    append("\"uf\":\"${o.uf ?: ""}\",")
                    append("\"status\":\"${o.status}\",")

                    // Veículos
                    append("\"veiculos\":[")
                    o.veiculos.forEachIndexed { i, vc ->
                        if (i > 0) append(",")
                        append("{")
                        append("\"placa\":\"${vc.placa}\",")
                        append("\"cor\":\"${vc.cor}\",")
                        append("\"chassi\":\"${vc.chassi}\",")
                        append("\"modelo\":\"${vc.modelo}\",")
                        append("\"marca\":\"${vc.marca}\"")
                        append("}")
                    }
                    append("],")

                    // Vítimas
                    append("\"vitimas\":[")
                    o.vitimas.forEachIndexed { i, vt ->
                        if (i > 0) append(",")
                        append("{")
                        append("\"nome\":\"${vt.nome}\",")
                        append("\"idade\":${vt.idade ?: "null"},")
                        append("\"destinoSocorro\":\"${vt.destinoSocorro}\",")
                        append("\"quemSocorreu\":\"${vt.quemSocorreu}\",")
                        append("\"resultadoOcorrencia\":\"${vt.resultadoOcorrencia}\",")
                        append("\"hospitalDestino\":\"${vt.hospitalDestino}\",")
                        append("\"nomeMedico\":\"${vt.nomeMedico}\",")
                        append("\"crmMedico\":\"${vt.crmMedico}\",")
                        append("\"cpf\":\"${vt.cpf ?: ""}\"")
                        append("}")
                    }
                    append("],")

                    // Viaturas
                    append("\"viaturas\":[")
                    o.viaturas.forEachIndexed { i, v ->
                        if (i > 0) append(",")
                        append("{")
                        append("\"prefixo\":\"${v.prefixo}\",")
                        append("\"tipo\":\"${v.tipo}\",")
                        append("\"unidade\":\"${v.unidade}\",")
                        append("\"kmSaida\":${v.kmSaida ?: "null"},")
                        append("\"kmLocal\":${v.kmLocal ?: "null"},")
                        append("\"kmRetorno\":${v.kmRetorno ?: "null"},")
                        append("\"horaDespacho\":\"${v.horaDespacho ?: ""}\",")
                        append("\"horaSaida\":\"${v.horaSaida ?: ""}\",")
                        append("\"horaChegada\":\"${v.horaChegada ?: ""}\",")
                        append("\"horaRetorno\":\"${v.horaRetorno ?: ""}\",")
                        append("\"observacoes\":\"${v.observacoes.replace("\"", "\\\"").replace("\n", "\\n")}\",")
                        // Equipe
                        append("\"equipe\":[")
                        v.equipe.forEachIndexed { j, m ->
                            if (j > 0) append(",")
                            append("{")
                            append("\"re\":\"${m.re}\",")
                            append("\"nomeGuerra\":\"${m.nomeGuerra}\",")
                            append("\"graduacao\":\"${m.graduacao}\",")
                            append("\"funcao\":\"${m.funcao}\"")
                            append("}")
                        }
                        append("]")
                        append("}")
                    }
                    append("],")

                    // Apoios
                    append("\"apoios\":[")
                    o.apoiosDetalhados.forEachIndexed { i, ap ->
                        if (i > 0) append(",")
                        append("{")
                        append("\"orgaoId\":\"${ap.orgaoId}\",")
                        append("\"orgaoSigla\":\"${ap.orgaoSigla}\",")
                        append("\"orgaoNome\":\"${ap.orgaoNome}\",")
                        append("\"viatura\":\"${ap.viatura}\",")
                        append("\"encarregado\":\"${ap.encarregado}\",")
                        append("\"descricaoOutros\":\"${ap.descricaoOutros}\"")
                        append("}")
                    }
                    append("],")
                    
                    // Pessoas (não vítimas)
                    val vitimasNomes = o.vitimas.map { it.nome.trim().lowercase() }.toSet()
                    val naoVitimas = _uiState.value.pessoas.filter { !vitimasNomes.contains(it.nome.trim().lowercase()) }
                    append("\"pessoas\":[")
                    naoVitimas.forEachIndexed { i, p ->
                        if (i > 0) append(",")
                        append("{")
                        append("\"nome\":\"${p.nome}\",")
                        append("\"nomeSocial\":\"${p.nomeSocial ?: ""}\",")
                        append("\"cpf\":\"${p.cpf ?: ""}\",")
                        append("\"rg\":\"${p.rg ?: ""}\",")
                        append("\"rgOrgaoEmissor\":\"${p.rgOrgaoEmissor ?: ""}\",")
                        append("\"rgUf\":\"${p.rgUf ?: ""}\",")
                        append("\"nascimento\":\"${p.nascimento ?: ""}\",")
                        append("\"naturalidade\":\"${p.naturalidade ?: ""}\",")
                        append("\"nacionalidade\":\"${p.nacionalidade ?: ""}\",")
                        append("\"filiacao\":\"${p.filiacao ?: ""}\",")
                        append("\"sexo\":\"${p.sexo ?: ""}\",")
                        append("\"telefone\":\"${p.telefone ?: ""}\",")
                        append("\"email\":\"${p.email ?: ""}\",")
                        append("\"logradouro\":\"${p.logradouro ?: ""}\",")
                        append("\"numero\":\"${p.numero ?: ""}\",")
                        append("\"bairro\":\"${p.bairro ?: ""}\",")
                        append("\"cidade\":\"${p.cidade ?: ""}\",")
                        append("\"uf\":\"${p.uf ?: ""}\",")
                        append("\"cep\":\"${p.cep ?: ""}\"")
                        append("}")
                    }
                    append("],")

                    // Fotos em Base64
                    append("\"fotosBase64\":[")
                    var encodedCount = 0
                    o.fotos.forEach { path ->
                        val file = File(path)
                        if (file.exists() && file.isFile) {
                            try {
                                val bytes = file.readBytes()
                                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                                if (encodedCount > 0) append(",")
                                append("{")
                                append("\"nome\":\"${file.name}\",")
                                append("\"bytes\":\"$base64\"")
                                append("}")
                                encodedCount++
                            } catch (e: Exception) {
                                android.util.Log.e("JsonExport", "Erro ao codificar imagem: ${e.message}")
                            }
                        }
                    }
                    append("],")

                    // Evidências
                    append("\"evidencias\":[")
                    var encodedEvCount = 0
                    _uiState.value.evidencias.forEach { ev ->
                        val file = File(ev.urlStorage)
                        var base64 = ""
                        if (file.exists() && file.isFile) {
                            try {
                                val bytes = file.readBytes()
                                base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                            } catch (e: java.lang.Exception) {
                                android.util.Log.e("JsonExport", "Erro ao ler arquivo de evidencia: ${e.message}")
                            }
                        }
                        
                        if (encodedEvCount > 0) append(",")
                        append("{")
                        append("\"id\":\"${ev.id ?: ""}\",")
                        append("\"tipo\":\"${ev.tipo}\",")
                        append("\"hashSha256\":\"${ev.hashSha256}\",")
                        append("\"latitude\":${ev.latitude ?: "null"},")
                        append("\"longitude\":${ev.longitude ?: "null"},")
                        append("\"dataHora\":\"${ev.dataHora}\",")
                        append("\"usuario\":\"${ev.usuario ?: ""}\",")
                        append("\"ocrBruto\":\"${ev.ocrBruto?.replace("\"", "\\\"")?.replace("\n", "\\n") ?: ""}\",")
                        
                        // Map jsonOcr
                        append("\"jsonOcr\":{")
                        var entryIdx = 0
                        ev.jsonOcr.entries.forEach { entry ->
                            if (entryIdx > 0) append(",")
                            append("\"${entry.key}\":\"${entry.value.replace("\"", "\\\"")}\"")
                            entryIdx++
                        }
                        append("},")

                        append("\"fileName\":\"${file.name}\",")
                        append("\"bytes\":\"$base64\"")
                        append("}")
                        encodedEvCount++
                    }
                    append("]")

                    append("}")
                }

                val file = File(context.cacheDir, "ocorrencia_${o.protocolo}.json")
                file.writeText(json)
                val uri = androidx.core.content.FileProvider.getUriForFile(context, "com.example.firenotes.fileprovider", file)
                onCompleted(uri)
            } catch (e: Exception) {
                android.util.Log.e("JsonExport", "Erro ao exportar json: ${e.message}", e)
            }
        }
    }

    fun updateOccurrencePhotos(updated: Ocorrencia) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            repository.createOcorrencia(updated).fold(
                onSuccess = { saved ->
                    _uiState.update { it.copy(occurrence = saved, isLoading = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Falha ao salvar imagem: ${e.localizedMessage}") }
                }
            )
        }
    }
}
