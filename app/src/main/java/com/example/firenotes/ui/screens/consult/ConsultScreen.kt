package com.example.firenotes.ui.screens.consult

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.window.Dialog
import com.example.firenotes.domain.model.*
import com.example.firenotes.ui.designsystem.colors.FireColors
import com.example.firenotes.ui.designsystem.shapes.FireShapes
import com.example.firenotes.ui.designsystem.spacing.FireSpacing
import com.example.firenotes.ui.designsystem.typography.FireTypography
import com.example.firenotes.ui.designsystem.icons.FireIcons
import com.example.firenotes.ui.designsystem.components.buttons.*
import com.example.firenotes.ui.designsystem.components.cards.FireCard
import com.example.firenotes.ui.designsystem.components.inputs.FireSearchBar
import com.example.firenotes.ui.designsystem.components.inputs.FireOutlinedTextField
import com.example.firenotes.ui.designsystem.components.dialogs.FireDialog
import com.example.firenotes.ui.designsystem.components.topbar.FireTopBar
import com.example.firenotes.ui.designsystem.components.widgets.FireStatusChip
import com.example.firenotes.ui.designsystem.components.widgets.FireDivider
import com.example.firenotes.ui.designsystem.states.FireLoading
import com.example.firenotes.ui.designsystem.states.FireEmptyState
import com.example.firenotes.ui.designsystem.states.FireErrorState
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ============================================
// LOGS PADRONIZADOS
// ============================================

private const val LOG_TAG = "FireConsult"
private fun logD(message: String) = android.util.Log.d(LOG_TAG, message)
private fun logE(message: String, throwable: Throwable? = null) =
    android.util.Log.e(LOG_TAG, message, throwable)

/* STREAMING_CHUNK: Initializing ConsultScreen with scaffold and reactive filtering states... */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsultScreen(
    viewModel: ConsultViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }

    // Check if there are active filters
    val hasActiveFilters = uiState.filters.talao.isNotBlank() ||
            uiState.filters.cidade.isNotBlank() ||
            uiState.filters.bairro.isNotBlank() ||
            uiState.filters.viatura.isNotBlank() ||
            uiState.filters.militar.isNotBlank() ||
            uiState.filters.placa.isNotBlank() ||
            uiState.filters.nome.isNotBlank() ||
            uiState.filters.hospital.isNotBlank() ||
            uiState.filters.natureza != null ||
            uiState.filters.status.isNotBlank()

    Scaffold(
        topBar = {
            FireTopBar(
                title = "🔍 Consultar Ocorrências",
                onBackClick = onNavigateBack,
                backgroundColor = FireColors.Surface,
                elevation = 2.dp,
                actions = {
                    // Botão de filtros com badge de notificação de filtros ativos
                    BadgedBox(
                        badge = {
                            if (hasActiveFilters) {
                                Badge(
                                    containerColor = FireColors.Primary,
                                    contentColor = Color.White
                                ) {
                                    Text("!", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    ) {
                        FireIconButton(
                            icon = FireIcons.FilterList,
                            onClick = {
                                logD("User clicked filters button")
                                showFilterSheet = true
                            }
                        )
                    }
                    FireIconButton(
                        icon = FireIcons.Refresh,
                        onClick = {
                            logD("User clicked refresh button")
                            viewModel.loadOccurrences()
                        }
                    )
                }
            )
        },
        containerColor = FireColors.Background,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(FireColors.Background)
                .padding(FireSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
        ) {

            /* STREAMING_CHUNK: Rendering search bar and global query components... */
            // Global search bar with visual elevations
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(400)) + slideInVertically(initialOffsetY = { -it / 2 })
            ) {
                FireSearchBar(
                    value = uiState.searchGlobal,
                    onValueChange = viewModel::updateSearchGlobal,
                    placeholder = "🔎 Buscar por Talão, Placa, Nome, Viatura...",
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(12.dp))
                )
            }

            if (uiState.searchGlobal.isNotBlank()) {
                Text(
                    text = "${uiState.filteredOccurrences.size} resultados encontrados",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = FireColors.OnSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            /* STREAMING_CHUNK: Building list sorting option selections... */
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ordenar por:",
                    style = FireTypography.Label,
                    fontWeight = FontWeight.Bold,
                    color = FireColors.OnSurfaceVariant
                )

                var sortExpanded by remember { mutableStateOf(false) }
                Box {
                    FireOutlinedButton(
                        text = when (uiState.sortBy) {
                            ConsultSort.RECENTES -> "📅 Mais Recentes"
                            ConsultSort.ANTIGAS -> "📅 Mais Antigas"
                            ConsultSort.TALAO -> "🔢 Talão"
                            ConsultSort.VITIMAS -> "👥 Vítimas"
                            ConsultSort.VEICULOS -> "🚗 Veículos"
                        },
                        onClick = { sortExpanded = true }
                    )
                    DropdownMenu(
                        expanded = sortExpanded,
                        onDismissRequest = { sortExpanded = false },
                        modifier = Modifier.background(FireColors.Surface)
                    ) {
                        ConsultSort.values().forEach { sort ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = when (sort) {
                                                ConsultSort.RECENTES -> FireIcons.Schedule
                                                ConsultSort.ANTIGAS -> FireIcons.Schedule
                                                ConsultSort.TALAO -> FireIcons.Numbers
                                                ConsultSort.VITIMAS -> FireIcons.People
                                                ConsultSort.VEICULOS -> FireIcons.DirectionsCar
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (uiState.sortBy == sort) FireColors.Primary else FireColors.OnSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = when (sort) {
                                                ConsultSort.RECENTES -> "Mais Recentes"
                                                ConsultSort.ANTIGAS -> "Mais Antigas"
                                                ConsultSort.TALAO -> "Nº Talão"
                                                ConsultSort.VITIMAS -> "Qtd Vítimas"
                                                ConsultSort.VEICULOS -> "Qtd Veículos"
                                            },
                                            color = if (uiState.sortBy == sort) FireColors.Primary else FireColors.OnBackground
                                        )
                                    }
                                },
                                onClick = {
                                    logD("User sorted list by $sort")
                                    viewModel.updateSort(sort)
                                    sortExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            /* STREAMING_CHUNK: Handling list states and rendering the dynamic occurrences list... */
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        FireLoading()
                    }
                }
                uiState.errorMessage != null -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        FireErrorState(
                            message = uiState.errorMessage!!,
                            onRetry = { viewModel.loadOccurrences() }
                        )
                    }
                }
                uiState.filteredOccurrences.isEmpty() -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        FireEmptyState(
                            message = if (uiState.searchGlobal.isNotBlank() || hasActiveFilters) {
                                "Nenhuma ocorrência encontrada com os filtros aplicados"
                            } else {
                                "Nenhuma ocorrência registrada"
                            }
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                    ) {
                        items(
                            items = uiState.filteredOccurrences,
                            key = { it.id ?: it.protocolo }
                        ) { ocorrencia ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(animationSpec = tween(300)) + slideInVertically(initialOffsetY = { 30 })
                            ) {
                                ConsultCard(
                                    ocorrencia = ocorrencia,
                                    onClick = {
                                        logD("User selected occurrence: ${ocorrencia.id}")
                                        onNavigateToDetails(ocorrencia.id!!)
                                    }
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Exibindo ${uiState.filteredOccurrences.size} ocorrências",
                                fontSize = 12.sp,
                                color = FireColors.OnSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        FiltersDialog(
            currentFilters = uiState.filters,
            onDismiss = { showFilterSheet = false },
            onApply = { filters ->
                logD("Applying filters: $filters")
                viewModel.updateFilters(filters)
                showFilterSheet = false
            },
            onClear = {
                logD("Clearing filters")
                viewModel.updateFilters(ConsultFilters())
                showFilterSheet = false
            }
        )
    }


}

/* STREAMING_CHUNK: Designing ConsultCard with dynamic turn status... */
@Composable
fun ConsultCard(
    ocorrencia: Ocorrencia,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = remember {
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault())
    }

    val natureColor = when (ocorrencia.natureza) {
        NaturezaOcorrencia.INCENDIO -> FireColors.NaturezaIncendio
        NaturezaOcorrencia.SALVAMENTO -> FireColors.NaturezaSalvamento
        NaturezaOcorrencia.ACIDENTE_TRANSITO -> FireColors.NaturezaAcidente
        NaturezaOcorrencia.QUEDA -> FireColors.NaturezaQueda
        NaturezaOcorrencia.PESSOAL -> FireColors.NaturezaPessoal
        NaturezaOcorrencia.INDEFINIDA -> androidx.compose.ui.graphics.Color.Gray
    }

    // Cálculo retroativo dinâmico do turno/prontidão com base no momento da ocorrência
    val localDate = remember(ocorrencia.dataHora) {
        java.time.LocalDate.ofInstant(ocorrencia.dataHora, java.time.ZoneId.systemDefault())
    }

    val prontidao = remember(localDate) {
        try {
            com.example.firenotes.data.service.ProntidaoService.getProntidaoForDate(localDate)
        } catch (e: Exception) {
            null
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = FireColors.Surface
        )
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Barra lateral colorida da natureza do chamado
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(natureColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(FireSpacing.Medium),
                verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
            ) {
                // Cabeçalho: Talão e Data/Prontidão
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "📋 ${ocorrencia.protocolo}",
                            style = FireTypography.Title,
                            fontWeight = FontWeight.Bold,
                            color = FireColors.OnBackground
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FireStatusChip(
                            text = "ATIVO",
                            backgroundColor = FireColors.Success.copy(alpha = 0.12f),
                            textColor = FireColors.Success
                        )
                    }

                    /* STREAMING_CHUNK: Rendering the Date and active Prontidao layout... */
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = formatter.format(ocorrencia.dataHora),
                            style = FireTypography.Label,
                            fontWeight = FontWeight.SemiBold,
                            color = FireColors.OnSurfaceVariant
                        )

                        // Badge dinâmico do turno/prontidão associada
                        if (prontidao != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(prontidao.cor.copy(alpha = 0.15f))
                                    .border(0.5.dp, prontidao.cor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .background(prontidao.cor, CircleShape)
                                    )
                                    Text(
                                        text = prontidao.nome.uppercase(),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (prontidao.cor == Color(0xFFFFB300)) Color(0xFF6B4C00) else prontidao.cor,
                                        letterSpacing = 0.3.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Natureza e Local
                Row(
                    horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = natureColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = ocorrencia.natureza.descricao,
                            color = natureColor,
                            style = FireTypography.Caption,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "📍 ${ocorrencia.cidade}/${ocorrencia.uf}",
                        style = FireTypography.Body,
                        color = FireColors.OnSurfaceVariant
                    )
                }

                // Estatísticas rápidas em pills
                Row(
                    horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatPill(
                        icon = "👥",
                        value = ocorrencia.vitimas.size,
                        label = "Vítimas"
                    )
                    StatPill(
                        icon = "🚗",
                        value = ocorrencia.veiculos.size,
                        label = "Veículos"
                    )
                    StatPill(
                        icon = "🚒",
                        value = ocorrencia.viaturas.size,
                        label = "Viaturas"
                    )
                }

                /* STREAMING_CHUNK: Displaying active viaturas badges inside ConsultCard... */
                // Tags rápidas de viaturas
                if (ocorrencia.viaturas.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ocorrencia.viaturas.take(3).forEach { viatura ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = FireColors.Primary.copy(alpha = 0.08f)
                            ) {
                                Text(
                                    text = "🚒 ${viatura.prefixo}",
                                    fontSize = 10.sp,
                                    color = FireColors.Primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (ocorrencia.viaturas.size > 3) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = FireColors.OnSurface.copy(alpha = 0.05f)
                            ) {
                                Text(
                                    text = "+${ocorrencia.viaturas.size - 3}",
                                    fontSize = 10.sp,
                                    color = FireColors.OnSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Indicador de seta lateral para expansão de detalhes
            Icon(
                imageVector = FireIcons.ChevronRight,
                contentDescription = null,
                tint = FireColors.OnSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier
                    .padding(end = 12.dp)
                    .align(Alignment.CenterVertically)
                    .size(20.dp)
            )
        }
    }
}

// ============================================
// STAT PILL
// ============================================

@Composable
private fun StatPill(
    icon: String,
    value: Int,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(text = icon, fontSize = 12.sp)
        Text(
            text = value.toString(),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = FireColors.OnBackground
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = FireColors.OnSurfaceVariant
        )
    }
}

/* STREAMING_CHUNK: Rendering the advanced Filters Dialog... */
// ============================================
// FILTERS DIALOG
// ============================================

@Composable
fun FiltersDialog(
    currentFilters: ConsultFilters,
    onDismiss: () -> Unit,
    onApply: (ConsultFilters) -> Unit,
    onClear: () -> Unit
) {
    var talao by remember { mutableStateOf(currentFilters.talao) }
    var cidade by remember { mutableStateOf(currentFilters.cidade) }
    var bairro by remember { mutableStateOf(currentFilters.bairro) }
    var viatura by remember { mutableStateOf(currentFilters.viatura) }
    var militar by remember { mutableStateOf(currentFilters.militar) }
    var placa by remember { mutableStateOf(currentFilters.placa) }
    var nome by remember { mutableStateOf(currentFilters.nome) }
    var hospital by remember { mutableStateOf(currentFilters.hospital) }
    var selectedNature by remember { mutableStateOf(currentFilters.natureza) }
    var selectedStatus by remember { mutableStateOf(currentFilters.status) }

    val activeFiltersCount = listOf(
        talao, cidade, bairro, viatura, militar, placa, nome, hospital, selectedNature?.let { "nature" }
    ).count { it != null && it != "" }

    FireDialog(
        onDismissRequest = onDismiss,
        title = "🔍 Filtros Avançados",
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FireButton(
                        text = "Aplicar ($activeFiltersCount)",
                        onClick = {
                            onApply(
                                ConsultFilters(
                                    talao = talao,
                                    cidade = cidade,
                                    bairro = bairro,
                                    viatura = viatura,
                                    militar = militar,
                                    placa = placa,
                                    nome = nome,
                                    hospital = hospital,
                                    natureza = selectedNature,
                                    status = selectedStatus
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FireTextButton(
                        text = "Limpar",
                        onClick = onClear,
                        modifier = Modifier.weight(1f)
                    )
                }
                FireTextButton(
                    text = "Cancelar",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
        ) {
            Text("Status:", style = FireTypography.Label, fontWeight = FontWeight.Bold)
            Row(
                horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("Todas", "Aberta", "Encerrada").forEach { status ->
                    val isSelected = (status == "Todas" && selectedStatus.isEmpty()) || (selectedStatus == status)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedStatus = if (status == "Todas") "" else status
                        },
                        label = status
                    )
                }
            }

            Text("Natureza:", style = FireTypography.Label, fontWeight = FontWeight.Bold)
            Row(
                horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small),
                modifier = Modifier.fillMaxWidth()
            ) {
                var expanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    FireOutlinedButton(
                        text = selectedNature?.descricao ?: "Todas",
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(FireColors.Surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Todas", style = FireTypography.Body) },
                            onClick = { selectedNature = null; expanded = false }
                        )
                        NaturezaOcorrencia.values().forEach { n ->
                            DropdownMenuItem(
                                text = { Text(n.descricao, style = FireTypography.Body) },
                                onClick = { selectedNature = n; expanded = false }
                            )
                        }
                    }
                }
            }

            FireOutlinedTextField(
                value = talao,
                onValueChange = { talao = it },
                label = "📋 Talão",
                modifier = Modifier.fillMaxWidth()
            )

            FireOutlinedTextField(
                value = cidade,
                onValueChange = { cidade = it },
                label = "📍 Cidade",
                modifier = Modifier.fillMaxWidth()
            )

            FireOutlinedTextField(
                value = bairro,
                onValueChange = { bairro = it },
                label = "🏘️ Bairro",
                modifier = Modifier.fillMaxWidth()
            )

            FireOutlinedTextField(
                value = viatura,
                onValueChange = { viatura = it },
                label = "🚒 Prefixo Viatura",
                modifier = Modifier.fillMaxWidth()
            )

            FireOutlinedTextField(
                value = militar,
                onValueChange = { militar = it },
                label = "👨‍🚒 Militar (Nome/RE)",
                modifier = Modifier.fillMaxWidth()
            )

            FireOutlinedTextField(
                value = placa,
                onValueChange = { placa = it },
                label = "🚗 Placa Veículo",
                modifier = Modifier.fillMaxWidth()
            )

            FireOutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = "👤 Nome da Vítima",
                modifier = Modifier.fillMaxWidth()
            )

            FireOutlinedTextField(
                value = hospital,
                onValueChange = { hospital = it },
                label = "🏥 Hospital de Destino",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ============================================
// FILTER CHIP
// ============================================

@Composable
fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = if (selected) {
            FireColors.Primary.copy(alpha = 0.15f)
        } else {
            FireColors.SurfaceVariant.copy(alpha = 0.5f)
        },
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(1.dp, FireColors.Primary)
        } else {
            null
        }
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = if (selected) FireColors.Primary else FireColors.OnSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
