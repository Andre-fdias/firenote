package com.example.firenotes.ui.screens.consult

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.firenotes.domain.model.Ocorrencia
import com.example.firenotes.domain.model.Viatura
import com.example.firenotes.ui.designsystem.colors.FireColors
import com.example.firenotes.ui.designsystem.components.buttons.FireButton
import com.example.firenotes.ui.designsystem.components.buttons.FireIconButton
import com.example.firenotes.ui.designsystem.components.topbar.FireTopBar
import com.example.firenotes.ui.designsystem.components.widgets.FireStatusChip
import com.example.firenotes.ui.designsystem.icons.FireIcons
import com.example.firenotes.ui.designsystem.spacing.FireSpacing
import com.example.firenotes.ui.designsystem.typography.FireTypography
import com.example.firenotes.ui.screens.reports.ReportsViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OccurrenceDetailsScreen(
    viewModel: OccurrenceDetailsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val reportsViewModel: ReportsViewModel = hiltViewModel()

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val formatter = remember {
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault())
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(text = "Excluir Ocorrência", fontWeight = FontWeight.Bold) },
            text = { Text(text = "Deseja realmente excluir permanentemente esta ocorrência? Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteOccurrence {
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = FireColors.Error)
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            FireTopBar(
                title = uiState.occurrence?.protocolo ?: "Detalhes da Ocorrência",
                onBackClick = onNavigateBack,
                backgroundColor = FireColors.Surface,
                elevation = 2.dp,
                actions = {
                    uiState.occurrence?.let { occurrence ->
                        IconButton(
                            onClick = {
                                reportsViewModel.exportOccurrencePdf(occurrence) { uri ->
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(intent, "Compartilhar Relatório"))
                                }
                            }
                        ) {
                            Icon(
                                imageVector = FireIcons.Share,
                                contentDescription = "Compartilhar",
                                tint = FireColors.OnSurface
                            )
                        }
                        IconButton(
                            onClick = {
                                reportsViewModel.exportOccurrencePdf(occurrence) { uri ->
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "application/pdf")
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(intent)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = FireIcons.PictureAsPdf,
                                contentDescription = "Visualizar PDF",
                                tint = FireColors.OnSurface
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            uiState.occurrence?.let { occurrence ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    color = FireColors.Surface
                ) {
                    Row(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = FireColors.Error
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, FireColors.Error)
                        ) {
                            Icon(FireIcons.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Excluir")
                        }
                        FireButton(
                            text = "Editar",
                            onClick = { onNavigateToEdit(occurrence.id!!) },
                            modifier = Modifier.weight(1.5f),
                            icon = FireIcons.Edit,
                            containerColor = FireColors.Primary
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(FireColors.Background),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(color = FireColors.Primary)
                }
                uiState.errorMessage != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "Erro desconhecido",
                            style = FireTypography.Body,
                            color = FireColors.Error
                        )
                        Button(onClick = { viewModel.loadOccurrence() }) {
                            Text("Tentar Novamente")
                        }
                    }
                }
                uiState.occurrence != null -> {
                    val occurrence = uiState.occurrence!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Cabeçalho de Resumo do Protocolo
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = FireColors.Surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Protocolo: ${occurrence.protocolo}",
                                        style = FireTypography.Title,
                                        fontWeight = FontWeight.Bold,
                                        color = FireColors.OnBackground
                                    )
                                    FireStatusChip(
                                        text = "ATIVO",
                                        backgroundColor = FireColors.Success.copy(alpha = 0.12f),
                                        textColor = FireColors.Success
                                    )
                                }
                                Text(
                                    text = "Data/Hora: ${formatter.format(occurrence.dataHora)}",
                                    style = FireTypography.Body,
                                    color = FireColors.OnSurfaceVariant
                                )
                            }
                        }

                        // Detalhes Gerais
                        SectionCard(title = "Informações Gerais") {
                            InfoRow(
                                icon = "🏷️",
                                label = "Natureza",
                                value = occurrence.natureza.descricao
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            InfoRow(
                                icon = "📍",
                                label = "Endereço",
                                value = "${occurrence.rua ?: ""}, ${occurrence.numero ?: ""} - ${occurrence.bairro ?: ""}, ${occurrence.cidade ?: ""}/${occurrence.uf ?: ""}"
                            )
                            occurrence.latitude?.let { lat ->
                                Spacer(modifier = Modifier.height(12.dp))
                                InfoRow(
                                    icon = "🗺️",
                                    label = "Coordenadas",
                                    value = "Lat ${"%.5f".format(lat)} | Lng ${"%.5f".format(occurrence.longitude)}"
                                )
                            }
                        }

                        // Guarnição e Viaturas
                        if (occurrence.viaturas.isNotEmpty()) {
                            SectionCard(title = "🚒 Guarnição e Viaturas") {
                                occurrence.viaturas.forEachIndexed { index, viatura ->
                                    if (index > 0) Spacer(modifier = Modifier.height(12.dp))
                                    ViaturaDetailCard(viatura = viatura)
                                }
                            }
                        }

                        // Veículos
                        if (occurrence.veiculos.isNotEmpty()) {
                            SectionCard(title = "🚗 Veículos Envolvidos") {
                                occurrence.veiculos.forEachIndexed { index, veiculo ->
                                    if (index > 0) Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "• Placa: ${veiculo.placa ?: "SEM PLACA"} | Modelo: ${veiculo.modelo ?: "N/I"} | Cor: ${veiculo.cor ?: "N/I"}",
                                        style = FireTypography.Body,
                                        color = FireColors.OnBackground
                                    )
                                }
                            }
                        }

                        // Vítimas
                        if (occurrence.vitimas.isNotEmpty()) {
                            SectionCard(title = "👤 Vítimas e Socorro") {
                                occurrence.vitimas.forEachIndexed { index, vitima ->
                                    if (index > 0) Spacer(modifier = Modifier.height(12.dp))
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = FireColors.SurfaceVariant.copy(alpha = 0.3f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = vitima.nome ?: "Não Identificado",
                                                style = FireTypography.Body,
                                                fontWeight = FontWeight.Medium,
                                                color = FireColors.OnBackground
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = "GCS (Glasgow): ${vitima.sinaisVitais.escalaGCS ?: "N/D"}",
                                                    fontSize = 12.sp,
                                                    color = FireColors.OnSurfaceVariant
                                                )
                                                Text(
                                                    text = "P.A.: ${vitima.sinaisVitais.pressaoArterial ?: "N/D"}",
                                                    fontSize = 12.sp,
                                                    color = FireColors.OnSurfaceVariant
                                                )
                                                Text(
                                                    text = "FC: ${vitima.sinaisVitais.pulso ?: "N/D"}",
                                                    fontSize = 12.sp,
                                                    color = FireColors.OnSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = "🏥 Destino: ${vitima.hospitalDestino ?: "Não encaminhado"}",
                                                fontSize = 12.sp,
                                                color = FireColors.OnSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Histórico
                        occurrence.historico?.takeIf { it.isNotBlank() }?.let { hist ->
                            SectionCard(title = "📝 Histórico Narrativo") {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = FireColors.SurfaceVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Text(
                                        text = hist,
                                        style = FireTypography.Body,
                                        color = FireColors.OnBackground,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = FireColors.Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = FireTypography.Title,
                fontWeight = FontWeight.Bold,
                color = FireColors.Primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(
    icon: String,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$icon $label:",
            style = FireTypography.Body,
            fontWeight = FontWeight.Medium,
            color = FireColors.OnSurfaceVariant,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = value,
            style = FireTypography.Body,
            color = FireColors.OnBackground,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ViaturaDetailCard(viatura: Viatura) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = FireColors.SurfaceVariant.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "${viatura.prefixo} (${viatura.tipo})",
                style = FireTypography.Body,
                fontWeight = FontWeight.Medium,
                color = FireColors.OnBackground
            )
            viatura.equipe.forEach { m ->
                Text(
                    text = "• RE ${m.re} - ${m.graduacao} ${m.nomeGuerra} [${m.funcao.takeIf { it.isNotEmpty() } ?: "Equipe"}]",
                    style = FireTypography.Body,
                    color = FireColors.OnSurfaceVariant
                )
            }
        }
    }
}
