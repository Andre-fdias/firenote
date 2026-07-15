package com.example.firenotes.ui.screens.occurrence.document

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.firenotes.ui.designsystem.components.inputs.FireOutlinedTextField
import com.example.firenotes.ui.designsystem.components.inputs.FireDatePicker
import com.example.firenotes.ui.designsystem.components.buttons.FireButton
import com.example.firenotes.ui.designsystem.spacing.FireSpacing

@Composable
fun CpfIdentificationScreen(
    state: CpfDocumentState,
    onStateChange: (CpfDocumentState) -> Unit,
    validationErrors: Map<String, String>,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
    ) {
        FireButton(
            text = "📷 Escanear Documento",
            onClick = onScanClick,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(FireSpacing.ExtraSmall))

        FireOutlinedTextField(
            value = state.nome,
            onValueChange = { onStateChange(state.copy(nome = it)) },
            label = "Nome Completo",
            error = validationErrors.containsKey("nome")
        )

        FireOutlinedTextField(
            value = state.cpf.filter { it.isDigit() },
            onValueChange = { onStateChange(state.copy(cpf = formatCpf(it))) },
            label = "CPF",
            error = validationErrors.containsKey("cpf"),
            visualTransformation = CpfVisualTransformation()
        )

        FireDatePicker(
            value = state.nascimento,
            onDateSelected = { onStateChange(state.copy(nascimento = it)) },
            label = "Data de Nascimento"
        )

        FireOutlinedTextField(
            value = state.filiacao,
            onValueChange = { onStateChange(state.copy(filiacao = it)) },
            label = "Filiação",
            error = validationErrors.containsKey("filiacao")
        )
    }
}
