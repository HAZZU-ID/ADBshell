package com.example.adbshell.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val TerminalGreen  = Color(0xFF00FF88)
val TerminalRed    = Color(0xFFFF6B6B)
val TerminalYellow = Color(0xFFFFD93D)
val TerminalCyan   = Color(0xFF4DD8E0)
val BgDark         = Color(0xFF0D1117)
val BgSurface      = Color(0xFF161B22)
val BgVariant      = Color(0xFF21262D)
val TextPrimary    = Color(0xFFE6EDF3)
val TextMuted      = Color(0xFF6E7681)
val BorderColor    = Color(0xFF30363D)

private val DarkColors = darkColorScheme(
    primary = TerminalGreen, onPrimary = Color(0xFF003920),
    primaryContainer = Color(0xFF005230), onPrimaryContainer = Color(0xFF7CFFA6),
    secondary = TerminalCyan, onSecondary = Color(0xFF003740),
    background = BgDark, surface = BgSurface,
    onBackground = TextPrimary, onSurface = TextPrimary,
    surfaceVariant = BgVariant, outline = BorderColor, error = TerminalRed,
)

@Composable
fun ADBShellTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
