package com.example.firenotes.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firenotes.domain.model.NaturezaOcorrencia
import com.example.firenotes.domain.model.Ocorrencia
import com.example.firenotes.ui.designsystem.colors.FireColor
import com.example.firenotes.ui.designsystem.spacing.FireSpacing
import com.example.firenotes.ui.designsystem.typography.FireTypography
import com.example.firenotes.ui.designsystem.icons.FireIcons
import com.example.firenotes.ui.designsystem.components.cards.FireCard
import com.example.firenotes.ui.designsystem.components.cards.FireStatCard
import com.example.firenotes.ui.designsystem.components.cards.FireOccurrenceCard
import com.example.firenotes.ui.designsystem.components.topbar.FireTopBar
import com.example.firenotes.ui.designsystem.states.FireLoading
import com.example.firenotes.ui.designsystem.states.FireErrorState
import com.example.firenotes.ui.designsystem.states.FireEmptyState
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToWizard: () -> Unit,
    onNavigateToDetails: (id: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val userName = remember(context) { getDeviceOwnerName(context) }

    val greeting = remember {
        val hour = LocalTime.now().hour
        when {
            hour < 12 -> "Bom dia"
            hour < 18 -> "Boa tarde"
            else -> "Boa noite"
        }
    }

    val todayDate = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM"))
    }

    Scaffold(
        topBar = {
            FireTopBar(title = "Fire Notes")
        },
        containerColor = Color(0xFFF5F5F5), // Background cinza claro como nos templates
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF5F5F5))
        ) {
            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    FireLoading(modifier = Modifier.align(Alignment.Center))
                }
                is HomeUiState.Error -> {
                    FireErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadOccurrences() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is HomeUiState.Success -> {
                    val list = state.occurrences
                    val totalOcorrencias = list.size
                    val totalViaturas = list.sumOf { it.viaturas.size }
                    val totalVitimas = list.sumOf { it.vitimas.size }
                    val totalVeiculos = list.sumOf { it.veiculos.size }

                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Greeting Header - Estilo Template
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(
                                        elevation = 4.dp,
                                        shape = RoundedCornerShape(12.dp),
                                        clip = false
                                    ),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "$greeting, $userName 👋",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1A1A1A)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = todayDate,
                                        fontSize = 14.sp,
                                        color = Color(0xFF757575)
                                    )
                                    // Badge de status estilo template
                                    Surface(
                                        modifier = Modifier.padding(top = 8.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        color = FireColor.Primary.copy(alpha = 0.1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(FireColor.Primary, RoundedCornerShape(50))
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Sistema Online",
                                                fontSize = 12.sp,
                                                color = FireColor.Primary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Botão Nova Ocorrência - Estilo Template
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp)
                                    .shadow(
                                        elevation = 6.dp,
                                        shape = RoundedCornerShape(12.dp),
                                        clip = false
                                    ),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = FireColor.Primary
                                ),
                                onClick = onNavigateToWizard
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = FireIcons.AddAlert,
                                            contentDescription = "Nova Ocorrência",
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "NOVA OCORRÊNCIA",
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Seção de Estatísticas - Cards estilo Template
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(
                                        elevation = 2.dp,
                                        shape = RoundedCornerShape(12.dp),
                                        clip = false
                                    ),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "📊 Resumo Geral",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1A1A1A)
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        StatItem(
                                            label = "Ocorrências",
                                            value = totalOcorrencias,
                                            icon = "📋",
                                            modifier = Modifier.weight(1f)
                                        )
                                        StatItem(
                                            label = "Viaturas",
                                            value = totalViaturas,
                                            icon = "🚒",
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        StatItem(
                                            label = "Vítimas",
                                            value = totalVitimas,
                                            icon = "👥",
                                            modifier = Modifier.weight(1f)
                                        )
                                        StatItem(
                                            label = "Veículos",
                                            value = totalVeiculos,
                                            icon = "🚗",
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        // Título de Ocorrências Recentes
                        item {
                            Text(
                                text = "📌 Últimas Ocorrências",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1A1A),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        if (list.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(
                                            elevation = 2.dp,
                                            shape = RoundedCornerShape(12.dp),
                                            clip = false
                                        ),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.White
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "📭",
                                                fontSize = 48.sp
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Nenhuma ocorrência registrada",
                                                fontSize = 16.sp,
                                                color = Color(0xFF757575)
                                            )
                                            Text(
                                                text = "Clique em 'NOVA OCORRÊNCIA' para começar",
                                                fontSize = 14.sp,
                                                color = Color(0xFFBDBDBD)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            items(list.take(5), key = { it.id ?: it.protocolo }) { ocorrencia ->
                                OccurrenceCardItem(
                                    ocorrencia = ocorrencia,
                                    onClick = { ocorrencia.id?.let(onNavigateToDetails) }
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
private fun StatItem(
    label: String,
    value: Int,
    icon: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .shadow(
                elevation = 1.dp,
                shape = RoundedCornerShape(8.dp),
                clip = false
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFAFAFA)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = icon,
                fontSize = 20.sp
            )
            Text(
                text = value.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color(0xFF757575)
            )
        }
    }
}

@Composable
private fun OccurrenceCardItem(
    ocorrencia: Ocorrencia,
    onClick: () -> Unit
) {
    val natureColor = when (ocorrencia.natureza) {
        NaturezaOcorrencia.INCENDIO -> FireColor.Primary
        NaturezaOcorrencia.SALVAMENTO -> Color(0xFF4CAF50)
        NaturezaOcorrencia.ACIDENTE_TRANSITO -> Color(0xFFFF9800)
        NaturezaOcorrencia.QUEDA -> Color(0xFF8B5A2B)
        NaturezaOcorrencia.PESSOAL -> Color(0xFF9C27B0)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(12.dp),
                clip = false
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicador de natureza
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = natureColor,
                        shape = RoundedCornerShape(50)
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = ocorrencia.natureza.descricao,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1A1A1A)
                    )
                    Text(
                        text = ocorrencia.protocolo,
                        fontSize = 12.sp,
                        color = Color(0xFF757575)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = ocorrencia.cidade ?: "",
                        fontSize = 13.sp,
                        color = Color(0xFF616161)
                    )
                    Text(
                        text = "•",
                        color = Color(0xFFBDBDBD)
                    )
                    Text(
                        text = ocorrencia.dataHora.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                        fontSize = 13.sp,
                        color = Color(0xFF616161)
                    )
                }
            }

            Icon(
                imageVector = FireIcons.ChevronRight,
                contentDescription = null,
                tint = Color(0xFFBDBDBD),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

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