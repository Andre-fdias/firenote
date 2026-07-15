package com.example.firenotes.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firenotes.domain.model.NaturezaOcorrencia
import com.example.firenotes.domain.repository.OcorrenciaRepository
import com.example.firenotes.data.service.DashboardService
import com.example.firenotes.ui.designsystem.colors.FireColors
import com.example.firenotes.ui.designsystem.components.buttons.FireButton
import com.example.firenotes.ui.designsystem.components.cards.FireCard
import com.example.firenotes.ui.designsystem.components.topbar.FireTopBar
import com.example.firenotes.ui.designsystem.components.widgets.FireSectionHeader
import com.example.firenotes.ui.designsystem.icons.FireIcons
import com.example.firenotes.ui.designsystem.shapes.FireShapes
import com.example.firenotes.ui.designsystem.spacing.FireSpacing
import com.example.firenotes.ui.designsystem.typography.FireTypography
import com.example.firenotes.ui.designsystem.states.FireLoading
import com.example.firenotes.ui.designsystem.states.FireErrorState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

/* STREAMING_CHUNK: Designing the main screen scaffold and topbar navigation... */
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

/* STREAMING_CHUNK: Building the analytical content tabs navigation... */
@Composable
private fun DashboardContent(
    state: DashboardUiState,
    onNavigateToReports: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Geral", "Recursos", "Logística")

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = FireColors.Primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = FireColors.Primary
                )
            },
            modifier = Modifier.padding(vertical = FireSpacing.Small)
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            style = FireTypography.Title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp
                        )
                    }
                )
            }
        }

        /* STREAMING_CHUNK: Displaying the filtered Tab sections with LazyColumn... */
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium),
            contentPadding = PaddingValues(bottom = FireSpacing.Large)
        ) {
            when (selectedTab) {
                0 -> {
                    // TAB: GERAL (KPIs e Gráfico de Donut)
                    item {
                        KPICardsSection(
                            totalHoje = state.totalHoje,
                            totalMes = state.totalMes,
                            totalAno = state.totalAno,
                            totalOcorrencias = state.totalHoje + state.totalMes + state.totalAno
                        )
                    }

                    item {
                        NaturezaAnaliseCard(state.natureStats)
                    }
                }
                1 -> {
                    // TAB: RECURSOS (Viaturas e Equipes)
                    if (state.viaturaRanking.isNotEmpty()) {
                        item {
                            FireSectionHeader(
                                title = "Viaturas Mais Ativas",
                                icon = "🚒",
                                subtitle = "Uso operacional e quilometragem"
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

                    if (state.militarRanking.isNotEmpty()) {
                        item {
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
                                        secondaryText = "Atendeu ${militar.count} ocorrências • ~${militar.horasTrabalhadas}h",
                                        value = militar.count,
                                        color = FireColors.Secondary
                                    )
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // TAB: LOGÍSTICA (Hospitais e Cidades)
                    item {
                        FireSectionHeader(
                            title = "Destino de APH",
                            icon = "🏥",
                            subtitle = "Hospitais mais demandados"
                        )
                        FireCard {
                            if (state.hospitalRanking.isNotEmpty()) {
                                state.hospitalRanking.take(5).forEachIndexed { index, hosp ->
                                    RankingItemWithBadge(
                                        rank = index + 1,
                                        primaryText = hosp.nome.uppercase(),
                                        secondaryText = "Transporte de ${hosp.count} pacientes",
                                        value = hosp.count,
                                        color = FireColors.Warning
                                    )
                                }
                            } else {
                                Text(
                                    text = "Nenhum hospital registrado nas vítimas.",
                                    style = FireTypography.BodyMedium,
                                    color = FireColors.OnSurfaceVariant,
                                    modifier = Modifier.padding(vertical = FireSpacing.Medium)
                                )
                            }
                        }
                    }

                    if (state.municipioRanking.isNotEmpty()) {
                        item {
                            FireSectionHeader(
                                title = "Municípios Atendidos",
                                icon = "📍",
                                subtitle = "Distribuição de chamados por cidade"
                            )
                            FireCard {
                                state.municipioRanking.take(5).forEachIndexed { index, municipio ->
                                    RankingItemWithBadge(
                                        rank = index + 1,
                                        primaryText = municipio.nome.uppercase(),
                                        secondaryText = "${municipio.count} atendimentos",
                                        value = municipio.count,
                                        color = FireColors.Primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(FireSpacing.Small))
                FireButton(
                    text = "📄 GERAR RELATÓRIO COMPLETO",
                    onClick = onNavigateToReports,
                    icon = FireIcons.PictureAsPdf,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                )
            }
        }
    }
}

/* STREAMING_CHUNK: Refining top KPI cards with premium modern styling... */
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
                title = "Mês Corrente",
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
                title = "Ano Atual",
                value = totalAno,
                icon = "📊",
                color = FireColors.Warning,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "Histórico Total",
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
            .height(96.dp)
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = FireColors.Surface
        ),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(FireSpacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(FireSpacing.Medium))
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = value.toString(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = color,
                    lineHeight = 28.sp
                )
                Text(
                    text = title.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = FireColors.OnSurfaceVariant.copy(alpha = 0.7f),
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

/* STREAMING_CHUNK: Designing the Interactive Donut Chart structure... */
data class PieChartData(
    val label: String,
    val value: Int,
    val color: Color,
    val percentage: Float
)

@Composable
private fun NaturezaAnaliseCard(natureStats: Map<NaturezaOcorrencia, Int>) {
    val total = remember(natureStats) { natureStats.values.sum() }
    var selectedIndex by remember { mutableStateOf(-1) }

    val chartData = remember(natureStats, total) {
        natureStats.map { (natureza, count) ->
            PieChartData(
                label = natureza.descricao,
                value = count,
                color = when (natureza) {
                    NaturezaOcorrencia.INCENDIO -> FireColors.NaturezaIncendio
                    NaturezaOcorrencia.SALVAMENTO -> FireColors.NaturezaSalvamento
                    NaturezaOcorrencia.ACIDENTE_TRANSITO -> FireColors.NaturezaAcidente
                    NaturezaOcorrencia.QUEDA -> FireColors.NaturezaQueda
                    NaturezaOcorrencia.PESSOAL -> FireColors.NaturezaPessoal
                    NaturezaOcorrencia.INDEFINIDA -> androidx.compose.ui.graphics.Color.Gray
                },
                percentage = if (total > 0) count.toFloat() / total else 0f
            )
        }
    }

    FireSectionHeader(
        title = "Distribuição por Natureza",
        icon = "🏷️",
        subtitle = "Percentual de ocorrências por tipo"
    )

    FireCard {
        if (total > 0) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Interactive Donut Chart
                Box(
                    modifier = Modifier.size(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DonutChart(
                        data = chartData,
                        selectedIndex = selectedIndex,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Central Text showing selected metrics
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (selectedIndex in chartData.indices) {
                            val selectedItem = chartData[selectedIndex]
                            Text(
                                text = selectedItem.label.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = FireColors.OnSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${(selectedItem.percentage * 100).toInt()}%",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = selectedItem.color
                            )
                            Text(
                                text = "${selectedItem.value} atendimentos",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = FireColors.OnSurfaceVariant.copy(alpha = 0.8f)
                            )
                        } else {
                            Text(
                                text = "TOTAL",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = FireColors.OnSurfaceVariant
                            )
                            Text(
                                text = total.toString(),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = FireColors.Primary
                            )
                            Text(
                                text = "Ocorrências",
                                fontSize = 11.sp,
                                color = FireColors.OnSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(FireSpacing.ExtraSmall))

                /* STREAMING_CHUNK: Building the interactive list legend below the Donut Chart... */
                // Legend list acting as interactive filter
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                ) {
                    chartData.forEachIndexed { index, item ->
                        val isSelected = selectedIndex == index
                        val cardBg = if (isSelected) item.color.copy(alpha = 0.08f) else Color.Transparent
                        val cardBorder = if (isSelected) BorderStroke(1.2.dp, item.color) else BorderStroke(1.dp, FireColors.OnSurfaceVariant.copy(alpha = 0.05f))

                        Card(
                            onClick = {
                                selectedIndex = if (isSelected) -1 else index
                            },
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            shape = RoundedCornerShape(12.dp),
                            border = cardBorder,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = FireSpacing.Medium, vertical = FireSpacing.Small),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(item.color, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(FireSpacing.Small))
                                    Text(
                                        text = item.label,
                                        style = FireTypography.BodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) item.color else FireColors.OnSurface
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${item.value} ch",
                                        style = FireTypography.LabelMedium,
                                        color = FireColors.OnSurfaceVariant,
                                        modifier = Modifier.padding(end = FireSpacing.Small)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(item.color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${(item.percentage * 100).toInt()}%",
                                            style = FireTypography.LabelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = item.color
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aguardando cadastro de ocorrências para consolidar dados.",
                    style = FireTypography.BodyMedium,
                    color = FireColors.OnSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/* STREAMING_CHUNK: Implementing the Custom Canvas Donut Chart drawing with animation... */
@Composable
private fun DonutChart(
    data: List<PieChartData>,
    selectedIndex: Int,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "donut_reveal"
    )

    Canvas(modifier = modifier) {
        val total = data.map { it.value }.sum()
        if (total > 0f) {
            var startAngle = -90f // Start drawing from the top
            val strokeWidth = 32.dp.toPx()
            val innerStrokeWidth = 8.dp.toPx()

            data.forEachIndexed { index, item ->
                val sweepAngle = (item.value.toFloat() / total) * 360f * animatedProgress
                val isSelected = selectedIndex == index

                // Draw selection highlight underlay (glow)
                if (isSelected) {
                    drawArc(
                        color = item.color.copy(alpha = 0.25f),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        size = Size(size.width - strokeWidth, size.height - strokeWidth),
                        topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                        style = Stroke(width = strokeWidth + 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Draw main sector stroke
                drawArc(
                    color = item.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    size = Size(size.width - strokeWidth, size.height - strokeWidth),
                    topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                startAngle += sweepAngle
            }
        }
    }
}

/* STREAMING_CHUNK: Implementing the horizontal bar ranking charts... */
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
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "bar_progress"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = FireSpacing.Small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank Badge
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    when (rank) {
                        1 -> Color(0xFFFFD700).copy(alpha = 0.15f)
                        2 -> Color(0xFFC0C0C0).copy(alpha = 0.15f)
                        3 -> Color(0xFFCD7F32).copy(alpha = 0.15f)
                        else -> FireColors.OnSurfaceVariant.copy(alpha = 0.05f)
                    }
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
                fontSize = if (rank <= 3) 18.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                color = when (rank) {
                    1 -> Color(0xFF8A6D00)
                    2 -> Color(0xFF555555)
                    3 -> Color(0xFF7A431D)
                    else -> FireColors.OnSurfaceVariant
                }
            )
        }

        Spacer(modifier = Modifier.width(FireSpacing.Medium))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label.uppercase(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = FireColors.OnBackground
                )
                Text(
                    text = "$value chamados  •  $detail",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = FireColors.OnSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            // Custom Track Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(FireColors.SurfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(color, color.copy(alpha = 0.7f))
                            )
                        )
                )
            }
        }
    }
}

/* STREAMING_CHUNK: Implementing the List rankings with numerical badges... */
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
            .padding(vertical = FireSpacing.Small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank Badge
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(
                    if (rank <= 3) color.copy(alpha = 0.12f) else FireColors.OnSurfaceVariant.copy(alpha = 0.05f),
                    CircleShape
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
                fontSize = if (rank <= 3) 18.sp else 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (rank <= 3) color else FireColors.OnSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(FireSpacing.Medium))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = primaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = FireColors.OnBackground
            )
            Text(
                text = secondaryText,
                fontSize = 12.sp,
                color = FireColors.OnSurfaceVariant.copy(alpha = 0.8f)
            )
        }

        Spacer(modifier = Modifier.width(FireSpacing.Small))

        // Value Badge Indicator
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = color.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
        ) {
            Text(
                text = value.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
            )
        }
    }
}