package com.example.firenotes.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

/**
 * Tipos de cards disponíveis no sistema
 */
enum class CardVariant {
    /** Card com sombra elevada e borda sutil */
    ELEVATED,
    /** Card com borda destacada */
    OUTLINED,
    /** Card com preenchimento sólido */
    FILLED,
    /** Card com gradiente de fundo */
    GRADIENT
}

/**
 * Card universal com estilo "Elevated Card"
 * Baseado no modelo de referência com sombra e fundo suave
 */
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
    badge: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    // Configuração baseada no tipo
    val style = when (variant) {
        CardVariant.ELEVATED -> CardStyle(
            borderColor = FireColors.Primary.copy(alpha = 0.12f),
            borderWidth = 0.5.dp,
            backgroundColor = FireColors.Surface.copy(alpha = 0.85f),
            cornerRadius = 16.dp,
            shadowElevation = 4.dp,
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
        // Gradiente background para variante GRADIENT
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
            // Header com título e ícone/trailing
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
                        // Ícone
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

                        // Título
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

                    // Badge ou trailing
                    if (badge != null) {
                        badge()
                    }
                    if (trailing != null) {
                        trailing()
                    }
                }
            }

            // Subtítulo
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = FireColors.OnSurfaceVariant,
                    modifier = Modifier.padding(start = if (icon != null) 52.dp else 0.dp)
                )
            }

            // Legenda/Caption
            if (caption != null) {
                Text(
                    text = caption,
                    fontSize = 12.sp,
                    color = FireColors.OnSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = if (icon != null) 52.dp else 0.dp)
                )
            }

            // Conteúdo personalizado
            content?.invoke(this)

            // Divisor
            if (actionText != null && onAction != null && content != null) {
                HorizontalDivider(
                    color = FireColors.OnSurfaceVariant.copy(alpha = 0.08f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Ação
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

/**
 * Card de ocorrência com estilo Elevated
 */
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
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Indicador de natureza
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

/**
 * Card de tarefa com estilo Elevated
 */
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
            // Indicador de prioridade
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(28.dp)
                    .background(priorityColor, RoundedCornerShape(2.dp))
            )

            // Checkbox
            Checkbox(
                checked = task.concluida,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF22C55E),
                    uncheckedColor = FireColors.OnSurfaceVariant.copy(alpha = 0.3f)
                ),
                modifier = Modifier.size(20.dp)
            )

            // Conteúdo
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

            // Badge de prioridade
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

/**
 * Card de evento com estilo Elevated
 */
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
                // Ícone do tipo
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

            // Indicador de tipo
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
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val weatherState by viewModel.weatherState.collectAsState()
    val isLoadingWeather by viewModel.isLoadingWeather.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val selectedDate by viewModel.selectedDate.collectAsState()
    val currentMonthDate by viewModel.currentMonth.collectAsState()
    val allTarefas by viewModel.allTarefas.collectAsState()
    val allEventos by viewModel.allEventos.collectAsState()
    val allProntidoes by viewModel.allProntidoes.collectAsState()

    var calendarViewType by remember { mutableStateOf(CalendarViewType.MONTH) }
    var showReportDialog by remember { mutableStateOf(false) }
    var filters by remember { mutableStateOf(ReportFilters()) }


    val context = LocalContext.current
    val userName = remember(context) { getDeviceOwnerName(context) }
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

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
                            onClick = { onNavigateToAgenda(selectedDate.toString()) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Outlined.CalendarMonth,
                                contentDescription = "Agenda",
                                tint = FireColors.OnSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(
                            onClick = { showReportDialog = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Outlined.BarChart,
                                contentDescription = "Relatórios",
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

                    if (filters.dateRange != DateRange.TODAY) {
                        // Implementar filtro por período
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
                    val stateSuccess = state

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

                    LazyColumn(
                        contentPadding = PaddingValues(
                            horizontal = if (isTablet) 32.dp else 16.dp,
                            vertical = 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            EnhancedWelcomeHeader(
                                greeting = greeting,
                                userName = userName,
                                todayDate = todayDate,
                                weatherState = weatherState,
                                isLoadingWeather = isLoadingWeather,
                                prontidaoInfo = remember(selectedDate, allProntidoes) {
                                    calcularProntidaoInfoParaData(selectedDate, allProntidoes)
                                },
                                isRefreshing = isRefreshing,
                                onRefresh = { viewModel.refreshAll() }
                            )
                        }

                        item {
                            CenterOperationalTabs(
                                currentType = calendarViewType,
                                onTypeSelected = { calendarViewType = it }
                            )
                        }

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
                                        DailyOperationalView(
                                            selectedDate = selectedDate,
                                            allProntidoes = allProntidoes,
                                            occurrences = occurrencesMap[selectedDate] ?: emptyList(),
                                            tasks = tasksMap[selectedDate] ?: emptyList(),
                                            events = eventsMap[selectedDate] ?: emptyList(),
                                            onToggleTask = { task -> viewModel.toggleTarefa(task) },
                                            onNavigateToDetails = onNavigateToDetails,
                                            onOpenAgenda = { onNavigateToAgenda(selectedDate.toString()) }
                                        )
                                    }
                                }
                            }
                        }

                        if (calendarViewType != CalendarViewType.DAY) {
                            item {
                                DailyOperationalView(
                                    selectedDate = selectedDate,
                                    allProntidoes = allProntidoes,
                                    occurrences = occurrencesMap[selectedDate] ?: emptyList(),
                                    tasks = tasksMap[selectedDate] ?: emptyList(),
                                    events = eventsMap[selectedDate] ?: emptyList(),
                                    onToggleTask = { task -> viewModel.toggleTarefa(task) },
                                    onNavigateToDetails = onNavigateToDetails,
                                    onOpenAgenda = { onNavigateToAgenda(selectedDate.toString()) }
                                )
                            }
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
// COMPONENTES DE FILTRO E RELATÓRIOS
// ============================================

@Composable
fun ReportFilterBar(
    filters: ReportFilters,
    onFiltersChanged: (ReportFilters) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                trailingIcon = {
                    if (filters.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onFiltersChanged(filters.copy(searchQuery = "")) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Limpar", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FireColors.Primary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            IconButton(
                onClick = { expanded = !expanded },
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (hasActiveFilters(filters)) FireColors.Primary.copy(alpha = 0.2f)
                        else Color.Transparent,
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Box {
                    Icon(
                        Icons.Outlined.Tune,
                        contentDescription = "Filtros",
                        tint = if (hasActiveFilters(filters)) FireColors.Primary else FireColors.OnSurfaceVariant
                    )
                    if (hasActiveFilters(filters)) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .align(Alignment.TopEnd)
                                .background(FireColors.Primary, CircleShape)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            ElevatedCard(
                variant = CardVariant.ELEVATED,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📅 Período:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = FireColors.OnSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            DateRange.values().forEach { range ->
                                FilterChip(
                                    selected = filters.dateRange == range,
                                    onClick = {
                                        onFiltersChanged(filters.copy(dateRange = range))
                                    },
                                    label = {
                                        Text(
                                            text = when(range) {
                                                DateRange.TODAY -> "Hoje"
                                                DateRange.THIS_WEEK -> "Semana"
                                                DateRange.THIS_MONTH -> "Mês"
                                                DateRange.CUSTOM -> "Personalizado"
                                            },
                                            fontSize = 10.sp
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = FireColors.Primary,
                                        selectedLabelColor = Color.White,
                                        containerColor = Color.Transparent,
                                        labelColor = FireColors.OnSurfaceVariant
                                    ),
                                    modifier = Modifier
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🚒 Natureza:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = FireColors.OnSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            FilterChip(
                                selected = filters.natureType == null,
                                onClick = {
                                    onFiltersChanged(filters.copy(natureType = null))
                                },
                                label = { Text("Todos", fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FireColors.SurfaceVariant,
                                    selectedLabelColor = FireColors.OnSurface,
                                    containerColor = Color.Transparent,
                                    labelColor = FireColors.OnSurfaceVariant
                                ),
                                modifier = Modifier
                            )
                            NaturezaOcorrencia.values().forEach { nature ->
                                FilterChip(
                                    selected = filters.natureType == nature,
                                    onClick = {
                                        onFiltersChanged(filters.copy(natureType = nature))
                                    },
                                    label = {
                                        Text(
                                            text = when(nature) {
                                                NaturezaOcorrencia.INCENDIO -> "🔥"
                                                NaturezaOcorrencia.SALVAMENTO -> "🆘"
                                                NaturezaOcorrencia.ACIDENTE_TRANSITO -> "🚗"
                                                NaturezaOcorrencia.QUEDA -> "⬇️"
                                                NaturezaOcorrencia.PESSOAL -> "👤"
                                                NaturezaOcorrencia.INDEFINIDA -> "❓"
                                            },
                                            fontSize = 12.sp
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = FireColors.Primary,
                                        selectedLabelColor = Color.White,
                                        containerColor = Color.Transparent,
                                        labelColor = FireColors.OnSurfaceVariant
                                    ),
                                    modifier = Modifier
                                )
                            }
                        }
                    }

                    if (hasActiveFilters(filters)) {
                        TextButton(
                            onClick = {
                                onFiltersChanged(ReportFilters())
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = FireColors.Error
                            )
                        ) {
                            Text("Limpar todos os filtros", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

fun hasActiveFilters(filters: ReportFilters): Boolean {
    return filters.natureType != null ||
            filters.searchQuery.isNotEmpty() ||
            filters.dateRange != DateRange.TODAY ||
            filters.status != ReportStatus.ALL
}

// ============================================
// REPORT DIALOG - CORRIGIDO COM FUNDO SÓLIDO
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
        // Fundo escuro para o diálogo
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000)) // Fundo escuro com opacidade
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            // Card com fundo sólido
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
                    containerColor = FireColors.Surface // Fundo sólido
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
                    // Header com botão fechar
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

                    ReportFilterBar(
                        filters = filters,
                        onFiltersChanged = onFiltersChanged
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

    ElevatedCard(
        variant = CardVariant.ELEVATED,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
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

// ============================================
// DAILY OPERATIONAL VIEW - ESTILO ELEVATED CARD
// ============================================

@Composable
fun DailyOperationalView(
    selectedDate: LocalDate,
    allProntidoes: List<RoomProntidaoDia>,
    occurrences: List<Ocorrencia>,
    tasks: List<RoomTarefa>,
    events: List<RoomEventoAgenda>,
    onToggleTask: (RoomTarefa) -> Unit,
    onNavigateToDetails: (id: String) -> Unit,
    onOpenAgenda: () -> Unit
) {
    val formattedDate = selectedDate.format(
        DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("pt", "BR"))
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Card Principal - Dia
        ElevatedCard(
            title = "📅 $formattedDate",
            subtitle = "Escala e agenda operacional",
            variant = CardVariant.ELEVATED,
            modifier = Modifier.fillMaxWidth(),
            trailing = {
                val prontidao = ProntidaoService.getProntidaoForDate(selectedDate)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = prontidao.cor.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(prontidao.cor, CircleShape)
                        )
                        Text(
                            text = prontidao.nome.replace("Prontidão ", ""),
                            fontSize = 10.sp,
                            color = prontidao.cor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onOpenAgenda,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = FireColors.Primary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ver agenda completa", fontSize = 11.sp)
                }
            }
        }

        // Seção de Ocorrências
        if (occurrences.isNotEmpty()) {
            ElevatedCard(
                title = "🚑 Ocorrências",
                caption = "${occurrences.size} ocorrência${if (occurrences.size > 1) "s" else ""} hoje",
                variant = CardVariant.ELEVATED,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    occurrences.forEach { ocorrencia ->
                        ElevatedOccurrenceCard(
                            ocorrencia = ocorrencia,
                            onClick = { ocorrencia.id?.let(onNavigateToDetails) }
                        )
                    }
                }
            }
        }

        // Seção de Eventos
        if (events.isNotEmpty()) {
            ElevatedCard(
                title = "📋 Agenda",
                caption = "${events.size} compromisso${if (events.size > 1) "s" else ""} hoje",
                variant = CardVariant.ELEVATED,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    events.forEach { event ->
                        ElevatedEventCard(event = event)
                    }
                }
            }
        }

        // Seção de Tarefas - Checklist Operacional
        if (tasks.isNotEmpty()) {
            OperationalChecklist(
                tasks = tasks,
                onToggleTask = onToggleTask
            )
        }
    }
}

// ============================================
// OPERATIONAL CHECKLIST - ESTILO ELEVATED CARD
// ============================================

@Composable
fun OperationalChecklist(
    tasks: List<RoomTarefa>,
    onToggleTask: (RoomTarefa) -> Unit,
    modifier: Modifier = Modifier
) {
    var filterPriority by remember { mutableStateOf<Prioridade?>(null) }

    ElevatedCard(
        title = "📋 Checklist Operacional",
        variant = CardVariant.ELEVATED,
        modifier = modifier.fillMaxWidth(),
        trailing = {
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
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    filterPriority = when(filterPriority) {
                        null -> Prioridade.ALTA
                        Prioridade.ALTA -> Prioridade.MEDIA
                        Prioridade.MEDIA -> Prioridade.BAIXA
                        Prioridade.BAIXA -> null
                    }
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Outlined.FilterList,
                    contentDescription = "Filtrar",
                    tint = if (filterPriority != null) FireColors.Primary else FireColors.OnSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val currentFilterPriority = filterPriority
        val filteredTasks = if (currentFilterPriority != null) {
            tasks.filter { it.prioridade == currentFilterPriority.name }
        } else {
            tasks
        }

        if (filteredTasks.isEmpty()) {
            Text(
                text = if (filterPriority != null) "Nenhuma tarefa com esta prioridade" else "Nenhuma tarefa cadastrada",
                fontSize = 12.sp,
                color = FireColors.OnSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            val sortedTasks = filteredTasks.sortedWith(
                compareBy<RoomTarefa> { it.concluida }
                    .thenBy { task -> runCatching { Prioridade.valueOf(task.prioridade) }.getOrDefault(Prioridade.MEDIA).ordinal }
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                sortedTasks.forEach { task ->
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
// ESTATÍSTICAS DINÂMICAS
// ============================================

@Composable
fun DynamicStatisticsSection(
    selectedDate: LocalDate,
    occurrencesMap: Map<LocalDate, List<Ocorrencia>>,
    tasksMap: Map<LocalDate, List<RoomTarefa>>,
    eventsMap: Map<LocalDate, List<RoomEventoAgenda>>,
    calendarViewType: CalendarViewType,
    modifier: Modifier = Modifier
) {
    val stats = when (calendarViewType) {
        CalendarViewType.DAY -> calculateDayStats(selectedDate, occurrencesMap, tasksMap, eventsMap)
        CalendarViewType.WEEK -> calculateWeekStats(selectedDate, occurrencesMap, tasksMap, eventsMap)
        CalendarViewType.MONTH -> calculateMonthStats(selectedDate, occurrencesMap, tasksMap, eventsMap)
    }

    ElevatedCard(
        title = "📊 Resumo ${getPeriodLabel(calendarViewType)}",
        subtitle = getPeriodDateRange(selectedDate, calendarViewType),
        variant = CardVariant.ELEVATED,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            StatCard(
                label = "Ocorrências",
                value = stats.totalOccurrences,
                icon = Icons.Default.List,
                color = FireColors.Primary,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Tarefas",
                value = stats.totalTasks,
                icon = Icons.Outlined.Checklist,
                color = FireColors.Secondary,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            StatCard(
                label = "Eventos",
                value = stats.totalEvents,
                icon = Icons.Outlined.Event,
                color = Color(0xFF9C27B0),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Concluídas",
                value = stats.completedTasks,
                icon = Icons.Outlined.CheckCircle,
                color = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

data class PeriodStats(
    val totalOccurrences: Int,
    val totalTasks: Int,
    val completedTasks: Int,
    val totalEvents: Int
)

fun calculateDayStats(
    selectedDate: LocalDate,
    occurrencesMap: Map<LocalDate, List<Ocorrencia>>,
    tasksMap: Map<LocalDate, List<RoomTarefa>>,
    eventsMap: Map<LocalDate, List<RoomEventoAgenda>>
): PeriodStats {
    val occurrences = occurrencesMap[selectedDate] ?: emptyList()
    val tasks = tasksMap[selectedDate] ?: emptyList()
    val events = eventsMap[selectedDate] ?: emptyList()

    return PeriodStats(
        totalOccurrences = occurrences.size,
        totalTasks = tasks.size,
        completedTasks = tasks.count { it.concluida },
        totalEvents = events.size
    )
}

fun calculateWeekStats(
    selectedDate: LocalDate,
    occurrencesMap: Map<LocalDate, List<Ocorrencia>>,
    tasksMap: Map<LocalDate, List<RoomTarefa>>,
    eventsMap: Map<LocalDate, List<RoomEventoAgenda>>
): PeriodStats {
    val weekDates = getWeekDates(selectedDate)
    return calculatePeriodStats(weekDates, occurrencesMap, tasksMap, eventsMap)
}

fun calculateMonthStats(
    selectedDate: LocalDate,
    occurrencesMap: Map<LocalDate, List<Ocorrencia>>,
    tasksMap: Map<LocalDate, List<RoomTarefa>>,
    eventsMap: Map<LocalDate, List<RoomEventoAgenda>>
): PeriodStats {
    val monthDates = getMonthDates(selectedDate)
    return calculatePeriodStats(monthDates, occurrencesMap, tasksMap, eventsMap)
}

private fun calculatePeriodStats(
    dates: List<LocalDate>,
    occurrencesMap: Map<LocalDate, List<Ocorrencia>>,
    tasksMap: Map<LocalDate, List<RoomTarefa>>,
    eventsMap: Map<LocalDate, List<RoomEventoAgenda>>
): PeriodStats {
    var occurrences = 0
    var tasks = 0
    var completed = 0
    var events = 0

    dates.forEach { date ->
        occurrences += occurrencesMap[date]?.size ?: 0
        val taskList = tasksMap[date] ?: emptyList()
        tasks += taskList.size
        completed += taskList.count { it.concluida }
        events += eventsMap[date]?.size ?: 0
    }

    return PeriodStats(occurrences, tasks, completed, events)
}

fun getWeekDates(date: LocalDate): List<LocalDate> {
    val dayOfWeek = date.dayOfWeek.value
    val mondayOffset = if (dayOfWeek == 7) 6 else dayOfWeek - 1
    val monday = date.minusDays(mondayOffset.toLong())
    return List(7) { monday.plusDays(it.toLong()) }
}

fun getMonthDates(date: LocalDate): List<LocalDate> {
    val firstDay = date.withDayOfMonth(1)
    val lastDay = date.withDayOfMonth(date.lengthOfMonth())
    return (0L..(lastDay.dayOfMonth - 1).toLong())
        .map { firstDay.plusDays(it) }
}

fun getPeriodLabel(viewType: CalendarViewType): String {
    return when (viewType) {
        CalendarViewType.DAY -> "do Dia"
        CalendarViewType.WEEK -> "da Semana"
        CalendarViewType.MONTH -> "do Mês"
    }
}

fun getPeriodDateRange(date: LocalDate, viewType: CalendarViewType): String {
    return when (viewType) {
        CalendarViewType.DAY -> date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        CalendarViewType.WEEK -> {
            val weekDates = getWeekDates(date)
            "${weekDates.first().format(DateTimeFormatter.ofPattern("dd/MM"))} - ${weekDates.last().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}"
        }
        CalendarViewType.MONTH -> date.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("pt-BR")))
    }
}

// ============================================
// DIÁLOGOS
// ============================================

@Composable
fun CityDialog(
    currentCity: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var inputCity by remember { mutableStateOf(currentCity) }

    Dialog(onDismissRequest = onDismiss) {
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
                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(16.dp))
                    .border(0.5.dp, FireColors.Primary.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FireColors.Surface)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "📍 Localidade",
                        style = FireTypography.Title,
                        fontWeight = FontWeight.Bold,
                        color = FireColors.OnBackground
                    )

                    OutlinedTextField(
                        value = inputCity,
                        onValueChange = { inputCity = it },
                        label = { Text("Cidade/UF") },
                        placeholder = { Text("Sorocaba/SP") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancelar", color = FireColors.OnSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onConfirm(inputCity) },
                            colors = ButtonDefaults.buttonColors(containerColor = FireColors.Primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Confirmar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddEventDialog(
    selectedDate: LocalDate,
    onConfirm: (String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var startHour by remember { mutableStateOf("08:00") }
    var endHour by remember { mutableStateOf("09:00") }

    Dialog(onDismissRequest = onDismiss) {
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
                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(16.dp))
                    .border(0.5.dp, FireColors.Primary.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FireColors.Surface)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "📅 Novo Evento - " + selectedDate.format(DateTimeFormatter.ofPattern("dd/MM")),
                        style = FireTypography.Title,
                        fontWeight = FontWeight.Bold,
                        color = FireColors.OnBackground
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Título do Evento") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descrição (opcional)") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = startHour,
                            onValueChange = { startHour = it },
                            label = { Text("Início") },
                            placeholder = { Text("08:00") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = endHour,
                            onValueChange = { endHour = it },
                            label = { Text("Fim") },
                            placeholder = { Text("18:00") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancelar", color = FireColors.OnSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    onConfirm(title, description, startHour, endHour)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FireColors.Primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Salvar")
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// GLASS CARD - DEPRECATED, MANTIDO PARA COMPATIBILIDADE
// ============================================

@Composable
@Deprecated("Use ElevatedCard instead", ReplaceWith("ElevatedCard()"))
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        variant = CardVariant.ELEVATED,
        modifier = modifier,
        content = content
    )
}

// ============================================
// ENHANCED WELCOME HEADER
// ============================================

@Composable
fun EnhancedWelcomeHeader(
    greeting: String,
    userName: String,
    todayDate: String,
    weatherState: WeatherUiState,
    isLoadingWeather: Boolean,
    prontidaoInfo: ProntidaoInfo,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "weather_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glow"
    )
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "scale"
    )

    ElevatedCard(
        variant = CardVariant.ELEVATED,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$greeting, $userName 👋",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = FireColors.OnBackground
                )
                Surface(
                    shape = CircleShape,
                    color = FireColors.Primary.copy(alpha = 0.10f),
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onRefresh() }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 1.5.dp,
                                color = FireColors.Primary
                            )
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Atualizar",
                                tint = FireColors.Primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(prontidaoInfo.corHex).copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(Color(prontidaoInfo.corHex), CircleShape)
                        )
                        Text(
                            text = "Prontidão ${prontidaoInfo.cor}",
                            fontSize = 11.sp,
                            color = Color(prontidaoInfo.corHex),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = todayDate,
                    fontSize = 12.sp,
                    color = FireColors.OnSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            ElevatedCard(
                variant = CardVariant.ELEVATED,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier.size(60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF6A3DE8).copy(alpha = glowAlpha * 0.55f),
                                            Color(0xFF3D9BE8).copy(alpha = glowAlpha * 0.25f),
                                            Color.Transparent
                                        )
                                    ),
                                    CircleShape
                                )
                        )
                        Text(
                            text = if (isLoadingWeather) "⏳" else weatherState.conditionIcon,
                            fontSize = (28 * iconScale).sp,
                            modifier = Modifier.graphicsLayer { scaleX = iconScale; scaleY = iconScale }
                        )
                        if (isLoadingWeather) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(44.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF6A3DE8).copy(alpha = 0.5f)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = weatherState.city.replace("/", ", "),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = FireColors.OnBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = java.time.LocalDate.now()
                                .format(java.time.format.DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("pt-BR")))
                                .replaceFirstChar { it.uppercase() },
                            fontSize = 10.sp,
                            color = FireColors.OnSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            WeatherDetailChip("💧", weatherState.humidity.replace("💧 ", ""), Color(0xFF4FC3F7))
                            WeatherDetailChip("🌬️", weatherState.windSpeed.replace("🌬️ ", ""), Color(0xFF90CAF9))
                            WeatherDetailChip("☁️", weatherState.rainChance.replace("🌧️ ", ""), Color(0xFF80CBC4))
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = if (isLoadingWeather) "--°C" else weatherState.temperature,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = FireColors.OnBackground
                        )
                        Text(
                            text = if (isLoadingWeather) "..." else weatherState.getTimeAgo(),
                            fontSize = 9.sp,
                            color = FireColors.OnSurfaceVariant.copy(alpha = 0.55f)
                        )
                        if (!isLoadingWeather && weatherState.error == null) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(FireColors.Success, CircleShape)
                                    .align(Alignment.End)
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.padding(top = 2.dp, start = 6.dp)
            ) {
                Box(modifier = Modifier.size(6.dp).background(FireColors.Success, CircleShape))
                Text(
                    text = "Online",
                    fontSize = 10.sp,
                    color = FireColors.Success,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ============================================
// WEATHER DETAIL CHIP
// ============================================

@Composable
fun WeatherDetailChip(
    icon: String,
    text: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = icon, fontSize = 10.sp)
            Text(
                text = text,
                fontSize = 10.sp,
                color = color,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false
            )
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
// STAT CARD
// ============================================

@Composable
fun StatCard(
    label: String,
    value: Int,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        variant = CardVariant.ELEVATED,
        modifier = modifier,
        icon = icon,
        iconTint = color
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = value.toString(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = FireColors.OnSurfaceVariant,
                letterSpacing = 0.3.sp
            )
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

// ============================================
// DETALHE E AUXILIARES DE CÁLCULO DE PRONTIDÃO
// ============================================

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

// ============================================
// DATA CLASS INTERNA PARA ESTILO DO CARD
// ============================================

private data class CardStyle(
    val borderColor: Color,
    val borderWidth: Dp,
    val backgroundColor: Color,
    val cornerRadius: Dp,
    val shadowElevation: Dp,
    val contentPadding: PaddingValues
)