package com.example.firenotes.data.local.dao

import androidx.room.*
import com.example.firenotes.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OcorrenciaDao {

    // --- Ocorrencia CRUDS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOcorrencia(ocorrencia: RoomOcorrencia)

    @Update
    suspend fun updateOcorrencia(ocorrencia: RoomOcorrencia)

    @Query("SELECT * FROM ocorrencias WHERE id = :id")
    suspend fun getOcorrenciaById(id: String): RoomOcorrencia?

    @Query("SELECT * FROM ocorrencias ORDER BY dataHora DESC")
    fun getOcorrenciasFlow(): Flow<List<RoomOcorrencia>>

    @Query("SELECT * FROM ocorrencias ORDER BY dataHora DESC")
    suspend fun getOcorrenciasList(): List<RoomOcorrencia>

    @Query("DELETE FROM ocorrencias WHERE id = :id")
    suspend fun deleteOcorrencia(id: String)

    // --- Enderecos ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEndereco(endereco: RoomEndereco)

    @Query("SELECT * FROM enderecos WHERE ocorrenciaId = :ocorrenciaId")
    suspend fun getEnderecoForOcorrencia(ocorrenciaId: String): RoomEndereco?

    // --- Pessoas ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPessoa(pessoa: RoomPessoa)

    @Query("SELECT * FROM pessoas WHERE id = :id")
    suspend fun getPessoaById(id: String): RoomPessoa?

    @Query("SELECT * FROM pessoas WHERE cpf = :cpf")
    suspend fun getPessoaByCpf(cpf: String): RoomPessoa?

    // --- Documentos ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocumento(documento: RoomDocumento)

    @Query("SELECT * FROM documentos WHERE ocorrenciaId = :ocorrenciaId")
    suspend fun getDocumentosForOcorrencia(ocorrenciaId: String): List<RoomDocumento>

    @Query("DELETE FROM documentos WHERE id = :id")
    suspend fun deleteDocumento(id: String)

    // --- Veiculos Master ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVeiculoMaster(veiculo: RoomVeiculoMaster)

    @Query("SELECT * FROM veiculos_master")
    suspend fun getVeiculosMaster(): List<RoomVeiculoMaster>

    @Query("SELECT * FROM veiculos_master WHERE placa = :placa")
    suspend fun getVeiculoMasterByPlaca(placa: String): RoomVeiculoMaster?

    // --- Veiculos Ocorrencia ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVeiculoOcorrencia(veiculo: RoomVeiculoOcorrencia)

    @Query("SELECT * FROM veiculos_ocorrencia WHERE ocorrenciaId = :ocorrenciaId")
    suspend fun getVeiculosForOcorrencia(ocorrenciaId: String): List<RoomVeiculoOcorrencia>

    @Query("DELETE FROM veiculos_ocorrencia WHERE id = :id")
    suspend fun deleteVeiculoOcorrencia(id: String)

    // --- Viaturas Master ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertViaturaMaster(viatura: RoomViaturaMaster)

    @Query("SELECT * FROM viaturas_master")
    suspend fun getViaturasMaster(): List<RoomViaturaMaster>

    // --- Militares Master ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMilitarMaster(militar: RoomMilitarMaster)

    @Query("SELECT * FROM militares_master")
    suspend fun getMilitaresMaster(): List<RoomMilitarMaster>

    // --- Viaturas Ocorrencia ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertViaturaOcorrencia(viatura: RoomViaturaOcorrencia): Long

    @Query("SELECT * FROM viaturas_ocorrencia WHERE ocorrenciaId = :ocorrenciaId")
    suspend fun getViaturasForOcorrencia(ocorrenciaId: String): List<RoomViaturaOcorrencia>

    @Query("DELETE FROM viaturas_ocorrencia WHERE id = :viaturaId")
    suspend fun deleteViaturaOcorrencia(viaturaId: String)

    @Query("SELECT * FROM viaturas_ocorrencia WHERE id = :viaturaId")
    suspend fun getViaturaById(viaturaId: String): RoomViaturaOcorrencia?

    @Query("SELECT EXISTS(SELECT 1 FROM viaturas_ocorrencia WHERE id = :viaturaId)")
    suspend fun viaturaExists(viaturaId: String): Boolean

    @Query("SELECT * FROM militares_viatura WHERE id = :militarId")
    suspend fun getMilitarById(militarId: String): RoomMilitarViatura?

    @Transaction
    suspend fun salvarViaturaComMilitares(viatura: RoomViaturaOcorrencia, militares: List<RoomMilitarViatura>) {
        insertViaturaOcorrencia(viatura)
        militares.forEach { insertMilitarViatura(it) }
    }

    @Transaction
    suspend fun salvarPessoaEDocumentoComCpf(pessoa: RoomPessoa, documento: RoomDocumento): String {
        val existingPessoa = if (!pessoa.cpf.isNullOrBlank()) getPessoaByCpf(pessoa.cpf) else null
        val finalPessoaId = existingPessoa?.id ?: pessoa.id
        val finalPessoa = if (existingPessoa != null) {
            pessoa.copy(
                id = finalPessoaId,
                telefone = if (pessoa.telefone.isNullOrBlank()) existingPessoa.telefone else pessoa.telefone,
                email = if (pessoa.email.isNullOrBlank()) existingPessoa.email else pessoa.email,
                logradouro = if (pessoa.logradouro.isNullOrBlank()) existingPessoa.logradouro else pessoa.logradouro,
                numero = if (pessoa.numero.isNullOrBlank()) existingPessoa.numero else pessoa.numero,
                bairro = if (pessoa.bairro.isNullOrBlank()) existingPessoa.bairro else pessoa.bairro,
                cidade = if (pessoa.cidade.isNullOrBlank()) existingPessoa.cidade else pessoa.cidade,
                uf = if (pessoa.uf.isNullOrBlank()) existingPessoa.uf else pessoa.uf,
                cep = if (pessoa.cep.isNullOrBlank()) existingPessoa.cep else pessoa.cep
            )
        } else {
            pessoa
        }
        insertPessoa(finalPessoa)
        insertDocumento(documento.copy(pessoaId = finalPessoaId))
        return finalPessoaId
    }

    // --- Militares Viatura ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMilitarViatura(militar: RoomMilitarViatura)

    @Query("SELECT * FROM militares_viatura WHERE viaturaId = :viaturaId")
    suspend fun getMilitaresForViatura(viaturaId: String): List<RoomMilitarViatura>

    @Query("DELETE FROM militares_viatura WHERE id = :militarId")
    suspend fun deleteMilitarViatura(militarId: String)

    @Query("UPDATE militares_viatura SET viaturaId = :newViaturaId WHERE id = :militarId")
    suspend fun updateMilitarViaturaId(militarId: String, newViaturaId: String)

    // --- Vitimas ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVitima(vitima: RoomVitima)

    @Query("SELECT * FROM vitimas WHERE ocorrenciaId = :ocorrenciaId")
    suspend fun getVitimasForOcorrencia(ocorrenciaId: String): List<RoomVitima>

    @Query("SELECT * FROM vitimas WHERE viaturaSocorroId = :viaturaId")
    suspend fun getVitimasForViatura(viaturaId: String): List<RoomVitima>

    // --- Orgaos Apoio ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrgaoApoio(orgao: RoomOrgaoApoio)

    @Query("SELECT * FROM orgaos_apoio")
    suspend fun getOrgaosApoio(): List<RoomOrgaoApoio>

    @Query("SELECT * FROM orgaos_apoio WHERE id = :id")
    suspend fun getOrgaoApoioById(id: String): RoomOrgaoApoio?

    // --- Apoio Ocorrencia ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApoioOcorrencia(apoio: RoomApoioOcorrencia)

    @Query("SELECT * FROM apoio_ocorrencia WHERE ocorrenciaId = :ocorrenciaId")
    suspend fun getApoioForOcorrencia(ocorrenciaId: String): List<RoomApoioOcorrencia>

    @Query("DELETE FROM apoio_ocorrencia WHERE ocorrenciaId = :ocorrenciaId AND orgaoId = :orgaoId")
    suspend fun deleteApoioOcorrencia(ocorrenciaId: String, orgaoId: String)

    // --- Evidencias ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvidencia(evidencia: RoomEvidencia)

    @Query("SELECT * FROM evidencias WHERE ocorrenciaId = :ocorrenciaId")
    suspend fun getEvidenciasForOcorrencia(ocorrenciaId: String): List<RoomEvidencia>

    @Query("DELETE FROM evidencias WHERE id = :id")
    suspend fun deleteEvidencia(id: String)

    // --- Timeline ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimelineEvento(evento: RoomTimelineEvento)

    @Query("SELECT * FROM timeline_eventos WHERE ocorrenciaId = :ocorrenciaId ORDER BY dataHora ASC")
    suspend fun getTimelineForOcorrencia(ocorrenciaId: String): List<RoomTimelineEvento>

    // --- Configuracoes ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfiguracao(config: RoomConfiguracao)

    @Query("SELECT * FROM configuracoes WHERE id = 'global_config'")
    suspend fun getConfiguracao(): RoomConfiguracao?

    @Query("SELECT * FROM configuracoes WHERE id = 'global_config'")
    fun getConfiguracaoFlow(): Flow<RoomConfiguracao?>

    // --- BackupLog ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackupLog(log: RoomBackupLog)

    @Query("SELECT * FROM backup_log ORDER BY dataHora DESC")
    suspend fun getBackupLogs(): List<RoomBackupLog>
}
