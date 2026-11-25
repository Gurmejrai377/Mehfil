package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val MehfilLuxuryDarkColorScheme = darkColorScheme(
    primary = MehfilGold,
    onPrimary = MehfilMidnightPlum,
    primaryContainer = MehfilLavenderContainer,
    onPrimaryContainer = MehfilChampagne,
    secondary = MehfilPurple,
    onSecondary = Color.White,
    secondaryContainer = MehfilGoldContainer,
    onSecondaryContainer = MehfilGoldOnContainer,
    tertiary = MehfilPurpleLight,
    onTertiary = MehfilMidnightPlum,
    tertiaryContainer = MehfilCoralContainer,
    onTertiaryContainer = Color(0xFFFFD9E2),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = MehfilCoral,
    onError = Color.White
)

private val MehfilLightColorScheme = lightColorScheme(
    primary = MehfilPurple,
    onPrimary = Color.White,
    primaryContainer = LightSurfaceVariant,
    onPrimaryContainer = MehfilDeepPurple,
    secondary = MehfilRichGold,
    onSecondary = MehfilMidnightPlum,
    secondaryContainer = Color(0xFFFFF0C2),
    onSecondaryContainer = Color(0xFF3A2800),
    tertiary = MehfilCoral,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    error = MehfilCoral,
    onError = Color.White
)

val MehfilShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

@Composable
fun MehfilTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) MehfilLuxuryDarkColorScheme else MehfilLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = WaveTypography,
        shapes = MehfilShapes,
        content = content
    )
}
