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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VeiculosModuleView(
    uiState: OccurrenceFormUiState,
    onNewVehicleClick: () -> Unit,
    onScanCrlvClick: () -> Unit,
    onEditVehicleClick: (VeiculoEnvolvido) -> Unit,
    onDeleteVehicleClick: (VeiculoEnvolvido) -> Unit,
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
                            text = "🚗",
                            fontSize = 28.sp,
                            modifier = Modifier.padding(end = FireSpacing.Small)
                        )
                        Column {
                            Text(
                                text = "Veículos",
                                style = FireTypography.Headline,
                                fontWeight = FontWeight.ExtraBold,
                                color = FireColors.Primary
                            )
                            Text(
                                text = "Veículos de terceiros e envolvidos",
                                style = FireTypography.LabelMedium,
                                color = FireColors.OnSurfaceVariant
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Scan CRLV button
                        FilledTonalButton(
                            onClick = onScanCrlvClick,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = FireColors.Success.copy(alpha = 0.12f),
                                contentColor = FireColors.Success
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(
                                imageVector = FireIcons.PhotoCamera,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Scanear CRLV",
                                style = FireTypography.LabelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Novo button
                        FilledTonalButton(
                            onClick = onNewVehicleClick,
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
            }

            item {
                FireGalleryCard(
                    title = "Galeria de CRLVs e Anexos",
                    category = "Veículo",
                    images = galleryImages,
                    onImageClick = onImageClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (uiState.veiculos.isEmpty()) {
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
                                text = "🚗",
                                fontSize = 48.sp
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Nenhum veículo registrado",
                                    style = FireTypography.Title,
                                    fontWeight = FontWeight.Bold,
                                    color = FireColors.OnSurfaceVariant
                                )
                                Text(
                                    text = "Cadastre carros, motocicletas ou caminhões civis envolvidos para associar proprietários ou condutores.",
                                    style = FireTypography.BodyMedium,
                                    color = FireColors.OnSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            FireButton(
                                text = "ADICIONAR VEÍCULO",
                                onClick = onNewVehicleClick,
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(48.dp)
                            )
                        }
                    }
                }
            } else {
                items(
                    items = uiState.veiculos,
                    key = { it.id ?: it.placa ?: java.util.UUID.randomUUID().toString() }
                ) { veiculo ->
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
                                // Badge estilizado simulando placa Mercosul real para leitura rápida
                                Column(
                                    modifier = Modifier
                                        .border(1.5.dp, Color(0xFF263238), RoundedCornerShape(6.dp))
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.White)
                                        .width(130.dp)
                                ) {
                                    // Tarja azul Mercosul
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF0D47A1))
                                            .padding(vertical = 1.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "BRASIL",
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                    // Dígitos da Placa
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = (veiculo.placa ?: "SEM PLACA").uppercase(),
                                            color = Color.Black,
                                            style = FireTypography.Title,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(FireColors.Primary.copy(alpha = 0.08f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "ANO ${veiculo.ano ?: veiculo.anoModelo ?: "N/D"}",
                                        style = FireTypography.LabelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = FireColors.Primary
                                    )
                                }
                            }

                            Text(
                                text = "${veiculo.marca?.uppercase() ?: ""} ${veiculo.modelo?.uppercase() ?: "MODELO NÃO ESPECIFICADO"}",
                                style = FireTypography.BodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = FireColors.OnSurface,
                                modifier = Modifier.padding(start = 2.dp, top = 2.dp)
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
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🎨", fontSize = 12.sp, modifier = Modifier.padding(end = 4.dp))
                                        Text(
                                            text = "COR: ",
                                            style = FireTypography.LabelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = FireColors.OnSurfaceVariant
                                        )
                                        Text(
                                            text = (veiculo.cor ?: "N/D").uppercase(),
                                            style = FireTypography.BodySmall,
                                            color = FireColors.OnSurface
                                        )
                                    }

                                    if (!veiculo.chassi.isNullOrBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("🆔", fontSize = 12.sp, modifier = Modifier.padding(end = 4.dp))
                                            Text(
                                                text = "CHASSI: ",
                                                style = FireTypography.LabelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = FireColors.OnSurfaceVariant
                                            )
                                            Text(
                                                text = veiculo.chassi.uppercase(),
                                                style = FireTypography.BodySmall,
                                                fontFamily = FontFamily.Monospace,
                                                color = FireColors.OnSurface
                                            )
                                        }
                                    }
                                }
                            }

                            val ownerName = veiculo.ocrDadosEstruturados["proprietario_nome"]
                            val ownerCpf = veiculo.ocrDadosEstruturados["proprietario_cpf"]

                            if (!ownerName.isNullOrBlank() || !ownerCpf.isNullOrBlank()) {
                                Column(
                                    modifier = Modifier.padding(start = 2.dp, top = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "PROPRIETÁRIO VINCULADO",
                                        style = FireTypography.LabelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = FireColors.OnSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                    if (!ownerName.isNullOrBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("👤", fontSize = 12.sp, modifier = Modifier.padding(end = 6.dp))
                                            Text(
                                                text = ownerName.uppercase(),
                                                style = FireTypography.BodySmall,
                                                color = FireColors.OnSurface
                                            )
                                        }
                                    }
                                    if (!ownerCpf.isNullOrBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("🪪", fontSize = 12.sp, modifier = Modifier.padding(end = 6.dp))
                                            Text(
                                                text = "CPF: $ownerCpf",
                                                style = FireTypography.BodySmall,
                                                color = FireColors.OnSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            FireDivider(
                                modifier = Modifier.padding(vertical = FireSpacing.ExtraSmall)
                            )

                            // CRUD Actions Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { onEditVehicleClick(veiculo) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = FireColors.Primary)
                                ) {
                                    Icon(
                                        imageVector = FireIcons.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Editar", style = FireTypography.LabelMedium)
                                }
                                Spacer(modifier = Modifier.width(FireSpacing.Small))
                                TextButton(
                                    onClick = { onDeleteVehicleClick(veiculo) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = FireColors.Error)
                                ) {
                                    Icon(
                                        imageVector = FireIcons.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Excluir", style = FireTypography.LabelMedium)
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
