@file:OptIn(ExperimentalFoundationApi::class)
package com.example.firenotes.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import com.example.firenotes.domain.model.SubtarefaInput
import com.example.firenotes.data.local.entities.RoomSubtarefa
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.activity.compose.BackHandler
import android.app.Activity
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.firenotes.domain.model.NaturezaOcorrencia
import com.example.firenotes.domain.model.Ocorrencia
import com.example.firenotes.ui.designsystem.colors.FireColors
import com.example.firenotes.ui.designsystem.typography.FireTypography
import com.example.firenotes.data.service.ProntidaoService
import com.example.firenotes.data.service.ProntidaoService.ProntidaoInfo
import com.example.firenotes.data.local.entities.RoomTarefa
import com.example.firenotes.data.local.entities.RoomTarefaComSubtarefas
import com.example.firenotes.data.local.entities.RoomEventoAgenda
import com.example.firenotes.data.local.entities.RoomEventoComLembretes
import com.example.firenotes.data.local.entities.RoomProntidaoDia
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.firenotes.ui.screens.calendar.CalendarViewModel
import com.example.firenotes.ui.screens.calendar.NotificationCenterViewModel
import com.example.firenotes.ui.screens.calendar.NotificationBottomSheet
import com.example.firenotes.ui.screens.calendar.DailySummaryPopup
import com.example.firenotes.domain.calendar.ScaleEngine
import android.graphics.Color.parseColor


// ============================================
// ENUMERADORES
// ============================================
enum class CalendarViewType {
    MONTH, WEEK, DAY
}

enum class TabType {
    OCORRENCIAS, AGENDA, CHECKS
}

data class ReportFilters(
    val dateRange: DateRange = DateRange.TODAY,
    val natureType: NaturezaOcorrencia? = null,
    val status: ReportStatus = ReportStatus.ALL,
    val searchQuery: String = ""
)

enum class DateRange {
    TODAY, THIS_WEEK, THIS_MONTH, CUSTOM
}

enum class ReportStatus {
    ALL, OPEN, CLOSED
}

enum class Prioridade {
    ALTA, MEDIA, BAIXA
}

// ============================================
// SISTEMA DE CARDS - ESTILO ELEVATED CARD
// ============================================

enum class CardVariant {
    ELEVATED,
    OUTLINED,
    FILLED,
    GRADIENT
}

@Composable
fun ElevatedCard(
    title: String? = null,
    subtitle: String? = null,
    caption: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = FireColors.Primary,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    variant: CardVariant = CardVariant.ELEVATED,
    modifier: Modifier = Modifier,
    content: (@Composable ColumnScope.() -> Unit)? = null,
    badge: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    val style = when (variant) {
        CardVariant.ELEVATED -> CardStyle(
            borderColor = Color.Transparent,
            borderWidth = 0.dp,
            backgroundColor = FireColors.Surface,
            cornerRadius = 12.dp,
            shadowElevation = 1.dp,
            contentPadding = PaddingValues(16.dp)
        )
        CardVariant.OUTLINED -> CardStyle(
            borderColor = FireColors.Primary.copy(alpha = 0.3f),
            borderWidth = 1.5.dp,
            backgroundColor = FireColors.Surface.copy(alpha = 0.05f),
            cornerRadius = 16.dp,
            shadowElevation = 0.dp,
            contentPadding = PaddingValues(16.dp)
        )
        CardVariant.FILLED -> CardStyle(
            borderColor = Color.Transparent,
            borderWidth = 0.dp,
            backgroundColor = FireColors.Surface.copy(alpha = 0.15f),
            cornerRadius = 16.dp,
            shadowElevation = 0.dp,
            contentPadding = PaddingValues(16.dp)
        )
        CardVariant.GRADIENT -> CardStyle(
            borderColor = FireColors.Primary.copy(alpha = 0.15f),
            borderWidth = 0.5.dp,
            backgroundColor = Color.Transparent,
            cornerRadius = 16.dp,
            shadowElevation = 2.dp,
            contentPadding = PaddingValues(16.dp)
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = style.shadowElevation,
                shape = RoundedCornerShape(style.cornerRadius),
                clip = false
            )
            .border(
                width = style.borderWidth,
                color = style.borderColor,
                shape = RoundedCornerShape(style.cornerRadius)
            )
            .background(
                color = style.backgroundColor,
                shape = RoundedCornerShape(style.cornerRadius)
            ),
        shape = RoundedCornerShape(style.cornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = when (variant) {
                CardVariant.GRADIENT -> Color.Transparent
                else -> style.backgroundColor
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (variant == CardVariant.ELEVATED) 4.dp else 0.dp
        )
    ) {
        if (variant == CardVariant.GRADIENT) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                FireColors.Primary.copy(alpha = 0.06f),
                                FireColors.Surface.copy(alpha = 0.03f)
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(style.contentPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (title != null || icon != null || trailing != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (icon != null) {
                            Surface(
                                shape = CircleShape,
                                color = iconTint.copy(alpha = 0.12f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = iconTint,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        if (title != null) {
                            Text(
                                text = title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = FireColors.OnBackground,
                                letterSpacing = 0.3.sp
                            )
                        }
                    }

                    if (badge != null) {
                        badge()
                    }
                    if (trailing != null) {
                        trailing()
                    }
                }
            }

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = FireColors.OnSurfaceVariant,
                    modifier = Modifier.padding(start = if (icon != null) 52.dp else 0.dp)
                )
            }

            if (caption != null) {
                Text(
                    text = caption,
                    fontSize = 12.sp,
                    color = FireColors.OnSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = if (icon != null) 52.dp else 0.dp)
                )
            }

            content?.invoke(this)

            if (actionText != null && onAction != null && content != null) {
                HorizontalDivider(
                    color = FireColors.OnSurfaceVariant.copy(alpha = 0.08f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (actionText != null && onAction != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onAction,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = FireColors.Primary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = actionText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ============================================
// CARD DE OCORRÊNCIA
// ============================================

@Composable
fun ElevatedOccurrenceCard(
    ocorrencia: Ocorrencia,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val natureColor = when (ocorrencia.natureza) {
        NaturezaOcorrencia.INCENDIO -> FireColors.Primary
        NaturezaOcorrencia.SALVAMENTO -> Color(0xFF4CAF50)
        NaturezaOcorrencia.ACIDENTE_TRANSITO -> Color(0xFFFF9800)
        NaturezaOcorrencia.QUEDA -> Color(0xFF8B5A2B)
        NaturezaOcorrencia.PESSOAL -> Color(0xFF9C27B0)
        NaturezaOcorrencia.INDEFINIDA -> Color.Gray
    }

    val prontidao = remember(ocorrencia.dataHora) {
        ProntidaoService.getProntidaoForInstant(ocorrencia.dataHora)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(12.dp),
                clip = false
            )
            .border(
                width = 0.5.dp,
                color = natureColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .background(
                color = FireColors.Surface.copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = FireColors.Surface.copy(alpha = 0.85f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .background(natureColor, RoundedCornerShape(4.dp))
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = ocorrencia.natureza.descricao,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = FireColors.OnBackground
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = prontidao.cor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = prontidao.nome.replace("Prontidão ", ""),
                                fontSize = 9.sp,
                                color = prontidao.cor,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = ocorrencia.protocolo,
                            fontSize = 11.sp,
                            color = FireColors.OnSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = FireColors.OnSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = ocorrencia.cidade ?: "Local não informado",
                        fontSize = 12.sp,
                        color = FireColors.OnSurfaceVariant
                    )
                    Text(
                        text = "•",
                        color = FireColors.OnSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Icon(
                        Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = FireColors.OnSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = ocorrencia.dataHora.atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                        fontSize = 12.sp,
                        color = FireColors.OnSurfaceVariant
                    )
                }
            }

            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = FireColors.OnSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ============================================
// CARD DE TAREFA
// ============================================

@Composable
fun ElevatedTaskCard(
    task: RoomTarefaComSubtarefas,
    onToggle: () -> Unit,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val priorityColor = when (runCatching { Prioridade.valueOf(task.tarefa.prioridade) }
        .getOrDefault(Prioridade.MEDIA)) {
        Prioridade.ALTA -> Color(0xFFEF4444)
        Prioridade.MEDIA -> Color(0xFFF59E0B)
        Prioridade.BAIXA -> Color(0xFF22C55E)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 1.dp,
                shape = RoundedCornerShape(12.dp),
                clip = false
            )
            .border(
                width = 0.5.dp,
                color = if (task.tarefa.concluida)
                    FireColors.OnSurfaceVariant.copy(alpha = 0.08f)
                else
                    priorityColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .background(
                color = if (task.tarefa.concluida)
                    FireColors.Surface.copy(alpha = 0.5f)
                else
                    FireColors.Surface.copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.tarefa.concluida)
                FireColors.Surface.copy(alpha = 0.5f)
            else
                FireColors.Surface.copy(alpha = 0.85f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(28.dp)
                    .background(priorityColor, RoundedCornerShape(2.dp))
            )

            Checkbox(
                checked = task.tarefa.concluida,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF22C55E),
                    uncheckedColor = FireColors.OnSurfaceVariant.copy(alpha = 0.3f)
                ),
                modifier = Modifier.size(20.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.tarefa.titulo,
                    fontSize = 13.sp,
                    fontWeight = if (task.tarefa.concluida) FontWeight.Normal else FontWeight.Medium,
                    color = if (task.tarefa.concluida)
                        FireColors.OnSurfaceVariant.copy(alpha = 0.5f)
                    else
                        FireColors.OnBackground,
                    textDecoration = if (task.tarefa.concluida) TextDecoration.LineThrough else null
                )
                if (!task.tarefa.descricao.isNullOrBlank()) {
                    Text(
                        text = task.tarefa.descricao,
                        fontSize = 10.sp,
                        color = FireColors.OnSurfaceVariant.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = priorityColor.copy(alpha = 0.1f)
            ) {
                Text(
                    text = task.tarefa.prioridade,
                    fontSize = 8.sp,
                    color = priorityColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

// ============================================
// CARD DE EVENTO
// ============================================

@Composable
fun ElevatedEventCard(
    event: RoomEventoComLembretes,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tipo = runCatching {
        com.example.firenotes.domain.model.TipoEvento.valueOf(event.evento.tipo ?: "OUTRO")
    }.getOrNull()

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(12.dp),
                clip = false
            )
            .border(
                width = 0.5.dp,
                color = FireColors.Primary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                color = FireColors.Surface.copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = FireColors.Surface.copy(alpha = 0.85f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = FireColors.Primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Icon(
                            imageVector = tipo?.icon ?: androidx.compose.material.icons.Icons.Default.Event,
                            contentDescription = "Ícone do evento",
                            tint = FireColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = event.evento.titulo,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = FireColors.OnBackground
                    )
                    if (!event.evento.descricao.isNullOrBlank()) {
                        Text(
                            text = event.evento.descricao,
                            fontSize = 11.sp,
                            color = FireColors.OnSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "🕒 ${event.evento.horaInicio ?: "08:00"} - ${event.evento.horaFim ?: "Retorno"}",
                        fontSize = 10.sp,
                        color = FireColors.Primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = FireColors.Primary.copy(alpha = 0.08f)
            ) {
                Text(
                    text = tipo?.name?.take(1) ?: "E",
                    fontSize = 10.sp,
                    color = FireColors.Primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(6.dp)
                )
            }
        }
    }
}

// ============================================
// SCREEN PRINCIPAL
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToWizard: () -> Unit,
    onNavigateToDetails: (id: String) -> Unit,

    onNavigateToConsult: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showExitDialog by remember { mutableStateOf(false) }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Sair do Aplicativo") },
            text = { Text("Deseja realmente sair do FireNotes?") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    (context as? Activity)?.finishAffinity() // Or just finish() depending on behavior
                }) {
                    Text("Sair", color = FireColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val selectedDate by viewModel.selectedDate.collectAsState()
    val currentMonthDate by viewModel.currentMonth.collectAsState()
    val allTarefas by viewModel.allTarefas.collectAsState()
    val allEventos by viewModel.allEventos.collectAsState()
    val allProntidoes by viewModel.allProntidoes.collectAsState()
    val availableEscalas by viewModel.availableEscalas.collectAsState()
    val selectedEscalaFilter by viewModel.selectedEscalaFilter.collectAsState()
    val currentCity by viewModel.currentCity.collectAsState()
    val hasDismissedAlertsThisSession by viewModel.hasDismissedAlertsThisSession.collectAsState()

    // ViewModels do Calendário e Notificações (V8)
    val calendarViewModel: CalendarViewModel = hiltViewModel()
    val notificationViewModel: NotificationCenterViewModel = hiltViewModel()
    
    val calendarUiState by calendarViewModel.uiState.collectAsState()
    val notificationUiState by notificationViewModel.uiState.collectAsState()

    var showNotificationSheet by remember { mutableStateOf(false) }
    var showDailySummaryPopup by remember { mutableStateOf(false) }

    var showFabOptionsSheet by remember { mutableStateOf(false) }
    var showEventTaskFormSheet by remember { mutableStateOf(false) }
    var formIsTaskMode by remember { mutableStateOf(false) }
    var selectedEventoToEdit by remember { mutableStateOf<RoomEventoAgenda?>(null) }
    var selectedTarefaToEdit by remember { mutableStateOf<RoomTarefa?>(null) }

    // LaunchedEffect aguarda settingsLoaded=true (dado real do DB) antes de verificar wizard
    // Isso corrige o bug onde o wizard era exibido a cada abertura por race condition
    LaunchedEffect(calendarUiState.settingsLoaded, calendarUiState.settings) {
        if (!calendarUiState.settingsLoaded) return@LaunchedEffect // aguarda DB

        val todayStr = LocalDate.now().toString()
        if (calendarUiState.settings.mostrarPopupInicial && calendarUiState.settings.popupExibidoHoje != todayStr) {
            showDailySummaryPopup = true
        }
        // Redireciona ao wizard SOMENTE se dado real do banco indicar não configurado
        if (!calendarUiState.settings.calendarioConfigurado) {
            onNavigateToWizard()
        }
    }

    var calendarViewType by remember { mutableStateOf(CalendarViewType.MONTH) }
    var itemToView by remember { mutableStateOf<Any?>(null) }

    BackHandler {
        if (itemToView != null) {
            itemToView = null
        } else if (calendarViewType != CalendarViewType.MONTH) {
            calendarViewType = CalendarViewType.MONTH
        } else {
            showExitDialog = true
        }
    }
    var selectedTab by remember { mutableStateOf(TabType.OCORRENCIAS) }
    var showReportDialog by remember { mutableStateOf(false) }
    var filters by remember { mutableStateOf(ReportFilters()) }

    var currentHour by remember { mutableStateOf(LocalTime.now(ZoneId.systemDefault()).hour) }
    var currentMinute by remember { mutableStateOf(LocalTime.now(ZoneId.systemDefault()).minute) }

    LaunchedEffect(Unit) {
        while (true) {
            val now = LocalTime.now(ZoneId.systemDefault())
            currentHour = now.hour
            currentMinute = now.minute
            kotlinx.coroutines.delay(60_000L)
        }
    }

    var hasShownAlertsThisSession by rememberSaveable { mutableStateOf(false) }
    var showAlertsDialog by remember { mutableStateOf(false) }
    val today = remember { LocalDate.now() }

    val activeEventAlerts = remember(allEventos, currentHour, currentMinute) {
        val nowTime = LocalTime.now(ZoneId.systemDefault())
        allEventos.filter { eventComLembretes ->
            val event = eventComLembretes.evento
            val isToday = event.data == today.toString()
            val hasTime = !event.horaInicio.isNullOrBlank()
            if (isToday && hasTime) {
                val evTime = runCatching { LocalTime.parse(event.horaInicio) }.getOrNull()
                evTime != null && nowTime.isBefore(evTime.plusHours(1))
            } else false
        }
    }

    val activeTaskAlerts = remember(allTarefas, currentHour, currentMinute) {
        val nowTime = LocalTime.now(ZoneId.systemDefault())
        allTarefas.filter { taskComSubtarefas ->
            val task = taskComSubtarefas.tarefa
            val isToday = task.data == today.toString()
            val hasTime = !task.hora.isNullOrBlank()
            val notDone = !task.concluida
            if (isToday && hasTime && notDone) {
                val tkTime = runCatching { LocalTime.parse(task.hora) }.getOrNull()
                tkTime != null && nowTime.isBefore(tkTime.plusHours(1))
            } else false
        }
    }

    LaunchedEffect(activeEventAlerts, activeTaskAlerts, hasDismissedAlertsThisSession) {
        if (!hasDismissedAlertsThisSession && !hasShownAlertsThisSession && (activeEventAlerts.isNotEmpty() || activeTaskAlerts.isNotEmpty())) {
            showAlertsDialog = true
            hasShownAlertsThisSession = true
        }
    }



    val userName = remember(context) { getDeviceOwnerName(context) }
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    val greeting = remember(currentHour) {
        when {
            currentHour < 12 -> "Bom dia"
            currentHour < 18 -> "Boa tarde"
            else -> "Boa noite"
        }
    }

    val todayDate = remember(currentHour, currentMinute) {
        LocalDate.now(ZoneId.systemDefault()).format(
            DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("pt-BR"))
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val pulseAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // States for pre-filling Lembretes and Subtarefas
    var loadedLembretesMinutos by remember { mutableStateOf<List<Int>>(emptyList()) }
    var loadedSubtarefas by remember { mutableStateOf<List<SubtarefaInput>>(emptyList()) }

    LaunchedEffect(selectedEventoToEdit, selectedTarefaToEdit) {
        val refId = selectedEventoToEdit?.id ?: selectedTarefaToEdit?.id
        if (refId != null) {
            val lembretes = viewModel.getLembretesByReferencia(refId)
            loadedLembretesMinutos = lembretes
            
            if (selectedTarefaToEdit != null) {
                val dbSubtarefas = viewModel.getSubtarefasByTarefa(refId)
                
                fun getSubtarefaLevel(sub: RoomSubtarefa, allSubs: List<RoomSubtarefa>): Int {
                    var level = 0
                    var parentId = sub.parentId
                    while (parentId != null) {
                        level++
                        val parent = allSubs.find { it.id == parentId }
                        parentId = parent?.parentId
                    }
                    return level
                }

                fun buildTreeSortedList(subtarefas: List<RoomSubtarefa>): List<RoomSubtarefa> {
                    val result = mutableListOf<RoomSubtarefa>()
                    val roots = subtarefas.filter { it.parentId == null }
                    
                    fun dfs(node: RoomSubtarefa) {
                        result.add(node)
                        val children = subtarefas.filter { it.parentId == node.id }
                        children.forEach { child ->
                            dfs(child)
                        }
                    }
                    
                    roots.forEach { root ->
                        dfs(root)
                    }
                    
                    val processedIds = result.map { it.id }.toSet()
                    subtarefas.filter { it.id !in processedIds }.forEach { orphaned ->
                        result.add(orphaned)
                    }
                    
                    return result
                }

                val sortedRoomSubs = buildTreeSortedList(dbSubtarefas)
                loadedSubtarefas = sortedRoomSubs.map { sub ->
                    SubtarefaInput(
                        id = sub.id,
                        titulo = sub.titulo,
                        level = getSubtarefaLevel(sub, dbSubtarefas),
                        concluida = sub.concluida
                    )
                }
            } else {
                loadedSubtarefas = emptyList()
            }
        } else {
            loadedLembretesMinutos = emptyList()
            loadedSubtarefas = emptyList()
        }
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                color = FireColors.Background.copy(alpha = 0.95f),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(
                                id = com.example.firenotes.R.mipmap.ic_launcher_round
                            ),
                            contentDescription = "FireNotes",
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                        )
                        Text(
                            text = "Fire Notes",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            style = androidx.compose.ui.text.TextStyle(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        FireColors.Primary,
                                        Color(0xFFFF7043)
                                    )
                                )
                            )
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Sino de Notificações com Badge de Quantidade Não Lidas
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { showNotificationSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notificações",
                                tint = FireColors.OnSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            if (notificationUiState.unreadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 2.dp, end = 2.dp)
                                        .size(16.dp)
                                        .background(FireColors.Error, shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = notificationUiState.unreadCount.toString(),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = onNavigateToConsult,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = "Consultas",
                                tint = FireColors.OnSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.refreshAll() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = FireColors.Primary
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.Refresh,
                                    contentDescription = "Atualizar",
                                    tint = FireColors.OnSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showFabOptionsSheet = true },
                containerColor = FireColors.Primary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Novo Evento ou Tarefa")
            }
        },
        containerColor = FireColors.Background,
        modifier = modifier
    ) { innerPadding ->
        // Bottom Sheet de Notificações
        if (showNotificationSheet) {
            NotificationBottomSheet(
                viewModel = notificationViewModel,
                onDismiss = { showNotificationSheet = false },
                onNotificationClick = { notif -> 
                    showNotificationSheet = false
                    itemToView = notif 
                }
            )
        }

        // Bottom Sheet de Seleção (Novo Evento / Nova Tarefa)
        if (showFabOptionsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFabOptionsSheet = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "O que você deseja cadastrar?",
                        style = FireTypography.Title,
                        fontWeight = FontWeight.Bold
                    )
                    Card(
                        onClick = {
                            showFabOptionsSheet = false
                            formIsTaskMode = false
                            showEventTaskFormSheet = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Event, contentDescription = null, tint = FireColors.Primary, modifier = Modifier.size(28.dp))
                            Column {
                                Text("📅 Novo Evento", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Compromissos, reuniões e eventos com horário", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Card(
                        onClick = {
                            showFabOptionsSheet = false
                            formIsTaskMode = true
                            showEventTaskFormSheet = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Checklist, contentDescription = null, tint = FireColors.Secondary, modifier = Modifier.size(28.dp))
                            Column {
                                Text("📋 Nova Tarefa", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Atividades diárias, listas e subtarefas pendentes", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Bottom Sheet de Formulário Unificado (Eventos & Tarefas)
        if (showEventTaskFormSheet) {
            com.example.firenotes.ui.screens.calendar.EventTaskFormBottomSheet(
                initialIsTask = selectedTarefaToEdit != null || formIsTaskMode,
                initialDate = selectedEventoToEdit?.let { runCatching { LocalDate.parse(it.data) }.getOrNull() } 
                    ?: selectedTarefaToEdit?.let { runCatching { LocalDate.parse(it.data) }.getOrNull() } 
                    ?: selectedDate,
                initialTitulo = selectedEventoToEdit?.titulo ?: selectedTarefaToEdit?.titulo ?: "",
                initialDescricao = selectedEventoToEdit?.descricao ?: selectedTarefaToEdit?.descricao ?: "",
                initialLocal = selectedEventoToEdit?.local ?: "",
                initialHoraInicio = selectedEventoToEdit?.horaInicio?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: selectedTarefaToEdit?.hora?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
                initialHoraFim = selectedEventoToEdit?.horaFim?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
                initialLembretesMinutos = loadedLembretesMinutos,
                initialSubtarefas = loadedSubtarefas,
                availableEscalas = viewModel.availableEscalas.value,
                initialEscalaId = selectedEventoToEdit?.escalaId ?: selectedTarefaToEdit?.escalaId,
                initialCorHex = selectedEventoToEdit?.cor ?: selectedTarefaToEdit?.cor,
                onDismiss = { 
                    showEventTaskFormSheet = false 
                    selectedEventoToEdit = null
                    selectedTarefaToEdit = null
                },
                onSaveEvento = { titulo, data, hIni, hFim, desc, corHex, lembreteMin, local, escalaId ->
                    val d = runCatching { LocalDate.parse(data) }.getOrDefault(selectedDate)
                    if (selectedEventoToEdit != null) {
                        viewModel.updateEvento(
                            selectedEventoToEdit!!.copy(
                                titulo = titulo,
                                data = data,
                                horaInicio = hIni,
                                horaFim = hFim,
                                descricao = desc,
                                local = local,
                                escalaId = escalaId,
                                cor = corHex
                            ),
                            lembretesMinutos = lembreteMin
                        )
                    } else {
                        viewModel.addEvento(
                            titulo = titulo,
                            descricao = desc,
                            data = d,
                            horaInicio = hIni,
                            horaFim = hFim,
                            local = local,
                            lembretesMinutos = lembreteMin,
                            escalaId = escalaId,
                            corHex = corHex
                        )
                    }
                },
                onSaveTarefa = { titulo, data, desc, corHex, lembreteMin, subtarefas, escalaId ->
                    val d = runCatching { LocalDate.parse(data) }.getOrDefault(selectedDate)
                    if (selectedTarefaToEdit != null) {
                        viewModel.updateTarefa(
                            tarefa = selectedTarefaToEdit!!.copy(
                                titulo = titulo,
                                data = data,
                                descricao = desc,
                                escalaId = escalaId,
                                cor = corHex
                            ),
                            lembretesMinutos = lembreteMin,
                            subtarefas = subtarefas
                        )
                    } else {
                        viewModel.addTarefa(
                            titulo = titulo,
                            descricao = desc,
                            data = d,
                            categoria = "Geral",
                            lembretesMinutos = lembreteMin,
                            subtarefas = subtarefas,
                            escalaId = escalaId,
                            corHex = corHex
                        )
                    }
                }
            )
        }

        // Popup de Resumo Diário
        if (showDailySummaryPopup) {
            val todayEvs = allEventos.filter { it.evento.data == LocalDate.now().toString() }.map {
                com.example.firenotes.domain.model.CalendarEvento(
                    id = it.evento.id,
                    titulo = it.evento.titulo,
                    descricao = it.evento.descricao ?: "",
                    data = it.evento.data,
                    hora = it.evento.horaInicio,
                    local = it.evento.local,
                    categoria = com.example.firenotes.domain.model.CategoriaEvento.PERSONALIZADO,
                    cor = it.evento.cor,
                    recorrencia = com.example.firenotes.domain.model.RecorrenciaTipo.NUNCA,
                    lembreteMinutos = 0,
                    escalaId = it.evento.escalaId
                )
            }
            val todayTks = allTarefas.filter { it.tarefa.data == LocalDate.now().toString() }.map {
                com.example.firenotes.domain.model.CalendarTarefa(
                    id = it.tarefa.id,
                    titulo = it.tarefa.titulo,
                    descricao = it.tarefa.descricao ?: "",
                    data = it.tarefa.data,
                    hora = it.tarefa.hora,
                    prioridade = com.example.firenotes.domain.model.PrioridadeTarefa.MEDIA,
                    status = if (it.tarefa.concluida) com.example.firenotes.domain.model.StatusTarefa.CONCLUIDA else com.example.firenotes.domain.model.StatusTarefa.PENDENTE,
                    categoria = it.tarefa.categoria,
                    responsavel = null,
                    escalaId = it.tarefa.escalaId
                )
            }

            DailySummaryPopup(
                // Usa activeTeamsRightNow (time-aware) para mostrar quem está de serviço AGORA
                activeTeams = calendarUiState.activeTeamsRightNow.ifEmpty {
                    calendarUiState.activeTeamsOnSelectedDate
                },
                todayEvents = todayEvs,
                todayTasks = todayTks,
                consecutiveDays = calendarUiState.consecutiveWorkDays,
                onDismiss = { showDailySummaryPopup = false },
                onDontShowAgainToday = { calendarViewModel.setPopupExibido() }
            )
        }

        itemToView?.let { item ->
            val title: String
            val subtitle: String
            val desc: String?
            val details = mutableListOf<Pair<String, String>>()
            var onEditAction: (() -> Unit)? = null
            var onDeleteAction: (() -> Unit)? = null

            when (item) {
                is RoomEventoComLembretes -> {
                    val ev = item.evento
                    title = ev.titulo
                    subtitle = "Evento da Agenda"
                    desc = ev.descricao
                    details.add("Data" to ev.data)
                    details.add("Início" to (ev.horaInicio ?: "O dia todo"))
                    ev.horaFim?.let { details.add("Fim" to it) }
                    ev.local?.let { details.add("Local" to it) }
                    onEditAction = {
                        itemToView = null
                        selectedEventoToEdit = ev
                        showEventTaskFormSheet = true
                    }
                    onDeleteAction = {
                        viewModel.deleteEvento(ev.id)
                        itemToView = null
                    }
                }
                is RoomTarefaComSubtarefas -> {
                    val tk = item.tarefa
                    title = tk.titulo
                    subtitle = "Tarefa Operacional"
                    desc = tk.descricao
                    details.add("Data" to tk.data)
                    tk.hora?.let { details.add("Horário" to it) }
                    details.add("Categoria" to tk.categoria)
                    details.add("Prioridade" to tk.prioridade)
                    details.add("Status" to if (tk.concluida) "Concluída" else "Pendente")
                    onEditAction = {
                        itemToView = null
                        selectedTarefaToEdit = tk
                        showEventTaskFormSheet = true
                    }
                    onDeleteAction = {
                        viewModel.deleteTarefa(tk.id)
                        itemToView = null
                    }
                }
                is com.example.firenotes.domain.model.CalendarNotificacao -> {
                    title = item.titulo
                    subtitle = "Notificação - ${item.categoria.name}"
                    desc = item.descricao
                    details.add("Data" to item.data)
                    details.add("Hora" to item.hora)
                    details.add("Origem" to item.origem)
                    details.add("Status" to if (item.lida) "Lida" else "Não Lida")
                    // Notifications don't have an edit/delete action directly mapped here for now, they are read-only
                }
                else -> {
                    title = "Desconhecido"
                    subtitle = ""
                    desc = null
                }
            }

            val subtarefas = if (itemToView is RoomTarefaComSubtarefas) {
                val taskId = (itemToView as RoomTarefaComSubtarefas).tarefa.id
                allTarefas.find { it.tarefa.id == taskId }?.subtarefas ?: emptyList()
            } else {
                emptyList()
            }

            com.example.firenotes.ui.screens.calendar.ViewDetailsBottomSheet(
                title = title,
                subtitle = subtitle,
                description = desc,
                details = details,
                canEdit = onEditAction != null,
                subtarefas = subtarefas,
                onToggleSubtarefa = { viewModel.toggleSubtarefa(it) },
                onEditSubtarefaTitle = { sub, newTitle -> viewModel.updateSubtarefaTitle(sub, newTitle) },
                onDeleteSubtarefa = { viewModel.deleteSubtarefa(it) },
                onEdit = { onEditAction?.invoke() },
                onDelete = {
                    com.example.firenotes.util.BiometricHelper.authenticate(
                        context = context,
                        onSuccess = { onDeleteAction?.invoke() },
                        onError = { err -> android.widget.Toast.makeText(context, "Erro: $err", android.widget.Toast.LENGTH_SHORT).show() }
                    )
                },
                onDismiss = { itemToView = null }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(FireColors.Background)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                FireColors.Primary.copy(alpha = 0.03f + pulseAnim * 0.02f),
                                FireColors.Background
                            ),
                            startY = 0f,
                            endY = 0.6f
                        )
                    )
            )

            val occurrencesMap = remember(uiState) {
                val successState = uiState as? HomeUiState.Success
                successState?.occurrences?.groupBy {
                    it.dataHora.atZone(ZoneId.systemDefault()).toLocalDate()
                } ?: emptyMap()
            }

            val tasksMap = remember(allTarefas, selectedEscalaFilter) {
                allTarefas.filter {
                    it.tarefa.escalaId == null || selectedEscalaFilter == null || it.tarefa.escalaId == selectedEscalaFilter
                }.groupBy { LocalDate.parse(it.tarefa.data) }
            }

            val eventsMap = remember(allEventos, selectedEscalaFilter) {
                allEventos.filter {
                    it.evento.escalaId == null || selectedEscalaFilter == null || it.evento.escalaId == selectedEscalaFilter
                }.groupBy { LocalDate.parse(it.evento.data) }
            }

            val filteredOccurrences = remember(uiState, filters) {
                val successState = uiState as? HomeUiState.Success
                successState?.occurrences?.filter { ocorrencia ->
                    var matches = true

                    if (filters.natureType != null) {
                        matches = matches && ocorrencia.natureza == filters.natureType
                    }

                    if (filters.searchQuery.isNotEmpty()) {
                        val query = filters.searchQuery.lowercase()
                        matches = matches && (
                                ocorrencia.protocolo.lowercase().contains(query) ||
                                        ocorrencia.natureza.descricao.lowercase().contains(query) ||
                                        ocorrencia.cidade?.lowercase()?.contains(query) == true
                                )
                    }

                    matches
                } ?: emptyList()
            }

            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = FireColors.Primary,
                                modifier = Modifier.size(48.dp),
                                strokeWidth = 4.dp
                            )
                            Text(
                                text = "Carregando centro operacional...",
                                style = FireTypography.BodyMedium,
                                color = FireColors.OnSurfaceVariant
                            )
                        }
                    }
                }

                is HomeUiState.Error -> {
                    ElevatedCard(
                        title = "Ops! Algo deu errado",
                        subtitle = state.message,
                        icon = Icons.Outlined.Warning,
                        iconTint = FireColors.Error,
                        actionText = "Tentar Novamente",
                        onAction = { viewModel.loadOccurrences() },
                        variant = CardVariant.ELEVATED,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                    )
                }

                is HomeUiState.Success -> {
                    if (showReportDialog) {
                        ReportDialog(
                            filters = filters,
                            onFiltersChanged = { filters = it },
                            onDismiss = { showReportDialog = false },
                            occurrences = filteredOccurrences,
                            onNavigateToDetails = { id ->
                                showReportDialog = false
                                onNavigateToDetails(id)
                            }
                        )
                    }

                    if (showAlertsDialog && !hasDismissedAlertsThisSession) {
                        HomeAlertsDialog(
                            activeEvents = activeEventAlerts,
                            activeTasks = activeTaskAlerts,
                            onDismiss = { showAlertsDialog = false },
                            onDismissPermanently = {
                                viewModel.dismissAlertsPermanently()
                                showAlertsDialog = false
                            }
                        )
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(
                            horizontal = if (isTablet) 32.dp else 16.dp,
                            vertical = 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Header com Saudação e Data
                        item {
                            EnhancedWelcomeHeader(
                                greeting = greeting,
                                userName = userName,
                                city = currentCity,
                                todayDate = todayDate,
                                activeTeamsRightNow = calendarUiState.activeTeamsRightNow
                            )
                        }


                        // Seletor de Vista + Filtros de Camadas
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Seletor Mês/Semana/Dia
                                CalendarViewSelector(
                                    currentType = calendarViewType,
                                    onTypeSelected = { calendarViewType = it }
                                )
                                // Filtros de Camadas
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box {
                                        var expanded by remember { mutableStateOf(false) }
                                        val currentFilter = selectedEscalaFilter
                                        val filterName = when (currentFilter) {
                                            null -> "Todas Escalas"
                                            "NONE" -> "Nenhuma"
                                            else -> availableEscalas.find { it.id == currentFilter }?.nome ?: "Escala..."
                                        }

                                        FilterChip(
                                            selected = currentFilter != "NONE",
                                            onClick = { expanded = true },
                                            label = { Text(filterName, fontSize = 11.sp) },
                                            trailingIcon = { Icon(androidx.compose.material.icons.Icons.Filled.ArrowDropDown, contentDescription = "Selecionar Escala") }
                                        )

                                        DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Todas Escalas") },
                                                onClick = { viewModel.setEscalaFilter(null); expanded = false },
                                                leadingIcon = { if (currentFilter == null) Icon(Icons.Default.Check, contentDescription = null) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Nenhuma (Ocultar)") },
                                                onClick = { viewModel.setEscalaFilter("NONE"); expanded = false },
                                                leadingIcon = { if (currentFilter == "NONE") Icon(Icons.Default.Check, contentDescription = null) }
                                            )
                                            if (availableEscalas.isNotEmpty()) {
                                                HorizontalDivider()
                                                availableEscalas.forEach { esc ->
                                                    DropdownMenuItem(
                                                        text = { Text(esc.nome) },
                                                        onClick = { viewModel.setEscalaFilter(esc.id); expanded = false },
                                                        leadingIcon = { if (currentFilter == esc.id) Icon(Icons.Default.Check, contentDescription = null) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    FilterChip(
                                        selected = calendarUiState.activeLayers.contains("EVENTO"),
                                        onClick = { calendarViewModel.toggleLayer("EVENTO") },
                                        label = { Text("Eventos", fontSize = 11.sp) }
                                    )
                                    FilterChip(
                                        selected = calendarUiState.activeLayers.contains("TAREFA"),
                                        onClick = { calendarViewModel.toggleLayer("TAREFA") },
                                        label = { Text("Tarefas", fontSize = 11.sp) }
                                    )
                                    FilterChip(
                                        selected = calendarUiState.activeLayers.contains("OCORRENCIA"),
                                        onClick = { calendarViewModel.toggleLayer("OCORRENCIA") },
                                        label = { Text("Ocorrências", fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        // Calendário
                        item {
                            AnimatedContent(
                                targetState = calendarViewType,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(200)) togetherWith
                                        fadeOut(animationSpec = tween(150))
                                },
                                label = "calendarViewAnimation"
                            ) { viewType ->
                                when (viewType) {
                                    CalendarViewType.MONTH -> {
                                        MonthlyCalendarView(
                                            currentMonth = currentMonthDate,
                                            selectedDate = selectedDate,
                                            allProntidoes = allProntidoes,
                                            occurrencesMap = occurrencesMap,
                                            tasksMap = tasksMap,
                                            eventsMap = eventsMap,
                                            scales = calendarUiState.scales,
                                            teams = calendarUiState.teams,
                                            activeLayers = calendarUiState.activeLayers,
                                            onDateSelected = { viewModel.selectDate(it) },
                                            onDateLongClick = { viewModel.selectDate(it); calendarViewType = CalendarViewType.DAY },
                                            onPrevMonth = { viewModel.previousMonth() },
                                            onNextMonth = { viewModel.nextMonth() },
                                            onEventClick = { ev -> itemToView = ev },
                                            onTaskClick = { t -> itemToView = t }
                                        )
                                    }
                                    CalendarViewType.WEEK -> {
                                        WeeklyCalendarView(
                                            selectedDate = selectedDate,
                                            allProntidoes = allProntidoes,
                                            occurrencesMap = occurrencesMap,
                                            tasksMap = tasksMap,
                                            eventsMap = eventsMap,
                                            scales = calendarUiState.scales,
                                            teams = calendarUiState.teams,
                                            activeLayers = calendarUiState.activeLayers,
                                            onDateSelected = { viewModel.selectDate(it); calendarViewType = CalendarViewType.DAY },
                                            onEventClick = { ev -> itemToView = ev },
                                            onTaskClick = { t -> itemToView = t }
                                        )
                                    }
                                    CalendarViewType.DAY -> {
                                        DayCalendarView(
                                            selectedDate = selectedDate,
                                            scales = calendarUiState.scales,
                                            teams = calendarUiState.teams,
                                            occurrences = occurrencesMap[selectedDate] ?: emptyList(),
                                            tasks = tasksMap[selectedDate] ?: emptyList(),
                                            events = eventsMap[selectedDate] ?: emptyList(),
                                            onEventClick = { ev -> itemToView = ev },
                                            onTaskClick = { t -> itemToView = t },
                                            onToggleTarefa = { viewModel.toggleTarefa(it) }
                                        )
                                    }
                                    else -> { /* THREE_DAYS / AGENDA — não implementado */ }
                                }
                            }
                        }

                        // Abas MD3 com listas
                        item {
                            OperationalTabsWithLists(
                                selectedDate = selectedDate,
                                occurrences = occurrencesMap[selectedDate] ?: emptyList(),
                                tasks = tasksMap[selectedDate] ?: emptyList(),
                                events = eventsMap[selectedDate] ?: emptyList(),
                                allProntidoes = allProntidoes,
                                selectedTab = selectedTab,
                                onTabSelected = { selectedTab = it },
                                onToggleTask = { task -> viewModel.toggleTarefa(task.tarefa) },
                                onNavigateToDetails = onNavigateToDetails,
                                onEventClick = { ev -> itemToView = ev },
                                onTaskClick = { t -> itemToView = t }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "•  Fire Notes Operational Center  •",
                                    fontSize = 11.sp,
                                    color = FireColors.OnSurfaceVariant.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// NOVO HEADER - SEM PREVISÃO DO TEMPO
// ============================================

@Composable
fun EnhancedWelcomeHeader(
    greeting: String,
    userName: String,
    city: String,
    todayDate: String,
    activeTeamsRightNow: Map<Int, List<com.example.firenotes.domain.model.EquipeConfig>> = emptyMap(),
    modifier: Modifier = Modifier
) {
    // Coleta todas as equipes ativas agora (todos os turnos)
    val allActiveTeams = remember(activeTeamsRightNow) {
        activeTeamsRightNow.values.flatten()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "$greeting, $userName 👋",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = FireColors.OnBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (city.isNotEmpty() && city != "...") "$city • $todayDate" else todayDate,
                fontSize = 13.sp,
                color = FireColors.OnSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Badge(s) de equipe(s) de serviço agora
        if (allActiveTeams.isEmpty()) {
            // Sem escala configurada: exibir badge neutro
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = FireColors.OnSurfaceVariant.copy(alpha = 0.08f),
                modifier = Modifier.padding(end = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(FireColors.OnSurfaceVariant.copy(alpha = 0.4f), CircleShape)
                    )
                    Text(
                        text = "Sem escala",
                        fontSize = 12.sp,
                        color = FireColors.OnSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            // Exibir chip por equipe ativa agora
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.End
            ) {
                allActiveTeams.take(2).forEach { equipe ->
                    val equipeColor = runCatching {
                        Color(parseColor(equipe.corFundo))
                    }.getOrDefault(FireColors.Primary)
                    val textColor = runCatching {
                        Color(parseColor(equipe.corTexto))
                    }.getOrDefault(Color.White)

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = equipeColor.copy(alpha = 0.15f),
                        modifier = Modifier.padding(end = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(equipeColor, CircleShape)
                            )
                            Text(
                                text = equipe.sigla.ifBlank { equipe.nome },
                                fontSize = 11.sp,
                                color = equipeColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                if (allActiveTeams.size > 2) {
                    Text(
                        text = "+${allActiveTeams.size - 2}",
                        fontSize = 10.sp,
                        color = FireColors.OnSurfaceVariant,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }
        }
    }
}

// ============================================
// NOVO COMPONENTE - ABAS MD3 COM LISTAS
// ============================================

@Composable
fun OperationalTabsWithLists(
    selectedDate: LocalDate,
    occurrences: List<Ocorrencia>,
    tasks: List<RoomTarefaComSubtarefas>,
    events: List<RoomEventoComLembretes>,
    allProntidoes: List<RoomProntidaoDia>,
    selectedTab: TabType,
    onTabSelected: (TabType) -> Unit,
    onToggleTask: (RoomTarefaComSubtarefas) -> Unit,
    onNavigateToDetails: (String) -> Unit,
    onEventClick: (RoomEventoComLembretes) -> Unit,
    onTaskClick: (RoomTarefaComSubtarefas) -> Unit
) {
    val formattedDate = selectedDate.format(
        DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("pt", "BR"))
    )
    val prontidaoColor = getProntidaoColorForDate(selectedDate, allProntidoes)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = FireColors.Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Linha com prontidão, data selecionada e botão ver agenda
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Círculo com a cor da prontidão
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(prontidaoColor, CircleShape)
                    )
                    Text(
                        text = formattedDate,
                        style = FireTypography.Title,
                        fontWeight = FontWeight.Bold,
                        color = FireColors.OnSurface
                    )
                }
            }

            HorizontalDivider(
                color = FireColors.OnSurfaceVariant.copy(alpha = 0.08f),
                thickness = 0.5.dp
            )

            // Abas MD3
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = Color.Transparent,
                contentColor = FireColors.Primary,
                edgePadding = 0.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                        height = 2.5.dp,
                        color = FireColors.Primary
                    )
                },
                divider = { }
            ) {
                TabType.values().forEachIndexed { index, tab ->
                    val selected = selectedTab == tab
                    Tab(
                        selected = selected,
                        onClick = { onTabSelected(tab) },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = when (tab) {
                                        TabType.OCORRENCIAS -> "🚑"
                                        TabType.AGENDA -> "📅"
                                        TabType.CHECKS -> "✅"
                                    },
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = when (tab) {
                                        TabType.OCORRENCIAS -> "Ocorrências"
                                        TabType.AGENDA -> "Agenda"
                                        TabType.CHECKS -> "Checks"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) FireColors.Primary else FireColors.OnSurfaceVariant
                                )
                                if (tab == TabType.OCORRENCIAS && occurrences.isNotEmpty()) {
                                    Surface(
                                        shape = CircleShape,
                                        color = FireColors.Primary.copy(alpha = 0.15f),
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = occurrences.size.toString(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = FireColors.Primary
                                            )
                                        }
                                    }
                                }
                                if (tab == TabType.CHECKS && tasks.isNotEmpty()) {
                                    val pending = tasks.count { !it.tarefa.concluida }
                                    if (pending > 0) {
                                        Surface(
                                            shape = CircleShape,
                                            color = FireColors.Primary.copy(alpha = 0.15f),
                                            modifier = Modifier.size(16.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = pending.toString(),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = FireColors.Primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Conteúdo da Tab
            when (selectedTab) {
                TabType.OCORRENCIAS -> {
                    OccurrencesList(
                        occurrences = occurrences,
                        onNavigateToDetails = onNavigateToDetails
                    )
                }
                TabType.AGENDA -> {
                    EventsList(
                        events = events,
                        onEventClick = onEventClick
                    )
                }
                TabType.CHECKS -> {
                    TasksList(
                        tasks = tasks,
                        onToggleTask = onToggleTask,
                        onTaskClick = onTaskClick
                    )
                }
            }
        }
    }
}

// ============================================
// LISTA DE OCORRÊNCIAS
// ============================================

@Composable
fun OccurrencesList(
    occurrences: List<Ocorrencia>,
    onNavigateToDetails: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (occurrences.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = FireColors.Success.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Nenhuma ocorrência hoje",
                        fontSize = 14.sp,
                        color = FireColors.OnSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(occurrences) { ocorrencia ->
                    ElevatedOccurrenceCard(
                        ocorrencia = ocorrencia,
                        onClick = { ocorrencia.id?.let(onNavigateToDetails) }
                    )
                }
            }
        }
    }
}

// ============================================
// LISTA DE EVENTOS
// ============================================

@Composable
fun EventsList(
    events: List<RoomEventoComLembretes>,
    onEventClick: (RoomEventoComLembretes) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (events.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = FireColors.Primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Nenhum evento agendado",
                        fontSize = 14.sp,
                        color = FireColors.OnSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(events) { event ->
                    ElevatedEventCard(
                        event = event,
                        onClick = { onEventClick(event) }
                    )
                }
            }
        }
    }
}

// ============================================
// LISTA DE TAREFAS
// ============================================

@Composable
fun TasksList(
    tasks: List<RoomTarefaComSubtarefas>,
    onToggleTask: (RoomTarefaComSubtarefas) -> Unit,
    onTaskClick: (RoomTarefaComSubtarefas) -> Unit
) {
    var filterPriority by remember { mutableStateOf<Prioridade?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header com filtro
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📋 ${tasks.size} tarefa${if (tasks.size > 1) "s" else ""}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = FireColors.OnBackground
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = FireColors.Success.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "${tasks.count { it.tarefa.concluida }}/${tasks.size}",
                        fontSize = 11.sp,
                        color = FireColors.Success,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            IconButton(
                onClick = {
                    filterPriority = when(filterPriority) {
                        null -> Prioridade.ALTA
                        Prioridade.ALTA -> Prioridade.MEDIA
                        Prioridade.MEDIA -> Prioridade.BAIXA
                        Prioridade.BAIXA -> null
                    }
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Outlined.FilterList,
                    contentDescription = "Filtrar",
                    tint = if (filterPriority != null) FireColors.Primary else FireColors.OnSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        val currentFilterPriority = filterPriority
        val filteredTasks = if (currentFilterPriority != null) {
            tasks.filter { it.tarefa.prioridade == currentFilterPriority.name }
        } else {
            tasks
        }

        if (filteredTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (currentFilterPriority != null)
                        "Nenhuma tarefa com prioridade ${currentFilterPriority.name.lowercase()}"
                    else
                        "Nenhuma tarefa cadastrada",
                    fontSize = 13.sp,
                    color = FireColors.OnSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        } else {
            val sortedTasks = filteredTasks.sortedWith(
                compareBy<RoomTarefaComSubtarefas> { it.tarefa.concluida }
                    .thenBy { task -> runCatching { Prioridade.valueOf(task.tarefa.prioridade) }.getOrDefault(Prioridade.MEDIA).ordinal }
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sortedTasks) { task ->
                    ElevatedTaskCard(
                        task = task,
                        onToggle = { onToggleTask(task) },
                        onClick = { onTaskClick(task) }
                    )
                }
            }
        }
    }
}

// ============================================
// COMPONENTES DO CALENDÁRIO
// ============================================

@Composable
fun CenterOperationalTabs(
    currentType: CalendarViewType,
    onTypeSelected: (CalendarViewType) -> Unit
) {
    ElevatedCard(
        variant = CardVariant.ELEVATED,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CalendarViewType.values().forEach { type ->
                val isSelected = type == currentType
                val label = when (type) {
                    CalendarViewType.MONTH -> "📅 Mês"
                    CalendarViewType.WEEK -> "📆 Semana"
                    CalendarViewType.DAY -> "📋 Dia"
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) FireColors.Primary else Color.Transparent)
                        .clickable { onTypeSelected(type) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else FireColors.OnSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Seletor visual de vista do calendário (Mês / Semana / Dia).
 * Mostra apenas as 3 views operacionais suportadas.
 */
@Composable
fun CalendarViewSelector(
    currentType: CalendarViewType,
    onTypeSelected: (CalendarViewType) -> Unit,
    modifier: Modifier = Modifier
) {
    val views = listOf(
        CalendarViewType.MONTH  to "📅 Mês",
        CalendarViewType.WEEK   to "📆 Semana",
        CalendarViewType.DAY    to "📋 Dia"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FireColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            views.forEach { (type, label) ->
                val isSelected = type == currentType
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) FireColors.Primary
                            else Color.Transparent
                        )
                        .clickable { onTypeSelected(type) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else FireColors.OnSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun MonthlyCalendarView(
    currentMonth: LocalDate,
    selectedDate: LocalDate,
    allProntidoes: List<RoomProntidaoDia>,
    occurrencesMap: Map<LocalDate, List<Ocorrencia>>,
    tasksMap: Map<LocalDate, List<RoomTarefaComSubtarefas>>,
    eventsMap: Map<LocalDate, List<RoomEventoComLembretes>>,
    scales: List<com.example.firenotes.domain.model.EscalaConfig> = emptyList(),
    teams: List<com.example.firenotes.domain.model.EquipeConfig> = emptyList(),
    activeLayers: Set<String> = setOf("ESCALA", "EVENTO", "TAREFA", "OCORRENCIA"),
    onDateSelected: (LocalDate) -> Unit,
    onDateLongClick: (LocalDate) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onEventClick: (RoomEventoComLembretes) -> Unit,
    onTaskClick: (RoomTarefaComSubtarefas) -> Unit
) {
    val daysList = remember(currentMonth) {
        val list = mutableListOf<LocalDate>()
        val firstDayOfMonth = currentMonth.withDayOfMonth(1)
        val dayOfWeek = firstDayOfMonth.dayOfWeek.value
        val firstDayOfWeek = dayOfWeek % 7

        val prevMonth = currentMonth.minusMonths(1)
        val daysInPrevMonth = prevMonth.lengthOfMonth()

        for (i in firstDayOfWeek - 1 downTo 0) {
            list.add(prevMonth.withDayOfMonth(daysInPrevMonth - i))
        }

        val daysInCurrentMonth = currentMonth.lengthOfMonth()
        for (i in 1..daysInCurrentMonth) {
            list.add(currentMonth.withDayOfMonth(i))
        }

        val totalCells = if (list.size <= 35) 35 else 42
        val nextMonthDaysNeeded = totalCells - list.size
        for (i in 1..nextMonthDaysNeeded) {
            list.add(currentMonth.plusMonths(1).withDayOfMonth(i))
        }
        list
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = FireColors.Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevMonth) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Mês Anterior")
                }

                Text(
                    text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("pt-BR")))
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.forLanguageTag("pt-BR")) else it.toString() },
                    style = FireTypography.Title,
                    fontWeight = FontWeight.Bold,
                    color = FireColors.OnBackground
                )

                IconButton(onClick = onNextMonth) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Próximo Mês")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val weekLabels = listOf("DOM", "SEG", "TER", "QUA", "QUI", "SEX", "SÁB")
                weekLabels.forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = FireColors.OnSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val weeks = daysList.chunked(7)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                weeks.forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        week.forEach { date ->
                            val isSelected = date == selectedDate
                            val occurrencesCount = occurrencesMap[date]?.size ?: 0
                            val dayEvents = eventsMap[date] ?: emptyList()
                            val dayTasks = tasksMap[date] ?: emptyList()
                            val hasTasks = dayTasks.isNotEmpty()
                            val hasEvents = dayEvents.isNotEmpty()

                            val readinessColor = getProntidaoColorForDate(date, allProntidoes)

                            DayCell(
                                date = date,
                                currentMonth = currentMonth,
                                isSelected = isSelected,
                                occurrencesCount = occurrencesCount,
                                hasTasks = hasTasks,
                                hasEvents = hasEvents,
                                dayEvents = dayEvents,
                                dayTasks = dayTasks,
                                readinessColor = readinessColor,
                                scales = scales,
                                teams = teams,
                                activeLayers = activeLayers,
                                onClick = { onDateSelected(date) },
                                onLongClick = { onDateLongClick(date) },
                                onEventClick = onEventClick,
                                onTaskClick = onTaskClick,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            ReadinessLegend(teams = teams)
        }
    }
}

@Composable
fun DayCell(
    date: LocalDate,
    currentMonth: LocalDate,
    isSelected: Boolean,
    occurrencesCount: Int,
    hasTasks: Boolean,
    hasEvents: Boolean,
    dayEvents: List<RoomEventoComLembretes> = emptyList(),
    dayTasks: List<RoomTarefaComSubtarefas> = emptyList(),
    readinessColor: Color,
    scales: List<com.example.firenotes.domain.model.EscalaConfig> = emptyList(),
    teams: List<com.example.firenotes.domain.model.EquipeConfig> = emptyList(),
    activeLayers: Set<String> = setOf("ESCALA", "EVENTO", "TAREFA", "OCORRENCIA"),
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEventClick: (RoomEventoComLembretes) -> Unit,
    onTaskClick: (RoomTarefaComSubtarefas) -> Unit,
    modifier: Modifier = Modifier
) {
    val isCurrentMonth = date.month == currentMonth.month
    val alpha = if (isCurrentMonth) 1.0f else 0.35f
    val isToday = date == LocalDate.now()

    // Resolução de Escala e cores personalizadas (V8)
    val activeTeams = remember(date, scales, teams) {
        ScaleEngine.getActiveTeamsForDate(date, scales, teams)
    }
    val showCustomScale = activeLayers.contains("ESCALA") && activeTeams.isNotEmpty()
    val teamDiurno = activeTeams[0]?.firstOrNull()
    val teamNoturno = activeTeams[1]?.firstOrNull()
    val defaultSurfaceVariant = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant

    val cellModifier = remember(showCustomScale, teamDiurno, teamNoturno, readinessColor, alpha) {
        if (showCustomScale) {
            val colorD = teamDiurno?.let { runCatching { Color(parseColor(it.corFundo)) }.getOrNull() }?.copy(alpha = 0.5f * alpha)
            val colorN = teamNoturno?.let { runCatching { Color(parseColor(it.corFundo)) }.getOrNull() }?.copy(alpha = 0.5f * alpha)
            
            val brush = if (colorD != null && colorN != null) {
                Brush.verticalGradient(
                    0.0f to colorD,
                    0.5f to colorD,
                    0.5f to colorN,
                    1.0f to colorN
                )
            } else if (colorD != null) {
                Brush.verticalGradient(listOf(colorD, colorD))
            } else if (colorN != null) {
                Brush.verticalGradient(listOf(colorN, colorN))
            } else {
                Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
            }
            Modifier.background(brush)
        } else {
            Modifier.background(defaultSurfaceVariant.copy(alpha = 0.4f * alpha))
        }
    }

    Box(
        modifier = modifier
            .height(68.dp)
            .clip(RoundedCornerShape(8.dp))
            .then(cellModifier)
            .border(
                width = if (isSelected) 2.5.dp else if (isToday) 1.5.dp else 0.5.dp,
                color = if (isSelected) FireColors.Primary else if (isToday) FireColors.Primary.copy(alpha = 0.5f) else FireColors.OnSurfaceVariant.copy(alpha = 0.1f * alpha),
                shape = RoundedCornerShape(8.dp)
            )
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = { onLongClick() }
            )
            .padding(3.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    fontSize = 11.sp,
                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                    color = FireColors.OnBackground.copy(alpha = alpha)
                )

                if (activeLayers.contains("OCORRENCIA") && occurrencesCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .background(FireColors.Error, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = occurrencesCount.toString(),
                            fontSize = 7.5.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Exibição de Títulos dos Eventos/Tarefas (Estilo Google Agenda)
            Column(
                verticalArrangement = Arrangement.spacedBy(1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val itemsToShow = mutableListOf<Triple<String, Color, () -> Unit>>()
                if (activeLayers.contains("EVENTO")) {
                    dayEvents.take(2).forEach { ev ->
                        val evColor = try { Color(android.graphics.Color.parseColor(ev.evento.cor)) } catch(e: Exception) { FireColors.Primary }
                        itemsToShow.add(Triple(ev.evento.titulo, evColor, { onEventClick(ev) }))
                    }
                }
                if (activeLayers.contains("TAREFA") && itemsToShow.size < 2) {
                    dayTasks.take(2 - itemsToShow.size).forEach { t ->
                        val tColor = try { Color(android.graphics.Color.parseColor(t.tarefa.cor)) } catch(e: Exception) { FireColors.Secondary }
                        itemsToShow.add(Triple(t.tarefa.titulo, tColor, { onTaskClick(t) }))
                    }
                }

                itemsToShow.take(2).forEach { (title, itemColor, itemClick) ->
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = itemColor,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(15.dp)
                    ) {
                        Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(horizontal = 3.dp)) {
                            Text(
                                text = title,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                val remaining = (dayEvents.size + dayTasks.size) - itemsToShow.size
                if (remaining > 0) {
                    Text(
                        text = "+$remaining",
                        fontSize = 7.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun WeeklyCalendarView(
    selectedDate: LocalDate,
    allProntidoes: List<RoomProntidaoDia>,
    occurrencesMap: Map<LocalDate, List<Ocorrencia>>,
    tasksMap: Map<LocalDate, List<RoomTarefaComSubtarefas>>,
    eventsMap: Map<LocalDate, List<RoomEventoComLembretes>>,
    scales: List<com.example.firenotes.domain.model.EscalaConfig> = emptyList(),
    teams: List<com.example.firenotes.domain.model.EquipeConfig> = emptyList(),
    activeLayers: Set<String> = setOf("ESCALA", "EVENTO", "TAREFA", "OCORRENCIA"),
    onDateSelected: (LocalDate) -> Unit,
    onEventClick: (RoomEventoComLembretes) -> Unit,
    onTaskClick: (RoomTarefaComSubtarefas) -> Unit
) {
    val weekDays = remember(selectedDate) {
        val list = mutableListOf<LocalDate>()
        val dayOfWeek = selectedDate.dayOfWeek.value
        val sundayOffset = dayOfWeek % 7
        val sunday = selectedDate.minusDays(sundayOffset.toLong())
        for (i in 0..6) {
            list.add(sunday.plusDays(i.toLong()))
        }
        list
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        weekDays.forEach { date ->
            val isSelected = date == selectedDate
            val occurrences = occurrencesMap[date] ?: emptyList()
            val tasks = tasksMap[date] ?: emptyList()
            val events = eventsMap[date] ?: emptyList()
            val prontidaoColor = getProntidaoColorForDate(date, allProntidoes)

            val activeTeams = remember(date, scales, teams) {
                ScaleEngine.getActiveTeamsForDate(date, scales, teams)
            }
            val showCustomScale = activeLayers.contains("ESCALA") && activeTeams.isNotEmpty()
            val team = activeTeams[0]?.firstOrNull() ?: activeTeams[1]?.firstOrNull()
            val resolvedColor = if (showCustomScale && team != null) {
                runCatching { Color(parseColor(team.corFundo)) }.getOrDefault(prontidaoColor)
            } else {
                prontidaoColor
            }

            ElevatedCard(
                variant = CardVariant.ELEVATED,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDateSelected(date) }
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(resolvedColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = date.format(DateTimeFormatter.ofPattern("EEEE, dd/MM", Locale.forLanguageTag("pt-BR")))
                                    .replaceFirstChar { it.uppercase() },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = FireColors.OnBackground
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (occurrences.isNotEmpty()) {
                                Badge(containerColor = FireColors.Error) {
                                    Text("🚑 ${occurrences.size}", color = Color.White, fontSize = 10.sp)
                                }
                            }
                            if (events.isNotEmpty()) {
                                Badge(containerColor = FireColors.Primary) {
                                    Text("📅 ${events.size}", color = Color.White, fontSize = 10.sp)
                                }
                            }
                            if (tasks.isNotEmpty()) {
                                val completedCount = tasks.count { it.tarefa.concluida }
                                Badge(containerColor = FireColors.Secondary) {
                                    Text("📋 $completedCount/${tasks.size}", color = Color.White, fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    if (events.isNotEmpty() || tasks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            events.forEach { event ->
                                Text(
                                    text = "• [${event.evento.horaInicio ?: "Todo o dia"}] ${event.evento.titulo}",
                                    fontSize = 12.sp,
                                    color = FireColors.OnSurfaceVariant
                                )
                            }
                            tasks.take(3).forEach { task ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (task.tarefa.concluida) "☑" else "☐",
                                        fontSize = 12.sp,
                                        color = if (task.tarefa.concluida) FireColors.Secondary else FireColors.OnSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = task.tarefa.titulo,
                                        fontSize = 12.sp,
                                        color = if (task.tarefa.concluida) FireColors.OnSurfaceVariant.copy(alpha = 0.6f) else FireColors.OnSurfaceVariant
                                    )
                                }
                            }
                            if (tasks.size > 3) {
                                Text(
                                    text = "... e mais ${tasks.size - 3} tarefas",
                                    fontSize = 11.sp,
                                    color = FireColors.Primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayCalendarView(
    selectedDate: LocalDate,
    scales: List<com.example.firenotes.domain.model.EscalaConfig> = emptyList(),
    teams: List<com.example.firenotes.domain.model.EquipeConfig> = emptyList(),
    occurrences: List<Ocorrencia> = emptyList(),
    tasks: List<RoomTarefaComSubtarefas> = emptyList(),
    events: List<RoomEventoComLembretes> = emptyList(),
    onEventClick: (RoomEventoComLembretes) -> Unit,
    onTaskClick: (RoomTarefaComSubtarefas) -> Unit,
    onToggleTarefa: (RoomTarefa) -> Unit = {},
    onDeleteEvents: (List<String>) -> Unit = {},
    onDeleteTasks: (List<String>) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val selectedEvents = remember { androidx.compose.runtime.mutableStateListOf<String>() }
    val selectedTasks = remember { androidx.compose.runtime.mutableStateListOf<String>() }
    val hasSelection = selectedEvents.isNotEmpty() || selectedTasks.isNotEmpty()
    val formattedDate = selectedDate.format(
        DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("pt", "BR"))
    )
    val activeTeamsDay = remember(selectedDate, scales, teams) {
        ScaleEngine.getActiveTeamsForDate(selectedDate, scales, teams)
    }
    val allActiveTeams = activeTeamsDay.values.flatten()

    val remindersList = remember(events, tasks) {
        val list = mutableListOf<Triple<String, Color, () -> Unit>>()
        events.forEach { ev ->
            ev.lembretes.forEach { l ->
                val label = "Lembrete: ${ev.evento.titulo} (${l.minutosAntes} min antes)"
                val col = try { Color(android.graphics.Color.parseColor(ev.evento.cor)) } catch(e: Exception) { FireColors.Primary }
                list.add(Triple(label, col, { onEventClick(ev) }))
            }
        }
        tasks.forEach { tk ->
            tk.lembretes.forEach { l ->
                val label = "Lembrete: ${tk.tarefa.titulo} (${l.minutosAntes} min antes)"
                val col = try { Color(android.graphics.Color.parseColor(tk.tarefa.cor)) } catch(e: Exception) { FireColors.Secondary }
                list.add(Triple(label, col, { onTaskClick(tk) }))
            }
        }
        list
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FireColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cabeçalho do dia (ou Contextual ActionBar)
            if (hasSelection) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(FireColors.Primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val total = selectedEvents.size + selectedTasks.size
                    Text("$total selecionados", fontWeight = FontWeight.Bold, color = FireColors.Primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = { selectedEvents.clear(); selectedTasks.clear() }) {
                            Icon(androidx.compose.material.icons.Icons.Default.Close, "Cancelar", tint = FireColors.OnSurfaceVariant)
                        }
                        IconButton(onClick = {
                            com.example.firenotes.util.BiometricHelper.authenticate(
                                context = context,
                                onSuccess = {
                                    if (selectedEvents.isNotEmpty()) onDeleteEvents(selectedEvents.toList())
                                    if (selectedTasks.isNotEmpty()) onDeleteTasks(selectedTasks.toList())
                                    selectedEvents.clear()
                                    selectedTasks.clear()
                                },
                                onError = { err ->
                                    android.widget.Toast.makeText(context, "Autenticação falhou: $err", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                        }) {
                            Icon(androidx.compose.material.icons.Icons.Default.Delete, "Excluir Selecionados", tint = FireColors.Error)
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = formattedDate.replaceFirstChar { it.uppercase() },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = FireColors.OnBackground
                        )
                        Text(
                            text = "Visualização do Dia",
                            fontSize = 12.sp,
                            color = FireColors.OnSurfaceVariant
                        )
                    }
                    // Badge de equipes ativas no dia
                    if (allActiveTeams.isNotEmpty()) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            allActiveTeams.take(2).forEach { equipe ->
                                val eColor = runCatching { Color(parseColor(equipe.corFundo)) }.getOrDefault(FireColors.Primary)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = eColor.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(modifier = Modifier.size(7.dp).background(eColor, CircleShape))
                                        Text(
                                            text = equipe.sigla.ifBlank { equipe.nome },
                                            fontSize = 11.sp, color = eColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = FireColors.OnSurfaceVariant.copy(alpha = 0.1f), thickness = 0.5.dp)

            // Blocos de turno
            if (allActiveTeams.isNotEmpty()) {
                Text("🕐 Equipes de Serviço", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = FireColors.OnBackground)
                allActiveTeams.forEach { equipe ->
                    val eColor = runCatching { Color(parseColor(equipe.corFundo)) }.getOrDefault(FireColors.Primary)
                    val turnoLabel = if (equipe.horaTermino > equipe.horaInicio) {
                        "Diurno ${equipe.horaInicio} – ${equipe.horaTermino}"
                    } else if (equipe.horaInicio == equipe.horaTermino) {
                        "Plantão 24h  a partir de ${equipe.horaInicio}"
                    } else {
                        "Noturno ${equipe.horaInicio} – ${equipe.horaTermino}(+1)"
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(36.dp)
                                .background(eColor, RoundedCornerShape(2.dp))
                        ) { }
                        Column {
                            Text(equipe.nome, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FireColors.OnBackground)
                            Text(turnoLabel, fontSize = 11.sp, color = FireColors.OnSurfaceVariant)
                        }
                    }
                }
            }

            // 1. Eventos
            if (events.isNotEmpty()) {
                Text("📅 Eventos", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = FireColors.OnBackground)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    events.forEach { event ->
                        val isSelected = selectedEvents.contains(event.evento.id)
                        val eColor = try { Color(android.graphics.Color.parseColor(event.evento.cor)) } catch(e: Exception) { FireColors.Primary }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) eColor.copy(alpha = 0.2f) else eColor)
                                .combinedClickable(
                                    onClick = {
                                        if (hasSelection) {
                                            if (isSelected) selectedEvents.remove(event.evento.id) else selectedEvents.add(event.evento.id)
                                        } else {
                                            onEventClick(event)
                                        }
                                    },
                                    onLongClick = {
                                        if (!isSelected) selectedEvents.add(event.evento.id)
                                    }
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (hasSelection) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { if (it) selectedEvents.add(event.evento.id) else selectedEvents.remove(event.evento.id) },
                                    colors = CheckboxDefaults.colors(checkedColor = Color.White, checkmarkColor = eColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = if (isSelected) eColor else Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    event.evento.titulo,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) FireColors.OnBackground else Color.White,
                                    fontSize = 14.sp
                                )
                                val timeText = if (event.evento.horaInicio != null) {
                                    "${event.evento.horaInicio} - ${event.evento.horaFim ?: ""}"
                                } else {
                                    "Todo o dia"
                                }
                                Text(timeText, color = if (isSelected) FireColors.OnSurfaceVariant else Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 2. Ocorrências
            if (occurrences.isNotEmpty()) {
                Text("🚒 Ocorrências", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = FireColors.Error)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    occurrences.forEach { oc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(FireColors.Error.copy(alpha = 0.08f))
                                .border(0.5.dp, FireColors.Error.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = FireColors.Error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(oc.natureza.descricao, fontWeight = FontWeight.Bold, color = FireColors.Error, fontSize = 14.sp)
                                Text("Protocolo: ${oc.protocolo}", color = FireColors.OnSurfaceVariant, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 3. Lembretes
            if (remindersList.isNotEmpty()) {
                Text("📝 Lembretes do Dia", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = FireColors.OnBackground)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    remindersList.forEach { (remTitle, remColor, remClick) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(remColor.copy(alpha = 0.08f))
                                .border(0.5.dp, remColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .clickable { remClick() }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = remColor)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(remTitle, fontWeight = FontWeight.SemiBold, color = FireColors.OnBackground, fontSize = 13.sp)
                        }
                    }
                }
            }

            // 4. Tarefas
            if (tasks.isNotEmpty()) {
                Text("✅ Tarefas (${tasks.count { it.tarefa.concluida }}/${tasks.size})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = FireColors.OnBackground)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    tasks.forEach { task ->
                        val isSelected = selectedTasks.contains(task.tarefa.id)
                        val tColor = try { Color(android.graphics.Color.parseColor(task.tarefa.cor)) } catch(e: Exception) { FireColors.Secondary }
                        val totalSubs = task.subtarefas.size
                        val completedSubs = task.subtarefas.count { it.concluida }
                        val progress = if (totalSubs > 0) completedSubs.toFloat() / totalSubs else if (task.tarefa.concluida) 1.0f else 0.0f
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) tColor.copy(alpha = 0.2f) else tColor.copy(alpha = 0.05f))
                                .border(0.5.dp, tColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .combinedClickable(
                                    onClick = {
                                        if (hasSelection) {
                                            if (isSelected) selectedTasks.remove(task.tarefa.id) else selectedTasks.add(task.tarefa.id)
                                        } else {
                                            onTaskClick(task)
                                        }
                                    },
                                    onLongClick = {
                                        if (!isSelected) selectedTasks.add(task.tarefa.id)
                                    }
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (hasSelection) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { if (it) selectedTasks.add(task.tarefa.id) else selectedTasks.remove(task.tarefa.id) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            } else {
                                Checkbox(
                                    checked = task.tarefa.concluida,
                                    onCheckedChange = { onToggleTarefa(task.tarefa) },
                                    colors = CheckboxDefaults.colors(checkedColor = tColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    task.tarefa.titulo,
                                    fontWeight = FontWeight.Bold,
                                    color = if (task.tarefa.concluida) FireColors.OnSurfaceVariant.copy(alpha = 0.6f) else FireColors.OnBackground,
                                    fontSize = 14.sp,
                                    textDecoration = if (task.tarefa.concluida) TextDecoration.LineThrough else TextDecoration.None
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                if (totalSubs > 0) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        LinearProgressIndicator(
                                            progress = progress,
                                            modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                                            color = tColor,
                                            trackColor = tColor.copy(alpha = 0.2f)
                                        )
                                        Text("$completedSubs/$totalSubs", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FireColors.OnSurfaceVariant)
                                    }
                                } else {
                                    Text("Nenhuma subtarefa", fontSize = 11.sp, color = FireColors.OnSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            if (allActiveTeams.isEmpty() && events.isEmpty() && tasks.isEmpty() && occurrences.isEmpty() && remindersList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhuma atividade registrada para este dia",
                        fontSize = 13.sp,
                        color = FireColors.OnSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

// ============================================
// REPORT DIALOG
// ============================================

@Composable
fun ReportDialog(
    filters: ReportFilters,
    onFiltersChanged: (ReportFilters) -> Unit,
    onDismiss: () -> Unit,
    occurrences: List<Ocorrencia>,
    onNavigateToDetails: (id: String) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .heightIn(max = 600.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(20.dp),
                        clip = false
                    )
                    .border(
                        width = 0.5.dp,
                        color = FireColors.Primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp)
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = FireColors.Surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 8.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📊 Relatório de Ocorrências",
                            style = FireTypography.HeadlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = FireColors.OnBackground
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Fechar",
                                tint = FireColors.OnSurfaceVariant
                            )
                        }
                    }

                    // Filtros simplificados
                    OutlinedTextField(
                        value = filters.searchQuery,
                        onValueChange = { onFiltersChanged(filters.copy(searchQuery = it)) },
                        placeholder = { Text("Buscar ocorrências...", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = FireColors.OnSurfaceVariant
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FireColors.Primary,
                            unfocusedBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.3f),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    HorizontalDivider(color = FireColors.OnSurfaceVariant.copy(alpha = 0.1f))

                    if (occurrences.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Search,
                                    contentDescription = null,
                                    tint = FireColors.OnSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "Nenhuma ocorrência encontrada",
                                    fontSize = 14.sp,
                                    color = FireColors.OnSurfaceVariant
                                )
                                Text(
                                    text = "Tente ajustar os filtros",
                                    fontSize = 12.sp,
                                    color = FireColors.OnSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Resultados: ${occurrences.size} ocorrências",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = FireColors.OnSurface
                            )

                            occurrences.take(20).forEach { ocorrencia ->
                                ElevatedOccurrenceCard(
                                    ocorrencia = ocorrencia,
                                    onClick = {
                                        ocorrencia.id?.let {
                                            onNavigateToDetails(it)
                                            onDismiss()
                                        }
                                    }
                                )
                            }

                            if (occurrences.size > 20) {
                                Text(
                                    text = "... e mais ${occurrences.size - 20} ocorrências",
                                    fontSize = 12.sp,
                                    color = FireColors.OnSurfaceVariant.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = FireColors.Primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Fechar", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

// ============================================
// READINESS LEGEND
// ============================================

@Composable
fun ReadinessLegend(
    teams: List<com.example.firenotes.domain.model.EquipeConfig> = emptyList()
) {
    if (teams.isNotEmpty()) {
        // Legenda dinâmica com equipes configuradas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            teams.forEachIndexed { index, equipe ->
                val equipeColor = runCatching {
                    Color(parseColor(equipe.corFundo))
                }.getOrDefault(Color.Gray)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(equipeColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = equipe.sigla.ifBlank { equipe.nome },
                        fontSize = 11.sp,
                        color = FireColors.OnSurfaceVariant
                    )
                }
                if (index < teams.size - 1) {
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }
        }
    } else {
        // Legenda padrão de prontidão (antes da escala ser configurada)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                Pair("Amarela", Color(0xFFFFC107)),
                Pair("Azul", Color(0xFF2196F3)),
                Pair("Verde", Color(0xFF4CAF50))
            ).forEachIndexed { index, item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(item.second, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = item.first, fontSize = 11.sp, color = FireColors.OnSurfaceVariant)
                }
                if (index < 2) {
                    Spacer(modifier = Modifier.width(16.dp))
                }
            }
        }
    }
}

// ============================================
// FUNÇÕES AUXILIARES
// ============================================

fun getDeviceOwnerName(context: android.content.Context): String {
    val deviceName = try {
        android.provider.Settings.Global.getString(context.contentResolver, "device_name")
            ?: android.provider.Settings.Secure.getString(context.contentResolver, "bluetooth_name")
            ?: android.os.Build.MODEL
    } catch (e: Exception) {
        android.os.Build.MODEL
    }

    val nameRegexes = listOf(
        Regex("de\\s+([^\\s]+)", RegexOption.IGNORE_CASE),
        Regex("([^\\s]+)'s", RegexOption.IGNORE_CASE),
        Regex("Phone\\s+([^\\s]+)", RegexOption.IGNORE_CASE)
    )

    for (regex in nameRegexes) {
        val match = regex.find(deviceName)
        if (match != null && match.groupValues.size > 1) {
            val candidate = match.groupValues[1].trim()
            if (candidate.isNotEmpty()) return candidate
        }
    }

    val firstWord = deviceName.split(" ").firstOrNull() ?: "Operador"
    val commonBrands = listOf("galaxy", "pixel", "moto", "xiaomi", "redmi", "iphone", "android", "emulator", "sdk")
    if (firstWord.lowercase() in commonBrands || firstWord.length < 2) {
        return "Operador"
    }
    return firstWord
}

fun getProntidaoColorForDate(date: LocalDate, allProntidoes: List<RoomProntidaoDia>): Color {
    val name = getProntidaoNameForDate(date, allProntidoes)
    return when (name) {
        "AMARELA" -> Color(0xFFFFC107).copy(alpha = 0.25f)
        "AZUL" -> Color(0xFF2196F3).copy(alpha = 0.25f)
        else -> Color(0xFF4CAF50).copy(alpha = 0.25f)
    }
}

fun getProntidaoNameForDate(date: LocalDate, allProntidoes: List<RoomProntidaoDia>): String {
    val override = allProntidoes.find { it.data == date.toString() }
    if (override != null) {
        return override.escala
    }
    val defaultPront = ProntidaoService.getProntidaoForDate(date)
    return defaultPront.name
}

fun calcularProntidaoInfoParaData(date: LocalDate, allProntidoes: List<RoomProntidaoDia>): ProntidaoInfo {
    val escalaNome = getProntidaoNameForDate(date, allProntidoes)
    val escalaHex = when (escalaNome) {
        "AMARELA" -> 0xFFFFC107
        "AZUL" -> 0xFF2196F3
        else -> 0xFF4CAF50
    }

    val baseTime = LocalTime.now()
    val baseDateTime = date.atTime(baseTime)

    val inicio = baseDateTime.withHour(7).withMinute(30).withSecond(0)
    val fim = baseDateTime.plusDays(1).withHour(7).withMinute(29).withSecond(0)

    return ProntidaoInfo(
        cor = escalaNome,
        corHex = escalaHex,
        inicio = inicio,
        fim = fim,
        horaInicio = "07:30",
        horaFim = "07:30",
        dataInicio = inicio.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
        dataFim = fim.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    )
}

private data class CardStyle(
    val borderColor: Color,
    val borderWidth: Dp,
    val backgroundColor: Color,
    val cornerRadius: Dp,
    val shadowElevation: Dp,
    val contentPadding: PaddingValues
)

@Composable
fun HomeAlertsDialog(
    activeEvents: List<RoomEventoComLembretes>,
    activeTasks: List<RoomTarefaComSubtarefas>,
    onDismiss: () -> Unit,
    onDismissPermanently: () -> Unit
) {
    val dialogSurface = MaterialTheme.colorScheme.surface
    val dialogOnSurface = MaterialTheme.colorScheme.onSurface
    val dialogOnSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val cardBackground = MaterialTheme.colorScheme.surfaceVariant

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🔔 Compromissos de Hoje", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = FireColors.Primary)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Você possui compromissos agendados para hoje:", fontSize = 14.sp, color = dialogOnSurface)

                if (activeEvents.isNotEmpty()) {
                    Text("📅 Eventos:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FireColors.Primary)
                    activeEvents.forEach { ev ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = cardBackground),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(ev.evento.titulo, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = dialogOnSurface)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Horário: ${ev.evento.horaInicio ?: ""} às ${ev.evento.horaFim ?: ""}", fontSize = 12.sp, color = dialogOnSurfaceVariant)
                                if (!ev.evento.descricao.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(ev.evento.descricao, fontSize = 12.sp, color = dialogOnSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                if (activeTasks.isNotEmpty()) {
                    Text("📋 Tarefas Pendentes:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FireColors.Warning)
                    activeTasks.forEach { task ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = cardBackground),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(task.tarefa.titulo, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = dialogOnSurface)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Prazo: ${task.tarefa.hora ?: ""}", fontSize = 12.sp, color = dialogOnSurfaceVariant)
                                if (!task.tarefa.descricao.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(task.tarefa.descricao, fontSize = 12.sp, color = dialogOnSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissPermanently
            ) {
                Text("Não alertar mais nesta sessão", color = dialogOnSurfaceVariant, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = FireColors.Primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Entendido", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = dialogSurface,
        shape = RoundedCornerShape(16.dp)
    )
}