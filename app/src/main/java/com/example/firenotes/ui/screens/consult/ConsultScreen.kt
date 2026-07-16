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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
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
import com.example.firenotes.ui.designsystem.components.inputs.FireDatePicker

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
    val context = LocalContext.current
    var showFilterSheet by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader ->
                    val jsonStr = reader.readText()
                    viewModel.importOccurrenceFromJson(
                        jsonStr = jsonStr,
                        onSuccess = {
                            Toast.makeText(context, "Ocorrência importada com sucesso!", Toast.LENGTH_SHORT).show()
                            viewModel.loadOccurrences()
                        },
                        onError = { err ->
                            Toast.makeText(context, "Erro na importação: $err", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            } catch (e: java.lang.Exception) {
                Toast.makeText(context, "Erro ao ler arquivo: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

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
                    FireIconButton(
                        icon = FireIcons.CloudDownload,
                        onClick = {
                            importLauncher.launch("application/json")
                        }
                    )
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
                    AssistChip(
                        onClick = { sortExpanded = true },
                        label = {
                            Text(
                                text = when (uiState.sortBy) {
                                    ConsultSort.RECENTES -> "Mais Recentes"
                                    ConsultSort.ANTIGAS -> "Mais Antigas"
                                    ConsultSort.TALAO -> "Talão"
                                    ConsultSort.VITIMAS -> "Vítimas"
                                    ConsultSort.VEICULOS -> "Veículos"
                                },
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = when (uiState.sortBy) {
                                    ConsultSort.RECENTES -> FireIcons.Schedule
                                    ConsultSort.ANTIGAS -> FireIcons.Schedule
                                    ConsultSort.TALAO -> FireIcons.Numbers
                                    ConsultSort.VITIMAS -> FireIcons.People
                                    ConsultSort.VEICULOS -> FireIcons.DirectionsCar
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = FireColors.Primary
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = FireIcons.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            FireColors.OnSurfaceVariant.copy(alpha = 0.2f)
                        ),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color.Transparent,
                            labelColor = FireColors.OnBackground
                        )
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
            occurrences = uiState.occurrences,
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
    occurrences: List<Ocorrencia>,
    onDismiss: () -> Unit,
    onApply: (ConsultFilters) -> Unit,
    onClear: () -> Unit
) {
    var dataFiltro by remember { mutableStateOf(currentFilters.dataFiltro) }
    var cidade by remember { mutableStateOf(currentFilters.cidade) }
    var bairro by remember { mutableStateOf(currentFilters.bairro) }
    var viatura by remember { mutableStateOf(currentFilters.viatura) }
    var militar by remember { mutableStateOf(currentFilters.militar) }
    var placa by remember { mutableStateOf(currentFilters.placa) }
    var envolvido by remember { mutableStateOf(currentFilters.envolvido) }
    var selectedNature by remember { mutableStateOf(currentFilters.natureza) }
    var selectedStatus by remember { mutableStateOf(currentFilters.status) }

    // Limpa o bairro se a cidade for alterada
    LaunchedEffect(cidade) {
        if (cidade.isBlank()) {
            bairro = ""
        }
    }

    val naturezasCadastradas = remember(occurrences) {
        occurrences.map { it.natureza }.distinct()
    }
    val cidadesCadastradas = remember(occurrences) {
        occurrences.mapNotNull { it.cidade }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val bairrosCadastrados = remember(occurrences, cidade) {
        if (cidade.isBlank()) emptyList()
        else occurrences.filter { it.cidade?.equals(cidade, ignoreCase = true) == true }
            .mapNotNull { it.bairro }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val viaturasCadastradas = remember(occurrences) {
        occurrences.flatMap { it.viaturas }.map { it.prefixo }.distinct().sorted()
    }
    val placasCadastradas = remember(occurrences) {
        occurrences.flatMap { it.veiculos }.mapNotNull { it.placa }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val activeFiltersCount = listOf(
        dataFiltro, cidade, bairro, viatura, militar, placa, envolvido, selectedNature?.let { "nature" }, selectedStatus
    ).count { it != null && it != "" }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFAFAFA), // Fundo em tons de branco
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                // Cabeçalho: Título e Botão Cancelar (X) no canto superior direito
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔍 Filtros Avançados",
                        style = FireTypography.Title,
                        fontWeight = FontWeight.Bold,
                        color = FireColors.OnBackground
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = FireIcons.Close,
                            contentDescription = "Fechar",
                            tint = FireColors.OnSurfaceVariant
                        )
                    }
                }
                
                Divider(
                    color = FireColors.OnSurfaceVariant.copy(alpha = 0.1f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                // Filtros (Corpo)
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
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

                    FireDatePicker(
                        value = dataFiltro,
                        onDateSelected = { dataFiltro = it },
                        label = "📅 Data da Ocorrência"
                    )

                    NatureDropdownSelect(
                        label = "🏷️ Natureza",
                        selectedValue = selectedNature,
                        options = naturezasCadastradas,
                        onValueChange = { selectedNature = it }
                    )

                    FilterDropdownSelect(
                        label = "📍 Cidade",
                        selectedValue = cidade,
                        options = cidadesCadastradas,
                        onValueChange = {
                            cidade = it
                            bairro = "" // Reset bairro ao mudar cidade
                        }
                    )

                    if (cidade.isNotBlank()) {
                        FilterDropdownSelect(
                            label = "🏘️ Bairro",
                            selectedValue = bairro,
                            options = bairrosCadastrados,
                            onValueChange = { bairro = it }
                        )
                    }

                    FilterDropdownSelect(
                        label = "🚒 Prefixo Viatura",
                        selectedValue = viatura,
                        options = viaturasCadastradas,
                        onValueChange = { viatura = it }
                    )

                    FireOutlinedTextField(
                        value = militar,
                        onValueChange = { militar = it },
                        label = "👨‍🚒 RE do Militar",
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    FilterDropdownSelect(
                        label = "🚗 Placa Veículo",
                        selectedValue = placa,
                        options = placasCadastradas,
                        onValueChange = { placa = it }
                    )

                    FireOutlinedTextField(
                        value = envolvido,
                        onValueChange = { envolvido = it },
                        label = "👤 Envolvido (Nome ou CPF)",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Rodapé: Botões de ação unificados em azul
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            dataFiltro = ""
                            cidade = ""
                            bairro = ""
                            viatura = ""
                            militar = ""
                            placa = ""
                            envolvido = ""
                            selectedNature = null
                            selectedStatus = ""
                        },
                        border = androidx.compose.foundation.BorderStroke(1.dp, FireColors.Primary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FireColors.Primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = FireIcons.Refresh, contentDescription = "Limpar", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Limpar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            onApply(
                                ConsultFilters(
                                    dataFiltro = dataFiltro,
                                    cidade = cidade,
                                    bairro = bairro,
                                    viatura = viatura,
                                    militar = militar,
                                    placa = placa,
                                    envolvido = envolvido,
                                    natureza = selectedNature,
                                    status = selectedStatus
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FireColors.Primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = FireIcons.Check, contentDescription = "Aplicar", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Aplicar ($activeFiltersCount)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDropdownSelect(
    label: String,
    selectedValue: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = label, style = FireTypography.Label, fontWeight = FontWeight.Bold, color = FireColors.OnSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                readOnly = true,
                value = selectedValue.ifBlank { "Todas" },
                onValueChange = {},
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FireColors.Primary,
                    unfocusedBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.4f),
                    focusedLabelColor = FireColors.Primary,
                    focusedTextColor = FireColors.OnBackground,
                    unfocusedTextColor = FireColors.OnBackground,
                    focusedContainerColor = FireColors.Surface,
                    unfocusedContainerColor = FireColors.Surface
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(FireColors.Surface)
            ) {
                DropdownMenuItem(
                    text = { Text("Todas / Limpar", style = FireTypography.Body) },
                    onClick = { onValueChange(""); expanded = false }
                )
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, style = FireTypography.Body) },
                        onClick = { onValueChange(option); expanded = false }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NatureDropdownSelect(
    label: String,
    selectedValue: NaturezaOcorrencia?,
    options: List<NaturezaOcorrencia>,
    onValueChange: (NaturezaOcorrencia?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = label, style = FireTypography.Label, fontWeight = FontWeight.Bold, color = FireColors.OnSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                readOnly = true,
                value = selectedValue?.descricao ?: "Todas",
                onValueChange = {},
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FireColors.Primary,
                    unfocusedBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.4f),
                    focusedLabelColor = FireColors.Primary,
                    focusedTextColor = FireColors.OnBackground,
                    unfocusedTextColor = FireColors.OnBackground,
                    focusedContainerColor = FireColors.Surface,
                    unfocusedContainerColor = FireColors.Surface
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(FireColors.Surface)
            ) {
                DropdownMenuItem(
                    text = { Text("Todas / Limpar", style = FireTypography.Body) },
                    onClick = { onValueChange(null); expanded = false }
                )
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.descricao, style = FireTypography.Body) },
                        onClick = { onValueChange(option); expanded = false }
                    )
                }
            }
        }
    }
}

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
