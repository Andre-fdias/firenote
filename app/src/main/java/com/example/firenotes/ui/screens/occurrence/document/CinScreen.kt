package com.example.firenotes.ui.screens.occurrence.document

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.firenotes.ui.designsystem.components.inputs.FireOutlinedTextField
import com.example.firenotes.ui.designsystem.components.inputs.FireDatePicker
import com.example.firenotes.ui.designsystem.spacing.FireSpacing

@Composable
fun CinIdentificationScreen(
    state: CinDocumentState,
    onStateChange: (CinDocumentState) -> Unit,
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
            label = "CPF (Chave Única)",
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
            value = state.pai,
            onValueChange = { onStateChange(state.copy(pai = it)) },
            label = "Filiação (Pai)",
            error = validationErrors.containsKey("pai")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
        ) {
            FireOutlinedTextField(
                value = state.sexo,
                onValueChange = { onStateChange(state.copy(sexo = it)) },
                label = "Sexo",
                error = validationErrors.containsKey("sexo"),
                modifier = Modifier.weight(1f)
            )
            FireOutlinedTextField(
                value = state.nacionalidade,
                onValueChange = { onStateChange(state.copy(nacionalidade = it)) },
                label = "Nacionalidade",
                error = validationErrors.containsKey("nacionalidade"),
                modifier = Modifier.weight(1f)
            )
        }

        FireOutlinedTextField(
            value = state.naturalidade,
            onValueChange = { onStateChange(state.copy(naturalidade = it)) },
            label = "Naturalidade",
            error = validationErrors.containsKey("naturalidade")
        )

        FireOutlinedTextField(
            value = state.orgao,
            onValueChange = { onStateChange(state.copy(orgao = it)) },
            label = "Órgão Emissor",
            error = validationErrors.containsKey("orgao")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
        ) {
            FireDatePicker(
                value = state.expedicao,
                onDateSelected = { onStateChange(state.copy(expedicao = it)) },
                label = "Data de Expedição",
                modifier = Modifier.weight(1f)
            )
            FireDatePicker(
                value = state.validade,
                onDateSelected = { onStateChange(state.copy(validade = it)) },
                label = "Data de Validade",
                modifier = Modifier.weight(1f)
            )
        }
    }
}
