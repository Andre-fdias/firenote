package com.example.firenotes.ui.screens.consult

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.firenotes.domain.model.*
import com.example.firenotes.ui.designsystem.colors.FireColor
import com.example.firenotes.ui.designsystem.dimensions.FireShapes
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
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ConsultScreen(
    viewModel: ConsultViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDuplicate: (Ocorrencia) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            FireTopBar(
                title = "Consultar Ocorrências",
                onBackClick = onNavigateBack,
                actions = {
                    FireIconButton(icon = FireIcons.List, onClick = { showFilterSheet = true })
                    FireIconButton(icon = FireIcons.CheckCircle, onClick = { viewModel.loadOccurrences() })
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(FireSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
        ) {
            // Global Search field using Design System
            FireSearchBar(
                value = uiState.searchGlobal,
                onValueChange = viewModel::updateSearchGlobal,
                placeholder = "Busca global (Talão, Placa, Viatura...)"
            )

            // Sorting choice Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ordenação:", style = FireTypography.Title, fontWeight = FontWeight.Bold)
                var sortExpanded by remember { mutableStateOf(false) }
                Box {
                    FireOutlinedButton(
                        text = when (uiState.sortBy) {
                            ConsultSort.RECENTES -> "Mais Recentes"
                            ConsultSort.ANTIGAS -> "Mais Antigas"
                            ConsultSort.TALAO -> "Nº Talão"
                            ConsultSort.VITIMAS -> "Qtd Vítimas"
                            ConsultSort.VEICULOS -> "Qtd Veículos"
                        },
                        onClick = { sortExpanded = true }
                    )
                    DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Mais Recentes", style = FireTypography.Body) },
                            onClick = { viewModel.updateSort(ConsultSort.RECENTES); sortExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Mais Antigas", style = FireTypography.Body) },
                            onClick = { viewModel.updateSort(ConsultSort.ANTIGAS); sortExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Nº Talão", style = FireTypography.Body) },
                            onClick = { viewModel.updateSort(ConsultSort.TALAO); sortExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Qtd Vítimas", style = FireTypography.Body) },
                            onClick = { viewModel.updateSort(ConsultSort.VITIMAS); sortExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Qtd Veículos", style = FireTypography.Body) },
                            onClick = { viewModel.updateSort(ConsultSort.VEICULOS); sortExpanded = false }
                        )
                    }
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    FireLoading()
                }
            } else if (uiState.filteredOccurrences.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    FireEmptyState(message = "Nenhuma ocorrência atende aos filtros de busca.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                ) {
                    items(uiState.filteredOccurrences) { ocorrencia ->
                        ConsultCard(
                            ocorrencia = ocorrencia,
                            onClick = { viewModel.selectOccurrence(ocorrencia) },
                            onDuplicateClick = {
                                viewModel.duplicateOccurrence(ocorrencia, onNavigateToDuplicate)
                            }
                        )
                    }
                }
            }
        }
    }

    // Detail dialog
    if (uiState.showDetailsDialog && uiState.selectedOccurrence != null) {
        OcorrenciaDetailsDialog(
            ocorrencia = uiState.selectedOccurrence!!,
            onDismiss = viewModel::dismissDetails
        )
    }

    // Filters sheet dialog
    if (showFilterSheet) {
        FiltersDialog(
            currentFilters = uiState.filters,
            onDismiss = { showFilterSheet = false },
            onApply = { filters ->
                viewModel.updateFilters(filters)
                showFilterSheet = false
            }
        )
    }
}

@Composable
fun ConsultCard(
    ocorrencia: Ocorrencia,
    onClick: () -> Unit,
    onDuplicateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault()) }
    val natureColor = when (ocorrencia.natureza) {
        NaturezaOcorrencia.INCENDIO -> FireColor.Primary
        NaturezaOcorrencia.SALVAMENTO -> FireColor.Success
        NaturezaOcorrencia.ACIDENTE_TRANSITO -> FireColor.Warning
        NaturezaOcorrencia.QUEDA -> Color(0xFF8B5A2B)
        NaturezaOcorrencia.PESSOAL -> FireColor.SecondaryLight
    }

    FireCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .fillMaxHeight()
                    .background(natureColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(FireSpacing.Medium),
                verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Talão: ${ocorrencia.protocolo}", style = FireTypography.Title, fontWeight = FontWeight.Bold)
                    Text(text = formatter.format(ocorrencia.dataHora), style = FireTypography.Label, color = Color.Gray)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(natureColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = FireSpacing.Small, vertical = FireSpacing.ExtraSmall)
                    ) {
                        Text(ocorrencia.natureza.descricao, color = natureColor, style = FireTypography.Caption, fontWeight = FontWeight.Bold)
                    }
                    Text("📍 ${ocorrencia.cidade}/${ocorrencia.uf}", style = FireTypography.Body, color = Color.DarkGray)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium), modifier = Modifier.fillMaxWidth()) {
                    Text("👥 Vítimas: ${ocorrencia.vitimas.size}", style = FireTypography.Caption)
                    Text("🚗 Veículos: ${ocorrencia.veiculos.size}", style = FireTypography.Caption)
                    Text("🚒 Viaturas: ${ocorrencia.viaturas.size}", style = FireTypography.Caption)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FireStatusChip(
                        text = "ATIVO",
                        backgroundColor = Color(0xFFE8F5E9),
                        textColor = Color(0xFF2E7D32)
                    )
                    
                    FireTextButton(
                        text = "Duplicar",
                        onClick = onDuplicateClick
                    )
                }
            }
        }
    }
}

@Composable
fun FiltersDialog(
    currentFilters: ConsultFilters,
    onDismiss: () -> Unit,
    onApply: (ConsultFilters) -> Unit
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

    FireDialog(
        onDismissRequest = onDismiss,
        title = "Filtros Avançados",
        confirmButton = {
            FireButton(
                text = "Aplicar",
                onClick = {
                    onApply(
                        ConsultFilters(
                            talao = talao, cidade = cidade, bairro = bairro, viatura = viatura,
                            militar = militar, placa = placa, nome = nome, hospital = hospital,
                            natureza = selectedNature
                        )
                    )
                }
            )
        },
        dismissButton = {
            FireTextButton(text = "Cancelar", onClick = onDismiss)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
        ) {
            FireOutlinedTextField(value = talao, onValueChange = { talao = it }, label = "Talão")
            
            Text("Natureza:", style = FireTypography.Label, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small), modifier = Modifier.fillMaxWidth()) {
                var expanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    FireOutlinedButton(text = selectedNature?.descricao ?: "Todas", onClick = { expanded = true }, modifier = Modifier.fillMaxWidth())
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("Todas", style = FireTypography.Body) }, onClick = { selectedNature = null; expanded = false })
                        NaturezaOcorrencia.values().forEach { n ->
                            DropdownMenuItem(text = { Text(n.descricao, style = FireTypography.Body) }, onClick = { selectedNature = n; expanded = false })
                        }
                    }
                }
            }

            FireOutlinedTextField(value = cidade, onValueChange = { cidade = it }, label = "Cidade")
            FireOutlinedTextField(value = bairro, onValueChange = { bairro = it }, label = "Bairro")
            FireOutlinedTextField(value = viatura, onValueChange = { viatura = it }, label = "Prefixo Viatura")
            FireOutlinedTextField(value = militar, onValueChange = { militar = it }, label = "Militar (Nome/RE)")
            FireOutlinedTextField(value = placa, onValueChange = { placa = it }, label = "Placa Veículo")
            FireOutlinedTextField(value = nome, onValueChange = { nome = it }, label = "Nome da Vítima")
            FireOutlinedTextField(value = hospital, onValueChange = { hospital = it }, label = "Hospital de Destino")
        }
    }
}

@Composable
fun OcorrenciaDetailsDialog(
    ocorrencia: Ocorrencia,
    onDismiss: () -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault()) }
    
    FireDialog(
        onDismissRequest = onDismiss,
        title = "Ocorrência ${ocorrencia.protocolo}",
        confirmButton = {
            FireButton(text = "Fechar", onClick = onDismiss)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
        ) {
            Text("Geral", style = FireTypography.Title, fontWeight = FontWeight.Bold, color = FireColor.Primary)
            Text("Natureza: ${ocorrencia.natureza.descricao}", style = FireTypography.Body)
            Text("Data/Hora: ${formatter.format(ocorrencia.dataHora)}", style = FireTypography.Body)
            Text("Endereço: ${ocorrencia.rua}, ${ocorrencia.numero} - ${ocorrencia.bairro}, ${ocorrencia.cidade}/${ocorrencia.uf}", style = FireTypography.Body)
            if (ocorrencia.latitude != null) {
                Text("GPS: Lat ${ocorrencia.latitude} | Lng ${ocorrencia.longitude}", style = FireTypography.Body)
            }

            if (ocorrencia.viaturas.isNotEmpty()) {
                FireDivider()
                Text("Guarnição e Viaturas", style = FireTypography.Title, fontWeight = FontWeight.Bold, color = FireColor.Primary)
                ocorrencia.viaturas.forEach { v ->
                    Text("Viatura ${v.prefixo} (${v.tipo})", style = FireTypography.Body, fontWeight = FontWeight.SemiBold)
                    v.equipe.forEach { m ->
                        Text("  • RE ${m.re} - ${m.graduacao.descricao} ${m.nomeGuerra} [${m.funcao ?: "Equipe"}]", style = FireTypography.Body)
                    }
                }
            }

            if (ocorrencia.veiculos.isNotEmpty()) {
                FireDivider()
                Text("Veículos", style = FireTypography.Title, fontWeight = FontWeight.Bold, color = FireColor.Primary)
                ocorrencia.veiculos.forEach { veiculo ->
                    Text("🚗 Placa: ${veiculo.placa} | Modelo: ${veiculo.modelo} | Cor: ${veiculo.cor}", style = FireTypography.Body)
                }
            }

            if (ocorrencia.vitimas.isNotEmpty()) {
                FireDivider()
                Text("Vítimas e Socorro", style = FireTypography.Title, fontWeight = FontWeight.Bold, color = FireColor.Primary)
                ocorrencia.vitimas.forEach { vitima ->
                    Text("👤 Nome: ${vitima.nome ?: "Não Identificado"}", style = FireTypography.Body, fontWeight = FontWeight.SemiBold)
                    Text("  • Glasgow: ${vitima.sinaisVitais.escalaGCS ?: "N/D"} | P.A: ${vitima.sinaisVitais.pressaoArterial ?: "N/D"} | FC: ${vitima.sinaisVitais.pulso ?: "N/D"}", style = FireTypography.Body)
                    Text("  • Destino: ${vitima.hospitalDestino ?: "Não encaminhado"}", style = FireTypography.Body)
                }
            }

            ocorrencia.historico?.let { hist ->
                FireDivider()
                Text("Histórico Narrativo", style = FireTypography.Title, fontWeight = FontWeight.Bold, color = FireColor.Primary)
                Text(hist, style = FireTypography.Caption)
            }
        }
    }
}
