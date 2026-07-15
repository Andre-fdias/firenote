package com.example.firenotes.ui.screens.occurrence.cards

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.firenotes.ui.designsystem.colors.FireColors
import com.example.firenotes.ui.designsystem.components.cards.FireCard
import com.example.firenotes.ui.designsystem.components.widgets.FireStatusChip
import com.example.firenotes.ui.designsystem.spacing.FireSpacing
import com.example.firenotes.ui.designsystem.typography.FireTypography

@Composable
fun SummaryCard(
    protocolo: String,
    natureza: String,
    cidade: String,
    tempoOcorrencia: String,
    veiculosCount: Int,
    vitimasCount: Int,
    viaturasCount: Int,
    prontidao: String? = null,
    modifier: Modifier = Modifier
) {
    FireCard(
        containerColor = FireColors.Surface,
        elevation = 4.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(FireSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)
        ) {
            Text(
                text = "📋 Resumo da Ocorrência",
                style = FireTypography.Title,
                fontWeight = FontWeight.Bold,
                color = FireColors.Primary
            )
            HorizontalDivider()

            InfoRow("Talão", protocolo)
            InfoRow("Natureza", natureza)
            InfoRow("Cidade", cidade)
            InfoRow("Duração", tempoOcorrencia)
            if (!prontidao.isNullOrBlank()) {
                InfoRow("Prontidão de Serviço", prontidao)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = FireSpacing.Small),
                horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small)
            ) {
                FireStatusChip(text = "🚗 $veiculosCount", backgroundColor = FireColors.PrimaryLight, textColor = FireColors.Primary)
                FireStatusChip(text = "🩺 $vitimasCount", backgroundColor = FireColors.SecondaryLight, textColor = FireColors.Secondary)
                FireStatusChip(text = "🚒 $viaturasCount", backgroundColor = FireColors.TertiaryLight, textColor = FireColors.PrimaryDark)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = FireTypography.LabelMedium, color = FireColors.OnSurfaceVariant)
        Text(text = value, style = FireTypography.BodyMedium, fontWeight = FontWeight.Medium)
    }
}
