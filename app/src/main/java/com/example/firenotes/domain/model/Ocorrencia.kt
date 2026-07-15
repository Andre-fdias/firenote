package com.example.firenotes.domain.model

import java.time.Instant

enum class NaturezaOcorrencia(val descricao: String) {
    INDEFINIDA("Indefinida"),
    INCENDIO("Incêndio"),
    SALVAMENTO("Salvamento"),
    ACIDENTE_TRANSITO("Acidente de Trânsito"),
    QUEDA("Queda"),
    PESSOAL("Pessoal");

    companion object {
        fun fromDescricao(descricao: String): NaturezaOcorrencia {
            return entries.find { it.descricao.equals(descricao, ignoreCase = true) }
                ?: PESSOAL
        }
    }
}

data class Ocorrencia(
    val id: String? = null,
    val protocolo: String,
    val natureza: NaturezaOcorrencia,
    val latitude: Double?,
    val longitude: Double?,
    val dataHora: Instant,
    val historico: String?,
    val fotos: List<String> = emptyList(),
    val rua: String? = null,
    val numero: String? = null,
    val bairro: String? = null,
    val cidade: String? = null,
    val uf: String? = null,
    val veiculos: List<VeiculoEnvolvido> = emptyList(),
    val vitimas: List<Vitima> = emptyList(),
    val orgaosApoio: List<OrgaoApoio> = emptyList(),
    val apoiosDetalhados: List<ApoioOcorrencia> = emptyList(),
    val viaturas: List<Viatura> = emptyList() // V3 Viaturas linked to the occurrence
)

data class ApoioOcorrencia(
    val id: String = "",
    val ocorrenciaId: String = "",
    val orgaoId: String = "",
    val orgaoSigla: String = "",               // PRF, PM, SAMU, GCM, CETESB, DER, DERSA, OUTROS
    val orgaoNome: String = "",
    val viatura: String = "",
    val encarregado: String = "",
    val descricaoOutros: String = "",          // Para opção "OUTROS"
    
    // Kept for database compatibility in repository mapping
    val orgao: OrgaoApoio = OrgaoApoio(orgaoId, orgaoNome, orgaoSigla)
)
