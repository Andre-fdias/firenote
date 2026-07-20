package com.example.firenotes.ui.screens.occurrence.components

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.firenotes.domain.model.NaturezaOcorrencia
import com.example.firenotes.ui.designsystem.colors.FireColors
import com.example.firenotes.ui.designsystem.components.buttons.FireButton
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.example.firenotes.ui.designsystem.components.cards.FireCard
import com.example.firenotes.ui.designsystem.components.inputs.*
import com.example.firenotes.ui.designsystem.components.widgets.FireStatusChip
import com.example.firenotes.ui.designsystem.icons.FireIcons
import com.example.firenotes.ui.designsystem.spacing.FireSpacing
import com.example.firenotes.ui.designsystem.typography.FireTypography
import com.example.firenotes.ui.screens.occurrence.OccurrenceFormUiState
import com.example.firenotes.ui.screens.occurrence.OccurrenceFormViewModel
import com.example.firenotes.ui.screens.occurrence.models.SubNatureza
import com.example.firenotes.ui.screens.occurrence.models.subNaturezas
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ============================================
// TELA PRINCIPAL - DESIGN MODERNO PREMIUM
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InitialDataScreen(
    uiState: OccurrenceFormUiState,
    viewModel: OccurrenceFormViewModel,
    locationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Estados para modais
    var showHelpModal by remember { mutableStateOf(false) }
    var showNaturezaModal by remember { mutableStateOf(false) }

    // Verificação de permissão de localização
    val hasLocationPermission = remember {
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    // Auto-captura de localização
    LaunchedEffect(Unit) {
        if (hasLocationPermission && uiState.latitude == null) {
            viewModel.captureLocationAndAddress()
        } else if (!hasLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    var showTechDetails by remember { mutableStateOf(false) }
    var isEditingAddress by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }

    // Verifica se os dados estão bloqueados (já salvos)
    val isDataLocked = uiState.isSaved || uiState.protocolo.isNotBlank()
    val isAddressLocked = uiState.latitude != null && uiState.rua.isNotBlank() && !isEditingAddress
    val isAddressEditable = !isAddressLocked || isEditingAddress

    // Validação do formulário
    val isFormValid = uiState.protocolo.isNotBlank() &&
            uiState.rua.isNotBlank() &&
            uiState.cidade.isNotBlank() &&
            uiState.uf.isNotBlank() &&
            uiState.natureza != NaturezaOcorrencia.INDEFINIDA

    // Data e hora formatadas para exibição
    val formattedDate = remember(uiState.data) {
        if (uiState.data.isNotEmpty()) {
            try {
                val date = LocalDate.parse(uiState.data)
                date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            } catch (e: Exception) {
                uiState.data
            }
        } else ""
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        FireColors.Background,
                        FireColors.Surface.copy(alpha = 0.8f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = FireSpacing.MediumLarge, vertical = FireSpacing.Large),
            verticalArrangement = Arrangement.spacedBy(FireSpacing.MediumLarge)
        ) {
            // ==========================================
            // CARD 1: DADOS DA OCORRÊNCIA
            // ==========================================
            ModernIdentificationCard(
                protocolo = uiState.protocolo,
                data = uiState.data,
                hora = uiState.hora,
                formattedDate = formattedDate,
                prontidaoColor = uiState.prontidaoColor,
                isLocked = isDataLocked,
                onProtocoloChange = { input ->
                    val formatted = formatTalao(input)
                    viewModel.updateInitialFields(formatted, uiState.data, uiState.hora)
                },
                onDateClick = { showDatePickerDialog = true },
                onTimeClick = { showTimePickerDialog = true }
            )

            // ==========================================
            // CARD 2: LOCALIZAÇÃO MODERNA
            // ==========================================
            ModernLocationCard(
                uiState = uiState,
                isAddressEditable = isAddressEditable,
                isAddressLocked = isAddressLocked,
                isEditingAddress = isEditingAddress,
                onToggleEditAddress = { isEditingAddress = !isEditingAddress },
                onAddressChanged = viewModel::updateManualAddress,
                showTechDetails = showTechDetails,
                onShowTechDetailsChange = { showTechDetails = it },
                onFetchGps = { viewModel.captureLocationAndAddress() },
                onRestoreSavedLocation = viewModel::updateFullLocation
            )

            // ==========================================
            // CARD 3: NATUREZA MODERNA
            // ==========================================
            ModernNaturezaCard(
                selectedSubNaturezaName = uiState.subNaturezaSelecionada ?: "",
                onSubNaturezaSelected = { sub ->
                    viewModel.selectNaturezaForCreation(sub.baseNatureza, sub.nome)
                },
                onOpenModal = { showNaturezaModal = true }
            )

            // ==========================================
            // BOTÃO DE AÇÃO PRINCIPAL
            // ==========================================
            AnimatedContent(
                targetState = isFormValid,
                transitionSpec = {
                    fadeIn() + slideInVertically() togetherWith fadeOut() + slideOutVertically()
                },
                label = "submit_button"
            ) { isValid ->
                if (isValid) {
                    FireButton(
                        text = "CRIAR OCORRÊNCIA",
                        enabled = isValid,
                        onClick = {
                            viewModel.selectNaturezaAndCreateOccurrence(uiState.natureza)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        containerColor = FireColors.Primary
                    )
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = FireColors.SurfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = FireColors.OnSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Preencha todos os campos obrigatórios",
                                style = FireTypography.BodyMedium,
                                color = FireColors.OnSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // MODAL DE AJUDA
    // ==========================================
    if (showHelpModal) {
        HelpModal(onDismiss = { showHelpModal = false })
    }

    // ==========================================
    // MODAL DE NATUREZA
    // ==========================================
    if (showNaturezaModal) {
        NaturezaModal(
            selectedSubNaturezaName = uiState.subNaturezaSelecionada ?: "",
            onSubNaturezaSelected = { sub ->
                viewModel.selectNaturezaForCreation(sub.baseNatureza, sub.nome)
                showNaturezaModal = false
            },
            onDismiss = { showNaturezaModal = false }
        )
    }

    // ==========================================
    // DIALOGS DE DATA E HORA
    // ==========================================
    if (showDatePickerDialog && !isDataLocked) {
        Dialog(
            onDismissRequest = { showDatePickerDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = FireColors.Surface)
            ) {
                Column(
                    modifier = Modifier.padding(FireSpacing.Large),
                    verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
                ) {
                    Text(
                        text = "📅 Selecione a Data",
                        style = FireTypography.Headline,
                        fontWeight = FontWeight.Bold,
                        color = FireColors.Primary
                    )
                    FireDatePicker(
                        value = uiState.data,
                        onDateSelected = {
                            viewModel.updateInitialFields(uiState.protocolo, it, uiState.hora)
                            showDatePickerDialog = false
                        },
                        label = "Data do Fato",
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        onClick = { showDatePickerDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancelar", color = FireColors.OnSurfaceVariant)
                    }
                }
            }
        }
    }

    if (showTimePickerDialog && !isDataLocked) {
        Dialog(
            onDismissRequest = { showTimePickerDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = FireColors.Surface)
            ) {
                Column(
                    modifier = Modifier.padding(FireSpacing.Large),
                    verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
                ) {
                    Text(
                        text = "🕐 Selecione a Hora",
                        style = FireTypography.Headline,
                        fontWeight = FontWeight.Bold,
                        color = FireColors.Primary
                    )
                    FireTimePicker(
                        value = uiState.hora,
                        onTimeSelected = {
                            viewModel.updateInitialFields(uiState.protocolo, uiState.data, it)
                            showTimePickerDialog = false
                        },
                        label = "Hora do Fato",
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        onClick = { showTimePickerDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancelar", color = FireColors.OnSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ============================================
// HEADER MODERNO COM ANIMAÇÃO
// ============================================

@Composable
private fun ModernHeader(
    isGpsFixed: Boolean,
    isGpsLoading: Boolean,
    onFetchGps: () -> Unit,
    onHelpClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = FireColors.Primary.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FireSpacing.MediumLarge),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = FireIcons.LocalFireDepartment,
                        contentDescription = null,
                        tint = FireColors.Primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "NOVA OCORRÊNCIA",
                        style = FireTypography.Headline,
                        fontWeight = FontWeight.ExtraBold,
                        color = FireColors.Primary,
                        fontSize = 20.sp
                    )
                }
                Text(
                    text = "Preencha os dados do atendimento",
                    style = FireTypography.BodyMedium,
                    color = FireColors.OnSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status GPS animado
                AnimatedContent(
                    targetState = Triple(isGpsLoading, isGpsFixed, true),
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    },
                    label = "gps_status"
                ) { (loading, fixed, _) ->
                    when {
                        loading -> {
                            Surface(
                                shape = CircleShape,
                                color = FireColors.Warning.copy(alpha = 0.15f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = FireColors.Warning,
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }
                        fixed -> {
                            Surface(
                                shape = CircleShape,
                                color = FireColors.Success.copy(alpha = 0.15f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "GPS Fixado",
                                        tint = FireColors.Success,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        else -> {
                            IconButton(
                                onClick = onFetchGps,
                                modifier = Modifier.size(40.dp),
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = FireColors.Secondary.copy(alpha = 0.12f),
                                    contentColor = FireColors.Secondary
                                )
                            ) {
                                Icon(
                                    imageVector = FireIcons.LocationOn,
                                    contentDescription = "Buscar GPS",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onHelpClick,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = FireColors.Primary.copy(alpha = 0.1f),
                        contentColor = FireColors.Primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Ajuda",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ============================================
// CARD DE IDENTIFICAÇÃO MODERNO
// ============================================

@Composable
private fun ModernIdentificationCard(
    protocolo: String,
    data: String,
    hora: String,
    formattedDate: String,
    prontidaoColor: String,
    isLocked: Boolean,
    onProtocoloChange: (String) -> Unit,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = FireColors.Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(FireSpacing.MediumLarge),
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = FireColors.Primary.copy(alpha = 0.1f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Assignment,
                                contentDescription = null,
                                tint = FireColors.Primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Nova Ocorrência",
                        style = FireTypography.Title,
                        fontWeight = FontWeight.Bold,
                        color = FireColors.Primary
                    )
                }

                if (isLocked) {
                    FireStatusChip(
                        text = "✓ Confirmado",
                        backgroundColor = FireColors.Success.copy(alpha = 0.15f),
                        textColor = FireColors.Success
                    )
                }
            }

            // Primeira Linha: Número do Talão e Prontidão de Serviço
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Número do Talão - Peso 1.2
                OutlinedTextField(
                    value = protocolo,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it == '-' }
                        val formatted = formatTalao(filtered)
                        onProtocoloChange(formatted)
                    },
                    label = { Text("Número do Talão") },
                    placeholder = { Text("Ex.: 2026-04587") },
                    leadingIcon = {
                        Icon(
                            imageVector = FireIcons.AddAlert,
                            contentDescription = null,
                            tint = FireColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    readOnly = false,
                    singleLine = true,
                    modifier = Modifier.weight(1.2f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FireColors.Primary,
                        unfocusedBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.3f),
                        focusedLabelColor = FireColors.Primary,
                        unfocusedLabelColor = FireColors.OnSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Prontidão de Serviço Automática - Peso 0.8
                val colorValue = when (prontidaoColor.uppercase()) {
                    "VERDE" -> Color(0xFF4CAF50)
                    "AMARELA" -> Color(0xFFFFC107)
                    "AZUL" -> Color(0xFF2196F3)
                    else -> FireColors.Primary
                }
                Card(
                    modifier = Modifier
                        .weight(0.8f)
                        .height(56.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorValue.copy(alpha = 0.08f)
                    ),
                    border = BorderStroke(1.dp, colorValue.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(colorValue, CircleShape)
                        )
                        Column {
                            Text(
                                text = "Prontidão",
                                style = FireTypography.LabelSmall,
                                color = FireColors.OnSurfaceVariant
                            )
                            Text(
                                text = prontidaoColor.uppercase(),
                                style = FireTypography.BodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = colorValue
                            )
                        }
                    }
                }
            }

            // Segunda Linha: Data e Hora - Layout Melhorado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
            ) {
                // Campo de Data com Clique
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = !isLocked) { onDateClick() }
                        .height(56.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isLocked) FireColors.SurfaceVariant.copy(alpha = 0.3f) else FireColors.SurfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = if (isLocked) FireColors.OnSurfaceVariant.copy(alpha = 0.5f) else FireColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Data",
                                style = FireTypography.LabelSmall,
                                color = FireColors.OnSurfaceVariant
                            )
                            Text(
                                text = if (formattedDate.isNotEmpty()) formattedDate else "Selecionar",
                                style = FireTypography.BodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = if (isLocked) FireColors.OnSurfaceVariant.copy(alpha = 0.5f) else FireColors.OnSurface
                            )
                        }
                    }
                }

                // Campo de Hora com Clique
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = !isLocked) { onTimeClick() }
                        .height(56.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isLocked) FireColors.SurfaceVariant.copy(alpha = 0.3f) else FireColors.SurfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = if (isLocked) FireColors.OnSurfaceVariant.copy(alpha = 0.5f) else FireColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Hora",
                                style = FireTypography.LabelSmall,
                                color = FireColors.OnSurfaceVariant
                            )
                            Text(
                                text = if (hora.isNotEmpty()) hora else "Selecionar",
                                style = FireTypography.BodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = if (isLocked) FireColors.OnSurfaceVariant.copy(alpha = 0.5f) else FireColors.OnSurface
                            )
                    }
                }
            }
        }
    }
}
}

// ============================================
// CARD DE LOCALIZAÇÃO MODERNO
// ============================================

@Composable
private fun ModernLocationCard(
    uiState: OccurrenceFormUiState,
    isAddressEditable: Boolean,
    isAddressLocked: Boolean,
    isEditingAddress: Boolean,
    onToggleEditAddress: () -> Unit,
    onAddressChanged: (String, String, String, String, String) -> Unit,
    showTechDetails: Boolean,
    onShowTechDetailsChange: (Boolean) -> Unit,
    onFetchGps: () -> Unit,
    onRestoreSavedLocation: (Double, Double, String, String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("firenotes_location_cache", android.content.Context.MODE_PRIVATE) }
    val hasCachedLocation = remember(uiState) { prefs.getFloat("lat", 0f) != 0f }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isAddressEditable) FireColors.Surface else FireColors.SurfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(FireSpacing.MediumLarge),
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
        ) {
            // Cabeçalho com Status GPS
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (uiState.latitude != null)
                            FireColors.Success.copy(alpha = 0.1f)
                        else
                            FireColors.Warning.copy(alpha = 0.1f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = FireIcons.LocationOn,
                                contentDescription = null,
                                tint = if (uiState.latitude != null) FireColors.Success else FireColors.Warning,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Localização",
                        style = FireTypography.Title,
                        fontWeight = FontWeight.Bold,
                        color = FireColors.Primary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (uiState.latitude != null) {
                        FireStatusChip(
                            text = "GPS ✓",
                            backgroundColor = FireColors.Success.copy(alpha = 0.15f),
                            textColor = FireColors.Success
                        )
                    }
                    if (isAddressLocked) {
                        IconButton(
                            onClick = onToggleEditAddress,
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (isEditingAddress)
                                    FireColors.Primary.copy(alpha = 0.15f)
                                else
                                    FireColors.Primary.copy(alpha = 0.1f),
                                contentColor = if (isEditingAddress) FireColors.Primary else FireColors.OnSurfaceVariant
                            )
                        ) {
                            Icon(
                                imageVector = if (isEditingAddress) Icons.Default.LockOpen else Icons.Default.Edit,
                                contentDescription = "Editar endereço",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Status de Localização
            if (uiState.isGpsLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = FireColors.Primary.copy(alpha = 0.05f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(FireSpacing.Medium),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = FireColors.Primary,
                            strokeWidth = 2.dp
                        )
                        Column {
                            Text(
                                text = "Obtendo localização...",
                                style = FireTypography.BodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = FireColors.Primary
                            )
                            Text(
                                text = "Aguarde o GPS estabilizar",
                                style = FireTypography.LabelSmall,
                                color = FireColors.OnSurfaceVariant
                            )
                        }
                    }
                }
            } else if (uiState.latitude != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = FireColors.Success.copy(alpha = 0.05f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, FireColors.Success.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(FireSpacing.Medium),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = FireColors.Success.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = FireColors.Success,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Coordenadas obtidas",
                                style = FireTypography.BodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = FireColors.Success
                            )
                            Text(
                                text = "Lat: ${"%.6f".format(uiState.latitude)} | Lng: ${"%.6f".format(uiState.longitude)}",
                                style = FireTypography.LabelSmall,
                                color = FireColors.OnSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                FilledTonalButton(
                    onClick = onFetchGps,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = FireColors.Secondary.copy(alpha = 0.1f),
                        contentColor = FireColors.Secondary
                    )
                ) {
                    Icon(
                        imageVector = FireIcons.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "OBTER LOCALIZAÇÃO",
                        style = FireTypography.LabelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Sistema de Carga e Armazenamento Offline de Localização (Cache)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.latitude != null) {
                    FilledTonalButton(
                        onClick = {
                            prefs.edit().apply {
                                putFloat("lat", uiState.latitude.toFloat())
                                putFloat("lng", uiState.longitude?.toFloat() ?: 0f)
                                putString("rua", uiState.rua)
                                putString("numero", uiState.numero)
                                putString("bairro", uiState.bairro)
                                putString("cidade", uiState.cidade)
                                putString("uf", uiState.uf)
                                apply()
                            }
                            android.widget.Toast.makeText(context, "Localização salva no cache!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = FireColors.Success.copy(alpha = 0.1f),
                            contentColor = FireColors.Success
                        )
                    ) {
                        Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Salvar Local", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (hasCachedLocation && isAddressEditable) {
                    FilledTonalButton(
                        onClick = {
                            val lat = prefs.getFloat("lat", 0f).toDouble()
                            val lng = prefs.getFloat("lng", 0f).toDouble()
                            val rua = prefs.getString("rua", "") ?: ""
                            val numero = prefs.getString("numero", "") ?: ""
                            val bairro = prefs.getString("bairro", "") ?: ""
                            val cidade = prefs.getString("cidade", "") ?: ""
                            val uf = prefs.getString("uf", "") ?: ""
                            
                            onRestoreSavedLocation(lat, lng, rua, numero, bairro, cidade, uf)
                            android.widget.Toast.makeText(context, "Localização carregada do cache!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = FireColors.Primary.copy(alpha = 0.1f),
                            contentColor = FireColors.Primary
                        )
                    ) {
                        Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Carregar Salva", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Campos de Endereço
            val alphaValue = if (isAddressEditable) 1f else 0.6f

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(alpha = alphaValue),
                verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
            ) {
                OutlinedTextField(
                    value = uiState.rua,
                    onValueChange = {
                        if (isAddressEditable) {
                            onAddressChanged(it, uiState.numero, uiState.bairro, uiState.cidade, uiState.uf)
                        }
                    },
                    label = { Text("Logradouro") },
                    placeholder = { Text("Rua/Avenida") },
                    readOnly = !isAddressEditable,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FireColors.Primary,
                        unfocusedBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.3f),
                        focusedLabelColor = FireColors.Primary,
                        unfocusedLabelColor = FireColors.OnSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
                ) {
                    OutlinedTextField(
                        value = uiState.numero,
                        onValueChange = {
                            if (isAddressEditable) {
                                onAddressChanged(uiState.rua, it, uiState.bairro, uiState.cidade, uiState.uf)
                            }
                        },
                        label = { Text("Número") },
                        readOnly = !isAddressEditable,
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FireColors.Primary,
                            unfocusedBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.3f),
                            focusedLabelColor = FireColors.Primary,
                            unfocusedLabelColor = FireColors.OnSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = uiState.bairro,
                        onValueChange = {
                            if (isAddressEditable) {
                                onAddressChanged(uiState.rua, uiState.numero, it, uiState.cidade, uiState.uf)
                            }
                        },
                        label = { Text("Bairro") },
                        readOnly = !isAddressEditable,
                        singleLine = true,
                        modifier = Modifier.weight(2f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FireColors.Primary,
                            unfocusedBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.3f),
                            focusedLabelColor = FireColors.Primary,
                            unfocusedLabelColor = FireColors.OnSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
                ) {
                    OutlinedTextField(
                        value = uiState.cidade,
                        onValueChange = {
                            if (isAddressEditable) {
                                onAddressChanged(uiState.rua, uiState.numero, uiState.bairro, it, uiState.uf)
                            }
                        },
                        label = { Text("Cidade") },
                        readOnly = !isAddressEditable,
                        singleLine = true,
                        modifier = Modifier.weight(3f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FireColors.Primary,
                            unfocusedBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.3f),
                            focusedLabelColor = FireColors.Primary,
                            unfocusedLabelColor = FireColors.OnSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (isAddressEditable) {
                        val ufs = listOf(
                            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA",
                            "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN",
                            "RS", "RO", "RR", "SC", "SP", "SE", "TO"
                        )
                        FireDropdown(
                            selectedOption = uiState.uf,
                            options = ufs,
                            onOptionSelected = {
                                onAddressChanged(uiState.rua, uiState.numero, uiState.bairro, uiState.cidade, it)
                            },
                            label = "UF",
                            modifier = Modifier.weight(2f)
                        )
                    } else {
                        OutlinedTextField(
                            value = uiState.uf,
                            onValueChange = {},
                            label = { Text("UF") },
                            readOnly = true,
                            singleLine = true,
                            modifier = Modifier.weight(2f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FireColors.Primary,
                                unfocusedBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.3f),
                                focusedLabelColor = FireColors.Primary,
                                unfocusedLabelColor = FireColors.OnSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Detalhes Técnicos
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = FireColors.SurfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(FireSpacing.Medium)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onShowTechDetailsChange(!showTechDetails) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = FireColors.OnSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Detalhes Técnicos",
                                style = FireTypography.BodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = FireColors.OnSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = if (showTechDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = FireColors.OnSurfaceVariant
                        )
                    }

                    AnimatedVisibility(
                        visible = showTechDetails,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.padding(top = FireSpacing.Small),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TechDetailRow("Latitude", uiState.latitude?.toString() ?: "N/A")
                            TechDetailRow("Longitude", uiState.longitude?.toString() ?: "N/A")
                            TechDetailRow("Status", if (uiState.latitude != null) "GPS Fix" else "Inativo")
                            TechDetailRow("Acurácia", if (uiState.latitude != null) "4.8m" else "N/A")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TechDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = FireTypography.LabelSmall,
            color = FireColors.OnSurfaceVariant.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = FireTypography.LabelSmall,
            fontWeight = FontWeight.Medium,
            color = FireColors.OnSurfaceVariant
        )
    }
}

// ============================================
// CARD DE NATUREZA MODERNO
// ============================================

@Composable
private fun ModernNaturezaCard(
    selectedSubNaturezaName: String,
    onSubNaturezaSelected: (SubNatureza) -> Unit,
    onOpenModal: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = FireColors.Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(FireSpacing.MediumLarge),
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = FireColors.Secondary.copy(alpha = 0.1f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                tint = FireColors.Secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Natureza",
                        style = FireTypography.Title,
                        fontWeight = FontWeight.Bold,
                        color = FireColors.Primary
                    )
                }

                if (selectedSubNaturezaName.isNotBlank()) {
                    FireStatusChip(
                        text = "✓ Selecionada",
                        backgroundColor = FireColors.Success.copy(alpha = 0.15f),
                        textColor = FireColors.Success
                    )
                }
            }

            // Campo de seleção de natureza - Botão estilizado
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenModal() }
                    .height(56.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedSubNaturezaName.isNotBlank())
                        FireColors.Primary.copy(alpha = 0.05f)
                    else
                        FireColors.SurfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (selectedSubNaturezaName.isNotBlank())
                        FireColors.Primary.copy(alpha = 0.3f)
                    else
                        FireColors.OnSurfaceVariant.copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (selectedSubNaturezaName.isNotBlank()) {
                            val emoji = subNaturezas.find { it.nome == selectedSubNaturezaName }
                                ?.categoria?.split(" ")?.firstOrNull() ?: "📌"
                            Text(
                                text = emoji,
                                fontSize = 24.sp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = FireColors.OnSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = if (selectedSubNaturezaName.isNotBlank())
                                selectedSubNaturezaName
                            else
                                "Selecione a classificação",
                            style = FireTypography.BodyLarge,
                            color = if (selectedSubNaturezaName.isNotBlank())
                                FireColors.OnSurface
                            else
                                FireColors.OnSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = FireColors.OnSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// ============================================
// MODAL DE NATUREZA
// ============================================

@Composable
private fun NaturezaModal(
    selectedSubNaturezaName: String,
    onSubNaturezaSelected: (SubNatureza) -> Unit,
    onDismiss: () -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val categories = subNaturezas.map { it.categoria }.distinct()

    val filteredSubNaturezas = remember(searchText, selectedCategory) {
        subNaturezas.filter { sub ->
            val matchesSearch = sub.nome.contains(searchText, ignoreCase = true) ||
                    sub.categoria.contains(searchText, ignoreCase = true)
            val matchesCategory = selectedCategory == null || sub.categoria == selectedCategory
            matchesSearch && matchesCategory
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(
                containerColor = FireColors.Surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(FireSpacing.Large),
                verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
            ) {
                // Cabeçalho
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏷️ Classificação da Natureza",
                        style = FireTypography.Headline,
                        fontWeight = FontWeight.Bold,
                        color = FireColors.Primary,
                        fontSize = 18.sp
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = FireColors.OnSurfaceVariant
                        )
                    }
                }

                // Search
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = { Text("Buscar natureza") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = FireColors.OnSurfaceVariant
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FireColors.Primary,
                        unfocusedBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.3f),
                        focusedLabelColor = FireColors.Primary,
                        unfocusedLabelColor = FireColors.OnSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Categorias
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = {
                                Text(
                                    "Todas",
                                    style = FireTypography.LabelSmall
                                )
                            },
                            modifier = Modifier.height(32.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FireColors.Primary,
                                selectedLabelColor = Color.White,
                                containerColor = FireColors.SurfaceVariant.copy(alpha = 0.5f)
                            )
                        )
                    }
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = {
                                selectedCategory = if (selectedCategory == category) null else category
                            },
                            label = {
                                Text(
                                    category,
                                    style = FireTypography.LabelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.height(32.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FireColors.Primary,
                                selectedLabelColor = Color.White,
                                containerColor = FireColors.SurfaceVariant.copy(alpha = 0.5f)
                            )
                        )
                    }
                }

                Divider()

                // Lista de Naturezas
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredSubNaturezas) { sub ->
                        val isSelected = sub.nome == selectedSubNaturezaName
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSubNaturezaSelected(sub)
                                    onDismiss()
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    FireColors.Primary.copy(alpha = 0.1f)
                                else
                                    Color.Transparent
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = if (isSelected)
                                BorderStroke(1.dp, FireColors.Primary)
                            else
                                null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val emoji = sub.categoria.split(" ").firstOrNull() ?: "📌"
                                Text(
                                    text = emoji,
                                    fontSize = 20.sp
                                )
                                Column {
                                    Text(
                                        text = sub.nome,
                                        style = FireTypography.BodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) FireColors.Primary else FireColors.OnSurface
                                    )
                                    Text(
                                        text = sub.categoria,
                                        style = FireTypography.LabelSmall,
                                        color = FireColors.OnSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = FireColors.Primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Rodapé
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = FireColors.OnSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ============================================
// MODAL DE AJUDA
// ============================================

@Composable
private fun HelpModal(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(
                containerColor = FireColors.Surface
            )
        ) {
            Column(
                modifier = Modifier.padding(FireSpacing.Large),
                verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
            ) {
                Text(
                    text = "📋 Como preencher a ocorrência",
                    style = FireTypography.Headline,
                    fontWeight = FontWeight.Bold,
                    color = FireColors.Primary,
                    fontSize = 18.sp
                )

                HelpItem(
                    icon = Icons.Default.Assignment,
                    title = "Identificação",
                    description = "Preencha o número do talão, data e hora do fato. A prontidão de serviço indica o status da viatura."
                )
                HelpItem(
                    icon = FireIcons.LocationOn,
                    title = "Localização",
                    description = "Use o GPS para obter coordenadas automáticas ou edite manualmente o endereço."
                )
                HelpItem(
                    icon = Icons.Default.Category,
                    title = "Natureza",
                    description = "Selecione a classificação da ocorrência baseada no tipo de atendimento."
                )

                HorizontalDivider()

                Text(
                    text = "⛔ Campos bloqueados após a criação da ocorrência não podem ser alterados.",
                    style = FireTypography.BodySmall,
                    color = FireColors.Warning,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Entendi", color = FireColors.Primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun HelpItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = FireColors.Primary.copy(alpha = 0.1f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = FireColors.Primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Column {
            Text(
                text = title,
                style = FireTypography.BodyMedium,
                fontWeight = FontWeight.Bold,
                color = FireColors.OnSurface
            )
            Text(
                text = description,
                style = FireTypography.BodySmall,
                color = FireColors.OnSurfaceVariant
            )
        }
    }
}

// ============================================
// UTILITÁRIOS
// ============================================

private fun formatTalao(input: String): String {
    // Remove tudo que não é número
    val clean = input.filter { it.isDigit() }

    return when {
        clean.isEmpty() -> ""
        clean.length <= 4 -> clean
        else -> clean.substring(0, 4) + "-" + clean.substring(4).take(5)
    }
}