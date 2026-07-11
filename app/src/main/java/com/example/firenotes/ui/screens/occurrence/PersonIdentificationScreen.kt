package com.example.firenotes.ui.screens.occurrence

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.firenotes.domain.model.DocumentType
import com.example.firenotes.ui.designsystem.colors.FireColors
import com.example.firenotes.ui.designsystem.components.buttons.FireButton
import com.example.firenotes.ui.designsystem.components.buttons.FireOutlinedButton
import com.example.firenotes.ui.designsystem.components.cards.FireCard
import com.example.firenotes.ui.designsystem.components.topbar.FireTopBar
import com.example.firenotes.ui.designsystem.spacing.FireSpacing
import com.example.firenotes.ui.designsystem.typography.FireTypography
import com.example.firenotes.ui.screens.occurrence.document.*

@Composable
fun PersonIdentificationScreen(
    occurrenceId: String,
    viewModel: PersonIdentificationViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.state.collectAsState()
    val context = LocalContext.current
    
    LaunchedEffect(occurrenceId) {
        viewModel.setOccurrenceId(occurrenceId)
    }

    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            tempPhotoUri?.let { uri ->
                viewModel.processOcr(uri)
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val uri = viewModel.createPhotoUri()
            tempPhotoUri = uri
            takePictureLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Permissão de câmera negada.", Toast.LENGTH_SHORT).show()
        }
    }

    fun triggerOcr() {
        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    Scaffold(
        topBar = {
            FireTopBar(
                title = if (uiState.selectedType == null) "Identificar Pessoa" else "Identificar: ${uiState.selectedType?.displayName}",
                onBackClick = {
                    if (uiState.selectedType != null) {
                        viewModel.resetSelection()
                    } else {
                        onNavigateBack()
                    }
                },
                backgroundColor = FireColors.Surface,
                elevation = 2.dp
            )
        },
        containerColor = FireColors.Background,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(FireColors.Background)
        ) {
            if (uiState.selectedType == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(FireSpacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
                ) {
                    Text(
                        text = "Selecione o documento para identificação:",
                        style = FireTypography.Title,
                        modifier = Modifier.padding(bottom = FireSpacing.Small)
                    )
                    
                    DocumentType.entries.forEach { type ->
                        FireCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectDocumentType(type) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(FireSpacing.Medium),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = type.icon,
                                    style = FireTypography.Title,
                                    modifier = Modifier.padding(end = FireSpacing.Medium)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = type.displayName,
                                        style = FireTypography.BodyLarge,
                                        color = FireColors.OnSurface
                                    )
                                    Text(
                                        text = "Identificar envolvido através do preenchimento de ${type.displayName}.",
                                        style = FireTypography.BodySmall,
                                        color = FireColors.OnSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "➔",
                                    style = FireTypography.BodyLarge,
                                    color = FireColors.Primary
                                )
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(FireSpacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(FireSpacing.Large)
                ) {
                    FireCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(FireSpacing.Medium)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = uiState.selectedType!!.icon,
                                    style = FireTypography.Title,
                                    modifier = Modifier.padding(end = FireSpacing.Medium)
                                )
                                Column {
                                    Text(
                                        text = uiState.selectedType!!.displayName,
                                        style = FireTypography.Title
                                    )
                                    Text(
                                        text = "Preencha os campos abaixo ou faça a leitura automática por OCR.",
                                        style = FireTypography.BodyMedium
                                    )
                                }
                            }
                        }
                    }

                    FireButton(
                        text = "📷 Ler Documento (OCR)",
                        onClick = { triggerOcr() },
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(color = FireColors.OnSurfaceVariant.copy(alpha = 0.1f))

                    when (uiState.selectedType) {
                        DocumentType.RG -> RgIdentificationScreen(
                            state = uiState.rgState,
                            onStateChange = { viewModel.updateRgState(it) },
                            validationErrors = uiState.validationErrors
                        )
                        DocumentType.CIN -> CinIdentificationScreen(
                            state = uiState.cinState,
                            onStateChange = { viewModel.updateCinState(it) },
                            validationErrors = uiState.validationErrors
                        )
                        DocumentType.CNH -> CnhIdentificationScreen(
                            state = uiState.cnhState,
                            onStateChange = { viewModel.updateCnhState(it) },
                            validationErrors = uiState.validationErrors
                        )
                        DocumentType.CPF -> CpfIdentificationScreen(
                            state = uiState.cpfState,
                            onStateChange = { viewModel.updateCpfState(it) },
                            validationErrors = uiState.validationErrors
                        )
                        DocumentType.CRLV -> CrlvIdentificationScreen(
                            state = uiState.crlvState,
                            onStateChange = { viewModel.updateCrlvState(it) },
                            validationErrors = uiState.validationErrors
                        )
                        DocumentType.OAB -> OabIdentificationScreen(
                            state = uiState.oabState,
                            onStateChange = { viewModel.updateOabState(it) },
                            validationErrors = uiState.validationErrors
                        )
                        null -> {}
                    }

                    if (uiState.validationErrors.containsKey("global")) {
                        Text(
                            text = uiState.validationErrors["global"] ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = FireTypography.BodyMedium,
                            modifier = Modifier.padding(vertical = FireSpacing.Small)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
                    ) {
                        FireOutlinedButton(
                            text = "🗑 Limpar",
                            onClick = { viewModel.clearForm() },
                            modifier = Modifier.weight(1f)
                        )
                        FireButton(
                            text = "💾 Salvar",
                            onClick = {
                                viewModel.saveDocument {
                                    Toast.makeText(context, "Pessoa identificada com sucesso!", Toast.LENGTH_SHORT).show()
                                    onNavigateBack()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    FireOutlinedButton(
                        text = "❌ Cancelar",
                        onClick = { onNavigateBack() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (uiState.isOcrProcessing || uiState.isSaving) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CircularProgressIndicator(color = FireColors.Primary)
                    }
                }
            }
        }
    }
}
