package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val MehfilAuroraDarkColorScheme = darkColorScheme(
    primary = MehfilNeonGreen,
    onPrimary = MehfilObsidian,
    primaryContainer = MehfilLavenderContainer,
    onPrimaryContainer = MehfilIceWhite,
    secondary = MehfilNeonCyan,
    onSecondary = MehfilObsidian,
    secondaryContainer = MehfilGoldContainer,
    onSecondaryContainer = MehfilGoldOnContainer,
    tertiary = MehfilNeonPink,
    onTertiary = Color.White,
    tertiaryContainer = MehfilCoralContainer,
    onTertiaryContainer = Color(0xFFFFD6EA),
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
    primary = Color(0xFF00B85C),
    onPrimary = Color.White,
    primaryContainer = LightSurfaceVariant,
    onPrimaryContainer = MehfilObsidian,
    secondary = Color(0xFF0099CC),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5F9E8),
    onSecondaryContainer = Color(0xFF00331A),
    tertiary = MehfilNeonPink,
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
    val colorScheme = if (darkTheme) MehfilAuroraDarkColorScheme else MehfilLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = WaveTypography,
        shapes = MehfilShapes,
        content = content
    )
}
