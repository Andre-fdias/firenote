package com.example.firenotes.ui.screens.occurrence.dialogs

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.firenotes.domain.model.*
import com.example.firenotes.ui.designsystem.colors.FireColors
import com.example.firenotes.ui.designsystem.icons.FireIcons
import com.example.firenotes.ui.designsystem.spacing.FireSpacing
import com.example.firenotes.ui.designsystem.typography.FireTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVictimDialogV3(
    pessoasDisponiveis: List<Pessoa>,
    viaturasDisponiveis: List<Viatura>,
    onDismiss: () -> Unit,
    onConfirm: (
        pessoaId: String,
        lesoes: String,
        lesoesEstruturadas: List<Lesao>,
        destinoSocorro: String,
        quemSocorreu: String,
        resultado: String,
        viaturaSocorroId: String?,
        hospitalDestino: String,
        nomeMedico: String,
        crmMedico: String,
        pulso: Int?,
        pressaoArterial: String,
        saturacaoO2: Int?,
        aberturaOcular: Int?,
        respostaVerbal: Int?,
        respostaMotora: Int?,
        respiracao: Int?
    ) -> Unit
) {
    // ─── Estados de identificação ────────────────────────────────────────────
    var selectedPessoaId by remember { mutableStateOf<String?>(null) }
    var expandedPessoaDropdown by remember { mutableStateOf(false) }
    var resultado by remember { mutableStateOf("") }

    // ─── Sinais Vitais ───────────────────────────────────────────────────────
    var pulso by remember { mutableStateOf("") }
    var pressaoArterial by remember { mutableStateOf("") }
    var saturacaoO2 by remember { mutableStateOf("") }
    var respiracao by remember { mutableStateOf("") }

    // ─── GCS por domínio ─────────────────────────────────────────────────────
    var aberturaOcular by remember { mutableStateOf<Int?>(null) }
    var respostaVerbal by remember { mutableStateOf<Int?>(null) }
    var respostaMotora by remember { mutableStateOf<Int?>(null) }
    val gcsTotal by remember {
        derivedStateOf {
            val ao = aberturaOcular; val rv = respostaVerbal; val rm = respostaMotora
            if (ao != null && rv != null && rm != null) ao + rv + rm else null
        }
    }

    // ─── Lesões e infográfico ────────────────────────────────────────────────
    var lesoesTexto by remember { mutableStateOf("") }
    var lesoesEstruturadas by remember { mutableStateOf<List<Lesao>>(emptyList()) }
    var showBodyMapDialog by remember { mutableStateOf(false) }

    // ─── Transporte e socorro ────────────────────────────────────────────────
    var quemSocorreu by remember { mutableStateOf("") }
    var expandedSocorreuDropdown by remember { mutableStateOf(false) }
    var expandedViaturaDropdown by remember { mutableStateOf(false) }
    var selectedViaturaId by remember { mutableStateOf<String?>(null) }
    var hospitalDestino by remember { mutableStateOf("") }

    // ─── Médico ──────────────────────────────────────────────────────────────
    var nomeMedico by remember { mutableStateOf("") }
    var crmMedico by remember { mutableStateOf("") }

    // ─── Validação ───────────────────────────────────────────────────────────
    var pulsoError by remember { mutableStateOf<String?>(null) }
    var satO2Error by remember { mutableStateOf<String?>(null) }
    var respError by remember { mutableStateOf<String?>(null) }

    // ─── Listas de opções ────────────────────────────────────────────────────
    val quemSocorreuOptions = listOf(
        "SAMU", "Corpo de Bombeiros", "Concessionária",
        "Ambulância", "PM", "GCM", "Outros"
    )
    val resultadoOptions = listOf("Atendida", "Cancelado", "Recusa")

    val pessoaSelecionada = pessoasDisponiveis.find { it.id == selectedPessoaId }
    val idadeCalculada = remember(selectedPessoaId) {
        pessoaSelecionada?.nascimento?.let { nasc ->
            try {
                val parts = nasc.split("/")
                if (parts.size == 3) {
                    val bd = java.time.LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
                    java.time.Period.between(bd, java.time.LocalDate.now()).years
                } else null
            } catch (e: Exception) { null }
        }
    }

    val isFormValid = selectedPessoaId != null &&
            quemSocorreu.isNotBlank() &&
            resultado.isNotBlank() &&
            pulsoError == null && satO2Error == null && respError == null

    // ════════════════════════════════════════════════════════════════════════
    // UI — Tela completa em Dialog fullscreen
    // ════════════════════════════════════════════════════════════════════════
    var showConfirmCancelDialog by remember { mutableStateOf(false) }

    val hasChanges = remember(
        selectedPessoaId, resultado, pulso, pressaoArterial, saturacaoO2, respiracao,
        aberturaOcular, respostaVerbal, respostaMotora, lesoesTexto, lesoesEstruturadas,
        quemSocorreu, selectedViaturaId, hospitalDestino, nomeMedico, crmMedico
    ) {
        selectedPessoaId != null || resultado.isNotEmpty() || pulso.isNotEmpty() ||
        pressaoArterial.isNotEmpty() || saturacaoO2.isNotEmpty() || respiracao.isNotEmpty() ||
        aberturaOcular != null || respostaVerbal != null || respostaMotora != null ||
        lesoesTexto.isNotEmpty() || lesoesEstruturadas.isNotEmpty() || quemSocorreu.isNotEmpty() ||
        selectedViaturaId != null || hospitalDestino.isNotEmpty() || nomeMedico.isNotEmpty() || crmMedico.isNotEmpty()
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

    Dialog(
        onDismissRequest = { attemptDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Registrar Vítima",
                            style = FireTypography.Title,
                            fontWeight = FontWeight.Bold,
                            color = FireColors.Primary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { attemptDismiss() }) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar")
                        }
                    },
                    actions = {
                        Button(
                            onClick = {
                                if (isFormValid) {
                                    onConfirm(
                                        selectedPessoaId!!,
                                        lesoesTexto,
                                        lesoesEstruturadas,
                                        "",
                                        quemSocorreu,
                                        resultado,
                                        selectedViaturaId,
                                        hospitalDestino,
                                        nomeMedico,
                                        crmMedico,
                                        pulso.toIntOrNull(),
                                        pressaoArterial,
                                        saturacaoO2.toIntOrNull(),
                                        aberturaOcular,
                                        respostaVerbal,
                                        respostaMotora,
                                        respiracao.toIntOrNull()
                                    )
                                }
                            },
                            enabled = isFormValid,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FireColors.Primary,
                                disabledContainerColor = FireColors.SurfaceVariant
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                "Confirmar",
                                color = if (isFormValid) Color.White else FireColors.OnSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = FireColors.Surface
                    )
                )
            },
            containerColor = FireColors.Background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = FireSpacing.Medium, vertical = FireSpacing.Small),
                verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
            ) {

                // ── CARD: Identificação ──────────────────────────────────────
                SectionCard(emoji = "👤", title = "Identificação da Vítima") {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = pessoaSelecionada?.nome ?: "",
                            onValueChange = {},
                            label = { Text("Selecionar Pessoa") },
                            placeholder = { Text("Buscar pessoa cadastrada") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { expandedPessoaDropdown = !expandedPessoaDropdown }) {
                                    Icon(
                                        if (expandedPessoaDropdown) FireIcons.ArrowDropUp else FireIcons.ArrowDropDown,
                                        contentDescription = null
                                    )
                                }
                            },
                            colors = outlinedColors(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(
                            expanded = expandedPessoaDropdown,
                            onDismissRequest = { expandedPessoaDropdown = false }
                        ) {
                            if (pessoasDisponiveis.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Nenhuma pessoa cadastrada", color = FireColors.OnSurfaceVariant) },
                                    onClick = {}
                                )
                            } else {
                                pessoasDisponiveis.forEach { p ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(p.nome, fontWeight = FontWeight.Medium)
                                                Text(
                                                    "CPF: ${p.cpf ?: "N/A"}",
                                                    style = FireTypography.LabelSmall,
                                                    color = FireColors.OnSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = { selectedPessoaId = p.id; expandedPessoaDropdown = false }
                                    )
                                }
                            }
                        }
                    }

                    if (idadeCalculada != null) {
                        Text(
                            text = "Idade calculada: $idadeCalculada anos",
                            style = FireTypography.BodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = FireColors.Primary
                        )
                    }

                    // Resultado
                    Box(modifier = Modifier.fillMaxWidth()) {
                        var expandedResultado by remember { mutableStateOf(false) }
                        OutlinedTextField(
                            value = resultado,
                            onValueChange = {},
                            label = { Text("Resultado da Ocorrência") },
                            placeholder = { Text("Selecione...") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { expandedResultado = !expandedResultado }) {
                                    Icon(if (expandedResultado) FireIcons.ArrowDropUp else FireIcons.ArrowDropDown, null)
                                }
                            },
                            colors = outlinedColors(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(expanded = expandedResultado, onDismissRequest = { expandedResultado = false }) {
                            resultadoOptions.forEach { opt ->
                                DropdownMenuItem(text = { Text(opt) }, onClick = { resultado = opt; expandedResultado = false })
                            }
                        }
                    }
                }

                // ── CARD: Lesões e Infográfico ───────────────────────────────
                SectionCard(emoji = "🩹", title = "Lesões e Avaliação") {
                    OutlinedTextField(
                        value = lesoesTexto,
                        onValueChange = { lesoesTexto = it },
                        label = { Text("Histórico das Lesões") },
                        placeholder = { Text("Descreva detalhadamente as lesões...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3, maxLines = 5,
                        colors = outlinedColors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    FilledTonalButton(
                        onClick = { showBodyMapDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = FireColors.Primary.copy(alpha = 0.1f),
                            contentColor = FireColors.Primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (lesoesEstruturadas.isEmpty()) "Mapear Lesões no Corpo"
                            else "Editar Lesões (${lesoesEstruturadas.size})",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (lesoesEstruturadas.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(lesoesEstruturadas) { lesao ->
                                LesaoChip(lesao = lesao, onRemove = {
                                    lesoesEstruturadas = lesoesEstruturadas - lesao
                                })
                            }
                        }
                    }
                }

                // ── CARD: Sinais Vitais ──────────────────────────────────────
                SectionCard(emoji = "🩺", title = "Sinais Vitais") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                    ) {
                        OutlinedTextField(
                            value = pulso,
                            onValueChange = {
                                val d = it.filter(Char::isDigit)
                                pulso = d
                                pulsoError = if (d.isNotBlank() && (d.toIntOrNull() ?: -1) !in 3..300) "FC: 3-300 bpm" else null
                            },
                            label = { Text("FC (BPM)") },
                            placeholder = { Text("80") },
                            isError = pulsoError != null,
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = outlinedColors(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = pressaoArterial,
                            onValueChange = { pressaoArterial = it },
                            label = { Text("P.A.") },
                            placeholder = { Text("120x80") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = outlinedColors(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                    ) {
                        OutlinedTextField(
                            value = saturacaoO2,
                            onValueChange = {
                                val d = it.filter(Char::isDigit)
                                saturacaoO2 = d
                                satO2Error = if (d.isNotBlank() && (d.toIntOrNull() ?: -1) !in 0..100) "SatO₂: 0-100%" else null
                            },
                            label = { Text("Sat. O₂ (%)") },
                            placeholder = { Text("98") },
                            isError = satO2Error != null,
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = outlinedColors(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = respiracao,
                            onValueChange = {
                                val d = it.filter(Char::isDigit)
                                respiracao = d
                                respError = if (d.isNotBlank() && (d.toIntOrNull() ?: -1) !in 0..60) "Resp.: 0-60 mpm" else null
                            },
                            label = { Text("Resp. (mpm)") },
                            placeholder = { Text("16") },
                            isError = respError != null,
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = outlinedColors(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    listOfNotNull(pulsoError, satO2Error, respError).forEach {
                        Text(it, style = FireTypography.LabelSmall, color = FireColors.Error)
                    }
                }

                // ── CARD: Glasgow Coma Scale ─────────────────────────────────
                SectionCard(emoji = "🧠", title = "Escala de Coma de Glasgow") {
                    val gcsBadgeColor = when {
                        gcsTotal == null -> FireColors.SurfaceVariant
                        gcsTotal!! >= 13 -> FireColors.Success
                        gcsTotal!! >= 9  -> FireColors.Warning
                        else             -> FireColors.Error
                    }
                    val gcsSeverity = when {
                        gcsTotal == null -> "Não avaliado"
                        gcsTotal!! >= 13 -> "Lesão Leve"
                        gcsTotal!! >= 9  -> "Lesão Moderada"
                        else             -> "Lesão Grave (Coma)"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(gcsBadgeColor.copy(alpha = 0.12f))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total GCS", style = FireTypography.LabelSmall, color = FireColors.OnSurfaceVariant)
                            Text(gcsSeverity, style = FireTypography.LabelMedium, fontWeight = FontWeight.Bold, color = gcsBadgeColor)
                        }
                        Text(
                            text = gcsTotal?.toString() ?: "--",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = gcsBadgeColor
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    GcsDomainSelector(
                        label = "Abertura Ocular (AO)",
                        description = "Como os olhos respondem",
                        options = listOf(1 to "Ausente", 2 to "Ao estímulo doloroso", 3 to "Ao comando verbal", 4 to "Espontânea"),
                        selected = aberturaOcular,
                        onSelect = { aberturaOcular = it }
                    )

                    GcsDomainSelector(
                        label = "Resposta Verbal (RV)",
                        description = "Qualidade da fala/sons",
                        options = listOf(1 to "Ausente", 2 to "Sons incompreensíveis", 3 to "Palavras inapropriadas", 4 to "Confusa", 5 to "Orientada"),
                        selected = respostaVerbal,
                        onSelect = { respostaVerbal = it }
                    )

                    GcsDomainSelector(
                        label = "Resposta Motora (RM)",
                        description = "Melhor resposta dos membros",
                        options = listOf(1 to "Ausente", 2 to "Extensão anormal", 3 to "Flexão anormal", 4 to "Flexão normal (retirada)", 5 to "Localiza a dor", 6 to "Obedece a comandos"),
                        selected = respostaMotora,
                        onSelect = { respostaMotora = it }
                    )
                }

                // ── CARD: Transporte e Socorro ───────────────────────────────
                SectionCard(emoji = "🚑", title = "Transporte e Socorro") {
                    // Quem Socorreu
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = quemSocorreu,
                            onValueChange = {},
                            label = { Text("Quem Socorreu") },
                            placeholder = { Text("Selecione...") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { expandedSocorreuDropdown = !expandedSocorreuDropdown }) {
                                    Icon(if (expandedSocorreuDropdown) FireIcons.ArrowDropUp else FireIcons.ArrowDropDown, null)
                                }
                            },
                            colors = outlinedColors(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(expanded = expandedSocorreuDropdown, onDismissRequest = { expandedSocorreuDropdown = false }) {
                            quemSocorreuOptions.forEach { opt ->
                                DropdownMenuItem(text = { Text(opt) }, onClick = { quemSocorreu = opt; expandedSocorreuDropdown = false })
                            }
                        }
                    }

                    // Viatura de Socorro
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = viaturasDisponiveis.find { it.id == selectedViaturaId }?.prefixo ?: "",
                            onValueChange = {},
                            label = { Text("Viatura de Socorro") },
                            placeholder = { Text("Selecione (opcional)") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { expandedViaturaDropdown = !expandedViaturaDropdown }) {
                                    Icon(if (expandedViaturaDropdown) FireIcons.ArrowDropUp else FireIcons.ArrowDropDown, null)
                                }
                            },
                            colors = outlinedColors(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(expanded = expandedViaturaDropdown, onDismissRequest = { expandedViaturaDropdown = false }) {
                            if (viaturasDisponiveis.isEmpty()) {
                                DropdownMenuItem(text = { Text("Nenhuma viatura cadastrada", color = FireColors.OnSurfaceVariant) }, onClick = {})
                            } else {
                                viaturasDisponiveis.forEach { v ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(v.prefixo, fontWeight = FontWeight.Medium)
                                                Text("Unidade: ${v.unidade}", style = FireTypography.LabelSmall, color = FireColors.OnSurfaceVariant)
                                            }
                                        },
                                        onClick = { selectedViaturaId = v.id; expandedViaturaDropdown = false }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = hospitalDestino,
                        onValueChange = { hospitalDestino = it },
                        label = { Text("Hospital / Destino") },
                        placeholder = { Text("Ex: Hospital Regional, UPA Sul") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = outlinedColors(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // ── CARD: Médico Responsável ─────────────────────────────────
                SectionCard(emoji = "👨‍⚕️", title = "Médico Responsável") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                    ) {
                        OutlinedTextField(
                            value = nomeMedico,
                            onValueChange = { nomeMedico = it },
                            label = { Text("Nome do Médico") },
                            placeholder = { Text("Dr. João Silva") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = outlinedColors(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = crmMedico,
                            onValueChange = { crmMedico = it },
                            label = { Text("CRM") },
                            placeholder = { Text("CRM-SP 123456") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = outlinedColors(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(Modifier.height(FireSpacing.Large))
            }
        }
    }

    // ── Infográfico corporal ─────────────────────────────────────────────────
    if (showBodyMapDialog) {
        BodyMapDialog(
            currentLesoes = lesoesEstruturadas,
            onConfirm = { lesoes ->
                lesoesEstruturadas = lesoes
                showBodyMapDialog = false
            },
            onDismiss = { showBodyMapDialog = false }
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════
// COMPONENTES AUXILIARES
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun SectionCard(
    emoji: String,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = FireColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(FireSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text(title, style = FireTypography.Title, fontWeight = FontWeight.Bold, color = FireColors.Primary)
            }
            HorizontalDivider(color = FireColors.OnSurfaceVariant.copy(alpha = 0.12f))
            content()
        }
    }
}

@Composable
private fun GcsDomainSelector(
    label: String,
    description: String,
    options: List<Pair<Int, String>>,
    selected: Int?,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = FireTypography.LabelLarge, fontWeight = FontWeight.SemiBold, color = FireColors.OnSurface)
                Text(description, style = FireTypography.LabelSmall, color = FireColors.OnSurfaceVariant)
            }
            if (selected != null) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(FireColors.Primary)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(selected.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            options.forEach { (score, desc) ->
                val isSelected = selected == score
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) FireColors.Primary.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable { onSelect(score) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) FireColors.Primary else FireColors.SurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(score.toString(), color = if (isSelected) Color.White else FireColors.OnSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Text(
                        desc,
                        style = FireTypography.BodySmall,
                        color = if (isSelected) FireColors.Primary else FireColors.OnSurface,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        Icon(Icons.Default.Check, null, tint = FireColors.Primary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LesaoChip(lesao: Lesao, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = FireColors.Primary.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, FireColors.Primary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "${lesao.regiao.emoji} ${lesao.regiao.label} • ${lesao.tipo.label}",
                style = FireTypography.LabelSmall,
                color = FireColors.Primary,
                fontWeight = FontWeight.Medium
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Close, null, tint = FireColors.Primary, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
private fun outlinedColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = FireColors.Primary,
    unfocusedBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.5f),
    focusedLabelColor = FireColors.Primary,
    unfocusedLabelColor = FireColors.OnSurfaceVariant
)

// ════════════════════════════════════════════════════════════════════════════
// INFOGRÁFICO CORPORAL — MAPA DE LESÕES
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun BodyMapDialog(
    currentLesoes: List<Lesao>,
    onConfirm: (List<Lesao>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedRegiao by remember { mutableStateOf<RegiaoCorporal?>(null) }
    var tempLesoes by remember { mutableStateOf(currentLesoes) }
    var showFront by remember { mutableStateOf(true) }

    val tiposFerrimento = TipoFerimento.values().toList()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = FireColors.Surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(FireSpacing.Medium),
                verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🗺️ Mapa Corporal", style = FireTypography.Title, fontWeight = FontWeight.Bold, color = FireColors.Primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancelar", color = FireColors.OnSurfaceVariant)
                        }
                        Button(
                            onClick = { onConfirm(tempLesoes) },
                            colors = ButtonDefaults.buttonColors(containerColor = FireColors.Primary)
                        ) {
                            Text("Aplicar (${tempLesoes.size})", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Toggle Frente/Dorso
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(FireColors.SurfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(true to "👤 Frente", false to "🔄 Dorso").forEach { (isFront, label) ->
                        val isActive = showFront == isFront
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isActive) FireColors.Primary else Color.Transparent)
                                .clickable { showFront = isFront; selectedRegiao = null }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                color = if (isActive) Color.White else FireColors.OnSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                ) {
                    // Silhueta corporal — lado esquerdo
                    Column(
                        modifier = Modifier
                            .width(155.dp)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val regioes = if (showFront) listOf(
                            RegiaoCorporal.CABECA_FRENTE,
                            RegiaoCorporal.TORAX_FRENTE,
                            RegiaoCorporal.ABDOME_FRENTE,
                            RegiaoCorporal.MEMBRO_SUP_DIREITO,
                            RegiaoCorporal.MEMBRO_SUP_ESQUERDO,
                            RegiaoCorporal.MEMBRO_INF_DIREITO,
                            RegiaoCorporal.MEMBRO_INF_ESQUERDO
                        ) else listOf(
                            RegiaoCorporal.CABECA_DORSO,
                            RegiaoCorporal.TORAX_DORSO,
                            RegiaoCorporal.ABDOME_DORSO,
                            RegiaoCorporal.MEMBRO_SUP_DIREITO,
                            RegiaoCorporal.MEMBRO_SUP_ESQUERDO,
                            RegiaoCorporal.MEMBRO_INF_DIREITO,
                            RegiaoCorporal.MEMBRO_INF_ESQUERDO
                        )

                        regioes.forEach { regiao ->
                            val hasLesao = tempLesoes.any { it.regiao == regiao }
                            val isActive = selectedRegiao == regiao
                            val lesaoCount = tempLesoes.count { it.regiao == regiao }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            isActive -> FireColors.Primary.copy(alpha = 0.2f)
                                            hasLesao -> FireColors.Error.copy(alpha = 0.15f)
                                            else -> FireColors.SurfaceVariant.copy(alpha = 0.5f)
                                        }
                                    )
                                    .border(
                                        width = if (isActive || hasLesao) 2.dp else 1.dp,
                                        color = when {
                                            isActive -> FireColors.Primary
                                            hasLesao -> FireColors.Error
                                            else -> FireColors.OnSurfaceVariant.copy(alpha = 0.2f)
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedRegiao = if (selectedRegiao == regiao) null else regiao }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(regiao.emoji, fontSize = 14.sp)
                                    Text(
                                        regiao.label,
                                        style = FireTypography.LabelSmall,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isActive -> FireColors.Primary
                                            hasLesao -> FireColors.Error
                                            else -> FireColors.OnSurface
                                        },
                                        textAlign = TextAlign.Start,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (hasLesao) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(3.dp)
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(FireColors.Error),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(lesaoCount.toString(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Painel de tipos de ferimento — lado direito
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        if (selectedRegiao == null) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("👆", fontSize = 36.sp)
                                    Text(
                                        "Toque em uma região\npara registrar um ferimento",
                                        style = FireTypography.BodyMedium,
                                        color = FireColors.OnSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            val regiao = selectedRegiao!!
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    "${regiao.emoji} ${regiao.label}",
                                    style = FireTypography.LabelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = FireColors.Primary
                                )
                                Text("Selecione o tipo de ferimento:", style = FireTypography.LabelSmall, color = FireColors.OnSurfaceVariant)

                                tiposFerrimento.forEach { tipo ->
                                    val jaExiste = tempLesoes.any { it.regiao == regiao && it.tipo == tipo }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (jaExiste) FireColors.Error.copy(alpha = 0.1f) else FireColors.SurfaceVariant.copy(alpha = 0.4f))
                                            .clickable {
                                                tempLesoes = if (jaExiste)
                                                    tempLesoes.filterNot { it.regiao == regiao && it.tipo == tipo }
                                                else
                                                    tempLesoes + Lesao(regiao = regiao, tipo = tipo)
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            tipo.label,
                                            style = FireTypography.BodyMedium,
                                            color = if (jaExiste) FireColors.Error else FireColors.OnSurface,
                                            fontWeight = if (jaExiste) FontWeight.SemiBold else FontWeight.Normal,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            if (jaExiste) Icons.Default.Check else Icons.Default.Add,
                                            null,
                                            tint = if (jaExiste) FireColors.Error else FireColors.OnSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Rodapé — lesões selecionadas
                if (tempLesoes.isNotEmpty()) {
                    HorizontalDivider()
                    Text("${tempLesoes.size} lesão(ões) registrada(s):", style = FireTypography.LabelSmall, color = FireColors.OnSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(tempLesoes) { l ->
                            LesaoChip(lesao = l, onRemove = { tempLesoes = tempLesoes - l })
                        }
                    }
                }
            }
        }
    }
}
