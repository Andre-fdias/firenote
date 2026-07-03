package com.example.firenotes.ui.screens.wizard

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.firenotes.domain.model.*
import com.example.firenotes.ui.designsystem.colors.FireColors
import com.example.firenotes.ui.designsystem.shapes.FireShapes
import com.example.firenotes.ui.designsystem.spacing.FireSpacing
import com.example.firenotes.ui.designsystem.typography.FireTypography
import com.example.firenotes.ui.designsystem.icons.FireIcons
import com.example.firenotes.ui.designsystem.components.topbar.FireTopBar
import com.example.firenotes.ui.designsystem.components.buttons.*
import com.example.firenotes.ui.designsystem.components.cards.FireCard
import com.example.firenotes.ui.designsystem.components.inputs.FireOutlinedTextField
import com.example.firenotes.ui.designsystem.components.dialogs.FireDialog
import com.example.firenotes.ui.designsystem.components.widgets.FireWizardProgress
import com.example.firenotes.ui.designsystem.components.widgets.FireDivider
import com.example.firenotes.ui.designsystem.states.FireLoading

@Composable
fun OccurrenceWizardScreen(
    viewModel: WizardViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val validator = remember { WizardValidator() }
    val checklist = validator.getChecklist(uiState)

    // Camera launcher for Step 4 Batch OCR Documents
    var ocrPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val docOcrLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            ocrPhotoUri?.let { uri ->
                viewModel.addPhotoToOcrQueue(uri)
            }
        }
    }

    // Camera launcher for Step 7 Evidence Scenes
    var evidencePhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showClassificationDialog by remember { mutableStateOf(false) }
    val evidenceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            showClassificationDialog = true
        }
    }

    Scaffold(
        topBar = {
            FireTopBar(
                title = "Modo Assistido - Passo ${uiState.currentStep.number}/9",
                onBackClick = onNavigateBack,
                actions = {
                    FireIconButton(
                        icon = if (uiState.isNightMode) FireIcons.Info else FireIcons.Warning,
                        onClick = { viewModel.toggleNightMode() }
                    )
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(FireSpacing.Medium)
        ) {
            // Wizard Progress Bar
            val progressPercent = (uiState.currentStep.ordinal + 1) * 100 / WizardStep.values().size
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = FireSpacing.Medium)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = uiState.currentStep.title,
                        color = FireColors.Primary,
                        style = FireTypography.Title,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "$progressPercent% concluído",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        style = FireTypography.Label,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(FireSpacing.ExtraSmall))
                FireWizardProgress(
                    currentStep = uiState.currentStep.ordinal + 1,
                    totalSteps = WizardStep.values().size
                )
            }

            // Step Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (uiState.currentStep) {
                    WizardStep.INITIAL_DATA -> StepInitialData(uiState, viewModel)
                    WizardStep.NATURE_SELECTION -> StepNatureSelection(viewModel)
                    WizardStep.VIATURAS_EQUIPE -> StepViaturasEquipe(uiState, viewModel)
                    WizardStep.BATCH_OCR -> StepBatchOcr(
                        uiState, viewModel, docOcrLauncher, { ocrPhotoUri = it }
                    )
                    WizardStep.VEICULOS -> StepVeiculos(uiState, viewModel)
                    WizardStep.VITIMAS -> StepVitimas(uiState, viewModel)
                    WizardStep.EVIDENCIAS -> StepEvidencias(
                        uiState, viewModel, evidenceLauncher, { evidencePhotoUri = it }
                    )
                    WizardStep.HISTORICO -> StepHistorico(uiState, viewModel)
                    WizardStep.CHECKLIST_FINAL -> StepChecklist(checklist, viewModel)
                }
            }

            // Bottom Navigation Buttons (Large glove-friendly targets)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = FireSpacing.Medium),
                horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
            ) {
                if (uiState.currentStep != WizardStep.INITIAL_DATA) {
                    FireButton(
                        text = "ANTERIOR",
                        onClick = { viewModel.prevStep() },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                    )
                }
                
                if (uiState.currentStep == WizardStep.CHECKLIST_FINAL) {
                    FireButton(
                        text = "FINALIZAR",
                        onClick = {
                            viewModel.submitWizardOccurrence {
                                onNavigateBack()
                            }
                        },
                        containerColor = FireColors.Primary,
                        enabled = validator.isWizardComplete(uiState) && !uiState.isLoading,
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                    )
                } else {
                    FireButton(
                        text = "AVANÇAR",
                        onClick = { viewModel.nextStep() },
                        containerColor = FireColors.Primary,
                        enabled = !uiState.isLoading,
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                    )
                }
            }
        }
    }

    // Scene Photo Classification dialog
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
                                viewModel.addEvidenciaCena(uri, type)
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

// Step 1 Layout
@Composable
fun StepInitialData(state: WizardState, viewModel: WizardViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showLocationDeniedDialog by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            viewModel.captureLocation()
        } else {
            showLocationDeniedDialog = true
        }
    }

    if (showLocationDeniedDialog) {
        FireDialog(
            onDismissRequest = { showLocationDeniedDialog = false },
            title = "Permissão Necessária",
            confirmButton = {
                FireButton(
                    text = "Permitir Novamente",
                    onClick = {
                        showLocationDeniedDialog = false
                        locationPermissionLauncher.launch(
                            arrayOf(
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )
            },
            dismissButton = {
                FireTextButton(
                    text = "Inserir Manualmente",
                    onClick = { showLocationDeniedDialog = false }
                )
            }
        ) {
            Text("É necessário permitir acesso à localização para utilizar o GPS.", style = FireTypography.Body)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
    ) {
        Text("Dados Iniciais da Emergência", style = FireTypography.Title, fontWeight = FontWeight.Bold)
        
        FireOutlinedTextField(
            value = state.protocolo,
            onValueChange = { viewModel.updateInitialData(it, state.data, state.hora) },
            label = "Número do Talão da Ocorrência"
        )

        Row(horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium)) {
            FireOutlinedTextField(
                value = state.data,
                onValueChange = { viewModel.updateInitialData(state.protocolo, it, state.hora) },
                label = "Data",
                modifier = Modifier.weight(1f)
            )
            FireOutlinedTextField(
                value = state.hora,
                onValueChange = { viewModel.updateInitialData(state.protocolo, state.data, it) },
                label = "Hora",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(FireSpacing.ExtraSmall))
        FireButton(
            text = "CAPTURAR GPS DA CENA",
            onClick = {
                val fineCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                val coarseCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (fineCheck || coarseCheck) {
                    viewModel.captureLocation()
                } else {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            },
            containerColor = FireColors.Primary,
            icon = FireIcons.LocationOn,
            modifier = Modifier.fillMaxWidth().height(55.dp)
        )

        if (state.latitude != null) {
            Text("📍 Localizado: ${state.latitude}, ${state.longitude}", style = FireTypography.Body, color = FireColors.Primary, fontWeight = FontWeight.SemiBold)
        }

        FireOutlinedTextField(
            value = state.rua,
            onValueChange = { viewModel.updateAddress(it, state.numero, state.bairro, state.cidade, state.uf) },
            label = "Logradouro (Rua/Av)"
        )
        Row(horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium)) {
            FireOutlinedTextField(
                value = state.numero,
                onValueChange = { viewModel.updateAddress(state.rua, it, state.bairro, state.cidade, state.uf) },
                label = "Nº",
                modifier = Modifier.weight(1f)
            )
            FireOutlinedTextField(
                value = state.bairro,
                onValueChange = { viewModel.updateAddress(state.rua, state.numero, it, state.cidade, state.uf) },
                label = "Bairro",
                modifier = Modifier.weight(2f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium)) {
            FireOutlinedTextField(
                value = state.cidade,
                onValueChange = { viewModel.updateAddress(state.rua, state.numero, state.bairro, it, state.uf) },
                label = "Cidade",
                modifier = Modifier.weight(3f)
            )
            FireOutlinedTextField(
                value = state.uf,
                onValueChange = { viewModel.updateAddress(state.rua, state.numero, state.bairro, state.cidade, it) },
                label = "UF",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// Step 2 Layout
@Composable
fun StepNatureSelection(viewModel: WizardViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
    ) {
        Text("Selecione a Natureza", style = FireTypography.Title, fontWeight = FontWeight.Bold)
        NaturezaOcorrencia.values().forEach { nature ->
            FireButton(
                text = nature.descricao,
                onClick = { viewModel.selectNatureza(nature) },
                containerColor = FireColors.Primary,
                modifier = Modifier.fillMaxWidth().height(60.dp)
            )
        }
    }
}

// Step 3 Layout
@Composable
fun StepViaturasEquipe(state: WizardState, viewModel: WizardViewModel) {
    var prefixo by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("") }
    var selectedViaturaId by remember { mutableStateOf<String?>(null) }
    
    var militarRe by remember { mutableStateOf("") }
    var militarNome by remember { mutableStateOf("") }
    var selectedGradIndex by remember { mutableStateOf(0) }
    var militarFuncao by remember { mutableStateOf("") }
    
    val graduacoes = GraduacaoMilitar.values()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
    ) {
        Text("Empenho de Viaturas e Militares", style = FireTypography.Title, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium)) {
            FireOutlinedTextField(value = prefixo, onValueChange = { prefixo = it }, label = "Prefixo Viatura", modifier = Modifier.weight(1f))
            FireOutlinedTextField(value = tipo, onValueChange = { tipo = it }, label = "Tipo", modifier = Modifier.weight(1f))
        }
        FireButton(
            text = "+ ADICIONAR VIATURA",
            onClick = {
                if (prefixo.isNotBlank()) {
                    viewModel.addViatura(prefixo, tipo, null, null)
                    prefixo = ""
                    tipo = ""
                }
            },
            containerColor = FireColors.Primary,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        )

        FireDivider()

        if (state.viaturas.isNotEmpty()) {
            Text("Viatura Selecionada para Alocar Equipe:", style = FireTypography.Body, fontWeight = FontWeight.SemiBold)
            Box(modifier = Modifier.fillMaxWidth()) {
                var expanded by remember { mutableStateOf(false) }
                val selectedV = state.viaturas.find { it.id == selectedViaturaId } ?: state.viaturas.first()
                if (selectedViaturaId == null) selectedViaturaId = selectedV.id

                FireOutlinedButton(
                    text = selectedV.prefixo,
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    icon = FireIcons.ArrowDropDown
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    state.viaturas.forEach { v ->
                        DropdownMenuItem(text = { Text(v.prefixo, style = FireTypography.Body) }, onClick = {
                            selectedViaturaId = v.id
                            expanded = false
                        })
                    }
                }
            }

            // Alocar Militar
            Row(horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small)) {
                FireOutlinedTextField(value = militarRe, onValueChange = { militarRe = it }, label = "RE", modifier = Modifier.weight(1f))
                FireOutlinedTextField(value = militarNome, onValueChange = { militarNome = it }, label = "Nome Guerra", modifier = Modifier.weight(1.5f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small)) {
                var gradExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1.5f)) {
                    FireOutlinedButton(
                        text = graduacoes[selectedGradIndex].descricao,
                        onClick = { gradExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        icon = FireIcons.ArrowDropDown
                    )
                    DropdownMenu(expanded = gradExpanded, onDismissRequest = { gradExpanded = false }) {
                        graduacoes.forEachIndexed { index, grad ->
                            DropdownMenuItem(text = { Text(grad.descricao, style = FireTypography.Body) }, onClick = {
                                selectedGradIndex = index
                                gradExpanded = false
                            })
                        }
                    }
                }
                FireOutlinedTextField(value = militarFuncao, onValueChange = { militarFuncao = it }, label = "Função", modifier = Modifier.weight(1f))
            }
            FireButton(
                text = "+ ESCALAR MILITAR",
                onClick = {
                    selectedViaturaId?.let { vId ->
                        if (militarRe.isNotBlank() && militarNome.isNotBlank()) {
                            viewModel.addMilitarToViatura(vId, militarRe, militarNome, graduacoes[selectedGradIndex], militarFuncao)
                            militarRe = ""
                            militarNome = ""
                            militarFuncao = ""
                        }
                    }
                },
                containerColor = FireColors.Primary,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            )
        }
    }
}

// Step 4 Layout (Batch OCR)
@Composable
fun StepBatchOcr(
    state: WizardState,
    viewModel: WizardViewModel,
    launcher: ActivityResultLauncher<Uri>,
    onUriPrepared: (Uri) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
    ) {
        Text("Captura em Lote de Documentos", style = FireTypography.Title, fontWeight = FontWeight.Bold)
        
        Row(horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium)) {
            FireButton(
                text = "FOTOGRAFAR DOCUMENTO",
                onClick = {
                    val uri = viewModel.createPhotoUri()
                    onUriPrepared(uri)
                    launcher.launch(uri)
                },
                containerColor = FireColors.Primary,
                icon = FireIcons.Add,
                modifier = Modifier.weight(1.5f).height(65.dp)
            )

            if (state.ocrQueueUris.isNotEmpty()) {
                FireButton(
                    text = "ESCANEAR (${state.ocrQueueUris.size})",
                    onClick = { viewModel.processOcrBatch {} },
                    containerColor = Color(0xFF2E7D32),
                    modifier = Modifier.weight(1f).height(65.dp)
                )
            }
        }

        if (state.isOcrProcessing) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FireLoading()
                Spacer(modifier = Modifier.height(FireSpacing.Small))
                Text("Processando OCR dos documentos...", style = FireTypography.Body)
            }
        }

        // Summary list of completed batch OCR
        if (state.ocrCompletedResults.isNotEmpty()) {
            Text("Resultados Processados:", style = FireTypography.Title, fontWeight = FontWeight.Bold)
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(FireSpacing.Small),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(state.ocrCompletedResults) { res ->
                    FireCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(FireSpacing.Medium), horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium)) {
                            Text(if (res.tipo == "CRLV") "🚗" else "👤", style = FireTypography.Headline)
                            Column {
                                Text("Tipo: ${res.tipo}", style = FireTypography.Body, fontWeight = FontWeight.Bold)
                                if (res.tipo == "CRLV") {
                                    Text("Placa: ${res.extractedFields["placa"]}", style = FireTypography.Body)
                                } else {
                                    Text("Nome: ${res.extractedFields["nome"]}", style = FireTypography.Body)
                                    Text("CPF: ${res.extractedFields["cpf"]}", style = FireTypography.Body)
                                }
                                if (res.isDuplicate) {
                                    Text("⚠️ Documento Duplicado Detectado", color = FireColors.Error, style = FireTypography.Caption, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Step 5 Layout
@Composable
fun StepVeiculos(state: WizardState, viewModel: WizardViewModel) {
    var placa by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf("") }
    var cor by remember { mutableStateOf("") }
    var chassi by remember { mutableStateOf("") }
    var ano by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
    ) {
        Text("Veículos Envolvidos", style = FireTypography.Title, fontWeight = FontWeight.Bold)

        if (state.veiculos.isNotEmpty()) {
            Text("Veículos Registrados:", style = FireTypography.Body, fontWeight = FontWeight.SemiBold)
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)) {
                items(state.veiculos) { veiculo ->
                    FireCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(FireSpacing.Medium)) {
                            Text("Placa: ${veiculo.placa}", style = FireTypography.Body, fontWeight = FontWeight.Bold)
                            Text("Modelo: ${veiculo.modelo} | Cor: ${veiculo.cor}", style = FireTypography.Body)
                        }
                    }
                }
            }
        }

        FireDivider()

        Text("Adicionar Veículo Manualmente:", style = FireTypography.Body, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small)) {
            FireOutlinedTextField(value = placa, onValueChange = { placa = it }, label = "Placa", modifier = Modifier.weight(1f))
            FireOutlinedTextField(value = modelo, onValueChange = { modelo = it }, label = "Modelo", modifier = Modifier.weight(1.5f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small)) {
            FireOutlinedTextField(value = cor, onValueChange = { cor = it }, label = "Cor", modifier = Modifier.weight(1f))
            FireOutlinedTextField(value = chassi, onValueChange = { chassi = it }, label = "Chassi", modifier = Modifier.weight(1.5f))
        }
        FireButton(
            text = "+ INSERIR VEÍCULO",
            onClick = {
                if (placa.isNotBlank()) {
                    viewModel.addManualVehicle(placa, modelo, cor, chassi, ano.toIntOrNull())
                    placa = ""
                    modelo = ""
                    cor = ""
                    chassi = ""
                    ano = ""
                }
            },
            containerColor = FireColors.Primary,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        )
    }
}

// Step 6 Layout
@Composable
fun StepVitimas(state: WizardState, viewModel: WizardViewModel) {
    val persons = viewModel.getParsedPersons()
    var selectedPersonId by remember { mutableStateOf<String?>(null) }
    var lesoes by remember { mutableStateOf("") }
    
    var glasgow by remember { mutableStateOf("") }
    var pa by remember { mutableStateOf("") }
    var pulso by remember { mutableStateOf("") }
    var oximetria by remember { mutableStateOf("") }
    var temp by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
    ) {
        Text("Atendimento e Socorro de Vítimas", style = FireTypography.Title, fontWeight = FontWeight.Bold)

        if (persons.isNotEmpty()) {
            Text("Pessoa Identificada para Registrar Avaliação:", style = FireTypography.Body, fontWeight = FontWeight.SemiBold)
            Box(modifier = Modifier.fillMaxWidth()) {
                var expanded by remember { mutableStateOf(false) }
                val selectedP = persons.find { it.id == selectedPersonId } ?: persons.first()
                if (selectedPersonId == null) selectedPersonId = selectedP.id

                FireOutlinedButton(
                    text = selectedP.nome,
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    icon = FireIcons.ArrowDropDown
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    persons.forEach { p ->
                        DropdownMenuItem(text = { Text(p.nome, style = FireTypography.Body) }, onClick = {
                            selectedPersonId = p.id
                            expanded = false
                        })
                    }
                }
            }

            FireOutlinedTextField(value = lesoes, onValueChange = { lesoes = it }, label = "Lesões Aparentes")
            
            Row(horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small)) {
                FireOutlinedTextField(value = glasgow, onValueChange = { glasgow = it }, label = "Glasgow", modifier = Modifier.weight(1f))
                FireOutlinedTextField(value = pa, onValueChange = { pa = it }, label = "PA", modifier = Modifier.weight(1f))
                FireOutlinedTextField(value = pulso, onValueChange = { pulso = it }, label = "FC/Pulso", modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small)) {
                FireOutlinedTextField(value = oximetria, onValueChange = { oximetria = it }, label = "Sat. O2", modifier = Modifier.weight(1f))
                FireOutlinedTextField(value = temp, onValueChange = { temp = it }, label = "Temp", modifier = Modifier.weight(1f))
            }

            FireButton(
                text = "REGISTRAR VÍTIMA",
                onClick = {
                    selectedPersonId?.let { pId ->
                        viewModel.addVictim(
                            pId, lesoes, glasgow.toIntOrNull(), pa, pulso.toIntOrNull(),
                            oximetria.toIntOrNull(), temp.toDoubleOrNull(), "Hospital Regional",
                            state.viaturas.firstOrNull()?.id, "Estável"
                        )
                        lesoes = ""
                        glasgow = ""
                        pa = ""
                        pulso = ""
                        oximetria = ""
                        temp = ""
                    }
                },
                containerColor = FireColors.Primary,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            )
        } else {
            Text("Nenhuma pessoa cadastrada nos passos anteriores. Retorne ao Passo 4 para escanear documentos.", style = FireTypography.Body, color = Color.Gray)
        }
    }
}

// Step 7 Layout
@Composable
fun StepEvidencias(
    state: WizardState,
    viewModel: WizardViewModel,
    launcher: ActivityResultLauncher<Uri>,
    onUriPrepared: (Uri) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
    ) {
        Text("Registro de Evidências da Ocorrência", style = FireTypography.Title, fontWeight = FontWeight.Bold)

        FireButton(
            text = "FOTOGRAFAR CENA",
            onClick = {
                val uri = viewModel.createPhotoUri()
                onUriPrepared(uri)
                launcher.launch(uri)
            },
            containerColor = FireColors.Primary,
            icon = FireIcons.Add,
            modifier = Modifier.fillMaxWidth().height(65.dp)
        )

        if (state.evidencias.isNotEmpty()) {
            Text("Evidências Registradas:", style = FireTypography.Body, fontWeight = FontWeight.Bold)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(FireSpacing.Small), modifier = Modifier.fillMaxWidth()) {
                items(state.evidencias) { ev ->
                    FireCard(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(FireSpacing.Medium), horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium)) {
                            Text("📁", style = FireTypography.Headline)
                            Column {
                                Text("Classificação: ${ev.classification}", style = FireTypography.Body, fontWeight = FontWeight.Bold)
                                Text("Data/Hora: ${ev.timestamp}", style = FireTypography.Caption, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Step 8 Layout
@Composable
fun StepHistorico(state: WizardState, viewModel: WizardViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
    ) {
        Text("Histórico da Ocorrência", style = FireTypography.Title, fontWeight = FontWeight.Bold)
        
        FireButton(
            text = "GERAR HISTÓRICO ESTRUTURADO",
            onClick = { viewModel.generateOccurrenceNarrative() },
            containerColor = FireColors.Primary,
            icon = FireIcons.Check,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        )

        FireOutlinedTextField(
            value = state.historico,
            onValueChange = { viewModel.updateHistorico(it) },
            label = "Resumo Histórico",
            singleLine = false,
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
    }
}

// Step 9 Layout (Checklist Final)
@Composable
fun StepChecklist(checklist: List<ChecklistItem>, viewModel: WizardViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
    ) {
        Text("Checklist de Validação Final", style = FireTypography.Title, fontWeight = FontWeight.Bold)
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(FireSpacing.Small), modifier = Modifier.fillMaxWidth()) {
            items(checklist) { item ->
                FireCard(
                    containerColor = if (item.isComplete) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.goToStep(item.targetStep)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(FireSpacing.Medium),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
                    ) {
                        Icon(
                            imageVector = if (item.isComplete) FireIcons.CheckCircle else FireIcons.Warning,
                            contentDescription = "Status",
                            tint = if (item.isComplete) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                        Column {
                            Text(item.title, style = FireTypography.Body, fontWeight = FontWeight.Bold, color = if (item.isComplete) Color(0xFF1B5E20) else Color(0xFFC62828))
                            if (!item.isComplete && item.errorDescription != null) {
                                Text(item.errorDescription, style = FireTypography.Caption, color = Color(0xFFC62828))
                            }
                        }
                    }
                }
            }
        }
    }
}
