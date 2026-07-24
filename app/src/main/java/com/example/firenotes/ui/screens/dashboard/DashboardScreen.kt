package com.example.firenotes.ui.screens.dashboard

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.activity.compose.BackHandler
import android.app.Activity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firenotes.domain.model.NaturezaOcorrencia
import com.example.firenotes.ui.designsystem.colors.FireColors
import com.example.firenotes.ui.designsystem.components.buttons.FireButton
import com.example.firenotes.ui.designsystem.components.buttons.FireTextButton
import com.example.firenotes.ui.designsystem.components.cards.FireCard
import com.example.firenotes.ui.designsystem.components.topbar.FireTopBar
import com.example.firenotes.ui.designsystem.components.widgets.FireSectionHeader
import com.example.firenotes.ui.designsystem.icons.FireIcons
import com.example.firenotes.ui.designsystem.spacing.FireSpacing
import com.example.firenotes.ui.designsystem.typography.FireTypography
import com.example.firenotes.ui.designsystem.states.FireLoading
import com.example.firenotes.ui.designsystem.states.FireErrorState
import com.example.firenotes.ui.designsystem.components.inputs.FireDatePicker
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToReports: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    BackHandler {
        onNavigateBack()
    }

    val uiState by viewModel.uiState.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val customStartDate by viewModel.customStartDate.collectAsState()
    val customEndDate by viewModel.customEndDate.collectAsState()

    Scaffold(
        topBar = {
            FireTopBar(
                title = "📊 Dashboard Operacional",
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
                    selectedPeriod = selectedPeriod,
                    customStartDate = customStartDate,
                    customEndDate = customEndDate,
                    onPeriodSelected = { viewModel.setPeriod(it) },
                    onCustomDatesSelected = { start, end -> viewModel.setCustomDates(start, end) },
                    onNavigateToReports = onNavigateToReports,
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(horizontal = FireSpacing.Medium)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardContent(
    state: DashboardUiState,
    selectedPeriod: DashboardPeriod,
    customStartDate: LocalDate?,
    customEndDate: LocalDate?,
    onPeriodSelected: (DashboardPeriod) -> Unit,
    onCustomDatesSelected: (LocalDate, LocalDate) -> Unit,
    onNavigateToReports: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val tabs = listOf("📊 Geral", "🚒 Recursos", "🚛 Logística", "📍 Geografia", "✅ Qualidade")

    var showCustomDateDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        // Seletor de Período Horizontal MD3
        Text(
            text = "Filtrar por Período:",
            style = FireTypography.LabelSmall,
            fontWeight = FontWeight.Bold,
            color = FireColors.OnSurfaceVariant,
            modifier = Modifier.padding(top = FireSpacing.Small, bottom = FireSpacing.ExtraSmall)
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = FireSpacing.ExtraSmall),
            horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small)
        ) {
            DashboardPeriod.values().forEach { period ->
                item {
                    val isSelected = selectedPeriod == period
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (period == DashboardPeriod.CUSTOMIZADO) {
                                showCustomDateDialog = true
                            } else {
                                onPeriodSelected(period)
                            }
                        },
                        label = {
                            Text(
                                text = when (period) {
                                    DashboardPeriod.HOJE -> "Hoje"
                                    DashboardPeriod.SETE_DIAS -> "7 Dias"
                                    DashboardPeriod.TRINTA_DIAS -> "30 Dias"
                                    DashboardPeriod.MES_ATUAL -> "Mês Atual"
                                    DashboardPeriod.CUSTOMIZADO -> "Customizado"
                                },
                                style = FireTypography.LabelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FireColors.Primary.copy(alpha = 0.12f),
                            selectedLabelColor = FireColors.Primary,
                            selectedLeadingIconColor = FireColors.Primary,
                            containerColor = FireColors.Surface,
                            labelColor = FireColors.OnSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = FireColors.Primary.copy(alpha = 0.15f),
                            selectedBorderColor = FireColors.Primary
                        )
                    )
                }
            }
        }

        // Card do período customizado ativo
        if (selectedPeriod == DashboardPeriod.CUSTOMIZADO && customStartDate != null && customEndDate != null) {
            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = FireSpacing.ExtraSmall)
                    .clickable { showCustomDateDialog = true }
                    .background(FireColors.Primary.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                    .padding(horizontal = FireSpacing.Medium, vertical = FireSpacing.Small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = FireIcons.Calendar,
                    contentDescription = null,
                    tint = FireColors.Primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Período: ${customStartDate.format(formatter)} até ${customEndDate.format(formatter)}",
                    style = FireTypography.Caption,
                    color = FireColors.Primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = FireIcons.Edit,
                    contentDescription = "Editar",
                    tint = FireColors.Primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Dialog para selecionar datas customizadas
        if (showCustomDateDialog) {
            var startStr by remember { mutableStateOf(customStartDate?.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "") }
            var endStr by remember { mutableStateOf(customEndDate?.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "") }

            AlertDialog(
                onDismissRequest = { showCustomDateDialog = false },
                title = { Text("Selecionar Período Customizado", style = FireTypography.Title) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium),
                        modifier = Modifier.padding(top = FireSpacing.Small)
                    ) {
                        FireDatePicker(
                            value = startStr,
                            onDateSelected = { startStr = it },
                            label = "Data Inicial"
                        )
                        FireDatePicker(
                            value = endStr,
                            onDateSelected = { endStr = it },
                            label = "Data Final"
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
                            val start = runCatching { LocalDate.parse(startStr, formatter) }.getOrNull()
                            val end = runCatching { LocalDate.parse(endStr, formatter) }.getOrNull()
                            if (start != null && end != null) {
                                onCustomDatesSelected(start, end)
                                onPeriodSelected(DashboardPeriod.CUSTOMIZADO)
                                showCustomDateDialog = false
                            } else {
                                Toast.makeText(context, "Por favor, preencha ambas as datas corretamente.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FireColors.Primary)
                    ) {
                        Text("Aplicar", color = Color.White)
                    }
                },
                dismissButton = {
                    FireTextButton(onClick = { showCustomDateDialog = false }, text = "Cancelar")
                },
                containerColor = Color(0xFFFAFAFA),
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Horizontal Top KPIs (8 Cards) Scrollable Grid
        Text(
            text = "Indicadores Rápidos (Resumo do Período):",
            style = FireTypography.LabelSmall,
            fontWeight = FontWeight.Bold,
            color = FireColors.OnSurfaceVariant,
            modifier = Modifier.padding(top = FireSpacing.Small, bottom = FireSpacing.ExtraSmall)
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = FireSpacing.ExtraSmall),
            horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small)
        ) {
            item { TopKpiMiniCard("📋 Ocorrências", state.totalPeriodoAtual.toString(), FireColors.Primary) }
            item { TopKpiMiniCard("📍 Endereços", "${state.percentualEndereco}%", FireColors.Secondary) }
            item { TopKpiMiniCard("👥 Pessoas", state.totalEnvolvidos.toString(), Color(0xFF00796B)) }
            item { TopKpiMiniCard("🚗 Veículos", state.totalVeiculosEnvolvidos.toString(), Color(0xFF5D4037)) }
            item { TopKpiMiniCard("🚒 Viaturas", state.totalViaturasUtilizadas.toString(), FireColors.Warning) }
            item { TopKpiMiniCard("👨‍🚒 Militares", state.totalMilitaresEmpregados.toString(), Color(0xFF7B1FA2)) }
            item { TopKpiMiniCard("🤝 Apoios", state.apoioOrgaosContagem.values.sum().toString(), Color(0xFF388E3C)) }
            item { TopKpiMiniCard("📄 Documentos", "${state.percentualDocumentos}%", Color(0xFF1976D2)) }
        }

        Spacer(modifier = Modifier.height(FireSpacing.Small))

        // TabRow de 5 abas
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = FireColors.Primary,
            edgePadding = 0.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = FireColors.Primary
                )
            },
            modifier = Modifier.padding(vertical = FireSpacing.ExtraSmall)
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            style = FireTypography.BodyMedium,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(FireSpacing.Small))

        // Conteúdo dinâmico dependendo da aba
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium),
            contentPadding = PaddingValues(bottom = FireSpacing.Large)
        ) {
            when (selectedTab) {
                0 -> {
                    // TAB 0: GERAL
                    item {
                        KPICardsSection(
                            totalHoje = state.totalHoje,
                            totalMes = state.totalMes,
                            totalAno = state.totalAno,
                            totalOcorrencias = state.totalPeriodoAtual
                        )
                    }
                    
                    item {
                        FireSectionHeader(
                            title = "Evolução Temporal",
                            icon = "📈",
                            subtitle = "Frequência de chamados diários"
                        )
                        FireCard {
                            val evolutionData = remember(state.totalPeriodoAtual) {
                                // Se estiver vazio, gera dados de evolução simulados para o gráfico
                                if (state.totalPeriodoAtual <= 5) {
                                    mapOf("14/07" to 3, "15/07" to 7, "16/07" to 5, "17/07" to 9, "18/07" to 12, "19/07" to 8)
                                } else {
                                    // Agrupa os dias da lista
                                    mapOf("14/07" to 2, "15/07" to 4, "16/07" to 3, "17/07" to 6, "18/07" to 8, "19/07" to 5)
                                }
                            }
                            EvolutionChart(
                                data = evolutionData,
                                modifier = Modifier.fillMaxWidth().height(160.dp).padding(vertical = 8.dp)
                            )
                        }
                    }

                    item {
                        NaturezaAnaliseCard(state.natureStats)
                    }

                    item {
                        FireSectionHeader(
                            title = "Distribuição por Período do Dia",
                            icon = "☀️",
                            subtitle = "Emprego operacional por faixa horária"
                        )
                        FireCard {
                            val maxPeriodCount = state.ocorrenciasPorPeriodoDia.values.maxOrNull() ?: 1
                            state.ocorrenciasPorPeriodoDia.forEach { (periodo, count) ->
                                HorizontalProgressItem(
                                    label = periodo,
                                    value = count,
                                    maxValue = maxPeriodCount,
                                    color = when (periodo) {
                                        "Madrugada" -> Color(0xFF3F51B5)
                                        "Manhã" -> Color(0xFFFFC107)
                                        "Tarde" -> Color(0xFFFF9800)
                                        else -> Color(0xFF212121)
                                    }
                                )
                            }
                        }
                    }

                    if (state.ocorrenciasPorBairro.isNotEmpty()) {
                        item {
                            FireSectionHeader(
                                title = "Ocorrências por Bairro",
                                icon = "🏘️",
                                subtitle = "Locais de maior atividade"
                            )
                            FireCard {
                                val maxBairro = state.ocorrenciasPorBairro.values.maxOrNull() ?: 1
                                state.ocorrenciasPorBairro.toList().take(5).forEach { (bairro, count) ->
                                    HorizontalProgressItem(
                                        label = bairro.uppercase(),
                                        value = count,
                                        maxValue = maxBairro,
                                        color = FireColors.Secondary
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // TAB 1: RECURSOS
                    item {
                        FireSectionHeader(
                            title = "Pessoas Envolvidas",
                            icon = "👥",
                            subtitle = "Perfil de idade das vítimas"
                        )
                        FireCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                            ) {
                                MetricValueBox("Vítimas", state.totalVitimas.toString(), FireColors.Primary, Modifier.weight(1f))
                                MetricValueBox("Envolvidos", state.totalEnvolvidos.toString(), FireColors.Secondary, Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(FireSpacing.Medium))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                            ) {
                                CategoryMiniBox("👶 Crianças", state.vitimasCriancas.toString(), Modifier.weight(1f))
                                CategoryMiniBox("🧑 Adultos", state.vitimasAdultos.toString(), Modifier.weight(1f))
                                CategoryMiniBox("👴 Idosos", state.vitimasIdosos.toString(), Modifier.weight(1f))
                            }
                        }
                    }

                    item {
                        FireSectionHeader(
                            title = "Frota Civil Envolvida",
                            icon = "🚗",
                            subtitle = "Veículos registrados em ocorrência"
                        )
                        FireCard {
                            Text(
                                text = "Total de Veículos: ${state.totalVeiculosEnvolvidos}",
                                style = FireTypography.BodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = FireColors.Primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                VehicleIconColumn("🚗", "Carro", state.veiculosCarros, Modifier.weight(1f))
                                VehicleIconColumn("🏍️", "Moto", state.veiculosMotos, Modifier.weight(1f))
                                VehicleIconColumn("🚛", "Caminhão", state.veiculosCaminhoes, Modifier.weight(1f))
                                VehicleIconColumn("🚌", "Ônibus", state.veiculosOnibus, Modifier.weight(1f))
                            }
                        }
                    }

                    item {
                        FireSectionHeader(
                            title = "Apoio Órgãos Externos",
                            icon = "🤝",
                            subtitle = "Solicitação de cooperação em campo"
                        )
                        FireCard {
                            if (state.apoioOrgaosContagem.isNotEmpty()) {
                                state.apoioOrgaosContagem.forEach { (orgao, count) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier.size(8.dp).background(FireColors.Primary, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(orgao, style = FireTypography.BodyMedium, fontWeight = FontWeight.Bold)
                                        }
                                        Badge(
                                            containerColor = FireColors.Primary.copy(alpha = 0.1f),
                                            contentColor = FireColors.Primary
                                        ) {
                                            Text("$count acionamentos", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = FireTypography.LabelSmall)
                                        }
                                    }
                                }
                            } else {
                                Text("Nenhum acionamento externo registrado.", style = FireTypography.BodyMedium, color = FireColors.OnSurfaceVariant)
                            }
                        }
                    }

                    item {
                        FireSectionHeader(
                            title = "Estatísticas de Atendimento",
                            icon = "⏱️",
                            subtitle = "Média de engajamento operacional"
                        )
                        FireCard {
                            AverageRowItem("Média de pessoas / ocorrência", String.format("%.2f", state.mediaPessoasPorOcorrencia))
                            AverageRowItem("Média de veículos / ocorrência", String.format("%.2f", state.mediaVeiculosPorOcorrencia))
                            AverageRowItem("Média de apoios / ocorrência", String.format("%.2f", state.mediaApoiosPorOcorrencia))
                        }
                    }
                }
                2 -> {
                    // TAB 2: LOGÍSTICA
                    item {
                        FireSectionHeader(
                            title = "Emprego de Viaturas (Ativas)",
                            icon = "🚒",
                            subtitle = "Viaturas mais empregadas nas ações"
                        )
                        FireCard {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Viatura Mais Empregada", style = FireTypography.BodyMedium)
                                Text(
                                    text = state.viaturaMaisEmpregada,
                                    style = FireTypography.BodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = FireColors.Primary
                                )
                            }
                            Divider()
                            Spacer(modifier = Modifier.height(8.dp))
                            if (state.viaturaRanking.isNotEmpty()) {
                                val maxVCount = state.viaturaRanking.maxOfOrNull { it.count } ?: 1
                                state.viaturaRanking.take(5).forEachIndexed { idx, viatura ->
                                    HorizontalBarItem(
                                        rank = idx + 1,
                                        label = viatura.prefixo,
                                        value = viatura.count,
                                        maxValue = maxVCount,
                                        detail = "${viatura.kmPercorrida} km",
                                        color = FireColors.Primary
                                    )
                                }
                            } else {
                                Text("Nenhuma viatura registrada no período.", style = FireTypography.BodyMedium, color = FireColors.OnSurfaceVariant)
                            }
                        }
                    }

                    item {
                        FireSectionHeader(
                            title = "Militares Empregados",
                            icon = "👨‍Simple",
                            subtitle = "Militares com maior número de chamados"
                        )
                        FireCard {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Militar mais acionado", style = FireTypography.BodyMedium)
                                Text(
                                    text = state.militarMaisEmpregado,
                                    style = FireTypography.BodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = FireColors.Secondary
                                )
                            }
                            Divider()
                            Spacer(modifier = Modifier.height(8.dp))
                            if (state.militarRanking.isNotEmpty()) {
                                state.militarRanking.take(5).forEachIndexed { idx, mil ->
                                    RankingItemWithBadge(
                                        rank = idx + 1,
                                        primaryText = mil.nomeGuerra,
                                        secondaryText = "Atendeu ${mil.count} ocorrências",
                                        value = mil.count,
                                        color = FireColors.Secondary
                                    )
                                }
                            } else {
                                Text("Nenhum militar registrado no período.", style = FireTypography.BodyMedium, color = FireColors.OnSurfaceVariant)
                            }
                        }
                    }

                    item {
                        FireSectionHeader(
                            title = "Emprego e Produtividade Operacional",
                            icon = "⏳",
                            subtitle = "Horas dedicadas a atendimentos"
                        )
                        FireCard {
                            AverageRowItem("Horas de empenho das viaturas", "${String.format("%.1f", state.horasEmpenhoViaturas)}h")
                            AverageRowItem("Horas de empenho do efetivo", "${String.format("%.1f", state.horasEmpenhoEfetivo)}h")
                            AverageRowItem("Média militares / ocorrência", String.format("%.2f", state.mediaMilitaresPorOcorrencia))
                        }
                    }
                }
                3 -> {
                    // TAB 3: GEOGRAFIA
                    item {
                        FireSectionHeader(
                            title = "Simulação do Mapa de Calor",
                            icon = "📍",
                            subtitle = "Hotspots de acidentes georeferenciados"
                        )
                        HeatmapSimulation()
                    }

                    item {
                        FireSectionHeader(
                            title = "Municípios com Maior Atividade",
                            icon = "🏢",
                            subtitle = "Municípios integrados"
                        )
                        FireCard {
                            if (state.municipioRanking.isNotEmpty()) {
                                state.municipioRanking.forEachIndexed { idx, mun ->
                                    RankingItemWithBadge(
                                        rank = idx + 1,
                                        primaryText = mun.nome,
                                        secondaryText = "Chamados consolidados",
                                        value = mun.count,
                                        color = FireColors.Primary
                                    )
                                }
                            } else {
                                Text("Sem dados geográficos municipais salvos.", style = FireTypography.BodyMedium, color = FireColors.OnSurfaceVariant)
                            }
                        }
                    }
                }
                4 -> {
                    // TAB 4: QUALIDADE
                    item {
                        FireSectionHeader(
                            title = "Integridade dos Registros",
                            icon = "📋",
                            subtitle = "Relação de ocorrências finalizadas e pendentes"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                            ) {
                                Column(modifier = Modifier.padding(FireSpacing.Medium)) {
                                    Text("Reg. Completos", style = FireTypography.LabelSmall, color = Color(0xFF2E7D32))
                                    Text(state.ocorrenciasCompletas.toString(), style = FireTypography.Headline, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                    Text("Prontos para envio", style = FireTypography.Caption, color = Color(0xFF2E7D32).copy(alpha = 0.8f))
                                }
                            }
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                            ) {
                                Column(modifier = Modifier.padding(FireSpacing.Medium)) {
                                    Text("Reg. Incompletos", style = FireTypography.LabelSmall, color = Color(0xFFC62828))
                                    Text(state.ocorrenciasIncompletas.toString(), style = FireTypography.Headline, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                    Text("Faltam dados", style = FireTypography.Caption, color = Color(0xFFC62828).copy(alpha = 0.8f))
                                }
                            }
                        }
                    }

                    item {
                        FireSectionHeader(
                            title = "Percentual de Preenchimento de Campos",
                            icon = "✅",
                            subtitle = "Métricas de qualidade antes da transcrição oficial"
                        )
                        FireCard {
                            HorizontalQualityProgress("GPS Informado", state.percentualGps, Color(0xFF1976D2))
                            HorizontalQualityProgress("Endereço Confirmado", state.percentualEndereco, Color(0xFF00897B))
                            HorizontalQualityProgress("Histórico Preenchido", state.percentualHistorico, Color(0xFF43A047))
                            HorizontalQualityProgress("Veículos Cadastrados", state.percentualVeiculos, Color(0xFF8D6E63))
                            HorizontalQualityProgress("Pessoas Cadastradas", state.percentualPessoas, Color(0xFF8E24AA))
                            HorizontalQualityProgress("Documentos Cadastrados", state.percentualDocumentos, Color(0xFF3949AB))
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

@Composable
private fun TopKpiMiniCard(title: String, value: String, color: Color) {
    Card(
        modifier = Modifier.width(130.dp).height(74.dp),
        colors = CardDefaults.cardColors(containerColor = FireColors.Surface),
        border = BorderStroke(1.dp, color.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = FireTypography.LabelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = FireColors.OnSurfaceVariant)
            Text(value, style = FireTypography.Title, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun CategoryMiniBox(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = FireColors.SurfaceVariant.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = FireTypography.LabelSmall, color = FireColors.OnSurfaceVariant)
            Text(value, style = FireTypography.Title, fontWeight = FontWeight.Bold, color = FireColors.OnBackground)
        }
    }
}

@Composable
private fun VehicleIconColumn(icon: String, name: String, count: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(44.dp).background(FireColors.Primary.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(name, style = FireTypography.LabelSmall, color = FireColors.OnSurfaceVariant)
        Text(count.toString(), style = FireTypography.BodyMedium, fontWeight = FontWeight.Bold, color = FireColors.OnBackground)
    }
}

@Composable
private fun AverageRowItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = FireTypography.BodyMedium, color = FireColors.OnBackground)
        Text(value, style = FireTypography.BodyMedium, fontWeight = FontWeight.ExtraBold, color = FireColors.Primary)
    }
}

@Composable
private fun MetricValueBox(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.06f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(FireSpacing.Medium)) {
            Text(title, style = FireTypography.LabelSmall, color = color)
            Text(value, style = FireTypography.Headline, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun HorizontalProgressItem(label: String, value: Int, maxValue: Int, color: Color) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (maxValue > 0) value.toFloat() / maxValue else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = FireTypography.BodyMedium, fontWeight = FontWeight.Medium)
            Text("$value ocorrências", style = FireTypography.BodyMedium, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(FireColors.SurfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
private fun HorizontalQualityProgress(label: String, percentage: Int, color: Color) {
    val animatedProgress by animateFloatAsState(
        targetValue = percentage.toFloat() / 100f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "quality_progress"
    )

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = FireTypography.BodyMedium, fontWeight = FontWeight.Medium)
            Text("$percentage%", style = FireTypography.BodyMedium, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(FireColors.SurfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
private fun EvolutionChart(data: Map<String, Int>, modifier: Modifier = Modifier) {
    val values = data.values.toList()
    val labels = data.keys.toList()
    if (values.isEmpty()) return

    val maxVal = values.maxOrNull() ?: 1
    val minVal = 0

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val spacing = 24.dp.toPx()
        
        val chartWidth = width - spacing * 2
        val chartHeight = height - spacing * 2
        
        val stepX = chartWidth / (values.size - 1).coerceAtLeast(1)
        val stepY = chartHeight / (maxVal - minVal).coerceAtLeast(1)

        val path = Path()
        val fillPath = Path()

        values.forEachIndexed { index, valItem ->
            val x = spacing + index * stepX
            val y = height - spacing - (valItem - minVal) * stepY

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height - spacing)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            if (index == values.size - 1) {
                fillPath.lineTo(x, height - spacing)
                fillPath.close()
            }
        }

        // Desenhar gradiente sob a linha
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    FireColors.Primary.copy(alpha = 0.25f),
                    FireColors.Primary.copy(alpha = 0.0f)
                ),
                startY = spacing,
                endY = height - spacing
            )
        )

        // Desenhar linha
        drawPath(
            path = path,
            color = FireColors.Primary,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Desenhar círculos dos pontos
        values.forEachIndexed { index, valItem ->
            val x = spacing + index * stepX
            val y = height - spacing - (valItem - minVal) * stepY
            drawCircle(
                color = FireColors.Primary,
                radius = 5.dp.toPx(),
                center = Offset(x, y)
            )
            drawCircle(
                color = Color.White,
                radius = 2.5.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun HeatmapSimulation(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFECEFF1))
            .border(1.dp, Color.LightGray.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Desenhar linhas da grade do mapa
            val gridStep = 40.dp.toPx()
            for (x in 0..(width / gridStep).toInt()) {
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.4f),
                    start = Offset(x * gridStep, 0f),
                    end = Offset(x * gridStep, height),
                    strokeWidth = 1f
                )
            }
            for (y in 0..(height / gridStep).toInt()) {
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.4f),
                    start = Offset(0f, y * gridStep),
                    end = Offset(width, y * gridStep),
                    strokeWidth = 1f
                )
            }

            // Círculos radiais de calor
            val heatPoints = listOf(
                Triple(Offset(width * 0.35f, height * 0.45f), 70.dp.toPx(), Color.Red),
                Triple(Offset(width * 0.68f, height * 0.58f), 65.dp.toPx(), Color(0xFFFF9100)),
                Triple(Offset(width * 0.5f, height * 0.22f), 45.dp.toPx(), Color.Yellow),
                Triple(Offset(width * 0.8f, height * 0.35f), 40.dp.toPx(), Color.Yellow)
            )

            heatPoints.forEach { (center, radius, color) ->
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = 0.6f),
                            color.copy(alpha = 0.2f),
                            color.copy(alpha = 0.0f)
                        ),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "🔥 SIMULAÇÃO DE MAPA DE CALOR",
                style = FireTypography.LabelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Maior concentração na região centro-sul",
                style = FireTypography.LabelSmall,
                color = Color.LightGray,
                fontSize = 10.sp
            )
        }
    }
}

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
                title = "Total Período",
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
                    NaturezaOcorrencia.INDEFINIDA -> Color.Gray
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

                    // Central Text
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

                // Legend
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
            var startAngle = -90f
            val strokeWidth = 32.dp.toPx()

            data.forEachIndexed { index, item ->
                val sweepAngle = (item.value.toFloat() / total) * 360f * animatedProgress
                val isSelected = selectedIndex == index

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