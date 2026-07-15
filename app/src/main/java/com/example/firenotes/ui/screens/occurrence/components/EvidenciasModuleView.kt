package com.example.firenotes.ui.screens.occurrence.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firenotes.ui.designsystem.colors.FireColors
import com.example.firenotes.ui.designsystem.components.buttons.FireButton
import com.example.firenotes.ui.designsystem.components.widgets.FireGalleryCard
import com.example.firenotes.ui.designsystem.components.widgets.GalleryImage
import com.example.firenotes.ui.designsystem.icons.FireIcons
import com.example.firenotes.ui.designsystem.spacing.FireSpacing
import com.example.firenotes.ui.designsystem.typography.FireTypography
import com.example.firenotes.ui.screens.occurrence.OccurrenceFormUiState

@Composable
fun EvidenciasModuleView(
    uiState: OccurrenceFormUiState,
    onTakePhoto: () -> Unit,
    galleryImages: List<GalleryImage>,
    onImageClick: (GalleryImage) -> Unit,
    onBack: () -> Unit
) {
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
                    text = "📷",
                    fontSize = 28.sp,
                    modifier = Modifier.padding(end = FireSpacing.Small)
                )
                Column {
                    Text(
                        text = "Evidências e Provas",
                        style = FireTypography.Headline,
                        fontWeight = FontWeight.ExtraBold,
                        color = FireColors.Primary
                    )
                    Text(
                        text = "Gerencie o registro fotográfico e provas coletadas",
                        style = FireTypography.LabelMedium,
                        color = FireColors.OnSurfaceVariant
                    )
                }
            }

            // Action Button
            FireButton(
                onClick = onTakePhoto,
                text = "TIRAR FOTO (EVIDÊNCIA)",
                icon = FireIcons.PhotoCamera,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )

            // Gallery
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                FireGalleryCard(
                    title = "Evidências Registradas",
                    category = "Evidência",
                    images = galleryImages,
                    onImageClick = onImageClick,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(FireSpacing.ExtraSmall))

            // Back Action
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