package com.firenotes.features.occurrence

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.firenotes.features.occurrence.ui.form.OccurrenceFormEvent
import com.firenotes.features.occurrence.ui.form.OccurrenceFormViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OccurrenceEditScreen(
    occurrenceId: String,
    onNavigateBack: () -> Unit,
    viewModel: OccurrenceFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Dispara o carregamento dos dados da ocorrência existente
    LaunchedEffect(occurrenceId) {
        viewModel.onEvent(OccurrenceFormEvent.LoadOccurrence(occurrenceId))
    }

    // Fechar a tela após salvar as alterações com sucesso
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Ocorrência") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(OccurrenceFormEvent.Save) }) {
                        Icon(Icons.Default.Save, contentDescription = "Salvar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (uiState.errorMessage != null) {
                    Text(
                        text = uiState.errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Text(
                    text = "Dados do Acionamento",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = uiState.internalNumber,
                    onValueChange = { viewModel.onEvent(OccurrenceFormEvent.InternalNumberChanged(it)) },
                    label = { Text("Número Interno (ex: 2026-001)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.nature,
                    onValueChange = { viewModel.onEvent(OccurrenceFormEvent.NatureChanged(it)) },
                    label = { Text("Natureza da Ocorrência") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.date,
                        onValueChange = { viewModel.onEvent(OccurrenceFormEvent.DateChanged(it)) },
                        label = { Text("Data (AAAA-MM-DD)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = uiState.dispatchTime,
                        onValueChange = { viewModel.onEvent(OccurrenceFormEvent.DispatchTimeChanged(it)) },
                        label = { Text("Acionamento (HH:MM)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.arrivalTime,
                        onValueChange = { viewModel.onEvent(OccurrenceFormEvent.ArrivalTimeChanged(it)) },
                        label = { Text("Chegada Local (HH:MM)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = uiState.completionTime,
                        onValueChange = { viewModel.onEvent(OccurrenceFormEvent.CompletionTimeChanged(it)) },
                        label = { Text("Término (HH:MM)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider()

                Text(
                    text = "Endereço e Localização",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = uiState.address,
                    onValueChange = { viewModel.onEvent(OccurrenceFormEvent.AddressChanged(it)) },
                    label = { Text("Rua / Logradouro") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.number,
                        onValueChange = { viewModel.onEvent(OccurrenceFormEvent.NumberChanged(it)) },
                        label = { Text("Número") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = uiState.complement,
                        onValueChange = { viewModel.onEvent(OccurrenceFormEvent.ComplementChanged(it)) },
                        label = { Text("Complemento") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.neighborhood,
                        onValueChange = { viewModel.onEvent(OccurrenceFormEvent.NeighborhoodChanged(it)) },
                        label = { Text("Bairro") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = uiState.zipCode,
                        onValueChange = { viewModel.onEvent(OccurrenceFormEvent.ZipCodeChanged(it)) },
                        label = { Text("CEP") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.city,
                        onValueChange = { viewModel.onEvent(OccurrenceFormEvent.CityChanged(it)) },
                        label = { Text("Cidade") },
                        modifier = Modifier.weight(2f)
                    )
                    OutlinedTextField(
                        value = uiState.state,
                        onValueChange = { viewModel.onEvent(OccurrenceFormEvent.StateChanged(it)) },
                        label = { Text("UF") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = uiState.referencePoint,
                    onValueChange = { viewModel.onEvent(OccurrenceFormEvent.ReferencePointChanged(it)) },
                    label = { Text("Ponto de Referência") },
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                Text(
                    text = "Informações Complementares",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = uiState.observations,
                    onValueChange = { viewModel.onEvent(OccurrenceFormEvent.ObservationsChanged(it)) },
                    label = { Text("Observações Operacionais") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { viewModel.onEvent(OccurrenceFormEvent.Save) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Salvar")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Salvar Edição")
                }
            }
        }
    }
}
