package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Mehfil Aurora Neon & Obsidian Palette
val MehfilObsidian = Color(0xFF070B14)
val MehfilSlateNight = Color(0xFF0C1322)
val MehfilSlateCard = Color(0xFF151E33)
val MehfilNeonGreen = Color(0xFF00E676)
val MehfilNeonCyan = Color(0xFF00D4FF)
val MehfilNeonPink = Color(0xFFFF2E93)
val MehfilIceWhite = Color(0xFFF5F8FF)
val MehfilSoftMuted = Color(0xFF9EB0D2)

// Semantic Tokens Mapped to the Aurora Neon Theme
val MehfilMidnightPlum = MehfilObsidian
val MehfilDeepPurple = MehfilSlateNight
val MehfilRoyalPurple = MehfilSlateCard
val MehfilPurple = MehfilNeonCyan
val MehfilPurpleLight = Color(0xFF7BE7FF)
val MehfilLavenderContainer = Color(0xFF13263E)
val MehfilLavenderOnContainer = Color(0xFFE5F7FF)

// Primary Neon Emerald & Magenta-Pink Gradient Accents
val MehfilGold = MehfilNeonGreen
val MehfilChampagne = MehfilIceWhite
val MehfilRichGold = MehfilNeonPink
val MehfilGoldContainer = Color(0xFF093824)
val MehfilGoldOnContainer = Color(0xFFB9FFE0)

// Glassmorphism Surface Tokens
val MehfilGlassBg = Color(0xA611192B)
val MehfilGlassBgStrong = Color(0xCC0D1424)
val MehfilGlassStroke = Color(0x5500D4FF)
val MehfilGlassStrokeSoft = Color(0x3DFFFFFF)

// Live, Music & Status Accents
val MehfilMint = Color(0xFF00F260)
val MehfilCoral = Color(0xFFFF3B7F)
val MehfilCoralContainer = Color(0xFF47122B)
val MehfilCyanGlow = MehfilNeonCyan

// Neutrals (Dark Obsidian Slate)
val DarkBackground = MehfilObsidian
val DarkSurface = MehfilSlateNight
val DarkSurfaceVariant = MehfilSlateCard
val DarkOutline = Color(0xFF283857)
val DarkOnSurface = MehfilIceWhite
val DarkOnSurfaceVariant = MehfilSoftMuted

// Neutrals (Light fallback)
val LightBackground = Color(0xFFF4F8FF)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE7EFFC)
val LightOutline = Color(0xFFC9D8F0)
val LightOnSurface = Color(0xFF0A1221)
val LightOnSurfaceVariant = Color(0xFF4B5D7E)

val MehfilAuroraPillBrush: Brush
    get() = Brush.horizontalGradient(
        listOf(MehfilNeonGreen, MehfilNeonCyan, MehfilNeonPink)
    )

val MehfilAuroraRingBrush: Brush
    get() = Brush.linearGradient(
        listOf(MehfilNeonGreen, MehfilNeonCyan, MehfilNeonPink)
    )
