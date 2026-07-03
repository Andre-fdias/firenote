package com.example.firenotes.ui.screens.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.firenotes.ui.designsystem.colors.FireColors
import com.example.firenotes.ui.designsystem.spacing.FireSpacing
import com.example.firenotes.ui.designsystem.typography.FireTypography
import com.example.firenotes.ui.designsystem.icons.FireIcons
import com.example.firenotes.ui.designsystem.components.inputs.FireOutlinedTextField

@Composable
fun PinLockScreen(
    correctPin: String,
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var enteredPin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(FireSpacing.MediumLarge),
            modifier = Modifier.padding(FireSpacing.Large)
        ) {
            Icon(
                imageVector = FireIcons.Lock,
                contentDescription = "Bloqueado",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )

            Text(
                text = "Fire Notes Bloqueado",
                style = FireTypography.Headline,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Insira seu PIN de 4 dígitos para acessar o aplicativo.",
                style = FireTypography.Body,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = FireSpacing.Medium)
            )

            FireOutlinedTextField(
                value = enteredPin,
                onValueChange = {
                    if (it.length <= 4) {
                        enteredPin = it
                        showError = false
                        if (it.length == 4) {
                            if (it == correctPin) {
                                onUnlocked()
                            } else {
                                showError = true
                                enteredPin = ""
                            }
                        }
                    }
                },
                label = "Digitar PIN",
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(180.dp)
            )

            if (showError) {
                Text(
                    text = "PIN incorreto. Tente novamente.",
                    style = FireTypography.Label,
                    color = FireColors.Error,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
