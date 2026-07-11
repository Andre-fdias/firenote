package com.example.firenotes.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.shadow
import com.example.firenotes.domain.model.NaturezaOcorrencia
import com.example.firenotes.ui.designsystem.colors.FireColors
import com.example.firenotes.ui.designsystem.shapes.FireShapes
import com.example.firenotes.ui.designsystem.spacing.FireSpacing
import com.example.firenotes.ui.designsystem.typography.FireTypography
import com.example.firenotes.ui.designsystem.components.topbar.FireTopBar
import com.example.firenotes.ui.designsystem.components.cards.FireCard
import com.example.firenotes.ui.designsystem.components.widgets.FireSectionHeader
import com.example.firenotes.ui.designsystem.components.buttons.FireButton
import com.example.firenotes.ui.designsystem.icons.FireIcons
import com.example.firenotes.ui.designsystem.states.FireLoading
import com.example.firenotes.ui.designsystem.states.FireErrorState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToReports: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            FireTopBar(
                title = "📊 Dashboard Analítico",
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
                    onNavigateToReports = onNavigateToReports,
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
    onNavigateToReports: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium),
        contentPadding = PaddingValues(vertical = FireSpacing.Medium)
    ) {
        // ============================================
        // KPI CARDS - TOP METRICS
        // ============================================
        item {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(400)) + slideInVertically(initialOffsetY = { -50 })
            ) {
                KPICardsSection(
                    totalHoje = state.totalHoje,
                    totalMes = state.totalMes,
                    totalAno = state.totalAno,
                    totalOcorrencias = state.totalHoje + state.totalMes + state.totalAno
                )
            }
        }

        // ============================================
        // GRÁFICO DE PIZZA - NATUREZAS
        // ============================================
        item {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 200)) + slideInVertically(initialOffsetY = { 50 })
            ) {
                Column {
                    FireSectionHeader(
                        title = "Distribuição por Natureza",
                        icon = "🏷️",
                        subtitle = "Percentual de ocorrências por tipo"
                    )
                    FireCard {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
                        ) {
                            // Pie Chart
                            val total = state.natureStats.values.sum()
                            if (total > 0) {
                                PieChart(
                                    data = state.natureStats.map { (natureza, count) ->
                                        PieChartData(
                                            label = natureza.descricao,
                                            value = count.toFloat() / total,
                                            color = when (natureza) {
                                                NaturezaOcorrencia.INCENDIO -> FireColors.NaturezaIncendio
                                                NaturezaOcorrencia.SALVAMENTO -> FireColors.NaturezaSalvamento
                                                NaturezaOcorrencia.ACIDENTE_TRANSITO -> FireColors.NaturezaAcidente
                                                NaturezaOcorrencia.QUEDA -> FireColors.NaturezaQueda
                                                NaturezaOcorrencia.PESSOAL -> FireColors.NaturezaPessoal
                                            }
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .padding(vertical = 8.dp)
                                )
                            }
                            
                            // Legend
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
                            ) {
                                state.natureStats.forEach { (natureza, count) ->
                                    val color = when (natureza) {
                                        NaturezaOcorrencia.INCENDIO -> FireColors.NaturezaIncendio
                                        NaturezaOcorrencia.SALVAMENTO -> FireColors.NaturezaSalvamento
                                        NaturezaOcorrencia.ACIDENTE_TRANSITO -> FireColors.NaturezaAcidente
                                        NaturezaOcorrencia.QUEDA -> FireColors.NaturezaQueda
                                        NaturezaOcorrencia.PESSOAL -> FireColors.NaturezaPessoal
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .background(color, FireShapes.Small)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${natureza.descricao} ($count)",
                                            fontSize = 12.sp,
                                            color = FireColors.OnSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ============================================
        // RANKING VIATURAS COM BARRAS
        // ============================================
        if (state.viaturaRanking.isNotEmpty()) {
            item {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(400, delayMillis = 300)) + slideInVertically(initialOffsetY = { 50 })
                ) {
                    Column {
                        FireSectionHeader(
                            title = "Viaturas Mais Ativas",
                            icon = "🚒",
                            subtitle = "Ranking de uso e quilometragem"
                        )
                        FireCard {
                            val maxCount = state.viaturaRanking.maxOfOrNull { it.count } ?: 1
                            state.viaturaRanking.take(5).forEachIndexed { index, viatura ->
                                HorizontalBarItem(
                                    rank = index + 1,
                                    label = viatura.prefixo,
                                    value = viatura.count,
                                    maxValue = maxCount,
                                    detail = "${viatura.kmPercorrida} km",
                                    color = FireColors.Primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // ============================================
        // RANKING MILITARES
        // ============================================
        if (state.militarRanking.isNotEmpty()) {
            item {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(400, delayMillis = 400)) + slideInVertically(initialOffsetY = { 50 })
                ) {
                    Column {
                        FireSectionHeader(
                            title = "Efetivo Operacional",
                            icon = "👨‍🚒",
                            subtitle = "Militares com maior participação"
                        )
                        FireCard {
                            state.militarRanking.take(5).forEachIndexed { index, militar ->
                                RankingItemWithBadge(
                                    rank = index + 1,
                                    primaryText = militar.nomeGuerra,
                                    secondaryText = "${militar.count} ocorrências • ${militar.horasTrabalhadas}h",
                                    value = militar.count,
                                    color = FireColors.Secondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // ============================================
        // HOSPITAIS E MUNICÍPIOS EM GRID
        // ============================================
        item {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 500)) + slideInVertically(initialOffsetY = { 50 })
            ) {
                Column {
                    FireSectionHeader(
                        title = "Destino das Vítimas",
                        icon = "🏥",
                        subtitle = "Hospitais mais utilizados"
                    )
                    FireCard {
                        if (state.hospitalRanking.isNotEmpty()) {
                            state.hospitalRanking.take(5).forEachIndexed { index, hosp ->
                                RankingItemWithBadge(
                                    rank = index + 1,
                                    primaryText = hosp.nome,
                                    secondaryText = "${hosp.count} pacientes",
                                    value = hosp.count,
                                    color = FireColors.Warning
                                )
                            }
                        } else {
                            Text(
                                text = "Nenhum hospital registrado",
                                style = FireTypography.BodyMedium,
                                color = FireColors.OnSurfaceVariant,
                                modifier = Modifier.padding(vertical = FireSpacing.Medium)
                            )
                        }
                    }
                }
            }
        }

        // ============================================
        // MUNICÍPIOS
        // ============================================
        if (state.municipioRanking.isNotEmpty()) {
            item {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(400, delayMillis = 600)) + slideInVertically(initialOffsetY = { 50 })
                ) {
                    Column {
                        FireSectionHeader(
                            title = "Municípios Atendidos",
                            icon = "📍",
                            subtitle = "Cidades com mais ocorrências"
                        )
                        FireCard {
                            state.municipioRanking.take(5).forEachIndexed { index, municipio ->
                                RankingItemWithBadge(
                                    rank = index + 1,
                                    primaryText = municipio.nome,
                                    secondaryText = "${municipio.count} ocorrências",
                                    value = municipio.count,
                                    color = FireColors.Info
                                )
                            }
                        }
                    }
                }
            }
        }

        // ============================================
        // BOTÃO RELATÓRIOS
        // ============================================
        item {
            Spacer(modifier = Modifier.height(FireSpacing.Small))
            FireButton(
                text = "📄 GERAR RELATÓRIO COMPLETO",
                onClick = onNavigateToReports,
                icon = FireIcons.PictureAsPdf,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(FireSpacing.Small))
        }
    }
}

// ============================================
// KPI CARDS
// ============================================

@Composable
private fun KPICardsSection(
    totalHoje: Int,
    totalMes: Int,
    totalAno: Int,
    totalOcorrencias: Int
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(FireSpacing.Small),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small),
            modifier = Modifier.fillMaxWidth()
        ) {
            KpiCard(
                title = "Hoje",
                value = totalHoje,
                icon = "📅",
                color = FireColors.Primary,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "Mês",
                value = totalMes,
                icon = "📆",
                color = FireColors.Secondary,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small),
            modifier = Modifier.fillMaxWidth()
        ) {
            KpiCard(
                title = "Ano",
                value = totalAno,
                icon = "📊",
                color = FireColors.Warning,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "Total",
                value = totalOcorrencias,
                icon = "🎯",
                color = Color(0xFF9C27B0),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: Int,
    icon: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(80.dp)
            .shadow(2.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = icon,
                fontSize = 20.sp
            )
            Text(
                text = value.toString(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                fontSize = 10.sp,
                color = FireColors.OnSurfaceVariant,
                letterSpacing = 0.5.sp
            )
        }
    }
}

// ============================================
// PIE CHART
// ============================================

data class PieChartData(
    val label: String,
    val value: Float,
    val color: Color
)

@Composable
private fun PieChart(
    data: List<PieChartData>,
    modifier: Modifier = Modifier
) {
    val animatedRotation by animateFloatAsState(
        targetValue = 360f,
        animationSpec = tween(1500, easing = LinearEasing),
        label = "rotation"
    )

    Canvas(
        modifier = modifier
    ) {
        val total = data.map { it.value }.sum()
        if (total > 0f) {
            var startAngle = 0f
            data.forEach { item ->
                val sweepAngle = (item.value / total) * 360f
                drawArc(
                    color = item.color,
                    startAngle = startAngle + animatedRotation,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    size = Size(size.width * 0.7f, size.height * 0.8f),
                    topLeft = Offset(size.width * 0.15f, size.height * 0.1f)
                )
                startAngle += sweepAngle
            }
        }
    }
}

// ============================================
// HORIZONTAL BAR CHART
// ============================================

@Composable
private fun HorizontalBarItem(
    rank: Int,
    label: String,
    value: Int,
    maxValue: Int,
    detail: String,
    color: Color
) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (maxValue > 0) value.toFloat() / maxValue else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "bar_progress"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank
        Text(
            text = when (rank) {
                1 -> "🥇"
                2 -> "🥈"
                3 -> "🥉"
                else -> rank.toString()
            },
            fontSize = 16.sp,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = FireColors.OnBackground
                )
                Text(
                    text = "$value • $detail",
                    fontSize = 12.sp,
                    color = FireColors.OnSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(FireColors.SurfaceVariant, FireShapes.Small)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(color, FireShapes.Small)
                )
            }
        }
    }
}

// ============================================
// RANKING ITEM WITH BADGE
// ============================================

@Composable
private fun RankingItemWithBadge(
    rank: Int,
    primaryText: String,
    secondaryText: String,
    value: Int,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank Badge
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    if (rank <= 3) color.copy(alpha = 0.15f) else Color.Transparent,
                    FireShapes.Circle
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (rank) {
                    1 -> "🥇"
                    2 -> "🥈"
                    3 -> "🥉"
                    else -> rank.toString()
                },
                fontSize = if (rank <= 3) 16.sp else 12.sp,
                fontWeight = if (rank <= 3) FontWeight.Bold else FontWeight.Normal
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = primaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = FireColors.OnBackground
            )
            Text(
                text = secondaryText,
                fontSize = 12.sp,
                color = FireColors.OnSurfaceVariant
            )
        }

        Surface(
            shape = FireShapes.Small,
            color = color.copy(alpha = 0.12f)
        ) {
            Text(
                text = value.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

// ============================================
// FLOW ROW EXTENSION
// ============================================

@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        content = { content() }
    )
}
