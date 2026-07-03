package com.example.firenotes.data.service

import com.example.firenotes.domain.model.NaturezaOcorrencia
import javax.inject.Inject
import javax.inject.Singleton

data class DashboardStats(
    val totalOcorrencias: Int,
    val totalPorNatureza: Map<NaturezaOcorrencia, Int>,
    val tempoRespostaMedioMinutos: Double,
    val tempoAtendimentoMedioMinutos: Double,
    val viaturasMaisAtivas: Map<String, Int>, // prefixo -> contagem
    val militaresMaisAtivos: Map<String, Int>, // RE -> contagem
    val principaisHospitaisDestino: Map<String, Int>, // Hospital -> contagem
    val ocorrenciasPorMunicipio: Map<String, Int> // Municipio -> contagem
)

@Singleton
class DashboardService @Inject constructor() {

    /**
     * Executes queries and compiles statistical metrics for Dashboard analytics.
     * Ready for database and UI integration in future versions.
     */
    suspend fun getStatisticsSummary(periodDays: Int): Result<DashboardStats> {
        return runCatching {
            // Stub data representing database count aggregates
            DashboardStats(
                totalOcorrencias = 124,
                totalPorNatureza = mapOf(
                    NaturezaOcorrencia.INCENDIO to 34,
                    NaturezaOcorrencia.SALVAMENTO to 45,
                    NaturezaOcorrencia.ACIDENTE_TRANSITO to 25,
                    NaturezaOcorrencia.QUEDA to 12,
                    NaturezaOcorrencia.PESSOAL to 8
                ),
                tempoRespostaMedioMinutos = 8.4,
                tempoAtendimentoMedioMinutos = 42.5,
                viaturasMaisAtivas = mapOf("UR-15201" to 42, "ABS-15012" to 28),
                militaresMaisAtivos = mapOf("123456-7" to 38, "765432-1" to 35),
                principaisHospitaisDestino = mapOf("Hospital das Clínicas" to 22, "Santa Casa" to 15),
                ocorrenciasPorMunicipio = mapOf("São Paulo" to 85, "Campinas" to 39)
            )
        }
    }
}
