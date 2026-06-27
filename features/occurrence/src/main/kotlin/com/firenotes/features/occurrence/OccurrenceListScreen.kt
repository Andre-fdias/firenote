package com.firenotes.features.occurrence

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.firenotes.core.common.domain.model.Occurrence
import com.firenotes.core.common.domain.model.OccurrenceStatus
import com.firenotes.features.occurrence.ui.list.OccurrenceListUiState
import com.firenotes.features.occurrence.ui.list.OccurrenceListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OccurrenceListScreen(
    onNavigateToDetails: (String) -> Unit,
    onNavigateToNew: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: OccurrenceListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ocorrências Registradas") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
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
                onClick = onNavigateToNew,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nova Ocorrência")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is OccurrenceListUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is OccurrenceListUiState.Success -> {
                    if (state.occurrences.isEmpty()) {
                        Text(
                            text = "Nenhuma ocorrência registrada offline.",
                            modifier = Modifier.align(Alignment.Center),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.occurrences) { occurrence ->
                                OccurrenceCard(
                                    occurrence = occurrence,
                                    onClick = { onNavigateToDetails(occurrence.id) }
                                )
                            }
                        }
                    }
                }
                is OccurrenceListUiState.Error -> {
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

@Composable
fun OccurrenceCard(
    occurrence: Occurrence,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nº Interno: ${occurrence.internalNumber}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                
                val statusIcon = if (occurrence.status == OccurrenceStatus.SYNCED) Icons.Default.CloudDone else Icons.Default.CloudQueue
                val statusColor = if (occurrence.status == OccurrenceStatus.SYNCED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                Icon(
                    imageVector = statusIcon,
                    contentDescription = if (occurrence.status == OccurrenceStatus.SYNCED) "Sincronizado" else "Pendente de Backup",
                    tint = statusColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Natureza: ${occurrence.nature}", fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Data: ${occurrence.date} às ${occurrence.dispatchTime}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            occurrence.address?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Endereço: $it, ${occurrence.number ?: "S/N"}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
