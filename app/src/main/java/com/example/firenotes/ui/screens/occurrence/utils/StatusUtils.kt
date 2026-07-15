package com.example.firenotes.ui.screens.occurrence.utils

import androidx.compose.ui.graphics.Color
import com.example.firenotes.ui.screens.occurrence.OccurrenceFormUiState
import com.example.firenotes.ui.screens.occurrence.models.OccurrenceModule

fun calculateStatus(module: OccurrenceModule, state: OccurrenceFormUiState): Pair<String, Color> {
    val gray = Color(0xFF757575)
    val blue = Color(0xFF1976D2)
    val green = Color(0xFF2E7D32)
    val orange = Color(0xFFF57C00)
    val red = Color(0xFFD32F2F)

    return when (module) {
        OccurrenceModule.ENDERECO -> {
            if (state.rua.isBlank() && state.cidade.isBlank()) "Pendente" to red
            else if (state.numero.isBlank() || state.bairro.isBlank()) "Em andamento" to blue
            else "Concluído" to green
        }
        OccurrenceModule.VIATURAS -> {
            if (state.viaturas.isEmpty()) "Não iniciado" to gray
            else "Concluído" to green
        }
        OccurrenceModule.MILITARES -> {
            val total = state.viaturas.sumOf { it.equipe.size }
            if (state.viaturas.isEmpty()) "Não iniciado" to gray
            else if (total == 0) "Sem equipe" to orange
            else "Concluído" to green
        }
        OccurrenceModule.VEICULOS -> {
            if (state.veiculos.isEmpty()) "Não iniciado" to gray
            else "Concluído" to green
        }
        OccurrenceModule.DOCUMENTOS -> {
            if (state.documentos.isEmpty()) "Não iniciado" to gray
            else "Concluído" to green
        }
        OccurrenceModule.VITIMAS -> {
            if (state.vitimas.isEmpty()) "Não iniciado" to gray
            else "Concluído" to green
        }
        OccurrenceModule.APOIOS -> {
            if (state.apoiosDetalhados.isEmpty()) "Não iniciado" to gray
            else "Concluído" to green
        }
        OccurrenceModule.HISTORICO -> {
            if (state.historico.isBlank()) "Pendente" to red
            else if (state.historico.length < 50) "Revisar" to orange
            else "Concluído" to green
        }
        OccurrenceModule.EVIDENCIAS -> {
            if (state.evidencias.isEmpty()) "Não iniciado" to gray
            else "Concluído" to green
        }
        OccurrenceModule.ANEXOS -> {
            if (state.fotos.isEmpty() && state.videos.isEmpty()) "Não iniciado" to gray
            else "Concluído" to green
        }
    }
}
