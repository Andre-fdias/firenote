package com.example.firenotes.ui.screens.occurrence.dialogs

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.firenotes.domain.repository.OcrDocumentResult
import com.example.firenotes.ui.designsystem.colors.FireColors
import com.example.firenotes.ui.designsystem.components.buttons.FireButton
import com.example.firenotes.ui.designsystem.components.buttons.FireTextButton
import com.example.firenotes.ui.designsystem.components.dialogs.FireDialog
import com.example.firenotes.ui.designsystem.components.buttons.FireOutlinedButton
import com.example.firenotes.ui.designsystem.components.inputs.FireOutlinedTextField
import com.example.firenotes.ui.designsystem.icons.FireIcons
import com.example.firenotes.ui.designsystem.spacing.FireSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDocumentDialog(
    ocrResult: OcrDocumentResult?,
    onDismiss: () -> Unit,
    onConfirm: (tipo: String, numero: String, fields: Map<String, String>, rawText: String, uri: Uri) -> Unit
) {
    val documentTypes = listOf("CNH", "CIN", "RG", "CREA", "CRM", "COREN")
    var selectedTypeIndex by remember { mutableStateOf(0) }
    var expandedDropdown by remember { mutableStateOf(false) }
    var numero by remember { mutableStateOf("") }
    var nome by remember { mutableStateOf("") }
    var cpf by remember { mutableStateOf("") }

    var showConfirmCancelDialog by remember { mutableStateOf(false) }

    val hasChanges = remember(selectedTypeIndex, numero, nome, cpf) {
        selectedTypeIndex != 0 || numero.isNotEmpty() || nome.isNotEmpty() || cpf.isNotEmpty()
    }

    val attemptDismiss = {
        if (hasChanges) {
            showConfirmCancelDialog = true
        } else {
            onDismiss()
        }
    }

    BackHandler {
        attemptDismiss()
    }

    if (showConfirmCancelDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmCancelDialog = false },
            title = { Text("Existem alterações não salvas") },
            text = { Text("Deseja realmente cancelar este cadastro?") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmCancelDialog = false
                    onDismiss()
                }) {
                    Text("Descartar alterações")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmCancelDialog = false }) {
                    Text("Continuar editando")
                }
            }
        )
    }

    FireDialog(
        onDismissRequest = { attemptDismiss() },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = "Adicionar Documento",
        confirmButton = {
            FireButton(
                onClick = {
                    val fields = mapOf(
                        "nome" to nome,
                        "cpf" to cpf
                    )
                    onConfirm(
                        documentTypes[selectedTypeIndex],
                        numero,
                        fields,
                        ocrResult?.rawText ?: "",
                        Uri.EMPTY
                    )
                },
                text = "Confirmar"
            )
        },
        dismissButton = {
            FireTextButton(onClick = { attemptDismiss() }, text = "Cancelar")
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
        ) {
            FireOutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = "Nome Completo"
            )
            FireOutlinedTextField(
                value = cpf,
                onValueChange = { cpf = it },
                label = "CPF"
            )
            FireOutlinedTextField(
                value = numero,
                onValueChange = { numero = it },
                label = "Número do Documento"
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                FireOutlinedButton(
                    text = "Tipo: ${documentTypes[selectedTypeIndex]}",
                    onClick = { expandedDropdown = true },
                    modifier = Modifier.fillMaxWidth(),
                    icon = FireIcons.ArrowDropDown
                )
                DropdownMenu(
                    expanded = expandedDropdown,
                    onDismissRequest = { expandedDropdown = false }
                ) {
                    documentTypes.forEachIndexed { index, type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                selectedTypeIndex = index
                                expandedDropdown = false
                            }
                        )
                    }
                }
            }
        }
    }
}
