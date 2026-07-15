package com.example.firenotes.ui.screens.occurrence.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firenotes.domain.model.ApoioOcorrencia
import com.example.firenotes.ui.designsystem.components.widgets.GalleryImage
import com.example.firenotes.domain.model.OrgaoApoio
import com.example.firenotes.ui.designsystem.colors.FireColors
import com.example.firenotes.ui.designsystem.components.buttons.FireButton
import com.example.firenotes.ui.designsystem.components.buttons.FireOutlinedButton
import com.example.firenotes.ui.designsystem.components.inputs.FireOutlinedTextField
import com.example.firenotes.ui.designsystem.components.widgets.FireGalleryCard
import com.example.firenotes.ui.designsystem.components.widgets.FireSectionHeader
import com.example.firenotes.ui.designsystem.icons.FireIcons
import com.example.firenotes.ui.designsystem.spacing.FireSpacing
import com.example.firenotes.ui.designsystem.typography.FireTypography
import com.example.firenotes.ui.screens.occurrence.OccurrenceFormUiState

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ApoiosModuleView(
    uiState: OccurrenceFormUiState,
    onAddApoio: (OrgaoApoio, String, String) -> Unit,
    onRemoveApoio: (Int) -> Unit,
    galleryImages: List<GalleryImage>,
    onImageClick: (GalleryImage) -> Unit,
    onBack: () -> Unit
) {
    var selectedOrgaoIndex by remember { mutableStateOf(0) }
    var viatura by remember { mutableStateOf("") }
    var encarregado by remember { mutableStateOf("") }
    var expandedDropdown by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FireColors.Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = FireSpacing.Medium, vertical = FireSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = FireSpacing.Small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🤝",
                    fontSize = 28.sp,
                    modifier = Modifier.padding(end = FireSpacing.Small)
                )
                Column {
                    Text(
                        text = "Órgãos de Apoio",
                        style = FireTypography.Headline,
                        fontWeight = FontWeight.ExtraBold,
                        color = FireColors.Primary
                    )
                    Text(
                        text = "Vincule organizações e recursos externos de cooperação",
                        style = FireTypography.LabelMedium,
                        color = FireColors.OnSurfaceVariant
                    )
                }
            }

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
                    verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                ) {
                    Text(
                        text = "🤝 Novo Vínculo de Apoio",
                        style = FireTypography.Title,
                        fontWeight = FontWeight.Bold,
                        color = FireColors.Primary
                    )

                    if (uiState.orgaosDisponiveis.isNotEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            FireOutlinedButton(
                                text = uiState.orgaosDisponiveis[selectedOrgaoIndex].sigla,
                                onClick = { expandedDropdown = true },
                                modifier = Modifier.fillMaxWidth(),
                                icon = FireIcons.ArrowDropDown
                            )
                            DropdownMenu(
                                expanded = expandedDropdown,
                                onDismissRequest = { expandedDropdown = false },
                                modifier = Modifier.background(FireColors.Surface)
                            ) {
                                uiState.orgaosDisponiveis.forEachIndexed { idx, org ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = org.sigla,
                                                style = FireTypography.BodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                        },
                                        onClick = {
                                            selectedOrgaoIndex = idx
                                            expandedDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            FireOutlinedTextField(
                                value = viatura,
                                onValueChange = { viatura = it.uppercase() },
                                label = "Prefixo Viatura"
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            FireOutlinedTextField(
                                value = encarregado,
                                onValueChange = { encarregado = it.uppercase() },
                                label = "Nome Encarregado"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    FireButton(
                        text = "VINCULAR ÓRGÃO DE APOIO",
                        onClick = {
                            if (uiState.orgaosDisponiveis.isNotEmpty()) {
                                onAddApoio(
                                    uiState.orgaosDisponiveis[selectedOrgaoIndex],
                                    viatura.trim(),
                                    encarregado.trim()
                                )
                                viatura = ""
                                encarregado = ""
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    )
                }
            }

            if (uiState.apoiosDetalhados.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(
                            color = FireColors.SurfaceVariant.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = FireColors.OnSurfaceVariant.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                    ) {
                        Text("🤝", fontSize = 48.sp)
                        Text(
                            text = "Nenhum órgão de apoio registrado.",
                            style = FireTypography.BodyMedium,
                            color = FireColors.OnSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(FireSpacing.Small),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    itemsIndexed(
                        items = uiState.apoiosDetalhados,
                        key = { _, item -> item.orgao.id + item.viatura + item.encarregado }
                    ) { idx, apoio ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = FireColors.Surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, FireColors.OnSurfaceVariant.copy(alpha = 0.06f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(FireSpacing.Medium),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = FireColors.Primary.copy(alpha = 0.08f),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = apoio.orgao.sigla,
                                            style = FireTypography.LabelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = FireColors.Primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = apoio.orgao.nome,
                                        style = FireTypography.BodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = FireColors.OnSurface
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Text(
                                            text = "🚒 Vtr: ${apoio.viatura.ifBlank { "N/A" }}  •  👤 Encarregado: ${apoio.encarregado.ifBlank { "N/A" }}",
                                            style = FireTypography.Caption,
                                            color = FireColors.OnSurfaceVariant
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = { onRemoveApoio(idx) },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = FireColors.Error
                                    ),
                                    border = BorderStroke(1.dp, FireColors.Error.copy(alpha = 0.25f)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(
                                        imageVector = FireIcons.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Remover",
                                        style = FireTypography.LabelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (galleryImages.any { it.category == "Apoio" }) {
                FireGalleryCard(
                    title = "Galeria de Apoios",
                    category = "Apoio",
                    images = galleryImages,
                    onImageClick = onImageClick,
                    modifier = Modifier.heightIn(max = 140.dp)
                )
            }

            Spacer(modifier = Modifier.height(FireSpacing.ExtraSmall))

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