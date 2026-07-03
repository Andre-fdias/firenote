package com.example.firenotes.domain.model

import java.lang.StringBuilder

object ReportGenerator {
    fun generateOccurrenceReport(ocorrencia: Ocorrencia): String {
        val sb = StringBuilder()
        sb.append("=========================================\n")
        sb.append("         RELATÓRIO DE OCORRÊNCIA\n")
        sb.append("=========================================\n")
        sb.append("TALÃO: ${ocorrencia.protocolo}\n")
        sb.append("NATUREZA: ${ocorrencia.natureza.descricao.uppercase()}\n")
        sb.append("DATA/HORA: ${ocorrencia.dataHora}\n")
        sb.append("ENDEREÇO: ${ocorrencia.rua ?: "N/D"}, ${ocorrencia.numero ?: "S/N"} - ${ocorrencia.bairro ?: "N/D"}, ${ocorrencia.cidade ?: "N/D"}/${ocorrencia.uf ?: "N/D"}\n")
        sb.append("-----------------------------------------\n")
        sb.append("HISTÓRICO:\n")
        sb.append(ocorrencia.historico ?: "Sem histórico registrado.")
        sb.append("\n\n-----------------------------------------\n")
        sb.append("VIATURAS EMPREGADAS\n")
        sb.append("-----------------------------------------\n")
        if (ocorrencia.viaturas.isEmpty()) {
            sb.append("Nenhuma viatura registrada.\n")
        } else {
            ocorrencia.viaturas.forEach { viatura ->
                sb.append("🚒 ${viatura.prefixo}\n")
                sb.append("Tipo: ${viatura.tipo}\n")
                sb.append("KM Saída: ${viatura.kmSaida ?: "N/D"}\n")
                sb.append("KM Local: ${viatura.kmLocal ?: "N/D"}\n")
                if (!viatura.unidade.isNullOrBlank()) {
                    sb.append("Unidade: ${viatura.unidade}\n")
                }
                sb.append("Equipe:\n")
                if (viatura.equipe.isEmpty()) {
                    sb.append("  Nenhum militar escalado.\n")
                } else {
                    viatura.equipe.forEach { militar ->
                        sb.append("  - ${militar.graduacao.descricao} ${militar.nomeGuerra} (${militar.funcao ?: "Membro"})\n")
                    }
                }
                sb.append("\n")
            }
        }
        sb.append("-----------------------------------------\n")
        sb.append("VÍTIMAS SOCORRIDAS\n")
        sb.append("-----------------------------------------\n")
        if (ocorrencia.vitimas.isEmpty()) {
            sb.append("Nenhuma vítima registrada.\n")
        } else {
            ocorrencia.vitimas.forEach { vitima ->
                sb.append("👤 Nome: ${vitima.nome ?: "Não Identificado"}\n")
                sb.append("Idade: ${vitima.idade ?: "N/D"} anos\n")
                sb.append("Lesões: ${vitima.lesoesAparentes ?: "N/D"}\n")
                sb.append("Sinais Vitais: PA: ${vitima.sinaisVitais.pressaoArterial ?: "N/D"} | FC: ${vitima.sinaisVitais.pulso ?: "N/D"} BPM | Sat. O2: ${vitima.sinaisVitais.saturacaoO2 ?: "N/D"}% | GCS: ${vitima.sinaisVitais.escalaGCS ?: "N/D"}\n")
                sb.append("Hospital Destino: ${vitima.hospitalDestino ?: "N/D"}\n")
                sb.append("Transportado Por: ${vitima.transportadoPor ?: "N/D"}\n")
                sb.append("\n")
            }
        }
        sb.append("=========================================\n")
        return sb.toString()
    }
}
