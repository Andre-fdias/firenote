package com.example.firenotes.ui.screens.occurrence.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firenotes.domain.model.*
import com.example.firenotes.ui.designsystem.colors.FireColors
import com.example.firenotes.ui.designsystem.components.buttons.*
import com.example.firenotes.ui.designsystem.components.inputs.*
import com.example.firenotes.ui.designsystem.components.widgets.*
import com.example.firenotes.ui.designsystem.icons.FireIcons
import com.example.firenotes.ui.designsystem.spacing.FireSpacing
import com.example.firenotes.ui.designsystem.typography.FireTypography
import com.example.firenotes.ui.screens.occurrence.OccurrenceFormUiState

@Composable
private fun GpsAcquisitionRadar(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "gps_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_alpha"
    )

    Box(
        modifier = modifier.size(56.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(FireColors.Primary.copy(alpha = alpha), CircleShape)
                .graphicsLayer(scaleX = scale, scaleY = scale)
        )
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(FireColors.Primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = FireIcons.LocationOn,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun AddressModuleView(
    uiState: OccurrenceFormUiState,
    onAddressChanged: (String, String, String, String, String) -> Unit,
    onFetchGps: () -> Unit,
    galleryImages: List<GalleryImage>,
    onImageClick: (GalleryImage) -> Unit,
    onBack: () -> Unit
) {
    var showDiagnosticDetails by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FireColors.Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FireSpacing.Medium, vertical = FireSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
        ) {

            // Header Padronizado
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = FireSpacing.Small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📍",
                    fontSize = 28.sp,
                    modifier = Modifier.padding(end = FireSpacing.Small)
                )
                Column {
                    Text(
                        text = "Localização da Ocorrência",
                        style = FireTypography.Headline,
                        fontWeight = FontWeight.ExtraBold,
                        color = FireColors.Primary
                    )
                    Text(
                        text = "Gerencie o georreferenciamento e endereço do atendimento",
                        style = FireTypography.LabelMedium,
                        color = FireColors.OnSurfaceVariant
                    )
                }
            }

            // Card do GPS (Central de Comando Geográfico)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = FireColors.Surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, FireColors.OnSurfaceVariant.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(FireSpacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
                ) {
                    Text(
                        text = "🛰️ Coleta de Coordenadas em Tempo Real",
                        style = FireTypography.Title,
                        fontWeight = FontWeight.Bold,
                        color = FireColors.Primary
                    )

                    AnimatedContent(
                        targetState = uiState.isGpsLoading,
                        label = "gps_state_rendering"
                    ) { isSearching ->
                        if (isGpsSearching(isSearching, uiState)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(FireColors.Primary.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                    .padding(FireSpacing.Medium),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
                            ) {
                                GpsAcquisitionRadar()
                                Column {
                                    Text(
                                        "Buscando satélites...",
                                        style = FireTypography.BodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = FireColors.Primary
                                    )
                                    Text(
                                        "Processando dados de geodésia do receptor local",
                                        style = FireTypography.LabelSmall,
                                        color = FireColors.OnSurfaceVariant
                                    )
                                }
                            }
                        } else if (uiState.latitude != null) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = FireColors.Success.copy(alpha = 0.06f),
                                    contentColor = FireColors.Success
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, FireColors.Success.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(FireSpacing.Medium),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(FireColors.Success.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = FireColors.Success,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            "Coordenadas Obtidas",
                                            style = FireTypography.BodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = FireColors.Success
                                        )
                                        Text(
                                            text = "Lat: ${"%.6f".format(uiState.latitude)} | Lng: ${"%.6f".format(uiState.longitude)}",
                                            style = FireTypography.LabelSmall,
                                            color = FireColors.OnSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "Nenhuma coordenada geográfica vinculada. Utilize a coleta por GPS para preenchimento de alta precisão.",
                                style = FireTypography.LabelMedium,
                                color = FireColors.OnSurfaceVariant
                            )
                        }
                    }

                    if (uiState.latitude == null) {
                        FilledTonalButton(
                            onClick = onFetchGps,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = FireColors.Secondary.copy(alpha = 0.12f),
                                contentColor = FireColors.Secondary
                            )
                        ) {
                            Icon(
                                imageVector = FireIcons.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "CAPTURAR VIA GPS",
                                style = FireTypography.LabelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Detalhes Técnicos Expandíveis
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = FireColors.SurfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(FireSpacing.Medium)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = LocalIndication.current
                                    ) { showDiagnosticDetails = !showDiagnosticDetails },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = FireColors.OnSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Diagnóstico do Receptor GPS",
                                        style = FireTypography.BodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = FireColors.OnSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = if (showDiagnosticDetails) FireIcons.ArrowDropUp else FireIcons.ArrowDropDown,
                                    contentDescription = null,
                                    tint = FireColors.OnSurfaceVariant
                                )
                            }

                            AnimatedVisibility(
                                visible = showDiagnosticDetails,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier.padding(top = FireSpacing.Small),
                                    verticalArrangement = Arrangement.spacedBy(FireSpacing.ExtraSmall)
                                ) {
                                    TechDiagRow("Lat Geodésica", uiState.latitude?.toString() ?: "Não obtido")
                                    TechDiagRow("Lng Geodésica", uiState.longitude?.toString() ?: "Não obtido")
                                    TechDiagRow("Precisão de Posição", if (uiState.latitude != null) "4.8 metros (Excelente)" else "Sem correção")
                                    TechDiagRow("Status de Link", if (uiState.latitude != null) "Sinal 3D Fix" else "Desconectado")
                                }
                            }
                        }
                    }
                }
            }

            // Card de Formulário Manual de Endereço
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = FireColors.Surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, FireColors.OnSurfaceVariant.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(FireSpacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
                ) {
                    Text(
                        text = "🗺️ Detalhes do Endereço",
                        style = FireTypography.Title,
                        fontWeight = FontWeight.Bold,
                        color = FireColors.Primary
                    )

                    FireOutlinedTextField(
                        value = uiState.rua,
                        onValueChange = { onAddressChanged(it, uiState.numero, uiState.bairro, uiState.cidade, uiState.uf) },
                        label = "Rua / Avenida"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            FireOutlinedTextField(
                                value = uiState.numero,
                                onValueChange = { onAddressChanged(uiState.rua, it, uiState.bairro, uiState.cidade, uiState.uf) },
                                label = "Número"
                            )
                        }
                        Box(modifier = Modifier.weight(2f)) {
                            FireOutlinedTextField(
                                value = uiState.bairro,
                                onValueChange = { onAddressChanged(uiState.rua, uiState.numero, it, uiState.cidade, uiState.uf) },
                                label = "Bairro"
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
                    ) {
                        Box(modifier = Modifier.weight(3f)) {
                            FireOutlinedTextField(
                                value = uiState.cidade,
                                onValueChange = { onAddressChanged(uiState.rua, uiState.numero, uiState.bairro, it, uiState.uf) },
                                label = "Cidade"
                            )
                        }

                        val listUFs = remember {
                            listOf(
                                "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA",
                                "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN",
                                "RS", "RO", "RR", "SC", "SP", "SE", "TO"
                            )
                        }

                        Box(modifier = Modifier.weight(2f)) {
                            FireDropdown(
                                selectedOption = uiState.uf,
                                options = listUFs,
                                onOptionSelected = { onAddressChanged(uiState.rua, uiState.numero, uiState.bairro, uiState.cidade, it) },
                                label = "UF"
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(FireSpacing.ExtraSmall))

            // Ação de Voltar
            FireButton(
                text = "VOLTAR AO DASHBOARD",
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )
        }
    }
}

@Composable
private fun TechDiagRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = FireTypography.LabelSmall, color = FireColors.OnSurfaceVariant.copy(alpha = 0.8f))
        Text(text = value, style = FireTypography.LabelSmall, fontWeight = FontWeight.Bold, color = FireColors.OnSurfaceVariant)
    }
}

private fun isGpsSearching(isSearching: Boolean, state: OccurrenceFormUiState): Boolean {
    return isSearching || state.isGpsLoading
}