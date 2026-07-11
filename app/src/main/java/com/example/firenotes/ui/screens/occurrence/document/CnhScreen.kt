package com.example.firenotes.ui.screens.occurrence.document

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.firenotes.ui.designsystem.components.inputs.FireOutlinedTextField
import com.example.firenotes.ui.designsystem.components.inputs.FireDatePicker
import com.example.firenotes.ui.designsystem.spacing.FireSpacing

@Composable
fun CnhIdentificationScreen(
    state: CnhDocumentState,
    onStateChange: (CnhDocumentState) -> Unit,
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
            label = "Nome do Condutor",
            error = validationErrors.containsKey("nome")
        )

        FireOutlinedTextField(
            value = state.cpf,
            onValueChange = { onStateChange(state.copy(cpf = it)) },
            label = "CPF",
            error = validationErrors.containsKey("cpf")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
        ) {
            FireOutlinedTextField(
                value = state.registro,
                onValueChange = { onStateChange(state.copy(registro = it)) },
                label = "Nº Registro CNH",
                error = validationErrors.containsKey("registro"),
                modifier = Modifier.weight(1f)
            )
            FireOutlinedTextField(
                value = state.categoria,
                onValueChange = { onStateChange(state.copy(categoria = it)) },
                label = "Cat. Hab.",
                error = validationErrors.containsKey("categoria"),
                modifier = Modifier.weight(1f)
            )
        }

        FireDatePicker(
            value = state.nascimento,
            onDateSelected = { onStateChange(state.copy(nascimento = it)) },
            label = "Data de Nascimento"
        )

        FireOutlinedTextField(
            value = state.filiacao,
            onValueChange = { onStateChange(state.copy(filiacao = it)) },
            label = "Filiação (Pai / Mãe)",
            error = validationErrors.containsKey("filiacao")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
        ) {
            FireDatePicker(
                value = state.primeiraHabilitacao,
                onDateSelected = { onStateChange(state.copy(primeiraHabilitacao = it)) },
                label = "1ª Habilitação",
                modifier = Modifier.weight(1f)
            )
            FireDatePicker(
                value = state.validade,
                onDateSelected = { onStateChange(state.copy(validade = it)) },
                label = "Validade CNH",
                modifier = Modifier.weight(1f)
            )
        }
    }
}
