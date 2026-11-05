package com.family.sudoku.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Coral,
    onPrimary = Paper,
    primaryContainer = CoralSoft,
    onPrimaryContainer = Color(0xFF4A1E14),
    secondary = Teal,
    onSecondary = Paper,
    secondaryContainer = TealSoft,
    onSecondaryContainer = Color(0xFF0A3B35),
    tertiary = Amber,
    onTertiary = Color(0xFF4A3300),
    tertiaryContainer = AmberSoft,
    onTertiaryContainer = Color(0xFF3D2A00),
    background = Cream,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Sand,
    onSurfaceVariant = MutedInk,
    surfaceTint = Coral,
    inverseSurface = Ink,
    inverseOnSurface = Cream,
    error = ErrorRed,
    onError = Paper,
    errorContainer = Color(0xFFFFE1DE),
    onErrorContainer = Color(0xFF5F1410),
    outline = Color(0xFFC9B7A3),
    outlineVariant = Color(0xFFE8DCCB),
    scrim = Color(0xFF000000),
)

private val DarkColors = darkColorScheme(
    primary = NightCoral,
    onPrimary = Color(0xFF4A160A),
    primaryContainer = Color(0xFF6A2A1B),
    onPrimaryContainer = Color(0xFFFFDAD1),
    secondary = NightTeal,
    onSecondary = Color(0xFF003730),
    secondaryContainer = Color(0xFF0C4A42),
    onSecondaryContainer = Color(0xFFB5F1E8),
    tertiary = NightAmber,
    onTertiary = Color(0xFF402D00),
    tertiaryContainer = Color(0xFF5C4400),
    onTertiaryContainer = Color(0xFFFFE08A),
    background = Night,
    onBackground = NightText,
    surface = NightSurface,
    onSurface = NightText,
    surfaceVariant = NightSurfaceHigh,
    onSurfaceVariant = NightMuted,
    surfaceTint = NightCoral,
    inverseSurface = NightText,
    inverseOnSurface = Night,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = NightBorder,
    outlineVariant = Color(0xFF3A3028),
    scrim = Color(0xFF000000),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun SudokuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = SudokuTypography,
        shapes = AppShapes,
        content = content,
    )
}
