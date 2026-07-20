package com.example.firenotes.ui.screens.occurrence

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firenotes.domain.model.DocumentType
import com.example.firenotes.ui.designsystem.colors.FireColors
import com.example.firenotes.ui.designsystem.components.buttons.FireButton
import com.example.firenotes.ui.designsystem.components.buttons.FireOutlinedButton
import com.example.firenotes.ui.designsystem.components.cards.FireCard
import com.example.firenotes.ui.designsystem.components.topbar.FireTopBar
import com.example.firenotes.ui.designsystem.spacing.FireSpacing
import com.example.firenotes.ui.designsystem.typography.FireTypography
import com.example.firenotes.ui.screens.occurrence.document.*
import androidx.compose.ui.text.font.FontWeight

@Composable
fun PersonIdentificationScreen(
    occurrenceId: String,
    documentId: String? = null,
    viewModel: PersonIdentificationViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.state.collectAsState()
    val context = LocalContext.current
    
    var phoneType by rememberSaveable { mutableStateOf("Celular") }
    
    LaunchedEffect(uiState.telefone) {
        val clean = uiState.telefone.filter { it.isDigit() }
        if (clean.length == 10) {
            if (phoneType == "Celular") {
                phoneType = "Fixo"
            }
        } else if (clean.length == 11) {
            phoneType = "Celular"
        }
    }
    
    LaunchedEffect(occurrenceId, documentId) {
        viewModel.setOccurrenceId(occurrenceId)
        if (documentId != null) {
            viewModel.loadDocumentForEditing(documentId)
        } else {
            viewModel.resetSelection()
        }
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
                title = "Identificar Pessoa",
                onBackClick = onNavigateBack,
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(FireSpacing.Medium),
                verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
            ) {
                Text(
                    text = "Tipo de Documento:",
                    style = FireTypography.Title,
                    fontWeight = FontWeight.Bold,
                    color = FireColors.OnSurface
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                ) {
                    val types = remember { listOf(DocumentType.RG, DocumentType.CIN, DocumentType.CNH, DocumentType.CPF) }
                    types.forEach { type ->
                        val isSelected = uiState.selectedType == type
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.selectDocumentType(type) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) FireColors.Primary.copy(alpha = 0.12f) else FireColors.Surface
                            ),
                            border = if (isSelected) BorderStroke(2.dp, FireColors.Primary) else BorderStroke(1.dp, FireColors.OnSurfaceVariant.copy(alpha = 0.1f)),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = FireSpacing.Small, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = type.icon,
                                    fontSize = 20.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = type.name,
                                    style = FireTypography.LabelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) FireColors.Primary else FireColors.OnSurface,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(FireSpacing.ExtraSmall))

                when (uiState.selectedType) {
                    DocumentType.RG -> RgIdentificationScreen(
                        state = uiState.rgState,
                        onStateChange = { viewModel.updateRgState(it) },
                        validationErrors = uiState.validationErrors,
                        onScanClick = { triggerOcr() }
                    )
                    DocumentType.CIN -> CinIdentificationScreen(
                        state = uiState.cinState,
                        onStateChange = { viewModel.updateCinState(it) },
                        validationErrors = uiState.validationErrors,
                        onScanClick = { triggerOcr() }
                    )
                    DocumentType.CNH -> CnhIdentificationScreen(
                        state = uiState.cnhState,
                        onStateChange = { viewModel.updateCnhState(it) },
                        validationErrors = uiState.validationErrors,
                        onScanClick = { triggerOcr() }
                    )
                    DocumentType.CPF -> CpfIdentificationScreen(
                        state = uiState.cpfState,
                        onStateChange = { viewModel.updateCpfState(it) },
                        validationErrors = uiState.validationErrors,
                        onScanClick = { triggerOcr() }
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
                    else -> {}
                }

                    // Card: Informações de Contato (Opcional)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = FireColors.SurfaceVariant.copy(alpha = 0.4f)),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(FireSpacing.Medium),
                            verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                        ) {
                            Text(
                                text = "📞 Informações de Contato (Opcional)",
                                style = FireTypography.Title,
                                fontWeight = FontWeight.Bold,
                                color = FireColors.Primary
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { 
                                        phoneType = "Celular"
                                        viewModel.updateTelefone(formatTelefone(uiState.telefone, true))
                                    }
                                ) {
                                    RadioButton(
                                        selected = phoneType == "Celular",
                                        onClick = { 
                                            phoneType = "Celular"
                                            viewModel.updateTelefone(formatTelefone(uiState.telefone, true))
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = FireColors.Primary)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Celular", style = FireTypography.BodyMedium)
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { 
                                        phoneType = "Fixo"
                                        viewModel.updateTelefone(formatTelefone(uiState.telefone, false))
                                    }
                                ) {
                                    RadioButton(
                                        selected = phoneType == "Fixo",
                                        onClick = { 
                                            phoneType = "Fixo"
                                            viewModel.updateTelefone(formatTelefone(uiState.telefone, false))
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = FireColors.Primary)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Fixo", style = FireTypography.BodyMedium)
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { 
                                        phoneType = "Serviço"
                                        viewModel.updateTelefone(formatTelefone(uiState.telefone, false))
                                    }
                                ) {
                                    RadioButton(
                                        selected = phoneType == "Serviço",
                                        onClick = { 
                                            phoneType = "Serviço"
                                            viewModel.updateTelefone(formatTelefone(uiState.telefone, false))
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = FireColors.Primary)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Serviço", style = FireTypography.BodyMedium)
                                }
                            }

                            OutlinedTextField(
                                value = uiState.telefone.filter { it.isDigit() },
                                onValueChange = { viewModel.updateTelefone(formatTelefone(it, phoneType == "Celular")) },
                                label = { Text("Telefone", style = FireTypography.BodyMedium) },
                                placeholder = { Text(if (phoneType == "Celular") "(11) 99999-9999" else "(11) 4444-4444", style = FireTypography.BodyMedium) },
                                visualTransformation = TelefoneVisualTransformation(phoneType == "Celular"),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = FireColors.Primary,
                                    unfocusedBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                            )
                            
                            OutlinedTextField(
                                value = uiState.email,
                                onValueChange = { viewModel.updateEmail(it) },
                                label = { Text("E-mail", style = FireTypography.BodyMedium) },
                                placeholder = { Text("exemplo@email.com", style = FireTypography.BodyMedium) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = FireColors.Primary,
                                    unfocusedBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                            )
                        }
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onNavigateBack() },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = FireColors.OnSurfaceVariant),
                            border = BorderStroke(1.dp, FireColors.OnSurfaceVariant.copy(alpha = 0.3f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("❌", fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cancelar", style = FireTypography.LabelMedium, maxLines = 1)
                        }
                        
                        OutlinedButton(
                            onClick = { viewModel.clearForm() },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = FireColors.OnSurfaceVariant),
                            border = BorderStroke(1.dp, FireColors.OnSurfaceVariant.copy(alpha = 0.3f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("🗑", fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Limpar", style = FireTypography.LabelMedium, maxLines = 1)
                        }
                        
                        Button(
                            onClick = {
                                viewModel.saveDocument {
                                    Toast.makeText(context, "Pessoa identificada com sucesso!", Toast.LENGTH_SHORT).show()
                                    onNavigateBack()
                                }
                            },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FireColors.Primary),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("💾", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Salvar", style = FireTypography.LabelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
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
