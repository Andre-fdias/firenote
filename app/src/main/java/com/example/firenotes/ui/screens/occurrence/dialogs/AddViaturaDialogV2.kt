package com.example.firenotes.ui.screens.occurrence.dialogs

import com.example.firenotes.ui.screens.occurrence.OccurrenceFormUiState
import com.example.firenotes.ui.screens.occurrence.models.FormStage
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.firenotes.domain.model.Viatura
import com.example.firenotes.ui.designsystem.components.buttons.FireButton
import com.example.firenotes.ui.designsystem.components.dialogs.FireDialog
import com.example.firenotes.ui.designsystem.components.widgets.FireStatusChip
import com.example.firenotes.ui.designsystem.components.buttons.FireTextButton
import com.example.firenotes.ui.designsystem.colors.FireColors
import com.example.firenotes.ui.designsystem.spacing.FireSpacing
import com.example.firenotes.ui.designsystem.typography.FireTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddViaturaDialogV2(
    viatura: Viatura? = null,
    onDismiss: () -> Unit,
    onConfirm: (prefixo: String, unidade: String, kmSaida: Int?, kmLocal: Int?, observacoes: String) -> Unit
) {
    // Estados
    var prefixo by remember { mutableStateOf(viatura?.prefixo ?: "") }
    var unidade by remember { mutableStateOf(viatura?.unidade ?: "") }
    var kmSaidaRaw by remember { mutableStateOf(viatura?.kmSaida?.toString() ?: "") }
    var kmLocalRaw by remember { mutableStateOf(viatura?.kmLocal?.toString() ?: "") }
    var observacoes by remember { mutableStateOf(viatura?.observacoes ?: "") }
    
    // Validação
    var prefixoError by remember { mutableStateOf<String?>(null) }
    
    fun validatePrefixo(input: String): Boolean {
        val clean = input.uppercase().replace(Regex("[^A-Z0-9]"), "")
        return clean.length == 7 && (
            clean.matches(Regex("^[A-Z]{2}\\d{5}$")) ||
            clean.matches(Regex("^[A-Z]{2}\\d[A-Z]\\d{3}$"))
        )
    }
    
    val isKmInvalid = remember(kmSaidaRaw, kmLocalRaw) {
        val sa = kmSaidaRaw.toIntOrNull()
        val lo = kmLocalRaw.toIntOrNull()
        sa != null && lo != null && lo < sa
    }
    
    val distancia = remember(kmSaidaRaw, kmLocalRaw) {
        val sa = kmSaidaRaw.toIntOrNull() ?: 0
        val lo = kmLocalRaw.toIntOrNull() ?: 0
        if (lo >= sa) lo - sa else 0
    }
    
    val isFormValid = remember(prefixo, prefixoError, isKmInvalid) {
        prefixo.isNotBlank() &&
        validatePrefixo(prefixo) &&
        prefixoError == null &&
        !isKmInvalid
    }
    
    FireDialog(
        onDismissRequest = onDismiss,
        title = if (viatura == null) "Adicionar Viatura" else "Editar Viatura",
        confirmButton = {
            FireButton(
                enabled = isFormValid,
                onClick = {
                    onConfirm(
                        prefixo.uppercase().replace(Regex("[^A-Z0-9]"), ""),
                        unidade.uppercase(),
                        kmSaidaRaw.toIntOrNull(),
                        kmLocalRaw.toIntOrNull(),
                        observacoes
                    )
                },
                text = "Confirmar",
                containerColor = FireColors.Primary
            )
        },
        dismissButton = {
            FireTextButton(onClick = onDismiss, text = "Cancelar")
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
        ) {
            // Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val hasAllFields = prefixo.isNotBlank() && unidade.isNotBlank() && 
                                   kmSaidaRaw.isNotBlank() && kmLocalRaw.isNotBlank() &&
                                   !isKmInvalid && prefixoError == null
                FireStatusChip(
                    text = if (hasAllFields) "✅ Dados Completos" else "⚠️ Faltam Dados",
                    backgroundColor = if (hasAllFields) FireColors.Success.copy(alpha = 0.2f) else FireColors.Warning.copy(alpha = 0.2f),
                    textColor = if (hasAllFields) FireColors.Success else FireColors.Warning
                )
            }
            
            // Card: Identificação
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
                        Text("🚒", style = FireTypography.Title)
                        Spacer(modifier = Modifier.width(FireSpacing.Small))
                        Text(
                            "Identificação da Viatura",
                            style = FireTypography.Title,
                            fontWeight = FontWeight.Bold,
                            color = FireColors.Primary
                        )
                    }
                    
                    OutlinedTextField(
                        value = prefixo,
                        onValueChange = { 
                            val formatted = it.uppercase().replace(" ", "")
                            prefixo = formatted
                            prefixoError = if (formatted.isNotBlank() && !validatePrefixo(formatted)) {
                                "Formato: XX-12345 ou XX1C234"
                            } else null
                        },
                        label = { Text("Prefixo", style = FireTypography.BodyMedium) },
                        placeholder = { Text("UR-12345", style = FireTypography.BodyMedium) },
                        isError = prefixoError != null,
                        supportingText = {
                            if (prefixoError != null) {
                                Text(prefixoError!!, color = FireColors.Error)
                            } else {
                                Text("Formato: XX-12345 ou XX1C234", style = FireTypography.LabelSmall)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FireColors.Primary,
                            unfocusedBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    OutlinedTextField(
                        value = unidade,
                        onValueChange = { 
                            if (it.length <= 80) {
                                unidade = it.uppercase()
                            }
                        },
                        label = { Text("Unidade/Batalhão", style = FireTypography.BodyMedium) },
                        placeholder = { Text("10º GB", style = FireTypography.BodyMedium) },
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
            
            // Card: Quilometragem
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
                        Text("🛣️", style = FireTypography.Title)
                        Spacer(modifier = Modifier.width(FireSpacing.Small))
                        Text(
                            "Quilometragem",
                            style = FireTypography.Title,
                            fontWeight = FontWeight.Bold,
                            color = FireColors.Primary
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                    ) {
                        OutlinedTextField(
                            value = kmSaidaRaw,
                            onValueChange = { 
                                val digits = it.filter { char -> char.isDigit() }
                                kmSaidaRaw = digits
                            },
                            label = { Text("KM Saída", style = FireTypography.BodyMedium) },
                            placeholder = { Text("0", style = FireTypography.BodyMedium) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FireColors.Primary,
                                unfocusedBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = kmLocalRaw,
                            onValueChange = { 
                                val digits = it.filter { char -> char.isDigit() }
                                kmLocalRaw = digits
                            },
                            label = { Text("KM Quartel", style = FireTypography.BodyMedium) },
                            placeholder = { Text("0", style = FireTypography.BodyMedium) },
                            isError = isKmInvalid,
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isKmInvalid) FireColors.Error else FireColors.Primary,
                                unfocusedBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    
                    if (isKmInvalid) {
                        Text(
                            "⚠️ KM de retorno não pode ser menor que KM de saída",
                            style = FireTypography.LabelSmall,
                            color = FireColors.Error
                        )
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = FireSpacing.Small))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "📏 Distância Percorrida:",
                            style = FireTypography.BodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "$distancia km",
                            style = FireTypography.Title,
                            fontWeight = FontWeight.Bold,
                            color = FireColors.Primary
                        )
                    }
                }
            }
            
            // Observações
            OutlinedTextField(
                value = observacoes,
                onValueChange = { 
                    if (it.length <= 1000) observacoes = it 
                },
                label = { Text("Observações", style = FireTypography.BodyMedium) },
                placeholder = {
                    Text(
                        "Ex: Viatura abastecida, pane elétrica, etc.",
                        style = FireTypography.BodyMedium
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FireColors.Primary,
                    unfocusedBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            )
            
            Text(
                text = "${observacoes.length}/1000 caracteres",
                style = FireTypography.LabelSmall,
                color = FireColors.OnSurfaceVariant,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}
