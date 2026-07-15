package com.example.firenotes.ui.screens.occurrence.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.firenotes.domain.model.Militar
import com.example.firenotes.domain.model.Viatura
import com.example.firenotes.ui.designsystem.components.buttons.FireButton
import com.example.firenotes.ui.designsystem.components.buttons.FireTextButton
import com.example.firenotes.ui.designsystem.components.dialogs.FireDialog
import com.example.firenotes.ui.designsystem.components.buttons.FireOutlinedButton
import com.example.firenotes.ui.designsystem.icons.FireIcons
import com.example.firenotes.ui.designsystem.spacing.FireSpacing
import com.example.firenotes.ui.designsystem.typography.FireTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveMilitarDialog(
    militar: Militar,
    viaturas: List<Viatura>,
    onDismiss: () -> Unit,
    onConfirm: (newViaturaId: String) -> Unit
) {
    var selectedViaturaIndex by remember { mutableStateOf(-1) }
    var expandedDropdown by remember { mutableStateOf(false) }

    FireDialog(
        onDismissRequest = onDismiss,
        title = "Mover Militar",
        confirmButton = {
            FireButton(
                enabled = selectedViaturaIndex >= 0,
                onClick = {
                    onConfirm(viaturas[selectedViaturaIndex].id!!)
                },
                text = "Mover"
            )
        },
        dismissButton = {
            FireTextButton(onClick = onDismiss, text = "Cancelar")
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
        ) {
            Text(
                "Selecione a nova viatura para ${militar.nomeGuerra}:",
                style = FireTypography.BodyMedium
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                FireOutlinedButton(
                    text = if (selectedViaturaIndex >= 0)
                        viaturas[selectedViaturaIndex].prefixo
                    else "Selecionar Viatura",
                    onClick = { expandedDropdown = true },
                    modifier = Modifier.fillMaxWidth(),
                    icon = FireIcons.ArrowDropDown
                )
                DropdownMenu(
                    expanded = expandedDropdown,
                    onDismissRequest = { expandedDropdown = false }
                ) {
                    viaturas.forEachIndexed { index, v ->
                        if (v.id != militar.viaturaId) {
                            DropdownMenuItem(
                                text = { Text(v.prefixo) },
                                onClick = {
                                    selectedViaturaIndex = index
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
