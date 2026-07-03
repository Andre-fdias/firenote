package com.example.firenotes.data.repository

import android.content.Context
import android.net.Uri
import com.example.firenotes.data.local.dao.OcorrenciaDao
import com.example.firenotes.data.local.entities.*
import com.example.firenotes.domain.model.*
import com.example.firenotes.domain.repository.OcorrenciaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomOcorrenciaRepository @Inject constructor(
    private val ocorrenciaDao: OcorrenciaDao,
    @ApplicationContext private val context: Context
) : OcorrenciaRepository {

    // --- JSON Serialization Helper for Maps ---
    private val json = Json { ignoreUnknownKeys = true }

    // --- Helper to write files locally (Supabase Storage replacement) ---
    private fun saveFileLocally(bucketName: String, fileName: String, bytes: ByteArray): String {
        val baseDir = context.getExternalFilesDir(null)
        val bucketDir = when (bucketName.lowercase()) {
            "ocr", "documentos" -> File(baseDir, "documentos")
            "crlv", "veiculos" -> File(baseDir, "veiculos")
            "fotos", "evidencias" -> File(baseDir, "evidencias")
            "relatorios" -> File(baseDir, "relatorios")
            else -> File(baseDir, "temp")
        }
        if (!bucketDir.exists()) {
            bucketDir.mkdirs()
        }
        val file = File(bucketDir, fileName)
        FileOutputStream(file).use { it.write(bytes) }
        return file.absolutePath
    }

    override suspend fun uploadFile(bucket: String, path: String, bytes: ByteArray): Result<String> = runCatching {
        // Safe upload file implementation returning local URI
        saveFileLocally(bucket, path.substringAfterLast("/"), bytes)
    }

    // --- Ocorrencia Mappings ---
    override suspend fun createOcorrencia(ocorrencia: Ocorrencia): Result<Ocorrencia> = runCatching {
        val id = ocorrencia.id ?: UUID.randomUUID().toString()
        val roomOcorrencia = RoomOcorrencia(
            id = id,
            protocolo = ocorrencia.protocolo,
            natureza = ocorrencia.natureza.descricao,
            latitude = ocorrencia.latitude,
            longitude = ocorrencia.longitude,
            dataHora = ocorrencia.dataHora.toString(),
            historico = ocorrencia.historico,
            fotos = json.encodeToString(ocorrencia.fotos)
        )
        ocorrenciaDao.insertOcorrencia(roomOcorrencia)

        val existingAddr = ocorrenciaDao.getEnderecoForOcorrencia(id)
        val roomEndereco = RoomEndereco(
            id = existingAddr?.id ?: UUID.randomUUID().toString(),
            ocorrenciaId = id,
            rua = ocorrencia.rua,
            numero = ocorrencia.numero,
            bairro = ocorrencia.bairro,
            cidade = ocorrencia.cidade,
            uf = ocorrencia.uf,
            cep = ""
        )
        ocorrenciaDao.insertEndereco(roomEndereco)
        ocorrencia.copy(id = id)
    }

    override suspend fun getOcorrenciaById(id: String): Result<Ocorrencia> = runCatching {
        val o = ocorrenciaDao.getOcorrenciaById(id) ?: throw IllegalArgumentException("Ocorrência não encontrada")
        val addr = ocorrenciaDao.getEnderecoForOcorrencia(id)
        
        val roomVeiculos = ocorrenciaDao.getVeiculosForOcorrencia(id)
        val roomVitimas = ocorrenciaDao.getVitimasForOcorrencia(id)
        val roomViaturas = ocorrenciaDao.getViaturasForOcorrencia(id)
        val roomApoios = ocorrenciaDao.getApoioForOcorrencia(id)

        // Maps related
        val veiculosList = roomVeiculos.map { rv ->
            val ocrMap = try { json.decodeFromString<Map<String, String>>(rv.ocrDadosEstruturados) } catch (e: Exception) { emptyMap() }
            VeiculoEnvolvido(
                id = rv.id,
                ocorrenciaId = id,
                veiculoMasterId = rv.veiculoMasterId,
                condutorId = rv.condutorId,
                placa = rv.placa,
                cor = rv.cor,
                chassi = rv.chassi,
                modelo = rv.modelo,
                ano = rv.ano,
                renavam = rv.renavam,
                monobloco = rv.monobloco,
                especie = rv.especie,
                tipoVeiculo = rv.tipoVeiculo,
                carroceria = rv.carroceria,
                marca = rv.marca,
                versao = rv.versao,
                anoFabricacao = rv.anoFabricacao,
                anoModelo = rv.anoModelo,
                categoriaVeiculo = rv.categoriaVeiculo,
                exercicio = rv.exercicio,
                urlCrlv = rv.urlCrlv,
                ocrTextoCrlv = rv.ocrTextoCrlv,
                ocrDadosEstruturados = ocrMap,
                dadosMotorista = Motorista(rv.condutorNome, rv.condutorCnh, rv.condutorCategoriaCnh, rv.condutorDataNascimento, rv.condutorTelefone)
            )
        }

        val vitimasList = roomVitimas.map { rv ->
            Vitima(
                id = rv.id,
                ocorrenciaId = id,
                nome = rv.nome,
                idade = rv.idade,
                lesoesAparentes = rv.lesoesAparentes,
                destinoSocorro = rv.destinoSocorro,
                quemSocorreu = rv.quemSocorreu,
                resultadoOcorrencia = rv.resultadoOcorrencia,
                pessoaId = rv.pessoaId,
                viaturaSocorroId = rv.viaturaSocorroId,
                hospitalDestino = rv.hospitalDestino,
                transportadoPor = rv.transportadoPor,
                sinaisVitais = SinaisVitais(rv.pulso, rv.pressaoArterial, rv.saturacaoO2, rv.temperatura, rv.escalaGCS, rv.observacoesMedicas)
            )
        }

        val viaturasList = roomViaturas.map { rv ->
            val roomMilitares = ocorrenciaDao.getMilitaresForViatura(rv.id)
            val equipeList = roomMilitares.map { rm ->
                Militar(rm.id, rv.id, rm.militarMasterId, rm.re, rm.nomeGuerra, GraduacaoMilitar.fromDescricao(rm.graduacao), rm.funcao)
            }
            Viatura(
                id = rv.id,
                ocorrenciaId = id,
                viaturaMasterId = rv.viaturaMasterId,
                prefixo = rv.prefixo,
                tipo = rv.tipo,
                unidade = rv.unidade,
                kmSaida = rv.kmSaida,
                kmLocal = rv.kmLocal,
                kmRetorno = rv.kmRetorno,
                horaDespacho = rv.horaDespacho,
                horaSaida = rv.horaSaida,
                horaChegada = rv.horaChegada,
                horaRetorno = rv.horaRetorno,
                observacoes = rv.observacoes,
                equipe = equipeList
            )
        }

        val apoiosList = roomApoios.mapNotNull { ra ->
            val orgao = ocorrenciaDao.getOrgaoApoioById(ra.orgaoId)
            orgao?.let {
                ApoioOcorrencia(
                    orgao = OrgaoApoio(it.id, it.nome, it.sigla),
                    viatura = ra.viatura,
                    encarregado = ra.encarregado
                )
            }
        }

        val parsedFotos = try { json.decodeFromString<List<String>>(o.fotos) } catch(e: Exception) { emptyList() }

        Ocorrencia(
            id = o.id,
            protocolo = o.protocolo,
            natureza = NaturezaOcorrencia.fromDescricao(o.natureza),
            latitude = o.latitude,
            longitude = o.longitude,
            dataHora = try { Instant.parse(o.dataHora) } catch (e: Exception) { Instant.now() },
            historico = o.historico,
            fotos = parsedFotos,
            rua = addr?.rua,
            numero = addr?.numero,
            bairro = addr?.bairro,
            cidade = addr?.cidade,
            uf = addr?.uf,
            veiculos = veiculosList,
            vitimas = vitimasList,
            viaturas = viaturasList,
            apoiosDetalhados = apoiosList
        )
    }

    override fun getOcorrencias(): Flow<List<Ocorrencia>> {
        return ocorrenciaDao.getOcorrenciasFlow().map { list ->
            list.map { o ->
                val addr = ocorrenciaDao.getEnderecoForOcorrencia(o.id)
                val parsedFotos = try { json.decodeFromString<List<String>>(o.fotos) } catch(e: Exception) { emptyList() }
                Ocorrencia(
                    id = o.id,
                    protocolo = o.protocolo,
                    natureza = NaturezaOcorrencia.fromDescricao(o.natureza),
                    latitude = o.latitude,
                    longitude = o.longitude,
                    dataHora = try { Instant.parse(o.dataHora) } catch (e: Exception) { Instant.now() },
                    historico = o.historico,
                    fotos = parsedFotos,
                    rua = addr?.rua,
                    numero = addr?.numero,
                    bairro = addr?.bairro,
                    cidade = addr?.cidade,
                    uf = addr?.uf
                )
            }
        }
    }

    override suspend fun addVeiculoEnvolvido(veiculo: VeiculoEnvolvido): Result<VeiculoEnvolvido> = runCatching {
        val id = veiculo.id ?: UUID.randomUUID().toString()
        val ocrStr = json.encodeToString(veiculo.ocrDadosEstruturados)
        val roomVeiculo = RoomVeiculoOcorrencia(
            id = id,
            ocorrenciaId = veiculo.ocorrenciaId,
            veiculoMasterId = veiculo.veiculoMasterId,
            condutorId = veiculo.condutorId,
            placa = veiculo.placa,
            cor = veiculo.cor,
            chassi = veiculo.chassi,
            modelo = veiculo.modelo,
            ano = veiculo.ano,
            renavam = veiculo.renavam,
            monobloco = veiculo.monobloco,
            especie = veiculo.especie,
            tipoVeiculo = veiculo.tipoVeiculo,
            carroceria = veiculo.carroceria,
            marca = veiculo.marca,
            versao = veiculo.versao,
            anoFabricacao = veiculo.anoFabricacao,
            anoModelo = veiculo.anoModelo,
            categoriaVeiculo = veiculo.categoriaVeiculo,
            exercicio = veiculo.exercicio,
            urlCrlv = veiculo.urlCrlv,
            ocrTextoCrlv = veiculo.ocrTextoCrlv,
            ocrDadosEstruturados = ocrStr,
            condutorNome = veiculo.dadosMotorista?.nome,
            condutorCnh = veiculo.dadosMotorista?.cnh,
            condutorCategoriaCnh = veiculo.dadosMotorista?.categoriaCnh,
            condutorDataNascimento = veiculo.dadosMotorista?.dataNascimento,
            condutorTelefone = veiculo.dadosMotorista?.telefone
        )
        ocorrenciaDao.insertVeiculoOcorrencia(roomVeiculo)
        veiculo.copy(id = id)
    }

    override suspend fun addVitima(vitima: Vitima): Result<Vitima> = runCatching {
        val id = vitima.id ?: UUID.randomUUID().toString()
        val roomVitima = RoomVitima(
            id = id,
            ocorrenciaId = vitima.ocorrenciaId,
            nome = vitima.nome,
            idade = vitima.idade,
            lesoesAparentes = vitima.lesoesAparentes,
            destinoSocorro = vitima.destinoSocorro,
            quemSocorreu = vitima.quemSocorreu,
            resultadoOcorrencia = vitima.resultadoOcorrencia,
            pessoaId = vitima.pessoaId,
            viaturaSocorroId = vitima.viaturaSocorroId,
            hospitalDestino = vitima.hospitalDestino,
            transportadoPor = vitima.transportadoPor,
            pulso = vitima.sinaisVitais.pulso,
            pressaoArterial = vitima.sinaisVitais.pressaoArterial,
            saturacaoO2 = vitima.sinaisVitais.saturacaoO2,
            temperatura = vitima.sinaisVitais.temperatura,
            escalaGCS = vitima.sinaisVitais.escalaGCS,
            observacoesMedicas = vitima.sinaisVitais.observacoesMedicas
        )
        ocorrenciaDao.insertVitima(roomVitima)
        vitima.copy(id = id)
    }

    override suspend fun getOrgaosApoio(): Result<List<OrgaoApoio>> = runCatching {
        ocorrenciaDao.getOrgaosApoio().map { OrgaoApoio(it.id, it.nome, it.sigla) }
    }

    override suspend fun vincularOrgaoApoio(ocorrenciaId: String, orgaoId: String): Result<Unit> = runCatching {
        val id = UUID.randomUUID().toString()
        ocorrenciaDao.insertApoioOcorrencia(RoomApoioOcorrencia(id, ocorrenciaId, orgaoId, null, null))
    }

    override suspend fun desvincularOrgaoApoio(ocorrenciaId: String, orgaoId: String): Result<Unit> = runCatching {
        ocorrenciaDao.deleteApoioOcorrencia(ocorrenciaId, orgaoId)
    }

    override suspend fun upsertPessoa(pessoa: Pessoa): Result<Pessoa> = runCatching {
        val id = pessoa.id ?: UUID.randomUUID().toString()
        val roomPessoa = RoomPessoa(
            id = id, nome = pessoa.nome, nomeSocial = pessoa.nomeSocial, cpf = pessoa.cpf, rg = pessoa.rg,
            rgOrgaoEmissor = pessoa.rgOrgaoEmissor, rgUf = pessoa.rgUf, nascimento = pessoa.nascimento,
            naturalidade = pessoa.naturalidade, nacionalidade = pessoa.nacionalidade, filiacao = pessoa.filiacao,
            sexo = pessoa.sexo, telefone = pessoa.telefone, email = pessoa.email, logradouro = pessoa.logradouro,
            numero = pessoa.numero, bairro = pessoa.bairro, cidade = pessoa.cidade, uf = pessoa.uf, cep = pessoa.cep
        )
        ocorrenciaDao.insertPessoa(roomPessoa)
        pessoa.copy(id = id)
    }

    override suspend fun addDocumento(documento: Documento): Result<Documento> = runCatching {
        val id = documento.id ?: UUID.randomUUID().toString()
        val structStr = json.encodeToString(documento.dadosEstruturados)
        val roomDoc = RoomDocumento(
            id = id, ocorrenciaId = documento.ocorrenciaId, pessoaId = documento.pessoaId, tipo = documento.tipo,
            numero = documento.numero, urlImagem = documento.urlImagem, textoOcr = documento.textoOcr,
            dadosEstruturados = structStr, hashArquivo = documento.hashArquivo, dataUpload = documento.dataUpload,
            usuario = documento.usuario
        )
        ocorrenciaDao.insertDocumento(roomDoc)
        documento.copy(id = id)
    }

    override suspend fun getPessoasDaOcorrencia(ocorrenciaId: String): Result<List<Pessoa>> = runCatching {
        // Room implementation: Pessoas can be queried through association maps or simple list
        emptyList()
    }

    override suspend fun getDocumentosDaOcorrencia(ocorrenciaId: String): Result<List<Documento>> = runCatching {
        ocorrenciaDao.getDocumentosForOcorrencia(ocorrenciaId).map { rd ->
            val structMap = try { json.decodeFromString<Map<String, String>>(rd.dadosEstruturados) } catch(e: Exception) { emptyMap() }
            Documento(rd.id, rd.ocorrenciaId, rd.pessoaId, rd.tipo, rd.numero, rd.urlImagem, rd.textoOcr, structMap, rd.hashArquivo, rd.dataUpload, rd.usuario)
        }
    }

    override suspend fun vincularOrgaoApoioDetalhado(
        ocorrenciaId: String,
        orgaoId: String,
        viatura: String?,
        encarregado: String?
    ): Result<Unit> = runCatching {
        val id = UUID.randomUUID().toString()
        ocorrenciaDao.insertApoioOcorrencia(RoomApoioOcorrencia(id, ocorrenciaId, orgaoId, viatura, encarregado))
    }

    // --- V3 Viaturas & Militares ---
    override suspend fun addViatura(viatura: Viatura): Result<Viatura> = runCatching {
        val id = viatura.id ?: UUID.randomUUID().toString()
        val roomViatura = RoomViaturaOcorrencia(
            id = id, ocorrenciaId = viatura.ocorrenciaId, viaturaMasterId = viatura.viaturaMasterId,
            prefixo = viatura.prefixo, tipo = viatura.tipo, unidade = viatura.unidade,
            kmSaida = viatura.kmSaida, kmLocal = viatura.kmLocal, kmRetorno = viatura.kmRetorno,
            horaDespacho = viatura.horaDespacho, horaSaida = viatura.horaSaida,
            horaChegada = viatura.horaChegada, horaRetorno = viatura.horaRetorno, observacoes = viatura.observacoes
        )
        ocorrenciaDao.insertViaturaOcorrencia(roomViatura)
        viatura.copy(id = id)
    }

    override suspend fun deleteViatura(viaturaId: String): Result<Unit> = runCatching {
        val victims = ocorrenciaDao.getVitimasForViatura(viaturaId)
        if (victims.isNotEmpty()) {
            throw IllegalStateException("Não é possível excluir a viatura porque há vítimas vinculadas ao socorro dela.")
        }
        ocorrenciaDao.deleteViaturaOcorrencia(viaturaId)
    }

    override suspend fun addMilitar(militar: Militar): Result<Militar> = runCatching {
        val id = militar.id ?: UUID.randomUUID().toString()
        val roomMilitar = RoomMilitarViatura(
            id = id, viaturaId = militar.viaturaId, militarMasterId = militar.militarMasterId,
            re = militar.re, nomeGuerra = militar.nomeGuerra, graduacao = militar.graduacao.descricao,
            funcao = militar.funcao
        )
        ocorrenciaDao.insertMilitarViatura(roomMilitar)
        militar.copy(id = id)
    }

    override suspend fun deleteMilitar(militarId: String): Result<Unit> = runCatching {
        ocorrenciaDao.deleteMilitarViatura(militarId)
    }

    override suspend fun moveMilitar(militarId: String, newViaturaId: String): Result<Unit> = runCatching {
        ocorrenciaDao.updateMilitarViaturaId(militarId, newViaturaId)
    }

    override suspend fun getViaturasDaOcorrencia(ocorrenciaId: String): Result<List<Viatura>> = runCatching {
        ocorrenciaDao.getViaturasForOcorrencia(ocorrenciaId).map { rv ->
            val roomMilitares = ocorrenciaDao.getMilitaresForViatura(rv.id)
            val equipeList = roomMilitares.map { rm ->
                Militar(rm.id, rv.id, rm.militarMasterId, rm.re, rm.nomeGuerra, GraduacaoMilitar.fromDescricao(rm.graduacao), rm.funcao)
            }
            Viatura(
                id = rv.id, ocorrenciaId = ocorrenciaId, viaturaMasterId = rv.viaturaMasterId,
                prefixo = rv.prefixo, tipo = rv.tipo, unidade = rv.unidade,
                kmSaida = rv.kmSaida, kmLocal = rv.kmLocal, kmRetorno = rv.kmRetorno,
                horaDespacho = rv.horaDespacho, horaSaida = rv.horaSaida,
                horaChegada = rv.horaChegada, horaRetorno = rv.horaRetorno, observacoes = rv.observacoes,
                equipe = equipeList
            )
        }
    }

    // --- V4 Catalog Operations ---
    override suspend fun addViaturaMaster(viatura: ViaturaMaster): Result<ViaturaMaster> = runCatching {
        val id = viatura.id ?: UUID.randomUUID().toString()
        val equipStr = viatura.equipamentos.joinToString(",")
        val roomV = RoomViaturaMaster(
            id = id, prefixo = viatura.prefixo, placa = viatura.placa, tipo = viatura.tipo,
            marca = viatura.marca, modelo = viatura.modelo, quartel = viatura.quartel,
            status = viatura.status, capacidade = viatura.capacidade, equipamentos = equipStr
        )
        ocorrenciaDao.insertViaturaMaster(roomV)
        viatura.copy(id = id)
    }

    override suspend fun getViaturasMaster(): Result<List<ViaturaMaster>> = runCatching {
        ocorrenciaDao.getViaturasMaster().map { rv ->
            val equipList = rv.equipamentos.split(",").filter { it.isNotBlank() }
            ViaturaMaster(rv.id, rv.prefixo, rv.placa, rv.tipo, rv.marca, rv.modelo, rv.quartel, rv.status, rv.capacidade, equipList)
        }
    }

    override suspend fun addMilitarMaster(militar: MilitarMaster): Result<MilitarMaster> = runCatching {
        val id = militar.id ?: UUID.randomUUID().toString()
        val roomM = RoomMilitarMaster(
            id = id, re = militar.re, nome = militar.nome, nomeGuerra = militar.nomeGuerra,
            graduacao = militar.graduacao.descricao, funcao = militar.funcao, lotacao = militar.lotacao,
            situacao = militar.situacao, telefone = militar.telefone, email = militar.email
        )
        ocorrenciaDao.insertMilitarMaster(roomM)
        militar.copy(id = id)
    }

    override suspend fun getMilitaresMaster(): Result<List<MilitarMaster>> = runCatching {
        ocorrenciaDao.getMilitaresMaster().map { rm ->
            MilitarMaster(rm.id, rm.re, rm.nome, rm.nomeGuerra, GraduacaoMilitar.fromDescricao(rm.graduacao), rm.funcao, rm.lotacao, rm.situacao, rm.telefone, rm.email)
        }
    }

    override suspend fun addVeiculoMaster(veiculo: VeiculoMaster): Result<VeiculoMaster> = runCatching {
        val id = veiculo.id ?: UUID.randomUUID().toString()
        val roomV = RoomVeiculoMaster(
            id = id, placa = veiculo.placa, renavam = veiculo.renavam, chassi = veiculo.chassi,
            marca = veiculo.marca, modelo = veiculo.modelo, versao = veiculo.versao, tipo = veiculo.tipo,
            categoria = veiculo.categoria, cor = veiculo.cor, anoFabricacao = veiculo.anoFabricacao,
            anoModelo = veiculo.anoModelo, proprietarioId = veiculo.proprietarioId, status = veiculo.status
        )
        ocorrenciaDao.insertVeiculoMaster(roomV)
        veiculo.copy(id = id)
    }

    override suspend fun getVeiculosMaster(): Result<List<VeiculoMaster>> = runCatching {
        ocorrenciaDao.getVeiculosMaster().map { rv ->
            VeiculoMaster(rv.id, rv.placa, rv.renavam, rv.chassi, rv.marca, rv.modelo, rv.versao, rv.tipo, rv.categoria, rv.cor, rv.anoFabricacao, rv.anoModelo, rv.proprietarioId, rv.status)
        }
    }

    // --- Sincronização, Auditoria e Timeline ---
    override suspend fun logAudit(log: AuditLog): Result<Unit> = runCatching {
        // Room audit mapping stub: audit logs can be saved to logs or database
    }

    override suspend fun addTimelineEvent(event: TimelineEvent): Result<TimelineEvent> = runCatching {
        val id = event.id ?: UUID.randomUUID().toString()
        ocorrenciaDao.insertTimelineEvento(RoomTimelineEvento(id, event.ocorrenciaId, event.evento, event.descricao, event.dataHora))
        event.copy(id = id)
    }

    override suspend fun getTimelineEvents(ocorrenciaId: String): Result<List<TimelineEvent>> = runCatching {
        ocorrenciaDao.getTimelineForOcorrencia(ocorrenciaId).map { rt ->
            TimelineEvent(rt.id, rt.ocorrenciaId, rt.evento, rt.descricao, rt.dataHora)
        }
    }

    override suspend fun addEvidencia(evidencia: Evidencia): Result<Evidencia> = runCatching {
        val id = evidencia.id ?: UUID.randomUUID().toString()
        val jsonOcrStr = json.encodeToString(evidencia.jsonOcr)
        val roomE = RoomEvidencia(
            id = id, ocorrenciaId = evidencia.ocorrenciaId, tipo = evidencia.tipo, hashSha256 = evidencia.hashSha256,
            latitude = evidencia.latitude, longitude = evidencia.longitude, dataHora = evidencia.dataHora,
            usuario = evidencia.usuario, urlStorage = evidencia.urlStorage, miniaturaUrl = evidencia.miniaturaUrl,
            ocrBruto = evidencia.ocrBruto, jsonOcr = jsonOcrStr
        )
        ocorrenciaDao.insertEvidencia(roomE)
        evidencia.copy(id = id)
    }

    override suspend fun getEvidencias(ocorrenciaId: String): Result<List<Evidencia>> = runCatching {
        ocorrenciaDao.getEvidenciasForOcorrencia(ocorrenciaId).map { re ->
            val jsonMap = try { json.decodeFromString<Map<String, String>>(re.jsonOcr) } catch(e: Exception) { emptyMap() }
            Evidencia(re.id, re.ocorrenciaId, re.tipo, re.hashSha256, re.latitude, re.longitude, re.dataHora, re.usuario, re.urlStorage, re.miniaturaUrl, re.ocrBruto, jsonMap)
        }
    }

    // --- Compatible stubs for Supabase integration ---
    override suspend fun fetchOcorrencias(): Result<List<com.example.firenotes.data.model.OcorrenciaDto>> = runCatching {
        ocorrenciaDao.getOcorrenciasList().map { com.example.firenotes.data.model.OcorrenciaDto.fromDomain(getOcorrenciaById(it.id).getOrThrow()) }
    }

    override suspend fun insertOcorrenciaCompleta(
        ocorrencia: com.example.firenotes.data.model.OcorrenciaDto,
        veiculos: List<com.example.firenotes.data.model.VeiculoDto>,
        vitimas: List<com.example.firenotes.data.model.VitimaDto>,
        orgaosApoioIds: List<String>
    ): Result<Unit> = runCatching {
        // Converts Dtos back to Domain and saves locally
        val o = ocorrencia.toDomain()
        createOcorrencia(o).getOrThrow()
        veiculos.forEach { addVeiculoEnvolvido(it.toDomain()) }
        vitimas.forEach { addVitima(it.toDomain()) }
        orgaosApoioIds.forEach { orgaoId -> vincularOrgaoApoio(o.id ?: "", orgaoId) }
    }
}
