package com.example.firenotes.data.service

import com.example.firenotes.domain.model.*
import java.lang.StringBuilder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OccurrenceNarrativeService @Inject constructor() {

    /**
     * Generates a structural narrative for the occurrence based on inputted parameters.
     * Ready for LLM/AI integration in V5.
     */
    fun generateNarrative(
        natureza: NaturezaOcorrencia,
        veiculos: List<VeiculoEnvolvido>,
        vitimas: List<Vitima>,
        viaturas: List<Viatura>,
        apoios: List<ApoioOcorrencia>,
        dataHora: String,
        endereco: String,
        resultado: String
    ): String {
        val builder = StringBuilder()
        builder.append("No dia $dataHora, a equipe de serviço foi acionada para atender a uma ocorrência de ${natureza.descricao} no endereço: $endereco.\n\n")

        if (viaturas.isNotEmpty()) {
            builder.append("Para o atendimento, foram empenhadas as seguintes viaturas: ")
            builder.append(viaturas.joinToString { it.prefixo })
            builder.append(".\n")
            
            viaturas.forEach { viatura ->
                if (viatura.equipe.isNotEmpty()) {
                    builder.append("A guarnição da viatura ${viatura.prefixo} estava composta por: ")
                    builder.append(viatura.equipe.joinToString { "${it.graduacao.descricao} ${it.nomeGuerra}" })
                    builder.append(".\n")
                }
            }
            builder.append("\n")
        }

        if (apoios.isNotEmpty()) {
            builder.append("Houve apoio dos órgãos: ")
            builder.append(apoios.joinToString { "${it.orgao.sigla} (${it.viatura ?: "s/ viatura"})" })
            builder.append(".\n\n")
        }

        if (veiculos.isNotEmpty()) {
            builder.append("Durante a verificação no local, constatou-se o envolvimento de: ")
            builder.append(veiculos.joinToString { "${it.modelo ?: "veículo"} (Placa: ${it.placa ?: "N/D"})" })
            builder.append(".\n\n")
        }

        if (vitimas.isNotEmpty()) {
            builder.append("Foram identificadas ${vitimas.size} vítima(s):\n")
            vitimas.forEach { vitima ->
                builder.append("- ${vitima.nome ?: "Não identificada"}, que apresentava lesões aparentes do tipo: ${vitima.lesoesAparentes ?: "N/D"}. ")
                builder.append("A vítima foi socorrida para o hospital ${vitima.hospitalDestino ?: "N/D"} pela viatura ${vitima.transportadoPor ?: "de apoio"}.\n")
            }
            builder.append("\n")
        }

        builder.append("Resultado final do atendimento: $resultado.\n")
        builder.append("Encerramento das atividades sem maiores alterações.")
        
        return builder.toString()
    }
}
