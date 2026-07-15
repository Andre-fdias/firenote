package com.example.firenotes.ui.screens.occurrence.components

import androidx.compose.ui.text.style.TextAlign
import com.example.firenotes.ui.designsystem.components.cards.FireCard
import com.example.firenotes.ui.designsystem.components.widgets.FireDivider
import com.example.firenotes.ui.screens.occurrence.models.OccurrenceModule
import com.example.firenotes.ui.screens.occurrence.utils.*
import com.example.firenotes.ui.screens.occurrence.OccurrenceFormUiState
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.*
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocumentosModuleView(
    uiState: OccurrenceFormUiState,
    onNewDocClick: () -> Unit,
    onScanDocClick: () -> Unit, // Mantido no contrato de assinatura para evitar erros de compilação
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
                            text = "📄",
                            fontSize = 28.sp,
                            modifier = Modifier.padding(end = FireSpacing.Small)
                        )
                        Column {
                            Text(
                                text = "Documentos",
                                style = FireTypography.Headline,
                                fontWeight = FontWeight.ExtraBold,
                                color = FireColors.Primary
                            )
                            Text(
                                text = "Pessoas envolvidas identificadas",
                                style = FireTypography.LabelMedium,
                                color = FireColors.OnSurfaceVariant
                            )
                        }
                    }

                    // Botão de ação rápida integrado ao cabeçalho (UX de fácil alcance)
                    FilledTonalButton(
                        onClick = onNewDocClick,
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
                            text = "Novo",
                            style = FireTypography.LabelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                FireGalleryCard(
                    title = "Anexos de Documentos de Identificação",
                    category = "Documento",
                    images = galleryImages,
                    onImageClick = onImageClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (uiState.documentos.isEmpty()) {
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
                                text = "🪪",
                                fontSize = 48.sp
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Nenhum documento anexado",
                                    style = FireTypography.Title,
                                    fontWeight = FontWeight.Bold,
                                    color = FireColors.OnSurfaceVariant
                                )
                                Text(
                                    text = "Cadastre manualmente o RG, CPF ou CNH das testemunhas e civis envolvidos.",
                                    style = FireTypography.BodyMedium,
                                    color = FireColors.OnSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            } else {

                items(
                    items = uiState.documentos,
                    key = { it.id ?: it.numero ?: java.util.UUID.randomUUID().toString() }
                ) { doc ->
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
                                            text = doc.tipo.uppercase(),
                                            style = FireTypography.LabelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = FireColors.Primary
                                        )
                                    }
                                }

                                Text(
                                    text = "Nº ${doc.numero ?: "N/D"}",
                                    style = FireTypography.LabelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = FireColors.OnSurfaceVariant
                                )
                            }

                            FireDivider(
                                modifier = Modifier.padding(vertical = FireSpacing.ExtraSmall)
                            )

                            // Informações da pessoa vinculada ao documento
                            val ownerName = doc.dadosEstruturados["nome"]
                            val ownerCpf = doc.dadosEstruturados["cpf"]

                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (!ownerName.isNullOrBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                                    ) {
                                        Text("👤", fontSize = 14.sp)
                                        Text(
                                            text = ownerName.uppercase(),
                                            style = FireTypography.BodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = FireColors.OnSurface
                                        )
                                    }
                                }
                                if (!ownerCpf.isNullOrBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                                    ) {
                                        Text("🪪", fontSize = 14.sp)
                                        Text(
                                            text = "CPF: $ownerCpf",
                                            style = FireTypography.BodyMedium,
                                            color = FireColors.OnSurfaceVariant
                                        )
                                    }
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