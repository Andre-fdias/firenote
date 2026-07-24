package com.example.firenotes.ui.screens.occurrence.dialogs

import com.example.firenotes.ui.screens.occurrence.OccurrenceFormUiState
import com.example.firenotes.ui.screens.occurrence.models.FormStage
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.firenotes.domain.model.Militar
import com.example.firenotes.ui.designsystem.components.buttons.FireButton
import com.example.firenotes.ui.designsystem.components.dialogs.FireDialog
import com.example.firenotes.ui.designsystem.components.inputs.FireDropdown
import com.example.firenotes.ui.designsystem.components.buttons.FireTextButton
import com.example.firenotes.ui.designsystem.colors.FireColors
import com.example.firenotes.ui.designsystem.spacing.FireSpacing
import com.example.firenotes.ui.designsystem.typography.FireTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMilitarDialog(
    militar: Militar? = null,
    militarSuggestions: List<Militar> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (re: String, nomeGuerra: String, graduacao: String, funcao: String) -> Unit
) {
    // Estados
    var re by remember { mutableStateOf(militar?.re ?: "") }
    var nomeGuerra by remember { mutableStateOf(militar?.nomeGuerra ?: "") }
    var graduacao by remember { mutableStateOf(militar?.graduacao ?: "") }
    var funcao by remember { mutableStateOf(militar?.funcao ?: "") }
    
    // Validação
    var reError by remember { mutableStateOf<String?>(null) }
    var nomeError by remember { mutableStateOf<String?>(null) }
    
    val graduacoes = listOf(
        "CEL PM", "TEN CEL PM", "MAJ PM", "CAP PM", "1º TEN PM",
        "1º TEN QAPM", "2º TEN PM", "2º TEN QAPM", "ASP OF PM",
        "SUBTEN PM", "1º SGT PM", "2º SGT PM", "3º SGT PM", "CB PM", "SD PM"
    )
    val funcoes = listOf("Comandante", "Motorista", "Auxiliar", "Encarregado")
    
    fun validateRe(input: String): Boolean {
        return input.length == 6 && input.all { it.isDigit() }
    }
    
    fun validateNome(input: String): Boolean {
        return input.isNotBlank() && input.length in 2..30 && input.all { it.isLetter() || it.isWhitespace() }
    }
    
    var showConfirmCancelDialog by remember { mutableStateOf(false) }

    val hasChanges = remember(re, nomeGuerra, graduacao, funcao, militar) {
        if (militar != null) {
            re != militar.re ||
            nomeGuerra != militar.nomeGuerra ||
            graduacao != militar.graduacao ||
            funcao != militar.funcao
        } else {
            re.isNotEmpty() || nomeGuerra.isNotEmpty() || graduacao.isNotEmpty() || funcao.isNotEmpty()
        }
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

    val isFormValid = remember(re, nomeGuerra, graduacao, funcao, reError, nomeError) {
        validateRe(re) &&
        validateNome(nomeGuerra) &&
        graduacao.isNotBlank() &&
        funcao.isNotBlank() &&
        reError == null &&
        nomeError == null
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
        title = if (militar == null) "Registrar Militar" else "Editar Militar",
        confirmButton = {
            FireButton(
                enabled = isFormValid,
                onClick = {
                    onConfirm(re, nomeGuerra.uppercase(), graduacao, funcao)
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
            // Card: Dados Operacionais
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
                        Text("👨‍🚒", style = FireTypography.Title)
                        Spacer(modifier = Modifier.width(FireSpacing.Small))
                        Text(
                            "Dados do Militar",
                            style = FireTypography.Title,
                            fontWeight = FontWeight.Bold,
                            color = FireColors.Primary
                        )
                    }
                    
                    // RE com validação de 6 dígitos
                    var expandedRe by remember { mutableStateOf(false) }
                    val filteredByRe = remember(re, militarSuggestions) {
                        if (re.isBlank()) emptyList()
                        else militarSuggestions.filter { it.re.contains(re) && it.re != re }
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = re,
                            onValueChange = { 
                                val digits = it.filter { char -> char.isDigit() }
                                if (digits.length <= 6) {
                                    re = digits
                                    expandedRe = true
                                    reError = if (digits.isNotBlank() && !validateRe(digits)) {
                                        "RE deve conter exatamente 6 dígitos"
                                    } else null
                                }
                            },
                            label = { Text("RE (Registro Escolar)", style = FireTypography.BodyMedium) },
                            placeholder = { Text("123456", style = FireTypography.BodyMedium) },
                            isError = reError != null,
                            supportingText = {
                                if (reError != null) {
                                    Text(reError!!, color = FireColors.Error)
                                } else {
                                    Text("Apenas números (6 dígitos)", style = FireTypography.LabelSmall)
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FireColors.Primary,
                                unfocusedBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(
                            expanded = expandedRe && filteredByRe.isNotEmpty(),
                            onDismissRequest = { expandedRe = false },
                            properties = androidx.compose.ui.window.PopupProperties(focusable = false),
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            filteredByRe.take(5).forEach { suggestion ->
                                DropdownMenuItem(
                                    text = { Text("${suggestion.re} - ${suggestion.nomeGuerra} (${suggestion.graduacao})") },
                                    onClick = {
                                        re = suggestion.re
                                        nomeGuerra = suggestion.nomeGuerra
                                        graduacao = suggestion.graduacao
                                        funcao = suggestion.funcao
                                        expandedRe = false
                                    }
                                )
                            }
                        }
                    }
                    
                    var expandedNome by remember { mutableStateOf(false) }
                    val filteredByNome = remember(nomeGuerra, militarSuggestions) {
                        if (nomeGuerra.isBlank()) emptyList()
                        else militarSuggestions.filter { it.nomeGuerra.contains(nomeGuerra, ignoreCase = true) && it.nomeGuerra != nomeGuerra }
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = nomeGuerra,
                            onValueChange = { 
                                nomeGuerra = it
                                expandedNome = true
                                nomeError = if (it.isNotBlank() && !validateNome(it)) {
                                    "Apenas letras (2 a 30 caracteres)"
                                } else null
                            },
                            label = { Text("Nome de Guerra", style = FireTypography.BodyMedium) },
                            placeholder = { Text("SILVA", style = FireTypography.BodyMedium) },
                            isError = nomeError != null,
                            supportingText = {
                                if (nomeError != null) {
                                    Text(nomeError!!, color = FireColors.Error)
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
                        DropdownMenu(
                            expanded = expandedNome && filteredByNome.isNotEmpty(),
                            onDismissRequest = { expandedNome = false },
                            properties = androidx.compose.ui.window.PopupProperties(focusable = false),
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            filteredByNome.take(5).forEach { suggestion ->
                                DropdownMenuItem(
                                    text = { Text("${suggestion.re} - ${suggestion.nomeGuerra} (${suggestion.graduacao})") },
                                    onClick = {
                                        re = suggestion.re
                                        nomeGuerra = suggestion.nomeGuerra
                                        graduacao = suggestion.graduacao
                                        funcao = suggestion.funcao
                                        expandedNome = false
                                    }
                                )
                            }
                        }
                    }
                    
                    // Graduação (Dropdown)
                    FireDropdown(
                        selectedOption = graduacao,
                        options = graduacoes,
                        onOptionSelected = { graduacao = it },
                        label = "Graduação Hierárquica",
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Função (Dropdown)
                    FireDropdown(
                        selectedOption = funcao,
                        options = funcoes,
                        onOptionSelected = { funcao = it },
                        label = "Função na Viatura",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
