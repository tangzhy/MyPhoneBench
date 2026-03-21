package com.phoneuse.mcvspharmacy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFC41230),
    secondary = Color(0xFF1D6AA5),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFC41230),
    secondary = Color(0xFF1D6AA5),
)

@Composable
fun MCVSPharmacyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, content = content)
}
