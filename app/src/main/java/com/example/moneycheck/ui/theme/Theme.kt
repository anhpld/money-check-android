package com.example.moneycheck.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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
    outlineVariant = LightInput,
    error = Color(0xFFCB473F),
    errorContainer = Color(0xFFFFF0ED),
)

private val MoneyShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(18.dp),
)

@Composable
fun MoneyCheckTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        shapes = MoneyShapes,
        content = content,
    )
}
