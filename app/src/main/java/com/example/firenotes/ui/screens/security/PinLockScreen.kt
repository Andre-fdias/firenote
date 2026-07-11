package com.example.firenotes.ui.screens.security

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firenotes.ui.designsystem.colors.FireColors
import com.example.firenotes.ui.designsystem.spacing.FireSpacing
import com.example.firenotes.ui.designsystem.typography.FireTypography
import com.example.firenotes.ui.designsystem.icons.FireIcons
import com.example.firenotes.ui.designsystem.components.buttons.FireButton
import kotlinx.coroutines.delay

private const val LOG_TAG = "FireSecurity"
private fun logD(message: String) = android.util.Log.d(LOG_TAG, message)
private fun logE(message: String, throwable: Throwable? = null) = 
    android.util.Log.e(LOG_TAG, message, throwable)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinLockScreen(
    correctPin: String,
    onUnlocked: () -> Unit,
    maxAttempts: Int = 3,
    modifier: Modifier = Modifier
) {
    var enteredPin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var attempts by remember { mutableStateOf(0) }
    var isLocked by remember { mutableStateOf(false) }
    var lockTimer by remember { mutableStateOf(0) }
    
    // Animação de erro (shake)
    val shakeAnim by animateFloatAsState(
        targetValue = if (showError) 10f else 0f,
        animationSpec = tween(200),
        label = "shake"
    )

    // Timer de bloqueio temporário
    LaunchedEffect(isLocked) {
        if (isLocked) {
            lockTimer = 30
            while (lockTimer > 0) {
                delay(1000)
                lockTimer--
            }
            isLocked = false
            attempts = 0
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FireColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(FireSpacing.MediumLarge),
            modifier = Modifier
                .padding(FireSpacing.Large)
                .offset(x = shakeAnim.dp)
                .background(FireColors.Surface, CircleShape)
                .padding(32.dp)
                .clip(CircleShape)
        ) {
            // Ícone animado trancado/destrancado
            AnimatedContent(
                targetState = if (showError || isLocked) "🔒" else "🔓",
                transitionSpec = {
                    fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
                },
                label = "lock_icon"
            ) { icon ->
                Text(
                    text = icon,
                    fontSize = 56.sp,
                    modifier = Modifier
                        .background(
                            if (showError || isLocked) FireColors.Error.copy(alpha = 0.1f) else FireColors.Primary.copy(alpha = 0.1f),
                            CircleShape
                        )
                        .padding(16.dp)
                )
            }

            Text(
                text = if (isLocked) "🔒 BLOQUEADO" else "Fire Notes",
                style = FireTypography.Headline,
                fontWeight = FontWeight.Bold,
                color = if (isLocked) FireColors.Error else FireColors.OnBackground
            )

            if (isLocked) {
                Text(
                    text = "Tente novamente em ${lockTimer}s",
                    style = FireTypography.Body,
                    color = FireColors.Error,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    text = "Insira seu PIN de 4 dígitos",
                    style = FireTypography.Body,
                    color = FireColors.OnSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Indicador visual de progresso dos dígitos (4 pontos)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                repeat(4) { index ->
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                color = if (index < enteredPin.length) {
                                    FireColors.Primary
                                } else {
                                    FireColors.OnSurfaceVariant.copy(alpha = 0.3f)
                                },
                                shape = CircleShape
                            )
                    )
                }
            }

            // Campo de entrada do teclado numérico
            OutlinedTextField(
                value = enteredPin,
                onValueChange = { 
                    if (!isLocked && it.length <= 4) {
                        enteredPin = it
                        showError = false
                        
                        if (it.length == 4) {
                            if (it == correctPin) {
                                logD("PIN correto. Acesso liberado.")
                                onUnlocked()
                            } else {
                                logD("PIN incorreto.")
                                showError = true
                                errorMessage = "PIN incorreto. Tentativas: ${attempts + 1}/$maxAttempts"
                                attempts++
                                enteredPin = ""
                                
                                if (attempts >= maxAttempts) {
                                    isLocked = true
                                    logE("PIN incorreto $maxAttempts vezes. Bloqueio ativado.")
                                }
                            }
                        }
                    }
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .width(180.dp)
                    .background(Color.Transparent),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (showError) FireColors.Error else FireColors.Primary,
                    unfocusedBorderColor = FireColors.OnSurfaceVariant.copy(alpha = 0.3f),
                    cursorColor = if (showError) FireColors.Error else FireColors.Primary
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Mensagem de erro com animação
            AnimatedVisibility(
                visible = showError,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "⚠️ $errorMessage",
                        style = FireTypography.Label,
                        color = FireColors.Error,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Tentativas restantes: ${maxAttempts - attempts}",
                        style = FireTypography.Caption,
                        color = FireColors.OnSurfaceVariant
                    )
                }
            }

            // Badge indicador de segurança
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = FireColors.Success.copy(alpha = 0.1f),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(FireColors.Success, CircleShape)
                    )
                    Text(
                        text = "Seguro",
                        fontSize = 11.sp,
                        color = FireColors.Success,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
