package com.example.firenotes.ui.screens.occurrence

import android.net.Uri
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
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
import com.example.firenotes.ui.designsystem.components.widgets.FireGalleryCard
import com.example.firenotes.ui.designsystem.components.widgets.GalleryImage
import com.example.firenotes.ui.designsystem.components.widgets.ImageViewerDialog
import com.example.firenotes.ui.designsystem.components.widgets.LocalImage
import java.time.Instant

data class SubNatureza(
    val nome: String,
    val baseNatureza: NaturezaOcorrencia,
    val categoria: String,
    val keywords: List<String>
)

val subNaturezas = listOf(
    // INCÊNDIOS
    SubNatureza("Incêndio em residência", NaturezaOcorrencia.INCENDIO, "🔥🔥 INCÊNDIOS", listOf("casa", "fogo", "residencia", "lar", "domestico")),
    SubNatureza("Incêndio em comércio", NaturezaOcorrencia.INCENDIO, "🔥🔥 INCÊNDIOS", listOf("loja", "fogo", "estabelecimento", "predio")),
    SubNatureza("Incêndio em veículo", NaturezaOcorrencia.INCENDIO, "🔥🔥 INCÊNDIOS", listOf("carro", "fogo", "veiculo", "moto", "caminhao")),
    SubNatureza("Incêndio florestal", NaturezaOcorrencia.INCENDIO, "🔥🔥 INCÊNDIOS", listOf("mato", "fogo", "arvore", "floresta", "queimada", "vegetacao")),
    SubNatureza("Incêndio industrial", NaturezaOcorrencia.INCENDIO, "🔥🔥 INCÊNDIOS", listOf("galpao", "fogo", "industria", "fabrica", "quimico")),

    // APH
    SubNatureza("Mal súbito", NaturezaOcorrencia.PESSOAL, "🚑🚑 APH", listOf("desmaio", "pressao", "passando mal", "infarto")),
    SubNatureza("Queda", NaturezaOcorrencia.PESSOAL, "🚑🚑 APH", listOf("altura", "propria altura", "chao", "queda")),
    SubNatureza("Trauma", NaturezaOcorrencia.PESSOAL, "🚑🚑 APH", listOf("fratura", "corte", "sangramento", "ferimento")),
    SubNatureza("PCR", NaturezaOcorrencia.PESSOAL, "🚑🚑 APH", listOf("parada", "cardio", "respiratoria", "reanimacao")),
    SubNatureza("Parto", NaturezaOcorrencia.PESSOAL, "🚑🚑 APH", listOf("nascimento", "bebe", "gravida", "gestante")),
    SubNatureza("Afogamento", NaturezaOcorrencia.PESSOAL, "🚑🚑 APH", listOf("agua", "piscina", "rio", "mar")),

    // SALVAMENTOS
    SubNatureza("Altura", NaturezaOcorrencia.SALVAMENTO, "🚒🚒 SALVAMENTOS", listOf("rapel", "ponte", "predio", "elevado")),
    SubNatureza("Aquático", NaturezaOcorrencia.SALVAMENTO, "🚒🚒 SALVAMENTOS", listOf("rio", "mar", "represa", "afogamento")),
    SubNatureza("Estrutural", NaturezaOcorrencia.SALVAMENTO, "🚒🚒 SALVAMENTOS", listOf("desabamento", "escombros", "colapso")),
    SubNatureza("Animal", NaturezaOcorrencia.SALVAMENTO, "🚒🚒 SALVAMENTOS", listOf("cachorro", "gato", "cobra", "resgate", "bicho")),
    SubNatureza("Busca", NaturezaOcorrencia.SALVAMENTO, "🚒🚒 SALVAMENTOS", listOf("desaparecido", "floresta", "resgate", "perdido")),

    // ACIDENTES
    SubNatureza("Colisão", NaturezaOcorrencia.ACIDENTE_TRANSITO, "🚗🚗 ACIDENTES", listOf("batida", "carro", "veiculo", "transito")),
    SubNatureza("Capotamento", NaturezaOcorrencia.ACIDENTE_TRANSITO, "🚗🚗 ACIDENTES", listOf("tombamento", "carro", "veiculo", "transito")),
    SubNatureza("Atropelamento", NaturezaOcorrencia.ACIDENTE_TRANSITO, "🚗🚗 ACIDENTES", listOf("pedestre", "carro", "veiculo", "atropelar")),
    SubNatureza("Moto", NaturezaOcorrencia.ACIDENTE_TRANSITO, "🚗🚗 ACIDENTES", listOf("colisao moto", "queda moto", "motocicleta")),
    SubNatureza("Caminhão", NaturezaOcorrencia.ACIDENTE_TRANSITO, "🚗🚗 ACIDENTES", listOf("carreta", "caminhao", "veiculo pesado")),

    // OUTROS
    SubNatureza("Queda de árvore", NaturezaOcorrencia.QUEDA, "🌳🌳 OUTROS", listOf("arvore", "via", "bloqueio", "vento")),
    SubNatureza("Choque elétrico", NaturezaOcorrencia.QUEDA, "🌳🌳 OUTROS", listOf("energia", "fio", "poste", "eletrocussao")),
    SubNatureza("Vazamento", NaturezaOcorrencia.QUEDA, "🌳🌳 OUTROS", listOf("gas", "agua", "produto", "vazando")),
    SubNatureza("Produtos perigosos", NaturezaOcorrencia.QUEDA, "🌳🌳 OUTROS", listOf("quimico", "gas", "carga", "explosivo"))
)

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
    onNavigateToDocumentScanner: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var activeModule by remember { mutableStateOf<OccurrenceModule?>(null) }
    
    // Dialog control states
    var showAddDocDialog by remember { mutableStateOf(false) }
    var showAddVehicleDialog by remember { mutableStateOf(false) }
    var showAddVictimDialog by remember { mutableStateOf(false) }
    var showAddViaturaDialog by remember { mutableStateOf(false) }
    var viaturaToEdit by remember { mutableStateOf<Viatura?>(null) }
    var showAddMilitarDialog by remember { mutableStateOf(false) }
    var showMoveMilitarDialog by remember { mutableStateOf(false) }
    
    var activeViaturaIdForMilitar by remember { mutableStateOf("") }
    var activeMilitarToMove by remember { mutableStateOf<Militar?>(null) }
    
    var ocrResultData by remember { mutableStateOf<OcrDocumentResult?>(null) }
    var crlvImageUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val crlvImageUri = crlvImageUriString?.let { Uri.parse(it) }
    
    // Pickers and Launchers
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var tempPhotoUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val tempPhotoUri = tempPhotoUriString?.let { Uri.parse(it) }
    var activeOcrScanType by rememberSaveable { mutableStateOf("") } // "DOCUMENTO" or "CRLV"

    val allGalleryImages = remember(uiState) {
        val list = mutableListOf<GalleryImage>()
        uiState.documentos.forEach { doc ->
            list.add(GalleryImage(id = doc.id ?: "", path = doc.urlImagem ?: "", title = "Documento: ${doc.tipo}", category = "Documento", date = doc.dataUpload ?: "N/A", origin = doc.tipo))
        }
        uiState.veiculos.forEach { veic ->
            if (!veic.urlCrlv.isNullOrBlank()) {
                list.add(GalleryImage(id = veic.id ?: "", path = veic.urlCrlv ?: "", title = "CRLV: ${veic.placa ?: ""}", category = "Veículo", date = "N/A", origin = "CRLV"))
            }
        }
        uiState.evidencias.forEach { ev ->
            val cat = when (ev.tipo) {
                "Documento" -> "Documento"
                "Veículo" -> "Veículo"
                "Vítima" -> "Vítima"
                "Local" -> "Local"
                else -> "Evidência"
            }
            list.add(GalleryImage(id = ev.id ?: "", path = ev.urlStorage, title = "Evidência: ${ev.tipo}", category = cat, date = ev.dataHora, origin = ev.tipo))
        }
        uiState.fotos.forEachIndexed { idx, path ->
            list.add(GalleryImage(id = "foto_$idx", path = path, title = "Anexo ${idx + 1}", category = "Anexo", date = "N/A", origin = "Anexo"))
        }
        list
    }

    var viewerImageId by remember { mutableStateOf<String?>(null) }

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
            android.widget.Toast.makeText(context, "Erro ao compartilhar imagem: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    var selectedNaturezaForCreation by remember { mutableStateOf(NaturezaOcorrencia.PESSOAL) }
    var isGpsMethod by remember { mutableStateOf(true) }

    // Premium stats & preferences for sub-nature selection
    val prefs = remember(context) {
        context.getSharedPreferences("fire_notes_nature_stats", android.content.Context.MODE_PRIVATE)
    }

    var favoritesList by remember {
        mutableStateOf(
            subNaturezas.map { it.nome }
                .map { name -> name to prefs.getInt("count_$name", 0) }
                .filter { it.second > 0 }
                .sortedByDescending { it.second }
                .map { it.first }
                .take(5)
        )
    }

    var recentsList by remember {
        mutableStateOf(
            prefs.getString("recent_natures", "")
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?: emptyList()
        )
    }

    var selectedSubNaturezaForCreation by remember {
        mutableStateOf(subNaturezas.first { it.nome == "Queda" })
    }

    var gpsCaptureTime by remember {
        mutableStateOf("")
    }

    LaunchedEffect(uiState.latitude) {
        if (uiState.latitude != null && gpsCaptureTime.isEmpty()) {
            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            gpsCaptureTime = java.time.LocalDateTime.now().format(formatter)
        } else if (uiState.latitude == null) {
            gpsCaptureTime = ""
        }
    }

    val categoriesMap = remember {
        subNaturezas.groupBy { it.categoria }.mapValues { entry -> entry.value.map { it.nome } }
    }
    val optionIconsMap = remember {
        subNaturezas.associate { sub ->
            val emojiPart = sub.categoria.split(" ").firstOrNull() ?: ""
            sub.nome to emojiPart
        }
    }

    var showTechDetails by remember { mutableStateOf(false) }

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

    LaunchedEffect(uiState.formStage) {
        if (uiState.formStage == FormStage.INITIAL_DATA) {
            val currentDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            val currentTime = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
            val dataVal = if (uiState.data.isBlank()) currentDate else uiState.data
            val horaVal = if (uiState.hora.isBlank()) currentTime else uiState.hora
            if (uiState.data.isBlank() || uiState.hora.isBlank()) {
                viewModel.updateInitialFields(uiState.protocolo, dataVal, horaVal)
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
            android.util.Log.d("FireCamera", "takePictureLauncher - Foto capturada com sucesso. Iniciando processamento de OCR.")
            val uri = tempPhotoUri ?: return@rememberLauncherForActivityResult
            viewModel.processAndRunOcrDirectly(
                imageUri = uri,
                onSuccess = { result, processedUri ->
                    ocrResultData = result
                    if (activeOcrScanType == "CRLV") {
                        crlvImageUriString = processedUri.toString()
                        showAddVehicleDialog = true
                    } else {
                        tempPhotoUriString = processedUri.toString()
                        showAddDocDialog = true
                    }
                }
            )
        } else {
            android.util.Log.w("FireCamera", "takePictureLauncher - Falha ou cancelamento na captura da foto.")
        }
    }

    // File picker for general media attachments
    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            android.util.Log.d("FireCamera", "Mídia selecionada da galeria: $it")
            viewModel.uploadOccurrenceFile(it, isVideo = false)
        }
    }

    // Camera launcher for Evidence
    var evidencePhotoUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val evidencePhotoUri = evidencePhotoUriString?.let { Uri.parse(it) }
    var showClassificationDialog by remember { mutableStateOf(false) }
    val evidenceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            android.util.Log.d("FireCamera", "evidenceLauncher - Foto de evidência capturada com sucesso.")
            showClassificationDialog = true
        } else {
            android.util.Log.w("FireCamera", "evidenceLauncher - Falha ou cancelamento na captura da foto de evidência.")
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            android.util.Log.d("FireCamera", "Permissão de câmera concedida.")
            try {
                if (activeOcrScanType == "EVIDENCIA") {
                    val uri = viewModel.createPhotoUri()
                    evidencePhotoUriString = uri.toString()
                    android.util.Log.d("FireCamera", "Iniciando captura de foto de evidência: $uri")
                    evidenceLauncher.launch(uri)
                } else if (activeOcrScanType == "CRLV" || activeOcrScanType == "DOCUMENTO") {
                    val uri = viewModel.createPhotoUri()
                    tempPhotoUriString = uri.toString()
                    android.util.Log.d("FireCamera", "Iniciando captura de foto para OCR ($activeOcrScanType): $uri")
                    takePictureLauncher.launch(uri)
                }
            } catch (e: Exception) {
                android.util.Log.e("FireCamera", "Erro ao iniciar câmera após permissão: ${e.message}", e)
                scope.launch {
                    snackbarHostState.showSnackbar("Erro ao iniciar a câmera: ${e.localizedMessage}")
                }
            }
        } else {
            android.util.Log.w("FireCamera", "Permissão de câmera negada pelo usuário.")
            scope.launch {
                snackbarHostState.showSnackbar("Permissão de câmera negada. Não é possível tirar foto.")
            }
        }
    }

    val launchCameraWithPermissionCheck: (String) -> Unit = { scanType ->
        activeOcrScanType = scanType
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        if (hasPermission) {
            try {
                val newUri = viewModel.createPhotoUri()
                if (scanType == "EVIDENCIA") {
                    evidencePhotoUriString = newUri.toString()
                    evidenceLauncher.launch(newUri)
                } else {
                    tempPhotoUriString = newUri.toString()
                    takePictureLauncher.launch(newUri)
                }
                android.util.Log.d("FireNotes", "Camera - URI criada: $newUri")
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("Erro ao iniciar a câmera: ${e.localizedMessage}")
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
                                crlvImageUriString = null
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
                                launchCameraWithPermissionCheck("EVIDENCIA")
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
                    // Local state helpers for city autocomplete
                    val citySuggestions = remember {
                        listOf(
                            "São Paulo", "Campinas", "Valinhos", "Vinhedo", "Jundiaí", "Guarulhos", 
                            "São Bernardo do Campo", "Santo André", "Osasco", "Santos", "Ribeirão Preto", 
                            "São José dos Campos", "Sorocaba", "Mogi das Cruzes", "Bauru", "Piracicaba"
                        )
                    }
                    var expandedCitySuggestions by remember { mutableStateOf(false) }
                    val filteredCities = remember(uiState.cidade) {
                        if (uiState.cidade.isBlank()) {
                            emptyList()
                        } else {
                            citySuggestions.filter { it.contains(uiState.cidade, ignoreCase = true) && it != uiState.cidade }
                        }
                    }

                    // Form validation & visual checkmarks
                    val isTalaoFilled = remember(uiState.protocolo, uiState.data, uiState.hora) {
                        uiState.protocolo.isNotBlank() && uiState.data.isNotBlank() && uiState.hora.isNotBlank()
                    }
                    val isLocationFilled = remember(uiState.latitude, uiState.rua, uiState.cidade) {
                        uiState.latitude != null && uiState.rua.isNotBlank() && uiState.cidade.isNotBlank()
                    }
                    val isNaturezaSelected = remember(selectedSubNaturezaForCreation) {
                        selectedSubNaturezaForCreation.nome.isNotBlank()
                    }
                    val isFormValid = remember(uiState.protocolo, uiState.rua, uiState.cidade, uiState.uf) {
                        uiState.protocolo.isNotBlank() && uiState.rua.isNotBlank() && uiState.cidade.isNotBlank() && uiState.uf.isNotBlank()
                    }

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

                        val currentProntidao = remember {
                            com.example.firenotes.data.service.ProntidaoService.getProntidaoForDate(java.time.LocalDate.now())
                        }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = currentProntidao.cor.copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(currentProntidao.cor, RoundedCornerShape(50))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Turno Ativo: ${currentProntidao.nome}",
                                    fontSize = 12.sp,
                                    color = if (currentProntidao.cor == Color(0xFFFFB300)) Color(0xFF6B4C00) else currentProntidao.cor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Identificação do Registro",
                                        style = FireTypography.Title,
                                        fontWeight = FontWeight.Bold,
                                        color = FireColors.Primary
                                    )
                                    if (isTalaoFilled) {
                                        Text("✓", color = FireColors.Success, fontWeight = FontWeight.Bold, style = FireTypography.Title)
                                    }
                                }

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    FireOutlinedTextField(
                                        value = uiState.protocolo,
                                        onValueChange = { viewModel.updateInitialFields(it, uiState.data, uiState.hora) },
                                        label = "Número do Talão da Ocorrência",
                                        placeholder = { Text("Ex.: 2026-04587", style = FireTypography.BodyMedium) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = FireIcons.AddAlert,
                                                contentDescription = "Número do Talão",
                                                tint = FireColors.Primary
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        text = "Formato do Talão. Ex.: 2026-04587",
                                        style = FireTypography.LabelSmall,
                                        color = FireColors.OnSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(start = FireSpacing.ExtraSmall, top = 2.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
                                ) {
                                    FireDatePicker(
                                        value = uiState.data,
                                        onDateSelected = { viewModel.updateInitialFields(uiState.protocolo, it, uiState.hora) },
                                        label = "Data (dd/MM/yyyy)",
                                        modifier = Modifier.weight(1f)
                                    )
                                    FireTimePicker(
                                        value = uiState.hora,
                                        onTimeSelected = { viewModel.updateInitialFields(uiState.protocolo, uiState.data, it) },
                                        label = "Hora (HH:mm)",
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Localização Geográfica",
                                        style = FireTypography.Title,
                                        fontWeight = FontWeight.Bold,
                                        color = FireColors.Primary
                                    )
                                    if (isLocationFilled) {
                                        Text("✓", color = FireColors.Success, fontWeight = FontWeight.Bold, style = FireTypography.Title)
                                    }
                                }

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

                                // M3 Success Card
                                if (uiState.latitude != null) {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = FireColors.Success.copy(alpha = 0.1f),
                                            contentColor = FireColors.Success
                                        ),
                                        shape = FireShapes.Medium,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, FireColors.Success.copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(FireSpacing.Medium),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("✓", style = FireTypography.Title, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.width(FireSpacing.Small))
                                            Column {
                                                Text(
                                                    text = "Localização obtida com sucesso",
                                                    style = FireTypography.BodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (uiState.rua.isNotBlank()) {
                                                    Text(
                                                        text = "Endereço: ${uiState.rua}, ${uiState.numero} - ${uiState.bairro}, ${uiState.cidade} - ${uiState.uf}",
                                                        style = FireTypography.LabelMedium
                                                    )
                                                }
                                                if (gpsCaptureTime.isNotBlank()) {
                                                    Text(
                                                        text = "Capturado em: $gpsCaptureTime",
                                                        style = FireTypography.LabelSmall,
                                                        color = FireColors.OnSurfaceVariant.copy(alpha = 0.7f)
                                                    )
                                                }
                                            }
                                        }
                                    }
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
                                    // Cidade autocomplete suggestions dropdown
                                    Box(modifier = Modifier.weight(3f)) {
                                        FireOutlinedTextField(
                                            value = uiState.cidade,
                                            onValueChange = { 
                                                viewModel.updateManualAddress(uiState.rua, uiState.numero, uiState.bairro, it, uiState.uf) 
                                                expandedCitySuggestions = true
                                            },
                                            label = "Cidade"
                                        )
                                        if (filteredCities.isNotEmpty()) {
                                            DropdownMenu(
                                                expanded = expandedCitySuggestions,
                                                onDismissRequest = { expandedCitySuggestions = false },
                                                properties = androidx.compose.ui.window.PopupProperties(focusable = false)
                                            ) {
                                                filteredCities.forEach { city ->
                                                    DropdownMenuItem(
                                                        text = { Text(city, style = FireTypography.Body) },
                                                        onClick = {
                                                            viewModel.updateManualAddress(uiState.rua, uiState.numero, uiState.bairro, city, uiState.uf)
                                                            expandedCitySuggestions = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
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

                                // Collapsible "Detalhes Técnicos" card
                                FireCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation = 1.dp,
                                    containerColor = FireColors.SurfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Column(modifier = Modifier.padding(FireSpacing.Medium)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { showTechDetails = !showTechDetails },
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Detalhes Técnicos do GPS",
                                                style = FireTypography.BodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = FireColors.OnSurfaceVariant
                                            )
                                            Icon(
                                                imageVector = if (showTechDetails) FireIcons.ArrowDropUp else FireIcons.ArrowDropDown,
                                                contentDescription = "Expandir detalhes"
                                            )
                                        }
                                        AnimatedVisibility(visible = showTechDetails) {
                                            Column(
                                                modifier = Modifier.padding(top = FireSpacing.Small),
                                                verticalArrangement = Arrangement.spacedBy(FireSpacing.ExtraSmall)
                                            ) {
                                                Text("Latitude: ${uiState.latitude ?: "N/A"}", style = FireTypography.LabelMedium)
                                                Text("Longitude: ${uiState.longitude ?: "N/A"}", style = FireTypography.LabelMedium)
                                                Text("Precisão GPS: 4.8 metros (Sinal Forte)", style = FireTypography.LabelMedium, color = FireColors.Success)
                                                Text("Data da Captura: ${gpsCaptureTime.ifEmpty { "N/A" }}", style = FireTypography.LabelMedium)
                                            }
                                        }
                                    }
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Classificação da Ocorrência",
                                        style = FireTypography.Title,
                                        fontWeight = FontWeight.Bold,
                                        color = FireColors.Primary
                                    )
                                    if (isNaturezaSelected) {
                                        Text("✓", color = FireColors.Success, fontWeight = FontWeight.Bold, style = FireTypography.Title)
                                    }
                                }

                                FireSearchableDropdownPremium(
                                    selectedOption = selectedSubNaturezaForCreation.nome,
                                    onOptionSelected = { desc ->
                                        val sub = subNaturezas.first { it.nome == desc }
                                        selectedSubNaturezaForCreation = sub
                                        selectedNaturezaForCreation = sub.baseNatureza
                                        
                                        // Track stats locally in SharedPreferences
                                        val selectedName = sub.nome
                                        val newCount = prefs.getInt("count_$selectedName", 0) + 1
                                        prefs.edit().putInt("count_$selectedName", newCount).apply()

                                        // Track recents list
                                        val currentRecents = prefs.getString("recent_natures", "")
                                            ?.split(",")
                                            ?.filter { it.isNotBlank() }
                                            ?.toMutableList()
                                            ?: mutableListOf()
                                        currentRecents.remove(selectedName)
                                        currentRecents.add(0, selectedName)
                                        val updatedRecents = currentRecents.take(10).joinToString(",")
                                        prefs.edit().putString("recent_natures", updatedRecents).apply()

                                        // Update list states to refresh UI immediately
                                        favoritesList = subNaturezas.map { it.nome }
                                            .map { name -> name to prefs.getInt("count_$name", 0) }
                                            .filter { it.second > 0 }
                                            .sortedByDescending { it.second }
                                            .map { it.first }
                                            .take(5)
                                        recentsList = currentRecents.take(10)
                                    },
                                    label = "Natureza da Ocorrência",
                                    categories = categoriesMap,
                                    favorites = favoritesList,
                                    recents = recentsList,
                                    optionIcons = optionIconsMap,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(FireSpacing.Medium))

                        FireButton(
                            text = "CONTINUAR E CRIAR OCORRÊNCIA",
                            enabled = isFormValid,
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
                                            galleryImages = allGalleryImages,
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
                                            onAddApoio = viewModel::addApoio,
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
                                        EvidenciasModuleView(
                                            uiState = uiState,
                                            onTakePhoto = {
                                                launchCameraWithPermissionCheck("EVIDENCIA")
                                            },
                                            galleryImages = allGalleryImages,
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
            }

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

    // Dialog: Add/Edit Viatura
    if (showAddViaturaDialog || viaturaToEdit != null) {
        AddViaturaDialog(
            viatura = viaturaToEdit,
            onDismiss = {
                showAddViaturaDialog = false
                viaturaToEdit = null
            },
            onConfirm = { prefixo, tipo, unidade, kmSaida, kmLocal, obs ->
                viewModel.addViatura(prefixo, tipo, unidade, kmSaida, kmLocal, obs, viaturaToEdit?.id)
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
            ModuleInfo("Documentos", "📄📄", uiState.documentos.size, "${uiState.documentos.size} documentos", calculateStatus(OccurrenceModule.DOCUMENTOS, uiState), onModuleSelected),
            ModuleInfo("Viaturas", "🚒🚒", uiState.viaturas.size, "${uiState.viaturas.size} viaturas", calculateStatus(OccurrenceModule.VIATURAS, uiState), onModuleSelected),
            ModuleInfo("Militares", "👨👨🚒🚒", uiState.viaturas.sumOf { it.equipe.size }, "${uiState.viaturas.sumOf { it.equipe.size }} militares", calculateStatus(OccurrenceModule.MILITARES, uiState), onModuleSelected),
            ModuleInfo("Veículos", "🚗🚗", uiState.veiculos.size, "${uiState.veiculos.size} veículos", calculateStatus(OccurrenceModule.VEICULOS, uiState), onModuleSelected),
            ModuleInfo("Vítimas", "🩺🩺", uiState.vitimas.size, "${uiState.vitimas.size} vítimas", calculateStatus(OccurrenceModule.VITIMAS, uiState), onModuleSelected),
            ModuleInfo("Apoios", "🤝🤝", uiState.apoiosDetalhados.size, "${uiState.apoiosDetalhados.size} apoios", calculateStatus(OccurrenceModule.APOIOS, uiState), onModuleSelected),
            ModuleInfo("Histórico", "📝📝", if (uiState.historico.isNotEmpty()) 1 else 0, if (uiState.historico.isNotEmpty()) "Preenchido" else "Não iniciado", calculateStatus(OccurrenceModule.HISTORICO, uiState), onModuleSelected),
            ModuleInfo("Evidências", "📷📷", uiState.evidencias.size, "${uiState.evidencias.size} evidências", calculateStatus(OccurrenceModule.EVIDENCIAS, uiState), onModuleSelected),
            ModuleInfo("Anexos", "📎📎", uiState.fotos.size + uiState.videos.size, "${uiState.fotos.size + uiState.videos.size} mídias", calculateStatus(OccurrenceModule.ANEXOS, uiState), onModuleSelected)
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
    galleryImages: List<GalleryImage>,
    onImageClick: (GalleryImage) -> Unit,
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

        FireGalleryCard(
            title = "Galeria de Fotos do Local",
            category = "Local",
            images = galleryImages,
            onImageClick = onImageClick
        )

        FireButton(text = "Voltar ao Dashboard", onClick = onBack, modifier = Modifier.fillMaxWidth())
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ViaturasModuleView(
    uiState: OccurrenceFormUiState,
    onNewViaturaClick: () -> Unit,
    onEditViaturaClick: (Viatura) -> Unit,
    onDeleteViatura: (String) -> Unit,
    galleryImages: List<GalleryImage>,
    onImageClick: (GalleryImage) -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(FireSpacing.Medium)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = FireSpacing.Medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🚒🚒 ", style = FireTypography.HeadlineMedium)
                    Column {
                        Text("Viatura", style = FireTypography.Headline, fontWeight = FontWeight.Bold)
                        Text("Cadastre os dados da viatura utilizada nesta ocorrência.", style = FireTypography.LabelMedium, color = FireColors.OnSurfaceVariant)
                    }
                }
            }

            item {
                FireGalleryCard(
                    title = "Galeria de Viaturas",
                    category = "Viatura",
                    images = galleryImages,
                    onImageClick = onImageClick
                )
            }

            if (uiState.viaturas.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        Text("Nenhuma viatura registrada.", style = FireTypography.BodyMedium, color = Color.Gray)
                    }
                }
            } else {
                items(
                    items = uiState.viaturas,
                    key = { it.id ?: it.prefixo }
                ) { viatura ->
                    val startVal = viatura.kmSaida ?: 0
                    val endVal = viatura.kmLocal ?: 0
                    val diff = if (endVal >= startVal) endVal - startVal else 0
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = FireColors.SurfaceVariant.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(FireSpacing.Medium)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🚒🚒 ", style = FireTypography.Title)
                                    Text(viatura.prefixo, style = FireTypography.Title, fontWeight = FontWeight.Bold)
                                }
                                
                                val count = viatura.equipe.size
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("👨🚒 ", style = FireTypography.LabelMedium)
                                    Text(
                                        text = "$count militar${if (count != 1) "es" else ""}",
                                        style = FireTypography.LabelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = FireColors.Primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(FireSpacing.ExtraSmall))
                            
                            Text(
                                text = viatura.unidade ?: "Unidade não especificada",
                                style = FireTypography.BodyMedium,
                                color = FireColors.OnSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(FireSpacing.Small))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("KM", style = FireTypography.LabelSmall, color = FireColors.OnSurfaceVariant)
                                    Text(
                                        text = "${viatura.kmSaida ?: 0} → ${viatura.kmLocal ?: 0}",
                                        style = FireTypography.BodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Percorrido", style = FireTypography.LabelSmall, color = FireColors.OnSurfaceVariant)
                                    Text(
                                        text = "$diff km",
                                        style = FireTypography.BodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = FireColors.Primary
                                    )
                                }
                            }

                            if (!viatura.observacoes.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(FireSpacing.Small))
                                Text(
                                    text = "Obs: ${viatura.observacoes}",
                                    style = FireTypography.LabelSmall,
                                    color = FireColors.OnSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = FireSpacing.Small))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { onEditViaturaClick(viatura) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = FireColors.Primary)
                                ) {
                                    Text("✏ Editar")
                                }
                                Spacer(modifier = Modifier.width(FireSpacing.Small))
                                TextButton(
                                    onClick = { onDeleteViatura(viatura.id!!) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = FireColors.Error)
                                ) {
                                    Text("🗑 Remover")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(FireSpacing.Small))
                FireButton(text = "Voltar ao Dashboard", onClick = onBack, modifier = Modifier.fillMaxWidth())
            }
        }

        // Floating Action Button (FAB) at bottom right
        FloatingActionButton(
            onClick = onNewViaturaClick,
            containerColor = FireColors.Primary,
            contentColor = androidx.compose.ui.graphics.Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 60.dp, end = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = FireSpacing.Medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = FireIcons.Add, contentDescription = "Nova Viatura")
                Spacer(modifier = Modifier.width(FireSpacing.ExtraSmall))
                Text("Nova Viatura", fontWeight = FontWeight.Bold)
            }
        }
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
    galleryImages: List<GalleryImage>,
    onImageClick: (GalleryImage) -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(FireSpacing.Medium)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = FireSpacing.Medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🚗🚗 ", style = FireTypography.HeadlineMedium)
                    Column {
                        Text("Veículos", style = FireTypography.Headline, fontWeight = FontWeight.Bold)
                        Text("Cadastre os veículos envolvidos nesta ocorrência.", style = FireTypography.LabelMedium, color = FireColors.OnSurfaceVariant)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = FireSpacing.Medium),
                    horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                ) {
                    FireButton(
                        text = "Escanear CRLV",
                        onClick = onScanCrlvClick,
                        containerColor = FireColors.Secondary,
                        modifier = Modifier.weight(1f),
                        icon = FireIcons.PhotoCamera
                    )
                }
            }

            item {
                FireGalleryCard(
                    title = "Galeria de CRLVs",
                    category = "Veículo",
                    images = galleryImages,
                    onImageClick = onImageClick
                )
            }

            if (uiState.veiculos.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        Text("Nenhum veículo registrado.", style = FireTypography.BodyMedium, color = Color.Gray)
                    }
                }
            } else {
                items(
                    items = uiState.veiculos,
                    key = { it.id ?: it.placa ?: java.util.UUID.randomUUID().toString() }
                ) { veiculo ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = FireColors.SurfaceVariant.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                    ) {
                        Column(modifier = Modifier.padding(FireSpacing.Medium)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🚗 ", style = FireTypography.Title)
                                    Text(veiculo.placa ?: "SEM PLACA", style = FireTypography.Title, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(FireSpacing.ExtraSmall))

                            Text(
                                text = "Modelo: ${veiculo.modelo ?: "N/D"} | Cor: ${veiculo.cor ?: "N/D"}",
                                style = FireTypography.BodyMedium,
                                color = FireColors.OnSurfaceVariant
                            )

                            if (!veiculo.chassi.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(FireSpacing.ExtraSmall))
                                Text(
                                    text = "Chassi: ${veiculo.chassi}",
                                    style = FireTypography.LabelSmall,
                                    color = FireColors.OnSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(FireSpacing.Small))
                FireButton(text = "Voltar ao Dashboard", onClick = onBack, modifier = Modifier.fillMaxWidth())
            }
        }

        FloatingActionButton(
            onClick = onNewVehicleClick,
            containerColor = FireColors.Primary,
            contentColor = androidx.compose.ui.graphics.Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 60.dp, end = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = FireSpacing.Medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = FireIcons.Add, contentDescription = "Novo Veículo")
                Spacer(modifier = Modifier.width(FireSpacing.ExtraSmall))
                Text("Novo Veículo", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DocumentosModuleView(
    uiState: OccurrenceFormUiState,
    onNewDocClick: () -> Unit,
    onScanDocClick: () -> Unit,
    galleryImages: List<GalleryImage>,
    onImageClick: (GalleryImage) -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(FireSpacing.Medium)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = FireSpacing.Medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📄📄 ", style = FireTypography.HeadlineMedium)
                    Column {
                        Text("Documentos", style = FireTypography.Headline, fontWeight = FontWeight.Bold)
                        Text("Cadastre os documentos das pessoas envolvidas.", style = FireTypography.LabelMedium, color = FireColors.OnSurfaceVariant)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = FireSpacing.Medium),
                    horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                ) {
                    FireButton(
                        text = "Escanear Documento",
                        onClick = onScanDocClick,
                        containerColor = FireColors.Secondary,
                        modifier = Modifier.weight(1f),
                        icon = FireIcons.PhotoCamera
                    )
                }
            }

            item {
                FireGalleryCard(
                    title = "Galeria de Documentos",
                    category = "Documento",
                    images = galleryImages,
                    onImageClick = onImageClick
                )
            }

            if (uiState.documentos.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        Text("Nenhum documento registrado.", style = FireTypography.BodyMedium, color = Color.Gray)
                    }
                }
            } else {
                items(
                    items = uiState.documentos,
                    key = { it.id ?: it.numero ?: java.util.UUID.randomUUID().toString() }
                ) { doc ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = FireColors.SurfaceVariant.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                    ) {
                        Column(modifier = Modifier.padding(FireSpacing.Medium)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("📄 ", style = FireTypography.Title)
                                    Text(doc.tipo, style = FireTypography.Title, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(FireSpacing.ExtraSmall))

                            Text(
                                text = "Número: ${doc.numero ?: "N/D"}",
                                style = FireTypography.BodyMedium,
                                color = FireColors.OnSurfaceVariant
                            )
                            val ownerName = doc.dadosEstruturados["nome"]
                            val ownerCpf = doc.dadosEstruturados["cpf"]
                            if (!ownerName.isNullOrBlank()) {
                                Text(
                                    text = "Nome: $ownerName",
                                    style = FireTypography.BodyMedium,
                                    color = FireColors.OnSurfaceVariant
                                )
                            }
                            if (!ownerCpf.isNullOrBlank()) {
                                Text(
                                    text = "CPF: $ownerCpf",
                                    style = FireTypography.BodyMedium,
                                    color = FireColors.OnSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(FireSpacing.Small))
                FireButton(text = "Voltar ao Dashboard", onClick = onBack, modifier = Modifier.fillMaxWidth())
            }
        }

        FloatingActionButton(
            onClick = onNewDocClick,
            containerColor = FireColors.Primary,
            contentColor = androidx.compose.ui.graphics.Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 60.dp, end = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = FireSpacing.Medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = FireIcons.Add, contentDescription = "Novo Documento")
                Spacer(modifier = Modifier.width(FireSpacing.ExtraSmall))
                Text("Novo Documento", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun VitimasModuleView(
    uiState: OccurrenceFormUiState,
    onNewVictimClick: () -> Unit,
    galleryImages: List<GalleryImage>,
    onImageClick: (GalleryImage) -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(FireSpacing.Medium)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = FireSpacing.Medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🩺🩺 ", style = FireTypography.HeadlineMedium)
                    Column {
                        Text("Vítimas", style = FireTypography.Headline, fontWeight = FontWeight.Bold)
                        Text("Cadastre as vítimas atendidas nesta ocorrência.", style = FireTypography.LabelMedium, color = FireColors.OnSurfaceVariant)
                    }
                }
            }

            item {
                FireGalleryCard(
                    title = "Galeria de Vítimas",
                    category = "Vítima",
                    images = galleryImages,
                    onImageClick = onImageClick
                )
            }

            if (uiState.vitimas.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        Text("Nenhuma vítima registrada.", style = FireTypography.BodyMedium, color = Color.Gray)
                    }
                }
            } else {
                items(
                    items = uiState.vitimas,
                    key = { it.id ?: (it.nome ?: java.util.UUID.randomUUID().toString()) }
                ) { vitima ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = FireColors.SurfaceVariant.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(FireSpacing.Medium)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("👤 ", style = FireTypography.Title)
                                    Text(vitima.nome ?: "N/D", style = FireTypography.Title, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(FireSpacing.ExtraSmall))

                            Text(
                                text = "Lesões: ${vitima.lesoesAparentes ?: "N/D"} | Destino: ${vitima.hospitalDestino ?: "Não informado"}",
                                style = FireTypography.BodyMedium,
                                color = FireColors.OnSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(FireSpacing.ExtraSmall))

                            Text(
                                text = "Glasgow: ${vitima.sinaisVitais.escalaGCS ?: "N/D"} | FC: ${vitima.sinaisVitais.pulso ?: "N/D"}",
                                style = FireTypography.LabelSmall,
                                color = FireColors.Primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(FireSpacing.Small))
                FireButton(text = "Voltar ao Dashboard", onClick = onBack, modifier = Modifier.fillMaxWidth())
            }
        }

        FloatingActionButton(
            onClick = onNewVictimClick,
            containerColor = FireColors.Primary,
            contentColor = androidx.compose.ui.graphics.Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 60.dp, end = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = FireSpacing.Medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = FireIcons.Add, contentDescription = "Nova Vítima")
                Spacer(modifier = Modifier.width(FireSpacing.ExtraSmall))
                Text("Nova Vítima", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ApoiosModuleView(
    uiState: OccurrenceFormUiState,
    onAddApoio: (OrgaoApoio, String, String) -> Unit,
    onRemoveApoio: (Int) -> Unit,
    galleryImages: List<GalleryImage>,
    onImageClick: (GalleryImage) -> Unit,
    onBack: () -> Unit
) {
    var selectedOrgaoIndex by remember { mutableStateOf(0) }
    var viatura by remember { mutableStateOf("") }
    var encarregado by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(FireSpacing.Medium)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🤝🤝 ", style = FireTypography.HeadlineMedium)
                Column {
                    Text("Apoios", style = FireTypography.Headline, fontWeight = FontWeight.Bold)
                    Text("Vincule os órgãos de apoio que prestaram auxílio.", style = FireTypography.LabelMedium, color = FireColors.OnSurfaceVariant)
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = FireColors.SurfaceVariant.copy(alpha = 0.4f)),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(FireSpacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                ) {
                    Text("🤝 Novo Apoio", style = FireTypography.Title, fontWeight = FontWeight.Bold, color = FireColors.Primary)
                    
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
                        FireOutlinedTextField(value = viatura, onValueChange = { viatura = it.uppercase(java.util.Locale("pt", "BR")) }, label = "Viatura", modifier = Modifier.weight(1f))
                        FireOutlinedTextField(value = encarregado, onValueChange = { encarregado = it.uppercase(java.util.Locale("pt", "BR")) }, label = "Encarregado", modifier = Modifier.weight(1f))
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
                }
            }

            if (uiState.apoiosDetalhados.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Nenhum apoio registrado.", style = FireTypography.BodyMedium, color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(
                        count = uiState.apoiosDetalhados.size,
                        key = { idx -> uiState.apoiosDetalhados[idx].orgao.id + uiState.apoiosDetalhados[idx].viatura }
                    ) { idx ->
                        val apoio = uiState.apoiosDetalhados[idx]
                        Card(
                            colors = CardDefaults.cardColors(containerColor = FireColors.SurfaceVariant.copy(alpha = 0.5f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(FireSpacing.Medium),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${apoio.orgao.sigla} - ${apoio.orgao.nome}", style = FireTypography.BodyMedium, fontWeight = FontWeight.Bold)
                                    Text("Viatura: ${apoio.viatura} | Encarregado: ${apoio.encarregado}", style = FireTypography.Caption)
                                }
                                FireIconButton(icon = FireIcons.Delete, onClick = { onRemoveApoio(idx) }, tint = FireColors.Error)
                            }
                        }
                    }
                }
            }

            FireGalleryCard(
                title = "Galeria de Apoios",
                category = "Apoio",
                images = galleryImages,
                onImageClick = onImageClick,
                modifier = Modifier.heightIn(max = 180.dp)
            )

            Spacer(modifier = Modifier.height(FireSpacing.Small))
            FireButton(text = "Voltar ao Dashboard", onClick = onBack, modifier = Modifier.fillMaxWidth())
        }
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
    galleryImages: List<GalleryImage>,
    onImageClick: (GalleryImage) -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(FireSpacing.Medium)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = FireSpacing.Medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📎📎 ", style = FireTypography.HeadlineMedium)
                    Column {
                        Text("Evidências", style = FireTypography.Headline, fontWeight = FontWeight.Bold)
                        Text("Fotografe e organize evidências físicas no local.", style = FireTypography.LabelMedium, color = FireColors.OnSurfaceVariant)
                    }
                }
            }

            item {
                FireGalleryCard(
                    title = "Galeria de Evidências",
                    category = "Evidência",
                    images = galleryImages,
                    onImageClick = onImageClick
                )
            }

            if (uiState.evidencias.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        Text("Nenhuma evidência registrada.", style = FireTypography.BodyMedium, color = Color.Gray)
                    }
                }
            } else {
                items(
                    items = uiState.evidencias,
                    key = { it.id ?: (it.urlStorage.takeIf { it.isNotEmpty() } ?: it.hashSha256) }
                ) { ev ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = FireColors.SurfaceVariant.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(FireSpacing.Medium),
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

            item {
                Spacer(modifier = Modifier.height(FireSpacing.Small))
                FireButton(text = "Voltar ao Dashboard", onClick = onBack, modifier = Modifier.fillMaxWidth())
            }
        }

        FloatingActionButton(
            onClick = onTakePhoto,
            containerColor = FireColors.Primary,
            contentColor = androidx.compose.ui.graphics.Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 60.dp, end = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = FireSpacing.Medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = FireIcons.PhotoCamera, contentDescription = "Fotografar Evidência")
                Spacer(modifier = Modifier.width(FireSpacing.ExtraSmall))
                Text("Tirar Foto", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnexosModuleView(
    uiState: OccurrenceFormUiState,
    onAddMedia: () -> Unit,
    galleryImages: List<GalleryImage>,
    onImageClick: (GalleryImage) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(FireSpacing.Medium)
    ) {
        FireButton(onClick = onAddMedia, text = "Carregar Foto / Arquivo", icon = FireIcons.CloudUpload, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(FireSpacing.Medium))

        FireGalleryCard(
            title = "Galeria de Anexos",
            category = "Anexo",
            images = galleryImages,
            onImageClick = onImageClick,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.height(FireSpacing.Small))
        FireButton(text = "Voltar ao Dashboard", onClick = onBack, modifier = Modifier.fillMaxWidth())
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
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = FireColors.SurfaceVariant.copy(alpha = 0.4f)),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(FireSpacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                ) {
                    Text("👤 Dados Pessoais", style = FireTypography.Title, fontWeight = FontWeight.Bold, color = FireColors.Primary)
                    Text("Dados pessoais da pessoa vinculada ao documento.", style = FireTypography.LabelMedium, color = FireColors.OnSurfaceVariant)

                    FireOutlinedTextField(value = nome, onValueChange = { nome = it }, label = "Nome Completo", colors = getFieldColors("nome"))
                    FireOutlinedTextField(value = cpf, onValueChange = { cpf = it }, label = "CPF", colors = getFieldColors("cpf"))
                    FireOutlinedTextField(value = nascimento, onValueChange = { nascimento = it }, label = "Data de Nascimento", colors = getFieldColors("nascimento"))
                    FireOutlinedTextField(value = filiacao, onValueChange = { filiacao = it }, label = "Filiação (Pai/Mãe)", colors = getFieldColors("filiacao"))
                    FireOutlinedTextField(value = naturalidade, onValueChange = { naturalidade = it }, label = "Naturalidade", colors = getFieldColors("naturalidade"))
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = FireColors.SurfaceVariant.copy(alpha = 0.4f)),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(FireSpacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                ) {
                    Text("🪪 Dados do Documento", style = FireTypography.Title, fontWeight = FontWeight.Bold, color = FireColors.Primary)
                    Text("Identificação e emissores do documento.", style = FireTypography.LabelMedium, color = FireColors.OnSurfaceVariant)

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
                    FireOutlinedTextField(value = rg, onValueChange = { rg = it }, label = "RG", colors = getFieldColors("rg"))
                    FireOutlinedTextField(value = rgOrgaoEmissor, onValueChange = { rgOrgaoEmissor = it.uppercase(java.util.Locale("pt", "BR")) }, label = "Órgão Emissor", colors = getFieldColors("rg_orgao_emissor"))
                    FireOutlinedTextField(value = rgUf, onValueChange = { rgUf = it.uppercase(java.util.Locale("pt", "BR")) }, label = "UF do Órgão", colors = getFieldColors("rg_uf"))
                }
            }
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
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = FireColors.SurfaceVariant.copy(alpha = 0.4f)),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(FireSpacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                ) {
                    Text("🚗 Identificação do Veículo", style = FireTypography.Title, fontWeight = FontWeight.Bold, color = FireColors.Primary)
                    Text("Dados de identificação e registro do veículo.", style = FireTypography.LabelMedium, color = FireColors.OnSurfaceVariant)
                    
                    FireOutlinedTextField(value = placa, onValueChange = { placa = it.uppercase(java.util.Locale("pt", "BR")) }, label = "Placa", colors = getFieldColors("placa"))
                    FireOutlinedTextField(value = modelo, onValueChange = { modelo = it }, label = "Modelo/Marca", colors = getFieldColors("marca_modelo"))
                    FireOutlinedTextField(value = cor, onValueChange = { cor = it }, label = "Cor", colors = getFieldColors("cor"))
                    FireOutlinedTextField(value = chassi, onValueChange = { chassi = it.uppercase(java.util.Locale("pt", "BR")) }, label = "Chassi", colors = getFieldColors("chassi"))
                    FireOutlinedTextField(value = ano, onValueChange = { ano = it }, label = "Ano", colors = getFieldColors("ano_modelo"))
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = FireColors.SurfaceVariant.copy(alpha = 0.4f)),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(FireSpacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                ) {
                    Text("👤 Proprietário", style = FireTypography.Title, fontWeight = FontWeight.Bold, color = FireColors.Primary)
                    Text("Selecione a pessoa proprietária do veículo envolvido.", style = FireTypography.LabelMedium, color = FireColors.OnSurfaceVariant)

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

            Card(
                colors = CardDefaults.cardColors(containerColor = FireColors.SurfaceVariant.copy(alpha = 0.4f)),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(FireSpacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(FireSpacing.Small),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("📄 Documento CRLV", style = FireTypography.Title, fontWeight = FontWeight.Bold, color = FireColors.Primary)
                    Text("CRLV anexado para este veículo.", style = FireTypography.LabelMedium, color = FireColors.OnSurfaceVariant)

                    if (crlvImage != null) {
                        Spacer(modifier = Modifier.height(FireSpacing.Small))
                        Text("✅ Documento CRLV capturado com sucesso", color = FireColors.Success, fontWeight = FontWeight.Bold, style = FireTypography.BodyMedium)
                    } else {
                        Text("Nenhum CRLV capturado para este veículo.", style = FireTypography.BodyMedium, color = Color.Gray)
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
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = FireColors.SurfaceVariant.copy(alpha = 0.4f)),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(FireSpacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                ) {
                    Text("👤 Identificação da Vítima", style = FireTypography.Title, fontWeight = FontWeight.Bold, color = FireColors.Primary)
                    Text("Selecione a pessoa e informe o estado.", style = FireTypography.LabelMedium, color = FireColors.OnSurfaceVariant)

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

                    FireOutlinedTextField(value = resultado, onValueChange = { resultado = it }, label = "Resultado / Estado")
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = FireColors.SurfaceVariant.copy(alpha = 0.4f)),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(FireSpacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                ) {
                    Text("🩺 Avaliação e Sinais Vitais", style = FireTypography.Title, fontWeight = FontWeight.Bold, color = FireColors.Primary)
                    Text("Parâmetros clínicos avaliados no local.", style = FireTypography.LabelMedium, color = FireColors.OnSurfaceVariant)

                    Row(horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small)) {
                        FireOutlinedTextField(value = pulso, onValueChange = { pulso = it }, label = "FC (BPM)", modifier = Modifier.weight(1f))
                        FireOutlinedTextField(value = pa, onValueChange = { pa = it }, label = "P.A.", modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small)) {
                        FireOutlinedTextField(value = satO2, onValueChange = { satO2 = it }, label = "Sat. O2 (%)", modifier = Modifier.weight(1f))
                        FireOutlinedTextField(value = temp, onValueChange = { temp = it }, label = "Temp (°C)", modifier = Modifier.weight(1f))
                    }
                    FireOutlinedTextField(value = gcs, onValueChange = { gcs = it }, label = "Escala Glasgow (GCS)")
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = FireColors.SurfaceVariant.copy(alpha = 0.4f)),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(FireSpacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                ) {
                    Text("🩹 Lesões Aparentes", style = FireTypography.Title, fontWeight = FontWeight.Bold, color = FireColors.Primary)
                    Text("Descreva as lesões observadas na vítima.", style = FireTypography.LabelMedium, color = FireColors.OnSurfaceVariant)

                    FireOutlinedTextField(value = lesoes, onValueChange = { lesoes = it }, label = "Descrição das Lesões")
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = FireColors.SurfaceVariant.copy(alpha = 0.4f)),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(FireSpacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                ) {
                    Text("🚑 Transporte e Socorro", style = FireTypography.Title, fontWeight = FontWeight.Bold, color = FireColors.Primary)
                    Text("Dados do destino e viatura de transporte.", style = FireTypography.LabelMedium, color = FireColors.OnSurfaceVariant)

                    Box(modifier = Modifier.fillMaxWidth()) {
                        FireOutlinedButton(
                            text = if (selectedViaturaSocorroIndex >= 0 && selectedViaturaSocorroIndex < viaturasDisponiveis.size) {
                                viaturasDisponiveis[selectedViaturaSocorroIndex].prefixo
                            } else {
                                "Selecione a Viatura de Socorro"
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

                    FireOutlinedTextField(value = hospitalDestino, onValueChange = { hospitalDestino = it }, label = "Hospital / Destino")
                    FireOutlinedTextField(value = outroDestino, onValueChange = { outroDestino = it }, label = "Destino Geral")
                    FireOutlinedTextField(value = quemSocorreu, onValueChange = { quemSocorreu = it }, label = "Quem Socorreu / Responsável")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddViaturaDialog(
    viatura: Viatura? = null,
    onDismiss: () -> Unit,
    onConfirm: (prefixo: String, tipo: String, unidade: String?, kmSaida: Int?, kmLocal: Int?, observacoes: String?) -> Unit
) {
    var prefixo by remember { mutableStateOf(viatura?.prefixo ?: "") }
    val tipo = remember { viatura?.tipo ?: "UR" }
    var unidade by remember { mutableStateOf(viatura?.unidade ?: "") }
    var kmSaidaRaw by remember { mutableStateOf(viatura?.kmSaida?.toString() ?: "") }
    var kmLocalRaw by remember { mutableStateOf(viatura?.kmLocal?.toString() ?: "") }
    var observacoes by remember { mutableStateOf(viatura?.observacoes ?: "") }

    val sa = kmSaidaRaw.toIntOrNull()
    val qu = kmLocalRaw.toIntOrNull()
    val isKmInvalid = sa != null && qu != null && qu < sa

    val isComplete = prefixo.isNotBlank() && unidade.isNotBlank() && kmSaidaRaw.isNotBlank() && kmLocalRaw.isNotBlank() && !isKmInvalid

    fun formatNumber(input: String): String {
        val digits = input.filter { it.isDigit() }
        if (digits.isEmpty()) return ""
        val number = digits.toLongOrNull() ?: return ""
        return java.text.NumberFormat.getInstance(java.util.Locale("pt", "BR")).format(number)
    }

    val distancia = remember(kmSaidaRaw, kmLocalRaw) {
        val startVal = kmSaidaRaw.toIntOrNull() ?: 0
        val endVal = kmLocalRaw.toIntOrNull() ?: 0
        if (endVal >= startVal) endVal - startVal else 0
    }

    FireDialog(
        onDismissRequest = onDismiss,
        title = if (viatura == null) "Adicionar Viatura" else "Editar Viatura",
        confirmButton = {
            FireButton(
                enabled = prefixo.isNotBlank() && !isKmInvalid,
                onClick = {
                    onConfirm(
                        prefixo.uppercase().trim(),
                        tipo,
                        unidade.ifBlank { null },
                        kmSaidaRaw.toIntOrNull(),
                        kmLocalRaw.toIntOrNull(),
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
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
        ) {
            // Status Chip indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isComplete) {
                    Text(
                        text = "🟢🟢 Dados completos",
                        color = FireColors.Success,
                        style = FireTypography.LabelMedium,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "🟠🟠 Faltam informações",
                        color = FireColors.Warning,
                        style = FireTypography.LabelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // CARD 1: Identificação
            Card(
                colors = CardDefaults.cardColors(containerColor = FireColors.SurfaceVariant.copy(alpha = 0.4f)),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(FireSpacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                ) {
                    Text("🚒 Identificação", style = FireTypography.Title, fontWeight = FontWeight.Bold, color = FireColors.Primary)
                    
                    FireOutlinedTextField(
                        value = prefixo,
                        onValueChange = { prefixo = it.uppercase() },
                        label = "Prefixo",
                        placeholder = { Text("Ex.: UR-12345") }
                    )

                    FireOutlinedTextField(
                        value = unidade,
                        onValueChange = { 
                            if (it.length <= 80) {
                                unidade = it.uppercase(java.util.Locale("pt", "BR"))
                            }
                        },
                        label = "Unidade/Batalhão",
                        placeholder = { Text("Ex.: 10º GB") },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            autoCorrectEnabled = false
                        ),
                        leadingIcon = { Text("🏢", modifier = Modifier.padding(start = 12.dp), style = FireTypography.Title) }
                    )
                }
            }

            // CARD 2: Quilometragem
            Card(
                colors = CardDefaults.cardColors(containerColor = FireColors.SurfaceVariant.copy(alpha = 0.4f)),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(FireSpacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                ) {
                    Text("🛣 Quilometragem", style = FireTypography.Title, fontWeight = FontWeight.Bold, color = FireColors.Primary)
                    
                    FireOutlinedTextField(
                        value = formatNumber(kmSaidaRaw),
                        onValueChange = { kmSaidaRaw = it.filter { char -> char.isDigit() } },
                        label = "KM Saída",
                        placeholder = { Text("Ex.: 125430") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        suffix = { Text("km", style = FireTypography.BodyMedium) }
                    )

                    FireOutlinedTextField(
                        value = formatNumber(kmLocalRaw),
                        onValueChange = { kmLocalRaw = it.filter { char -> char.isDigit() } },
                        label = "KM Quartel",
                        placeholder = { Text("Ex.: 125470") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        error = isKmInvalid,
                        suffix = { Text("km", style = FireTypography.BodyMedium) }
                    )

                    if (isKmInvalid) {
                        Text(
                            text = "O KM de retorno ao quartel não pode ser menor que o KM de saída.",
                            color = FireColors.Error,
                            style = FireTypography.LabelSmall,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = FireSpacing.ExtraSmall))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Distância Percorrida:", style = FireTypography.BodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "${formatNumber(distancia.toString()).ifBlank { "0" }} km",
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
                onValueChange = { if (it.length <= 1000) observacoes = it },
                label = { Text("Observações", style = FireTypography.BodyMedium) },
                placeholder = { Text("Ex.: Viatura abastecida, pane elétrica...", style = FireTypography.BodyMedium) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                shape = FireShapes.Medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FireColors.Primary,
                    unfocusedBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.5f),
                    focusedLabelColor = FireColors.Primary,
                    unfocusedLabelColor = FireColors.OnSurfaceVariant
                )
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

