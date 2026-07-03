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

// V4 Master Viatura Entity
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

// V4 Master Militar Entity
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

// Transactional Viatura-Ocorrencia Link Entity (Kept as Viatura for compatibility)
data class Viatura(
    val id: String? = null,
    val ocorrenciaId: String,
    val viaturaMasterId: String? = null, // Link to master viatura catalog
    val prefixo: String,
    val tipo: String,
    val unidade: String? = null, // Quartel/Unidade
    val kmSaida: Int?,
    val kmLocal: Int?, // Chegada ao local
    val kmRetorno: Int? = null,
    val horaDespacho: String? = null,
    val horaSaida: String? = null,
    val horaChegada: String? = null,
    val horaRetorno: String? = null,
    val observacoes: String? = null,
    val equipe: List<Militar> = emptyList()
)

// Transactional Militar-Viatura Link Entity (Kept as Militar for compatibility)
data class Militar(
    val id: String? = null,
    val viaturaId: String, // viatura_ocorrencia_id in DB
    val militarMasterId: String? = null, // Link to master militar catalog
    val re: String,
    val nomeGuerra: String,
    val graduacao: GraduacaoMilitar,
    val funcao: String? = null
)
