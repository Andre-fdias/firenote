package com.example.firenotes.ui.screens.occurrence

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.firenotes.domain.model.*
import com.example.firenotes.domain.repository.OcrDocumentResult
import com.example.firenotes.ui.designsystem.colors.FireColors
import com.example.firenotes.ui.designsystem.components.buttons.FireButton
import com.example.firenotes.ui.designsystem.components.buttons.FireFAB
import com.example.firenotes.ui.designsystem.components.buttons.FireIconButton
import com.example.firenotes.ui.designsystem.components.buttons.FireTextButton
import com.example.firenotes.ui.designsystem.components.dialogs.FireDialog
import com.example.firenotes.ui.designsystem.components.topbar.FireTopBar
import com.example.firenotes.ui.designsystem.states.FireLoading
import com.example.firenotes.ui.designsystem.components.widgets.GalleryImage
import com.example.firenotes.ui.designsystem.components.widgets.ImageViewerDialog
import com.example.firenotes.ui.designsystem.icons.FireIcons
import com.example.firenotes.ui.designsystem.spacing.FireSpacing
import com.example.firenotes.ui.screens.occurrence.components.*
import com.example.firenotes.ui.screens.occurrence.dialogs.*
import com.example.firenotes.ui.screens.occurrence.models.FormStage
import com.example.firenotes.ui.screens.occurrence.models.OccurrenceModule
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OccurrenceFormScreen(
    viewModel: OccurrenceFormViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDocumentScanner: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                uiState.id?.let { id ->
                    viewModel.loadOccurrence(id, showLoading = false)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Estados de diálogo
    var activeModule by remember { mutableStateOf<OccurrenceModule?>(null) }
    var showAddVehicleDialog by remember { mutableStateOf(false) }
    var showAddVictimDialog by remember { mutableStateOf(false) }
    var showAddViaturaDialog by remember { mutableStateOf(false) }
    var showAddMilitarDialog by remember { mutableStateOf(false) }
    var showMoveMilitarDialog by remember { mutableStateOf(false) }
    var showClassificationDialog by remember { mutableStateOf(false) }

    var viaturaToEdit by remember { mutableStateOf<Viatura?>(null) }
    var activeViaturaIdForMilitar by remember { mutableStateOf("") }
    var activeMilitarToMove by remember { mutableStateOf<Militar?>(null) }
    var viewerImageId by remember { mutableStateOf<String?>(null) }

    // Estados OCR
    var ocrResultData by remember { mutableStateOf<OcrDocumentResult?>(null) }
    var crlvImageUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var tempPhotoUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var activeOcrScanType by rememberSaveable { mutableStateOf("") }
    var evidencePhotoUriString by rememberSaveable { mutableStateOf<String?>(null) }

    val crlvImageUri = crlvImageUriString?.let { Uri.parse(it) }
    val tempPhotoUri = tempPhotoUriString?.let { Uri.parse(it) }
    val evidencePhotoUri = evidencePhotoUriString?.let { Uri.parse(it) }

    // ============================================
    // LAUNCHERS E PERMISSÕES
    // ============================================

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            viewModel.captureLocationAndAddress()
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoUri != null) {
            viewModel.processAndRunOcrDirectly(
                imageUri = tempPhotoUri,
                onSuccess = { result, processedUri ->
                    ocrResultData = result
                    if (activeOcrScanType == "CRLV") {
                        crlvImageUriString = processedUri.toString()
                        showAddVehicleDialog = true
                    }
                }
            )
        }
    }

    val evidenceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            showClassificationDialog = true
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val uri = viewModel.createPhotoUri()
                when (activeOcrScanType) {
                    "EVIDENCIA" -> {
                        evidencePhotoUriString = uri.toString()
                        evidenceLauncher.launch(uri)
                    }
                    "CRLV", "DOCUMENTO" -> {
                        tempPhotoUriString = uri.toString()
                        takePictureLauncher.launch(uri)
                    }
                }
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("Erro ao iniciar câmera: ${e.localizedMessage}")
                }
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Permissão de câmera negada")
            }
        }
    }

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.uploadOccurrenceFile(it, isVideo = false) }
    }

    // ============================================
    // GALERIA DE IMAGENS
    // ============================================

    val allGalleryImages = remember(uiState) {
        buildList {
            uiState.documentos.forEach { doc ->
                add(GalleryImage(
                    id = doc.id ?: "",
                    path = doc.urlImagem ?: "",
                    title = "Documento: ${doc.tipo}",
                    category = "Documento",
                    date = doc.dataUpload ?: "N/A",
                    origin = doc.tipo
                ))
            }
            uiState.veiculos.forEach { veic ->
                if (!veic.urlCrlv.isNullOrBlank()) {
                    add(GalleryImage(
                        id = veic.id ?: "",
                        path = veic.urlCrlv!!,
                        title = "CRLV: ${veic.placa ?: ""}",
                        category = "Veículo",
                        date = "N/A",
                        origin = "CRLV"
                    ))
                }
            }
            uiState.evidencias.forEach { ev ->
                add(GalleryImage(
                    id = ev.id ?: "",
                    path = ev.urlStorage,
                    title = "Evidência: ${ev.tipo}",
                    category = when (ev.tipo) {
                        "Documento" -> "Documento"
                        "Veículo" -> "Veículo"
                        "Vítima" -> "Vítima"
                        "Local" -> "Local"
                        else -> "Evidência"
                    },
                    date = ev.dataHora,
                    origin = ev.tipo
                ))
            }
            uiState.fotos.forEachIndexed { idx, path ->
                add(GalleryImage(
                    id = "foto_$idx",
                    path = path,
                    title = "Anexo ${idx + 1}",
                    category = "Anexo",
                    date = "N/A",
                    origin = "Anexo"
                ))
            }
        }
    }

    // ============================================
    // CALLBACKS DA GALERIA
    // ============================================

    val onDeleteImage: (GalleryImage) -> Unit = { img ->
        when (img.category) {
            "Documento" -> viewModel.deleteDocumento(img.id)
            "Veículo" -> viewModel.deleteVeiculo(img.id)
            "Evidência" -> viewModel.deleteEvidencia(img.id)
            "Anexo" -> viewModel.removeFoto(img.path)
        }
    }

    val onShareImage: (GalleryImage) -> Unit = { img ->
        try {
            val file = java.io.File(img.path)
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "com.example.firenotes.fileprovider",
                file
            )
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Compartilhar Imagem"))
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                context,
                "Erro ao compartilhar: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ============================================
    // AÇÕES DA CÂMERA
    // ============================================

    val launchCameraWithPermissionCheck: (String) -> Unit = { scanType ->
        activeOcrScanType = scanType
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                val uri = viewModel.createPhotoUri()
                when (scanType) {
                    "EVIDENCIA" -> {
                        evidencePhotoUriString = uri.toString()
                        evidenceLauncher.launch(uri)
                    }
                    else -> {
                        tempPhotoUriString = uri.toString()
                        takePictureLauncher.launch(uri)
                    }
                }
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("Erro ao iniciar câmera: ${e.localizedMessage}")
                }
            }
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    val autoSaveAndCloseModule: () -> Unit = {
        viewModel.finalizeOccurrence()
        activeModule = null
    }

    // ============================================
    // SCAFFOLD PRINCIPAL
    // ============================================

    Scaffold(
        topBar = {
            FireTopBar(
                title = buildTopBarTitle(activeModule, uiState),
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
                            onClick = { /* TODO: Compartilhar PDF */ }
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {},
        containerColor = FireColors.Background,
        modifier = modifier
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(FireColors.Background)
        ) {
            // Conteúdo Principal
            when (uiState.formStage) {
                FormStage.INITIAL_DATA, FormStage.NATURE_SELECTION -> {
                    InitialDataScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        locationPermissionLauncher = locationPermissionLauncher
                    )
                }
                FormStage.TABS -> {
                    if (activeModule == null) {
                        // Dashboard com módulos
                        ModularDashboardView(
                            uiState = uiState,
                            onModuleSelected = { module ->
                                activeModule = module
                            },
                            onFinishClick = onNavigateBack
                        )
                    } else {
                        // Módulos individuais carregados dinamicamente
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
                                        galleryImages = allGalleryImages,
                                        onImageClick = { viewerImageId = it.id },
                                        onBack = autoSaveAndCloseModule
                                    )
                                }
                                OccurrenceModule.VIATURAS -> {
                                    ViaturasModuleView(
                                        uiState = uiState,
                                        onNewViaturaClick = { showAddViaturaDialog = true },
                                        onEditViaturaClick = { viaturaToEdit = it },
                                        onDeleteViatura = viewModel::deleteViatura,
                                        galleryImages = allGalleryImages,
                                        onImageClick = { viewerImageId = it.id },
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
                                            crlvImageUriString = null
                                            showAddVehicleDialog = true
                                        },
                                        onScanCrlvClick = {
                                            launchCameraWithPermissionCheck("CRLV")
                                        },
                                        galleryImages = allGalleryImages.filter { it.category == "Veículo" },
                                        onImageClick = { viewerImageId = it.id },
                                        onBack = autoSaveAndCloseModule
                                    )
                                }
                                OccurrenceModule.DOCUMENTOS -> {
                                    DocumentosModuleView(
                                        uiState = uiState,
                                        onNewDocClick = {
                                            uiState.id?.let { occurrenceId ->
                                                onNavigateToDocumentScanner(occurrenceId)
                                            }
                                        },
                                        onScanDocClick = {
                                            uiState.id?.let { occurrenceId ->
                                                onNavigateToDocumentScanner(occurrenceId)
                                            }
                                        },
                                        galleryImages = allGalleryImages,
                                        onImageClick = { viewerImageId = it.id },
                                        onBack = autoSaveAndCloseModule
                                    )
                                }
                                OccurrenceModule.VITIMAS -> {
                                    VitimasModuleView(
                                        uiState = uiState,
                                        onNewVictimClick = { showAddVictimDialog = true },
                                        galleryImages = allGalleryImages,
                                        onImageClick = { viewerImageId = it.id },
                                        onBack = autoSaveAndCloseModule
                                    )
                                }
                                OccurrenceModule.APOIOS -> {
                                    ApoiosModuleView(
                                        uiState = uiState,
                                        onAddApoio = { orgao, viatura, encarregado ->
                                            viewModel.addApoio(
                                                orgaoSigla = orgao.sigla,
                                                orgaoNome = orgao.nome,
                                                viatura = viatura,
                                                encarregado = encarregado,
                                                descricaoOutros = ""
                                            )
                                        },
                                        onRemoveApoio = viewModel::removeApoio,
                                        galleryImages = allGalleryImages,
                                        onImageClick = { viewerImageId = it.id },
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
                                    val evidenceImages = allGalleryImages.filter { img ->
                                        uiState.evidencias.any { ev -> ev.id == img.id }
                                    }.map { img ->
                                        img.copy(category = "Evidência")
                                    }
                                    EvidenciasModuleView(
                                        uiState = uiState,
                                        onTakePhoto = {
                                            launchCameraWithPermissionCheck("EVIDENCIA")
                                        },
                                        galleryImages = evidenceImages,
                                        onImageClick = { viewerImageId = it.id },
                                        onBack = autoSaveAndCloseModule
                                    )
                                }
                                OccurrenceModule.ANEXOS -> {
                                    AnexosModuleView(
                                        uiState = uiState,
                                        onAddMedia = { mediaPicker.launch("image/*") },
                                        galleryImages = allGalleryImages,
                                        onImageClick = { viewerImageId = it.id },
                                        onBack = autoSaveAndCloseModule
                                    )
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }

            // Loading Overlay
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(enabled = false, onClick = {}),
                    contentAlignment = Alignment.Center
                ) {
                    FireLoading()
                }
            }
        }
    }

    // ============================================
    // DIÁLOGOS
    // ============================================



    // Dialog: Add Vehicle
    if (showAddVehicleDialog) {
        AddVehicleDialog(
            ocrResult = ocrResultData,
            pessoasDisponiveis = uiState.pessoas,
            crlvImage = crlvImageUri,
            onDismiss = { showAddVehicleDialog = false },
            onConfirm = { placa, modelo, cor, chassi, ano, propId, marca, versao, exercicio, crlvUri ->
                val fields = mapOf(
                    "placa" to placa,
                    "modelo" to modelo,
                    "cor" to cor,
                    "chassi" to chassi,
                    "ano" to ano,
                    "marca" to marca,
                    "versao" to versao,
                    "exercicio" to exercicio
                )
                viewModel.saveVeiculo(
                    placa = placa,
                    modelo = modelo,
                    cor = cor,
                    chassi = chassi,
                    ano = ano,
                    proprietarioId = propId,
                    extractedFields = fields,
                    rawText = ocrResultData?.rawText ?: "",
                    imageUri = crlvUri
                )
                showAddVehicleDialog = false
            }
        )
    }

    // Dialog: Add Victim
    if (showAddVictimDialog) {
        AddVictimDialogV3(
            pessoasDisponiveis = uiState.pessoas,
            viaturasDisponiveis = uiState.viaturas,
            onDismiss = { showAddVictimDialog = false },
            onConfirm = { pId, lesoes, lesoesEstruturadas, destino, quemSocorreu, resultado, vSocorroId, hDestino, nMed, crmMed, fc, pa, satO2, aberturaOcular, respostaVerbal, respostaMotora, resp ->
                viewModel.saveVitima(
                    pessoaId = pId,
                    lesoes = lesoes,
                    lesoesEstruturadas = lesoesEstruturadas,
                    destino = destino,
                    quemSocorreu = quemSocorreu,
                    resultado = resultado,
                    viaturaSocorroId = vSocorroId,
                    hospitalDestino = hDestino,
                    nomeMedico = nMed,
                    crmMedico = crmMed,
                    pulso = fc,
                    pa = pa,
                    satO2 = satO2,
                    aberturaOcular = aberturaOcular,
                    respostaVerbal = respostaVerbal,
                    respostaMotora = respostaMotora,
                    respiracao = resp
                )
                showAddVictimDialog = false
            }
        )
    }

    // Dialog: Add Viatura
    if (showAddViaturaDialog || viaturaToEdit != null) {
        AddViaturaDialogV2(
            viatura = viaturaToEdit,
            onDismiss = {
                showAddViaturaDialog = false
                viaturaToEdit = null
            },
            onConfirm = { prefixo, unidade, kmSaida, kmLocal, obs ->
                val prefixoLetters = prefixo.takeWhile { it.isLetter() }
                viewModel.addViatura(prefixo, prefixoLetters, unidade, kmSaida, kmLocal, obs, viaturaToEdit?.id)
                showAddViaturaDialog = false
                viaturaToEdit = null
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

    // Dialog: Classify Evidence
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

    // Dialog: Image Viewer
    if (viewerImageId != null) {
        ImageViewerDialog(
            initialImageId = viewerImageId!!,
            imagesList = allGalleryImages,
            onDismiss = { viewerImageId = null },
            onDeleteImage = onDeleteImage,
            onShareImage = onShareImage,
            onDownloadImage = { img ->
                try {
                    val file = java.io.File(img.path)
                    if (!file.exists()) {
                        android.widget.Toast.makeText(context, "Arquivo não encontrado.", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        val resolver = context.contentResolver
                        val contentValues = android.content.ContentValues().apply {
                            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                        }

                        val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                        if (uri != null) {
                            resolver.openOutputStream(uri)?.use { outputStream ->
                                file.inputStream().use { inputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }
                            android.widget.Toast.makeText(context, "Imagem salva na pasta Downloads!", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            android.widget.Toast.makeText(context, "Erro ao criar arquivo no Downloads.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Erro ao baixar: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

// ============================================
// FUNÇÕES AUXILIARES
// ============================================

@Composable
private fun BuildContextualFAB(
    activeModule: OccurrenceModule,
    onAddVehicle: () -> Unit,
    onAddViatura: () -> Unit,
    onAddVictim: () -> Unit,
    onAddEvidence: () -> Unit,
    onAddMedia: () -> Unit
) {
    val onClick = when (activeModule) {
        OccurrenceModule.VEICULOS -> onAddVehicle
        OccurrenceModule.VIATURAS -> onAddViatura
        OccurrenceModule.VITIMAS -> onAddVictim
        OccurrenceModule.EVIDENCIAS -> onAddEvidence
        OccurrenceModule.ANEXOS -> onAddMedia
        else -> null
    }

    if (onClick != null) {
        FireFAB(
            icon = FireIcons.Add,
            onClick = onClick
        )
    }
}

@Composable
private fun buildTopBarTitle(activeModule: OccurrenceModule?, uiState: OccurrenceFormUiState): String {
    return if (activeModule != null) {
        "Módulo: ${activeModule.name.lowercase().replaceFirstChar { it.uppercase() }}"
    } else {
        if (uiState.formStage == FormStage.INITIAL_DATA) "Dados Iniciais"
        else "Ocorrência: ${uiState.protocolo}"
    }
}