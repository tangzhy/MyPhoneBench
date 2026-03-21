package com.phoneuse.mzillow.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ZillowBlue = Color(0xFF006AFF)
private val ZillowDarkBlue = Color(0xFF003D99)

private val LightColorScheme = lightColorScheme(
    primary = ZillowBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = ZillowDarkBlue,
    secondary = Color(0xFF565E71),
    background = Color(0xFFFDFBFF),
    surface = Color(0xFFFDFBFF),
)

@Composable
fun MZillowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content,
    )
}
