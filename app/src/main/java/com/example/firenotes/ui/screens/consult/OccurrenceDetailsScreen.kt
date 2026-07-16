package com.example.firenotes.ui.screens.consult

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.absoluteValue
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.firenotes.domain.model.Ocorrencia
import com.example.firenotes.domain.model.Viatura
import com.example.firenotes.ui.designsystem.colors.FireColors
import com.example.firenotes.ui.designsystem.components.buttons.FireButton
import com.example.firenotes.ui.designsystem.components.buttons.FireIconButton
import com.example.firenotes.ui.designsystem.components.topbar.FireTopBar
import com.example.firenotes.ui.designsystem.components.widgets.FireStatusChip
import com.example.firenotes.ui.designsystem.icons.FireIcons
import com.example.firenotes.ui.designsystem.spacing.FireSpacing
import com.example.firenotes.ui.designsystem.typography.FireTypography
import com.example.firenotes.ui.screens.reports.ReportsViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import java.io.File
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OccurrenceDetailsScreen(
    viewModel: OccurrenceDetailsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val reportsViewModel: ReportsViewModel = hiltViewModel()

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var editingImagePath by remember { mutableStateOf<String?>(null) }

    editingImagePath?.let { path ->
        ImageEditorDialog(
            imagePath = path,
            onDismiss = { editingImagePath = null },
            onSave = { editedBitmap, saveAsNew ->
                try {
                    if (saveAsNew) {
                        val originalFile = File(path)
                        val folder = originalFile.parentFile ?: context.cacheDir
                        val newFile = File(folder, "foto_${System.currentTimeMillis()}.jpg")
                        java.io.FileOutputStream(newFile).use { out ->
                            editedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                        }
                        uiState.occurrence?.let { occurrence ->
                            val updatedFotos = occurrence.fotos + newFile.absolutePath
                            viewModel.updateOccurrencePhotos(occurrence.copy(fotos = updatedFotos))
                        }
                    } else {
                        val file = File(path)
                        java.io.FileOutputStream(file).use { out ->
                            editedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                        }
                        viewModel.loadOccurrence()
                    }
                    Toast.makeText(context, "Imagem salva com sucesso!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Erro ao salvar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
                editingImagePath = null
            }
        )
    }

    val formatter = remember {
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault())
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(text = "Excluir Ocorrência", fontWeight = FontWeight.Bold) },
            text = { Text(text = "Deseja realmente excluir permanentemente esta ocorrência? Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteOccurrence {
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = FireColors.Error)
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            uiState.occurrence?.let { occurrence ->
                val prontidao = remember(occurrence.dataHora) {
                    com.example.firenotes.data.service.ProntidaoService.getProntidaoForInstant(occurrence.dataHora)
                }
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Talão nº: ${occurrence.protocolo}",
                                    style = FireTypography.Title,
                                    fontWeight = FontWeight.Bold,
                                    color = FireColors.OnBackground
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                val isAberto = occurrence.status == "ABERTA"
                                FireStatusChip(
                                    text = if (isAberto) "EM ABERTO" else "ENCERRADO",
                                    backgroundColor = if (isAberto) FireColors.Primary.copy(alpha = 0.12f) else FireColors.Success.copy(alpha = 0.12f),
                                    textColor = if (isAberto) FireColors.Primary else FireColors.Success
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = formatter.format(occurrence.dataHora),
                                    style = FireTypography.Label,
                                    color = FireColors.OnSurfaceVariant
                                )
                                Text(
                                    text = " | ",
                                    style = FireTypography.Label,
                                    color = FireColors.OnSurfaceVariant
                                )
                                Text(
                                    text = prontidao.nome,
                                    style = FireTypography.Label,
                                    fontWeight = FontWeight.Bold,
                                    color = prontidao.cor
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = FireIcons.ArrowBack,
                                contentDescription = "Voltar",
                                tint = FireColors.OnSurface
                            )
                        }
                    },
                    actions = {
                        // Botão Editar (apenas ícone)
                        IconButton(onClick = { onNavigateToEdit(occurrence.id!!) }) {
                            Icon(
                                imageVector = FireIcons.Edit,
                                contentDescription = "Editar ocorrência",
                                tint = FireColors.OnSurface
                            )
                        }

                        var menuExpanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = FireIcons.MoreVert,
                                contentDescription = "Mais opções",
                                tint = FireColors.OnSurface
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(FireColors.Surface)
                        ) {
                            val isAberto = occurrence.status == "ABERTA"
                            DropdownMenuItem(
                                text = { Text(if (isAberto) "Encerrar Ocorrência" else "Reabrir Ocorrência", style = FireTypography.Body) },
                                leadingIcon = { Icon(if (isAberto) FireIcons.Check else FireIcons.Refresh, contentDescription = null, tint = if (isAberto) FireColors.Success else FireColors.Primary) },
                                onClick = {
                                    menuExpanded = false
                                    val newStatus = if (isAberto) "ENCERRADA" else "ABERTA"
                                    viewModel.updateOccurrenceStatus(newStatus)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Visualizar PDF", style = FireTypography.Body) },
                                leadingIcon = { Icon(FireIcons.PictureAsPdf, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    reportsViewModel.exportOccurrencePdf(occurrence) { uri ->
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "application/pdf")
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Compartilhar PDF (Apenas Relatório)", style = FireTypography.Body) },
                                leadingIcon = { Icon(FireIcons.Share, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    reportsViewModel.exportOccurrencePdf(occurrence) { uri ->
                                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "application/pdf"
                                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(intent, "Compartilhar Relatório"))
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Compartilhar com Fotos", style = FireTypography.Body) },
                                leadingIcon = { Icon(FireIcons.Share, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    reportsViewModel.exportOccurrencePdf(occurrence) { pdfUri ->
                                        val uris = ArrayList<android.net.Uri>()
                                        uris.add(pdfUri)

                                        occurrence.fotos.forEach { path ->
                                            val file = File(path)
                                            if (file.exists()) {
                                                val imgUri = androidx.core.content.FileProvider.getUriForFile(context, "com.example.firenotes.fileprovider", file)
                                                uris.add(imgUri)
                                            }
                                        }

                                        uiState.evidencias
                                            .filter { it.tipo.contains("Imagem", ignoreCase = true) || it.tipo.contains("Croqui", ignoreCase = true) || it.urlStorage.endsWith(".jpg") || it.urlStorage.endsWith(".png") || it.urlStorage.endsWith(".jpeg") }
                                            .map { it.urlStorage }
                                            .distinct()
                                            .forEach { path ->
                                                val file = File(path)
                                                if (file.exists()) {
                                                    val imgUri = androidx.core.content.FileProvider.getUriForFile(context, "com.example.firenotes.fileprovider", file)
                                                    uris.add(imgUri)
                                                }
                                            }

                                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND_MULTIPLE).apply {
                                            type = "*/*"
                                            putParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, uris)
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(intent, "Compartilhar Ocorrência e Imagens"))
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Compartilhar JSON (Com outro Usuário)", style = FireTypography.Body) },
                                leadingIcon = { Icon(FireIcons.FileOpen, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.exportOccurrenceJson(context) { uri ->
                                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "application/json"
                                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(intent, "Enviar Ocorrência"))
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Excluir", style = FireTypography.Body, color = FireColors.Error) },
                                leadingIcon = { Icon(FireIcons.Delete, contentDescription = null, tint = FireColors.Error) },
                                onClick = {
                                    menuExpanded = false
                                    showDeleteConfirmDialog = true
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = FireColors.Surface,
                        titleContentColor = FireColors.OnSurface
                    ),
                    modifier = Modifier.shadow(2.dp)
                )
            }
        },
        bottomBar = {},
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(FireColors.Background),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(color = FireColors.Primary)
                }
                uiState.errorMessage != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "Erro desconhecido",
                            style = FireTypography.Body,
                            color = FireColors.Error
                        )
                        Button(onClick = { viewModel.loadOccurrence() }) {
                            Text("Tentar Novamente")
                        }
                    }
                }
                uiState.occurrence != null -> {
                    val occurrence = uiState.occurrence!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Detalhes Gerais
                        SectionCard(
                            title = "Informações Gerais",
                            action = {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            val geoUri = if (occurrence.latitude != null && occurrence.longitude != null) {
                                                "geo:${occurrence.latitude},${occurrence.longitude}?q=${occurrence.latitude},${occurrence.longitude}(Local+da+Ocorrencia)"
                                            } else {
                                                val addressQuery = "${occurrence.rua ?: ""}, ${occurrence.numero ?: ""}, ${occurrence.bairro ?: ""}, ${occurrence.cidade ?: ""}"
                                                "geo:0,0?q=${android.net.Uri.encode(addressQuery)}"
                                            }
                                            val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(geoUri)).apply {
                                                setPackage("com.google.android.apps.maps")
                                            }
                                            try {
                                                context.startActivity(mapIntent)
                                            } catch (e: Exception) {
                                                val addressQuery = "${occurrence.rua ?: ""}, ${occurrence.numero ?: ""}, ${occurrence.bairro ?: ""}, ${occurrence.cidade ?: ""}"
                                                val webIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com/maps/search/?api=1&query=${android.net.Uri.encode(addressQuery)}"))
                                                context.startActivity(webIntent)
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = FireIcons.Map,
                                            contentDescription = "Abrir no Google Maps",
                                            tint = FireColors.Primary
                                        )
                                    }
                                    Text(
                                        text = "Maps",
                                        style = FireTypography.Label,
                                        fontSize = 10.sp,
                                        color = FireColors.Primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        ) {
                            InfoRow(
                                icon = "🏷️",
                                label = "Protocolo",
                                textValue = occurrence.protocolo
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            InfoRow(
                                icon = "🏷️",
                                label = "Natureza",
                                textValue = occurrence.natureza.descricao
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            InfoRow(
                                icon = "📍",
                                label = "Endereço",
                                textValue = "${occurrence.rua ?: ""}, ${occurrence.numero ?: ""} - ${occurrence.bairro ?: ""}, ${occurrence.cidade ?: ""}/${occurrence.uf ?: ""}"
                            )
                            occurrence.latitude?.let { lat ->
                                Spacer(modifier = Modifier.height(8.dp))
                                InfoRow(
                                    icon = "🗺️",
                                    label = "Coordenadas",
                                    textValue = "Lat ${"%.5f".format(lat)} | Lng ${"%.5f".format(occurrence.longitude)}"
                                )
                            }
                        }

                        // Guarnição e Viaturas
                        if (occurrence.viaturas.isNotEmpty()) {
                            SectionCard(title = "🚒 Guarnição e Viaturas") {
                                occurrence.viaturas.forEachIndexed { index, viatura ->
                                    if (index > 0) Spacer(modifier = Modifier.height(12.dp))
                                    ViaturaDetailCard(viatura = viatura)
                                }
                            }
                        }

                        // Veículos
                        if (occurrence.veiculos.isNotEmpty()) {
                            SectionCard(title = "🚗 Veículos Envolvidos") {
                                occurrence.veiculos.forEachIndexed { index, veiculo ->
                                    if (index > 0) Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "• Placa: ${veiculo.placa.ifBlank { "SEM PLACA" }} | Modelo/Marca: ${veiculo.modelo.ifBlank { "N/I" }}",
                                        style = FireTypography.Body,
                                        color = FireColors.OnBackground
                                    )
                                }
                            }
                        }

                        // Vítimas
                        if (occurrence.vitimas.isNotEmpty()) {
                            SectionCard(title = "👤 Vítimas e Socorro") {
                                occurrence.vitimas.forEachIndexed { index, vitima ->
                                    val pCadastro = remember(uiState.pessoas, vitima) {
                                        uiState.pessoas.find { p ->
                                            p.nome.trim().equals(vitima.nome.trim(), ignoreCase = true) ||
                                            (p.cpf != null && p.cpf == vitima.cpf)
                                        }
                                    }
                                    if (index > 0) Divider(modifier = Modifier.padding(vertical = 12.dp))
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = FireColors.SurfaceVariant.copy(alpha = 0.3f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = vitima.nome.ifBlank { "Não Identificado" },
                                                style = FireTypography.Body,
                                                fontWeight = FontWeight.Bold,
                                                color = FireColors.OnBackground
                                            )
                                            if (pCadastro != null) {
                                                Text(
                                                    text = "CPF: ${pCadastro.cpf ?: "Não informado"} | Sexo: ${pCadastro.sexo ?: "Não informado"}",
                                                    fontSize = 12.sp,
                                                    color = FireColors.OnSurfaceVariant
                                                )
                                                Text(
                                                    text = "Nascimento: ${pCadastro.nascimento ?: "Não informado"} | Idade: ${vitima.idade ?: "Não informado"}",
                                                    fontSize = 12.sp,
                                                    color = FireColors.OnSurfaceVariant
                                                )
                                            } else {
                                                Text(
                                                    text = "CPF: ${vitima.cpf ?: "Não informado"} | Idade: ${vitima.idade ?: "Não informado"}",
                                                    fontSize = 12.sp,
                                                    color = FireColors.OnSurfaceVariant
                                                )
                                            }
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = "GCS: ${vitima.sinaisVitais.escalaGCS ?: "N/D"}",
                                                    fontSize = 12.sp,
                                                    color = FireColors.OnSurfaceVariant
                                                )
                                                Text(
                                                    text = "PA: ${vitima.sinaisVitais.pressaoArterial.ifBlank { "N/D" }}",
                                                    fontSize = 12.sp,
                                                    color = FireColors.OnSurfaceVariant
                                                )
                                                Text(
                                                    text = "FC: ${vitima.sinaisVitais.pulso ?: "N/D"} bpm",
                                                    fontSize = 12.sp,
                                                    color = FireColors.OnSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = "🏥 Destino: ${vitima.hospitalDestino.ifBlank { "Não encaminhado" }}",
                                                fontSize = 12.sp,
                                                color = FireColors.OnSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Pessoas Envolvidas Adicionais
                        val envolvidos = remember(uiState.pessoas, occurrence.vitimas) {
                            val vitimasCpfs = occurrence.vitimas.mapNotNull { it.cpf }.filter { it.isNotBlank() }.toSet()
                            val vitimasNomes = occurrence.vitimas.map { it.nome.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
                            uiState.pessoas.filter { p ->
                                val matchesCpf = p.cpf?.let { it.isNotBlank() && vitimasCpfs.contains(it) } == true
                                val matchesNome = vitimasNomes.contains(p.nome.trim().lowercase())
                                !matchesCpf && !matchesNome
                            }
                        }
                        if (envolvidos.isNotEmpty()) {
                            SectionCard(title = "👥 Pessoas Envolvidas") {
                                envolvidos.forEachIndexed { index, pessoa ->
                                    if (index > 0) Divider(modifier = Modifier.padding(vertical = 12.dp))
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = FireColors.SurfaceVariant.copy(alpha = 0.3f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = pessoa.nome,
                                                style = FireTypography.Body,
                                                fontWeight = FontWeight.Bold,
                                                color = FireColors.OnBackground
                                            )
                                            Text(
                                                text = "CPF: ${pessoa.cpf ?: "Não informado"} | Telefone: ${pessoa.telefone ?: "Não informado"}",
                                                fontSize = 12.sp,
                                                color = FireColors.OnSurfaceVariant
                                            )
                                            Text(
                                                text = "Sexo: ${pessoa.sexo ?: "Não informado"} | Nascimento: ${pessoa.nascimento ?: "Não informado"}",
                                                fontSize = 12.sp,
                                                color = FireColors.OnSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Órgãos de Apoio
                        val apoios = occurrence.apoiosDetalhados
                        if (apoios.isNotEmpty()) {
                            SectionCard(title = "🤝 Apoios Acionados") {
                                apoios.forEachIndexed { index, apoio ->
                                    if (index > 0) Divider(modifier = Modifier.padding(vertical = 12.dp))
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = FireColors.SurfaceVariant.copy(alpha = 0.3f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "${apoio.orgaoSigla} - ${apoio.orgaoNome}",
                                                style = FireTypography.Body,
                                                fontWeight = FontWeight.Bold,
                                                color = FireColors.OnBackground
                                            )
                                            Text(
                                                text = "Viatura: ${apoio.viatura.ifBlank { "N/I" }}",
                                                fontSize = 12.sp,
                                                color = FireColors.OnSurfaceVariant
                                            )
                                            if (apoio.encarregado.isNotBlank() || apoio.descricaoOutros.isNotBlank()) {
                                                Text(
                                                    text = "Informações: ${if (apoio.encarregado.isNotBlank()) apoio.encarregado else apoio.descricaoOutros}",
                                                    fontSize = 12.sp,
                                                    color = FireColors.OnSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Histórico
                        occurrence.historico?.takeIf { it.isNotBlank() }?.let { hist ->
                            SectionCard(title = "📝 Histórico Narrativo") {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = FireColors.SurfaceVariant.copy(alpha = 0.15f)
                                    )
                                ) {
                                    Text(
                                        text = hist,
                                        style = FireTypography.Body,
                                        color = FireColors.OnBackground,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }

                        // Miniaturas de Imagens e Evidências (Carrossel MD3)
                        val evidenciasImagens = remember(uiState.evidencias) {
                            uiState.evidencias
                                .filter { it.tipo.contains("Imagem", ignoreCase = true) || it.tipo.contains("Croqui", ignoreCase = true) || it.urlStorage.endsWith(".jpg") || it.urlStorage.endsWith(".png") || it.urlStorage.endsWith(".jpeg") }
                               .map { it.urlStorage }
                        }
                        val todasImagens = remember(occurrence.fotos, evidenciasImagens) {
                            (occurrence.fotos + evidenciasImagens).distinct()
                        }
                        if (todasImagens.isNotEmpty()) {
                            SectionCard(title = "📸 Imagens Anexadas e Evidências") {
                                val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { todasImagens.size })
                                androidx.compose.foundation.pager.HorizontalPager(
                                    state = pagerState,
                                    contentPadding = PaddingValues(horizontal = 48.dp),
                                    pageSpacing = 16.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                ) { page ->
                                    val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                                    val scale = 0.85f + (1f - 0.85f) * (1f - pageOffset.coerceIn(0f, 1f))
                                    val alpha = 0.5f + (1f - 0.5f) * (1f - pageOffset.coerceIn(0f, 1f))

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp)
                                            .graphicsLayer {
                                                scaleX = scale
                                                scaleY = scale
                                                this.alpha = alpha
                                            }
                                            .clickable { editingImagePath = todasImagens[page] },
                                        shape = RoundedCornerShape(16.dp),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        AsyncImage(
                                            model = File(todasImagens[page]),
                                            contentDescription = "Imagem da ocorrência/evidência",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }

                        // Outros Anexos e Evidências (Áudios, PDFs, etc.)
                        val outrasEvidencias = remember(uiState.evidencias) {
                            uiState.evidencias.filter {
                                !it.tipo.contains("Imagem", ignoreCase = true) &&
                                !it.tipo.contains("Croqui", ignoreCase = true) &&
                                !it.urlStorage.endsWith(".jpg") &&
                                !it.urlStorage.endsWith(".png") &&
                                !it.urlStorage.endsWith(".jpeg")
                            }
                        }
                        if (outrasEvidencias.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            SectionCard(title = "📁 Outras Evidências e Anexos") {
                                outrasEvidencias.forEachIndexed { index, evidencia ->
                                    if (index > 0) Spacer(modifier = Modifier.height(8.dp))
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = FireColors.SurfaceVariant.copy(alpha = 0.3f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            val icon = if (evidencia.tipo.contains("Áudio", ignoreCase = true)) {
                                                FireIcons.Mic
                                            } else {
                                                FireIcons.Description
                                            }
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = evidencia.tipo,
                                                tint = FireColors.Primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Tipo: ${evidencia.tipo}",
                                                    style = FireTypography.Body,
                                                    fontWeight = FontWeight.Bold,
                                                    color = FireColors.OnBackground
                                                )
                                                if (!evidencia.ocrBruto.isNullOrBlank()) {
                                                    Text(
                                                        text = evidencia.ocrBruto,
                                                        fontSize = 12.sp,
                                                        color = FireColors.OnSurfaceVariant
                                                    )
                                                }
                                                Text(
                                                    text = "Arquivo: ${evidencia.urlStorage.substringAfterLast("/")}",
                                                    fontSize = 11.sp,
                                                    color = FireColors.OnSurfaceVariant.copy(alpha = 0.8f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    action: @Composable (RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = FireColors.Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = FireTypography.Title,
                    fontWeight = FontWeight.Bold,
                    color = FireColors.Primary
                )
                action?.invoke(this)
            }
            Spacer(modifier = Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(
    icon: String,
    label: String,
    textValue: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$icon $label:",
            style = FireTypography.Body,
            fontWeight = FontWeight.Medium,
            color = FireColors.OnSurfaceVariant,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = textValue,
            style = FireTypography.Body,
            color = FireColors.OnBackground,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ViaturaDetailCard(viatura: Viatura) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = FireColors.SurfaceVariant.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "${viatura.prefixo} (${viatura.tipo})",
                style = FireTypography.Body,
                fontWeight = FontWeight.Medium,
                color = FireColors.OnBackground
            )
            viatura.equipe.forEach { m ->
                Text(
                    text = "• RE ${m.re} - ${m.graduacao} ${m.nomeGuerra} [${m.funcao.takeIf { it.isNotEmpty() } ?: "Equipe"}]",
                    style = FireTypography.Body,
                    color = FireColors.OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ImageEditorDialog(
    imagePath: String,
    onDismiss: () -> Unit,
    onSave: (Bitmap, Boolean) -> Unit
) {
    var editedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    // Classes de auxílio para desenho fluido
    class DrawingPath(val points: List<Offset>, val color: Color = Color.Red)
    class TextDrawing(val text: String, val position: Offset, val color: Color = Color.Yellow)

    val allPaths = remember { mutableStateListOf<DrawingPath>() }
    val allTexts = remember { mutableStateListOf<TextDrawing>() }
    val currentPath = remember { mutableStateListOf<Offset>() }

    var isDrawingMode by remember { mutableStateOf(false) }
    var isTextMode by remember { mutableStateOf(false) }
    var textToDraw by remember { mutableStateOf("") }
    var showTextInput by remember { mutableStateOf(false) }
    var triggerUpdate by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    
    // WhatsApp Style Crop States
    var isCroppingMode by remember { mutableStateOf(false) }
    var cropLeft by remember { mutableStateOf(0f) }
    var cropTop by remember { mutableStateOf(0f) }
    var cropRight by remember { mutableStateOf(0f) }
    var cropBottom by remember { mutableStateOf(0f) }
    var cropInitialized by remember { mutableStateOf(false) }
    var activeHandle by remember { mutableStateOf<String?>(null) } // "TL", "TR", "BL", "BR", "MOVE" ou null

    var canvasSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize(0, 0)) }
    val context = LocalContext.current
    
    LaunchedEffect(imagePath) {
        try {
            val file = File(imagePath)
            if (file.exists()) {
                val original = BitmapFactory.decodeFile(file.absolutePath)
                val mutable = original.copy(Bitmap.Config.ARGB_8888, true)
                editedBitmap = mutable
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao carregar imagem: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Mescla todas as edições feitas na tela no Bitmap final com pixels reais ao salvar
    val finalBitmapToSave = remember(editedBitmap, allPaths, allTexts, canvasSize) {
        editedBitmap?.let { bmp ->
            if (canvasSize.width > 0 && canvasSize.height > 0) {
                val mutable = bmp.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = android.graphics.Canvas(mutable)
                val scaleX = mutable.width.toFloat() / canvasSize.width
                val scaleY = mutable.height.toFloat() / canvasSize.height
                
                // 1. Desenhar todos os caminhos
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.RED
                    strokeWidth = 12f
                    style = android.graphics.Paint.Style.STROKE
                    strokeJoin = android.graphics.Paint.Join.ROUND
                    strokeCap = android.graphics.Paint.Cap.ROUND
                    isAntiAlias = true
                }
                allPaths.forEach { path ->
                    if (path.points.size > 1) {
                        val gPath = android.graphics.Path()
                        val first = path.points.first()
                        gPath.moveTo(first.x * scaleX, first.y * scaleY)
                        for (i in 1 until path.points.size) {
                            val pt = path.points[i]
                            gPath.lineTo(pt.x * scaleX, pt.y * scaleY)
                        }
                        canvas.drawPath(gPath, paint)
                    }
                }
                
                // 2. Desenhar todos os textos
                val textPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.YELLOW
                    textSize = 45f * scaleX
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                }
                allTexts.forEach { textDrawing ->
                    canvas.drawText(
                        textDrawing.text,
                        textDrawing.position.x * scaleX,
                        textDrawing.position.y * scaleY,
                        textPaint
                    )
                }
                mutable
            } else {
                bmp
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color.Black.copy(alpha = 0.8f))
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = FireIcons.Close, contentDescription = "Cancelar", tint = Color.White)
                    }
                    Text(
                        text = if (isCroppingMode) "Recortar Imagem" else "Edição de Evidência",
                        style = FireTypography.Title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { showSaveDialog = true },
                        enabled = editedBitmap != null
                    ) {
                        Icon(imageVector = FireIcons.Check, contentDescription = "Salvar", tint = FireColors.Primary)
                    }
                }
                
                // Image Canvas Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    editedBitmap?.let { bmp ->
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .aspectRatio(bmp.width.toFloat() / bmp.height.toFloat())
                                .background(Color.DarkGray)
                                .pointerInput(isDrawingMode, isCroppingMode) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            if (isDrawingMode) {
                                                currentPath.clear()
                                                currentPath.add(offset)
                                            } else if (isCroppingMode) {
                                                val radius = 55f
                                                activeHandle = when {
                                                    (offset - Offset(cropLeft, cropTop)).getDistance() < radius -> "TL"
                                                    (offset - Offset(cropRight, cropTop)).getDistance() < radius -> "TR"
                                                    (offset - Offset(cropLeft, cropBottom)).getDistance() < radius -> "BL"
                                                    (offset - Offset(cropRight, cropBottom)).getDistance() < radius -> "BR"
                                                    offset.x > cropLeft && offset.x < cropRight && offset.y > cropTop && offset.y < cropBottom -> "MOVE"
                                                    else -> null
                                                }
                                            }
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            if (isDrawingMode) {
                                                currentPath.add(change.position)
                                                triggerUpdate = !triggerUpdate
                                            } else if (isCroppingMode && activeHandle != null) {
                                                val delta = dragAmount
                                                val w = size.width.toFloat()
                                                val h = size.height.toFloat()
                                                when (activeHandle) {
                                                    "TL" -> {
                                                        cropLeft = (cropLeft + delta.x).coerceIn(0f, cropRight - 100f)
                                                        cropTop = (cropTop + delta.y).coerceIn(0f, cropBottom - 100f)
                                                    }
                                                    "TR" -> {
                                                        cropRight = (cropRight + delta.x).coerceIn(cropLeft + 100f, w)
                                                        cropTop = (cropTop + delta.y).coerceIn(0f, cropBottom - 100f)
                                                    }
                                                    "BL" -> {
                                                        cropLeft = (cropLeft + delta.x).coerceIn(0f, cropRight - 100f)
                                                        cropBottom = (cropBottom + delta.y).coerceIn(cropTop + 100f, h)
                                                    }
                                                    "BR" -> {
                                                        cropRight = (cropRight + delta.x).coerceIn(cropLeft + 100f, w)
                                                        cropBottom = (cropBottom + delta.y).coerceIn(cropTop + 100f, h)
                                                    }
                                                    "MOVE" -> {
                                                        val rectW = cropRight - cropLeft
                                                        val rectH = cropBottom - cropTop
                                                        val newLeft = (cropLeft + delta.x).coerceIn(0f, w - rectW)
                                                        val newTop = (cropTop + delta.y).coerceIn(0f, h - rectH)
                                                        cropLeft = newLeft
                                                        cropRight = newLeft + rectW
                                                        cropTop = newTop
                                                        cropBottom = newTop + rectH
                                                    }
                                                }
                                                triggerUpdate = !triggerUpdate
                                            }
                                        },
                                        onDragEnd = {
                                            if (isDrawingMode && currentPath.isNotEmpty()) {
                                                allPaths.add(DrawingPath(currentPath.toList()))
                                                currentPath.clear()
                                                triggerUpdate = !triggerUpdate
                                            }
                                            activeHandle = null
                                        }
                                    )
                                }
                                .pointerInput(isTextMode, textToDraw) {
                                    if (isTextMode && textToDraw.isNotEmpty()) {
                                        detectTapGestures { offset ->
                                            allTexts.add(TextDrawing(textToDraw, offset))
                                            isTextMode = false
                                            textToDraw = ""
                                            triggerUpdate = !triggerUpdate
                                        }
                                    }
                                }
                        ) {
                            canvasSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())
                            
                            if (!cropInitialized && size.width > 0f) {
                                cropLeft = size.width * 0.1f
                                cropTop = size.height * 0.1f
                                cropRight = size.width * 0.9f
                                cropBottom = size.height * 0.9f
                                cropInitialized = true
                            }
                            
                            // 1. Desenha o bitmap base
                            drawContext.canvas.nativeCanvas.drawBitmap(
                                bmp,
                                null,
                                android.graphics.RectF(0f, 0f, size.width, size.height),
                                null
                            )
                            
                            // 2. Desenha todos os caminhos concluídos
                            allPaths.forEach { path ->
                                if (path.points.size > 1) {
                                    for (i in 0 until path.points.size - 1) {
                                        drawLine(
                                            color = path.color,
                                            start = path.points[i],
                                            end = path.points[i+1],
                                            strokeWidth = 10f
                                        )
                                    }
                                }
                            }
                            
                            // 3. Desenha o caminho temporário atual
                            if (isDrawingMode && currentPath.size > 1) {
                                val strokeColor = Color.Red
                                for (i in 0 until currentPath.size - 1) {
                                    drawLine(
                                        color = strokeColor,
                                        start = currentPath[i],
                                        end = currentPath[i+1],
                                        strokeWidth = 10f
                                    )
                                }
                            }
                            
                            // 4. Desenha todos os textos inseridos
                            allTexts.forEach { textDrawing ->
                                drawContext.canvas.nativeCanvas.drawText(
                                    textDrawing.text,
                                    textDrawing.position.x,
                                    textDrawing.position.y,
                                    android.graphics.Paint().apply {
                                        color = android.graphics.Color.YELLOW
                                        textSize = 45f
                                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                                    }
                                )
                            }

                            // 5. WhatsApp Crop Overlay
                            if (isCroppingMode) {
                                // Máscara escura
                                drawRect(color = Color.Black.copy(alpha = 0.7f), topLeft = Offset(0f, 0f), size = Size(size.width, cropTop))
                                drawRect(color = Color.Black.copy(alpha = 0.7f), topLeft = Offset(0f, cropBottom), size = Size(size.width, size.height - cropBottom))
                                drawRect(color = Color.Black.copy(alpha = 0.7f), topLeft = Offset(0f, cropTop), size = Size(cropLeft, cropBottom - cropTop))
                                drawRect(color = Color.Black.copy(alpha = 0.7f), topLeft = Offset(cropRight, cropTop), size = Size(size.width - cropRight, cropBottom - cropTop))

                                // Borda
                                drawRect(
                                    color = Color.White,
                                    topLeft = Offset(cropLeft, cropTop),
                                    size = Size(cropRight - cropLeft, cropBottom - cropTop),
                                    style = Stroke(width = 3f)
                                )

                                // Alças do WhatsApp nos cantos
                                val hLen = 40f
                                val hThick = 10f
                                // TL
                                drawLine(color = Color.White, start = Offset(cropLeft, cropTop), end = Offset(cropLeft + hLen, cropTop), strokeWidth = hThick)
                                drawLine(color = Color.White, start = Offset(cropLeft, cropTop), end = Offset(cropLeft, cropTop + hLen), strokeWidth = hThick)
                                // TR
                                drawLine(color = Color.White, start = Offset(cropRight, cropTop), end = Offset(cropRight - hLen, cropTop), strokeWidth = hThick)
                                drawLine(color = Color.White, start = Offset(cropRight, cropTop), end = Offset(cropRight, cropTop + hLen), strokeWidth = hThick)
                                // BL
                                drawLine(color = Color.White, start = Offset(cropLeft, cropBottom), end = Offset(cropLeft + hLen, cropBottom), strokeWidth = hThick)
                                drawLine(color = Color.White, start = Offset(cropLeft, cropBottom), end = Offset(cropLeft, cropBottom - hLen), strokeWidth = hThick)
                                // BR
                                drawLine(color = Color.White, start = Offset(cropRight, cropBottom), end = Offset(cropRight - hLen, cropBottom), strokeWidth = hThick)
                                drawLine(color = Color.White, start = Offset(cropRight, cropBottom), end = Offset(cropRight, cropBottom - hLen), strokeWidth = hThick)
                            }
                        }
                    }
                }
                
                // Bottom Tools Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.8f))
                        .navigationBarsPadding()
                        .padding(bottom = 96.dp, top = 12.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isCroppingMode) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    editedBitmap?.let { bmp ->
                                        if (canvasSize.width > 0 && canvasSize.height > 0) {
                                            val scaleX = bmp.width.toFloat() / canvasSize.width
                                            val scaleY = bmp.height.toFloat() / canvasSize.height
                                            val x = (cropLeft * scaleX).toInt().coerceIn(0, bmp.width - 1)
                                            val y = (cropTop * scaleY).toInt().coerceIn(0, bmp.height - 1)
                                            val width = ((cropRight - cropLeft) * scaleX).toInt().coerceIn(1, bmp.width - x)
                                            val height = ((cropBottom - cropTop) * scaleY).toInt().coerceIn(1, bmp.height - y)
                                            val cropped = Bitmap.createBitmap(bmp, x, y, width, height)
                                            
                                            editedBitmap = cropped
                                            allPaths.clear()
                                            allTexts.clear()
                                            isCroppingMode = false
                                            cropInitialized = false
                                        }
                                    }
                                }
                                .padding(8.dp)
                        ) {
                            Icon(imageVector = FireIcons.Check, contentDescription = "Confirmar", tint = Color.Green, modifier = Modifier.size(28.dp))
                            Text("Confirmar", fontSize = 11.sp, color = Color.White)
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    isCroppingMode = false
                                }
                                .padding(8.dp)
                        ) {
                            Icon(imageVector = FireIcons.Close, contentDescription = "Cancelar", tint = Color.Red, modifier = Modifier.size(28.dp))
                            Text("Cancelar", fontSize = 11.sp, color = Color.White)
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    isDrawingMode = !isDrawingMode
                                    isTextMode = false
                                }
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = FireIcons.Edit,
                                contentDescription = "Desenhar",
                                tint = if (isDrawingMode) FireColors.Primary else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                "Desenhar",
                                fontSize = 11.sp,
                                color = if (isDrawingMode) FireColors.Primary else Color.White
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    showTextInput = true
                                    isDrawingMode = false
                                }
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = FireIcons.Description,
                                contentDescription = "Texto",
                                tint = if (isTextMode) FireColors.Primary else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                "Texto",
                                fontSize = 11.sp,
                                color = if (isTextMode) FireColors.Primary else Color.White
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    isCroppingMode = true
                                    isDrawingMode = false
                                    isTextMode = false
                                    cropInitialized = false
                                }
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = FireIcons.FileDownload,
                                contentDescription = "Recortar",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Text("Recortar", fontSize = 11.sp, color = Color.White)
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    editedBitmap?.let { bmp ->
                                        val matrix = android.graphics.Matrix().apply { postRotate(90f) }
                                        val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                                        editedBitmap = rotated
                                        allPaths.clear()
                                        allTexts.clear()
                                        triggerUpdate = !triggerUpdate
                                    }
                                }
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = FireIcons.Refresh,
                                contentDescription = "Girar",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Text("Girar", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
            
            if (showTextInput) {
                var enteredText by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showTextInput = false },
                    title = { Text("Adicionar Texto") },
                    text = {
                        OutlinedTextField(
                            value = enteredText,
                            onValueChange = { enteredText = it },
                            placeholder = { Text("Digite o texto...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (enteredText.isNotBlank()) {
                                    textToDraw = enteredText
                                    isTextMode = true
                                    showTextInput = false
                                    Toast.makeText(context, "Toque na imagem onde deseja fixar o texto.", Toast.LENGTH_LONG).show()
                                }
                            }
                        ) {
                            Text("Confirmar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTextInput = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }
            
            if (showSaveDialog) {
                AlertDialog(
                    onDismissRequest = { showSaveDialog = false },
                    title = { Text("Salvar Imagem") },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Deseja substituir a imagem atual ou salvar as edições como uma nova foto?")
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    showSaveDialog = false
                                    val finalBmp = finalBitmapToSave ?: editedBitmap
                                    finalBmp?.let { onSave(it, false) }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Sobrescrever Atual")
                            }
                            OutlinedButton(
                                onClick = {
                                    showSaveDialog = false
                                    val finalBmp = finalBitmapToSave ?: editedBitmap
                                    finalBmp?.let { onSave(it, true) }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Salvar como Nova")
                            }
                            TextButton(
                                onClick = { showSaveDialog = false },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.textButtonColors(contentColor = FireColors.Error)
                            ) {
                                Text("Cancelar")
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = null
                )
            }
        }
    }
}
