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
            fotos = json.encodeToString(ocorrencia.fotos),
            status = ocorrencia.status
        )
        val existing = ocorrenciaDao.getOcorrenciaById(id)
        if (existing != null) {
            ocorrenciaDao.updateOcorrencia(roomOcorrencia)
        } else {
            ocorrenciaDao.insertOcorrencia(roomOcorrencia)
        }

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
        // Maps related
        val veiculosList = roomVeiculos.map { rv ->
            val ocrMap = try { json.decodeFromString<Map<String, String>>(rv.ocrDadosEstruturados) } catch (e: Exception) { emptyMap() }
            VeiculoEnvolvido(
                id = rv.id,
                ocorrenciaId = id,
                veiculoMasterId = rv.veiculoMasterId,
                condutorId = rv.condutorId,
                placa = rv.placa ?: "",
                cor = rv.cor ?: "",
                chassi = rv.chassi ?: "",
                modelo = rv.modelo ?: "",
                ano = if (rv.anoFabricacao != null && rv.anoModelo != null) "${rv.anoFabricacao}/${rv.anoModelo}" else rv.ano?.toString() ?: "",
                renavam = rv.renavam,
                monobloco = rv.monobloco,
                especie = rv.especie,
                tipoVeiculo = rv.tipoVeiculo,
                carroceria = rv.carroceria,
                marca = rv.marca ?: "",
                versao = rv.versao ?: "",
                anoFabricacao = rv.anoFabricacao,
                anoModelo = rv.anoModelo,
                categoriaVeiculo = rv.categoriaVeiculo,
                exercicio = rv.exercicio ?: "",
                urlCrlv = rv.urlCrlv,
                ocrTextoCrlv = rv.ocrTextoCrlv,
                ocrDadosEstruturados = ocrMap,
                dadosMotorista = Motorista(rv.condutorNome, rv.condutorCnh, rv.condutorCategoriaCnh, rv.condutorDataNascimento, rv.condutorTelefone)
            )
        }

        val vitimasList = roomVitimas.map { rv ->
            val parsedNomeMedico = if (rv.observacoesMedicas?.startsWith("Médico: ") == true) {
                rv.observacoesMedicas.substringAfter("Médico: ").substringBefore(" | CRM: ")
            } else {
                ""
            }
            val parsedCrmMedico = if (rv.observacoesMedicas?.contains(" | CRM: ") == true) {
                rv.observacoesMedicas.substringAfter(" | CRM: ")
            } else {
                ""
            }
            // Deserialize structured lesions
            val lesoesEstruturadas = rv.lesoesJson
                ?.split("|")
                ?.mapNotNull { entry ->
                    val parts = entry.split(":")
                    if (parts.size == 2) {
                        runCatching {
                            com.example.firenotes.domain.model.Lesao(
                                regiao = com.example.firenotes.domain.model.RegiaoCorporal.valueOf(parts[0]),
                                tipo = com.example.firenotes.domain.model.TipoFerimento.valueOf(parts[1])
                            )
                        }.getOrNull()
                    } else null
                } ?: emptyList()
            val parsedCpf = rv.pessoaId?.let { ocorrenciaDao.getPessoaById(it)?.cpf }
            Vitima(
                id = rv.id,
                ocorrenciaId = id,
                nome = rv.nome ?: "",
                idade = rv.idade,
                pessoaId = rv.pessoaId,
                lesoes = rv.lesoesAparentes ?: "",
                lesoesEstruturadas = lesoesEstruturadas,
                destinoSocorro = rv.destinoSocorro ?: "",
                quemSocorreu = rv.quemSocorreu ?: "",
                resultadoOcorrencia = rv.resultadoOcorrencia ?: "",
                viaturaSocorroId = rv.viaturaSocorroId,
                hospitalDestino = rv.hospitalDestino ?: "",
                nomeMedico = parsedNomeMedico,
                crmMedico = parsedCrmMedico,
                sinaisVitais = SinaisVitais(
                    pulso = rv.pulso,
                    pressaoArterial = rv.pressaoArterial ?: "",
                    saturacaoO2 = rv.saturacaoO2,
                    escalaGCS = rv.escalaGCS,
                    aberturaOcular = rv.gcsAberturaOcular,
                    respostaVerbal = rv.gcsRespostaVerbal,
                    respostaMotora = rv.gcsRespostaMotora,
                    respiracao = rv.respiracao,
                    temperatura = rv.temperatura,
                    observacoesMedicas = rv.observacoesMedicas
                ),
                cpf = parsedCpf,
                lesoesAparentes = rv.lesoesAparentes,
                transportadoPor = rv.transportadoPor
            )
        }

        val viaturasList = roomViaturas.map { rv ->
            val roomMilitares = ocorrenciaDao.getMilitaresForViatura(rv.id)
            val equipeList = roomMilitares.map { rm ->
                Militar(
                    id = rm.id,
                    viaturaId = rv.id,
                    militarMasterId = rm.militarMasterId,
                    re = rm.re,
                    nomeGuerra = rm.nomeGuerra,
                    graduacao = rm.graduacao,
                    funcao = rm.funcao ?: ""
                )
            }
            Viatura(
                id = rv.id,
                ocorrenciaId = id,
                viaturaMasterId = rv.viaturaMasterId,
                prefixo = rv.prefixo,
                tipo = rv.tipo,
                unidade = rv.unidade ?: "",
                kmSaida = rv.kmSaida,
                kmLocal = rv.kmLocal,
                kmRetorno = rv.kmRetorno,
                horaDespacho = rv.horaDespacho,
                horaSaida = rv.horaSaida,
                horaChegada = rv.horaChegada,
                horaRetorno = rv.horaRetorno,
                observacoes = rv.observacoes ?: "",
                equipe = equipeList
            )
        }

        val apoiosList = roomApoios.mapNotNull { ra ->
            val orgao = ocorrenciaDao.getOrgaoApoioById(ra.orgaoId)
            orgao?.let {
                val rawV = ra.viatura ?: ""
                val (pDesc, pViat) = if (rawV.startsWith("OUTROS:")) {
                    rawV.substringAfter("OUTROS:").substringBefore(" - ") to rawV.substringAfter(" - ")
                } else {
                    "" to rawV
                }
                ApoioOcorrencia(
                    id = ra.id,
                    ocorrenciaId = id,
                    orgaoId = ra.orgaoId,
                    orgaoSigla = it.sigla,
                    orgaoNome = it.nome,
                    viatura = pViat,
                    encarregado = ra.encarregado ?: "",
                    descricaoOutros = pDesc
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
            apoiosDetalhados = apoiosList,
            status = o.status
        )
    }

    override fun getOcorrencias(): Flow<List<Ocorrencia>> {
        return ocorrenciaDao.getOcorrenciasFlow().map { list ->
            list.map { o ->
                val id = o.id
                val addr = ocorrenciaDao.getEnderecoForOcorrencia(id)
                val parsedFotos = try { json.decodeFromString<List<String>>(o.fotos) } catch(e: Exception) { emptyList() }
                
                val roomVeiculos = ocorrenciaDao.getVeiculosForOcorrencia(id)
                val roomVitimas = ocorrenciaDao.getVitimasForOcorrencia(id)
                val roomViaturas = ocorrenciaDao.getViaturasForOcorrencia(id)
                val roomApoios = ocorrenciaDao.getApoioForOcorrencia(id)
                val veiculosList = roomVeiculos.map { rv ->
                    val ocrMap = try { json.decodeFromString<Map<String, String>>(rv.ocrDadosEstruturados) } catch (e: Exception) { emptyMap() }
                    VeiculoEnvolvido(
                        id = rv.id,
                        ocorrenciaId = id,
                        veiculoMasterId = rv.veiculoMasterId,
                        condutorId = rv.condutorId,
                        placa = rv.placa ?: "",
                        cor = rv.cor ?: "",
                        chassi = rv.chassi ?: "",
                        modelo = rv.modelo ?: "",
                        ano = if (rv.anoFabricacao != null && rv.anoModelo != null) "${rv.anoFabricacao}/${rv.anoModelo}" else rv.ano?.toString() ?: "",
                        renavam = rv.renavam,
                        monobloco = rv.monobloco,
                        especie = rv.especie,
                        tipoVeiculo = rv.tipoVeiculo,
                        carroceria = rv.carroceria,
                        marca = rv.marca ?: "",
                        versao = rv.versao ?: "",
                        anoFabricacao = rv.anoFabricacao,
                        anoModelo = rv.anoModelo,
                        categoriaVeiculo = rv.categoriaVeiculo,
                        exercicio = rv.exercicio ?: "",
                        urlCrlv = rv.urlCrlv,
                        ocrTextoCrlv = rv.ocrTextoCrlv,
                        ocrDadosEstruturados = ocrMap,
                        dadosMotorista = Motorista(rv.condutorNome, rv.condutorCnh, rv.condutorCategoriaCnh, rv.condutorDataNascimento, rv.condutorTelefone)
                    )
                }

                val vitimasList = roomVitimas.map { rv ->
                    val parsedCpf = rv.pessoaId?.let { ocorrenciaDao.getPessoaById(it)?.cpf }
                    val parsedNomeMedico = if (rv.observacoesMedicas?.startsWith("Médico: ") == true) {
                        rv.observacoesMedicas.substringAfter("Médico: ").substringBefore(" | CRM: ")
                    } else {
                        ""
                    }
                    val parsedCrmMedico = if (rv.observacoesMedicas?.contains(" | CRM: ") == true) {
                        rv.observacoesMedicas.substringAfter(" | CRM: ")
                    } else {
                        ""
                    }
                    Vitima(
                        id = rv.id,
                        ocorrenciaId = id,
                        nome = rv.nome ?: "",
                        idade = rv.idade,
                        pessoaId = rv.pessoaId,
                        lesoes = rv.lesoesAparentes ?: "",
                        destinoSocorro = rv.destinoSocorro ?: "",
                        quemSocorreu = rv.quemSocorreu ?: "",
                        resultadoOcorrencia = rv.resultadoOcorrencia ?: "",
                        viaturaSocorroId = rv.viaturaSocorroId,
                        hospitalDestino = rv.hospitalDestino ?: "",
                        nomeMedico = parsedNomeMedico,
                        crmMedico = parsedCrmMedico,
                        sinaisVitais = SinaisVitais(
                            pulso = rv.pulso,
                            pressaoArterial = rv.pressaoArterial ?: "",
                            saturacaoO2 = rv.saturacaoO2,
                            escalaGCS = rv.escalaGCS,
                            temperatura = rv.temperatura,
                            observacoesMedicas = rv.observacoesMedicas
                        ),
                        cpf = parsedCpf,
                        lesoesAparentes = rv.lesoesAparentes,
                        transportadoPor = rv.transportadoPor
                    )
                }

                val viaturasList = roomViaturas.map { rv ->
                    val roomMilitares = ocorrenciaDao.getMilitaresForViatura(rv.id)
                    val equipeList = roomMilitares.map { rm ->
                        Militar(
                            id = rm.id,
                            viaturaId = rv.id,
                            militarMasterId = rm.militarMasterId,
                            re = rm.re,
                            nomeGuerra = rm.nomeGuerra,
                            graduacao = rm.graduacao,
                            funcao = rm.funcao ?: ""
                        )
                    }
                    Viatura(
                        id = rv.id,
                        ocorrenciaId = id,
                        viaturaMasterId = rv.viaturaMasterId,
                        prefixo = rv.prefixo,
                        tipo = rv.tipo,
                        unidade = rv.unidade ?: "",
                        kmSaida = rv.kmSaida,
                        kmLocal = rv.kmLocal,
                        kmRetorno = rv.kmRetorno,
                        horaDespacho = rv.horaDespacho,
                        horaSaida = rv.horaSaida,
                        horaChegada = rv.horaChegada,
                        horaRetorno = rv.horaRetorno,
                        observacoes = rv.observacoes ?: "",
                        equipe = equipeList
                    )
                }

                val apoiosList = roomApoios.mapNotNull { ra ->
                    val orgao = ocorrenciaDao.getOrgaoApoioById(ra.orgaoId)
                    orgao?.let {
                        val rawV = ra.viatura ?: ""
                        val (pDesc, pViat) = if (rawV.startsWith("OUTROS:")) {
                            rawV.substringAfter("OUTROS:").substringBefore(" - ") to rawV.substringAfter(" - ")
                        } else {
                            "" to rawV
                        }
                        ApoioOcorrencia(
                            id = ra.id,
                            ocorrenciaId = id,
                            orgaoId = ra.orgaoId,
                            orgaoSigla = it.sigla,
                            orgaoNome = it.nome,
                            viatura = pViat,
                            encarregado = ra.encarregado ?: "",
                            descricaoOutros = pDesc
                        )
                    }
                }

                Ocorrencia(
                    id = id,
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
                    apoiosDetalhados = apoiosList,
                    status = o.status
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
            ano = veiculo.ano.substringBefore("/").toIntOrNull(),
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
        // Serialize structured lesions to JSON
        val lesoesJsonStr = if (vitima.lesoesEstruturadas.isNotEmpty()) {
            vitima.lesoesEstruturadas.joinToString("|") { "${it.regiao.name}:${it.tipo.name}" }
        } else null
        val roomVitima = RoomVitima(
            id = id,
            ocorrenciaId = vitima.ocorrenciaId,
            nome = vitima.nome,
            idade = vitima.idade,
            lesoesAparentes = vitima.lesoesAparentes ?: vitima.lesoes.ifBlank { null },
            lesoesJson = lesoesJsonStr,
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
            gcsAberturaOcular = vitima.sinaisVitais.aberturaOcular,
            gcsRespostaVerbal = vitima.sinaisVitais.respostaVerbal,
            gcsRespostaMotora = vitima.sinaisVitais.respostaMotora,
            respiracao = vitima.sinaisVitais.respiracao,
            observacoesMedicas = vitima.sinaisVitais.observacoesMedicas
        )
        ocorrenciaDao.insertVitima(roomVitima)
        vitima.copy(id = id)
    }

    override suspend fun getOrgaosApoio(): Result<List<OrgaoApoio>> = runCatching {
        ocorrenciaDao.getOrgaosApoio().map { OrgaoApoio(it.id, it.nome, it.sigla) }
    }

    override suspend fun addOrgaoApoio(orgao: OrgaoApoio): Result<OrgaoApoio> = runCatching {
        ocorrenciaDao.insertOrgaoApoio(RoomOrgaoApoio(orgao.id, orgao.nome, orgao.sigla))
        orgao
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
        val cleanCpf = pessoa.cpf?.trim()?.takeIf { it.isNotBlank() }
        val cleanRg = pessoa.rg?.trim()?.takeIf { it.isNotBlank() }
        val cleanNomeSocial = pessoa.nomeSocial?.trim()?.takeIf { it.isNotBlank() }

        android.util.Log.d("FireRoom", "Inserindo/Atualizando Pessoa no Room: Nome=${pessoa.nome}, CPF=$cleanCpf, RG=$cleanRg")
        val roomPessoa = RoomPessoa(
            id = id,
            nome = pessoa.nome,
            nomeSocial = cleanNomeSocial,
            cpf = cleanCpf,
            rg = cleanRg,
            rgOrgaoEmissor = pessoa.rgOrgaoEmissor,
            rgUf = pessoa.rgUf,
            nascimento = pessoa.nascimento,
            naturalidade = pessoa.naturalidade,
            nacionalidade = pessoa.nacionalidade,
            filiacao = pessoa.filiacao,
            sexo = pessoa.sexo,
            telefone = pessoa.telefone,
            email = pessoa.email,
            logradouro = pessoa.logradouro,
            numero = pessoa.numero,
            bairro = pessoa.bairro,
            cidade = pessoa.cidade,
            uf = pessoa.uf,
            cep = pessoa.cep
        )
        ocorrenciaDao.insertPessoa(roomPessoa)
        pessoa.copy(id = id, cpf = cleanCpf, rg = cleanRg, nomeSocial = cleanNomeSocial)
    }

    override suspend fun addDocumento(documento: Documento): Result<Documento> = runCatching {
        val id = documento.id ?: UUID.randomUUID().toString()
        android.util.Log.d("FireRoom", "Inserindo/Atualizando Documento no Room: Tipo=${documento.tipo}, Numero=${documento.numero}")
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

    override suspend fun salvarPessoaEDocumento(pessoa: Pessoa, documento: Documento): Result<String> = runCatching {
        val finalPessoaId = pessoa.id ?: UUID.randomUUID().toString()
        val cleanCpf = if (pessoa.cpf.isNullOrBlank()) null else pessoa.cpf.trim()
        val cleanRg = if (pessoa.rg.isNullOrBlank()) null else pessoa.rg.replace(Regex("[.-]"), "")
        val cleanNomeSocial = if (pessoa.nomeSocial.isNullOrBlank()) null else pessoa.nomeSocial

        val roomPessoa = RoomPessoa(
            id = finalPessoaId,
            nome = pessoa.nome,
            nomeSocial = cleanNomeSocial,
            cpf = cleanCpf,
            rg = cleanRg,
            rgOrgaoEmissor = pessoa.rgOrgaoEmissor,
            rgUf = pessoa.rgUf,
            nascimento = pessoa.nascimento,
            naturalidade = pessoa.naturalidade,
            nacionalidade = pessoa.nacionalidade,
            filiacao = pessoa.filiacao,
            sexo = pessoa.sexo,
            telefone = pessoa.telefone,
            email = pessoa.email,
            logradouro = pessoa.logradouro,
            numero = pessoa.numero,
            bairro = pessoa.bairro,
            cidade = pessoa.cidade,
            uf = pessoa.uf,
            cep = pessoa.cep
        )

        val docId = documento.id ?: UUID.randomUUID().toString()
        val structStr = json.encodeToString(documento.dadosEstruturados)
        val roomDoc = RoomDocumento(
            id = docId,
            ocorrenciaId = documento.ocorrenciaId,
            pessoaId = finalPessoaId,
            tipo = documento.tipo,
            numero = documento.numero,
            urlImagem = documento.urlImagem,
            textoOcr = documento.textoOcr,
            dadosEstruturados = structStr,
            hashArquivo = documento.hashArquivo,
            dataUpload = documento.dataUpload,
            usuario = documento.usuario
        )

        ocorrenciaDao.salvarPessoaEDocumentoComCpf(roomPessoa, roomDoc)
    }

    override suspend fun getPessoasDaOcorrencia(ocorrenciaId: String): Result<List<Pessoa>> = runCatching {
        android.util.Log.d("FireRoom", "Carregando pessoas vinculadas à ocorrência: $ocorrenciaId")
        val personIds = mutableSetOf<String>()

        ocorrenciaDao.getDocumentosForOcorrencia(ocorrenciaId).forEach { doc ->
            doc.pessoaId?.let { personIds.add(it) }
        }

        ocorrenciaDao.getVeiculosForOcorrencia(ocorrenciaId).forEach { vec ->
            vec.condutorId?.let { personIds.add(it) }
        }

        ocorrenciaDao.getVitimasForOcorrencia(ocorrenciaId).forEach { vit ->
            vit.pessoaId?.let { personIds.add(it) }
        }

        personIds.mapNotNull { id ->
            ocorrenciaDao.getPessoaById(id)?.let { rp ->
                Pessoa(
                    id = rp.id, nome = rp.nome, nomeSocial = rp.nomeSocial, cpf = rp.cpf, rg = rp.rg,
                    rgOrgaoEmissor = rp.rgOrgaoEmissor, rgUf = rp.rgUf, nascimento = rp.nascimento,
                    naturalidade = rp.naturalidade, nacionalidade = rp.nacionalidade, filiacao = rp.filiacao,
                    sexo = rp.sexo, telefone = rp.telefone, email = rp.email, logradouro = rp.logradouro,
                    numero = rp.numero, bairro = rp.bairro, cidade = rp.cidade, uf = rp.uf, cep = rp.cep
                )
            }
        }
    }

    override suspend fun getDocumentosDaOcorrencia(ocorrenciaId: String): Result<List<Documento>> = runCatching {
        android.util.Log.d("FireRoom", "Carregando documentos vinculados à ocorrência: $ocorrenciaId")
        ocorrenciaDao.getDocumentosForOcorrencia(ocorrenciaId).map { rd ->
            val structMap = try { json.decodeFromString<Map<String, String>>(rd.dadosEstruturados) } catch(e: Exception) { emptyMap() }
            Documento(rd.id, rd.ocorrenciaId, rd.pessoaId, rd.tipo, rd.numero, rd.urlImagem, rd.textoOcr, structMap, rd.hashArquivo, rd.dataUpload, rd.usuario)
        }
    }

    override suspend fun vincularOrgaoApoioDetalhado(
        ocorrenciaId: String,
        orgaoId: String,
        viatura: String?,
        encarregado: String?,
        descricaoOutros: String?
    ): Result<Unit> = runCatching {
        val id = UUID.randomUUID().toString()
        val viaturaFinal = if (orgaoId == "OUTROS" && !descricaoOutros.isNullOrBlank()) {
            "OUTROS:$descricaoOutros"
        } else {
            viatura
        }
        ocorrenciaDao.insertApoioOcorrencia(RoomApoioOcorrencia(id, ocorrenciaId, orgaoId, viaturaFinal, encarregado))
    }

    // --- V3 Viaturas & Militares ---
    override suspend fun addViatura(viatura: Viatura): Result<Viatura> {
        return try {
            val viaturaId = if (viatura.id.isNullOrBlank()) UUID.randomUUID().toString() else viatura.id
            if (viatura.ocorrenciaId.isBlank() || viatura.ocorrenciaId == "TEMP") {
                return Result.failure(
                    Exception("Ocorrência ID inválido: ${viatura.ocorrenciaId}")
                )
            }
            
            android.util.Log.d("FireRoom", "📦 Salvando viatura: ID=$viaturaId, Ocorrência=${viatura.ocorrenciaId}")
            
            val roomViatura = RoomViaturaOcorrencia(
                id = viaturaId, ocorrenciaId = viatura.ocorrenciaId, viaturaMasterId = viatura.viaturaMasterId,
                prefixo = viatura.prefixo, tipo = viatura.tipo, unidade = viatura.unidade,
                kmSaida = viatura.kmSaida, kmLocal = viatura.kmLocal, kmRetorno = viatura.kmRetorno,
                horaDespacho = viatura.horaDespacho, horaSaida = viatura.horaSaida,
                horaChegada = viatura.horaChegada, horaRetorno = viatura.horaRetorno, observacoes = viatura.observacoes
            )
            ocorrenciaDao.insertViaturaOcorrencia(roomViatura)
            
            val saved = ocorrenciaDao.getViaturaById(viaturaId)
            if (saved == null) {
                return Result.failure(Exception("Falha ao salvar viatura no banco de dados"))
            }
            
            Result.success(viatura.copy(id = viaturaId))
        } catch (e: Exception) {
            android.util.Log.e("FireRoom", "❌ Erro ao salvar viatura: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun salvarViaturaComMilitares(
        viatura: Viatura,
        militares: List<Militar>
    ): Result<Viatura> {
        return try {
            val viaturaId = if (viatura.id.isNullOrBlank()) UUID.randomUUID().toString() else viatura.id
            if (viatura.ocorrenciaId.isBlank() || viatura.ocorrenciaId == "TEMP") {
                return Result.failure(
                    Exception("Ocorrência ID inválido: ${viatura.ocorrenciaId}")
                )
            }
            
            android.util.Log.d("FireRoom", "📦 Salvando viatura com militares: ID=$viaturaId, Ocorrência=${viatura.ocorrenciaId}, Militares=${militares.size}")
            
            val roomViatura = RoomViaturaOcorrencia(
                id = viaturaId, ocorrenciaId = viatura.ocorrenciaId, viaturaMasterId = viatura.viaturaMasterId,
                prefixo = viatura.prefixo, tipo = viatura.tipo, unidade = viatura.unidade,
                kmSaida = viatura.kmSaida, kmLocal = viatura.kmLocal, kmRetorno = viatura.kmRetorno,
                horaDespacho = viatura.horaDespacho, horaSaida = viatura.horaSaida,
                horaChegada = viatura.horaChegada, horaRetorno = viatura.horaRetorno, observacoes = viatura.observacoes
            )
            
            val roomMilitares = militares.map { militar ->
                val militarId = if (militar.id.isNullOrBlank()) UUID.randomUUID().toString() else militar.id
                RoomMilitarViatura(
                    id = militarId, viaturaId = viaturaId, 
                    militarMasterId = if (militar.militarMasterId.isNullOrBlank()) null else militar.militarMasterId,
                    re = militar.re, nomeGuerra = militar.nomeGuerra, graduacao = militar.graduacao,
                    funcao = militar.funcao
                )
            }
            
            ocorrenciaDao.salvarViaturaComMilitares(roomViatura, roomMilitares)
            
            val updatedEquipe = roomMilitares.map { rm ->
                Militar(
                    id = rm.id,
                    viaturaId = viaturaId,
                    militarMasterId = rm.militarMasterId,
                    re = rm.re,
                    nomeGuerra = rm.nomeGuerra,
                    graduacao = rm.graduacao,
                    funcao = rm.funcao ?: ""
                )
            }
            
            Result.success(viatura.copy(id = viaturaId, equipe = updatedEquipe))
        } catch (e: Exception) {
            android.util.Log.e("FireRoom", "❌ Erro ao salvar viatura com militares: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteViatura(viaturaId: String): Result<Unit> = runCatching {
        android.util.Log.d("FireRoom", "Deletando Viatura do Room: ID=$viaturaId")
        val victims = ocorrenciaDao.getVitimasForViatura(viaturaId)
        if (victims.isNotEmpty()) {
            throw IllegalStateException("Não é possível excluir a viatura porque há vítimas vinculadas ao socorro dela.")
        }
        ocorrenciaDao.deleteViaturaOcorrencia(viaturaId)
    }

    override suspend fun addMilitar(militar: Militar): Result<Militar> {
        return try {
            val viatura = ocorrenciaDao.getViaturaById(militar.viaturaId)
            if (viatura == null) {
                return Result.failure(
                    Exception("Viatura com ID ${militar.viaturaId} não encontrada")
                )
            }
            if (viatura.ocorrenciaId.isBlank() || viatura.ocorrenciaId == "TEMP") {
                return Result.failure(
                    Exception("Viatura com ocorrenciaId inválido: ${viatura.ocorrenciaId}")
                )
            }
            
            val militarId = if (militar.id.isNullOrBlank()) UUID.randomUUID().toString() else militar.id
            android.util.Log.d("FireRoom", "📦 Salvando militar: ID=$militarId, Viatura=${militar.viaturaId}")
            
            val roomMilitar = RoomMilitarViatura(
                id = militarId, viaturaId = militar.viaturaId, 
                militarMasterId = if (militar.militarMasterId.isNullOrBlank()) null else militar.militarMasterId,
                re = militar.re, nomeGuerra = militar.nomeGuerra, graduacao = militar.graduacao,
                funcao = militar.funcao
            )
            ocorrenciaDao.insertMilitarViatura(roomMilitar)
            
            val saved = ocorrenciaDao.getMilitarById(militarId)
            if (saved == null) {
                return Result.failure(Exception("Falha ao salvar militar no banco de dados"))
            }
            
            Result.success(militar.copy(id = militarId))
        } catch (e: Exception) {
            android.util.Log.e("FireRoom", "❌ Erro ao salvar militar: ${e.message}", e)
            Result.failure(e)
        }
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
                Militar(
                    id = rm.id,
                    viaturaId = rv.id,
                    militarMasterId = rm.militarMasterId,
                    re = rm.re,
                    nomeGuerra = rm.nomeGuerra,
                    graduacao = rm.graduacao,
                    funcao = rm.funcao ?: ""
                )
            }
            Viatura(
                id = rv.id, ocorrenciaId = ocorrenciaId, viaturaMasterId = rv.viaturaMasterId,
                prefixo = rv.prefixo, tipo = rv.tipo, unidade = rv.unidade ?: "",
                kmSaida = rv.kmSaida, kmLocal = rv.kmLocal, kmRetorno = rv.kmRetorno,
                horaDespacho = rv.horaDespacho, horaSaida = rv.horaSaida,
                horaChegada = rv.horaChegada, horaRetorno = rv.horaRetorno, observacoes = rv.observacoes ?: "",
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

    override suspend fun deleteDocumento(id: String): Result<Unit> = runCatching {
        ocorrenciaDao.deleteDocumento(id)
    }

    override suspend fun deleteVeiculo(id: String): Result<Unit> = runCatching {
        ocorrenciaDao.deleteVeiculoOcorrencia(id)
    }

    override suspend fun deleteEvidencia(id: String): Result<Unit> = runCatching {
        ocorrenciaDao.deleteEvidencia(id)
    }

    override suspend fun deleteOcorrencia(id: String): Result<Unit> = runCatching {
        ocorrenciaDao.deleteOcorrencia(id)
    }
}
