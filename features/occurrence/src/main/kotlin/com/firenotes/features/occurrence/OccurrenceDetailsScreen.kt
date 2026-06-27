package com.firenotes.features.occurrence

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.firenotes.features.occurrence.ui.details.OccurrenceDetailsUiState
import com.firenotes.features.occurrence.ui.details.OccurrenceDetailsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OccurrenceDetailsScreen(
    occurrenceId: String,
    onNavigateToEdit: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: OccurrenceDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Dispara a carga dos dados da ocorrência com base no ID recebido por parâmetro
    LaunchedEffect(occurrenceId) {
        viewModel.loadOccurrence(occurrenceId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalhes da Ocorrência") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToEdit(occurrenceId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToEdit(occurrenceId) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Editar")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is OccurrenceDetailsUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is OccurrenceDetailsUiState.Success -> {
                    val occurrence = state.occurrence
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Nº Interno: ${occurrence.internalNumber}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Natureza: ${occurrence.nature}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Status: ${if (occurrence.status.name == "SYNCED") "Sincronizado" else "Pendente de Backup"}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (occurrence.status.name == "SYNCED") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        Text(
                            text = "Tempos Operacionais",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Text(text = "Data: ${occurrence.date}")
                        Text(text = "Hora de Acionamento: ${occurrence.dispatchTime}")
                        Text(text = "Hora de Chegada Local: ${occurrence.arrivalTime ?: "Não registrada"}")
                        Text(text = "Hora de Término: ${occurrence.completionTime ?: "Não registrada"}")

                        HorizontalDivider()

                        Text(
                            text = "Endereço e Localização",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Text(text = "Rua: ${occurrence.address ?: "Não registrada"}")
                        Text(text = "Número: ${occurrence.number ?: "S/N"}")
                        Text(text = "Complemento: ${occurrence.complement ?: "Nenhum"}")
                        Text(text = "Bairro: ${occurrence.neighborhood ?: "Não registrado"}")
                        Text(text = "Cidade: ${occurrence.city ?: "Não registrada"} - ${occurrence.state ?: ""}")
                        Text(text = "CEP: ${occurrence.zipCode ?: ""}")
                        Text(text = "Ponto de Referência: ${occurrence.referencePoint ?: "Nenhum"}")

                        HorizontalDivider()

                        Text(
                            text = "Observações Operacionais",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = occurrence.observations.ifBlank { "Nenhuma observação registrada." },
                            fontSize = 16.sp
                        )
                    }
                }
                is OccurrenceDetailsUiState.Error -> {
                    Text(
                        text = state.message,
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
