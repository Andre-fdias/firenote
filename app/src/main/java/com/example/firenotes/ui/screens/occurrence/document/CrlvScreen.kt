package com.example.firenotes.ui.screens.occurrence.document

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.firenotes.ui.designsystem.components.inputs.FireOutlinedTextField
import com.example.firenotes.ui.designsystem.spacing.FireSpacing

@Composable
fun CrlvIdentificationScreen(
    state: CrlvDocumentState,
    onStateChange: (CrlvDocumentState) -> Unit,
    validationErrors: Map<String, String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
        ) {
            FireOutlinedTextField(
                value = state.placa,
                onValueChange = { onStateChange(state.copy(placa = it)) },
                label = "Placa",
                error = validationErrors.containsKey("placa"),
                modifier = Modifier.weight(1f)
            )
            FireOutlinedTextField(
                value = state.cor,
                onValueChange = { onStateChange(state.copy(cor = it)) },
                label = "Cor",
                error = validationErrors.containsKey("cor"),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
        ) {
            FireOutlinedTextField(
                value = state.marca,
                onValueChange = { onStateChange(state.copy(marca = it)) },
                label = "Marca",
                error = validationErrors.containsKey("marca"),
                modifier = Modifier.weight(1f)
            )
            FireOutlinedTextField(
                value = state.modelo,
                onValueChange = { onStateChange(state.copy(modelo = it)) },
                label = "Modelo",
                error = validationErrors.containsKey("modelo"),
                modifier = Modifier.weight(1f)
            )
        }

        FireOutlinedTextField(
            value = state.versao,
            onValueChange = { onStateChange(state.copy(versao = it)) },
            label = "Versão do Veículo",
            error = validationErrors.containsKey("versao")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
        ) {
            FireOutlinedTextField(
                value = state.anoFabricacao,
                onValueChange = { onStateChange(state.copy(anoFabricacao = it)) },
                label = "Ano Fabr.",
                error = validationErrors.containsKey("anoFabricacao"),
                modifier = Modifier.weight(1f)
            )
            FireOutlinedTextField(
                value = state.anoModelo,
                onValueChange = { onStateChange(state.copy(anoModelo = it)) },
                label = "Ano Mod.",
                error = validationErrors.containsKey("anoModelo"),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
        ) {
            FireOutlinedTextField(
                value = state.renavam,
                onValueChange = { onStateChange(state.copy(renavam = it)) },
                label = "RENAVAM",
                error = validationErrors.containsKey("renavam"),
                modifier = Modifier.weight(1f)
            )
            FireOutlinedTextField(
                value = state.chassi,
                onValueChange = { onStateChange(state.copy(chassi = it)) },
                label = "Chassi",
                error = validationErrors.containsKey("chassi"),
                modifier = Modifier.weight(1f)
            )
        }

        FireOutlinedTextField(
            value = state.proprietario,
            onValueChange = { onStateChange(state.copy(proprietario = it)) },
            label = "Nome do Proprietário",
            error = validationErrors.containsKey("proprietario")
        )

        FireOutlinedTextField(
            value = state.cpfProprietario.filter { it.isDigit() },
            onValueChange = { onStateChange(state.copy(cpfProprietario = formatCpf(it))) },
            label = "CPF do Proprietário",
            error = validationErrors.containsKey("cpfProprietario"),
            visualTransformation = CpfVisualTransformation()
        )
    }
}
