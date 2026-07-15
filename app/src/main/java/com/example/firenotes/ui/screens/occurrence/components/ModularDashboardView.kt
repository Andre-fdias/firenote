package com.example.firenotes.ui.screens.occurrence.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firenotes.domain.model.*
import com.example.firenotes.ui.designsystem.colors.FireColors
import com.example.firenotes.ui.designsystem.components.buttons.FireButton
import com.example.firenotes.ui.designsystem.components.cards.FireCard
import com.example.firenotes.ui.designsystem.shapes.FireShapes
import com.example.firenotes.ui.designsystem.spacing.FireSpacing
import com.example.firenotes.ui.designsystem.typography.FireTypography
import com.example.firenotes.ui.screens.occurrence.OccurrenceFormUiState
import com.example.firenotes.ui.screens.occurrence.cards.ChecklistCard
import com.example.firenotes.ui.screens.occurrence.cards.SummaryCard
import com.example.firenotes.ui.screens.occurrence.models.ModuleInfo
import com.example.firenotes.ui.screens.occurrence.models.OccurrenceModule
import com.example.firenotes.ui.screens.occurrence.utils.calculateStatus
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ModularDashboardView(
    uiState: OccurrenceFormUiState,
    onModuleSelected: (OccurrenceModule) -> Unit,
    onFinishClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Cálculo seguro e reativo do tempo de duração do atendimento
    val tempoOcorrencia = remember(uiState.data, uiState.hora) {
        try {
            val dateStr = "${uiState.data.trim()} ${uiState.hora.trim()}"
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            val localDateTime = LocalDateTime.parse(dateStr, formatter)
            val created = localDateTime.atZone(ZoneId.systemDefault()).toInstant()
            val duration = Duration.between(created, Instant.now())
            val hours = duration.toHours()
            val minutes = duration.toMinutes() % 60
            "${hours}h ${minutes}min"
        } catch (e: Exception) {
            "N/D"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(FireSpacing.Medium),
        verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
    ) {
        // Card de Resumo Geral do Atendimento (Inalterado)
        SummaryCard(
            protocolo = uiState.protocolo,
            natureza = uiState.natureza.descricao,
            cidade = uiState.cidade,
            tempoOcorrencia = tempoOcorrencia,
            veiculosCount = uiState.veiculos.size,
            vitimasCount = uiState.vitimas.size,
            viaturasCount = uiState.viaturas.size,
            prontidao = uiState.prontidaoColor
        )

        // Checklist Operacional (Mantido Intacto)
        ChecklistCard(
            protocoloOk = uiState.protocolo.isNotBlank(),
            enderecoOk = uiState.rua.isNotBlank(),
            historicoOk = uiState.historico.isNotBlank(),
            viaturasOk = uiState.viaturas.isNotEmpty()
        )

        // Otimização Crítica: Evita reconstrução da lista e queries em recomposições frequentes da UI
        val modulesList = remember(uiState) {
            listOf(
                ModuleInfo("Endereço", "📍", uiState.rua.ifBlank { "Nenhum endereço" }, calculateStatus(OccurrenceModule.ENDERECO, uiState), onModuleSelected),
                ModuleInfo("Pessoas", "📄", "${uiState.documentos.size} pessoas", calculateStatus(OccurrenceModule.DOCUMENTOS, uiState), onModuleSelected),
                ModuleInfo("Viaturas", "🚒", "${uiState.viaturas.size} viaturas", calculateStatus(OccurrenceModule.VIATURAS, uiState), onModuleSelected),
                ModuleInfo("Militares", "👨‍🚒", "${uiState.viaturas.sumOf { it.equipe.size }} militares", calculateStatus(OccurrenceModule.MILITARES, uiState), onModuleSelected),
                ModuleInfo("Veículos", "🚗", "${uiState.veiculos.size} veículos", calculateStatus(OccurrenceModule.VEICULOS, uiState), onModuleSelected),
                ModuleInfo("Vítimas", "🩺", "${uiState.vitimas.size} vítimas", calculateStatus(OccurrenceModule.VITIMAS, uiState), onModuleSelected),
                ModuleInfo("Apoios", "🤝", "${uiState.apoiosDetalhados.size} apoios", calculateStatus(OccurrenceModule.APOIOS, uiState), onModuleSelected),
                ModuleInfo("Histórico", "📝", if (uiState.historico.isNotEmpty()) "Preenchido" else "Não iniciado", calculateStatus(OccurrenceModule.HISTORICO, uiState), onModuleSelected),
                ModuleInfo("Evidências", "📷", "${uiState.evidencias.size} evidências", calculateStatus(OccurrenceModule.EVIDENCIAS, uiState), onModuleSelected),
                ModuleInfo("Anexos", "📎", "${uiState.fotos.size + uiState.videos.size} mídias", calculateStatus(OccurrenceModule.ANEXOS, uiState), onModuleSelected)
            )
        }

        // Otimização de Layout: Divide a lista em pares de 2 e renderiza rows reais.
        // Isso elimina a limitação de altura arbitrária e garante 100% de renderização sem cortes.
        val chunkedModules = remember(modulesList) { modulesList.chunked(2) }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
        ) {
            chunkedModules.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                ) {
                    rowItems.forEach { item ->
                        Box(modifier = Modifier.weight(1f)) {
                            DashboardCard(item)
                        }
                    }
                    // Mantém o alinhamento em grid mesmo se o número de itens na linha for ímpar
                    if (rowItems.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(FireSpacing.ExtraSmall))

        // Botão de conclusão com alvo tátil de emergência robusto (glove-friendly)
        FireButton(
            text = "🏁 CONCLUIR OCORRÊNCIA",
            onClick = onFinishClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            containerColor = FireColors.Success
        )
    }
}

@Composable
fun DashboardCard(info: ModuleInfo) {
    val occurrenceModule = remember(info.title) {
        when(info.title) {
            "Endereço" -> OccurrenceModule.ENDERECO
            "Viaturas" -> OccurrenceModule.VIATURAS
            "Militares" -> OccurrenceModule.MILITARES
            "Veículos" -> OccurrenceModule.VEICULOS
            "Pessoas" -> OccurrenceModule.DOCUMENTOS
            "Vítimas" -> OccurrenceModule.VITIMAS
            "Apoios" -> OccurrenceModule.APOIOS
            "Histórico" -> OccurrenceModule.HISTORICO
            "Evidências" -> OccurrenceModule.EVIDENCIAS
            else -> OccurrenceModule.ANEXOS
        }
    }

    FireCard(
        onClick = { info.onSelected(occurrenceModule) },
        containerColor = FireColors.Surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(FireSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(FireSpacing.ExtraSmall)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(info.icon, fontSize = 24.sp)
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(info.statusColor, FireShapes.Circle)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = info.title,
                style = FireTypography.Title,
                fontWeight = FontWeight.Bold,
                color = FireColors.OnSurface
            )
            Text(
                text = info.summary,
                style = FireTypography.BodySmall,
                color = FireColors.OnSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = info.statusText.uppercase(),
                style = FireTypography.LabelSmall,
                color = info.statusColor,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
        }
    }
}