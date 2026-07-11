package com.example.firenotes.ui.screens.occurrence.document

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.firenotes.ui.designsystem.components.inputs.FireOutlinedTextField
import com.example.firenotes.ui.designsystem.components.inputs.FireDatePicker
import com.example.firenotes.ui.designsystem.spacing.FireSpacing

@Composable
fun OabIdentificationScreen(
    state: OabDocumentState,
    onStateChange: (OabDocumentState) -> Unit,
    validationErrors: Map<String, String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
    ) {
        FireOutlinedTextField(
            value = state.nome,
            onValueChange = { onStateChange(state.copy(nome = it)) },
            label = "Nome do Advogado",
            error = validationErrors.containsKey("nome")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
        ) {
            FireOutlinedTextField(
                value = state.numero,
                onValueChange = { onStateChange(state.copy(numero = it)) },
                label = "Nº Inscrição OAB",
                error = validationErrors.containsKey("numero"),
                modifier = Modifier.weight(1f)
            )
            FireOutlinedTextField(
                value = state.uf,
                onValueChange = { onStateChange(state.copy(uf = it)) },
                label = "Seccional (UF)",
                error = validationErrors.containsKey("uf"),
                modifier = Modifier.weight(1f)
            )
        }

        FireDatePicker(
            value = state.expedicao,
            onDateSelected = { onStateChange(state.copy(expedicao = it)) },
            label = "Data de Expedição"
        )
    }
}
