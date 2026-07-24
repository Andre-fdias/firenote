package com.example.firenotes.ui.screens.occurrence.dialogs

import com.example.firenotes.ui.screens.occurrence.OccurrenceFormUiState
import com.example.firenotes.ui.screens.occurrence.models.FormStage
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.firenotes.ui.designsystem.components.buttons.FireButton
import com.example.firenotes.ui.designsystem.components.dialogs.FireDialog
import com.example.firenotes.ui.designsystem.components.inputs.FireDropdown
import com.example.firenotes.ui.designsystem.components.buttons.FireTextButton
import com.example.firenotes.ui.designsystem.colors.FireColors
import com.example.firenotes.ui.designsystem.spacing.FireSpacing
import com.example.firenotes.ui.designsystem.typography.FireTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddApoioDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        orgaoSigla: String,
        orgaoNome: String,
        viatura: String,
        encarregado: String,
        descricaoOutros: String
    ) -> Unit
) {
    // Estados
    var selectedOrgaoSigla by remember { mutableStateOf("") }
    var viatura by remember { mutableStateOf("") }
    var encarregado by remember { mutableStateOf("") }
    var descricaoOutros by remember { mutableStateOf("") }
    
    val orgaosOptions = listOf(
        "PM - Policiamento área" to "Polícia Militar - Policiamento de Área",
        "PM - Ambiental" to "Polícia Militar - Ambiental",
        "PM - Choque" to "Polícia Militar - Choque",
        "PM - Rodoviaria" to "Polícia Militar - Rodoviária",
        "PRF" to "Polícia Rodoviária Federal",
        "PF" to "Polícia Federal",
        "SAMU" to "Serviço de Atendimento Móvel de Urgência",
        "GCM" to "Guarda Civil Metropolitana",
        "Defesa Civil" to "Defesa Civil",
        "Concessionárias" to "Concessionárias de Rodovias",
        "Outros" to "Outro Órgão de Apoio"
    )
    
    val siglaOptions = orgaosOptions.map { it.first }
    
    var showConfirmCancelDialog by remember { mutableStateOf(false) }

    val hasChanges = remember(selectedOrgaoSigla, viatura, encarregado, descricaoOutros) {
        selectedOrgaoSigla.isNotEmpty() || viatura.isNotEmpty() || encarregado.isNotEmpty() || descricaoOutros.isNotEmpty()
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

    val isFormValid = remember(selectedOrgaoSigla, viatura, encarregado, descricaoOutros) {
        selectedOrgaoSigla.isNotBlank() &&
        (selectedOrgaoSigla != "Outros" || descricaoOutros.isNotBlank())
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
        title = "Solicitar / Registrar Apoio",
        confirmButton = {
            FireButton(
                enabled = isFormValid,
                onClick = {
                    val pair = orgaosOptions.find { it.first == selectedOrgaoSigla }
                    val orgaoNome = pair?.second ?: selectedOrgaoSigla
                    onConfirm(
                        selectedOrgaoSigla,
                        orgaoNome,
                        viatura.uppercase(),
                        encarregado.uppercase(),
                        if (selectedOrgaoSigla == "Outros") descricaoOutros else ""
                    )
                },
                text = "Confirmar",
                containerColor = FireColors.Primary
            )
        },
        dismissButton = {
            FireTextButton(onClick = { attemptDismiss() }, text = "Cancelar")
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = FireColors.SurfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(FireSpacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🤝", style = FireTypography.Title)
                        Spacer(modifier = Modifier.width(FireSpacing.Small))
                        Text(
                            "Órgão de Apoio Externo",
                            style = FireTypography.Title,
                            fontWeight = FontWeight.Bold,
                            color = FireColors.Primary
                        )
                    }
                    
                    // Órgão (Dropdown)
                    FireDropdown(
                        selectedOption = selectedOrgaoSigla,
                        options = siglaOptions,
                        onOptionSelected = { selectedOrgaoSigla = it },
                        label = "Selecionar Órgão",
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Descrição customizada quando "OUTROS" for selecionado
                    AnimatedVisibility(visible = selectedOrgaoSigla == "Outros") {
                        OutlinedTextField(
                            value = descricaoOutros,
                            onValueChange = { descricaoOutros = it },
                            label = { Text("Nome do Órgão/Serviço", style = FireTypography.BodyMedium) },
                            placeholder = { Text("Ex: DER, Polícia Civil, etc.", style = FireTypography.BodyMedium) },
                            modifier = Modifier.fillMaxWidth().padding(top = FireSpacing.Small),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FireColors.Primary,
                                unfocusedBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    
                    // Viatura do Apoio
                    OutlinedTextField(
                        value = viatura,
                        onValueChange = { viatura = it },
                        label = { Text("Viatura / Placa (Opcional)", style = FireTypography.BodyMedium) },
                        placeholder = { Text("Ex: ABS-01, Placa XYZ-1234", style = FireTypography.BodyMedium) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FireColors.Primary,
                            unfocusedBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    // Encarregado do Apoio
                    OutlinedTextField(
                        value = encarregado,
                        onValueChange = { encarregado = it },
                        label = { Text("Responsável / Encarregado (Opcional)", style = FireTypography.BodyMedium) },
                        placeholder = { Text("Ex: Sgt Silva, Dr. Roberto", style = FireTypography.BodyMedium) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FireColors.Primary,
                            unfocusedBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    }
}
