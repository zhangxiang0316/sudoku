package com.family.sudoku.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * Board-specific colours. They are kept separate from the Material scheme because the
 * 9x9 grid needs a few extra, clearly distinguishable states (given / user / error / peer).
 */
data class BoardColors(
    val boardBackground: Color,
    val line: Color,
    val thickLine: Color,
    val givenText: Color,
    val userText: Color,
    val errorText: Color,
    val errorBackground: Color,
    val selectedBackground: Color,
    val peerBackground: Color,
    val sameNumberBackground: Color,
    val noteText: Color,
    val emptyBackground: Color,
    /** Cells holding the digit that caused a clash, so the "why" is visible on the board. */
    val conflictBackground: Color,
    val conflictBorder: Color,
)

@Composable
@ReadOnlyComposable
fun rememberBoardColors(dark: Boolean = false): BoardColors {
    return if (dark) {
        BoardColors(
            boardBackground = NightSurface,
            line = Color(0xFF494038),
            thickLine = Color(0xFFE6D5C3),
            givenText = NightText,
            userText = NightTeal,
            errorText = Color(0xFFFF9A93),
            errorBackground = Color(0xFF5A2320),
            selectedBackground = Color(0xFF5C3A2A),
            peerBackground = Color(0xFF2C2A25),
            sameNumberBackground = Color(0xFF3E3830),
            noteText = NightMuted,
            emptyBackground = NightSurface,
            conflictBackground = Color(0xFF4A3A18),
            conflictBorder = Color(0xFFE0B04A),
        )
    } else {
        BoardColors(
            boardBackground = Paper,
            line = Color(0xFFE0D2C0),
            thickLine = Color(0xFF4A3B2E),
            givenText = Ink,
            userText = Color(0xFF1F7A6E),
            errorText = Color(0xFFC0392B),
            errorBackground = Color(0xFFFFE0DC),
            selectedBackground = Color(0xFFFFD9C9),
            peerBackground = Color(0xFFF7EFE2),
            sameNumberBackground = Color(0xFFFFEBC2),
            noteText = Color(0xFF8A7766),
            emptyBackground = Paper,
            conflictBackground = Color(0xFFFFF0C9),
            conflictBorder = Color(0xFFE39A1F),
        )
    }
}

val isDarkTheme: Boolean
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.background.luminance() < 0.5f

private fun Color.luminance(): Float {
    val r = red
    val g = green
    val b = blue
    return 0.299f * r + 0.587f * g + 0.114f * b
}
