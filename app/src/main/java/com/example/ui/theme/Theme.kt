package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MateDarkColorScheme = darkColorScheme(
    primary = MatePrimary,
    onPrimary = Color.White,
    primaryContainer = MatePrimaryDark,
    onPrimaryContainer = Color.White,
    secondary = MateSecondary,
    onSecondary = Color.White,
    secondaryContainer = MateSecondaryDark,
    onSecondaryContainer = Color.White,
    tertiary = MateTertiary,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceCard,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    error = Color(0xFFF43F5E),
    onError = Color.White
)

private val MateLightColorScheme = lightColorScheme(
    primary = MatePrimaryDark,
    onPrimary = Color.White,
    secondary = MateSecondary,
    onSecondary = Color.White,
    tertiary = MateTertiary,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = Color(0xFFE11D48),
    onError = Color.White
)

@Composable
fun MateChatTheme(
    darkTheme: Boolean = true, // Default to sleek dark mode for high-tech messenger feel
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) MateDarkColorScheme else MateLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backward compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MateChatTheme(darkTheme = true, content = content)
}
