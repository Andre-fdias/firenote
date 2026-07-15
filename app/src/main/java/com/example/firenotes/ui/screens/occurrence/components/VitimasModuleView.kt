package com.example.firenotes.ui.screens.occurrence.components

import androidx.compose.ui.text.style.TextAlign
import com.example.firenotes.ui.designsystem.components.cards.FireCard
import com.example.firenotes.ui.designsystem.components.widgets.FireDivider
import com.example.firenotes.ui.screens.occurrence.models.OccurrenceModule
import com.example.firenotes.ui.screens.occurrence.utils.*
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VitimasModuleView(
    uiState: OccurrenceFormUiState,
    onNewVictimClick: () -> Unit,
    galleryImages: List<GalleryImage>,
    onImageClick: (GalleryImage) -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FireColors.Background)
            .padding(horizontal = FireSpacing.Medium, vertical = FireSpacing.Small)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = FireSpacing.Small),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "🩺",
                            fontSize = 28.sp,
                            modifier = Modifier.padding(end = FireSpacing.Small)
                        )
                        Column {
                            Text(
                                text = "Vítimas",
                                style = FireTypography.Headline,
                                fontWeight = FontWeight.ExtraBold,
                                color = FireColors.Primary
                            )
                            Text(
                                text = "Pessoas socorridas e triagem de APH",
                                style = FireTypography.LabelMedium,
                                color = FireColors.OnSurfaceVariant
                            )
                        }
                    }

                    // Botão de ação rápida integrado ao cabeçalho (UX limpa e de fácil alcance)
                    FilledTonalButton(
                        onClick = onNewVictimClick,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = FireColors.Primary.copy(alpha = 0.12f),
                            contentColor = FireColors.Primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(
                            imageVector = FireIcons.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Nova",
                            style = FireTypography.LabelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                FireGalleryCard(
                    title = "Galeria de Fichas e Fotos de Triagem",
                    category = "Vítima",
                    images = galleryImages,
                    onImageClick = onImageClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (uiState.vitimas.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = FireColors.SurfaceVariant.copy(alpha = 0.25f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, FireColors.OnSurfaceVariant.copy(alpha = 0.1f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = FireSpacing.Medium)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp, horizontal = FireSpacing.Large),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
                        ) {
                            Text(
                                text = "🩺",
                                fontSize = 48.sp
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Nenhuma vítima registrada",
                                    style = FireTypography.Title,
                                    fontWeight = FontWeight.Bold,
                                    color = FireColors.OnSurfaceVariant
                                )
                                Text(
                                    text = "Cadastre as pessoas socorridas, triagens de traumas, sinais vitais e hospitais de destino de urgência.",
                                    style = FireTypography.BodyMedium,
                                    color = FireColors.OnSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            FireButton(
                                text = "ADICIONAR VÍTIMA",
                                onClick = onNewVictimClick,
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(48.dp)
                            )
                        }
                    }
                }
            } else {
                items(
                    items = uiState.vitimas,
                    key = { it.id ?: (it.nome ?: java.util.UUID.randomUUID().toString()) }
                ) { vitima ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = FireColors.Surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, FireColors.OnSurfaceVariant.copy(alpha = 0.08f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                    ) {
                        Column(
                            modifier = Modifier.padding(FireSpacing.Medium),
                            verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                                ) {
                                    Text("👤", fontSize = 18.sp)
                                    Text(
                                        text = (vitima.nome ?: "NOME NÃO INFORMADO").uppercase(),
                                        style = FireTypography.Title,
                                        fontWeight = FontWeight.Bold,
                                        color = FireColors.OnSurface
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(FireColors.Primary.copy(alpha = 0.08f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${vitima.idade ?: "N/D"} ANOS",
                                        style = FireTypography.LabelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = FireColors.Primary
                                    )
                                }
                            }

                            FireDivider(
                                modifier = Modifier.padding(vertical = FireSpacing.ExtraSmall)
                            )

                            Column(
                                modifier = Modifier.padding(horizontal = 2.dp),
                                verticalArrangement = Arrangement.spacedBy(FireSpacing.ExtraSmall)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("🩹", fontSize = 14.sp, modifier = Modifier.padding(end = FireSpacing.Small, top = 2.dp))
                                    Column {
                                        Text(
                                            text = "LESÕES APARENTES:",
                                            style = FireTypography.LabelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = FireColors.OnSurfaceVariant
                                        )
                                        Text(
                                            text = (vitima.lesoesAparentes ?: "Nenhuma lesão visível relatada").uppercase(),
                                            style = FireTypography.BodySmall,
                                            color = FireColors.OnSurface
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("🚑", fontSize = 14.sp, modifier = Modifier.padding(end = FireSpacing.Small, top = 2.dp))
                                    Column {
                                        Text(
                                            text = "DESTINO E TRANSPORTE:",
                                            style = FireTypography.LabelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = FireColors.OnSurfaceVariant
                                        )
                                        val destino = vitima.hospitalDestino ?: vitima.destinoSocorro ?: "LIBERADO NO LOCAL"
                                        val transportador = if (!vitima.transportadoPor.isNullOrBlank()) " VIA ${vitima.transportadoPor.uppercase()}" else ""
                                        Text(
                                            text = "${destino.uppercase()}$transportador",
                                            style = FireTypography.BodySmall,
                                            color = FireColors.OnSurface
                                        )
                                    }
                                }
                            }

                            FireDivider(
                                modifier = Modifier.padding(vertical = FireSpacing.ExtraSmall)
                            )

                            Text(
                                text = "MONITORAMENTO DE SINAIS VITAIS (APH)",
                                style = FireTypography.LabelSmall,
                                fontWeight = FontWeight.Bold,
                                color = FireColors.OnSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = FireColors.SurfaceVariant.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(FireSpacing.Medium),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Glasgow
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🧠 GCS", style = FireTypography.LabelSmall, color = FireColors.OnSurfaceVariant, fontWeight = FontWeight.Bold)
                                    val gcs = vitima.sinaisVitais.escalaGCS
                                    val gcsColor = when {
                                        gcs == null -> FireColors.OnSurface
                                        gcs >= 13 -> FireColors.Success
                                        gcs >= 9 -> FireColors.Warning
                                        else -> FireColors.Error
                                    }
                                    Text(
                                        text = gcs?.toString() ?: "--",
                                        style = FireTypography.Title,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = gcsColor
                                    )
                                }

                                // Frequência Cardíaca
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("❤️ FC", style = FireTypography.LabelSmall, color = FireColors.OnSurfaceVariant, fontWeight = FontWeight.Bold)
                                    val fc = vitima.sinaisVitais.pulso
                                    val fcColor = when {
                                        fc == null -> FireColors.OnSurface
                                        fc in 60..100 -> FireColors.Success
                                        else -> FireColors.Warning
                                    }
                                    Text(
                                        text = if (fc != null) "$fc bpm" else "--",
                                        style = FireTypography.Title,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = fcColor
                                    )
                                }

                                // Pressão Arterial
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🩺 PA", style = FireTypography.LabelSmall, color = FireColors.OnSurfaceVariant, fontWeight = FontWeight.Bold)
                                    val pa = vitima.sinaisVitais.pressaoArterial
                                    Text(
                                        text = if (!pa.isNullOrBlank()) pa else "--",
                                        style = FireTypography.Title,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = FireColors.Primary
                                    )
                                }

                                // Saturação
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🫁 SpO₂", style = FireTypography.LabelSmall, color = FireColors.OnSurfaceVariant, fontWeight = FontWeight.Bold)
                                    val sat = vitima.sinaisVitais.saturacaoO2
                                    val satColor = when {
                                        sat == null -> FireColors.OnSurface
                                        sat >= 95 -> FireColors.Success
                                        sat >= 90 -> FireColors.Warning
                                        else -> FireColors.Error
                                    }
                                    Text(
                                        text = if (sat != null) "$sat%" else "--",
                                        style = FireTypography.Title,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = satColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(FireSpacing.Medium))
                FireOutlinedButton(
                    text = "VOLTAR AO DASHBOARD",
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )
            }
        }
    }
}