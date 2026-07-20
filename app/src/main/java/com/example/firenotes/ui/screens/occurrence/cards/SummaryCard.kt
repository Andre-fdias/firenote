package com.example.firenotes.ui.screens.occurrence.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.firenotes.ui.designsystem.colors.FireColors
import com.example.firenotes.ui.designsystem.components.cards.FireCard
import com.example.firenotes.ui.designsystem.components.widgets.FireStatusChip
import com.example.firenotes.ui.designsystem.spacing.FireSpacing
import com.example.firenotes.ui.designsystem.typography.FireTypography
import com.example.firenotes.ui.screens.occurrence.models.SubNatureza
import com.example.firenotes.ui.screens.occurrence.models.subNaturezas

@Composable
fun SummaryCard(
    protocolo: String,
    natureza: String,
    subNatureza: String?,
    cidade: String,
    tempoOcorrencia: String,
    veiculosCount: Int,
    vitimasCount: Int,
    viaturasCount: Int,
    prontidao: String? = null,
    onProtocoloChange: (String) -> Unit = {},
    onNaturezaChange: (SubNatureza) -> Unit = {},
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
            var showEditDialog by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📋 Resumo da Ocorrência",
                    style = FireTypography.Title,
                    fontWeight = FontWeight.Bold,
                    color = FireColors.Primary
                )
                IconButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar talão e natureza",
                        tint = FireColors.Primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            HorizontalDivider()

            if (showEditDialog) {
                var localProtocolo by remember { mutableStateOf(protocolo) }
                var localSubNatureza by remember { mutableStateOf(subNatureza ?: "") }
                var localNaturezaDesc by remember { mutableStateOf(natureza) }
                var selectedSubNatObj by remember { mutableStateOf<SubNatureza?>(null) }
                var showNaturezaModal by remember { mutableStateOf(false) }
                
                AlertDialog(
                    onDismissRequest = { showEditDialog = false },
                    title = { Text("Editar Resumo", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Talão Field
                            OutlinedTextField(
                                value = localProtocolo,
                                onValueChange = { input ->
                                    val filtered = input.filter { it.isDigit() || it == '-' }
                                    val clean = filtered.filter { it.isDigit() }
                                    val formatted = when {
                                        clean.isEmpty() -> ""
                                        clean.length <= 4 -> clean
                                        else -> clean.substring(0, 4) + "-" + clean.substring(4).take(5)
                                    }
                                    localProtocolo = formatted
                                },
                                label = { Text("Número do Talão") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = FireColors.Primary,
                                    unfocusedBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            
                            // Natureza Selection Field
                            Text("Natureza", fontWeight = FontWeight.SemiBold, style = FireTypography.BodyMedium)
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showNaturezaModal = true }
                                    .height(56.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = FireColors.Primary.copy(alpha = 0.05f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, FireColors.Primary.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = localSubNatureza.ifBlank { localNaturezaDesc },
                                        fontWeight = FontWeight.Bold,
                                        color = FireColors.Primary,
                                        style = FireTypography.BodyMedium
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = FireColors.Primary
                                    )
                                }
                            }
                            
                            if (showNaturezaModal) {
                                NaturezaModal(
                                    selectedSubNaturezaName = localSubNatureza,
                                    onSubNaturezaSelected = { sub ->
                                        localSubNatureza = sub.nome
                                        localNaturezaDesc = sub.baseNatureza.descricao
                                        selectedSubNatObj = sub
                                    },
                                    onDismiss = { showNaturezaModal = false }
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                onProtocoloChange(localProtocolo)
                                selectedSubNatObj?.let {
                                    onNaturezaChange(it)
                                } ?: run {
                                    val fallback = subNaturezas.find { it.nome == localSubNatureza }
                                    if (fallback != null) {
                                        onNaturezaChange(fallback)
                                    }
                                }
                                showEditDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FireColors.Primary)
                        ) {
                            Text("Salvar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditDialog = false }) {
                            Text("Cancelar", color = FireColors.OnSurfaceVariant)
                        }
                    }
                )
            }

            InfoRow("Talão", protocolo)
            InfoRow("Natureza", if (!subNatureza.isNullOrBlank()) subNatureza else natureza)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NaturezaModal(
    selectedSubNaturezaName: String,
    onSubNaturezaSelected: (SubNatureza) -> Unit,
    onDismiss: () -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val categories = subNaturezas.map { it.categoria }.distinct()

    val filteredSubNaturezas = remember(searchText, selectedCategory) {
        subNaturezas.filter { sub ->
            val matchesSearch = sub.nome.contains(searchText, ignoreCase = true) ||
                    sub.categoria.contains(searchText, ignoreCase = true)
            val matchesCategory = selectedCategory == null || sub.categoria == selectedCategory
            matchesSearch && matchesCategory
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(
                containerColor = FireColors.Surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(FireSpacing.Large),
                verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
            ) {
                // Cabeçalho
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏷️ Classificação da Natureza",
                        style = FireTypography.Headline,
                        fontWeight = FontWeight.Bold,
                        color = FireColors.Primary,
                        fontSize = 18.sp
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = FireColors.OnSurfaceVariant
                        )
                    }
                }

                // Busca
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = { Text("Buscar natureza") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = FireColors.OnSurfaceVariant
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FireColors.Primary,
                        unfocusedBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.3f),
                        focusedLabelColor = FireColors.Primary,
                        unfocusedLabelColor = FireColors.OnSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Categorias
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = {
                                Text(
                                    "Todas",
                                    style = FireTypography.LabelSmall
                                )
                            },
                            modifier = Modifier.height(32.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FireColors.Primary,
                                selectedLabelColor = Color.White,
                                containerColor = FireColors.SurfaceVariant.copy(alpha = 0.5f)
                            )
                        )
                    }
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = {
                                selectedCategory = if (selectedCategory == category) null else category
                            },
                            label = {
                                Text(
                                    category,
                                    style = FireTypography.LabelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.height(32.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FireColors.Primary,
                                selectedLabelColor = Color.White,
                                containerColor = FireColors.SurfaceVariant.copy(alpha = 0.5f)
                            )
                        )
                    }
                }

                HorizontalDivider()

                // Lista de Naturezas
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredSubNaturezas) { sub ->
                        val isSelected = sub.nome == selectedSubNaturezaName
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSubNaturezaSelected(sub)
                                    onDismiss()
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    FireColors.Primary.copy(alpha = 0.1f)
                                else
                                    Color.Transparent
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = if (isSelected)
                                BorderStroke(1.dp, FireColors.Primary)
                            else
                                null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sub.nome,
                                    style = FireTypography.BodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) FireColors.Primary else FireColors.OnSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
