package com.example.firenotes.ui.screens.occurrence.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.firenotes.ui.designsystem.colors.FireColors
import com.example.firenotes.ui.designsystem.components.buttons.FireOutlinedButton
import com.example.firenotes.ui.designsystem.components.widgets.GalleryImage
import com.example.firenotes.ui.designsystem.icons.FireIcons
import com.example.firenotes.ui.designsystem.spacing.FireSpacing
import com.example.firenotes.ui.designsystem.typography.FireTypography
import com.example.firenotes.ui.screens.occurrence.OccurrenceFormUiState
import java.io.File

@Composable
fun AnexosModuleView(
    uiState: OccurrenceFormUiState,
    onAddMedia: () -> Unit,
    galleryImages: List<GalleryImage>,
    onImageClick: (GalleryImage) -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("Todas") }
    
    val filteredImages = remember(galleryImages, selectedTab) {
        if (selectedTab == "Todas") {
            galleryImages
        } else {
            galleryImages.filter { img ->
                when (selectedTab) {
                    "Documentos" -> img.category == "Documento"
                    "Evidências" -> img.category in listOf("Evidência", "Local", "Vítima")
                    "Veículos" -> img.category == "Veículo"
                    "Anexos" -> img.category == "Anexo"
                    else -> true
                }
            }
        }
    }

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
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = FireSpacing.Small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🖼️",
                    fontSize = 28.sp,
                    modifier = Modifier.padding(end = FireSpacing.Small)
                )
                Column {
                    Text(
                        text = "Galeria Geral",
                        style = FireTypography.Headline,
                        fontWeight = FontWeight.ExtraBold,
                        color = FireColors.Primary
                    )
                    Text(
                        text = "Visualização de todas as mídias registradas no atendimento",
                        style = FireTypography.LabelMedium,
                        color = FireColors.OnSurfaceVariant
                    )
                }
            }

            // Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tabs = listOf("Todas", "Documentos", "Evidências", "Veículos", "Anexos")
                tabs.forEach { tab ->
                    val isSelected = selectedTab == tab
                    val count = remember(galleryImages, tab) {
                        if (tab == "Todas") {
                            galleryImages.size
                        } else {
                            galleryImages.count { img ->
                                when (tab) {
                                    "Documentos" -> img.category == "Documento"
                                    "Evidências" -> img.category in listOf("Evidência", "Local", "Vítima")
                                    "Veículos" -> img.category == "Veículo"
                                    "Anexos" -> img.category == "Anexo"
                                    else -> false
                                }
                            }
                        }
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        label = { Text("$tab ($count)", style = FireTypography.LabelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FireColors.Primary,
                            selectedLabelColor = Color.White,
                            containerColor = FireColors.SurfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            // Gallery Grid
            if (filteredImages.isEmpty()) {
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
                        verticalArrangement = Arrangement.spacedBy(FireSpacing.Small),
                        modifier = Modifier.padding(FireSpacing.Medium)
                    ) {
                        Text("📷", fontSize = 48.sp)
                        Text(
                            text = "Nenhuma imagem encontrada",
                            style = FireTypography.BodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = FireColors.OnSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "As fotos cadastradas nos outros módulos (pessoas, evidências, veículos) aparecerão consolidadas aqui.",
                            style = FireTypography.LabelMedium,
                            color = FireColors.OnSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredImages) { image ->
                        Card(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable { onImageClick(image) },
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                val file = remember(image.path) { File(image.path) }
                                AsyncImage(
                                    model = file,
                                    contentDescription = image.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                
                                // Tag de Categoria no canto inferior
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = image.category,
                                        color = Color.White,
                                        style = FireTypography.LabelSmall,
                                        fontSize = 9.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(FireSpacing.ExtraSmall))

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