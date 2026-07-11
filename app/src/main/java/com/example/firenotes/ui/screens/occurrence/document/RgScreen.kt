package com.example.firenotes.ui.screens.occurrence.document

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.firenotes.ui.designsystem.components.inputs.FireOutlinedTextField
import com.example.firenotes.ui.designsystem.components.inputs.FireDatePicker
import com.example.firenotes.ui.designsystem.spacing.FireSpacing

@Composable
fun RgIdentificationScreen(
    state: RgDocumentState,
    onStateChange: (RgDocumentState) -> Unit,
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
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
        ) {
            FireOutlinedTextField(
                value = state.rg,
                onValueChange = { onStateChange(state.copy(rg = it)) },
                label = "RG",
                error = validationErrors.containsKey("rg"),
                modifier = Modifier.weight(1f)
            )
            FireOutlinedTextField(
                value = state.uf,
                onValueChange = { onStateChange(state.copy(uf = it)) },
                label = "UF",
                error = validationErrors.containsKey("uf"),
                modifier = Modifier.weight(1f)
            )
        }

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
            value = state.mae,
            onValueChange = { onStateChange(state.copy(mae = it)) },
            label = "Filiação (Mãe)",
            error = validationErrors.containsKey("mae")
        )

        FireOutlinedTextField(
            value = state.naturalidade,
            onValueChange = { onStateChange(state.copy(naturalidade = it)) },
            label = "Naturalidade",
            error = validationErrors.containsKey("naturalidade")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
        ) {
            FireOutlinedTextField(
                value = state.orgaoExpedidor,
                onValueChange = { onStateChange(state.copy(orgaoExpedidor = it)) },
                label = "Órgão Expedidor",
                error = validationErrors.containsKey("orgaoExpedidor"),
                modifier = Modifier.weight(1f)
            )
            FireDatePicker(
                value = state.dataExpedicao,
                onDateSelected = { onStateChange(state.copy(dataExpedicao = it)) },
                label = "Data de Expedição",
                modifier = Modifier.weight(1f)
            )
        }
    }
}
