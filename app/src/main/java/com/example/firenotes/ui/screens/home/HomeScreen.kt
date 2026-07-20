package com.example.firenotes.ui.screens.home

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.firenotes.domain.model.NaturezaOcorrencia
import com.example.firenotes.domain.model.Ocorrencia
import com.example.firenotes.ui.designsystem.colors.FireColors
import com.example.firenotes.ui.designsystem.typography.FireTypography
import com.example.firenotes.data.service.ProntidaoService
import com.example.firenotes.data.service.ProntidaoService.ProntidaoInfo
import com.example.firenotes.data.local.entities.RoomTarefa
import com.example.firenotes.data.local.entities.RoomEventoAgenda
import com.example.firenotes.data.local.entities.RoomProntidaoDia
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.hilt.navigation.compose.hiltViewModel


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
    task: RoomTarefa,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val priorityColor = when (runCatching { Prioridade.valueOf(task.prioridade) }
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
                color = if (task.concluida)
                    FireColors.OnSurfaceVariant.copy(alpha = 0.08f)
                else
                    priorityColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onToggle() }
            .background(
                color = if (task.concluida)
                    FireColors.Surface.copy(alpha = 0.5f)
                else
                    FireColors.Surface.copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.concluida)
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
                checked = task.concluida,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF22C55E),
                    uncheckedColor = FireColors.OnSurfaceVariant.copy(alpha = 0.3f)
                ),
                modifier = Modifier.size(20.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.titulo,
                    fontSize = 13.sp,
                    fontWeight = if (task.concluida) FontWeight.Normal else FontWeight.Medium,
                    color = if (task.concluida)
                        FireColors.OnSurfaceVariant.copy(alpha = 0.5f)
                    else
                        FireColors.OnBackground,
                    textDecoration = if (task.concluida) TextDecoration.LineThrough else null
                )
                if (!task.descricao.isNullOrBlank()) {
                    Text(
                        text = task.descricao,
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
                    text = task.prioridade,
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
    event: RoomEventoAgenda,
    modifier: Modifier = Modifier
) {
    val tipo = runCatching {
        com.example.firenotes.ui.screens.agenda.TipoEvento.valueOf(event.tipo ?: "OUTRO")
    }.getOrNull()

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
                        Text(
                            text = tipo?.icon ?: "📌",
                            fontSize = 16.sp
                        )
                    }
                }

                Column {
                    Text(
                        text = event.titulo,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = FireColors.OnBackground
                    )
                    if (!event.descricao.isNullOrBlank()) {
                        Text(
                            text = event.descricao,
                            fontSize = 11.sp,
                            color = FireColors.OnSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "🕒 ${event.horaInicio ?: "08:00"} - ${event.horaFim ?: "Retorno"}",
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
    onNavigateToAgenda: (date: String) -> Unit = {},
    onNavigateToConsult: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val selectedDate by viewModel.selectedDate.collectAsState()
    val currentMonthDate by viewModel.currentMonth.collectAsState()
    val allTarefas by viewModel.allTarefas.collectAsState()
    val allEventos by viewModel.allEventos.collectAsState()
    val allProntidoes by viewModel.allProntidoes.collectAsState()
    val currentCity by viewModel.currentCity.collectAsState()
    val hasDismissedAlertsThisSession by viewModel.hasDismissedAlertsThisSession.collectAsState()

    var calendarViewType by remember { mutableStateOf(CalendarViewType.MONTH) }
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
        allEventos.filter { event ->
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
        allTarefas.filter { task ->
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


    val context = LocalContext.current
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
        containerColor = FireColors.Background,
        modifier = modifier
    ) { innerPadding ->
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

            val tasksMap = remember(allTarefas) {
                allTarefas.groupBy { LocalDate.parse(it.data) }
            }

            val eventsMap = remember(allEventos) {
                allEventos.groupBy { LocalDate.parse(it.data) }
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
                                prontidaoInfo = remember(selectedDate, allProntidoes) {
                                    calcularProntidaoInfoParaData(selectedDate, allProntidoes)
                                }
                            )
                        }

                        // Calendário
                        item {
                            AnimatedContent(
                                targetState = calendarViewType,
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
                                            onDateSelected = { viewModel.selectDate(it) },
                                            onPrevMonth = { viewModel.previousMonth() },
                                            onNextMonth = { viewModel.nextMonth() }
                                        )
                                    }
                                    CalendarViewType.WEEK -> {
                                        WeeklyCalendarView(
                                            selectedDate = selectedDate,
                                            allProntidoes = allProntidoes,
                                            occurrencesMap = occurrencesMap,
                                            tasksMap = tasksMap,
                                            eventsMap = eventsMap,
                                            onDateSelected = { viewModel.selectDate(it) }
                                        )
                                    }
                                    CalendarViewType.DAY -> {
                                        DayCalendarView(
                                            selectedDate = selectedDate,
                                            allProntidoes = allProntidoes
                                        )
                                    }
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
                                onToggleTask = { task -> viewModel.toggleTarefa(task) },
                                onNavigateToDetails = onNavigateToDetails,
                                onOpenAgenda = { onNavigateToAgenda(selectedDate.toString()) }
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
    prontidaoInfo: ProntidaoInfo,
    modifier: Modifier = Modifier
) {
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

        // Badge de prontidão ocupando as 2 linhas à direita
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(prontidaoInfo.corHex).copy(alpha = 0.12f),
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
                        .background(Color(prontidaoInfo.corHex), CircleShape)
                )
                Text(
                    text = prontidaoInfo.cor,
                    fontSize = 12.sp,
                    color = Color(prontidaoInfo.corHex),
                    fontWeight = FontWeight.Bold
                )
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
    tasks: List<RoomTarefa>,
    events: List<RoomEventoAgenda>,
    allProntidoes: List<RoomProntidaoDia>,
    selectedTab: TabType,
    onTabSelected: (TabType) -> Unit,
    onToggleTask: (RoomTarefa) -> Unit,
    onNavigateToDetails: (String) -> Unit,
    onOpenAgenda: () -> Unit
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

                OutlinedButton(
                    onClick = onOpenAgenda,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = FireColors.Primary
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp,
                        FireColors.Primary.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ver agenda", fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                                    val pending = tasks.count { !it.concluida }
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
                        events = events
                    )
                }
                TabType.CHECKS -> {
                    TasksList(
                        tasks = tasks,
                        onToggleTask = onToggleTask
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
    events: List<RoomEventoAgenda>
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
                    ElevatedEventCard(event = event)
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
    tasks: List<RoomTarefa>,
    onToggleTask: (RoomTarefa) -> Unit
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
                        text = "${tasks.count { it.concluida }}/${tasks.size}",
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
            tasks.filter { it.prioridade == currentFilterPriority.name }
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
                compareBy<RoomTarefa> { it.concluida }
                    .thenBy { task -> runCatching { Prioridade.valueOf(task.prioridade) }.getOrDefault(Prioridade.MEDIA).ordinal }
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
                        onToggle = { onToggleTask(task) }
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

@Composable
fun MonthlyCalendarView(
    currentMonth: LocalDate,
    selectedDate: LocalDate,
    allProntidoes: List<RoomProntidaoDia>,
    occurrencesMap: Map<LocalDate, List<Ocorrencia>>,
    tasksMap: Map<LocalDate, List<RoomTarefa>>,
    eventsMap: Map<LocalDate, List<RoomEventoAgenda>>,
    onDateSelected: (LocalDate) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
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
                            val hasTasks = tasksMap[date]?.isNotEmpty() == true
                            val hasEvents = eventsMap[date]?.isNotEmpty() == true

                            val readinessColor = getProntidaoColorForDate(date, allProntidoes)

                            DayCell(
                                date = date,
                                currentMonth = currentMonth,
                                isSelected = isSelected,
                                occurrencesCount = occurrencesCount,
                                hasTasks = hasTasks,
                                hasEvents = hasEvents,
                                readinessColor = readinessColor,
                                onClick = { onDateSelected(date) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            ReadinessLegend()
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
    readinessColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCurrentMonth = date.month == currentMonth.month
    val alpha = if (isCurrentMonth) 1.0f else 0.35f
    val isToday = date == LocalDate.now()

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(readinessColor.copy(alpha = readinessColor.alpha * alpha))
            .border(
                width = if (isSelected) 2.5.dp else if (isToday) 1.5.dp else 0.5.dp,
                color = if (isSelected) FireColors.Primary else if (isToday) FireColors.Primary.copy(alpha = 0.5f) else FireColors.OnSurfaceVariant.copy(alpha = 0.1f * alpha),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    fontSize = 12.sp,
                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                    color = FireColors.OnBackground.copy(alpha = alpha)
                )

                if (occurrencesCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(FireColors.Error, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = occurrencesCount.toString(),
                            fontSize = 8.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasEvents) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(FireColors.Primary, CircleShape)
                    )
                }
                if (hasTasks) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(FireColors.Secondary, CircleShape)
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
    tasksMap: Map<LocalDate, List<RoomTarefa>>,
    eventsMap: Map<LocalDate, List<RoomEventoAgenda>>,
    onDateSelected: (LocalDate) -> Unit
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
                                    .background(prontidaoColor, CircleShape)
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
                                val completedCount = tasks.count { it.concluida }
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
                                    text = "• [${event.horaInicio ?: "Todo o dia"}] ${event.titulo}",
                                    fontSize = 12.sp,
                                    color = FireColors.OnSurfaceVariant
                                )
                            }
                            tasks.take(3).forEach { task ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (task.concluida) "☑" else "☐",
                                        fontSize = 12.sp,
                                        color = if (task.concluida) FireColors.Secondary else FireColors.OnSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = task.titulo,
                                        fontSize = 12.sp,
                                        color = if (task.concluida) FireColors.OnSurfaceVariant.copy(alpha = 0.6f) else FireColors.OnSurfaceVariant
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
    allProntidoes: List<RoomProntidaoDia>
) {
    val prontidaoColor = getProntidaoColorForDate(selectedDate, allProntidoes)
    val formattedDate = selectedDate.format(
        DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("pt", "BR"))
    )

    ElevatedCard(
        variant = CardVariant.ELEVATED,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                    text = "Visualização diária",
                    fontSize = 12.sp,
                    color = FireColors.OnSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(prontidaoColor, CircleShape)
            )
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
fun ReadinessLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(
            Triple("Amarela", Color(0xFFFFC107), "🟡"),
            Triple("Azul", Color(0xFF2196F3), "🔵"),
            Triple("Verde", Color(0xFF4CAF50), "🟢")
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
    activeEvents: List<RoomEventoAgenda>,
    activeTasks: List<RoomTarefa>,
    onDismiss: () -> Unit,
    onDismissPermanently: () -> Unit
) {
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
                Text("Voce possui compromissos agendados para hoje:", fontSize = 14.sp, color = FireColors.OnBackground)

                if (activeEvents.isNotEmpty()) {
                    Text("📅 Eventos:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FireColors.Primary)
                    activeEvents.forEach { ev ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = FireColors.Primary.copy(alpha = 0.05f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(ev.titulo, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FireColors.OnBackground)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Horario: ${ev.horaInicio ?: ""} as ${ev.horaFim ?: ""}", fontSize = 12.sp, color = FireColors.OnSurfaceVariant)
                                if (!ev.descricao.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(ev.descricao, fontSize = 12.sp, color = FireColors.OnSurfaceVariant)
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
                            colors = CardDefaults.cardColors(containerColor = FireColors.Warning.copy(alpha = 0.05f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(task.titulo, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FireColors.OnBackground)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Prazo: ${task.hora ?: ""}", fontSize = 12.sp, color = FireColors.OnSurfaceVariant)
                                if (!task.descricao.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(task.descricao, fontSize = 12.sp, color = FireColors.OnSurfaceVariant)
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
                Text("Nao alertar mais nesta sessao", color = FireColors.OnSurfaceVariant, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = FireColors.Primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Entendido")
            }
        }
    )
}