package com.example.firenotes.ui.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.example.firenotes.ui.designsystem.colors.FireColors

private val AppColorScheme = lightColorScheme(
    primary = FireColors.Primary,
    primaryContainer = FireColors.PrimaryLight,
    secondary = FireColors.Secondary,
    secondaryContainer = FireColors.SecondaryLight,
    tertiary = FireColors.Tertiary,
    tertiaryContainer = FireColors.TertiaryLight,
    background = FireColors.Background,
    surface = FireColors.Surface,
    surfaceVariant = FireColors.SurfaceVariant,
    onBackground = FireColors.OnBackground,
    onSurface = FireColors.OnSurface,
    onSurfaceVariant = FireColors.OnSurfaceVariant,
    error = FireColors.Error
)

@Composable
fun FireNotesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        content = content
    )
}
