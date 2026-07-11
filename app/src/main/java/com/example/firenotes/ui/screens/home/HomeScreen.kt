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
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

// ============================================
// ANIMAÇÕES GLOBAIS
// ============================================

private val shimmerColors = listOf(
    Color.White.copy(alpha = 0.0f),
    Color.White.copy(alpha = 0.5f),
    Color.White.copy(alpha = 0.0f)
)

private val pulseGradient = Brush.radialGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.2f),
        Color.White.copy(alpha = 0.0f)
    ),
    center = Offset(0.5f, 0.5f),
    radius = 0.5f
)

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
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy"))
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
                        // Logo animado
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
                            Text("🔥", fontSize = 16.sp)
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
                    // Indicador de status
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
                                        .animateContentSize()
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
                            modifier = Modifier
                                .size(24.dp)
                                .rotate(if (isRefreshing) 360f else 0f)
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
            // Fundo com gradiente animado
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
                            // Loading com animação de ondulação
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(
                                        FireColors.Primary.copy(alpha = 0.1f),
                                        CircleShape
                                    )
                                    .then(
                                        Modifier
                                            .size(80.dp + 40.dp * pulseAnim)
                                            .background(
                                                FireColors.Primary.copy(alpha = 0.05f),
                                                CircleShape
                                            )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = FireColors.Primary,
                                    modifier = Modifier.size(48.dp),
                                    strokeWidth = 4.dp
                                )
                            }
                            Text(
                                text = "Carregando ocorrências...",
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
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FireColors.Primary
                                ),
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

                    // Diálogo de cidade - Design Glassmorphism
                    if (showCityDialog) {
                        Dialog(
                            onDismissRequest = { showCityDialog = false },
                            properties = DialogProperties(
                                usePlatformDefaultWidth = false,
                                decorFitsSystemWindows = false
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .clickable { showCityDialog = false },
                                contentAlignment = Alignment.Center
                            ) {
                                GlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp)
                                        .clickable(enabled = false) { }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Outlined.LocationOn,
                                                    contentDescription = null,
                                                    tint = FireColors.Primary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "📍 Localidade",
                                                    style = FireTypography.HeadlineSmall,
                                                    color = FireColors.OnBackground
                                                )
                                            }
                                            IconButton(onClick = { showCityDialog = false }) {
                                                Icon(
                                                    Icons.Outlined.Close,
                                                    contentDescription = "Fechar",
                                                    tint = FireColors.OnSurfaceVariant
                                                )
                                            }
                                        }
                                        
                                        Text(
                                            text = "Digite o nome da cidade para atualizar a previsão do tempo",
                                            style = FireTypography.BodyMedium,
                                            color = FireColors.OnSurfaceVariant
                                        )
                                        
                                        OutlinedTextField(
                                            value = inputCity,
                                            onValueChange = { inputCity = it },
                                            label = { Text("Cidade/UF") },
                                            placeholder = { Text("Ex: Sorocaba/SP") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = FireColors.Primary,
                                                unfocusedBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.3f),
                                                focusedLabelColor = FireColors.Primary
                                            )
                                        )
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(onClick = { showCityDialog = false }) {
                                                Text("Cancelar", color = FireColors.OnSurfaceVariant)
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(
                                                onClick = {
                                                    if (inputCity.isNotBlank()) {
                                                        viewModel.fetchWeatherForCity(inputCity)
                                                    }
                                                    showCityDialog = false
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = FireColors.Primary
                                                ),
                                                shape = RoundedCornerShape(16.dp)
                                            ) {
                                                Icon(Icons.Outlined.Check, contentDescription = null)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Confirmar")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(
                            horizontal = if (isTablet) 32.dp else 16.dp,
                            vertical = 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Header com gradiente
                        item {
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(animationSpec = tween(600)) + 
                                        slideInVertically(initialOffsetY = { -it / 2 }),
                                label = "welcomeHeader"
                            ) {
                                GradientWelcomeHeader(
                                    greeting = greeting,
                                    userName = userName,
                                    todayDate = todayDate,
                                    weatherState = weatherState,
                                    isLoadingWeather = isLoadingWeather,
                                    prontidaoInfo = remember { ProntidaoService.calcularProntidao() },
                                    onCityClick = {
                                        inputCity = weatherState.city.takeIf { 
                                            it != "Carregando..."
                                        } ?: ""
                                        showCityDialog = true
                                    },
                                    isRefreshing = isRefreshing
                                )
                            }
                        }

                        // Botão Nova Ocorrência com gradiente
                        item {
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(animationSpec = tween(600, delayMillis = 100)) +
                                        slideInVertically(initialOffsetY = { 50 }),
                                label = "newOccurrenceButton"
                            ) {
                                GradientNewOccurrenceButton(
                                    onClick = onNavigateToWizard
                                )
                            }
                        }

                        // Estatísticas com design moderno
                        item {
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(animationSpec = tween(600, delayMillis = 200)) +
                                        slideInVertically(initialOffsetY = { 100 }),
                                label = "statisticsSection"
                            ) {
                                ModernStatisticsSection(
                                    totalOcorrencias = totalOcorrencias,
                                    totalViaturas = totalViaturas,
                                    totalVitimas = totalVitimas,
                                    totalVeiculos = totalVeiculos
                                )
                            }
                        }

                        // Título da lista com decoração
                        item {
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(animationSpec = tween(400, delayMillis = 300)),
                                label = "listHeader"
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .background(FireColors.Primary, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Últimas Ocorrências",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = FireColors.OnBackground
                                        )
                                    }
                                    if (list.isNotEmpty()) {
                                        Text(
                                            text = "Ver todas →",
                                            fontSize = 14.sp,
                                            color = FireColors.Primary,
                                            modifier = Modifier.clickable { /* Navigate */ }
                                        )
                                    }
                                }
                            }
                        }

                        // Lista de Ocorrências
                        if (list.isEmpty()) {
                            item {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn(animationSpec = tween(600, delayMillis = 400)),
                                    label = "emptyState"
                                ) {
                                    ModernEmptyState()
                                }
                            }
                        } else {
                            items(
                                items = list.take(10),
                                key = { it.id ?: it.protocolo }
                            ) { ocorrencia ->
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn(animationSpec = tween(400, delayMillis = 200 * list.indexOf(ocorrencia) % 3)) +
                                            slideInVertically(initialOffsetY = { 50 }),
                                    label = "occurrenceCard"
                                ) {
                                    PremiumOccurrenceCard(
                                        ocorrencia = ocorrencia,
                                        onClick = { ocorrencia.id?.let(onNavigateToDetails) }
                                    )
                                }
                            }
                        }

                        // Rodapé
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "•",
                                    fontSize = 12.sp,
                                    color = FireColors.OnSurfaceVariant.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Fire Notes v2.0",
                                    fontSize = 12.sp,
                                    color = FireColors.OnSurfaceVariant.copy(alpha = 0.4f),
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "•",
                                    fontSize = 12.sp,
                                    color = FireColors.OnSurfaceVariant.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${list.size} ocorrências",
                                    fontSize = 12.sp,
                                    color = FireColors.OnSurfaceVariant.copy(alpha = 0.4f)
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
// COMPONENTES PREMIUM
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
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
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
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Saudação com gradiente
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
                
                // Badge de status animado
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
                                .animateContentSize()
                        )
                    }
                }
            }

            HorizontalDivider(
                color = FireColors.OnSurface.copy(alpha = 0.06f),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Clima com design robusto e responsivo
            WeatherCard(
                weatherState = weatherState,
                isLoading = isLoadingWeather,
                onCityClick = onCityClick
            )

            // Badges com design moderno
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Status Online
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

                // Prontidão
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

                // Horário
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = FireColors.OnSurface.copy(alpha = 0.05f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🕐 ${prontidaoInfo.horaInicio} - ${prontidaoInfo.horaFim}",
                            fontSize = 10.sp,
                            color = FireColors.OnSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// HomeScreen.kt - Weather Card
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
        colors = CardDefaults.cardColors(
            containerColor = FireColors.Surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                // Ícone do clima
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            FireColors.Primary.copy(alpha = 0.1f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = FireColors.Primary
                        )
                    } else {
                        Text(
                            text = weatherState.conditionIcon,
                            fontSize = 24.sp
                        )
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
fun GradientNewOccurrenceButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            FireColors.Primary,
                            FireColors.Primary.copy(alpha = 0.8f),
                            Color(0xFF7C4DFF)
                        )
                    )
                )
        ) {
            // Efeito de brilho animado
            val infiniteTransition = rememberInfiniteTransition(label = "shine")
            val shineAnim by infiniteTransition.animateFloat(
                initialValue = -1f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "shine"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.1f),
                                Color.White.copy(alpha = 0.0f)
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = (shineAnim * 2f * 100).dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.0f),
                                Color.White.copy(alpha = 0.15f),
                                Color.White.copy(alpha = 0.0f)
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = "Nova Ocorrência",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "NOVA OCORRÊNCIA",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
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
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "📊 Resumo Geral",
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
                    icon = Icons.Outlined.ListAlt,
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
        modifier = modifier
            .shadow(2.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.06f)
        )
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
fun ModernEmptyState() {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(FireColors.Primary.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📭",
                    fontSize = 40.sp
                )
            }
            Text(
                text = "Nenhuma ocorrência",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = FireColors.OnBackground
            )
            Text(
                text = "Clique em 'NOVA OCORRÊNCIA' para começar",
                fontSize = 14.sp,
                color = FireColors.OnSurfaceVariant,
                textAlign = TextAlign.Center
            )
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
        colors = CardDefaults.cardColors(
            containerColor = FireColors.Surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicador de natureza com barra lateral
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