package com.example.firenotes.util

import com.example.firenotes.domain.model.*
import com.example.firenotes.domain.repository.OcorrenciaRepository
import kotlinx.serialization.json.*
import java.util.UUID
import java.time.Instant

object JsonImportHelper {
    suspend fun importOccurrenceFromJson(
        jsonStr: String,
        repository: OcorrenciaRepository
    ): Result<Ocorrencia> {
        return runCatching {
            val json = Json.parseToJsonElement(jsonStr)
            val obj = json.jsonObject

            val protocolo = obj["protocolo"]?.jsonPrimitive?.content ?: ""
            val naturezaStr = obj["natureza"]?.jsonPrimitive?.content ?: "INDEFINIDA"
            val natureza = NaturezaOcorrencia.fromDescricao(naturezaStr)
            val latitude = obj["latitude"]?.jsonPrimitive?.doubleOrNull
            val longitude = obj["longitude"]?.jsonPrimitive?.doubleOrNull
            val dataHoraStr = obj["dataHora"]?.jsonPrimitive?.content ?: ""
            val dataHora = Instant.parse(dataHoraStr)
            val historico = obj["historico"]?.jsonPrimitive?.content
            val rua = obj["rua"]?.jsonPrimitive?.content
            val numero = obj["numero"]?.jsonPrimitive?.content
            val bairro = obj["bairro"]?.jsonPrimitive?.content
            val cidade = obj["cidade"]?.jsonPrimitive?.content
            val uf = obj["uf"]?.jsonPrimitive?.content
            val status = obj["status"]?.jsonPrimitive?.content ?: "ABERTA"

            val oId = UUID.randomUUID().toString()

            val fotosList = mutableListOf<String>()
            obj["fotosBase64"]?.jsonArray?.forEach { fotoElement ->
                val fObj = fotoElement.jsonObject
                val nome = fObj["nome"]?.jsonPrimitive?.content ?: "imported_photo_${System.currentTimeMillis()}.jpg"
                val base64 = fObj["bytes"]?.jsonPrimitive?.content ?: ""
                if (base64.isNotEmpty()) {
                    try {
                        val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                        repository.uploadFile("fotos", nome, bytes).onSuccess { localPath ->
                            fotosList.add(localPath)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("JsonImport", "Erro ao decodificar imagem: ${e.message}")
                    }
                }
            }

            val ocorrencia = Ocorrencia(
                id = oId,
                protocolo = protocolo,
                natureza = natureza,
                latitude = latitude,
                longitude = longitude,
                dataHora = dataHora,
                historico = historico,
                fotos = fotosList,
                rua = rua,
                numero = numero,
                bairro = bairro,
                cidade = cidade,
                uf = uf,
                status = status
            )

            repository.createOcorrencia(ocorrencia).getOrThrow()

            // Veículos
            obj["veiculos"]?.jsonArray?.forEach { vElement ->
                val vObj = vElement.jsonObject
                val placa = vObj["placa"]?.jsonPrimitive?.content ?: ""
                val cor = vObj["cor"]?.jsonPrimitive?.content ?: ""
                val chassi = vObj["chassi"]?.jsonPrimitive?.content ?: ""
                val modelo = vObj["modelo"]?.jsonPrimitive?.content ?: ""
                val marca = vObj["marca"]?.jsonPrimitive?.content ?: ""

                repository.addVeiculoEnvolvido(
                    VeiculoEnvolvido(
                        id = UUID.randomUUID().toString(),
                        ocorrenciaId = oId,
                        placa = placa,
                        cor = cor,
                        chassi = chassi,
                        modelo = modelo,
                        marca = marca
                    )
                ).getOrThrow()
            }

            // Vítimas
            obj["vitimas"]?.jsonArray?.forEach { vtElement ->
                val vtObj = vtElement.jsonObject
                val nome = vtObj["nome"]?.jsonPrimitive?.content ?: ""
                val idade = vtObj["idade"]?.jsonPrimitive?.intOrNull
                val destinoSocorro = vtObj["destinoSocorro"]?.jsonPrimitive?.content ?: ""
                val quemSocorreu = vtObj["quemSocorreu"]?.jsonPrimitive?.content ?: ""
                val resultadoOcorrencia = vtObj["resultadoOcorrencia"]?.jsonPrimitive?.content ?: ""
                val hospitalDestino = vtObj["hospitalDestino"]?.jsonPrimitive?.content ?: ""
                val nomeMedico = vtObj["nomeMedico"]?.jsonPrimitive?.content ?: ""
                val crmMedico = vtObj["crmMedico"]?.jsonPrimitive?.content ?: ""
                val cpf = vtObj["cpf"]?.jsonPrimitive?.content

                var pessoaId: String? = null
                if (!cpf.isNullOrBlank()) {
                    val pessoa = Pessoa(
                        id = UUID.randomUUID().toString(),
                        nome = nome,
                        cpf = cpf
                    )
                    repository.upsertPessoa(pessoa).onSuccess { p ->
                        pessoaId = p.id
                    }
                }

                repository.addVitima(
                    Vitima(
                        id = UUID.randomUUID().toString(),
                        ocorrenciaId = oId,
                        nome = nome,
                        idade = idade,
                        destinoSocorro = destinoSocorro,
                        quemSocorreu = quemSocorreu,
                        resultadoOcorrencia = resultadoOcorrencia,
                        hospitalDestino = hospitalDestino,
                        nomeMedico = nomeMedico,
                        crmMedico = crmMedico,
                        pessoaId = pessoaId
                    )
                ).getOrThrow()
            }

            // Viaturas
            obj["viaturas"]?.jsonArray?.forEach { viatElement ->
                val viatObj = viatElement.jsonObject
                val prefixo = viatObj["prefixo"]?.jsonPrimitive?.content ?: ""
                val tipo = viatObj["tipo"]?.jsonPrimitive?.content ?: ""
                val unidade = viatObj["unidade"]?.jsonPrimitive?.content ?: ""
                val kmSaida = viatObj["kmSaida"]?.jsonPrimitive?.intOrNull
                val kmLocal = viatObj["kmLocal"]?.jsonPrimitive?.intOrNull
                val kmRetorno = viatObj["kmRetorno"]?.jsonPrimitive?.intOrNull
                val horaDespacho = viatObj["horaDespacho"]?.jsonPrimitive?.content ?: ""
                val horaSaida = viatObj["horaSaida"]?.jsonPrimitive?.content ?: ""
                val horaChegada = viatObj["horaChegada"]?.jsonPrimitive?.content ?: ""
                val horaRetorno = viatObj["horaRetorno"]?.jsonPrimitive?.content ?: ""
                val observacoes = viatObj["observacoes"]?.jsonPrimitive?.content ?: ""

                val viatId = UUID.randomUUID().toString()
                repository.addViatura(
                    Viatura(
                        id = viatId,
                        ocorrenciaId = oId,
                        prefixo = prefixo,
                        tipo = tipo,
                        unidade = unidade,
                        kmSaida = kmSaida,
                        kmLocal = kmLocal,
                        kmRetorno = kmRetorno,
                        horaDespacho = horaDespacho,
                        horaSaida = horaSaida,
                        horaChegada = horaChegada,
                        horaRetorno = horaRetorno,
                        observacoes = observacoes
                    )
                ).onSuccess { savedViatura ->
                    viatObj["equipe"]?.jsonArray?.forEach { milElement ->
                        val milObj = milElement.jsonObject
                        val re = milObj["re"]?.jsonPrimitive?.content ?: ""
                        val nomeGuerra = milObj["nomeGuerra"]?.jsonPrimitive?.content ?: ""
                        val graduacao = milObj["graduacao"]?.jsonPrimitive?.content ?: ""
                        val funcao = milObj["funcao"]?.jsonPrimitive?.content ?: ""

                        repository.addMilitar(
                            Militar(
                                id = UUID.randomUUID().toString(),
                                viaturaId = viatId,
                                re = re,
                                nomeGuerra = nomeGuerra,
                                graduacao = graduacao,
                                funcao = funcao
                            )
                        )
                    }
                }.getOrThrow()
            }

            // Importar apoios
            obj["apoios"]?.jsonArray?.forEach { apElement ->
                val apObj = apElement.jsonObject
                val orgaoId = apObj["orgaoId"]?.jsonPrimitive?.content ?: ""
                val orgaoSigla = apObj["orgaoSigla"]?.jsonPrimitive?.content ?: ""
                val orgaoNome = apObj["orgaoNome"]?.jsonPrimitive?.content ?: ""
                val viat = apObj["viatura"]?.jsonPrimitive?.content ?: ""
                val enc = apObj["encarregado"]?.jsonPrimitive?.content ?: ""
                val desc = apObj["descricaoOutros"]?.jsonPrimitive?.content ?: ""

                repository.vincularOrgaoApoioDetalhado(
                    ocorrenciaId = oId,
                    orgaoId = orgaoId,
                    viatura = viat,
                    encarregado = enc,
                    descricaoOutros = desc
                )
            }

            // Importar pessoas (não vítimas)
            obj["pessoas"]?.jsonArray?.forEach { pElement ->
                val pObj = pElement.jsonObject
                val nome = pObj["nome"]?.jsonPrimitive?.content ?: ""
                val cpf = pObj["cpf"]?.jsonPrimitive?.content
                val rg = pObj["rg"]?.jsonPrimitive?.content
                val reOrgao = pObj["rgOrgaoEmissor"]?.jsonPrimitive?.content
                val rgUf = pObj["rgUf"]?.jsonPrimitive?.content
                val nascimento = pObj["nascimento"]?.jsonPrimitive?.content
                val naturalidade = pObj["naturalidade"]?.jsonPrimitive?.content
                val nacionalidade = pObj["nacionalidade"]?.jsonPrimitive?.content
                val filiacao = pObj["filiacao"]?.jsonPrimitive?.content
                val sexo = pObj["sexo"]?.jsonPrimitive?.content
                val telefone = pObj["telefone"]?.jsonPrimitive?.content
                val email = pObj["email"]?.jsonPrimitive?.content
                val logradouro = pObj["logradouro"]?.jsonPrimitive?.content
                val numero = pObj["numero"]?.jsonPrimitive?.content
                val bairro = pObj["bairro"]?.jsonPrimitive?.content
                val cidade = pObj["cidade"]?.jsonPrimitive?.content
                val uf = pObj["uf"]?.jsonPrimitive?.content
                val cep = pObj["cep"]?.jsonPrimitive?.content

                val pessoa = Pessoa(
                    id = UUID.randomUUID().toString(),
                    nome = nome,
                    cpf = cpf,
                    rg = rg,
                    rgOrgaoEmissor = reOrgao,
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
                repository.upsertPessoa(pessoa).onSuccess { p ->
                    val docId = UUID.randomUUID().toString()
                    val doc = Documento(
                        id = docId,
                        ocorrenciaId = oId,
                        pessoaId = p.id,
                        tipo = "IDENTIFICACAO",
                        numero = cpf ?: rg ?: "S/N",
                        urlImagem = "",
                        textoOcr = "",
                        dadosEstruturados = emptyMap()
                    )
                    repository.salvarPessoaEDocumento(p, doc)
                }
            }

            // Evidências
            obj["evidencias"]?.jsonArray?.forEach { evElement ->
                val evObj = evElement.jsonObject
                val evId = evObj["id"]?.jsonPrimitive?.content ?: UUID.randomUUID().toString()
                val tipo = evObj["tipo"]?.jsonPrimitive?.content ?: "Imagem"
                val hashSha256 = evObj["hashSha256"]?.jsonPrimitive?.content ?: ""
                val latitude = evObj["latitude"]?.jsonPrimitive?.doubleOrNull
                val longitude = evObj["longitude"]?.jsonPrimitive?.doubleOrNull
                val dataHora = evObj["dataHora"]?.jsonPrimitive?.content ?: ""
                val usuario = evObj["usuario"]?.jsonPrimitive?.content
                val ocrBruto = evObj["ocrBruto"]?.jsonPrimitive?.content
                val fileName = evObj["fileName"]?.jsonPrimitive?.content ?: "imported_evidence_${System.currentTimeMillis()}"
                val base64 = evObj["bytes"]?.jsonPrimitive?.content ?: ""

                val jsonOcrMap = mutableMapOf<String, String>()
                evObj["jsonOcr"]?.jsonObject?.entries?.forEach { entry ->
                    jsonOcrMap[entry.key] = entry.value.jsonPrimitive.content
                }

                var localPath = ""
                if (base64.isNotEmpty()) {
                    try {
                        val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                        repository.uploadFile("evidencias", fileName, bytes).onSuccess { path ->
                            localPath = path
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("JsonImport", "Erro ao decodificar evidencia: ${e.message}")
                    }
                }

                val evidencia = Evidencia(
                    id = evId,
                    ocorrenciaId = oId,
                    tipo = tipo,
                    hashSha256 = hashSha256,
                    latitude = latitude,
                    longitude = longitude,
                    dataHora = dataHora,
                    usuario = usuario,
                    urlStorage = localPath,
                    miniaturaUrl = localPath,
                    ocrBruto = ocrBruto,
                    jsonOcr = jsonOcrMap
                )
                repository.addEvidencia(evidencia).getOrThrow()
            }

            ocorrencia
        }
    }
}
