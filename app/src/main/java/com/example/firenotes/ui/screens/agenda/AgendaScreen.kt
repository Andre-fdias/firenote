package com.example.firenotes.ui.screens.agenda

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import com.example.firenotes.data.local.entities.RoomEventoAgenda
import com.example.firenotes.data.local.entities.RoomTarefa
import com.example.firenotes.data.service.ProntidaoService
import com.example.firenotes.ui.designsystem.colors.FireColors
import com.example.firenotes.ui.designsystem.typography.FireTypography
import com.example.firenotes.ui.screens.home.HomeViewModel
import com.example.firenotes.ui.screens.home.Prioridade
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class TipoEvento(val label: String, val icon: String) {
    INSTRUCAO("Instrucao", "📚"),
    REUNIAO("Reuniao", "🤝"),
    ADMINISTRATIVO("Administrativo", "📋"),
    SAUDE("Saude", "🏥"),
    ESCALA("Escala", "🚒"),
    OUTRO("Outro", "📌")
}

enum class CategoriaTask(val label: String, val icon: String) {
    OPERACIONAL("Operacional", "🚒"),
    ADMINISTRATIVO("Administrativo", "📋"),
    MANUTENCAO("Manutencao", "🔧"),
    PESSOAL("Pessoal", "👤")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(
    date: LocalDate,
    viewModel: HomeViewModel,
    onNavigateBack: () -> Unit
) {
    val allTarefas by viewModel.allTarefas.collectAsState()
    val allEventos by viewModel.allEventos.collectAsState()

    val tarefasDoDia = remember(allTarefas, date) {
        allTarefas.filter { it.data == date.toString() }
    }
    val eventosDoDia = remember(allEventos, date) {
        allEventos.filter { it.data == date.toString() }
    }

    val prontidao = remember(date) { ProntidaoService.getProntidaoForDate(date) }
    val prontidaoColor = when (prontidao) {
        ProntidaoService.Prontidao.AMARELA -> Color(0xFFFFC107)
        ProntidaoService.Prontidao.AZUL    -> Color(0xFF2196F3)
        ProntidaoService.Prontidao.VERDE   -> Color(0xFF4CAF50)
    }

    var showAddEventDialog by remember { mutableStateOf(false) }
    var showAddTaskDialog  by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<RoomEventoAgenda?>(null) }
    var editingTask  by remember { mutableStateOf<RoomTarefa?>(null) }
    var filterPriority by remember { mutableStateOf<Prioridade?>(null) }
    var activeTab by remember { mutableStateOf(0) }

    val dateLabel = remember(date) {
        date.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.forLanguageTag("pt-BR")))
            .replaceFirstChar { it.uppercase() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("📅 Agenda Operacional", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = FireColors.OnBackground)
                        Text(dateLabel, fontSize = 12.sp, color = FireColors.OnSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar", tint = FireColors.OnBackground)
                    }
                },
                actions = {
                    Surface(shape = RoundedCornerShape(20.dp), color = prontidaoColor.copy(alpha = 0.15f)) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(prontidaoColor, CircleShape))
                            Text(prontidao.nome.replace("Prontidão ", ""), fontSize = 11.sp, color = prontidaoColor, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FireColors.Surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (activeTab == 0) showAddTaskDialog = true else showAddEventDialog = true },
                containerColor = FireColors.Primary, contentColor = Color.White, shape = CircleShape
            ) { Icon(Icons.Filled.Add, contentDescription = "Adicionar") }
        },
        containerColor = FireColors.Background
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = FireColors.Surface,
                contentColor = FireColors.Primary
            ) {
                Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, text = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("📋 Tarefas")
                        if (tarefasDoDia.isNotEmpty()) {
                            val done = tarefasDoDia.count { it.concluida }
                            Surface(shape = CircleShape, color = FireColors.Secondary.copy(alpha = 0.2f)) {
                                Text("$done/${tarefasDoDia.size}", fontSize = 10.sp, color = FireColors.Secondary,
                                    fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                })
                Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, text = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("📅 Eventos")
                        if (eventosDoDia.isNotEmpty()) {
                            Surface(shape = CircleShape, color = FireColors.Primary.copy(alpha = 0.2f)) {
                                Text("${eventosDoDia.size}", fontSize = 10.sp, color = FireColors.Primary,
                                    fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                })
            }

            AnimatedContent(targetState = activeTab, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) }, label = "tab") { tab ->
                when (tab) {
                    0 -> TaskTab(tasks = tarefasDoDia, filterPriority = filterPriority,
                        onFilterPriorityChange = { filterPriority = it },
                        onToggle = { viewModel.toggleTarefa(it) },
                        onDelete = { viewModel.deleteTarefa(it) },
                        onEdit   = { editingTask = it })
                    else -> EventTab(events = eventosDoDia, onDelete = { viewModel.deleteEvento(it) }, onEdit = { editingEvent = it })
                }
            }
        }

        if (showAddTaskDialog) {
            TaskDialog(initial = null, initialDate = date,
                onConfirm = { titulo, desc, prio, cat, selectedDate, selectedTime ->
                    viewModel.addTarefa(titulo, selectedDate, desc, prio, cat.label, selectedTime)
                    showAddTaskDialog = false
                }, onDismiss = { showAddTaskDialog = false })
        }
        editingTask?.let { task ->
            val initDate = runCatching { LocalDate.parse(task.data) }.getOrDefault(date)
            TaskDialog(initial = task, initialDate = initDate,
                onConfirm = { titulo, desc, prio, cat, selectedDate, selectedTime ->
                    viewModel.updateTarefa(task.copy(titulo = titulo, descricao = desc, prioridade = prio.name, categoria = cat.label, data = selectedDate.toString(), hora = selectedTime))
                    editingTask = null
                }, onDismiss = { editingTask = null })
        }
        if (showAddEventDialog) {
            EventDialog(initial = null, initialDate = date,
                onConfirm = { titulo, desc, selectedDate, inicio, fim, tipo ->
                    viewModel.addEvento(titulo, desc, selectedDate, inicio, fim, tipo)
                    showAddEventDialog = false
                }, onDismiss = { showAddEventDialog = false })
        }
        editingEvent?.let { event ->
            val initDate = runCatching { LocalDate.parse(event.data) }.getOrDefault(date)
            EventDialog(initial = event, initialDate = initDate,
                onConfirm = { titulo, desc, selectedDate, inicio, fim, tipo ->
                    viewModel.updateEvento(event.copy(titulo = titulo, descricao = desc, data = selectedDate.toString(), horaInicio = inicio, horaFim = fim, tipo = tipo.name))
                    editingEvent = null
                }, onDismiss = { editingEvent = null })
        }
    }
}

@Composable
fun TaskTab(tasks: List<RoomTarefa>, filterPriority: Prioridade?,
    onFilterPriorityChange: (Prioridade?) -> Unit,
    onToggle: (RoomTarefa) -> Unit, onDelete: (String) -> Unit, onEdit: (RoomTarefa) -> Unit) {
    val filtered = remember(tasks, filterPriority) {
        tasks.filter { filterPriority == null || it.prioridade == filterPriority.name }
            .sortedWith(compareBy<RoomTarefa> { it.concluida }
                .thenBy { runCatching { Prioridade.valueOf(it.prioridade) }.getOrDefault(Prioridade.MEDIA).ordinal })
    }
    val done = tasks.count { it.concluida }
    val total = tasks.size

    Column(modifier = Modifier.fillMaxSize()) {
        if (total > 0) {
            Column(modifier = Modifier.fillMaxWidth().background(FireColors.Surface).padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Progresso do dia", fontSize = 12.sp, color = FireColors.OnSurfaceVariant)
                    Text("$done/$total concluidas", fontSize = 12.sp, color = FireColors.Secondary, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(progress = { if (total > 0) done.toFloat()/total else 0f },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = FireColors.Secondary, trackColor = FireColors.Secondary.copy(alpha = 0.15f))
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Prioridade:", fontSize = 11.sp, color = FireColors.OnSurfaceVariant)
            Prioridade.values().forEach { p ->
                val sel = filterPriority == p
                val color = when(p) { Prioridade.ALTA -> FireColors.Error; Prioridade.MEDIA -> FireColors.Warning; else -> FireColors.Success }
                val lbl = when(p) { Prioridade.ALTA -> "🔴 Alta"; Prioridade.MEDIA -> "🟡 Media"; else -> "🟢 Baixa" }
                FilterChip(selected = sel, onClick = { onFilterPriorityChange(if (sel) null else p) }, label = { Text(lbl, fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = color, selectedLabelColor = Color.White,
                        containerColor = color.copy(alpha = 0.1f), labelColor = color))
            }
        }
        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📋", fontSize = 40.sp)
                    Text("Nenhuma tarefa para este dia", color = FireColors.OnSurfaceVariant, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.id }) { task ->
                    AgendaTaskItem(task = task, onToggle = { onToggle(task) }, onDelete = { onDelete(task.id) }, onEdit = { onEdit(task) })
                }
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }
}

@Composable
fun AgendaTaskItem(task: RoomTarefa, onToggle: () -> Unit, onDelete: () -> Unit, onEdit: () -> Unit) {
    val prio = runCatching { Prioridade.valueOf(task.prioridade) }.getOrDefault(Prioridade.MEDIA)
    val color = when(prio) { Prioridade.ALTA -> FireColors.Error; Prioridade.MEDIA -> FireColors.Warning; else -> FireColors.Success }
    Card(shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (task.concluida) FireColors.Surface.copy(alpha=0.4f) else FireColors.Surface),
        modifier = Modifier.fillMaxWidth().border(1.dp, if (task.concluida) Color.Transparent else color.copy(alpha=0.25f), RoundedCornerShape(14.dp))) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(4.dp).height(48.dp).clip(RoundedCornerShape(2.dp))
                .background(if (task.concluida) color.copy(alpha=0.3f) else color))
            Spacer(modifier = Modifier.width(10.dp))
            Checkbox(checked = task.concluida, onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = FireColors.Secondary, uncheckedColor = FireColors.OnSurfaceVariant))
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(task.titulo, fontSize = 14.sp, fontWeight = if (task.concluida) FontWeight.Normal else FontWeight.SemiBold,
                    color = if (task.concluida) FireColors.OnSurfaceVariant.copy(alpha=0.5f) else FireColors.OnBackground,
                    textDecoration = if (task.concluida) TextDecoration.LineThrough else TextDecoration.None)
                if (!task.descricao.isNullOrBlank()) Text(task.descricao, fontSize = 11.sp, color = FireColors.OnSurfaceVariant.copy(alpha=0.7f))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top=4.dp)) {
                    Surface(shape = RoundedCornerShape(4.dp), color = color.copy(alpha=0.12f)) {
                        Text(task.prioridade, fontSize = 9.sp, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal=5.dp, vertical=2.dp))
                    }
                    if (task.categoria.isNotBlank()) Text("• ${task.categoria}", fontSize = 10.sp, color = FireColors.OnSurfaceVariant.copy(alpha=0.6f))
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) { Icon(Icons.Outlined.Edit, null, tint = FireColors.Primary.copy(alpha=0.7f), modifier = Modifier.size(16.dp)) }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Delete, null, tint = FireColors.Error.copy(alpha=0.6f), modifier = Modifier.size(16.dp)) }
            }
        }
    }
}

@Composable
fun EventTab(events: List<RoomEventoAgenda>, onDelete: (String) -> Unit, onEdit: (RoomEventoAgenda) -> Unit) {
    if (events.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("📅", fontSize = 40.sp)
                Text("Nenhum evento para este dia", color = FireColors.OnSurfaceVariant, fontSize = 14.sp)
            }
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal=16.dp, vertical=12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(events, key = { it.id }) { event -> AgendaEventItem(event=event, onDelete={ onDelete(event.id) }, onEdit={ onEdit(event) }) }
            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}

@Composable
fun AgendaEventItem(event: RoomEventoAgenda, onDelete: () -> Unit, onEdit: () -> Unit) {
    val tipo = runCatching { TipoEvento.valueOf(event.tipo ?: "OUTRO") }.getOrDefault(TipoEvento.OUTRO)
    val tipoColor = when(tipo) {
        TipoEvento.INSTRUCAO -> Color(0xFF9C27B0); TipoEvento.REUNIAO -> Color(0xFF2196F3)
        TipoEvento.ADMINISTRATIVO -> Color(0xFF607D8B); TipoEvento.SAUDE -> Color(0xFF4CAF50)
        TipoEvento.ESCALA -> FireColors.Primary; else -> FireColors.OnSurfaceVariant
    }
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = FireColors.Surface),
        modifier = Modifier.fillMaxWidth().border(1.dp, tipoColor.copy(alpha=0.25f), RoundedCornerShape(14.dp))) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).background(tipoColor.copy(alpha=0.12f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Text(tipo.icon, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(event.titulo, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = FireColors.OnBackground)
                if (!event.descricao.isNullOrBlank()) Text(event.descricao, fontSize = 11.sp, color = FireColors.OnSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top=4.dp)) {
                    Surface(shape = RoundedCornerShape(6.dp), color = tipoColor.copy(alpha=0.12f)) {
                        Text("${tipo.icon} ${tipo.label}", fontSize = 9.sp, color = tipoColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal=6.dp, vertical=2.dp))
                    }
                    Icon(Icons.Outlined.AccessTime, null, modifier = Modifier.size(12.dp), tint = FireColors.OnSurfaceVariant)
                    Text("${event.horaInicio ?: "08:00"} - ${event.horaFim ?: "Fim"}", fontSize = 11.sp, color = FireColors.Primary, fontWeight = FontWeight.Medium)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) { Icon(Icons.Outlined.Edit, null, tint = FireColors.Primary.copy(alpha=0.7f), modifier = Modifier.size(16.dp)) }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Delete, null, tint = FireColors.Error.copy(alpha=0.6f), modifier = Modifier.size(16.dp)) }
            }
        }
    }
}

@Composable
fun TaskDialog(
    initial: RoomTarefa?,
    initialDate: LocalDate,
    onConfirm: (String, String?, Prioridade, CategoriaTask, LocalDate, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var titulo by remember { mutableStateOf(initial?.titulo ?: "") }
    var descricao by remember { mutableStateOf(initial?.descricao ?: "") }
    var prioridade by remember { mutableStateOf(runCatching { Prioridade.valueOf(initial?.prioridade ?: "") }.getOrDefault(Prioridade.MEDIA)) }
    var categoria by remember { mutableStateOf(CategoriaTask.values().firstOrNull { it.label == initial?.categoria } ?: CategoriaTask.OPERACIONAL) }
    
    var selectedDate by remember { mutableStateOf(if (initial != null) LocalDate.parse(initial.data) else initialDate) }
    var selectedTime by remember { mutableStateOf(initial?.hora ?: "") }

    val context = LocalContext.current
    val datePickerDialog = remember(selectedDate) {
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
            },
            selectedDate.year,
            selectedDate.monthValue - 1,
            selectedDate.dayOfMonth
        )
    }

    val timePickerDialog = remember(selectedTime) {
        val parsedTime = runCatching { java.time.LocalTime.parse(selectedTime) }.getOrDefault(java.time.LocalTime.now())
        android.app.TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                selectedTime = String.format("%02d:%02d", hourOfDay, minute)
            },
            parsedTime.hour,
            parsedTime.minute,
            true
        )
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(modifier = Modifier.fillMaxWidth(0.95f).wrapContentHeight(), shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = FireColors.Surface)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(if (initial == null) "📋 Nova Tarefa" else "✏️ Editar Tarefa", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = FireColors.OnBackground)
                OutlinedTextField(value = titulo, onValueChange = { titulo = it }, label = { Text("Titulo *") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = descricao, onValueChange = { descricao = it }, label = { Text("Observacoes (opcional)") }, maxLines = 3, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                
                // Date & Time pickers
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.weight(1f).clickable { datePickerDialog.show() }) {
                        OutlinedTextField(
                            value = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("Data") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = FireColors.OnBackground,
                                disabledBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.5f),
                                disabledLabelColor = FireColors.OnSurfaceVariant,
                                disabledLeadingIconColor = FireColors.OnSurfaceVariant
                            ),
                            leadingIcon = { Icon(Icons.Default.DateRange, null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                    Box(modifier = Modifier.weight(1f).clickable { timePickerDialog.show() }) {
                        OutlinedTextField(
                            value = selectedTime,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("Hora (opcional)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = FireColors.OnBackground,
                                disabledBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.5f),
                                disabledLabelColor = FireColors.OnSurfaceVariant,
                                disabledLeadingIconColor = FireColors.OnSurfaceVariant,
                                disabledTrailingIconColor = FireColors.OnSurfaceVariant
                            ),
                            leadingIcon = { Icon(Icons.Outlined.AccessTime, null, modifier = Modifier.size(16.dp)) },
                            trailingIcon = {
                                if (selectedTime.isNotEmpty()) {
                                    IconButton(onClick = { selectedTime = "" }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Clear, null, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Prioridade", fontSize = 12.sp, color = FireColors.OnSurfaceVariant, fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Prioridade.values().forEach { p ->
                            val sel = p == prioridade
                            val color = when(p) { Prioridade.ALTA -> FireColors.Error; Prioridade.MEDIA -> FireColors.Warning; else -> FireColors.Success }
                            val lbl = when(p) { Prioridade.ALTA -> "Alta"; Prioridade.MEDIA -> "Media"; else -> "Baixa" }
                            FilterChip(selected = sel, onClick = { prioridade = p }, label = { Text(lbl, fontSize = 11.sp) }, modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = color, selectedLabelColor = Color.White, containerColor = color.copy(alpha=0.1f), labelColor = color))
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Categoria", fontSize = 12.sp, color = FireColors.OnSurfaceVariant, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CategoriaTask.values().forEach { cat ->
                            val sel = cat == categoria
                            FilterChip(
                                selected = sel,
                                onClick = { categoria = cat },
                                label = { Text("${cat.icon} ${cat.label}", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FireColors.Primary,
                                    selectedLabelColor = Color.White,
                                    containerColor = FireColors.Primary.copy(alpha=0.08f),
                                    labelColor = FireColors.OnSurfaceVariant
                                )
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("Cancelar", color = FireColors.OnSurfaceVariant) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { if (titulo.isNotBlank()) onConfirm(titulo.trim(), descricao.trim().ifBlank { null }, prioridade, categoria, selectedDate, selectedTime.ifBlank { null }) },
                        enabled = titulo.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = FireColors.Primary), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(6.dp))
                        Text(if (initial == null) "Salvar" else "Atualizar")
                    }
                }
            }
        }
    }
}

@Composable
fun EventDialog(
    initial: RoomEventoAgenda?,
    initialDate: LocalDate,
    onConfirm: (String, String?, LocalDate, String?, String?, TipoEvento) -> Unit,
    onDismiss: () -> Unit
) {
    var titulo by remember { mutableStateOf(initial?.titulo ?: "") }
    var descricao by remember { mutableStateOf(initial?.descricao ?: "") }
    var inicio by remember { mutableStateOf(initial?.horaInicio ?: "08:00") }
    var fim by remember { mutableStateOf(initial?.horaFim ?: "09:00") }
    var tipo by remember { mutableStateOf(runCatching { TipoEvento.valueOf(initial?.tipo ?: "") }.getOrDefault(TipoEvento.OUTRO)) }
    
    var selectedDate by remember { mutableStateOf(if (initial != null) LocalDate.parse(initial.data) else initialDate) }

    val context = LocalContext.current
    val datePickerDialog = remember(selectedDate) {
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
            },
            selectedDate.year,
            selectedDate.monthValue - 1,
            selectedDate.dayOfMonth
        )
    }

    val timeInicioPickerDialog = remember(inicio) {
        val parsedTime = runCatching { java.time.LocalTime.parse(inicio) }.getOrDefault(java.time.LocalTime.of(8, 0))
        android.app.TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                inicio = String.format("%02d:%02d", hourOfDay, minute)
            },
            parsedTime.hour,
            parsedTime.minute,
            true
        )
    }

    val timeFimPickerDialog = remember(fim) {
        val parsedTime = runCatching { java.time.LocalTime.parse(fim) }.getOrDefault(java.time.LocalTime.of(9, 0))
        android.app.TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                fim = String.format("%02d:%02d", hourOfDay, minute)
            },
            parsedTime.hour,
            parsedTime.minute,
            true
        )
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(modifier = Modifier.fillMaxWidth(0.95f).wrapContentHeight(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = FireColors.Surface)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(if (initial == null) "📅 Novo Evento" else "✏️ Editar Evento", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = FireColors.OnBackground)
                
                // Clickable date picker
                Box(modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() }) {
                    OutlinedTextField(
                        value = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.forLanguageTag("pt-BR"))).replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Data do Evento") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = FireColors.OnBackground,
                            disabledBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.5f),
                            disabledLabelColor = FireColors.OnSurfaceVariant,
                            disabledLeadingIconColor = FireColors.OnSurfaceVariant
                        ),
                        leadingIcon = { Icon(Icons.Default.DateRange, null, modifier = Modifier.size(16.dp)) }
                    )
                }

                OutlinedTextField(value = titulo, onValueChange = { titulo = it }, label = { Text("Titulo *") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = descricao, onValueChange = { descricao = it }, label = { Text("Descricao (opcional)") }, maxLines = 3, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.weight(1f).clickable { timeInicioPickerDialog.show() }) {
                        OutlinedTextField(
                            value = inicio,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("Inicio") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = FireColors.OnBackground,
                                disabledBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.5f),
                                disabledLabelColor = FireColors.OnSurfaceVariant,
                                disabledLeadingIconColor = FireColors.OnSurfaceVariant
                            ),
                            leadingIcon = { Icon(Icons.Outlined.AccessTime, null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                    Box(modifier = Modifier.weight(1f).clickable { timeFimPickerDialog.show() }) {
                        OutlinedTextField(
                            value = fim,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("Termino") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = FireColors.OnBackground,
                                disabledBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.5f),
                                disabledLabelColor = FireColors.OnSurfaceVariant,
                                disabledLeadingIconColor = FireColors.OnSurfaceVariant
                            ),
                            leadingIcon = { Icon(Icons.Outlined.AccessTime, null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Tipo de evento", fontSize = 12.sp, color = FireColors.OnSurfaceVariant, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TipoEvento.values().forEach { t ->
                            val sel = t == tipo
                            FilterChip(
                                selected = t == tipo,
                                onClick = { tipo = t },
                                label = { Text("${t.icon} ${t.label}", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FireColors.Primary,
                                    selectedLabelColor = Color.White,
                                    containerColor = FireColors.Primary.copy(alpha = 0.08f),
                                    labelColor = FireColors.OnSurfaceVariant
                                )
                            )
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("Cancelar", color = FireColors.OnSurfaceVariant) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { if (titulo.isNotBlank()) onConfirm(titulo.trim(), descricao.trim().ifBlank { null }, selectedDate, inicio.trim().ifBlank { null }, fim.trim().ifBlank { null }, tipo) },
                        enabled = titulo.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = FireColors.Primary), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(6.dp))
                        Text(if (initial == null) "Salvar" else "Atualizar")
                    }
                }
            }
        }
    }
}
