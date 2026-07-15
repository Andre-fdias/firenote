package com.example.firenotes.ui.screens.occurrence.cards

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.example.firenotes.ui.designsystem.colors.FireColors
import com.example.firenotes.ui.designsystem.components.cards.FireCard
import com.example.firenotes.ui.designsystem.spacing.FireSpacing
import com.example.firenotes.ui.designsystem.typography.FireTypography

@Composable
fun ChecklistCard(
    protocoloOk: Boolean,
    enderecoOk: Boolean,
    historicoOk: Boolean,
    viaturasOk: Boolean,
    modifier: Modifier = Modifier
) {
    FireCard(containerColor = FireColors.Surface, modifier = modifier) {
        Column(modifier = Modifier.padding(FireSpacing.Medium)) {
            Text("✅ Checklist Operacional", style = FireTypography.LabelLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(FireSpacing.ExtraSmall))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FireSpacing.ExtraSmall)
            ) {
                ChecklistItem("Talão", protocoloOk)
                Text("•", color = Color.Gray, style = FireTypography.BodySmall)
                ChecklistItem("Endereço", enderecoOk)
                Text("•", color = Color.Gray, style = FireTypography.BodySmall)
                ChecklistItem("Histórico", historicoOk)
                Text("•", color = Color.Gray, style = FireTypography.BodySmall)
                ChecklistItem("Viaturas", viaturasOk)
            }
        }
    }
}

@Composable
private fun ChecklistItem(label: String, ok: Boolean) {
    Text(
        text = "$label ${if (ok) "✅" else "⬜"}",
        color = if (ok) FireColors.Success else Color.Gray,
        style = FireTypography.BodySmall,
        fontWeight = if (ok) FontWeight.Bold else FontWeight.Normal
    )
}
