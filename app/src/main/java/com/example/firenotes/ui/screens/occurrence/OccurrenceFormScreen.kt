package com.example.firenotes.ui.screens.occurrence

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firenotes.domain.repository.OcrDocumentResult
import com.example.firenotes.domain.model.*
import com.example.firenotes.ui.designsystem.colors.FireColors
import com.example.firenotes.ui.designsystem.shapes.FireShapes
import com.example.firenotes.ui.designsystem.spacing.FireSpacing
import com.example.firenotes.ui.designsystem.typography.FireTypography
import com.example.firenotes.ui.designsystem.icons.FireIcons
import com.example.firenotes.ui.designsystem.components.topbar.FireTopBar
import com.example.firenotes.ui.designsystem.components.buttons.*
import com.example.firenotes.ui.designsystem.components.cards.FireCard
import com.example.firenotes.ui.designsystem.components.dialogs.FireDialog
import com.example.firenotes.ui.designsystem.components.inputs.*
import com.example.firenotes.ui.designsystem.components.widgets.FireDivider
import com.example.firenotes.ui.designsystem.components.widgets.FireSectionHeader
import com.example.firenotes.ui.designsystem.components.widgets.FireStatusChip
import com.example.firenotes.ui.designsystem.states.FireLoading
import java.time.Instant

enum class OccurrenceModule {
    ENDERECO,
    VIATURAS,
    MILITARES,
    VEICULOS,
    DOCUMENTOS,
    VITIMAS,
    APOIOS,
    HISTORICO,
    EVIDENCIAS,
    ANEXOS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OccurrenceFormScreen(
    viewModel: OccurrenceFormViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var activeModule by remember { mutableStateOf<OccurrenceModule?>(null) }
    
    // Dialog control states
    var showAddDocDialog by remember { mutableStateOf(false) }
    var showAddVehicleDialog by remember { mutableStateOf(false) }
    var showAddVictimDialog by remember { mutableStateOf(false) }
    var showAddViaturaDialog by remember { mutableStateOf(false) }
    var showAddMilitarDialog by remember { mutableStateOf(false) }
    var showMoveMilitarDialog by remember { mutableStateOf(false) }
    
    var activeViaturaIdForMilitar by remember { mutableStateOf("") }
    var activeMilitarToMove by remember { mutableStateOf<Militar?>(null) }
    
    var ocrResultData by remember { mutableStateOf<OcrDocumentResult?>(null) }
    var crlvImageUri by remember { mutableStateOf<Uri?>(null) }
    
    // Pickers and Launchers
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var activeOcrScanType by remember { mutableStateOf("") } // "DOCUMENTO" or "CRLV"

    var selectedNaturezaForCreation by remember { mutableStateOf(NaturezaOcorrencia.PESSOAL) }
    var isGpsMethod by remember { mutableStateOf(true) }

    // GPS permissions launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            viewModel.captureLocationAndAddress()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { errorMsg ->
            if (errorMsg.contains("GPS") || errorMsg.contains("Erro")) {
                val result = snackbarHostState.showSnackbar(
                    message = "Não foi possível obter sua localização.",
                    actionLabel = "Tentar novamente",
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            }
        }
    }

    LaunchedEffect(isGpsMethod, uiState.formStage) {
        if (isGpsMethod && uiState.formStage == FormStage.INITIAL_DATA && uiState.latitude == null) {
            val fineCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val coarseCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (fineCheck || coarseCheck) {
                viewModel.captureLocationAndAddress()
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    // TakePicture contract launcher for direct OCR
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            val uri = tempPhotoUri ?: return@rememberLauncherForActivityResult
            viewModel.processAndRunOcrDirectly(
                imageUri = uri,
                onSuccess = { result, processedUri ->
                    ocrResultData = result
                    if (activeOcrScanType == "CRLV") {
                        crlvImageUri = processedUri
                        showAddVehicleDialog = true
                    } else {
                        tempPhotoUri = processedUri
                        showAddDocDialog = true
                    }
                }
            )
        }
    }

    // File picker for general media attachments
    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.uploadOccurrenceFile(it, isVideo = false)
        }
    }

    // Camera launcher for Evidence
    var evidencePhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showClassificationDialog by remember { mutableStateOf(false) }
    val evidenceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            showClassificationDialog = true
        }
    }

    val autoSaveAndCloseModule: () -> Unit = {
        viewModel.finalizeOccurrence()
        activeModule = null
    }

    Scaffold(
        topBar = {
            FireTopBar(
                title = if (activeModule == null) {
                    if (uiState.formStage == FormStage.INITIAL_DATA) "Dados Iniciais" else "Ocorrência: ${uiState.protocolo}"
                } else {
                    "Módulo: ${activeModule!!.name.lowercase().replaceFirstChar { it.uppercase() }}"
                },
                onBackClick = {
                    if (activeModule != null) {
                        autoSaveAndCloseModule()
                    } else {
                        onNavigateBack()
                    }
                },
                actions = {
                    if (activeModule == null && uiState.formStage == FormStage.TABS) {
                        FireIconButton(
                            icon = FireIcons.Share,
                            onClick = { /* Share PDF action */ }
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            // Contextual FAB depending on active module
            if (activeModule != null) {
                when (activeModule) {
                    OccurrenceModule.VEICULOS -> {
                        FireFAB(
                            icon = FireIcons.Add,
                            onClick = {
                                ocrResultData = null
                                crlvImageUri = null
                                showAddVehicleDialog = true
                            }
                        )
                    }
                    OccurrenceModule.DOCUMENTOS -> {
                        FireFAB(
                            icon = FireIcons.Add,
                            onClick = {
                                ocrResultData = null
                                showAddDocDialog = true
                            }
                        )
                    }
                    OccurrenceModule.VITIMAS -> {
                        FireFAB(
                            icon = FireIcons.Add,
                            onClick = { showAddVictimDialog = true }
                        )
                    }
                    OccurrenceModule.VIATURAS -> {
                        FireFAB(
                            icon = FireIcons.Add,
                            onClick = { showAddViaturaDialog = true }
                        )
                    }
                    OccurrenceModule.EVIDENCIAS -> {
                        FireFAB(
                            icon = FireIcons.LocationOn,
                            onClick = {
                                val uri = viewModel.createPhotoUri()
                                evidencePhotoUri = uri
                                evidenceLauncher.launch(uri)
                            }
                        )
                    }
                    OccurrenceModule.ANEXOS -> {
                        FireFAB(
                            icon = FireIcons.CloudUpload,
                            onClick = { mediaPicker.launch("image/*") }
                        )
                    }
                    else -> {}
                }
            }
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
            when (uiState.formStage) {
                FormStage.INITIAL_DATA, FormStage.NATURE_SELECTION -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = FireSpacing.MediumLarge, vertical = FireSpacing.Large),
                        verticalArrangement = Arrangement.spacedBy(FireSpacing.MediumLarge)
                    ) {
                        FireSectionHeader(
                            title = "Dados Iniciais da Ocorrência",
                            icon = "📋📋",
                            subtitle = "Identifique o atendimento para iniciar o dashboard modular."
                        )

                        // CARD 1: Identificação da Ocorrência
                        FireCard(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = 2.dp,
                            containerColor = FireColors.Surface
                        ) {
                            Column(
                                modifier = Modifier.padding(FireSpacing.Medium),
                                verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
                            ) {
                                Text(
                                    text = "Identificação do Registro",
                                    style = FireTypography.Title,
                                    fontWeight = FontWeight.Bold,
                                    color = FireColors.Primary
                                )

                                FireOutlinedTextField(
                                    value = uiState.protocolo,
                                    onValueChange = { viewModel.updateInitialFields(it, uiState.data, uiState.hora) },
                                    label = "Número do Talão da Ocorrência",
                                    placeholder = { Text("Ex: 2026-A12", style = FireTypography.BodyMedium) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = FireIcons.AddAlert,
                                            contentDescription = "Número do Talão",
                                            tint = FireColors.Primary
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
                                ) {
                                    FireDatePicker(
                                        value = uiState.data,
                                        onDateSelected = { viewModel.updateInitialFields(uiState.protocolo, it, uiState.hora) },
                                        label = "Data",
                                        modifier = Modifier.weight(1f)
                                    )
                                    FireTimePicker(
                                        value = uiState.hora,
                                        onTimeSelected = { viewModel.updateInitialFields(uiState.protocolo, uiState.data, it) },
                                        label = "Hora",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // CARD 2: Localização
                        FireCard(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = 2.dp,
                            containerColor = FireColors.Surface
                        ) {
                            Column(
                                modifier = Modifier.padding(FireSpacing.Medium),
                                verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
                            ) {
                                Text(
                                    text = "Localização Geográfica",
                                    style = FireTypography.Title,
                                    fontWeight = FontWeight.Bold,
                                    color = FireColors.Primary
                                )

                                FireButton(
                                    text = "Capturar Localização",
                                    onClick = {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    },
                                    containerColor = FireColors.Secondary,
                                    icon = FireIcons.LocationOn,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (uiState.isGpsLoading) {
                                    FireLoading()
                                }

                                if (uiState.latitude != null) {
                                    Text(
                                        text = "Coordenadas: Lat ${"%.5f".format(uiState.latitude)} | Lng ${"%.5f".format(uiState.longitude)}",
                                        style = FireTypography.LabelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = FireColors.Secondary
                                    )
                                }

                                FireOutlinedTextField(
                                    value = uiState.rua,
                                    onValueChange = { viewModel.updateManualAddress(it, uiState.numero, uiState.bairro, uiState.cidade, uiState.uf) },
                                    label = "Rua/Avenida"
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
                                ) {
                                    FireOutlinedTextField(
                                        value = uiState.numero,
                                        onValueChange = { viewModel.updateManualAddress(uiState.rua, it, uiState.bairro, uiState.cidade, uiState.uf) },
                                        label = "Número",
                                        modifier = Modifier.weight(1f)
                                    )
                                    FireOutlinedTextField(
                                        value = uiState.bairro,
                                        onValueChange = { viewModel.updateManualAddress(uiState.rua, uiState.numero, it, uiState.cidade, uiState.uf) },
                                        label = "Bairro",
                                        modifier = Modifier.weight(2f)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
                                ) {
                                    FireOutlinedTextField(
                                        value = uiState.cidade,
                                        onValueChange = { viewModel.updateManualAddress(uiState.rua, uiState.numero, uiState.bairro, it, uiState.uf) },
                                        label = "Cidade",
                                        modifier = Modifier.weight(3f)
                                    )
                                    
                                    val listUFs = remember {
                                        listOf(
                                            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", 
                                            "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", 
                                            "RS", "RO", "RR", "SC", "SP", "SE", "TO"
                                        )
                                    }
                                    FireDropdown(
                                        selectedOption = uiState.uf,
                                        options = listUFs,
                                        onOptionSelected = { viewModel.updateManualAddress(uiState.rua, uiState.numero, uiState.bairro, uiState.cidade, it) },
                                        label = "UF",
                                        modifier = Modifier.weight(2f)
                                    )
                                }
                            }
                        }

                        // CARD 3: Natureza
                        FireCard(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = 2.dp,
                            containerColor = FireColors.Surface
                        ) {
                            Column(
                                modifier = Modifier.padding(FireSpacing.Medium),
                                verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
                            ) {
                                Text(
                                    text = "Classificação da Ocorrência",
                                    style = FireTypography.Title,
                                    fontWeight = FontWeight.Bold,
                                    color = FireColors.Primary
                                )

                                val natureOptions = remember {
                                    NaturezaOcorrencia.entries.map { it.descricao }
                                }
                                val natureIcons = remember {
                                    mapOf(
                                        NaturezaOcorrencia.INCENDIO.descricao to "🔥🔥",
                                        NaturezaOcorrencia.PESSOAL.descricao to "🚑🚑",
                                        NaturezaOcorrencia.SALVAMENTO.descricao to "🚒🚒",
                                        NaturezaOcorrencia.QUEDA.descricao to "🌳🌳",
                                        NaturezaOcorrencia.ACIDENTE_TRANSITO.descricao to "🚗🚗"
                                    )
                                }

                                FireSearchableDropdown(
                                    selectedOption = selectedNaturezaForCreation.descricao,
                                    options = natureOptions,
                                    onOptionSelected = { desc ->
                                        selectedNaturezaForCreation = NaturezaOcorrencia.entries.first { it.descricao == desc }
                                    },
                                    label = "Natureza do Acidente",
                                    optionIcons = natureIcons,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(FireSpacing.Medium))

                        FireButton(
                            text = "CRIAR OCORRÊNCIA",
                            onClick = {
                                viewModel.selectNaturezaAndCreateOccurrence(selectedNaturezaForCreation)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                FormStage.TABS -> {
                    // Modular Dashboard layout
                    AnimatedVisibility(
                        visible = activeModule == null,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        ModularDashboardView(
                            uiState = uiState,
                            onModuleSelected = { activeModule = it },
                            onFinishClick = onNavigateBack
                        )
                    }

                    // Individual modules pages loaded dynamically
                    if (activeModule != null) {
                        AnimatedVisibility(
                            visible = activeModule != null,
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(FireColors.Background)
                            ) {
                                when (activeModule) {
                                    OccurrenceModule.ENDERECO -> {
                                        AddressModuleView(
                                            uiState = uiState,
                                            onAddressChanged = viewModel::updateManualAddress,
                                            onFetchGps = {
                                                locationPermissionLauncher.launch(
                                                    arrayOf(
                                                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                                                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                                                    )
                                                )
                                            },
                                            onBack = autoSaveAndCloseModule
                                        )
                                    }
                                    OccurrenceModule.VIATURAS -> {
                                        ViaturasModuleView(
                                            uiState = uiState,
                                            onNewViaturaClick = { showAddViaturaDialog = true },
                                            onDeleteViatura = viewModel::deleteViatura,
                                            onBack = autoSaveAndCloseModule
                                        )
                                    }
                                    OccurrenceModule.MILITARES -> {
                                        MilitaresModuleView(
                                            uiState = uiState,
                                            onAddMilitarClick = {
                                                activeViaturaIdForMilitar = it
                                                showAddMilitarDialog = true
                                            },
                                            onDeleteMilitar = viewModel::deleteMilitar,
                                            onMoveMilitarClick = {
                                                activeMilitarToMove = it
                                                showMoveMilitarDialog = true
                                            },
                                            onBack = autoSaveAndCloseModule
                                        )
                                    }
                                    OccurrenceModule.VEICULOS -> {
                                        VeiculosModuleView(
                                            uiState = uiState,
                                            onNewVehicleClick = {
                                                ocrResultData = null
                                                crlvImageUri = null
                                                showAddVehicleDialog = true
                                            },
                                            onScanCrlvClick = {
                                                val newUri = viewModel.createPhotoUri()
                                                tempPhotoUri = newUri
                                                activeOcrScanType = "CRLV"
                                                takePictureLauncher.launch(newUri)
                                            },
                                            onBack = autoSaveAndCloseModule
                                        )
                                    }
                                    OccurrenceModule.DOCUMENTOS -> {
                                        DocumentosModuleView(
                                            uiState = uiState,
                                            onNewDocClick = {
                                                ocrResultData = null
                                                showAddDocDialog = true
                                            },
                                            onScanDocClick = {
                                                val newUri = viewModel.createPhotoUri()
                                                tempPhotoUri = newUri
                                                activeOcrScanType = "DOCUMENTO"
                                                takePictureLauncher.launch(newUri)
                                            },
                                            onBack = autoSaveAndCloseModule
                                        )
                                    }
                                    OccurrenceModule.VITIMAS -> {
                                        VitimasModuleView(
                                            uiState = uiState,
                                            onNewVictimClick = { showAddVictimDialog = true },
                                            onBack = autoSaveAndCloseModule
                                        )
                                    }
                                    OccurrenceModule.APOIOS -> {
                                        ApoiosModuleView(
                                            uiState = uiState,
                                            onAddApoio = viewModel::addApoio,
                                            onRemoveApoio = viewModel::removeApoio,
                                            onBack = autoSaveAndCloseModule
                                        )
                                    }
                                    OccurrenceModule.HISTORICO -> {
                                        HistoricoModuleView(
                                            uiState = uiState,
                                            onHistoryChanged = viewModel::updateHistorico,
                                            onBack = autoSaveAndCloseModule
                                        )
                                    }
                                    OccurrenceModule.EVIDENCIAS -> {
                                        EvidenciasModuleView(
                                            uiState = uiState,
                                            onTakePhoto = {
                                                val uri = viewModel.createPhotoUri()
                                                evidencePhotoUri = uri
                                                evidenceLauncher.launch(uri)
                                            },
                                            onBack = autoSaveAndCloseModule
                                        )
                                    }
                                    OccurrenceModule.ANEXOS -> {
                                        AnexosModuleView(
                                            uiState = uiState,
                                            onAddMedia = { mediaPicker.launch("image/*") },
                                            onBack = autoSaveAndCloseModule
                                        )
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Add Document
    if (showAddDocDialog) {
        AddDocumentDialog(
            ocrResult = ocrResultData,
            onDismiss = { showAddDocDialog = false },
            onConfirm = { tipo, numero, fields, rawText, uri ->
                val finalUri = tempPhotoUri ?: uri
                viewModel.saveDocument(tipo, numero, fields, rawText, finalUri)
                showAddDocDialog = false
            }
        )
    }

    // Dialog: Add Vehicle
    if (showAddVehicleDialog) {
        AddVehicleDialog(
            ocrResult = ocrResultData,
            pessoasDisponiveis = uiState.pessoas,
            crlvImage = crlvImageUri,
            onDismiss = { showAddVehicleDialog = false },
            onConfirm = { placa, modelo, cor, chassi, ano, propId, fields, rawText, crlvUri ->
                viewModel.saveVeiculo(placa, modelo, cor, chassi, ano, propId, fields, rawText, crlvUri)
                showAddVehicleDialog = false
            }
        )
    }

    // Dialog: Add Victim
    if (showAddVictimDialog) {
        AddVictimDialogV2(
            pessoasDisponiveis = uiState.pessoas,
            viaturasDisponiveis = uiState.viaturas,
            calculateAge = viewModel::calculateAge,
            onDismiss = { showAddVictimDialog = false },
            onConfirm = { pId, lesoes, destino, quemSocorreu, resultado, fc, pa, satO2, temp, gcs, vSocorroId, hDestino, transPor ->
                viewModel.saveVitima(pId, lesoes, destino, quemSocorreu, resultado, fc, pa, satO2, temp, gcs, vSocorroId, hDestino, transPor)
                showAddVictimDialog = false
            }
        )
    }

    // Dialog: Add Viatura
    if (showAddViaturaDialog) {
        AddViaturaDialog(
            onDismiss = { showAddViaturaDialog = false },
            onConfirm = { prefixo, tipo, unidade, kmSaida, kmLocal, obs ->
                viewModel.addViatura(prefixo, tipo, unidade, kmSaida, kmLocal, obs)
                showAddViaturaDialog = false
            }
        )
    }

    // Dialog: Add Militar
    if (showAddMilitarDialog) {
        AddMilitarDialog(
            onDismiss = { showAddMilitarDialog = false },
            onConfirm = { re, nome, grad, func ->
                viewModel.addMilitar(activeViaturaIdForMilitar, re, nome, grad, func)
                showAddMilitarDialog = false
            }
        )
    }

    // Dialog: Move Militar
    if (showMoveMilitarDialog && activeMilitarToMove != null) {
        MoveMilitarDialog(
            militar = activeMilitarToMove!!,
            viaturas = uiState.viaturas,
            onDismiss = { showMoveMilitarDialog = false },
            onConfirm = { newViaturaId ->
                viewModel.moveMilitar(activeMilitarToMove!!.id!!, activeMilitarToMove!!.viaturaId!!, newViaturaId)
                showMoveMilitarDialog = false
            }
        )
    }

    // Scene Photo Classification Dialog
    if (showClassificationDialog) {
        FireDialog(
            onDismissRequest = { showClassificationDialog = false },
            title = "Classificar Evidência",
            confirmButton = {}
        ) {
            val types = listOf("Documento", "Veículo", "Vítima", "Local", "Evidência", "Outro")
            Column(verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)) {
                types.forEach { type ->
                    FireButton(
                        text = type,
                        onClick = {
                            evidencePhotoUri?.let { uri ->
                                viewModel.addEvidencia(uri, type)
                            }
                            showClassificationDialog = false
                        },
                        containerColor = FireColors.Primary,
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    )
                }
                Spacer(modifier = Modifier.height(FireSpacing.ExtraSmall))
                FireTextButton(
                    text = "Cancelar",
                    onClick = { showClassificationDialog = false },
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun ModularDashboardView(
    uiState: OccurrenceFormUiState,
    onModuleSelected: (OccurrenceModule) -> Unit,
    onFinishClick: () -> Unit
) {
    val context = LocalContext.current
    val tempoOcorrencia = remember(uiState.data, uiState.hora) {
        try {
            val dateStr = "${uiState.data} ${uiState.hora}"
            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            val localDateTime = java.time.LocalDateTime.parse(dateStr, formatter)
            val created = localDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant()
            val duration = java.time.Duration.between(created, java.time.Instant.now())
            val hours = duration.toHours()
            val minutes = duration.toMinutes() % 60
            "${hours}h ${minutes}min"
        } catch (e: Exception) {
            "N/D"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(FireSpacing.Medium),
        verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
    ) {
        // Upper Resumo Card
        FireCard(
            containerColor = FireColors.Surface,
            elevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(FireSpacing.Medium),
                verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
            ) {
                Text(
                    text = "Resumo da Ocorrência",
                    style = FireTypography.Title,
                    fontWeight = FontWeight.Bold,
                    color = FireColors.Primary
                )
                FireDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Talão: ${uiState.protocolo}", style = FireTypography.BodyMedium)
                    Text("Natureza: ${uiState.natureza.descricao}", style = FireTypography.BodyMedium)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Cidade: ${uiState.cidade}", style = FireTypography.BodyMedium)
                    Text("Duração: $tempoOcorrencia", style = FireTypography.BodyMedium)
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = FireSpacing.Small),
                    horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                ) {
                    FireStatusChip("Veículos: ${uiState.veiculos.size}", FireColors.PrimaryLight, FireColors.Primary)
                    FireStatusChip("Vítimas: ${uiState.vitimas.size}", FireColors.SecondaryLight, FireColors.Secondary)
                    FireStatusChip("Viaturas: ${uiState.viaturas.size}", FireColors.TertiaryLight, FireColors.PrimaryDark)
                }
            }
        }

        // Checklist Operacional Row
        FireCard(containerColor = FireColors.Surface) {
            Column(modifier = Modifier.padding(FireSpacing.Medium)) {
                Text("Checklist Operacional", style = FireTypography.LabelLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(FireSpacing.ExtraSmall))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FireSpacing.ExtraSmall)
                ) {
                    val statusText = { cond: Boolean -> if (cond) "✔" else "○" }
                    val color = { cond: Boolean -> if (cond) FireColors.Secondary else Color.Gray }
                    
                    Text("Talão ${statusText(uiState.protocolo.isNotBlank())}", color = color(uiState.protocolo.isNotBlank()), style = FireTypography.BodySmall)
                    Text("•", color = Color.Gray, style = FireTypography.BodySmall)
                    Text("Endereço ${statusText(uiState.rua.isNotBlank())}", color = color(uiState.rua.isNotBlank()), style = FireTypography.BodySmall)
                    Text("•", color = Color.Gray, style = FireTypography.BodySmall)
                    Text("Histórico ${statusText(uiState.historico.isNotBlank())}", color = color(uiState.historico.isNotBlank()), style = FireTypography.BodySmall)
                    Text("•", color = Color.Gray, style = FireTypography.BodySmall)
                    Text("Viaturas ${statusText(uiState.viaturas.isNotEmpty())}", color = color(uiState.viaturas.isNotEmpty()), style = FireTypography.BodySmall)
                }
            }
        }

        // Grid of Module Cards
        val modulesList = listOf(
            ModuleInfo("Endereço", "📍📍", 1, uiState.rua.ifBlank { "Nenhum endereço" }, calculateStatus(OccurrenceModule.ENDERECO, uiState), onModuleSelected),
            ModuleInfo("Viaturas", "🚒🚒", uiState.viaturas.size, "${uiState.viaturas.size} viaturas", calculateStatus(OccurrenceModule.VIATURAS, uiState), onModuleSelected),
            ModuleInfo("Militares", "👮👮", uiState.viaturas.sumOf { it.equipe.size }, "${uiState.viaturas.sumOf { it.equipe.size }} militares", calculateStatus(OccurrenceModule.MILITARES, uiState), onModuleSelected),
            ModuleInfo("Veículos", "🚗🚗", uiState.veiculos.size, "${uiState.veiculos.size} veículos", calculateStatus(OccurrenceModule.VEICULOS, uiState), onModuleSelected),
            ModuleInfo("Documentos", "🪪🪪", uiState.documentos.size, "${uiState.documentos.size} documentos", calculateStatus(OccurrenceModule.DOCUMENTOS, uiState), onModuleSelected),
            ModuleInfo("Vítimas", "🩺🩺", uiState.vitimas.size, "${uiState.vitimas.size} vítimas", calculateStatus(OccurrenceModule.VITIMAS, uiState), onModuleSelected),
            ModuleInfo("Apoios", "🤝🤝", uiState.apoiosDetalhados.size, "${uiState.apoiosDetalhados.size} apoios", calculateStatus(OccurrenceModule.APOIOS, uiState), onModuleSelected),
            ModuleInfo("Histórico", "📝📝", if (uiState.historico.isNotEmpty()) 1 else 0, if (uiState.historico.isNotEmpty()) "Preenchido" else "Não iniciado", calculateStatus(OccurrenceModule.HISTORICO, uiState), onModuleSelected),
            ModuleInfo("Evidências", "📷📷", uiState.evidencias.size, "${uiState.evidencias.size} evidências", calculateStatus(OccurrenceModule.EVIDENCIAS, uiState), onModuleSelected),
            ModuleInfo("Anexos", "📄📄", uiState.fotos.size + uiState.videos.size, "${uiState.fotos.size + uiState.videos.size} mídias", calculateStatus(OccurrenceModule.ANEXOS, uiState), onModuleSelected)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp),
            horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
        ) {
            items(modulesList) { item ->
                DashboardCard(item)
            }
        }

        FireButton(
            text = "CONCLUIR OCORRÊNCIA",
            onClick = onFinishClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DashboardCard(info: ModuleInfo) {
    val occurrenceModule = when(info.title) {
        "Endereço" -> OccurrenceModule.ENDERECO
        "Viaturas" -> OccurrenceModule.VIATURAS
        "Militares" -> OccurrenceModule.MILITARES
        "Veículos" -> OccurrenceModule.VEICULOS
        "Documentos" -> OccurrenceModule.DOCUMENTOS
        "Vítimas" -> OccurrenceModule.VITIMAS
        "Apoios" -> OccurrenceModule.APOIOS
        "Histórico" -> OccurrenceModule.HISTORICO
        "Evidências" -> OccurrenceModule.EVIDENCIAS
        else -> OccurrenceModule.ANEXOS
    }

    FireCard(
        onClick = { info.onSelected(occurrenceModule) },
        containerColor = FireColors.Surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(FireSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(FireSpacing.ExtraSmall)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(info.icon, fontSize = 24.sp)
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(info.statusColor, FireShapes.Circle)
                )
            }
            Text(info.title, style = FireTypography.Title, fontWeight = FontWeight.Bold)
            Text(info.summary, style = FireTypography.BodySmall, color = Color.Gray, maxLines = 1)
            Text(
                text = info.statusText,
                style = FireTypography.LabelMedium,
                color = info.statusColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private data class ModuleInfo(
    val title: String,
    val icon: String,
    val count: Int,
    val summary: String,
    val status: Pair<String, Color>,
    val onSelected: (OccurrenceModule) -> Unit
) {
    val statusText = status.first
    val statusColor = status.second
}

private fun calculateStatus(module: OccurrenceModule, state: OccurrenceFormUiState): Pair<String, Color> {
    val gray = Color(0xFF757575)
    val blue = Color(0xFF1976D2)
    val green = Color(0xFF2E7D32)
    val orange = Color(0xFFF57C00)
    val red = Color(0xFFD32F2F)

    return when (module) {
        OccurrenceModule.ENDERECO -> {
            if (state.rua.isBlank() && state.cidade.isBlank()) {
                "Pendente" to red
            } else if (state.numero.isBlank() || state.bairro.isBlank()) {
                "Em andamento" to blue
            } else {
                "Concluído" to green
            }
        }
        OccurrenceModule.VIATURAS -> {
            if (state.viaturas.isEmpty()) {
                "Não iniciado" to gray
            } else {
                "Concluído" to green
            }
        }
        OccurrenceModule.MILITARES -> {
            val totalMilitares = state.viaturas.sumOf { it.equipe.size }
            if (state.viaturas.isEmpty()) {
                "Não iniciado" to gray
            } else if (totalMilitares == 0) {
                "Sem equipe" to orange
            } else {
                "Concluído" to green
            }
        }
        OccurrenceModule.VEICULOS -> {
            if (state.veiculos.isEmpty()) {
                "Não iniciado" to gray
            } else {
                "Concluído" to green
            }
        }
        OccurrenceModule.DOCUMENTOS -> {
            if (state.documentos.isEmpty()) {
                "Não iniciado" to gray
            } else {
                "Concluído" to green
            }
        }
        OccurrenceModule.VITIMAS -> {
            if (state.vitimas.isEmpty()) {
                "Não iniciado" to gray
            } else {
                "Concluído" to green
            }
        }
        OccurrenceModule.APOIOS -> {
            if (state.apoiosDetalhados.isEmpty()) {
                "Não iniciado" to gray
            } else {
                "Concluído" to green
            }
        }
        OccurrenceModule.HISTORICO -> {
            if (state.historico.isBlank()) {
                "Pendente" to red
            } else if (state.historico.length < 50) {
                "Revisar" to orange
            } else {
                "Concluído" to green
            }
        }
        OccurrenceModule.EVIDENCIAS -> {
            if (state.evidencias.isEmpty()) {
                "Não iniciado" to gray
            } else {
                "Concluído" to green
            }
        }
        OccurrenceModule.ANEXOS -> {
            if (state.fotos.isEmpty() && state.videos.isEmpty()) {
                "Não iniciado" to gray
            } else {
                "Concluído" to green
            }
        }
    }
}

// --- Module Content Views ---

@Composable
private fun AddressModuleView(
    uiState: OccurrenceFormUiState,
    onAddressChanged: (String, String, String, String, String) -> Unit,
    onFetchGps: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(FireSpacing.Medium),
        verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
    ) {
        FireSectionHeader(title = "Localização da Ocorrência", icon = "📍📍")
        
        FireButton(
            text = "Capturar Localização",
            onClick = onFetchGps,
            containerColor = FireColors.Secondary,
            icon = FireIcons.LocationOn,
            modifier = Modifier.fillMaxWidth()
        )

        if (uiState.isGpsLoading) {
            FireLoading()
        }

        if (uiState.latitude != null) {
            Text(
                text = "Coordenadas: Lat ${"%.5f".format(uiState.latitude)} | Lng ${"%.5f".format(uiState.longitude)}",
                style = FireTypography.LabelMedium,
                color = FireColors.Secondary,
                fontWeight = FontWeight.Bold
            )
        }

        FireOutlinedTextField(
            value = uiState.rua,
            onValueChange = { onAddressChanged(it, uiState.numero, uiState.bairro, uiState.cidade, uiState.uf) },
            label = "Rua/Avenida"
        )
        Row(horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium)) {
            FireOutlinedTextField(
                value = uiState.numero,
                onValueChange = { onAddressChanged(uiState.rua, it, uiState.bairro, uiState.cidade, uiState.uf) },
                label = "Número",
                modifier = Modifier.weight(1f)
            )
            FireOutlinedTextField(
                value = uiState.bairro,
                onValueChange = { onAddressChanged(uiState.rua, uiState.numero, it, uiState.cidade, uiState.uf) },
                label = "Bairro",
                modifier = Modifier.weight(2f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium)) {
            FireOutlinedTextField(
                value = uiState.cidade,
                onValueChange = { onAddressChanged(uiState.rua, uiState.numero, uiState.bairro, it, uiState.uf) },
                label = "Cidade",
                modifier = Modifier.weight(3f)
            )
            val listUFs = remember {
                listOf(
                    "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", 
                    "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", 
                    "RS", "RO", "RR", "SC", "SP", "SE", "TO"
                )
            }
            FireDropdown(
                selectedOption = uiState.uf,
                options = listUFs,
                onOptionSelected = { onAddressChanged(uiState.rua, uiState.numero, uiState.bairro, uiState.cidade, it) },
                label = "UF",
                modifier = Modifier.weight(2f)
            )
        }

        FireButton(text = "Voltar ao Dashboard", onClick = onBack, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ViaturasModuleView(
    uiState: OccurrenceFormUiState,
    onNewViaturaClick: () -> Unit,
    onDeleteViatura: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(FireSpacing.Medium)
    ) {
        FireButton(onClick = onNewViaturaClick, text = "+ Adicionar Viatura", modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(FireSpacing.Medium))
        
        if (uiState.viaturas.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Nenhuma viatura registrada.", style = FireTypography.BodyMedium, color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
            ) {
                items(uiState.viaturas) { viatura ->
                    FireCard {
                        Row(
                            modifier = Modifier.padding(FireSpacing.Medium),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("🚒 Viatura: ${viatura.prefixo}", style = FireTypography.Title, fontWeight = FontWeight.Bold)
                                Text("Tipo: ${viatura.tipo} | KM Saída: ${viatura.kmSaida ?: "N/D"}", style = FireTypography.Caption)
                            }
                            FireIconButton(icon = FireIcons.Delete, onClick = { onDeleteViatura(viatura.id!!) }, tint = FireColors.Error)
                        }
                    }
                }
            }
        }
        FireButton(text = "Voltar ao Dashboard", onClick = onBack, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun MilitaresModuleView(
    uiState: OccurrenceFormUiState,
    onAddMilitarClick: (String) -> Unit,
    onDeleteMilitar: (String, String) -> Unit,
    onMoveMilitarClick: (Militar) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(FireSpacing.Medium)
    ) {
        if (uiState.viaturas.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Cadastre uma viatura no módulo anterior antes de escalar militares.", style = FireTypography.BodyMedium, color = Color.Gray, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
            ) {
                items(uiState.viaturas) { viatura ->
                    FireCard {
                        Column(modifier = Modifier.padding(FireSpacing.Medium)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Equipe Viatura ${viatura.prefixo}", style = FireTypography.Title, fontWeight = FontWeight.Bold)
                                FireIconButton(icon = FireIcons.Add, onClick = { onAddMilitarClick(viatura.id!!) })
                            }
                            FireDivider()
                            if (viatura.equipe.isEmpty()) {
                                Text("Nenhum militar escalado.", style = FireTypography.Caption, color = Color.Gray)
                            } else {
                                viatura.equipe.forEach { mil ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = FireSpacing.ExtraSmall),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${mil.graduacao.descricao} ${mil.nomeGuerra} [${mil.funcao ?: "Equipe"}]", style = FireTypography.BodyMedium)
                                        Row {
                                            FireTextButton(text = "Mover", onClick = { onMoveMilitarClick(mil) })
                                            FireIconButton(icon = FireIcons.Delete, onClick = { onDeleteMilitar(mil.id!!, viatura.id!!) }, tint = FireColors.Error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        FireButton(text = "Voltar ao Dashboard", onClick = onBack, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun VeiculosModuleView(
    uiState: OccurrenceFormUiState,
    onNewVehicleClick: () -> Unit,
    onScanCrlvClick: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(FireSpacing.Medium)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small)
        ) {
            FireButton(text = "Digitar Manual", onClick = onNewVehicleClick, modifier = Modifier.weight(1f))
            FireButton(text = "Escanear CRLV", onClick = onScanCrlvClick, containerColor = FireColors.Secondary, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(FireSpacing.Medium))

        if (uiState.veiculos.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Nenhum veículo registrado.", style = FireTypography.BodyMedium, color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
            ) {
                items(uiState.veiculos) { veiculo ->
                    FireCard {
                        Column(modifier = Modifier.padding(FireSpacing.Medium)) {
                            Text("🚗 Placa: ${veiculo.placa ?: "SEM PLACA"}", style = FireTypography.Title, fontWeight = FontWeight.Bold)
                            Text("Modelo: ${veiculo.modelo} | Cor: ${veiculo.cor}", style = FireTypography.BodyMedium)
                        }
                    }
                }
            }
        }
        FireButton(text = "Voltar ao Dashboard", onClick = onBack, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun DocumentosModuleView(
    uiState: OccurrenceFormUiState,
    onNewDocClick: () -> Unit,
    onScanDocClick: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(FireSpacing.Medium)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small)
        ) {
            FireButton(text = "Digitar Manual", onClick = onNewDocClick, modifier = Modifier.weight(1f))
            FireButton(text = "Escanear Doc", onClick = onScanDocClick, containerColor = FireColors.Secondary, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(FireSpacing.Medium))

        if (uiState.documentos.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Nenhum documento registrado.", style = FireTypography.BodyMedium, color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
            ) {
                items(uiState.documentos) { doc ->
                    FireCard {
                        Column(modifier = Modifier.padding(FireSpacing.Medium)) {
                            Text("🪪 Tipo: ${doc.tipo}", style = FireTypography.Title, fontWeight = FontWeight.Bold)
                            Text("Número: ${doc.numero ?: "N/D"}", style = FireTypography.BodyMedium)
                        }
                    }
                }
            }
        }
        FireButton(text = "Voltar ao Dashboard", onClick = onBack, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun VitimasModuleView(
    uiState: OccurrenceFormUiState,
    onNewVictimClick: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(FireSpacing.Medium)
    ) {
        FireButton(onClick = onNewVictimClick, text = "+ Registrar Vítima", modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(FireSpacing.Medium))

        if (uiState.vitimas.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Nenhuma vítima registrada.", style = FireTypography.BodyMedium, color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
            ) {
                items(uiState.vitimas) { vitima ->
                    FireCard {
                        Column(modifier = Modifier.padding(FireSpacing.Medium)) {
                            Text("👤 Nome: ${vitima.nome}", style = FireTypography.Title, fontWeight = FontWeight.Bold)
                            Text("Lesões: ${vitima.lesoesAparentes} | Destino: ${vitima.hospitalDestino ?: "Não informado"}", style = FireTypography.BodyMedium)
                            Text("Glasgow: ${vitima.sinaisVitais.escalaGCS ?: "N/D"} | FC: ${vitima.sinaisVitais.pulso ?: "N/D"}", style = FireTypography.Caption)
                        }
                    }
                }
            }
        }
        FireButton(text = "Voltar ao Dashboard", onClick = onBack, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ApoiosModuleView(
    uiState: OccurrenceFormUiState,
    onAddApoio: (OrgaoApoio, String, String) -> Unit,
    onRemoveApoio: (Int) -> Unit,
    onBack: () -> Unit
) {
    var selectedOrgaoIndex by remember { mutableStateOf(0) }
    var viatura by remember { mutableStateOf("") }
    var encarregado by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(FireSpacing.Medium)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
        ) {
            Text("Vincular Órgão de Apoio", style = FireTypography.Title, fontWeight = FontWeight.Bold)
            if (uiState.orgaosDisponiveis.isNotEmpty()) {
                var expandedDropdown by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    FireOutlinedButton(
                        text = "${uiState.orgaosDisponiveis[selectedOrgaoIndex].sigla} - ${uiState.orgaosDisponiveis[selectedOrgaoIndex].nome}",
                        onClick = { expandedDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        icon = FireIcons.ArrowDropDown
                    )
                    DropdownMenu(expanded = expandedDropdown, onDismissRequest = { expandedDropdown = false }) {
                        uiState.orgaosDisponiveis.forEachIndexed { idx, org ->
                            DropdownMenuItem(text = { Text("${org.sigla} - ${org.nome}") }, onClick = {
                                selectedOrgaoIndex = idx
                                expandedDropdown = false
                            })
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium)) {
                FireOutlinedTextField(value = viatura, onValueChange = { viatura = it }, label = "Viatura", modifier = Modifier.weight(1f))
                FireOutlinedTextField(value = encarregado, onValueChange = { encarregado = it }, label = "Encarregado", modifier = Modifier.weight(1f))
            }

            FireButton(
                text = "Adicionar Apoio",
                onClick = {
                    if (uiState.orgaosDisponiveis.isNotEmpty()) {
                        onAddApoio(uiState.orgaosDisponiveis[selectedOrgaoIndex], viatura, encarregado)
                        viatura = ""
                        encarregado = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            FireDivider()

            LazyColumn(verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)) {
                items(uiState.apoiosDetalhados.size) { idx ->
                    val apoio = uiState.apoiosDetalhados[idx]
                    FireCard {
                        Row(
                            modifier = Modifier.padding(FireSpacing.Medium),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("${apoio.orgao.sigla} - ${apoio.orgao.nome}", style = FireTypography.BodyMedium, fontWeight = FontWeight.Bold)
                                Text("Viatura: ${apoio.viatura} | Encarregado: ${apoio.encarregado}", style = FireTypography.Caption)
                            }
                            FireIconButton(icon = FireIcons.Delete, onClick = { onRemoveApoio(idx) }, tint = FireColors.Error)
                        }
                    }
                }
            }
        }
        FireButton(text = "Voltar ao Dashboard", onClick = onBack, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun HistoricoModuleView(
    uiState: OccurrenceFormUiState,
    onHistoryChanged: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(FireSpacing.Medium),
        verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
    ) {
        FireSectionHeader(title = "Relatório e Histórico Narrativo", icon = "📝📝")
        
        FireOutlinedTextField(
            value = uiState.historico,
            onValueChange = onHistoryChanged,
            label = "Descrição detalhada do atendimento",
            singleLine = false,
            modifier = Modifier.weight(1f).fillMaxWidth()
        )

        FireButton(text = "Voltar ao Dashboard", onClick = onBack, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun EvidenciasModuleView(
    uiState: OccurrenceFormUiState,
    onTakePhoto: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(FireSpacing.Medium)
    ) {
        FireButton(onClick = onTakePhoto, text = "Fotografar Evidência", icon = FireIcons.Add, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(FireSpacing.Medium))

        if (uiState.evidencias.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Nenhuma evidência registrada.", style = FireTypography.BodyMedium, color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
            ) {
                items(uiState.evidencias) { ev ->
                    FireCard {
                        Row(
                            modifier = Modifier.padding(FireSpacing.Medium),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Evidência: ${ev.tipo}", style = FireTypography.BodyMedium, fontWeight = FontWeight.Bold)
                                Text("Data: ${ev.dataHora.take(16).replace("T", " ")}", style = FireTypography.Caption, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
        FireButton(text = "Voltar ao Dashboard", onClick = onBack, modifier = Modifier.fillMaxWidth())
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnexosModuleView(
    uiState: OccurrenceFormUiState,
    onAddMedia: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(FireSpacing.Medium)
    ) {
        FireButton(onClick = onAddMedia, text = "Carregar Foto / Arquivo", icon = FireIcons.CloudUpload, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(FireSpacing.Medium))

        if (uiState.fotos.isEmpty() && uiState.videos.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Nenhuma foto ou mídia anexada.", style = FireTypography.BodyMedium, color = Color.Gray)
            }
        } else {
            Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Text("Mídias Anexadas:", style = FireTypography.LabelLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(FireSpacing.Small))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                ) {
                    uiState.fotos.forEach { url ->
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .border(1.dp, Color.Gray, FireShapes.Medium)
                                .clip(FireShapes.Medium)
                                .background(Color.LightGray)
                        ) {
                            Text("FOTO", style = FireTypography.Caption, modifier = Modifier.align(Alignment.Center))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDocumentDialog(
    ocrResult: OcrDocumentResult?,
    onDismiss: () -> Unit,
    onConfirm: (tipo: String, numero: String, fields: Map<String, String>, rawText: String, uri: Uri) -> Unit
) {
    val documentTypes = listOf("CNH", "CIN", "RG", "Carteira OAB", "CREA", "CRM", "COREN", "CRP", "CRQ", "CRBio", "Outros Conselhos")
    var selectedTypeIndex by remember { mutableStateOf(0) }
    var expandedDropdown by remember { mutableStateOf(false) }

    var numero by remember { mutableStateOf("") }
    var nome by remember { mutableStateOf("") }
    var cpf by remember { mutableStateOf("") }
    var rg by remember { mutableStateOf("") }
    var nascimento by remember { mutableStateOf("") }
    var filiacao by remember { mutableStateOf("") }
    var rgOrgaoEmissor by remember { mutableStateOf("") }
    var rgUf by remember { mutableStateOf("") }
    var naturalidade by remember { mutableStateOf("") }
    
    LaunchedEffect(ocrResult) {
        ocrResult?.let {
            val idx = documentTypes.indexOf(it.tipo)
            if (idx != -1) selectedTypeIndex = idx
            
            numero = it.extractedFields["registro"] ?: it.extractedFields["rg"] ?: ""
            nome = it.extractedFields["nome"] ?: ""
            cpf = it.extractedFields["cpf"] ?: ""
            rg = it.extractedFields["rg"] ?: ""
            nascimento = it.extractedFields["nascimento"] ?: ""
            filiacao = it.extractedFields["filiacao"] ?: ""
            rgOrgaoEmissor = it.extractedFields["rg_orgao_emissor"] ?: ""
            rgUf = it.extractedFields["rg_uf"] ?: ""
            naturalidade = it.extractedFields["naturalidade"] ?: ""
        }
    }

    val getFieldColors: @Composable (String) -> TextFieldColors = { fieldKey ->
        val confidence = ocrResult?.fieldsWithConfidence?.get(fieldKey)?.confidence ?: 1.0f
        val color = when {
            confidence >= 0.80f -> Color(0xFF2E7D32)
            confidence >= 0.50f -> Color(0xFFF57F17)
            else -> Color(0xFFC62828)
        }
        OutlinedTextFieldDefaults.colors(
            focusedBorderColor = color,
            unfocusedBorderColor = color,
            focusedLabelColor = color,
            unfocusedLabelColor = color
        )
    }

    FireDialog(
        onDismissRequest = onDismiss,
        title = "Adicionar Documento",
        confirmButton = {
            FireButton(onClick = {
                val fields = mapOf(
                    "nome" to nome,
                    "cpf" to cpf,
                    "rg" to rg,
                    "nascimento" to nascimento,
                    "filiacao" to filiacao,
                    "rg_orgao_emissor" to rgOrgaoEmissor,
                    "rg_uf" to rgUf,
                    "naturalidade" to naturalidade
                )
                onConfirm(
                    documentTypes[selectedTypeIndex],
                    numero,
                    fields,
                    ocrResult?.rawText ?: "",
                    Uri.parse("android.resource://com.example.firenotes/drawable/ic_launcher_foreground")
                )
            }, text = "Confirmar")
        },
        dismissButton = {
            FireTextButton(onClick = onDismiss, text = "Cancelar")
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                FireOutlinedButton(
                    text = "Tipo: ${documentTypes[selectedTypeIndex]}",
                    onClick = { expandedDropdown = true },
                    modifier = Modifier.fillMaxWidth(),
                    icon = FireIcons.ArrowDropDown
                )
                DropdownMenu(expanded = expandedDropdown, onDismissRequest = { expandedDropdown = false }) {
                    documentTypes.forEachIndexed { index, type ->
                        DropdownMenuItem(text = { Text(type) }, onClick = {
                            selectedTypeIndex = index
                            expandedDropdown = false
                        })
                    }
                }
            }

            FireOutlinedTextField(value = numero, onValueChange = { numero = it }, label = "Número do Documento", colors = getFieldColors("registro"))
            FireOutlinedTextField(value = nome, onValueChange = { nome = it }, label = "Nome Completo", colors = getFieldColors("nome"))
            FireOutlinedTextField(value = cpf, onValueChange = { cpf = it }, label = "CPF", colors = getFieldColors("cpf"))
            FireOutlinedTextField(value = rg, onValueChange = { rg = it }, label = "RG", colors = getFieldColors("rg"))
            FireOutlinedTextField(value = nascimento, onValueChange = { nascimento = it }, label = "Data de Nascimento", colors = getFieldColors("nascimento"))
            FireOutlinedTextField(value = filiacao, onValueChange = { filiacao = it }, label = "Filiação", colors = getFieldColors("filiacao"))
            FireOutlinedTextField(value = rgOrgaoEmissor, onValueChange = { rgOrgaoEmissor = it }, label = "Órgão Emissor", colors = getFieldColors("rg_orgao_emissor"))
            FireOutlinedTextField(value = rgUf, onValueChange = { rgUf = it }, label = "RG UF", colors = getFieldColors("rg_uf"))
            FireOutlinedTextField(value = naturalidade, onValueChange = { naturalidade = it }, label = "Naturalidade", colors = getFieldColors("naturalidade"))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVehicleDialog(
    ocrResult: OcrDocumentResult?,
    pessoasDisponiveis: List<Pessoa>,
    crlvImage: Uri?,
    onDismiss: () -> Unit,
    onConfirm: (
        placa: String, modelo: String, cor: String, chassi: String, ano: Int?, propId: String?,
        fields: Map<String, String>, rawText: String, crlvUri: Uri?
    ) -> Unit
) {
    var placa by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf("") }
    var cor by remember { mutableStateOf("") }
    var chassi by remember { mutableStateOf("") }
    var ano by remember { mutableStateOf("") }

    var selectedPessoaIndex by remember { mutableStateOf(-1) }
    var expandedDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(ocrResult) {
        ocrResult?.let {
            placa = it.extractedFields["placa"] ?: ""
            modelo = it.extractedFields["marca_modelo"] ?: ""
            cor = it.extractedFields["cor"] ?: ""
            chassi = it.extractedFields["chassi"] ?: ""
            ano = it.extractedFields["ano_modelo"] ?: ""
        }
    }

    val getFieldColors: @Composable (String) -> TextFieldColors = { fieldKey ->
        val confidence = ocrResult?.fieldsWithConfidence?.get(fieldKey)?.confidence ?: 1.0f
        val color = when {
            confidence >= 0.80f -> Color(0xFF2E7D32)
            confidence >= 0.50f -> Color(0xFFF57F17)
            else -> Color(0xFFC62828)
        }
        OutlinedTextFieldDefaults.colors(
            focusedBorderColor = color,
            unfocusedBorderColor = color,
            focusedLabelColor = color,
            unfocusedLabelColor = color
        )
    }

    FireDialog(
        onDismissRequest = onDismiss,
        title = "Registrar Veículo",
        confirmButton = {
            FireButton(
                enabled = true,
                onClick = {
                    val propId = pessoasDisponiveis.getOrNull(selectedPessoaIndex)?.id
                    val fields = ocrResult?.extractedFields ?: emptyMap()
                    onConfirm(
                        placa, modelo, cor, chassi, ano.toIntOrNull(), propId,
                        fields, ocrResult?.rawText ?: "", crlvImage
                    )
                },
                text = "Confirmar"
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
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
        ) {
            FireOutlinedTextField(value = placa, onValueChange = { placa = it }, label = "Placa", colors = getFieldColors("placa"))
            FireOutlinedTextField(value = modelo, onValueChange = { modelo = it }, label = "Modelo", colors = getFieldColors("marca_modelo"))
            FireOutlinedTextField(value = cor, onValueChange = { cor = it }, label = "Cor", colors = getFieldColors("cor"))
            FireOutlinedTextField(value = chassi, onValueChange = { chassi = it }, label = "Chassi", colors = getFieldColors("chassi"))
            FireOutlinedTextField(value = ano, onValueChange = { ano = it }, label = "Ano", colors = getFieldColors("ano_modelo"))

            Spacer(modifier = Modifier.height(FireSpacing.Small))
            Text("Proprietário do Veículo:", style = FireTypography.LabelLarge, fontWeight = FontWeight.Bold)

            Box(modifier = Modifier.fillMaxWidth()) {
                FireOutlinedButton(
                    text = if (selectedPessoaIndex >= 0 && selectedPessoaIndex < pessoasDisponiveis.size) {
                        "${pessoasDisponiveis[selectedPessoaIndex].nome} (CPF: ${pessoasDisponiveis[selectedPessoaIndex].cpf ?: "N/D"})"
                    } else {
                        "Selecione o Proprietário"
                    },
                    onClick = { expandedDropdown = true },
                    modifier = Modifier.fillMaxWidth(),
                    icon = FireIcons.ArrowDropDown
                )
                DropdownMenu(expanded = expandedDropdown, onDismissRequest = { expandedDropdown = false }) {
                    pessoasDisponiveis.forEachIndexed { index, p ->
                        DropdownMenuItem(text = { Text("${p.nome} - CPF: ${p.cpf ?: "N/D"}") }, onClick = {
                            selectedPessoaIndex = index
                            expandedDropdown = false
                        })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVictimDialogV2(
    pessoasDisponiveis: List<Pessoa>,
    viaturasDisponiveis: List<Viatura>,
    calculateAge: (String?) -> Int?,
    onDismiss: () -> Unit,
    onConfirm: (
        pessoaId: String, lesoes: String, destino: String, quemSocorreu: String, resultado: String,
        pulso: Int?, pa: String, satO2: Int?, temp: Double?, gcs: Int?,
        viaturaSocorroId: String?, hospitalDestino: String?, transportadoPor: String?
    ) -> Unit
) {
    var selectedPessoaIndex by remember { mutableStateOf(-1) }
    var expandedDropdown by remember { mutableStateOf(false) }

    var lesoes by remember { mutableStateOf("") }
    var outroDestino by remember { mutableStateOf("") }
    var quemSocorreu by remember { mutableStateOf("") }
    var resultado by remember { mutableStateOf("Estável") }
    
    var pulso by remember { mutableStateOf("") }
    var pa by remember { mutableStateOf("") }
    var satO2 by remember { mutableStateOf("") }
    var temp by remember { mutableStateOf("") }
    var gcs by remember { mutableStateOf("") }

    var selectedViaturaSocorroIndex by remember { mutableStateOf(-1) }
    var expandedViaturaSocorroDropdown by remember { mutableStateOf(false) }
    
    var hospitalDestino by remember { mutableStateOf("") }
    var transportadoPorViatura by remember { mutableStateOf(true) }
    var selectedTransporteViaturaIndex by remember { mutableStateOf(-1) }
    var expandedTransporteViaturaDropdown by remember { mutableStateOf(false) }
    var outroOrgaoTransporte by remember { mutableStateOf("") }

    FireDialog(
        onDismissRequest = onDismiss,
        title = "Registrar Vítima",
        confirmButton = {
            FireButton(
                enabled = selectedPessoaIndex >= 0,
                onClick = {
                    val pId = pessoasDisponiveis[selectedPessoaIndex].id!!
                    val viaturaSocorroId = viaturasDisponiveis.getOrNull(selectedViaturaSocorroIndex)?.id
                    
                    val transPor = if (transportadoPorViatura) {
                        viaturasDisponiveis.getOrNull(selectedTransporteViaturaIndex)?.prefixo ?: "Viatura"
                    } else {
                        outroOrgaoTransporte
                    }

                    onConfirm(
                        pId, lesoes, outroDestino, quemSocorreu, resultado,
                        pulso.toIntOrNull(), pa, satO2.toIntOrNull(), temp.toDoubleOrNull(), gcs.toIntOrNull(),
                        viaturaSocorroId, hospitalDestino, transPor
                    )
                },
                text = "Confirmar"
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
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
        ) {
            Text("Pessoa Atendida:", style = FireTypography.LabelLarge, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.fillMaxWidth()) {
                FireOutlinedButton(
                    text = if (selectedPessoaIndex >= 0 && selectedPessoaIndex < pessoasDisponiveis.size) {
                        pessoasDisponiveis[selectedPessoaIndex].nome
                    } else {
                        "Selecione a Pessoa"
                    },
                    onClick = { expandedDropdown = true },
                    modifier = Modifier.fillMaxWidth(),
                    icon = FireIcons.ArrowDropDown
                )
                DropdownMenu(expanded = expandedDropdown, onDismissRequest = { expandedDropdown = false }) {
                    pessoasDisponiveis.forEachIndexed { index, p ->
                        DropdownMenuItem(text = { Text(p.nome) }, onClick = {
                            selectedPessoaIndex = index
                            expandedDropdown = false
                        })
                    }
                }
            }

            FireOutlinedTextField(value = lesoes, onValueChange = { lesoes = it }, label = "Lesões Aparentes")
            FireOutlinedTextField(value = outroDestino, onValueChange = { outroDestino = it }, label = "Destino")
            FireOutlinedTextField(value = quemSocorreu, onValueChange = { quemSocorreu = it }, label = "Quem Socorreu")
            FireOutlinedTextField(value = resultado, onValueChange = { resultado = it }, label = "Resultado / Estado")

            Text("Viatura de Socorro:", style = FireTypography.LabelLarge, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.fillMaxWidth()) {
                FireOutlinedButton(
                    text = if (selectedViaturaSocorroIndex >= 0 && selectedViaturaSocorroIndex < viaturasDisponiveis.size) {
                        viaturasDisponiveis[selectedViaturaSocorroIndex].prefixo
                    } else {
                        "Selecione a Viatura"
                    },
                    onClick = { expandedViaturaSocorroDropdown = true },
                    modifier = Modifier.fillMaxWidth(),
                    icon = FireIcons.ArrowDropDown
                )
                DropdownMenu(expanded = expandedViaturaSocorroDropdown, onDismissRequest = { expandedViaturaSocorroDropdown = false }) {
                    viaturasDisponiveis.forEachIndexed { index, v ->
                        DropdownMenuItem(text = { Text(v.prefixo) }, onClick = {
                            selectedViaturaSocorroIndex = index
                            expandedViaturaSocorroDropdown = false
                        })
                    }
                }
            }

            FireOutlinedTextField(value = hospitalDestino, onValueChange = { hospitalDestino = it }, label = "Hospital de Destino")

            Spacer(modifier = Modifier.height(FireSpacing.Small))
            Text("Sinais Vitais", style = FireTypography.Title, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small)) {
                FireOutlinedTextField(value = pulso, onValueChange = { pulso = it }, label = "Pulso (BPM)", modifier = Modifier.weight(1f))
                FireOutlinedTextField(value = pa, onValueChange = { pa = it }, label = "P.A.", modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small)) {
                FireOutlinedTextField(value = satO2, onValueChange = { satO2 = it }, label = "Sat. O2 (%)", modifier = Modifier.weight(1f))
                FireOutlinedTextField(value = temp, onValueChange = { temp = it }, label = "Temp (°C)", modifier = Modifier.weight(1f))
            }
            FireOutlinedTextField(value = gcs, onValueChange = { gcs = it }, label = "Escala Glasgow")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddViaturaDialog(
    onDismiss: () -> Unit,
    onConfirm: (prefixo: String, tipo: String, unidade: String?, kmSaida: Int?, kmLocal: Int?, observacoes: String?) -> Unit
) {
    var prefixo by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("UR") }
    var unidade by remember { mutableStateOf("") }
    var kmSaida by remember { mutableStateOf("") }
    var kmLocal by remember { mutableStateOf("") }
    var observacoes by remember { mutableStateOf("") }

    FireDialog(
        onDismissRequest = onDismiss,
        title = "Adicionar Viatura",
        confirmButton = {
            FireButton(
                enabled = prefixo.isNotBlank(),
                onClick = {
                    onConfirm(
                        prefixo, tipo,
                        unidade.ifBlank { null },
                        kmSaida.toIntOrNull(),
                        kmLocal.toIntOrNull(),
                        observacoes.ifBlank { null }
                    )
                },
                text = "Confirmar"
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
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
        ) {
            FireOutlinedTextField(value = prefixo, onValueChange = { prefixo = it }, label = "Prefixo")
            FireOutlinedTextField(value = tipo, onValueChange = { tipo = it }, label = "Tipo (Ex: UR, ABS, ASE)")
            FireOutlinedTextField(value = unidade, onValueChange = { unidade = it }, label = "Unidade/Batalhão")
            FireOutlinedTextField(value = kmSaida, onValueChange = { kmSaida = it }, label = "KM Saída")
            FireOutlinedTextField(value = kmLocal, onValueChange = { kmLocal = it }, label = "KM Local")
            FireOutlinedTextField(value = observacoes, onValueChange = { observacoes = it }, label = "Observações")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMilitarDialog(
    onDismiss: () -> Unit,
    onConfirm: (re: String, nomeGuerra: String, graduacaoStr: String, funcao: String?) -> Unit
) {
    var re by remember { mutableStateOf("") }
    var nomeGuerra by remember { mutableStateOf("") }
    var selectedGradIndex by remember { mutableStateOf(0) }
    var expandedDropdown by remember { mutableStateOf(false) }
    var funcao by remember { mutableStateOf("") }

    val graduacoes = listOf(
        "Soldado", "Cabo", "3º Sargento", "2º Sargento", "1º Sargento", 
        "Subtenente", "2º Tenente", "1º Tenente", "Capitão", "Major", "Tenente-Coronel", "Coronel"
    )

    FireDialog(
        onDismissRequest = onDismiss,
        title = "Adicionar Militar",
        confirmButton = {
            FireButton(
                enabled = re.isNotBlank() && nomeGuerra.isNotBlank(),
                onClick = {
                    onConfirm(re, nomeGuerra, graduacoes[selectedGradIndex], funcao.ifBlank { null })
                },
                text = "Confirmar"
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
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
        ) {
            FireOutlinedTextField(value = re, onValueChange = { re = it }, label = "RE (Registro Estatístico)")
            FireOutlinedTextField(value = nomeGuerra, onValueChange = { nomeGuerra = it }, label = "Nome de Guerra")
            
            Text("Graduação:", style = FireTypography.LabelLarge, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.fillMaxWidth()) {
                FireOutlinedButton(
                    text = graduacoes[selectedGradIndex],
                    onClick = { expandedDropdown = true },
                    modifier = Modifier.fillMaxWidth(),
                    icon = FireIcons.ArrowDropDown
                )
                DropdownMenu(expanded = expandedDropdown, onDismissRequest = { expandedDropdown = false }) {
                    graduacoes.forEachIndexed { index, grad ->
                        DropdownMenuItem(text = { Text(grad) }, onClick = {
                            selectedGradIndex = index
                            expandedDropdown = false
                        })
                    }
                }
            }

            FireOutlinedTextField(value = funcao, onValueChange = { funcao = it }, label = "Função na Viatura (Ex: Motorista, Encarregado, Auxiliar)")
        }
    }
}

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
            Text("Selecione a nova viatura para ${militar.nomeGuerra}:", style = FireTypography.BodyMedium)
            
            Box(modifier = Modifier.fillMaxWidth()) {
                FireOutlinedButton(
                    text = if (selectedViaturaIndex >= 0) viaturas[selectedViaturaIndex].prefixo else "Selecionar Viatura",
                    onClick = { expandedDropdown = true },
                    modifier = Modifier.fillMaxWidth(),
                    icon = FireIcons.ArrowDropDown
                )
                DropdownMenu(expanded = expandedDropdown, onDismissRequest = { expandedDropdown = false }) {
                    viaturas.forEachIndexed { index, v ->
                        if (v.id != militar.viaturaId) {
                            DropdownMenuItem(text = { Text(v.prefixo) }, onClick = {
                                selectedViaturaIndex = index
                                expandedDropdown = false
                            })
                        }
                    }
                }
            }
        }
    }
}

