package com.example.firenotes.ui.screens.occurrence.document

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.firenotes.ui.designsystem.components.inputs.FireOutlinedTextField
import com.example.firenotes.ui.designsystem.components.inputs.FireDatePicker
import com.example.firenotes.ui.designsystem.spacing.FireSpacing

@Composable
fun CpfIdentificationScreen(
    state: CpfDocumentState,
    onStateChange: (CpfDocumentState) -> Unit,
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
            label = "Nome Completo",
            error = validationErrors.containsKey("nome")
        )

        FireOutlinedTextField(
            value = state.cpf,
            onValueChange = { onStateChange(state.copy(cpf = it)) },
            label = "CPF",
            error = validationErrors.containsKey("cpf")
        )

        FireDatePicker(
            value = state.nascimento,
            onDateSelected = { onStateChange(state.copy(nascimento = it)) },
            label = "Data de Nascimento"
        )

        FireOutlinedTextField(
            value = state.situacao,
            onValueChange = { onStateChange(state.copy(situacao = it)) },
            label = "Situação Cadastral",
            error = validationErrors.containsKey("situacao")
        )

        FireDatePicker(
            value = state.dataInscricao,
            onDateSelected = { onStateChange(state.copy(dataInscricao = it)) },
            label = "Data de Inscrição"
        )
    }
}
