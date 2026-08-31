package com.example.moneycheck.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = MoneyGreenDark,
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF005044),
    onPrimaryContainer = Color(0xFFADF2DE),
    secondary = Color(0xFFB5CBD2),
    tertiary = Color(0xFFF3C47C),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF7A2928),
)

private val LightColorScheme = lightColorScheme(
    primary = MoneyGreen,
    onPrimary = Color.White,
    primaryContainer = MoneyGreenContainer,
    onPrimaryContainer = Color(0xFF00382F),
    secondary = Color(0xFF14534C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE7F1EF),
    onSecondaryContainer = Color(0xFF14534C),
    tertiary = MoneyGold,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    error = Color(0xFFC94F52),
    errorContainer = Color(0xFFFCE8E8),
)

private val MoneyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun MoneyCheckTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = MoneyShapes,
        content = content,
    )
}
