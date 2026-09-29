package com.spendly.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF006C60), onPrimary = Color.White,
    primaryContainer = Color(0xFF9EF2DE), onPrimaryContainer = Color(0xFF00201B),
    secondary = Color(0xFF4B635D), onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDE8DF), onSecondaryContainer = Color(0xFF07201A),
    tertiary = Color(0xFF426277), background = Color(0xFFF4FBF8),
    surface = Color(0xFFF4FBF8), surfaceVariant = Color(0xFFD9E5DF),
    outline = Color(0xFF6F7974),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF82D6C2), onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF005046), onPrimaryContainer = Color(0xFF9EF2DE),
    secondary = Color(0xFFB2CCC4), onSecondary = Color(0xFF1D352F),
    secondaryContainer = Color(0xFF344C45), onSecondaryContainer = Color(0xFFCDE8DF),
    tertiary = Color(0xFFAACBE1), background = Color(0xFF101A17),
    surface = Color(0xFF101A17), surfaceVariant = Color(0xFF3E4944),
    outline = Color(0xFF89938E),
)

@Composable
fun SpendlyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
