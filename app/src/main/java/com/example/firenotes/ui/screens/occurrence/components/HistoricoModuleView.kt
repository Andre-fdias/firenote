package com.example.firenotes.ui.screens.occurrence.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import android.speech.RecognizerIntent
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import androidx.compose.ui.platform.LocalContext

@Composable
fun HistoricoModuleView(
    uiState: OccurrenceFormUiState,
    onHistoryChanged: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showGuidelines by remember { mutableStateOf(true) }
    val characterLimit = 4000
    val currentLength = uiState.historico.length

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.firstOrNull() ?: ""
            if (spokenText.isNotBlank()) {
                val newText = if (uiState.historico.isBlank()) {
                    spokenText
                } else {
                    "${uiState.historico} $spokenText"
                }
                if (newText.length <= characterLimit) {
                    onHistoryChanged(newText)
                }
            }
        }
    }

    val startSpeechToText = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale o histórico narrativo...")
        }
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Reconhecimento de voz não suportado", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FireColors.Background)
            .padding(horizontal = FireSpacing.Medium, vertical = FireSpacing.Small)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
        ) {

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
                        text = "📝",
                        fontSize = 28.sp,
                        modifier = Modifier.padding(end = FireSpacing.Small)
                    )
                    Column {
                        Text(
                            text = "Histórico Narrativo",
                            style = FireTypography.Headline,
                            fontWeight = FontWeight.ExtraBold,
                            color = FireColors.Primary
                        )
                        Text(
                            text = "Relatório circunstanciado e registro cronológico",
                            style = FireTypography.LabelMedium,
                            color = FireColors.OnSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = { startSpeechToText() },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = FireColors.Primary.copy(alpha = 0.1f),
                        contentColor = FireColors.Primary
                    ),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = FireIcons.Mic,
                        contentDescription = "Inserir via áudio",
                        modifier = Modifier.size(24.dp)
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showGuidelines = !showGuidelines },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💡", fontSize = 16.sp, modifier = Modifier.padding(end = FireSpacing.Small))
                            Text(
                                text = "Diretrizes de Narrativa Padrão",
                                style = FireTypography.Title,
                                fontWeight = FontWeight.Bold,
                                color = FireColors.Primary
                            )
                        }
                        Icon(
                            imageVector = if (showGuidelines) FireIcons.ArrowDropUp else FireIcons.ArrowDropDown,
                            contentDescription = null,
                            tint = FireColors.OnSurfaceVariant
                        )
                    }

                    AnimatedVisibility(
                        visible = showGuidelines,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Para uma redação jurídica e operacional perfeita, busque citar:",
                                style = FireTypography.LabelSmall,
                                fontWeight = FontWeight.Bold,
                                color = FireColors.OnSurfaceVariant
                            )
                            GuidelineItem("Situação encontrada na chegada da guarnição (isolamento, chamas, etc.).")
                            GuidelineItem("Ações e táticas operacionais empregadas para o controle da situação.")
                            GuidelineItem("Vítimas encontradas, triagem de trauma e socorros imediatos prestados.")
                            GuidelineItem("Órgãos de apoio acionados e presentes no local da ocorrência.")
                            GuidelineItem("Estado final de preservação do local ao retornar para o quartel.")
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    FireOutlinedTextField(
                        value = uiState.historico,
                        onValueChange = { input ->
                            if (input.length <= characterLimit) {
                                onHistoryChanged(input)
                            }
                        },
                        label = "Histórico detalhado do atendimento",
                        singleLine = false,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 4.dp, end = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val limitColor = if (currentLength > characterLimit * 0.9) FireColors.Error else FireColors.OnSurfaceVariant
                        Text(
                            text = "Evite abreviações não oficiais ou termos vagos.",
                            style = FireTypography.LabelSmall,
                            color = FireColors.OnSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "$currentLength / $characterLimit caracteres",
                            style = FireTypography.LabelSmall,
                            fontWeight = FontWeight.Bold,
                            color = limitColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(FireSpacing.ExtraSmall))
            FireButton(
                text = "SALVAR E VOLTAR AO DASHBOARD",
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )
        }
    }
}

@Composable
private fun GuidelineItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = FireTypography.BodySmall,
            fontWeight = FontWeight.Bold,
            color = FireColors.Primary,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            style = FireTypography.BodySmall,
            color = FireColors.OnSurfaceVariant.copy(alpha = 0.9f)
        )
    }
}