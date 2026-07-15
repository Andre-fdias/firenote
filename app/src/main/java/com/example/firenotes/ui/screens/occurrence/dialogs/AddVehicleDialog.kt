package com.example.firenotes.ui.screens.occurrence.dialogs

import com.example.firenotes.ui.screens.occurrence.OccurrenceFormUiState
import com.example.firenotes.ui.screens.occurrence.models.FormStage
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.firenotes.domain.model.Pessoa
import com.example.firenotes.domain.repository.OcrDocumentResult
import com.example.firenotes.ui.designsystem.components.buttons.FireButton
import com.example.firenotes.ui.designsystem.components.dialogs.FireDialog
import com.example.firenotes.ui.designsystem.components.buttons.FireTextButton
import com.example.firenotes.ui.designsystem.colors.FireColors
import com.example.firenotes.ui.designsystem.icons.FireIcons
import com.example.firenotes.ui.designsystem.spacing.FireSpacing
import com.example.firenotes.ui.designsystem.typography.FireTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVehicleDialog(
    ocrResult: OcrDocumentResult?,
    pessoasDisponiveis: List<Pessoa>,
    crlvImage: Uri?,
    onDismiss: () -> Unit,
    onConfirm: (
        placa: String,
        modelo: String,
        cor: String,
        chassi: String,
        ano: String,
        proprietarioId: String?,
        marca: String,
        versao: String,
        exercicio: String,
        crlvUri: Uri?
    ) -> Unit
) {
    // Estados dos campos
    var placa by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf("") }
    var cor by remember { mutableStateOf("") }
    var chassi by remember { mutableStateOf("") }
    var anoFabricacao by remember { mutableStateOf("") }
    var anoModelo by remember { mutableStateOf("") }
    var marca by remember { mutableStateOf("") }
    var versao by remember { mutableStateOf("") }
    var exercicio by remember { mutableStateOf("") }
    var selectedPessoaId by remember { mutableStateOf<String?>(null) }
    
    // Estados de UI
    var expandedPessoaDropdown by remember { mutableStateOf(false) }
    var expandedCorDropdown by remember { mutableStateOf(false) }
    var placaError by remember { mutableStateOf<String?>(null) }
    var anoError by remember { mutableStateOf<String?>(null) }
    
    // Preencher com OCR
    LaunchedEffect(ocrResult) {
        ocrResult?.let {
            placa = it.extractedFields["placa"]?.uppercase() ?: ""
            modelo = it.extractedFields["modelo"] ?: it.extractedFields["marca_modelo"] ?: ""
            cor = it.extractedFields["cor"] ?: ""
            chassi = it.extractedFields["chassi"]?.uppercase() ?: ""
            marca = it.extractedFields["marca"] ?: ""
            versao = it.extractedFields["versao"] ?: ""
            exercicio = it.extractedFields["exercicio"] ?: ""
            
            val anoFab = it.extractedFields["ano_fabricacao"]?.toIntOrNull()
            val anoMod = it.extractedFields["ano_modelo"]?.toIntOrNull()
            if (anoFab != null) anoFabricacao = anoFab.toString()
            if (anoMod != null) anoModelo = anoMod.toString()
        }
    }
    
    // Cores disponíveis
    val cores = listOf(
        "Branco", "Preto", "Prata", "Cinza", "Vermelho",
        "Azul", "Verde", "Amarelo", "Laranja", "Marrom",
        "Bege", "Dourado", "Rosa", "Roxo", "Outra"
    )
    
    // Validações
    fun validatePlaca(input: String): Boolean {
        val clean = input.uppercase().replace(Regex("[^A-Z0-9]"), "")
        return clean.length == 7 && (
            clean.matches(Regex("^[A-Z]{3}\\d{4}$")) ||
            clean.matches(Regex("^[A-Z]{3}\\d[A-Z]\\d{2}$"))
        )
    }
    
    fun validateAno(input: String): Boolean {
        val anoVal = input.toIntOrNull() ?: return false
        val anoAtual = java.time.Year.now().value
        return anoVal in 1900..(anoAtual + 1)
    }
    
    val isFormValid = remember(
        placa, modelo, cor, chassi, anoFabricacao, anoModelo,
        selectedPessoaId, placaError, anoError
    ) {
        placa.isNotBlank() &&
        validatePlaca(placa) &&
        modelo.isNotBlank() &&
        cor.isNotBlank() &&
        chassi.isNotBlank() &&
        (anoFabricacao.isBlank() || validateAno(anoFabricacao)) &&
        (anoModelo.isBlank() || validateAno(anoModelo)) &&
        placaError == null &&
        anoError == null
    }
    
    FireDialog(
        onDismissRequest = onDismiss,
        title = "Registrar Veículo",
        confirmButton = {
            FireButton(
                enabled = isFormValid,
                onClick = {
                    val ano = if (anoFabricacao.isNotBlank() && anoModelo.isNotBlank()) {
                        "$anoFabricacao/$anoModelo"
                    } else if (anoFabricacao.isNotBlank()) {
                        anoFabricacao
                    } else {
                        anoModelo
                    }
                    
                    onConfirm(
                        placa.uppercase().replace(Regex("[^A-Z0-9]"), ""),
                        modelo,
                        cor,
                        chassi.uppercase(),
                        ano,
                        selectedPessoaId,
                        marca,
                        versao,
                        exercicio,
                        crlvImage
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
            // Card: Identificação do Veículo
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
                        Text("🚗", style = FireTypography.Title)
                        Spacer(modifier = Modifier.width(FireSpacing.Small))
                        Text(
                            "Identificação do Veículo",
                            style = FireTypography.Title,
                            fontWeight = FontWeight.Bold,
                            color = FireColors.Primary
                        )
                    }
                    
                    // Placa com validação
                    OutlinedTextField(
                        value = placa,
                        onValueChange = { 
                            val newValue = it.uppercase()
                            placa = newValue
                            placaError = if (newValue.isNotBlank() && !validatePlaca(newValue)) {
                                "Placa inválida. Formato: ABC-1234 ou ABC1D23"
                            } else null
                        },
                        label = { Text("Placa", style = FireTypography.BodyMedium) },
                        placeholder = { Text("ABC-1234", style = FireTypography.BodyMedium) },
                        isError = placaError != null,
                        supportingText = { 
                            if (placaError != null) {
                                Text(placaError!!, color = FireColors.Error)
                            } else {
                                Text("Formato Mercosul ou tradicional", style = FireTypography.LabelSmall)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FireColors.Primary,
                            unfocusedBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = {
                             Icon(
                                 imageVector = FireIcons.DirectionsCar,
                                 contentDescription = null,
                                 tint = FireColors.Primary
                             )
                        }
                    )
                    
                    // Modelo
                    OutlinedTextField(
                        value = modelo,
                        onValueChange = { modelo = it },
                        label = { Text("Modelo/Marca", style = FireTypography.BodyMedium) },
                        placeholder = { Text("Ex: Corolla, HB20, Onix", style = FireTypography.BodyMedium) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FireColors.Primary,
                            unfocusedBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    // Cor com dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = cor,
                            onValueChange = { cor = it },
                            label = { Text("Cor", style = FireTypography.BodyMedium) },
                            placeholder = { Text("Selecione ou digite", style = FireTypography.BodyMedium) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { expandedCorDropdown = !expandedCorDropdown }) {
                                    Icon(
                                        imageVector = if (expandedCorDropdown) FireIcons.ArrowDropUp else FireIcons.ArrowDropDown,
                                        contentDescription = null
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FireColors.Primary,
                                unfocusedBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(
                            expanded = expandedCorDropdown,
                            onDismissRequest = { expandedCorDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            cores.forEach { corItem ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .background(
                                                        color = Color(android.graphics.Color.parseColor(
                                                            when(corItem.lowercase()) {
                                                                "branco" -> "#FFFFFF"
                                                                "preto" -> "#000000"
                                                                "prata" -> "#C0C0C0"
                                                                "cinza" -> "#808080"
                                                                "vermelho" -> "#FF0000"
                                                                "azul" -> "#0000FF"
                                                                "verde" -> "#008000"
                                                                "amarelo" -> "#FFFF00"
                                                                "laranja" -> "#FFA500"
                                                                "marrom" -> "#8B4513"
                                                                "bege" -> "#F5F5DC"
                                                                "dourado" -> "#FFD700"
                                                                "rosa" -> "#FF69B4"
                                                                "roxo" -> "#800080"
                                                                else -> "#CCCCCC"
                                                            }
                                                        )),
                                                        shape = CircleShape
                                                    )
                                                    .border(1.dp, Color.Gray, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(FireSpacing.Small))
                                            Text(corItem)
                                        }
                                    },
                                    onClick = {
                                        cor = corItem
                                        expandedCorDropdown = false
                                    }
                                )
                            }
                        }
                    }
                    
                    // Chassi
                    OutlinedTextField(
                        value = chassi,
                        onValueChange = { chassi = it.uppercase() },
                        label = { Text("Chassi", style = FireTypography.BodyMedium) },
                        placeholder = { Text("Ex: 9BWZZZ12345678901", style = FireTypography.BodyMedium) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FireColors.Primary,
                            unfocusedBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    // Ano com validação
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                    ) {
                        OutlinedTextField(
                            value = anoFabricacao,
                            onValueChange = { 
                                val digits = it.filter { char -> char.isDigit() }
                                if (digits.length <= 4) {
                                    anoFabricacao = digits
                                    anoError = if (digits.isNotBlank() && !validateAno(digits)) {
                                        "Ano inválido"
                                    } else null
                                }
                            },
                            label = { Text("Ano Fabricação", style = FireTypography.BodyMedium) },
                            placeholder = { Text("2024", style = FireTypography.BodyMedium) },
                            isError = anoError != null,
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FireColors.Primary,
                                unfocusedBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = anoModelo,
                            onValueChange = { 
                                val digits = it.filter { char -> char.isDigit() }
                                if (digits.length <= 4) {
                                    anoModelo = digits
                                    anoError = if (digits.isNotBlank() && !validateAno(digits)) {
                                        "Ano inválido"
                                    } else null
                                }
                            },
                            label = { Text("Ano Modelo", style = FireTypography.BodyMedium) },
                            placeholder = { Text("2025", style = FireTypography.BodyMedium) },
                            isError = anoError != null,
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FireColors.Primary,
                                unfocusedBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    if (anoError != null) {
                        Text(
                            "Ano inválido (1900-${java.time.Year.now().value + 1})",
                            style = FireTypography.LabelSmall,
                            color = FireColors.Error,
                            modifier = Modifier.padding(start = FireSpacing.ExtraSmall)
                        )
                    }
                }
            }
            
            // Card: Proprietário
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
                        Text("👤", style = FireTypography.Title)
                        Spacer(modifier = Modifier.width(FireSpacing.Small))
                        Text(
                            "Proprietário",
                            style = FireTypography.Title,
                            fontWeight = FontWeight.Bold,
                            color = FireColors.Primary
                        )
                    }
                    
                    // Dropdown de Proprietários
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = pessoasDisponiveis.find { it.id == selectedPessoaId }?.nome ?: "",
                            onValueChange = {},
                            label = { Text("Selecionar Proprietário", style = FireTypography.BodyMedium) },
                            placeholder = { Text("Selecione uma pessoa", style = FireTypography.BodyMedium) },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { expandedPessoaDropdown = !expandedPessoaDropdown }) {
                                    Icon(
                                        imageVector = if (expandedPessoaDropdown) FireIcons.ArrowDropUp else FireIcons.ArrowDropDown,
                                        contentDescription = null
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FireColors.Primary,
                                unfocusedBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(
                            expanded = expandedPessoaDropdown,
                            onDismissRequest = { expandedPessoaDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            if (pessoasDisponiveis.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Nenhuma pessoa cadastrada", color = Color.Gray) },
                                    onClick = {}
                                )
                            } else {
                                pessoasDisponiveis.forEach { pessoa ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(pessoa.nome, fontWeight = FontWeight.Medium)
                                                Text(
                                                    "CPF: ${pessoa.cpf ?: "N/A"}",
                                                    style = FireTypography.LabelSmall,
                                                    color = Color.Gray
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedPessoaId = pessoa.id
                                            expandedPessoaDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Card: CRLV Capturado
            if (crlvImage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = FireColors.Success.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, FireColors.Success.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(FireSpacing.Medium),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                    ) {
                        Icon(
                            imageVector = FireIcons.PhotoCamera,
                            contentDescription = null,
                            tint = FireColors.Success
                        )
                        Column {
                            Text(
                                "✅ CRLV Capturado com Sucesso",
                                style = FireTypography.BodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = FireColors.Success
                            )
                            Text(
                                "Documento anexado ao veículo",
                                style = FireTypography.LabelSmall,
                                color = FireColors.Success
                            )
                        }
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = FireColors.SurfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(FireSpacing.Medium),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                    ) {
                        Icon(
                            imageVector = FireIcons.Info,
                            contentDescription = null,
                            tint = FireColors.OnSurfaceVariant
                        )
                        Text(
                            "Nenhum CRLV capturado. Use o botão 'Scanear CRLV'",
                            style = FireTypography.LabelMedium,
                            color = FireColors.OnSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
