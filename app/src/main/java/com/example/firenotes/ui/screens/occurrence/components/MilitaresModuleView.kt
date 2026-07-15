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
fun MilitaresModuleView(
    uiState: OccurrenceFormUiState,
    onAddMilitarClick: (String) -> Unit,
    onDeleteMilitar: (String, String) -> Unit,
    onMoveMilitarClick: (Militar) -> Unit,
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
                            text = "👨‍🚒",
                            fontSize = 28.sp,
                            modifier = Modifier.padding(end = FireSpacing.Small)
                        )
                        Column {
                            Text(
                                text = "Militares",
                                style = FireTypography.Headline,
                                fontWeight = FontWeight.ExtraBold,
                                color = FireColors.Primary
                            )
                            Text(
                                text = "Guarnição e escala operacional das viaturas",
                                style = FireTypography.LabelMedium,
                                color = FireColors.OnSurfaceVariant
                            )
                        }
                    }
                }
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
                                    text = "Você precisa cadastrar pelo menos uma viatura operacional no módulo anterior antes de escalar os militares e as equipes de prontidão.",
                                    style = FireTypography.BodyMedium,
                                    color = FireColors.OnSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            FireButton(
                                text = "VOLTAR AO DASHBOARD",
                                onClick = onBack,
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
                                            fontSize = 15.sp
                                        )
                                    }
                                    Text(
                                        text = "GUARNIÇÃO",
                                        style = FireTypography.LabelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = FireColors.OnSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }

                                /* Botão de ação premium para escala rápida (Glove-Friendly) */
                                FilledTonalButton(
                                    onClick = { onAddMilitarClick(viatura.id!!) },
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
                                        text = "Escalar",
                                        style = FireTypography.LabelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            FireDivider(
                                modifier = Modifier.padding(vertical = FireSpacing.ExtraSmall)
                            )

                            if (viatura.equipe.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = FireColors.SurfaceVariant.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Nenhum bombeiro escalado nesta viatura.",
                                        style = FireTypography.BodyMedium,
                                        color = FireColors.OnSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            } else {
                                viatura.equipe.forEach { mil ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                color = FireColors.SurfaceVariant.copy(alpha = 0.25f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .padding(FireSpacing.Medium),
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
                                                Text("👨‍🚒", fontSize = 16.sp)

                                                val rankLabel = try {
                                                    mil.graduacao.toString().uppercase()
                                                } catch(e: Exception) {
                                                    "MILITAR"
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(FireColors.Primary.copy(alpha = 0.08f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = rankLabel,
                                                        style = FireTypography.LabelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = FireColors.Primary
                                                    )
                                                }

                                                Text(
                                                    text = mil.nomeGuerra.uppercase(),
                                                    style = FireTypography.BodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = FireColors.OnSurface
                                                )
                                            }

                                            Text(
                                                text = "RE ${mil.re}",
                                                style = FireTypography.LabelSmall,
                                                fontFamily = FontFamily.Monospace,
                                                color = FireColors.OnSurfaceVariant
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("📋", fontSize = 12.sp, modifier = Modifier.padding(end = 4.dp))
                                                Text(
                                                    text = "FUNÇÃO: ${(mil.funcao ?: "EQUIPE").uppercase()}",
                                                    style = FireTypography.LabelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = FireColors.Secondary
                                                )
                                            }

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                FilledTonalButton(
                                                    onClick = { onMoveMilitarClick(mil) },
                                                    colors = ButtonDefaults.filledTonalButtonColors(
                                                        containerColor = FireColors.Secondary.copy(alpha = 0.08f),
                                                        contentColor = FireColors.Secondary
                                                    ),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Text(
                                                        text = "Mover",
                                                        style = FireTypography.LabelSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }

                                                OutlinedButton(
                                                    onClick = { onDeleteMilitar(mil.id!!, viatura.id!!) },
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
                                                        modifier = Modifier.size(12.dp)
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