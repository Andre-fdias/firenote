package com.example.firenotes.domain.model

enum class GraduacaoMilitar(val descricao: String, val hierarquia: Int) {
    SD_PM("SD PM", 1),
    CB_PM("CB PM", 2),
    SGT3_PM("3º SGT PM", 3),
    SGT2_PM("2º SGT PM", 4),
    SGT1_PM("1º SGT PM", 5),
    SUBTEN_PM("SUBTEN PM", 6),
    ASP_OF_PM("ASP OF PM", 7),
    TEN2_PM("2º TEN PM", 8),
    TEN1_PM("1º TEN PM", 9),
    CAP_PM("CAP PM", 10),
    MAJ_PM("MAJ PM", 11),
    TEN_CEL_PM("TEN CEL PM", 12),
    CEL_PM("CEL PM", 13);

    companion object {
        fun fromDescricao(desc: String): GraduacaoMilitar {
            return entries.find { it.descricao.equals(desc, ignoreCase = true) }
                ?: SD_PM
        }
    }
}

data class ViaturaMaster(
    val id: String? = null,
    val prefixo: String,
    val placa: String? = null,
    val tipo: String,
    val marca: String? = null,
    val modelo: String? = null,
    val quartel: String? = null,
    val status: String = "Ativo",
    val capacidade: Int? = null,
    val equipamentos: List<String> = emptyList()
)

data class MilitarMaster(
    val id: String? = null,
    val re: String,
    val nome: String,
    val nomeGuerra: String,
    val graduacao: GraduacaoMilitar,
    val funcao: String? = null,
    val lotacao: String? = null,
    val situacao: String = "Ativo",
    val telefone: String? = null,
    val email: String? = null
)

data class Viatura(
    val id: String = "",
    val ocorrenciaId: String = "",
    val prefixo: String = "",                  // Formato: XX-12345 (maiúsculo)
    val unidade: String = "",
    val kmSaida: Int? = null,
    val kmLocal: Int? = null,
    val observacoes: String = "",
    val equipe: List<Militar> = emptyList(),

    // Database compatibility fields
    val viaturaMasterId: String? = null,
    val tipo: String = "",
    val kmRetorno: Int? = null,
    val horaDespacho: String? = null,
    val horaSaida: String? = null,
    val horaChegada: String? = null,
    val horaRetorno: String? = null
)
