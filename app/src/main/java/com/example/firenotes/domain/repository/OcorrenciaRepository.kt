package com.example.firenotes.domain.repository

import com.example.firenotes.domain.model.*
import kotlinx.coroutines.flow.Flow

interface OcorrenciaRepository {
    suspend fun createOcorrencia(ocorrencia: Ocorrencia): Result<Ocorrencia>
    suspend fun getOcorrenciaById(id: String): Result<Ocorrencia>
    fun getOcorrencias(): Flow<List<Ocorrencia>>
    
    // Add vehicles/victims to an existing occurrence
    suspend fun addVeiculoEnvolvido(veiculo: VeiculoEnvolvido): Result<VeiculoEnvolvido>
    suspend fun addVitima(vitima: Vitima): Result<Vitima>
    
    // Support agencies operations
    suspend fun getOrgaosApoio(): Result<List<OrgaoApoio>>
    suspend fun vincularOrgaoApoio(ocorrenciaId: String, orgaoId: String): Result<Unit>
    suspend fun desvincularOrgaoApoio(ocorrenciaId: String, orgaoId: String): Result<Unit>

    // New DTO-based operations for Supabase
    suspend fun fetchOcorrencias(): Result<List<com.example.firenotes.data.model.OcorrenciaDto>>
    suspend fun insertOcorrenciaCompleta(
        ocorrencia: com.example.firenotes.data.model.OcorrenciaDto,
        veiculos: List<com.example.firenotes.data.model.VeiculoDto>,
        vitimas: List<com.example.firenotes.data.model.VitimaDto>,
        orgaosApoioIds: List<String>
    ): Result<Unit>

    // V2 Enhanced Operations
    suspend fun upsertPessoa(pessoa: Pessoa): Result<Pessoa>
    suspend fun addDocumento(documento: Documento): Result<Documento>
    suspend fun getPessoasDaOcorrencia(ocorrenciaId: String): Result<List<Pessoa>>
    suspend fun getDocumentosDaOcorrencia(ocorrenciaId: String): Result<List<Documento>>
    suspend fun uploadFile(bucket: String, path: String, bytes: ByteArray): Result<String>
    suspend fun vincularOrgaoApoioDetalhado(ocorrenciaId: String, orgaoId: String, viatura: String?, encarregado: String?): Result<Unit>

    // V3 Viaturas & Militares Operations
    suspend fun addViatura(viatura: Viatura): Result<Viatura>
    suspend fun deleteViatura(viaturaId: String): Result<Unit>
    suspend fun addMilitar(militar: Militar): Result<Militar>
    suspend fun deleteMilitar(militarId: String): Result<Unit>
    suspend fun moveMilitar(militarId: String, newViaturaId: String): Result<Unit>
    suspend fun getViaturasDaOcorrencia(ocorrenciaId: String): Result<List<Viatura>>

    // V4 Architectural Master Catalog operations
    suspend fun addViaturaMaster(viatura: ViaturaMaster): Result<ViaturaMaster>
    suspend fun getViaturasMaster(): Result<List<ViaturaMaster>>
    suspend fun addMilitarMaster(militar: MilitarMaster): Result<MilitarMaster>
    suspend fun getMilitaresMaster(): Result<List<MilitarMaster>>
    suspend fun addVeiculoMaster(veiculo: VeiculoMaster): Result<VeiculoMaster>
    suspend fun getVeiculosMaster(): Result<List<VeiculoMaster>>

    // V4 Sincronização, Auditoria & Evidências
    suspend fun logAudit(log: AuditLog): Result<Unit>
    suspend fun addTimelineEvent(event: TimelineEvent): Result<TimelineEvent>
    suspend fun getTimelineEvents(ocorrenciaId: String): Result<List<TimelineEvent>>
    suspend fun addEvidencia(evidencia: Evidencia): Result<Evidencia>
    suspend fun getEvidencias(ocorrenciaId: String): Result<List<Evidencia>>
    suspend fun deleteDocumento(id: String): Result<Unit>
    suspend fun deleteVeiculo(id: String): Result<Unit>
    suspend fun deleteEvidencia(id: String): Result<Unit>
}
