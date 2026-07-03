package com.example.firenotes.data.model

import com.example.firenotes.domain.model.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class OcorrenciaDto(
    @SerialName("id") val id: String? = null,
    @SerialName("protocolo") val protocolo: String,
    @SerialName("natureza") val natureza: String,
    @SerialName("latitude") val latitude: Double?,
    @SerialName("longitude") val longitude: Double?,
    @SerialName("data_hora") val dataHora: String, // ISO-8601 TIMESTAMPTZ representation
    @SerialName("historico") val historico: String?,
    @SerialName("fotos") val fotos: List<String> = emptyList(), // JSONB field mapped to list of image URLs/paths
    
    // V2 Address fields
    @SerialName("rua") val rua: String? = null,
    @SerialName("numero") val numero: String? = null,
    @SerialName("bairro") val bairro: String? = null,
    @SerialName("cidade") val cidade: String? = null,
    @SerialName("uf") val uf: String? = null,
    
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    fun toDomain(
        veiculos: List<VeiculoEnvolvido> = emptyList(),
        vitimas: List<Vitima> = emptyList(),
        orgaos: List<OrgaoApoio> = emptyList(),
        apoios: List<ApoioOcorrencia> = emptyList(),
        viaturas: List<Viatura> = emptyList()
    ): Ocorrencia {
        return Ocorrencia(
            id = id,
            protocolo = protocolo,
            natureza = NaturezaOcorrencia.fromDescricao(natureza),
            latitude = latitude,
            longitude = longitude,
            dataHora = try { Instant.parse(dataHora) } catch (e: Exception) { Instant.now() },
            historico = historico,
            fotos = fotos,
            rua = rua,
            numero = numero,
            bairro = bairro,
            cidade = cidade,
            uf = uf,
            veiculos = veiculos,
            vitimas = vitimas,
            orgaosApoio = orgaos,
            apoiosDetalhados = apoios,
            viaturas = viaturas
        )
    }

    companion object {
        fun fromDomain(domain: Ocorrencia): OcorrenciaDto {
            return OcorrenciaDto(
                id = domain.id,
                protocolo = domain.protocolo,
                natureza = domain.natureza.descricao,
                latitude = domain.latitude,
                longitude = domain.longitude,
                dataHora = domain.dataHora.toString(),
                historico = domain.historico,
                fotos = domain.fotos,
                rua = domain.rua,
                numero = domain.numero,
                bairro = domain.bairro,
                cidade = domain.cidade,
                uf = domain.uf
            )
        }
    }
}

@Serializable
data class MotoristaDto(
    @SerialName("nome") val nome: String? = null,
    @SerialName("cnh") val cnh: String? = null,
    @SerialName("categoria_cnh") val categoriaCnh: String? = null,
    @SerialName("data_nascimento") val dataNascimento: String? = null,
    @SerialName("telefone") val telefone: String? = null
) {
    fun toDomain() = Motorista(nome, cnh, categoriaCnh, dataNascimento, telefone)
    companion object {
        fun fromDomain(domain: Motorista?) = domain?.let {
            MotoristaDto(it.nome, it.cnh, it.categoriaCnh, it.dataNascimento, it.telefone)
        }
    }
}

@Serializable
data class VeiculoDto(
    @SerialName("id") val id: String? = null,
    @SerialName("ocorrencia_id") val ocorrenciaId: String,
    @SerialName("placa") val placa: String?,
    @SerialName("cor") val cor: String?,
    @SerialName("chassi") val chassi: String?,
    @SerialName("modelo") val modelo: String?,
    @SerialName("ano") val ano: Int?,
    @SerialName("dados_motorista") val dadosMotorista: MotoristaDto? = null,
    
    // V2 CRLV and Owner fields
    @SerialName("proprietario_id") val proprietarioId: String? = null,
    @SerialName("renavam") val renavam: String? = null,
    @SerialName("monobloco") val monobloco: String? = null,
    @SerialName("especie") val especie: String? = null,
    @SerialName("tipo_veiculo") val tipoVeiculo: String? = null,
    @SerialName("carroceria") val carroceria: String? = null,
    @SerialName("marca") val marca: String? = null,
    @SerialName("versao") val versao: String? = null,
    @SerialName("ano_fabricacao") val anoFabricacao: Int? = null,
    @SerialName("ano_modelo") val anoModelo: Int? = null,
    @SerialName("categoria_veiculo") val categoriaVeiculo: String? = null,
    @SerialName("exercicio") val exercicio: String? = null,
    @SerialName("url_crlv") val urlCrlv: String? = null,
    @SerialName("ocr_texto_crlv") val ocrTextoCrlv: String? = null,
    @SerialName("ocr_dados_estruturados") val ocrDadosEstruturados: Map<String, String> = emptyMap()
) {
    fun toDomain() = VeiculoEnvolvido(
        id = id,
        ocorrenciaId = ocorrenciaId,
        placa = placa,
        cor = cor,
        chassi = chassi,
        modelo = modelo,
        ano = ano,
        dadosMotorista = dadosMotorista?.toDomain(),
        proprietarioId = proprietarioId,
        renavam = renavam,
        monobloco = monobloco,
        especie = especie,
        tipoVeiculo = tipoVeiculo,
        carroceria = carroceria,
        marca = marca,
        versao = versao,
        anoFabricacao = anoFabricacao,
        anoModelo = anoModelo,
        categoriaVeiculo = categoriaVeiculo,
        exercicio = exercicio,
        urlCrlv = urlCrlv,
        ocrTextoCrlv = ocrTextoCrlv,
        ocrDadosEstruturados = ocrDadosEstruturados
    )

    companion object {
        fun fromDomain(domain: VeiculoEnvolvido) = VeiculoDto(
            id = domain.id,
            ocorrenciaId = domain.ocorrenciaId,
            placa = domain.placa,
            cor = domain.cor,
            chassi = domain.chassi,
            modelo = domain.modelo,
            ano = domain.ano,
            dadosMotorista = MotoristaDto.fromDomain(domain.dadosMotorista),
            proprietarioId = domain.proprietarioId,
            renavam = domain.renavam,
            monobloco = domain.monobloco,
            especie = domain.especie,
            tipoVeiculo = domain.tipoVeiculo,
            carroceria = domain.carroceria,
            marca = domain.marca,
            versao = domain.versao,
            anoFabricacao = domain.anoFabricacao,
            anoModelo = domain.anoModelo,
            categoriaVeiculo = domain.categoriaVeiculo,
            exercicio = domain.exercicio,
            urlCrlv = domain.urlCrlv,
            ocrTextoCrlv = domain.ocrTextoCrlv,
            ocrDadosEstruturados = domain.ocrDadosEstruturados
        )
    }
}

@Serializable
data class SinaisVitaisDto(
    @SerialName("pulso") val pulso: Int? = null,
    @SerialName("pressao_arterial") val pressaoArterial: String? = null,
    @SerialName("saturacao_o2") val saturacaoO2: Int? = null,
    @SerialName("temperatura") val temperatura: Double? = null,
    @SerialName("escala_gcs") val escalaGCS: Int? = null,
    @SerialName("observacoes_medicas") val observacoesMedicas: String? = null
) {
    fun toDomain() = SinaisVitais(
        pulso = pulso,
        pressaoArterial = pressaoArterial,
        saturacaoO2 = saturacaoO2,
        temperatura = temperatura,
        escalaGCS = escalaGCS,
        observacoesMedicas = observacoesMedicas
    )

    companion object {
        fun fromDomain(domain: SinaisVitais) = SinaisVitaisDto(
            pulso = domain.pulso,
            pressaoArterial = domain.pressaoArterial,
            saturacaoO2 = domain.saturacaoO2,
            temperatura = domain.temperatura,
            escalaGCS = domain.escalaGCS,
            observacoesMedicas = domain.observacoesMedicas
        )
    }
}

@Serializable
data class VitimaDto(
    @SerialName("id") val id: String? = null,
    @SerialName("ocorrencia_id") val ocorrenciaId: String,
    @SerialName("nome") val nome: String?,
    @SerialName("idade") val idade: Int?,
    @SerialName("lesoes_aparentes") val lesoesAparentes: String?,
    @SerialName("destino_socorro") val destinoSocorro: String?,
    @SerialName("quem_socorreu") val quemSocorreu: String?,
    @SerialName("resultado_ocorrencia") val resultadoOcorrencia: String?,
    @SerialName("sinais_vitais") val sinaisVitais: SinaisVitaisDto = SinaisVitaisDto(),
    @SerialName("pessoa_id") val pessoaId: String? = null,
    
    // V3 Fields
    @SerialName("viatura_socorro_id") val viaturaSocorroId: String? = null,
    @SerialName("hospital_destino") val hospitalDestino: String? = null,
    @SerialName("transportado_por") val transportadoPor: String? = null
) {
    fun toDomain() = Vitima(
        id = id,
        ocorrenciaId = ocorrenciaId,
        nome = nome,
        idade = idade,
        lesoesAparentes = lesoesAparentes,
        destinoSocorro = destinoSocorro,
        quemSocorreu = quemSocorreu,
        resultadoOcorrencia = resultadoOcorrencia,
        sinaisVitais = sinaisVitais.toDomain(),
        pessoaId = pessoaId,
        viaturaSocorroId = viaturaSocorroId,
        hospitalDestino = hospitalDestino,
        transportadoPor = transportadoPor
    )

    companion object {
        fun fromDomain(domain: Vitima) = VitimaDto(
            id = domain.id,
            ocorrenciaId = domain.ocorrenciaId,
            nome = domain.nome,
            idade = domain.idade,
            lesoesAparentes = domain.lesoesAparentes,
            destinoSocorro = domain.destinoSocorro,
            quemSocorreu = domain.quemSocorreu,
            resultadoOcorrencia = domain.resultadoOcorrencia,
            sinaisVitais = SinaisVitaisDto.fromDomain(domain.sinaisVitais),
            pessoaId = domain.pessoaId,
            viaturaSocorroId = domain.viaturaSocorroId,
            hospitalDestino = domain.hospitalDestino,
            transportadoPor = domain.transportadoPor
        )
    }
}

@Serializable
data class OrgaoApoioDto(
    @SerialName("id") val id: String,
    @SerialName("nome") val nome: String,
    @SerialName("sigla") val sigla: String
) {
    fun toDomain() = OrgaoApoio(id = id, nome = nome, sigla = sigla)
    companion object {
        fun fromDomain(domain: OrgaoApoio) = OrgaoApoioDto(
            id = domain.id,
            nome = domain.nome,
            sigla = domain.sigla
        )
    }
}

@Serializable
data class ApoioOcorrenciaDto(
    @SerialName("id") val id: String? = null,
    @SerialName("ocorrencia_id") val ocorrenciaId: String,
    @SerialName("orgao_id") val orgaoId: String,
    @SerialName("viatura") val viatura: String? = null,
    @SerialName("encarregado") val encarregado: String? = null
)

@Serializable
data class PessoaDto(
    @SerialName("id") val id: String? = null,
    @SerialName("nome") val nome: String,
    @SerialName("nome_social") val nomeSocial: String? = null,
    @SerialName("cpf") val cpf: String? = null,
    @SerialName("rg") val rg: String? = null,
    @SerialName("rg_orgao_emissor") val rgOrgaoEmissor: String? = null,
    @SerialName("rg_uf") val rgUf: String? = null,
    @SerialName("nascimento") val nascimento: String? = null, // yyyy-MM-dd
    @SerialName("naturalidade") val naturalidade: String? = null,
    @SerialName("nacionalidade") val nacionalidade: String? = null,
    @SerialName("filiacao") val filiacao: String? = null,
    
    // V4 fields
    @SerialName("sexo") val sexo: String? = null,
    @SerialName("telefone") val telefone: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("logradouro") val logradouro: String? = null,
    @SerialName("numero") val numero: String? = null,
    @SerialName("bairro") val bairro: String? = null,
    @SerialName("cidade") val cidade: String? = null,
    @SerialName("uf") val uf: String? = null,
    @SerialName("cep") val cep: String? = null
) {
    fun toDomain() = Pessoa(
        id = id,
        nome = nome,
        nomeSocial = nomeSocial,
        cpf = cpf,
        rg = rg,
        rgOrgaoEmissor = rgOrgaoEmissor,
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
    companion object {
        fun fromDomain(domain: Pessoa) = PessoaDto(
            id = domain.id,
            nome = domain.nome,
            nomeSocial = domain.nomeSocial,
            cpf = domain.cpf,
            rg = domain.rg,
            rgOrgaoEmissor = domain.rgOrgaoEmissor,
            rgUf = domain.rgUf,
            nascimento = domain.nascimento,
            naturalidade = domain.naturalidade,
            nacionalidade = domain.nacionalidade,
            filiacao = domain.filiacao,
            sexo = domain.sexo,
            telefone = domain.telefone,
            email = domain.email,
            logradouro = domain.logradouro,
            numero = domain.numero,
            bairro = domain.bairro,
            cidade = domain.cidade,
            uf = domain.uf,
            cep = domain.cep
        )
    }
}

@Serializable
data class DocumentoDto(
    @SerialName("id") val id: String? = null,
    @SerialName("ocorrencia_id") val ocorrenciaId: String,
    @SerialName("pessoa_id") val pessoaId: String? = null,
    @SerialName("tipo") val tipo: String,
    @SerialName("numero") val numero: String? = null,
    @SerialName("url_imagem") val urlImagem: String? = null,
    @SerialName("texto_ocr") val textoOcr: String? = null,
    @SerialName("dados_estruturados") val dadosEstruturados: Map<String, String> = emptyMap(),
    @SerialName("hash_arquivo") val hashArquivo: String? = null,
    @SerialName("data_upload") val dataUpload: String? = null,
    @SerialName("usuario") val usuario: String? = null
) {
    fun toDomain() = Documento(
        id = id,
        ocorrenciaId = ocorrenciaId,
        pessoaId = pessoaId,
        tipo = tipo,
        numero = numero,
        urlImagem = urlImagem,
        textoOcr = textoOcr,
        dadosEstruturados = dadosEstruturados,
        hashArquivo = hashArquivo,
        dataUpload = dataUpload,
        usuario = usuario
    )
    companion object {
        fun fromDomain(domain: Documento) = DocumentoDto(
            id = domain.id,
            ocorrenciaId = domain.ocorrenciaId,
            pessoaId = domain.pessoaId,
            tipo = domain.tipo,
            numero = domain.numero,
            urlImagem = domain.urlImagem,
            textoOcr = domain.textoOcr,
            dadosEstruturados = domain.dadosEstruturados,
            hashArquivo = domain.hashArquivo,
            dataUpload = domain.dataUpload,
            usuario = domain.usuario
        )
    }
}

@Serializable
data class ViaturaDto(
    @SerialName("id") val id: String? = null,
    @SerialName("ocorrencia_id") val ocorrenciaId: String,
    @SerialName("prefixo") val prefixo: String,
    @SerialName("tipo") val tipo: String,
    @SerialName("unidade") val unidade: String? = null,
    @SerialName("km_saida") val kmSaida: Int?,
    @SerialName("km_local") val kmLocal: Int?,
    @SerialName("observacoes") val observacoes: String? = null
) {
    fun toDomain(equipe: List<Militar> = emptyList()) = Viatura(
        id = id,
        ocorrenciaId = ocorrenciaId,
        prefixo = prefixo,
        tipo = tipo,
        unidade = unidade,
        kmSaida = kmSaida,
        kmLocal = kmLocal,
        observacoes = observacoes,
        equipe = equipe
    )
    companion object {
        fun fromDomain(domain: Viatura) = ViaturaDto(
            id = domain.id,
            ocorrenciaId = domain.ocorrenciaId,
            prefixo = domain.prefixo,
            tipo = domain.tipo,
            unidade = domain.unidade,
            kmSaida = domain.kmSaida,
            kmLocal = domain.kmLocal,
            observacoes = domain.observacoes
        )
    }
}

@Serializable
data class MilitarDto(
    @SerialName("id") val id: String? = null,
    @SerialName("viatura_id") val viaturaId: String,
    @SerialName("re") val re: String,
    @SerialName("nome_guerra") val nomeGuerra: String,
    @SerialName("graduacao") val graduacao: String,
    @SerialName("funcao") val funcao: String? = null
) {
    fun toDomain() = Militar(
        id = id,
        viaturaId = viaturaId,
        re = re,
        nomeGuerra = nomeGuerra,
        graduacao = GraduacaoMilitar.fromDescricao(graduacao),
        funcao = funcao
    )
    companion object {
        fun fromDomain(domain: Militar) = MilitarDto(
            id = domain.id,
            viaturaId = domain.viaturaId,
            re = domain.re,
            nomeGuerra = domain.nomeGuerra,
            graduacao = domain.graduacao.descricao,
            funcao = domain.funcao
        )
    }
}

@Serializable
data class VeiculoMasterDto(
    @SerialName("id") val id: String? = null,
    @SerialName("placa") val placa: String,
    @SerialName("renavam") val renavam: String? = null,
    @SerialName("chassi") val chassi: String? = null,
    @SerialName("marca") val marca: String? = null,
    @SerialName("modelo") val modelo: String? = null,
    @SerialName("versao") val versao: String? = null,
    @SerialName("tipo") val tipo: String? = null,
    @SerialName("categoria") val categoria: String? = null,
    @SerialName("cor") val cor: String? = null,
    @SerialName("ano_fabricacao") val anoFabricacao: Int? = null,
    @SerialName("ano_modelo") val anoModelo: Int? = null,
    @SerialName("proprietario_id") val proprietarioId: String? = null,
    @SerialName("status") val status: String = "Ativo"
) {
    fun toDomain() = VeiculoMaster(
        id = id, placa = placa, renavam = renavam, chassi = chassi, marca = marca, modelo = modelo,
        versao = versao, tipo = tipo, categoria = categoria, cor = cor, anoFabricacao = anoFabricacao,
        anoModelo = anoModelo, proprietarioId = proprietarioId, status = status
    )
    companion object {
        fun fromDomain(domain: VeiculoMaster) = VeiculoMasterDto(
            id = domain.id, placa = domain.placa, renavam = domain.renavam, chassi = domain.chassi,
            marca = domain.marca, modelo = domain.modelo, versao = domain.versao, tipo = domain.tipo,
            categoria = domain.categoria, cor = domain.cor, anoFabricacao = domain.anoFabricacao,
            anoModelo = domain.anoModelo, proprietarioId = domain.proprietarioId, status = domain.status
        )
    }
}

@Serializable
data class ViaturaMasterDto(
    @SerialName("id") val id: String? = null,
    @SerialName("prefixo") val prefixo: String,
    @SerialName("placa") val placa: String? = null,
    @SerialName("tipo") val tipo: String,
    @SerialName("marca") val marca: String? = null,
    @SerialName("modelo") val modelo: String? = null,
    @SerialName("quartel") val quartel: String? = null,
    @SerialName("status") val status: String = "Ativo",
    @SerialName("capacidade") val capacidade: Int? = null,
    @SerialName("equipamentos") val equipamentos: List<String> = emptyList()
) {
    fun toDomain() = ViaturaMaster(
        id = id, prefixo = prefixo, placa = placa, tipo = tipo, marca = marca, modelo = modelo,
        quartel = quartel, status = status, capacidade = capacidade, equipamentos = equipamentos
    )
    companion object {
        fun fromDomain(domain: ViaturaMaster) = ViaturaMasterDto(
            id = domain.id, prefixo = domain.prefixo, placa = domain.placa, tipo = domain.tipo,
            marca = domain.marca, modelo = domain.modelo, quartel = domain.quartel, status = domain.status,
            capacidade = domain.capacidade, equipamentos = domain.equipamentos
        )
    }
}

@Serializable
data class MilitarMasterDto(
    @SerialName("id") val id: String? = null,
    @SerialName("re") val re: String,
    @SerialName("nome") val nome: String,
    @SerialName("nome_guerra") val nomeGuerra: String,
    @SerialName("graduacao") val graduacao: String,
    @SerialName("funcao") val funcao: String? = null,
    @SerialName("lotacao") val lotacao: String? = null,
    @SerialName("situacao") val situacao: String = "Ativo",
    @SerialName("telefone") val telefone: String? = null,
    @SerialName("email") val email: String? = null
) {
    fun toDomain() = MilitarMaster(
        id = id, re = re, nome = nome, nomeGuerra = nomeGuerra,
        graduacao = GraduacaoMilitar.fromDescricao(graduacao), funcao = funcao,
        lotacao = lotacao, situacao = situacao, telefone = telefone, email = email
    )
    companion object {
        fun fromDomain(domain: MilitarMaster) = MilitarMasterDto(
            id = domain.id, re = domain.re, nome = domain.nome, nomeGuerra = domain.nomeGuerra,
            graduacao = domain.graduacao.descricao, funcao = domain.funcao, lotacao = domain.lotacao,
            situacao = domain.situacao, telefone = domain.telefone, email = domain.email
        )
    }
}

@Serializable
data class AvaliacaoClinicaDto(
    @SerialName("id") val id: String? = null,
    @SerialName("vitima_id") val vitimaId: String,
    @SerialName("glasgow") val glasgow: Int?,
    @SerialName("pressao") val pressao: String?,
    @SerialName("frequencia_cardiaca") val frequenciaCardiaca: Int?,
    @SerialName("frequencia_respiratoria") val frequenciaRespiratoria: Int?,
    @SerialName("temperatura") val temperatura: Double?,
    @SerialName("oximetria") val oximetria: Int?,
    @SerialName("lesoes") val lesoes: String?,
    @SerialName("hospital_destino") val hospitalDestino: String?,
    @SerialName("viatura_socorro") val viaturaSocorroId: String?,
    @SerialName("resultado") val resultado: String?
) {
    fun toDomain() = AvaliacaoClinica(
        id = id, vitimaId = vitimaId, glasgow = glasgow, pressao = pressao,
        frequenciaCardiaca = frequenciaCardiaca, frequenciaRespiratoria = frequenciaRespiratoria,
        temperatura = temperatura, oximetria = oximetria, lesoes = lesoes,
        hospitalDestino = hospitalDestino, viaturaSocorroId = viaturaSocorroId, resultado = resultado
    )
    companion object {
        fun fromDomain(domain: AvaliacaoClinica) = AvaliacaoClinicaDto(
            id = domain.id, vitimaId = domain.vitimaId, glasgow = domain.glasgow, pressao = domain.pressao,
            frequenciaCardiaca = domain.frequenciaCardiaca, frequenciaRespiratoria = domain.frequenciaRespiratoria,
            temperatura = domain.temperatura, oximetria = domain.oximetria, lesoes = domain.lesoes,
            hospitalDestino = domain.hospitalDestino, viaturaSocorroId = domain.viaturaSocorroId, resultado = domain.resultado
        )
    }
}

@Serializable
data class EvidenciaDto(
    @SerialName("id") val id: String? = null,
    @SerialName("ocorrencia_id") val ocorrenciaId: String,
    @SerialName("tipo") val tipo: String,
    @SerialName("hash_sha256") val hashSha256: String,
    @SerialName("latitude") val latitude: Double?,
    @SerialName("longitude") val longitude: Double?,
    @SerialName("data_hora") val dataHora: String,
    @SerialName("usuario") val usuario: String?,
    @SerialName("url_storage") val urlStorage: String,
    @SerialName("miniatura_url") val miniaturaUrl: String? = null,
    @SerialName("ocr_bruto") val ocrBruto: String? = null,
    @SerialName("json_ocr") val jsonOcr: Map<String, String> = emptyMap()
) {
    fun toDomain() = Evidencia(
        id = id, ocorrenciaId = ocorrenciaId, tipo = tipo, hashSha256 = hashSha256,
        latitude = latitude, longitude = longitude, dataHora = dataHora, usuario = usuario,
        urlStorage = urlStorage, miniaturaUrl = miniaturaUrl, ocrBruto = ocrBruto, jsonOcr = jsonOcr
    )
    companion object {
        fun fromDomain(domain: Evidencia) = EvidenciaDto(
            id = domain.id, ocorrenciaId = domain.ocorrenciaId, tipo = domain.tipo,
            hashSha256 = domain.hashSha256, latitude = domain.latitude, longitude = domain.longitude,
            dataHora = domain.dataHora, usuario = domain.usuario, urlStorage = domain.urlStorage,
            miniaturaUrl = domain.miniaturaUrl, ocrBruto = domain.ocrBruto, jsonOcr = domain.jsonOcr
        )
    }
}

@Serializable
data class TimelineEventDto(
    @SerialName("id") val id: String? = null,
    @SerialName("ocorrencia_id") val ocorrenciaId: String,
    @SerialName("evento") val evento: String,
    @SerialName("descricao") val descricao: String?,
    @SerialName("data_hora") val dataHora: String
) {
    fun toDomain() = TimelineEvent(
        id = id, ocorrenciaId = ocorrenciaId, evento = evento, descricao = descricao, dataHora = dataHora
    )
    companion object {
        fun fromDomain(domain: TimelineEvent) = TimelineEventDto(
            id = domain.id, ocorrenciaId = domain.ocorrenciaId, evento = domain.evento,
            descricao = domain.descricao, dataHora = domain.dataHora
        )
    }
}

@Serializable
data class AuditLogDto(
    @SerialName("id") val id: String? = null,
    @SerialName("ocorrencia_id") val ocorrenciaId: String?,
    @SerialName("usuario") val usuario: String,
    @SerialName("data_hora") val dataHora: String,
    @SerialName("tabela_alterada") val tabelaAlterada: String,
    @SerialName("campo_alterado") val campoAlterado: String,
    @SerialName("valor_anterior") val valorAnterior: String?,
    @SerialName("valor_novo") val valorNovo: String?
) {
    fun toDomain() = AuditLog(
        id = id, ocorrenciaId = ocorrenciaId, usuario = usuario, dataHora = dataHora,
        tabelaAlterada = tabelaAlterada, campoAlterado = campoAlterado,
        valorAnterior = valorAnterior, valorNovo = valorNovo
    )
    companion object {
        fun fromDomain(domain: AuditLog) = AuditLogDto(
            id = domain.id, ocorrenciaId = domain.ocorrenciaId, usuario = domain.usuario,
            dataHora = domain.dataHora, tabelaAlterada = domain.tabelaAlterada,
            campoAlterado = domain.campoAlterado, valorAnterior = domain.valorAnterior,
            valorNovo = domain.valorNovo
        )
    }
}
