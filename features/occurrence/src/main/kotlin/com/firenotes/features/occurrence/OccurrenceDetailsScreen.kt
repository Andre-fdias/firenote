package com.firenotes.features.occurrence

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun OccurrenceDetailsScreen(
    occurrenceId: String,
    onNavigateToEdit: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Detalhes Operacionais da Ocorrência (ID: $occurrenceId)")
    }
}
