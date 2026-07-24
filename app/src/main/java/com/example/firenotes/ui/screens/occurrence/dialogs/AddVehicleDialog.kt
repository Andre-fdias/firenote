package com.example.firenotes.ui.screens.occurrence.dialogs

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.example.firenotes.domain.model.Pessoa
import com.example.firenotes.domain.model.VeiculoEnvolvido
import com.example.firenotes.domain.repository.OcrDocumentResult
import com.example.firenotes.ui.designsystem.colors.FireColors
import com.example.firenotes.ui.designsystem.components.buttons.FireButton
import com.example.firenotes.ui.designsystem.components.buttons.FireTextButton
import com.example.firenotes.ui.designsystem.components.dialogs.FireDialog
import com.example.firenotes.ui.designsystem.icons.FireIcons
import com.example.firenotes.ui.designsystem.spacing.FireSpacing
import com.example.firenotes.ui.designsystem.typography.FireTypography
import com.example.firenotes.ui.screens.occurrence.utils.FipeApiClient
import com.example.firenotes.ui.screens.occurrence.utils.FipeBrand
import com.example.firenotes.ui.screens.occurrence.utils.FipeModel
import com.example.firenotes.ui.screens.occurrence.utils.FipeYear
import com.example.firenotes.ui.screens.occurrence.utils.VehicleCatalog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVehicleDialog(
    veiculoParaEditar: VeiculoEnvolvido? = null,
    ocrResult: OcrDocumentResult? = null,
    pessoasDisponiveis: List<Pessoa>,
    crlvImage: Uri? = null,
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
    ) -> Unit,
    onScanCrlvClick: () -> Unit
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
    var currentCrlvImageUri by remember { mutableStateOf<Uri?>(null) }
    
    // Estados de UI auxiliares
    var expandedPessoaDropdown by remember { mutableStateOf(false) }
    var expandedAnoFabDropdown by remember { mutableStateOf(false) }
    var expandedAnoModDropdown by remember { mutableStateOf(false) }
    var expandedFipeYearDropdown by remember { mutableStateOf(false) }
    var placaError by remember { mutableStateOf<String?>(null) }
    
    // Estados de busca Marca/Modelo
    var searchMarca by remember { mutableStateOf("") }
    var searchModelo by remember { mutableStateOf("") }
    var expandedMarca by remember { mutableStateOf(false) }
    var expandedModelo by remember { mutableStateOf(false) }

    // Estados da API FIPE
    var fipeBrands by remember { mutableStateOf<List<FipeBrand>>(emptyList()) }
    var fipeModels by remember { mutableStateOf<List<FipeModel>>(emptyList()) }
    var fipeYears by remember { mutableStateOf<List<FipeYear>>(emptyList()) }
    var selectedFipeBrandId by remember { mutableStateOf<String?>(null) }
    var selectedFipeModelId by remember { mutableStateOf<String?>(null) }
    var isLoadingBrands by remember { mutableStateOf(false) }
    var isLoadingModels by remember { mutableStateOf(false) }
    var isLoadingYears by remember { mutableStateOf(false) }

    // Buscar Marcas ao inicializar
    LaunchedEffect(Unit) {
        isLoadingBrands = true
        try {
            fipeBrands = FipeApiClient.getBrands()
        } catch (e: Exception) {
            android.util.Log.e("FipeAPI", "Erro ao carregar marcas da API: ${e.localizedMessage}")
        } finally {
            isLoadingBrands = false
        }
    }

    // Buscar Modelos quando a marca for selecionada
    LaunchedEffect(selectedFipeBrandId) {
        if (!selectedFipeBrandId.isNullOrBlank()) {
            isLoadingModels = true
            try {
                fipeModels = FipeApiClient.getModels(selectedFipeBrandId!!)
            } catch (e: Exception) {
                android.util.Log.e("FipeAPI", "Erro ao carregar modelos da API: ${e.localizedMessage}")
                fipeModels = emptyList()
            } finally {
                isLoadingModels = false
            }
        } else {
            fipeModels = emptyList()
        }
    }

    // Buscar Anos quando o modelo for selecionado
    LaunchedEffect(selectedFipeBrandId, selectedFipeModelId) {
        if (!selectedFipeBrandId.isNullOrBlank() && !selectedFipeModelId.isNullOrBlank()) {
            isLoadingYears = true
            try {
                fipeYears = FipeApiClient.getYears(selectedFipeBrandId!!, selectedFipeModelId!!)
            } catch (e: Exception) {
                android.util.Log.e("FipeAPI", "Erro ao carregar anos da API: ${e.localizedMessage}")
                fipeYears = emptyList()
            } finally {
                isLoadingYears = false
            }
        } else {
            fipeYears = emptyList()
        }
    }

    // Auto-mapeamento de IDs ao editar veículo existente
    LaunchedEffect(fipeBrands, veiculoParaEditar) {
        if (veiculoParaEditar != null && fipeBrands.isNotEmpty() && selectedFipeBrandId == null) {
            val matchingBrand = fipeBrands.find { it.nome.equals(veiculoParaEditar.marca, ignoreCase = true) }
            if (matchingBrand != null) {
                selectedFipeBrandId = matchingBrand.codigo
            }
        }
    }

    LaunchedEffect(fipeModels, veiculoParaEditar) {
        if (veiculoParaEditar != null && fipeModels.isNotEmpty() && selectedFipeModelId == null) {
            val matchingModel = fipeModels.find { it.nome.equals(veiculoParaEditar.modelo, ignoreCase = true) }
            if (matchingModel != null) {
                selectedFipeModelId = matchingModel.codigo
            }
        }
    }

    // Preencher com OCR ou Dados de Edição
    LaunchedEffect(veiculoParaEditar, ocrResult, crlvImage) {
        if (ocrResult != null) {
            val extractedPlaca = ocrResult.extractedFields["placa"]?.uppercase() ?: ""
            if (extractedPlaca.isNotBlank()) placa = extractedPlaca
            
            val extractedMarca = ocrResult.extractedFields["marca"] ?: ocrResult.extractedFields["marca_modelo"]?.substringBefore("/")?.trim() ?: ""
            if (extractedMarca.isNotBlank()) {
                marca = extractedMarca
                searchMarca = extractedMarca
            }
            
            val extractedModelo = ocrResult.extractedFields["modelo"] ?: ocrResult.extractedFields["marca_modelo"]?.substringAfter("/")?.trim() ?: ""
            if (extractedModelo.isNotBlank()) {
                modelo = extractedModelo
                searchModelo = extractedModelo
            }
            
            val extractedCor = ocrResult.extractedFields["cor"] ?: ""
            if (extractedCor.isNotBlank()) cor = extractedCor
            
            val extractedChassi = ocrResult.extractedFields["chassi"]?.uppercase() ?: ""
            if (extractedChassi.isNotBlank()) chassi = extractedChassi
            
            val extractedEx = ocrResult.extractedFields["exercicio"] ?: ""
            if (extractedEx.isNotBlank()) exercicio = extractedEx
            
            val anoFab = ocrResult.extractedFields["ano_fabricacao"]?.toIntOrNull()
            val anoMod = ocrResult.extractedFields["ano_modelo"]?.toIntOrNull()
            if (anoFab != null) anoFabricacao = anoFab.toString()
            if (anoMod != null) anoModelo = anoMod.toString()
            if (crlvImage != null) {
                currentCrlvImageUri = crlvImage
            }
        } else if (veiculoParaEditar != null) {
            placa = veiculoParaEditar.placa
            modelo = veiculoParaEditar.modelo
            searchModelo = veiculoParaEditar.modelo
            cor = veiculoParaEditar.cor
            chassi = veiculoParaEditar.chassi
            
            val parts = veiculoParaEditar.ano.split("/")
            anoFabricacao = veiculoParaEditar.anoFabricacao?.toString() ?: parts.getOrNull(0)?.trim() ?: ""
            anoModelo = veiculoParaEditar.anoModelo?.toString() ?: parts.getOrNull(1)?.trim() ?: ""
            
            marca = veiculoParaEditar.marca
            searchMarca = veiculoParaEditar.marca
            versao = veiculoParaEditar.versao
            exercicio = veiculoParaEditar.exercicio
            selectedPessoaId = veiculoParaEditar.proprietarioId
            currentCrlvImageUri = if (!veiculoParaEditar.urlCrlv.isNullOrBlank()) Uri.parse(veiculoParaEditar.urlCrlv) else null
        }
    }
    
    // Cores comuns para os chips interativos
    val commonColors = listOf(
        "Branco" to "#FFFFFF",
        "Preto" to "#000000",
        "Prata" to "#C0C0C0",
        "Cinza" to "#808080",
        "Vermelho" to "#D32F2F",
        "Azul" to "#1976D2",
        "Verde" to "#388E3C",
        "Amarelo" to "#FBC02D",
        "Laranja" to "#F57C00",
        "Marrom" to "#795548"
    )

    // Filtros de ano (Offline)
    val currentYear = java.time.Year.now().value
    val yearsRange = remember { ((currentYear + 1) downTo 1970).map { it.toString() } }
    
    // Validação de Placa Mercosul ou Tradicional
    fun validatePlaca(input: String): Boolean {
        val clean = input.uppercase().replace(Regex("[^A-Z0-9]"), "")
        return clean.length == 7 && (
            clean.matches(Regex("^[A-Z]{3}\\d{4}$")) ||
            clean.matches(Regex("^[A-Z]{3}\\d[A-Z]\\d{2}$"))
        )
    }
    
    var showConfirmCancelDialog by remember { mutableStateOf(false) }

    val hasChanges = remember(
        placa, modelo, cor, chassi, anoFabricacao, anoModelo, marca, versao, exercicio, selectedPessoaId, currentCrlvImageUri, veiculoParaEditar
    ) {
        if (veiculoParaEditar != null) {
            val parts = veiculoParaEditar.ano.split("/")
            val vAnoFab = veiculoParaEditar.anoFabricacao?.toString() ?: parts.getOrNull(0)?.trim() ?: ""
            val vAnoMod = veiculoParaEditar.anoModelo?.toString() ?: parts.getOrNull(1)?.trim() ?: ""
            
            placa != veiculoParaEditar.placa ||
            modelo != veiculoParaEditar.modelo ||
            cor != veiculoParaEditar.cor ||
            chassi != veiculoParaEditar.chassi ||
            anoFabricacao != vAnoFab ||
            anoModelo != vAnoMod ||
            marca != veiculoParaEditar.marca ||
            versao != veiculoParaEditar.versao ||
            exercicio != veiculoParaEditar.exercicio ||
            selectedPessoaId != veiculoParaEditar.proprietarioId ||
            currentCrlvImageUri != (if (!veiculoParaEditar.urlCrlv.isNullOrBlank()) Uri.parse(veiculoParaEditar.urlCrlv) else null)
        } else {
            placa.isNotEmpty() || modelo.isNotEmpty() || cor.isNotEmpty() || chassi.isNotEmpty() ||
            anoFabricacao.isNotEmpty() || anoModelo.isNotEmpty() || marca.isNotEmpty() ||
            versao.isNotEmpty() || exercicio.isNotEmpty() || selectedPessoaId != null || currentCrlvImageUri != null
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

    val isFormValid = remember(placa, modelo, cor, chassi, placaError) {
        placa.isNotBlank() &&
        validatePlaca(placa) &&
        modelo.isNotBlank() &&
        cor.isNotBlank() &&
        chassi.isNotBlank() &&
        placaError == null
    }

    // Filtragem de marcas (Online/Offline)
    val filteredBrands = remember(fipeBrands, searchMarca) {
        if (fipeBrands.isNotEmpty()) {
            fipeBrands.filter { it.nome.contains(searchMarca, ignoreCase = true) }
        } else {
            emptyList()
        }
    }
    val filteredOfflineBrands = remember(searchMarca) {
        VehicleCatalog.brands.filter { it.contains(searchMarca, ignoreCase = true) }
    }

    // Filtragem de modelos (Online/Offline)
    val filteredModels = remember(fipeModels, searchModelo) {
        if (fipeModels.isNotEmpty()) {
            fipeModels.filter { it.nome.contains(searchModelo, ignoreCase = true) }
        } else {
            emptyList()
        }
    }
    val filteredOfflineModels = remember(marca, searchModelo) {
        val offline = VehicleCatalog.modelsByBrand[marca] ?: emptyList()
        offline.filter { it.contains(searchModelo, ignoreCase = true) }
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
        title = if (veiculoParaEditar != null) "Editar Veículo" else "Registrar Veículo",
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
                        currentCrlvImageUri
                    )
                },
                text = "Salvar",
                containerColor = FireColors.Primary
            )
        },
        dismissButton = {
            FireTextButton(onClick = { attemptDismiss() }, text = "Cancelar")
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
        ) {
            // Real-time Mercosul Plate Preview (Premium aesthetic)
            if (placa.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .border(2.dp, Color(0xFF263238), RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .width(180.dp)
                    ) {
                        // Tarja azul Mercosul
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0D47A1))
                                .padding(vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "BRASIL",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp
                            )
                        }
                        // Dígitos da Placa
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = placa.uppercase(),
                                color = Color.Black,
                                style = FireTypography.Title,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                    }
                }
            }

            // Scanner CRLV (OCR) Button inside Modal
            Card(
                colors = CardDefaults.cardColors(containerColor = FireColors.Primary.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, FireColors.Primary.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onScanCrlvClick() }
                        .padding(FireSpacing.Medium),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = FireIcons.PhotoCamera,
                        contentDescription = null,
                        tint = FireColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(FireSpacing.Small))
                    Text(
                        text = "ESCANEAR CRLV VIA OCR (CÂMERA)",
                        style = FireTypography.LabelMedium,
                        fontWeight = FontWeight.Bold,
                        color = FireColors.Primary
                    )
                }
            }

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
                            "Dados do Veículo",
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
                                Text("Mercosul ou tradicional", style = FireTypography.LabelSmall)
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
                    
                    // Marca (FIPE / Catalog Offline)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = searchMarca,
                            onValueChange = { 
                                searchMarca = it
                                marca = it
                                expandedMarca = true
                                selectedFipeBrandId = null
                            },
                            label = { Text("Marca", style = FireTypography.BodyMedium) },
                            placeholder = { Text("Ex: Fiat, Chevrolet, Volkswagen", style = FireTypography.BodyMedium) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isLoadingBrands) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = FireColors.Primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    IconButton(onClick = { expandedMarca = !expandedMarca }) {
                                        Icon(
                                            imageVector = if (expandedMarca) FireIcons.ArrowDropUp else FireIcons.ArrowDropDown,
                                            contentDescription = null
                                        )
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FireColors.Primary,
                                unfocusedBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(
                            expanded = expandedMarca,
                            onDismissRequest = { expandedMarca = false },
                            properties = PopupProperties(focusable = false),
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            if (fipeBrands.isNotEmpty()) {
                                // Exibir marcas carregadas da API FIPE
                                filteredBrands.take(7).forEach { brandItem ->
                                    DropdownMenuItem(
                                        text = { Text(brandItem.nome) },
                                        onClick = {
                                            searchMarca = brandItem.nome
                                            marca = brandItem.nome
                                            selectedFipeBrandId = brandItem.codigo
                                            expandedMarca = false
                                            // Reset Modelo
                                            modelo = ""
                                            searchModelo = ""
                                            selectedFipeModelId = null
                                            fipeYears = emptyList()
                                        }
                                    )
                                }
                            } else {
                                // Fallback para catálogo local offline
                                filteredOfflineBrands.take(7).forEach { brandItem ->
                                    DropdownMenuItem(
                                        text = { Text(brandItem) },
                                        onClick = {
                                            searchMarca = brandItem
                                            marca = brandItem
                                            selectedFipeBrandId = null
                                            expandedMarca = false
                                            // Reset Modelo
                                            modelo = ""
                                            searchModelo = ""
                                            selectedFipeModelId = null
                                            fipeYears = emptyList()
                                        }
                                    )
                                }
                            }
                            if ((fipeBrands.isNotEmpty() && filteredBrands.isEmpty() && searchMarca.isNotBlank()) ||
                                (fipeBrands.isEmpty() && filteredOfflineBrands.isEmpty() && searchMarca.isNotBlank())) {
                                DropdownMenuItem(
                                    text = { Text("Usar \"$searchMarca\"") },
                                    onClick = { expandedMarca = false }
                                )
                            }
                        }
                    }
                    
                    // Modelo (FIPE / Catalog Offline)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = searchModelo,
                            onValueChange = { 
                                searchModelo = it
                                modelo = it
                                expandedModelo = true
                                selectedFipeModelId = null
                            },
                            label = { Text("Modelo", style = FireTypography.BodyMedium) },
                            placeholder = { Text("Ex: Onix, Palio, Corolla", style = FireTypography.BodyMedium) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isLoadingModels) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = FireColors.Primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    IconButton(onClick = { expandedModelo = !expandedModelo }) {
                                        Icon(
                                            imageVector = if (expandedModelo) FireIcons.ArrowDropUp else FireIcons.ArrowDropDown,
                                            contentDescription = null
                                        )
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FireColors.Primary,
                                unfocusedBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(
                            expanded = expandedModelo,
                            onDismissRequest = { expandedModelo = false },
                            properties = PopupProperties(focusable = false),
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            if (fipeModels.isNotEmpty()) {
                                // Exibir modelos carregados da API FIPE
                                filteredModels.take(8).forEach { modelItem ->
                                    DropdownMenuItem(
                                        text = { Text(modelItem.nome) },
                                        onClick = {
                                            searchModelo = modelItem.nome
                                            modelo = modelItem.nome
                                            selectedFipeModelId = modelItem.codigo
                                            expandedModelo = false
                                            fipeYears = emptyList()
                                        }
                                    )
                                }
                            } else {
                                // Fallback para catálogo local offline
                                filteredOfflineModels.take(8).forEach { modelItem ->
                                    DropdownMenuItem(
                                        text = { Text(modelItem) },
                                        onClick = {
                                            searchModelo = modelItem
                                            modelo = modelItem
                                            selectedFipeModelId = null
                                            expandedModelo = false
                                            fipeYears = emptyList()
                                        }
                                    )
                                }
                            }
                            if ((fipeModels.isNotEmpty() && filteredModels.isEmpty() && searchModelo.isNotBlank()) ||
                                (fipeModels.isEmpty() && filteredOfflineModels.isEmpty() && searchModelo.isNotBlank())) {
                                DropdownMenuItem(
                                    text = { Text("Usar \"$searchModelo\"") },
                                    onClick = { expandedModelo = false }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Color selection Section (Chips + Custom text input)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Selecione a Cor",
                            style = FireTypography.LabelSmall,
                            fontWeight = FontWeight.Bold,
                            color = FireColors.OnSurfaceVariant
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            items(commonColors) { (name, hex) ->
                                val isSelected = cor.equals(name, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(hex)))
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) FireColors.Primary else Color.LightGray,
                                            shape = CircleShape
                                        )
                                        .clickable { cor = name },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = if (name == "Branco" || name == "Amarelo") Color.Black else Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                        
                        OutlinedTextField(
                            value = cor,
                            onValueChange = { cor = it },
                            label = { Text("Cor Customizada", style = FireTypography.BodyMedium) },
                            placeholder = { Text("Branco, Preto, etc.", style = FireTypography.BodyMedium) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FireColors.Primary,
                                unfocusedBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
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
                    
                    // Ano FIPE (se carregado da API)
                    if (fipeYears.isNotEmpty() && !selectedFipeModelId.isNullOrBlank()) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val fipeSelectedYear = fipeYears.find { y ->
                                val yr = y.nome.filter { it.isDigit() }.take(4)
                                yr == anoFabricacao || yr == anoModelo
                            }?.nome ?: ""

                            OutlinedTextField(
                                value = fipeSelectedYear,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Ano e Combustível (FIPE)", style = FireTypography.BodyMedium) },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isLoadingYears) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = FireColors.Primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        IconButton(onClick = { expandedFipeYearDropdown = !expandedFipeYearDropdown }) {
                                            Icon(
                                                imageVector = if (expandedFipeYearDropdown) FireIcons.ArrowDropUp else FireIcons.ArrowDropDown,
                                                contentDescription = null
                                            )
                                        }
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = FireColors.Primary,
                                    unfocusedBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            DropdownMenu(
                                expanded = expandedFipeYearDropdown,
                                onDismissRequest = { expandedFipeYearDropdown = false },
                                modifier = Modifier.heightIn(max = 200.dp)
                            ) {
                                fipeYears.forEach { y ->
                                    DropdownMenuItem(
                                        text = { Text(y.nome) },
                                        onClick = {
                                            val digits = y.nome.filter { it.isDigit() }
                                            val yr = if (digits.length >= 4) digits.take(4) else digits
                                            if (yr.isNotBlank()) {
                                                anoFabricacao = yr
                                                anoModelo = yr
                                            }
                                            expandedFipeYearDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Ano Fabricação & Ano Modelo (Dropdowns de Fallback / Especificação manual)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                    ) {
                        // Ano Fabricação Select
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = anoFabricacao,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Ano Fab.", style = FireTypography.BodyMedium) },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    IconButton(onClick = { expandedAnoFabDropdown = !expandedAnoFabDropdown }) {
                                        Icon(
                                            imageVector = if (expandedAnoFabDropdown) FireIcons.ArrowDropUp else FireIcons.ArrowDropDown,
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
                                expanded = expandedAnoFabDropdown,
                                onDismissRequest = { expandedAnoFabDropdown = false },
                                modifier = Modifier.heightIn(max = 200.dp)
                            ) {
                                yearsRange.forEach { y ->
                                    DropdownMenuItem(
                                        text = { Text(y) },
                                        onClick = {
                                            anoFabricacao = y
                                            expandedAnoFabDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        // Ano Modelo Select
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = anoModelo,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Ano Mod.", style = FireTypography.BodyMedium) },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    IconButton(onClick = { expandedAnoModDropdown = !expandedAnoModDropdown }) {
                                        Icon(
                                            imageVector = if (expandedAnoModDropdown) FireIcons.ArrowDropUp else FireIcons.ArrowDropDown,
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
                                expanded = expandedAnoModDropdown,
                                onDismissRequest = { expandedAnoModDropdown = false },
                                modifier = Modifier.heightIn(max = 200.dp)
                            ) {
                                yearsRange.forEach { y ->
                                    DropdownMenuItem(
                                        text = { Text(y) },
                                        onClick = {
                                            anoModelo = y
                                            expandedAnoModDropdown = false
                                        }
                                    )
                                }
                            }
                        }
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
                            DropdownMenuItem(
                                text = { Text("Nenhum (Sem proprietário vinculado)", color = Color.Gray) },
                                onClick = {
                                    selectedPessoaId = null
                                    expandedPessoaDropdown = false
                                }
                            )
                            if (pessoasDisponiveis.isNotEmpty()) {
                                Divider()
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
            if (currentCrlvImageUri != null) {
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
                            imageVector = FireIcons.Check,
                            contentDescription = null,
                            tint = FireColors.Success
                        )
                        Column {
                            Text(
                                "✅ CRLV Anexado",
                                style = FireTypography.BodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = FireColors.Success
                            )
                            Text(
                                "Imagem de documento vinculada",
                                style = FireTypography.LabelSmall,
                                color = FireColors.Success
                            )
                        }
                    }
                }
            }
        }
    }
}
