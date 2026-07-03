package com.example.firenotes.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import com.example.firenotes.domain.model.NaturezaOcorrencia
import com.example.firenotes.ui.designsystem.colors.FireColors
import com.example.firenotes.ui.designsystem.shapes.FireShapes
import com.example.firenotes.ui.designsystem.spacing.FireSpacing
import com.example.firenotes.ui.designsystem.typography.FireTypography
import com.example.firenotes.ui.designsystem.components.topbar.FireTopBar
import com.example.firenotes.ui.designsystem.components.cards.FireCard
import com.example.firenotes.ui.designsystem.components.widgets.FireSectionHeader
import com.example.firenotes.ui.designsystem.components.widgets.FireStatItem
import com.example.firenotes.ui.designsystem.components.widgets.FireRankingItem
import com.example.firenotes.ui.designsystem.states.FireLoading
import com.example.firenotes.ui.designsystem.states.FireErrorState

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            FireTopBar(
                title = "📊📊 Dashboard",
                onBackClick = onNavigateBack,
                backgroundColor = FireColors.Surface,
                elevation = 2.dp
            )
        },
        containerColor = FireColors.Background,
        modifier = modifier
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    FireLoading()
                }
            }
            uiState.errorMessage != null -> {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    FireErrorState(
                        message = uiState.errorMessage!!,
                        onRetry = { viewModel.loadStats() }
                    )
                }
            }
            else -> {
                DashboardContent(
                    state = uiState,
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(horizontal = FireSpacing.Medium)
                )
            }
        }
    }
}

@Composable
private fun DashboardContent(
    state: DashboardUiState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium),
        contentPadding = PaddingValues(vertical = FireSpacing.Medium)
    ) {
        // Resumo Geral
        item {
            FireSectionHeader(
                title = "Resumo Geral",
                icon = "📈📈"
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small),
                modifier = Modifier.fillMaxWidth()
            ) {
                FireStatItem(
                    label = "Hoje",
                    value = state.totalHoje,
                    icon = "📅📅",
                    color = FireColors.Primary,
                    modifier = Modifier.weight(1f)
                )
                FireStatItem(
                    label = "Mês",
                    value = state.totalMes,
                    icon = "📆📆",
                    color = FireColors.Secondary,
                    modifier = Modifier.weight(1f)
                )
                FireStatItem(
                    label = "Ano",
                    value = state.totalAno,
                    icon = "📊📊",
                    color = FireColors.Warning,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Naturezas
        item {
            FireSectionHeader(
                title = "Naturezas",
                icon = "🏷🏷️"
            )
            FireCard {
                val total = state.natureStats.values.sum()
                state.natureStats.forEach { (natureza, count) ->
                    FireNaturezaRow(
                        natureza = natureza,
                        count = count,
                        total = total
                    )
                }
            }
        }

        // Ranking Viaturas
        if (state.viaturaRanking.isNotEmpty()) {
            item {
                FireSectionHeader(
                    title = "Viaturas Mais Ativas",
                    icon = "🚒🚒"
                )
                FireCard {
                    state.viaturaRanking.take(5).forEachIndexed { index, viatura ->
                        FireRankingItem(
                            rank = index + 1,
                            primaryText = "Viatura ${viatura.prefixo}",
                            secondaryText = "${viatura.count} ocorrências • ${viatura.kmPercorrida}km",
                            value = viatura.count,
                            color = FireColors.Primary
                        )
                    }
                }
            }
        }

        // Ranking Militares
        if (state.militarRanking.isNotEmpty()) {
            item {
                FireSectionHeader(
                    title = "Efetivo Operacional",
                    icon = "👤👤"
                )
                FireCard {
                    state.militarRanking.take(5).forEachIndexed { index, militar ->
                        FireRankingItem(
                            rank = index + 1,
                            primaryText = militar.nomeGuerra,
                            secondaryText = "RE: ${militar.re} • ${militar.horasTrabalhadas} horas",
                            value = militar.count,
                            color = FireColors.Secondary
                        )
                    }
                }
            }
        }

        // Ranking Hospitais
        if (state.hospitalRanking.isNotEmpty()) {
            item {
                FireSectionHeader(
                    title = "Destino das Vítimas",
                    icon = "🏥🏥"
                )
                FireCard {
                    state.hospitalRanking.take(5).forEachIndexed { index, hosp ->
                        FireRankingItem(
                            rank = index + 1,
                            primaryText = hosp.nome,
                            secondaryText = "Vítimas encaminhadas",
                            value = hosp.count,
                            color = FireColors.Warning
                        )
                    }
                }
            }
        }

        // Georreferenciamento de Regiões
        if (state.regionGroups.isNotEmpty()) {
            item {
                FireSectionHeader(
                    title = "Regiões de Atendimento",
                    icon = "📍📍"
                )
                FireCard {
                    state.regionGroups.forEachIndexed { index, region ->
                        FireRankingItem(
                            rank = index + 1,
                            primaryText = region.regiao,
                            secondaryText = "Coordenadas: ${region.coordenadas.firstOrNull()?.first ?: 0.0}, ${region.coordenadas.firstOrNull()?.second ?: 0.0}",
                            value = region.count,
                            color = FireColors.Primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FireNaturezaRow(
    natureza: NaturezaOcorrencia,
    count: Int,
    total: Int
) {
    val color = when (natureza) {
        NaturezaOcorrencia.INCENDIO -> FireColors.NaturezaIncendio
        NaturezaOcorrencia.SALVAMENTO -> FireColors.NaturezaSalvamento
        NaturezaOcorrencia.ACIDENTE_TRANSITO -> FireColors.NaturezaAcidente
        NaturezaOcorrencia.QUEDA -> FireColors.NaturezaQueda
        NaturezaOcorrencia.PESSOAL -> FireColors.NaturezaPessoal
    }
    val ratio = if (total > 0) count.toFloat() / total else 0f
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = FireSpacing.ExtraSmall),
        verticalArrangement = Arrangement.spacedBy(FireSpacing.ExtraSmall)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(natureza.descricao, style = FireTypography.BodyMedium, fontWeight = FontWeight.Medium)
            Text("$count ocorrências", style = FireTypography.LabelMedium)
        }
        LinearProgressIndicator(
            progress = { ratio.coerceIn(0f, 1f) },
            color = color,
            trackColor = FireColors.SurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(FireShapes.Small)
        )
    }
}
