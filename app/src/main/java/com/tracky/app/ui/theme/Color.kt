package com.tracky.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Primary — Blue
val BluePrimary = Color(0xFF1A73E8)
val BlueOnPrimary = Color(0xFFFFFFFF)
val BluePrimaryContainer = Color(0xFFD2E3FC)
val BlueOnPrimaryContainer = Color(0xFF041E49)

// Secondary — Green
val GreenSecondary = Color(0xFF34A853)
val GreenOnSecondary = Color(0xFFFFFFFF)
val GreenSecondaryContainer = Color(0xFFC8E6C9)
val GreenOnSecondaryContainer = Color(0xFF0D3B13)

// Tertiary — Gold
val GoldTertiary = Color(0xFFFBBC04)
val GoldOnTertiary = Color(0xFF1A1A1A)
val GoldTertiaryContainer = Color(0xFFFFF8E1)
val GoldOnTertiaryContainer = Color(0xFF4D3500)

// Error — Red
val ErrorRed = Color(0xFFEA4335)
val ErrorOnError = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFFFFDAD6)
val ErrorOnErrorContainer = Color(0xFF410002)

// Background & Surface — Light
val LightBackground = Color(0xFFF8F9FA)
val LightOnBackground = Color(0xFF202124)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF202124)
val LightSurfaceVariant = Color(0xFFE8EAED)
val LightOnSurfaceVariant = Color(0xFF5F6368)
val LightOutline = Color(0xFFDADCE0)
val LightOutlineVariant = Color(0xFFC4C7C5)

// Background & Surface — Dark
val DarkBackground = Color(0xFF121212)
val DarkOnBackground = Color(0xFFE8EAED)
val DarkSurface = Color(0xFF1E1E1E)
val DarkOnSurface = Color(0xFFE8EAED)
val DarkSurfaceVariant = Color(0xFF2D2D2D)
val DarkOnSurfaceVariant = Color(0xFF9AA0A6)
val DarkOutline = Color(0xFF3C4043)
val DarkOutlineVariant = Color(0xFF444746)

// Darker versions of key colors for dark scheme
val DarkPrimary = Color(0xFF8AB4F8)
val DarkOnPrimary = Color(0xFF062E6F)
val DarkPrimaryContainer = Color(0xFF0842A0)
val DarkOnPrimaryContainer = Color(0xFFD2E3FC)

val DarkSecondary = Color(0xFF81C784)
val DarkOnSecondary = Color(0xFF003910)
val DarkSecondaryContainer = Color(0xFF1B5E20)
val DarkOnSecondaryContainer = Color(0xFFC8E6C9)

val DarkTertiary = Color(0xFFFFD54F)
val DarkOnTertiary = Color(0xFF3A2800)
val DarkTertiaryContainer = Color(0xFF6D4C00)
val DarkOnTertiaryContainer = Color(0xFFFFF8E1)

val DarkError = Color(0xFFCF6679)
val DarkOnError = Color(0xFF601410)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)

val lightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = BlueOnPrimary,
    primaryContainer = BluePrimaryContainer,
    onPrimaryContainer = BlueOnPrimaryContainer,
    secondary = GreenSecondary,
    onSecondary = GreenOnSecondary,
    secondaryContainer = GreenSecondaryContainer,
    onSecondaryContainer = GreenOnSecondaryContainer,
    tertiary = GoldTertiary,
    onTertiary = GoldOnTertiary,
    tertiaryContainer = GoldTertiaryContainer,
    onTertiaryContainer = GoldOnTertiaryContainer,
    error = ErrorRed,
    onError = ErrorOnError,
    errorContainer = ErrorContainer,
    onErrorContainer = ErrorOnErrorContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant
)

val darkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant
)
