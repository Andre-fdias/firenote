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
fun ViaturasModuleView(
    uiState: OccurrenceFormUiState,
    onNewViaturaClick: () -> Unit,
    onEditViaturaClick: (Viatura) -> Unit,
    onDeleteViatura: (String) -> Unit,
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
                            text = "🚒",
                            fontSize = 28.sp,
                            modifier = Modifier.padding(end = FireSpacing.Small)
                        )
                        Column {
                            Text(
                                text = "Viaturas",
                                style = FireTypography.Headline,
                                fontWeight = FontWeight.ExtraBold,
                                color = FireColors.Primary
                            )
                            Text(
                                text = "Viaturas operacionais empenhadas",
                                style = FireTypography.LabelMedium,
                                color = FireColors.OnSurfaceVariant
                            )
                        }
                    }

                    // Botão de ação rápida premium integrado ao cabeçalho (UX otimizada)
                    FilledTonalButton(
                        onClick = onNewViaturaClick,
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
                    title = "Galeria de Fotos das Viaturas",
                    category = "Viatura",
                    images = galleryImages,
                    onImageClick = onImageClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (uiState.viaturas.isEmpty()) {
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
                                text = "🚒",
                                fontSize = 48.sp
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Nenhuma viatura registrada",
                                    style = FireTypography.Title,
                                    fontWeight = FontWeight.Bold,
                                    color = FireColors.OnSurfaceVariant
                                )
                                Text(
                                    text = "Cadastre a viatura utilizada no atendimento para associar a equipe e calcular quilometragem.",
                                    style = FireTypography.BodyMedium,
                                    color = FireColors.OnSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            FireButton(
                                text = "ADICIONAR VIATURA",
                                onClick = onNewViaturaClick,
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(48.dp)
                            )
                        }
                    }
                }
            } else {
                items(
                    items = uiState.viaturas,
                    key = { it.id ?: it.prefixo ?: java.util.UUID.randomUUID().toString() }
                ) { viatura ->
                    val startVal = viatura.kmSaida ?: 0
                    val endVal = viatura.kmLocal ?: 0
                    val diff = if (endVal >= startVal) endVal - startVal else 0

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
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(FireColors.Primary.copy(alpha = 0.1f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = viatura.prefixo.uppercase(),
                                            style = FireTypography.Title,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = FireColors.Primary,
                                            fontSize = 16.sp
                                        )
                                    }
                                }

                                val count = viatura.equipe.size
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("👥", fontSize = 14.sp)
                                    Text(
                                        text = "$count militar${if (count != 1) "es" else ""}",
                                        style = FireTypography.LabelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = FireColors.Secondary
                                    )
                                }
                            }

                            Text(
                                text = viatura.unidade?.uppercase() ?: "Batalhão / Seção não informada",
                                style = FireTypography.BodyMedium,
                                color = FireColors.OnSurfaceVariant,
                                modifier = Modifier.padding(start = 2.dp)
                            )

                            FireDivider(
                                modifier = Modifier.padding(vertical = FireSpacing.ExtraSmall)
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
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "REGISTRO DE QUILOMETRAGEM",
                                        style = FireTypography.LabelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = FireColors.OnSurfaceVariant
                                    )
                                    Text(
                                        text = "Saída: ${startVal} km  •  Quartel: ${endVal} km",
                                        style = FireTypography.BodyMedium,
                                        color = FireColors.OnSurface
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "PERCORRIDO",
                                        style = FireTypography.LabelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = FireColors.Primary
                                    )
                                    Text(
                                        text = "$diff KM",
                                        style = FireTypography.Title,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = FireColors.Primary
                                    )
                                }
                            }

                            if (!viatura.observacoes.isNullOrBlank()) {
                                Row(
                                    modifier = Modifier.padding(top = 4.dp, start = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("📝", fontSize = 14.sp)
                                    Text(
                                        text = viatura.observacoes,
                                        style = FireTypography.BodySmall,
                                        color = FireColors.OnSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            FireDivider(
                                modifier = Modifier.padding(vertical = FireSpacing.ExtraSmall)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalButton(
                                    onClick = { onEditViaturaClick(viatura) },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = FireColors.Primary.copy(alpha = 0.08f),
                                        contentColor = FireColors.Primary
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Icon(
                                        imageVector = FireIcons.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Editar",
                                        style = FireTypography.LabelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(FireSpacing.Small))

                                OutlinedButton(
                                    onClick = { onDeleteViatura(viatura.id!!) },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = FireColors.Error
                                    ),
                                    border = BorderStroke(1.dp, FireColors.Error.copy(alpha = 0.25f)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Icon(
                                        imageVector = FireIcons.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Remover",
                                        style = FireTypography.LabelMedium,
                                        fontWeight = FontWeight.Bold
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