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
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.firenotes.domain.model.NaturezaOcorrencia
import com.example.firenotes.domain.model.Ocorrencia
import com.example.firenotes.ui.designsystem.colors.FireColors
import com.example.firenotes.ui.designsystem.typography.FireTypography
import com.example.firenotes.data.service.ProntidaoService
import com.example.firenotes.data.service.ProntidaoService.ProntidaoInfo
import com.example.firenotes.data.service.ProntidaoService.Prontidao
import com.example.firenotes.data.local.entities.RoomTarefa
import com.example.firenotes.data.local.entities.RoomEventoAgenda
import com.example.firenotes.data.local.entities.RoomProntidaoDia
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ============================================
// ENUMERADOR DE TIPOS DE VISUALIZAÇÃO DO CALENDÁRIO
// ============================================
enum class CalendarViewType {
    MONTH, WEEK, DAY
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
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val weatherState by viewModel.weatherState.collectAsState()
    val isLoadingWeather by viewModel.isLoadingWeather.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    
    // Estados do Calendário Local
    val selectedDate by viewModel.selectedDate.collectAsState()
    val currentMonthDate by viewModel.currentMonth.collectAsState()
    val allTarefas by viewModel.allTarefas.collectAsState()
    val allEventos by viewModel.allEventos.collectAsState()
    val allProntidoes by viewModel.allProntidoes.collectAsState()
    
    var calendarViewType by remember { mutableStateOf(CalendarViewType.MONTH) }
    var showAddEventDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val userName = remember(context) { getDeviceOwnerName(context) }
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    val greeting = remember {
        val hour = LocalTime.now().hour
        when {
            hour < 12 -> "Bom dia"
            hour < 18 -> "Boa tarde"
            else -> "Boa noite"
        }
    }

    val todayDate = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("pt-BR")))
    }

    var showCityDialog by remember { mutableStateOf(false) }
    var inputCity by remember { mutableStateOf("") }

    // Animação de pulso para o fundo
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
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(FireColors.Primary, FireColors.Primary.copy(alpha = 0.6f))
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🚒", fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Fire Notes",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = FireColors.OnBackground
                        )
                    }
                },
                actions = {
                    Surface(
                        shape = CircleShape,
                        color = if (isRefreshing) FireColors.Warning.copy(alpha = 0.2f) else FireColors.Success.copy(alpha = 0.2f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = FireColors.Warning
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(FireColors.Success, CircleShape)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    IconButton(onClick = { viewModel.refreshAll() }) {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = "Atualizar",
                            tint = FireColors.OnSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FireColors.Surface.copy(alpha = 0.8f)
                ),
                modifier = Modifier
                    .shadow(4.dp, RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                    .background(FireColors.Surface.copy(alpha = 0.8f))
            )
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
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Warning,
                                contentDescription = null,
                                tint = FireColors.Error,
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = "Ops! Algo deu errado",
                                style = FireTypography.HeadlineSmall,
                                color = FireColors.OnBackground
                            )
                            Text(
                                text = state.message,
                                style = FireTypography.BodyMedium,
                                color = FireColors.OnSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { viewModel.loadOccurrences() },
                                colors = ButtonDefaults.buttonColors(containerColor = FireColors.Primary),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Outlined.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Tentar Novamente", fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
                
                is HomeUiState.Success -> {
                    val list = state.occurrences
                    val totalOcorrencias = list.size
                    val totalViaturas = list.sumOf { it.viaturas.size }
                    val totalVitimas = list.sumOf { it.vitimas.size }
                    val totalVeiculos = list.sumOf { it.veiculos.size }

                    // Processamento local de dados do calendário
                    val occurrencesMap = remember(list) {
                        list.groupBy { it.dataHora.atZone(ZoneId.systemDefault()).toLocalDate() }
                    }
                    val tasksMap = remember(allTarefas) {
                        allTarefas.groupBy { LocalDate.parse(it.data) }
                    }
                    val eventsMap = remember(allEventos) {
                        allEventos.groupBy { LocalDate.parse(it.data) }
                    }

                    if (showCityDialog) {
                        CityDialog(
                            currentCity = weatherState.city.takeIf { it != "Carregando..." } ?: "",
                            onConfirm = { cityName ->
                                viewModel.fetchWeatherForCity(cityName)
                                showCityDialog = false
                            },
                            onDismiss = { showCityDialog = false }
                        )
                    }

                    if (showAddEventDialog) {
                        AddEventDialog(
                            selectedDate = selectedDate,
                            onConfirm = { title, desc, start, end ->
                                viewModel.addEvento(title, desc, selectedDate, start, end)
                                showAddEventDialog = false
                            },
                            onDismiss = { showAddEventDialog = false }
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
                        // 1. Clima e Bem-vindo Header
                        item {
                            GradientWelcomeHeader(
                                greeting = greeting,
                                userName = userName,
                                todayDate = todayDate,
                                weatherState = weatherState,
                                isLoadingWeather = isLoadingWeather,
                                prontidaoInfo = remember(selectedDate, allProntidoes) {
                                    calcularProntidaoInfoParaData(selectedDate, allProntidoes)
                                },
                                onCityClick = {
                                    inputCity = weatherState.city.takeIf { it != "Carregando..." } ?: ""
                                    showCityDialog = true
                                },
                                isRefreshing = isRefreshing
                            )
                        }

                        // 2. Abas do Centro Operacional
                        item {
                            CenterOperationalTabs(
                                currentType = calendarViewType,
                                onTypeSelected = { calendarViewType = it }
                            )
                        }

                        // 3. Calendário Dinâmico / Agenda
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
                                            onProntidaoChanged = { escala -> viewModel.setProntidaoDia(selectedDate, escala) },
                                            onAddTask = { titulo -> viewModel.addTarefa(titulo, selectedDate) },
                                            onToggleTask = { task -> viewModel.toggleTarefa(task) },
                                            onDeleteTask = { id -> viewModel.deleteTarefa(id) },
                                            onAddEventClick = { showAddEventDialog = true },
                                            onDeleteEvent = { id -> viewModel.deleteEvento(id) },
                                            onNavigateToDetails = onNavigateToDetails
                                        )
                                    }
                                }
                            }
                        }

                        // 4. Detalhes do Dia Selecionado (Exibido abaixo do Calendário Mensal e Semanal)
                        if (calendarViewType != CalendarViewType.DAY) {
                            item {
                                Text(
                                    text = "📌 Detalhes de: " + selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                    style = FireTypography.Title,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            
                            item {
                                DailyOperationalView(
                                    selectedDate = selectedDate,
                                    allProntidoes = allProntidoes,
                                    occurrences = occurrencesMap[selectedDate] ?: emptyList(),
                                    tasks = tasksMap[selectedDate] ?: emptyList(),
                                    events = eventsMap[selectedDate] ?: emptyList(),
                                    onProntidaoChanged = { escala -> viewModel.setProntidaoDia(selectedDate, escala) },
                                    onAddTask = { titulo -> viewModel.addTarefa(titulo, selectedDate) },
                                    onToggleTask = { task -> viewModel.toggleTarefa(task) },
                                    onDeleteTask = { id -> viewModel.deleteTarefa(id) },
                                    onAddEventClick = { showAddEventDialog = true },
                                    onDeleteEvent = { id -> viewModel.deleteEvento(id) },
                                    onNavigateToDetails = onNavigateToDetails
                                )
                            }
                        }

                        // 5. Seção de Estatísticas do Painel
                        item {
                            ModernStatisticsSection(
                                totalOcorrencias = totalOcorrencias,
                                totalViaturas = totalViaturas,
                                totalVitimas = totalVitimas,
                                totalVeiculos = totalVeiculos
                            )
                        }

                        // 6. Rodapé
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "•  Fire Notes Operational Center MVP  •",
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
// COMPONENTES DO CALENDÁRIO LOCAL
// ============================================

@Composable
fun CenterOperationalTabs(
    currentType: CalendarViewType,
    onTypeSelected: (CalendarViewType) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FireColors.Surface.copy(alpha = 0.8f)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
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
                    CalendarViewType.MONTH -> "Visão Mensal"
                    CalendarViewType.WEEK -> "Visão Semanal"
                    CalendarViewType.DAY -> "Visão Diária"
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
        val dayOfWeek = firstDayOfMonth.dayOfWeek.value // 1 (Mon) to 7 (Sun)
        val firstDayOfWeek = dayOfWeek % 7 // Sun=0, Mon=1, ..., Sat=6
        
        val prevMonth = currentMonth.minusMonths(1)
        val daysInPrevMonth = prevMonth.lengthOfMonth()
        
        // Pad previous month days
        for (i in firstDayOfWeek - 1 downTo 0) {
            list.add(prevMonth.withDayOfMonth(daysInPrevMonth - i))
        }
        
        // Current month days
        val daysInCurrentMonth = currentMonth.lengthOfMonth()
        for (i in 1..daysInCurrentMonth) {
            list.add(currentMonth.withDayOfMonth(i))
        }
        
        // Pad next month days to fit 35 or 42 grids
        val totalCells = if (list.size <= 35) 35 else 42
        val nextMonthDaysNeeded = totalCells - list.size
        for (i in 1..nextMonthDaysNeeded) {
            list.add(currentMonth.plusMonths(1).withDayOfMonth(i))
        }
        list
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Month Selector Header
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

            // Week Days Label Header
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

            // Days Grid
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
            
            // Legend
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
                width = if (isSelected) 2.5.dp else if (isToday) 1.5.dp else 1.dp,
                color = if (isSelected) FireColors.Primary else if (isToday) FireColors.Primary.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f * alpha),
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
            
            // Indicators
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
        val dayOfWeek = selectedDate.dayOfWeek.value // 1 (Mon) to 7 (Sun)
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
            
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FireColors.Surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) FireColors.Primary else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onDateSelected(date) }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
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
fun DailyOperationalView(
    selectedDate: LocalDate,
    allProntidoes: List<RoomProntidaoDia>,
    occurrences: List<Ocorrencia>,
    tasks: List<RoomTarefa>,
    events: List<RoomEventoAgenda>,
    onProntidaoChanged: (String) -> Unit,
    onAddTask: (String) -> Unit,
    onToggleTask: (RoomTarefa) -> Unit,
    onDeleteTask: (String) -> Unit,
    onAddEventClick: () -> Unit,
    onDeleteEvent: (String) -> Unit,
    onNavigateToDetails: (id: String) -> Unit
) {
    var newTaskTitle by remember { mutableStateOf("") }
    
    val currentProntidao = remember(selectedDate, allProntidoes) {
        getProntidaoNameForDate(selectedDate, allProntidoes)
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🚒 Escala Operacional",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = FireColors.OnBackground
                )
                
                // Selector Row for Readiness Scale
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("AMARELA", "AZUL", "VERDE").forEach { scaleName ->
                        val isSelected = scaleName == currentProntidao
                        val color = when (scaleName) {
                            "AMARELA" -> Color(0xFFFFC107)
                            "AZUL" -> Color(0xFF2196F3)
                            else -> Color(0xFF4CAF50)
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) color else color.copy(alpha = 0.2f))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color.White else color.copy(alpha = 0.4f),
                                    shape = CircleShape
                                )
                                .clickable { onProntidaoChanged(scaleName) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Ocorrências do Dia
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🚑 Ocorrências do Dia (${occurrences.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = FireColors.OnBackground
                    )
                }
                
                if (occurrences.isEmpty()) {
                    Text("Sem ocorrências registradas hoje.", fontSize = 12.sp, color = FireColors.OnSurfaceVariant.copy(alpha = 0.7f))
                } else {
                    occurrences.forEach { ocorrencia ->
                        PremiumOccurrenceCard(
                            ocorrencia = ocorrencia,
                            onClick = { ocorrencia.id?.let(onNavigateToDetails) }
                        )
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Agenda / Eventos
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📅 Agenda e Compromissos (${events.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = FireColors.OnBackground
                    )
                    
                    IconButton(onClick = onAddEventClick, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Adicionar Evento", tint = FireColors.Primary)
                    }
                }
                
                if (events.isEmpty()) {
                    Text("Nenhum evento registrado hoje.", fontSize = 12.sp, color = FireColors.OnSurfaceVariant.copy(alpha = 0.7f))
                } else {
                    events.forEach { event ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = FireColors.Surface.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = event.titulo, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FireColors.OnBackground)
                                    if (!event.descricao.isNullOrBlank()) {
                                        Text(text = event.descricao, fontSize = 11.sp, color = FireColors.OnSurfaceVariant)
                                    }
                                    Text(
                                        text = "🕒 ${event.horaInicio ?: "08:00"} - ${event.horaFim ?: "Retorno"}",
                                        fontSize = 10.sp,
                                        color = FireColors.Primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                IconButton(onClick = { onDeleteEvent(event.id) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = FireColors.Error.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Checklist de Tarefas
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "📋 Checklist Operacional",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = FireColors.OnBackground
                )
                
                // Quick add task row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        placeholder = { Text("Adicionar item...", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FireColors.Primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    IconButton(
                        onClick = {
                            if (newTaskTitle.isNotBlank()) {
                                onAddTask(newTaskTitle)
                                newTaskTitle = ""
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(FireColors.Primary, RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Adicionar", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (tasks.isEmpty()) {
                    Text("Nenhuma tarefa cadastrada.", fontSize = 12.sp, color = FireColors.OnSurfaceVariant.copy(alpha = 0.7f))
                } else {
                    tasks.forEach { task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (task.concluida) FireColors.Secondary.copy(alpha = 0.05f) else Color.Transparent)
                                .clickable { onToggleTask(task) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Checkbox(
                                    checked = task.concluida,
                                    onCheckedChange = { onToggleTask(task) },
                                    colors = CheckboxDefaults.colors(checkedColor = FireColors.Secondary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = task.titulo,
                                    fontSize = 13.sp,
                                    color = if (task.concluida) FireColors.OnSurfaceVariant.copy(alpha = 0.6f) else FireColors.OnBackground,
                                    fontWeight = if (task.concluida) FontWeight.Normal else FontWeight.Medium
                                )
                            }
                            IconButton(onClick = { onDeleteTask(task.id) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = FireColors.Error.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

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
// DIÁLOGOS DE ENTRADA DO USUÁRIO
// ============================================

@Composable
fun CityDialog(
    currentCity: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var inputCity by remember { mutableStateOf(currentCity) }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Column(
                modifier = Modifier.padding(20.dp),
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
        GlassCard(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
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

// ============================================
// COMPONENTES PREMIUM (REUTILIZADOS)
// ============================================

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        FireColors.Surface.copy(alpha = 0.85f),
                        FireColors.Surface.copy(alpha = 0.7f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

@Composable
fun GradientWelcomeHeader(
    greeting: String,
    userName: String,
    todayDate: String,
    weatherState: WeatherUiState,
    isLoadingWeather: Boolean,
    prontidaoInfo: ProntidaoInfo,
    onCityClick: () -> Unit,
    isRefreshing: Boolean
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$greeting, $userName 👋",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = FireColors.OnBackground
                    )
                    Text(
                        text = todayDate,
                        fontSize = 13.sp,
                        color = FireColors.OnSurfaceVariant
                    )
                }
                
                Surface(
                    shape = CircleShape,
                    color = if (isRefreshing) FireColors.Warning.copy(alpha = 0.15f) else FireColors.Success.copy(alpha = 0.15f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (isRefreshing) FireColors.Warning else FireColors.Success,
                                    CircleShape
                                )
                        )
                    }
                }
            }

            HorizontalDivider(
                color = FireColors.OnSurface.copy(alpha = 0.06f),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            WeatherCard(
                weatherState = weatherState,
                isLoading = isLoadingWeather,
                onCityClick = onCityClick
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = FireColors.Success.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(FireColors.Success, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Online",
                            fontSize = 11.sp,
                            color = FireColors.Success,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(prontidaoInfo.corHex).copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(prontidaoInfo.corHex), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Prontidão ${prontidaoInfo.cor}",
                            fontSize = 11.sp,
                            color = if (prontidaoInfo.corHex == 0xFFFFC107) Color(0xFF6B4C00) else Color(prontidaoInfo.corHex),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherCard(
    weatherState: WeatherUiState,
    isLoading: Boolean,
    onCityClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clickable { onCityClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FireColors.Surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(FireColors.Primary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = FireColors.Primary
                        )
                    } else {
                        Text(text = weatherState.conditionIcon, fontSize = 24.sp)
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = weatherState.city,
                            style = FireTypography.Title,
                            fontWeight = FontWeight.Bold,
                            color = FireColors.Primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Alterar cidade",
                            tint = FireColors.Primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    
                    if (weatherState.error != null) {
                        Text(
                            text = "⚠️ ${weatherState.error}",
                            fontSize = 12.sp,
                            color = FireColors.Error
                        )
                    } else {
                        Text(
                            text = weatherState.condition,
                            style = FireTypography.BodyMedium,
                            color = FireColors.OnSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .horizontalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = weatherState.humidity,
                                fontSize = 11.sp,
                                color = FireColors.OnSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                            Text(
                                text = "•",
                                color = FireColors.OnSurfaceVariant.copy(alpha = 0.3f),
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = weatherState.windSpeed,
                                fontSize = 11.sp,
                                color = FireColors.OnSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                            Text(
                                text = "•",
                                color = FireColors.OnSurfaceVariant.copy(alpha = 0.3f),
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = weatherState.rainChance,
                                fontSize = 11.sp,
                                color = FireColors.OnSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = weatherState.temperature,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = FireColors.Primary
                )
                Text(
                    text = if (isLoading) "⏳ Atualizando..." else "Agora",
                    fontSize = 9.sp,
                    color = FireColors.OnSurfaceVariant.copy(alpha = 0.5f),
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun ModernStatisticsSection(
    totalOcorrencias: Int,
    totalViaturas: Int,
    totalVitimas: Int,
    totalVeiculos: Int
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "📊 Resumo Geral do Plantão",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = FireColors.OnBackground
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatCard(
                    label = "Ocorrências",
                    value = totalOcorrencias,
                    icon = Icons.Default.List,
                    color = FireColors.Primary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Viaturas",
                    value = totalViaturas,
                    icon = Icons.Outlined.LocalFireDepartment,
                    color = Color(0xFFFF6B35),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatCard(
                    label = "Vítimas",
                    value = totalVitimas,
                    icon = Icons.Outlined.People,
                    color = Color(0xFF9C27B0),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Veículos",
                    value = totalVeiculos,
                    icon = Icons.Outlined.DirectionsCar,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: Int,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.shadow(2.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.06f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
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
}

@Composable
fun PremiumOccurrenceCard(
    ocorrencia: Ocorrencia,
    onClick: () -> Unit
) {
    val natureColor = when (ocorrencia.natureza) {
        NaturezaOcorrencia.INCENDIO -> FireColors.Primary
        NaturezaOcorrencia.SALVAMENTO -> Color(0xFF4CAF50)
        NaturezaOcorrencia.ACIDENTE_TRANSITO -> Color(0xFFFF9800)
        NaturezaOcorrencia.QUEDA -> Color(0xFF8B5A2B)
        NaturezaOcorrencia.PESSOAL -> Color(0xFF9C27B0)
    }

    val prontidao = remember(ocorrencia.dataHora) {
        ProntidaoService.getProntidaoForInstant(ocorrencia.dataHora)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FireColors.Surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .background(natureColor, RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

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
                                color = if (prontidao.cor == Color(0xFFFFB300)) Color(0xFF6B4C00) else prontidao.cor,
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