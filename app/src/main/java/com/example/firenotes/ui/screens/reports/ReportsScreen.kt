package com.example.firenotes.ui.screens.reports

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.firenotes.domain.model.Ocorrencia
import com.example.firenotes.ui.designsystem.colors.FireColors
import com.example.firenotes.ui.designsystem.spacing.FireSpacing
import com.example.firenotes.ui.designsystem.typography.FireTypography
import com.example.firenotes.ui.designsystem.icons.FireIcons
import com.example.firenotes.ui.designsystem.components.topbar.FireTopBar
import com.example.firenotes.ui.designsystem.components.inputs.FireOutlinedTextField
import com.example.firenotes.ui.designsystem.components.buttons.FireButton
import com.example.firenotes.ui.designsystem.components.cards.FireCard
import com.example.firenotes.ui.designsystem.components.widgets.FireDivider
import com.example.firenotes.ui.designsystem.states.FireLoading

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Helper to share/open generated report file
    val shareFile: (Uri) -> Unit = { uri ->
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartilhar Relatório"))
    }

    Scaffold(
        topBar = {
            FireTopBar(
                title = "Relatórios Operacionais",
                onBackClick = onNavigateBack
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
            // Period filters input
            Row(horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium), modifier = Modifier.fillMaxWidth()) {
                FireOutlinedTextField(
                    value = uiState.dataInicial,
                    onValueChange = { viewModel.updateDates(it, uiState.dataFinal) },
                    label = "Data Inicial",
                    modifier = Modifier.weight(1f)
                )
                FireOutlinedTextField(
                    value = uiState.dataFinal,
                    onValueChange = { viewModel.updateDates(uiState.dataInicial, it) },
                    label = "Data Final",
                    modifier = Modifier.weight(1f)
                )
            }

            // Export Actions
            Text("Exportar Dados Consolidados do Período", style = FireTypography.Title, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small)
            ) {
                FireButton(
                    text = "CSV",
                    onClick = { viewModel.exportPeriodCsv(shareFile) },
                    containerColor = FireColors.Secondary,
                    icon = FireIcons.List,
                    modifier = Modifier.weight(1f)
                )

                FireButton(
                    text = "Excel (XLS)",
                    onClick = { viewModel.exportPeriodExcel(shareFile) },
                    containerColor = Color(0xFF2E7D32),
                    icon = FireIcons.List,
                    modifier = Modifier.weight(1f)
                )
            }

            if (uiState.exportMessage != null) {
                Text(
                    text = uiState.exportMessage!!,
                    style = FireTypography.Label,
                    fontWeight = FontWeight.Bold,
                    color = FireColors.Primary,
                    modifier = Modifier.fillMaxWidth()
                )
                uiState.exportedFileUri?.let { uri ->
                    FireButton(
                        text = "COMPARTILHAR / ENVIAR RELATÓRIO",
                        onClick = { shareFile(uri) },
                        containerColor = FireColors.Primary,
                        icon = FireIcons.Share,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            FireDivider()

            Text("Exportação Individual por Ocorrência (PDF)", style = FireTypography.Title, fontWeight = FontWeight.Bold)
            if (uiState.isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    FireLoading()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                ) {
                    items(uiState.occurrences) { o ->
                        FireCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(FireSpacing.Medium),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Talão: ${o.protocolo}", style = FireTypography.Body, fontWeight = FontWeight.Bold)
                                    Text("Natureza: ${o.natureza.descricao}", style = FireTypography.Caption, color = Color.Gray)
                                }
                                FireButton(
                                    text = "PDF",
                                    onClick = { viewModel.exportOccurrencePdf(o, shareFile) },
                                    containerColor = Color(0xFFC62828),
                                    icon = FireIcons.Share
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
