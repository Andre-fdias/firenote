package com.example.firenotes.data.repository

import com.example.firenotes.data.model.*
import com.example.firenotes.domain.model.*
import com.example.firenotes.domain.repository.OcorrenciaRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseOcorrenciaRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) : OcorrenciaRepository {

    override suspend fun createOcorrencia(ocorrencia: Ocorrencia): Result<Ocorrencia> = runCatching {
        val dto = OcorrenciaDto.fromDomain(ocorrencia)
        val response = supabaseClient.postgrest["ocorrencias"]
            .insert(dto) {
                select()
            }
            .decodeSingle<OcorrenciaDto>()
        response.toDomain()
    }

    override suspend fun getOcorrenciaById(id: String): Result<Ocorrencia> = runCatching {
        // Retrieve main occurrence data
        val occurrenceDto = supabaseClient.postgrest["ocorrencias"]
            .select {
                filter {
                    eq("id", id)
                }
            }
            .decodeSingle<OcorrenciaDto>()

        // Retrieve related vehicles (V4 Decoupled catalog)
        val vehiclesDto = supabaseClient.postgrest["veiculos_ocorrencia"]
            .select {
                filter {
                    eq("ocorrencia_id", id)
                }
            }
            .decodeList<VeiculoDto>()

        val vehiclesList = if (vehiclesDto.isNotEmpty()) {
            val masterVehicles = supabaseClient.postgrest["veiculos"]
                .select()
                .decodeList<VeiculoMasterDto>()
                .map { it.toDomain() }

            vehiclesDto.map { vDto ->
                val m = masterVehicles.find { it.placa == vDto.placa }
                vDto.toDomain().copy(
                    placa = m?.placa ?: vDto.placa,
                    cor = m?.cor ?: vDto.cor,
                    chassi = m?.chassi ?: vDto.chassi,
                    modelo = m?.modelo ?: vDto.modelo,
                    ano = m?.anoModelo ?: vDto.ano,
                    renavam = m?.renavam ?: vDto.renavam,
                    proprietarioId = m?.proprietarioId ?: vDto.proprietarioId
                )
            }
        } else {
            emptyList()
        }

        // Retrieve related victims (V4 clinical evaluation catalog)
        val victimsDto = supabaseClient.postgrest["vitimas"]
            .select {
                filter {
                    eq("ocorrencia_id", id)
                }
            }
            .decodeList<VitimaDto>()

        val victimsList = if (victimsDto.isNotEmpty()) {
            val personIds = victimsDto.mapNotNull { it.pessoaId }
            val victimsIds = victimsDto.mapNotNull { it.id }

            val persons = if (personIds.isNotEmpty()) {
                supabaseClient.postgrest["pessoas"]
                    .select {
                        filter {
                            isIn("id", personIds)
                        }
                    }
                    .decodeList<PessoaDto>()
                    .map { it.toDomain() }
            } else {
                emptyList()
            }

            val evals = if (victimsIds.isNotEmpty()) {
                supabaseClient.postgrest["avaliacao_clinica"]
                    .select {
                        filter {
                            isIn("vitima_id", victimsIds)
                        }
                    }
                    .decodeList<AvaliacaoClinicaDto>()
                    .map { it.toDomain() }
            } else {
                emptyList()
            }

            victimsDto.map { vDto ->
                val p = persons.find { it.id == vDto.pessoaId }
                val ev = evals.find { it.vitimaId == vDto.id }
                val birthDateStr = p?.nascimento
                val age = if (birthDateStr != null) {
                    try {
                        val parts = birthDateStr.split("/")
                        val birthDate = if (parts.size == 3) {
                            java.time.LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
                        } else {
                            java.time.LocalDate.parse(birthDateStr)
                        }
                        java.time.Period.between(birthDate, java.time.LocalDate.now()).years
                    } catch (e: Exception) {
                        null
                    }
                } else null

                Vitima(
                    id = vDto.id,
                    ocorrenciaId = id,
                    nome = p?.nome,
                    idade = age,
                    lesoesAparentes = ev?.lesoes,
                    destinoSocorro = ev?.hospitalDestino ?: vDto.destinoSocorro,
                    quemSocorreu = vDto.quemSocorreu,
                    resultadoOcorrencia = ev?.resultado ?: vDto.resultadoOcorrencia,
                    sinaisVitais = SinaisVitais(
                        pulso = ev?.frequenciaCardiaca,
                        pressaoArterial = ev?.pressao,
                        saturacaoO2 = ev?.oximetria,
                        temperatura = ev?.temperatura,
                        escalaGCS = ev?.glasgow
                    ),
                    pessoaId = vDto.pessoaId,
                    viaturaSocorroId = ev?.viaturaSocorroId ?: vDto.viaturaSocorroId,
                    hospitalDestino = ev?.hospitalDestino ?: vDto.hospitalDestino,
                    transportadoPor = vDto.transportadoPor
                )
            }
        } else {
            emptyList()
        }

        // Retrieve support agencies links and details
        val apoiosDto = supabaseClient.postgrest["apoio_ocorrencia"]
            .select {
                filter {
                    eq("ocorrencia_id", id)
                }
            }
            .decodeList<ApoioOcorrenciaDto>()

        val orgaoIds = apoiosDto.map { it.orgaoId }.distinct()
        val orgaosList = if (orgaoIds.isNotEmpty()) {
            supabaseClient.postgrest["orgaos_apoio"]
                .select {
                    filter {
                        isIn("id", orgaoIds)
                    }
                }
                .decodeList<OrgaoApoioDto>()
                .map { it.toDomain() }
        } else {
            emptyList()
        }

        val apoiosDetalhados = apoiosDto.mapNotNull { dto ->
            val orgao = orgaosList.find { it.id == dto.orgaoId }
            orgao?.let {
                ApoioOcorrencia(
                    orgao = it,
                    viatura = dto.viatura,
                    encarregado = dto.encarregado
                )
            }
        }

        // Retrieve related viaturas with their crew (sorted automatically)
        val viaturasResult = getViaturasDaOcorrencia(id).getOrDefault(emptyList())

        occurrenceDto.toDomain(
            veiculos = vehiclesList,
            vitimas = victimsList,
            orgaos = orgaosList,
            apoios = apoiosDetalhados,
            viaturas = viaturasResult
        )
    }

    override fun getOcorrencias(): Flow<List<Ocorrencia>> = flow {
        val occurrences = supabaseClient.postgrest["ocorrencias"]
            .select()
            .decodeList<OcorrenciaDto>()
            .map { it.toDomain() }
        emit(occurrences)
    }

    override suspend fun addVeiculoEnvolvido(veiculo: VeiculoEnvolvido): Result<VeiculoEnvolvido> = runCatching {
        // 1. Check/Insert master vehicle
        val master = VeiculoMaster(
            placa = veiculo.placa ?: "N/D",
            renavam = veiculo.renavam,
            chassi = veiculo.chassi,
            marca = veiculo.marca,
            modelo = veiculo.modelo,
            versao = veiculo.versao,
            tipo = veiculo.tipoVeiculo,
            categoria = veiculo.categoriaVeiculo,
            cor = veiculo.cor,
            anoFabricacao = veiculo.anoFabricacao,
            anoModelo = veiculo.anoModelo,
            proprietarioId = veiculo.proprietarioId
        )
        val masterSaved = addVeiculoMaster(master).getOrThrow()

        // 2. Insert into veiculos_ocorrencia
        val dto = VeiculoDto.fromDomain(veiculo.copy(veiculoMasterId = masterSaved.id))
        val response = supabaseClient.postgrest["veiculos_ocorrencia"]
            .insert(dto) {
                select()
            }
            .decodeSingle<VeiculoDto>()
        response.toDomain()
    }

    override suspend fun addVitima(vitima: Vitima): Result<Vitima> = runCatching {
        val dto = VitimaDto.fromDomain(vitima)
        val response = supabaseClient.postgrest["vitimas"]
            .insert(dto) {
                select()
            }
            .decodeSingle<VitimaDto>()

        // Insert clinical evaluation
        val evalDto = AvaliacaoClinicaDto(
            vitimaId = response.id!!,
            glasgow = vitima.sinaisVitais.escalaGCS,
            pressao = vitima.sinaisVitais.pressaoArterial,
            frequenciaCardiaca = vitima.sinaisVitais.pulso,
            frequenciaRespiratoria = null,
            temperatura = vitima.sinaisVitais.temperatura,
            oximetria = vitima.sinaisVitais.saturacaoO2,
            lesoes = vitima.lesoesAparentes,
            hospitalDestino = vitima.hospitalDestino,
            viaturaSocorroId = vitima.viaturaSocorroId,
            resultado = vitima.resultadoOcorrencia
        )
        supabaseClient.postgrest["avaliacao_clinica"].insert(evalDto)

        response.toDomain()
    }

    override suspend fun getOrgaosApoio(): Result<List<OrgaoApoio>> = runCatching {
        supabaseClient.postgrest["orgaos_apoio"]
            .select()
            .decodeList<OrgaoApoioDto>()
            .map { it.toDomain() }
    }

    override suspend fun vincularOrgaoApoio(ocorrenciaId: String, orgaoId: String): Result<Unit> = runCatching {
        val relationDto = ApoioOcorrenciaDto(ocorrenciaId = ocorrenciaId, orgaoId = orgaoId)
        supabaseClient.postgrest["apoio_ocorrencia"]
            .insert(relationDto)
    }

    override suspend fun desvincularOrgaoApoio(ocorrenciaId: String, orgaoId: String): Result<Unit> = runCatching {
        supabaseClient.postgrest["apoio_ocorrencia"]
            .delete {
                filter {
                    eq("ocorrencia_id", ocorrenciaId)
                    eq("orgao_id", orgaoId)
                }
            }
    }

    // --- New DTO-based operations requested by the user ---

    override suspend fun fetchOcorrencias(): Result<List<OcorrenciaDto>> = runCatching {
        supabaseClient.postgrest["ocorrencias"]
            .select {
                order("data_hora", Order.DESCENDING)
            }
            .decodeList<OcorrenciaDto>()
    }

    override suspend fun insertOcorrenciaCompleta(
        ocorrencia: OcorrenciaDto,
        veiculos: List<VeiculoDto>,
        vitimas: List<VitimaDto>,
        orgaosApoioIds: List<String>
    ): Result<Unit> = runCatching {
        // 1. Insert the main occurrence and retrieve the generated ID
        val insertedOcorrencia = supabaseClient.postgrest["ocorrencias"]
            .insert(ocorrencia) {
                select()
            }
            .decodeSingle<OcorrenciaDto>()
        
        val ocorrenciaId = insertedOcorrencia.id ?: throw IllegalStateException("Failed to retrieve generated occurrence ID.")

        // 2. Insert related vehicles if any
        if (veiculos.isNotEmpty()) {
            val veiculosWithId = veiculos.map { it.copy(ocorrenciaId = ocorrenciaId) }
            supabaseClient.postgrest["veiculos_envolvidos"].insert(veiculosWithId)
        }

        // 3. Insert related victims if any
        if (vitimas.isNotEmpty()) {
            val vitimasWithId = vitimas.map { it.copy(ocorrenciaId = ocorrenciaId) }
            supabaseClient.postgrest["vitimas"].insert(vitimasWithId)
        }

        // 4. Link supporting agencies in the N:N intermediate table
        if (orgaosApoioIds.isNotEmpty()) {
            val apoios = orgaosApoioIds.map { orgaoId ->
                ApoioOcorrenciaDto(ocorrenciaId = ocorrenciaId, orgaoId = orgaoId)
            }
            supabaseClient.postgrest["apoio_ocorrencia"].insert(apoios)
        }
    }

    // --- V2 Enhanced Operations ---

    override suspend fun upsertPessoa(pessoa: Pessoa): Result<Pessoa> = runCatching {
        val cpf = pessoa.cpf
        if (!cpf.isNullOrBlank()) {
            val existing = supabaseClient.postgrest["pessoas"]
                .select {
                    filter {
                        eq("cpf", cpf)
                    }
                }
                .decodeList<PessoaDto>()
            if (existing.isNotEmpty()) {
                val dto = PessoaDto.fromDomain(pessoa).copy(id = existing.first().id)
                val updated = supabaseClient.postgrest["pessoas"]
                    .update(dto) {
                        filter {
                            eq("id", dto.id!!)
                        }
                        select()
                    }
                    .decodeSingle<PessoaDto>()
                return@runCatching updated.toDomain()
            }
        }

        val dto = PessoaDto.fromDomain(pessoa)
        val inserted = supabaseClient.postgrest["pessoas"]
            .insert(dto) {
                select()
            }
            .decodeSingle<PessoaDto>()
        inserted.toDomain()
    }

    override suspend fun addDocumento(documento: Documento): Result<Documento> = runCatching {
        val dto = DocumentoDto.fromDomain(documento)
        val response = supabaseClient.postgrest["documentos"]
            .insert(dto) {
                select()
            }
            .decodeSingle<DocumentoDto>()
        response.toDomain()
    }

    override suspend fun getPessoasDaOcorrencia(ocorrenciaId: String): Result<List<Pessoa>> = runCatching {
        val documents = supabaseClient.postgrest["documentos"]
            .select {
                filter {
                    eq("ocorrencia_id", ocorrenciaId)
                }
            }
            .decodeList<DocumentoDto>()

        val pessoaIds = documents.mapNotNull { it.pessoaId }.distinct()
        if (pessoaIds.isEmpty()) return@runCatching emptyList()

        supabaseClient.postgrest["pessoas"]
            .select {
                filter {
                    isIn("id", pessoaIds)
                }
            }
            .decodeList<PessoaDto>()
            .map { it.toDomain() }
    }

    override suspend fun getDocumentosDaOcorrencia(ocorrenciaId: String): Result<List<Documento>> = runCatching {
        supabaseClient.postgrest["documentos"]
            .select {
                filter {
                    eq("ocorrencia_id", ocorrenciaId)
                }
            }
            .decodeList<DocumentoDto>()
            .map { it.toDomain() }
    }

    override suspend fun uploadFile(bucket: String, path: String, bytes: ByteArray): Result<String> = runCatching {
        val bucketRef = supabaseClient.storage.from(bucket)
        bucketRef.upload(path, bytes) {
            upsert = true
        }
        bucketRef.publicUrl(path)
    }

    override suspend fun vincularOrgaoApoioDetalhado(
        ocorrenciaId: String,
        orgaoId: String,
        viatura: String?,
        encarregado: String?
    ): Result<Unit> = runCatching {
        val relationDto = ApoioOcorrenciaDto(
            ocorrenciaId = ocorrenciaId,
            orgaoId = orgaoId,
            viatura = viatura,
            encarregado = encarregado
        )
        supabaseClient.postgrest["apoio_ocorrencia"].insert(relationDto)
    }

    // --- V3 Viaturas & Militares Operations (Mapped to V4 viaturas_ocorrencia & militares_viatura tables) ---

    override suspend fun addViatura(viatura: Viatura): Result<Viatura> = runCatching {
        val dto = ViaturaDto.fromDomain(viatura)
        val response = supabaseClient.postgrest["viaturas_ocorrencia"]
            .insert(dto) {
                select()
            }
            .decodeSingle<ViaturaDto>()
        response.toDomain()
    }

    override suspend fun deleteViatura(viaturaId: String): Result<Unit> = runCatching {
        val linkedVictims = supabaseClient.postgrest["vitimas"]
            .select {
                filter {
                    eq("viatura_socorro_id", viaturaId)
                }
            }
            .decodeList<VitimaDto>()
        
        if (linkedVictims.isNotEmpty()) {
            throw IllegalStateException("Não é possível excluir a viatura porque há vítimas vinculadas ao socorro dela.")
        }

        supabaseClient.postgrest["viaturas_ocorrencia"].delete {
            filter {
                eq("id", viaturaId)
            }
        }
    }

    override suspend fun addMilitar(militar: Militar): Result<Militar> = runCatching {
        val existing = supabaseClient.postgrest["militares_viatura"]
            .select {
                filter {
                    eq("viatura_id", militar.viaturaId)
                    eq("re", militar.re)
                }
            }
            .decodeList<MilitarDto>()
        
        if (existing.isNotEmpty()) {
            throw IllegalArgumentException("Já existe um militar com este RE cadastrado nesta viatura.")
        }

        val dto = MilitarDto.fromDomain(militar)
        val response = supabaseClient.postgrest["militares_viatura"]
            .insert(dto) {
                select()
            }
            .decodeSingle<MilitarDto>()
        response.toDomain()
    }

    override suspend fun deleteMilitar(militarId: String): Result<Unit> = runCatching {
        supabaseClient.postgrest["militares_viatura"].delete {
            filter {
                eq("id", militarId)
            }
        }
    }

    override suspend fun moveMilitar(militarId: String, newViaturaId: String): Result<Unit> = runCatching {
        val militarDto = supabaseClient.postgrest["militares_viatura"]
            .select {
                filter {
                    eq("id", militarId)
                }
            }
            .decodeSingle<MilitarDto>()
        
        val existing = supabaseClient.postgrest["militares_viatura"]
            .select {
                filter {
                    eq("viatura_id", newViaturaId)
                    eq("re", militarDto.re)
                }
            }
            .decodeList<MilitarDto>()
        
        if (existing.isNotEmpty()) {
            throw IllegalArgumentException("Já existe um militar com este RE cadastrado na viatura de destino.")
        }

        val updatedDto = militarDto.copy(viaturaId = newViaturaId)
        supabaseClient.postgrest["militares_viatura"].update(updatedDto) {
            filter {
                eq("id", militarId)
            }
        }
    }

    override suspend fun getViaturasDaOcorrencia(ocorrenciaId: String): Result<List<Viatura>> = runCatching {
        val viaturasDto = supabaseClient.postgrest["viaturas_ocorrencia"]
            .select {
                filter {
                    eq("ocorrencia_id", ocorrenciaId)
                }
            }
            .decodeList<ViaturaDto>()

        if (viaturasDto.isEmpty()) return@runCatching emptyList()

        val viaturaIds = viaturasDto.mapNotNull { it.id }
        val militaresDto = supabaseClient.postgrest["militares_viatura"]
            .select {
                filter {
                    isIn("viatura_id", viaturaIds)
                }
            }
            .decodeList<MilitarDto>()

        val militares = militaresDto.map { it.toDomain() }

        viaturasDto.map { vDto ->
            val equipe = militares
                .filter { it.viaturaId == vDto.id }
                .sortedByDescending { it.graduacao.hierarquia }
            vDto.toDomain(equipe)
        }
    }

    // --- V4 Architectural Master Catalog operations ---

    override suspend fun addViaturaMaster(viatura: ViaturaMaster): Result<ViaturaMaster> = runCatching {
        val dto = ViaturaMasterDto.fromDomain(viatura)
        val response = supabaseClient.postgrest["viaturas"]
            .insert(dto) {
                select()
            }
            .decodeSingle<ViaturaMasterDto>()
        response.toDomain()
    }

    override suspend fun getViaturasMaster(): Result<List<ViaturaMaster>> = runCatching {
        supabaseClient.postgrest["viaturas"]
            .select()
            .decodeList<ViaturaMasterDto>()
            .map { it.toDomain() }
    }

    override suspend fun addMilitarMaster(militar: MilitarMaster): Result<MilitarMaster> = runCatching {
        val dto = MilitarMasterDto.fromDomain(militar)
        val response = supabaseClient.postgrest["militares"]
            .insert(dto) {
                select()
            }
            .decodeSingle<MilitarMasterDto>()
        response.toDomain()
    }

    override suspend fun getMilitaresMaster(): Result<List<MilitarMaster>> = runCatching {
        supabaseClient.postgrest["militares"]
            .select()
            .decodeList<MilitarMasterDto>()
            .map { it.toDomain() }
    }

    override suspend fun addVeiculoMaster(veiculo: VeiculoMaster): Result<VeiculoMaster> = runCatching {
        val dto = VeiculoMasterDto.fromDomain(veiculo)
        val response = supabaseClient.postgrest["veiculos"]
            .insert(dto) {
                select()
            }
            .decodeSingle<VeiculoMasterDto>()
        response.toDomain()
    }

    override suspend fun getVeiculosMaster(): Result<List<VeiculoMaster>> = runCatching {
        supabaseClient.postgrest["veiculos"]
            .select()
            .decodeList<VeiculoMasterDto>()
            .map { it.toDomain() }
    }

    // --- V4 Sincronização, Auditoria & Evidências ---

    override suspend fun logAudit(log: AuditLog): Result<Unit> = runCatching {
        val dto = AuditLogDto.fromDomain(log)
        supabaseClient.postgrest["audit_log"].insert(dto)
    }

    override suspend fun addTimelineEvent(event: TimelineEvent): Result<TimelineEvent> = runCatching {
        val dto = TimelineEventDto.fromDomain(event)
        val response = supabaseClient.postgrest["timeline_ocorrencia"]
            .insert(dto) {
                select()
            }
            .decodeSingle<TimelineEventDto>()
        response.toDomain()
    }

    override suspend fun getTimelineEvents(ocorrenciaId: String): Result<List<TimelineEvent>> = runCatching {
        supabaseClient.postgrest["timeline_ocorrencia"]
            .select {
                filter {
                    eq("ocorrencia_id", ocorrenciaId)
                }
            }
            .decodeList<TimelineEventDto>()
            .map { it.toDomain() }
    }

    override suspend fun addEvidencia(evidencia: Evidencia): Result<Evidencia> = runCatching {
        val dto = EvidenciaDto.fromDomain(evidencia)
        val response = supabaseClient.postgrest["evidencias"]
            .insert(dto) {
                select()
            }
            .decodeSingle<EvidenciaDto>()
        response.toDomain()
    }

    override suspend fun getEvidencias(ocorrenciaId: String): Result<List<Evidencia>> = runCatching {
        supabaseClient.postgrest["evidencias"]
            .select {
                filter {
                    eq("ocorrencia_id", ocorrenciaId)
                }
            }
            .decodeList<EvidenciaDto>()
            .map { it.toDomain() }
    }
}
