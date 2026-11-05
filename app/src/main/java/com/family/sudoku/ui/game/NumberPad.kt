package com.family.sudoku.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.family.sudoku.game.BoardSpec
import com.family.sudoku.ui.icons.SudokuIcons

/**
 * Digit keys plus the erase key. The layout follows the board size so that small boards do
 * not waste screen space and tall boards stay reachable: 4x4 uses a single row of five
 * keys, 6x6 a single row of seven, and 9x9 the familiar two rows of five.
 */
@Composable
fun NumberPad(
    spec: BoardSpec,
    remainingCounts: Map<Int, Int>,
    enabled: Boolean,
    noteMode: Boolean,
    onDigit: (Int) -> Unit,
    onErase: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val columns = if (spec.size <= 6) spec.size + 1 else 5
    val rows = (spec.size + 1 + columns - 1) / columns

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (col in 0 until columns) {
                    val position = row * columns + col
                    when {
                        position < spec.size -> {
                            val digit = position + 1
                            val remaining = remainingCounts[digit] ?: spec.size
                            DigitButton(
                                digit = digit,
                                remaining = remaining,
                                enabled = enabled && remaining > 0,
                                noteMode = noteMode,
                                size = spec.size,
                                onClick = { onDigit(digit) },
                                modifier = Modifier.weight(1f),
                            )
                        }

                        position == spec.size -> EraseButton(
                            enabled = enabled,
                            onClick = onErase,
                            modifier = Modifier.weight(1f),
                            height = if (spec.size <= 6) 64.dp else 58.dp,
                        )

                        else -> Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DigitButton(
    digit: Int,
    remaining: Int,
    enabled: Boolean,
    noteMode: Boolean,
    size: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val container = if (noteMode) scheme.secondaryContainer else scheme.surface
    val content = if (noteMode) scheme.onSecondaryContainer else scheme.onSurface
    val alpha = when {
        !enabled -> 0.32f
        remaining == 0 -> 0.32f
        else -> 1f
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(if (size <= 6) 64.dp else 58.dp)
            .testTag("digit_$digit"),
        shape = RoundedCornerShape(18.dp),
        color = container.copy(alpha = if (container == scheme.surface) 0.96f else 1f),
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.7f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = digit.toString(),
                fontSize = if (size <= 6) 32.sp else 28.sp,
                lineHeight = if (size <= 6) 32.sp else 28.sp,
                fontWeight = FontWeight.Bold,
                color = content.copy(alpha = alpha),
            )
            if (remaining in 1 until size) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 6.dp, bottom = 5.dp),
                ) {
                    Text(
                        text = remaining.toString(),
                        fontSize = 10.sp,
                        lineHeight = 11.sp,
                        color = content.copy(alpha = 0.5f * alpha),
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun EraseButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 58.dp,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(height),
        shape = RoundedCornerShape(18.dp),
        color = scheme.surfaceVariant,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = SudokuIcons.Backspace,
                contentDescription = "擦除",
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
fun ToolRow(
    noteMode: Boolean,
    canUndo: Boolean,
    hintsUsed: Int,
    maxHints: Int,
    onUndo: () -> Unit,
    onToggleNotes: () -> Unit,
    onHint: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ToolButton(
            icon = SudokuIcons.Undo,
            label = "撤销",
            active = false,
            enabled = canUndo,
            onClick = onUndo,
            modifier = Modifier.weight(1f),
        )
        ToolButton(
            icon = SudokuIcons.EditNote,
            label = "笔记",
            active = noteMode,
            enabled = true,
            onClick = onToggleNotes,
            modifier = Modifier.weight(1f),
        )
        ToolButton(
            icon = SudokuIcons.Lightbulb,
            label = if (maxHints > 0) "提示 ${maxHints - hintsUsed}" else "提示",
            active = false,
            enabled = hintsUsed < maxHints,
            onClick = onHint,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ToolButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val container = if (active) scheme.secondary else scheme.surface
    val content = if (active) scheme.onSecondary else scheme.onSurface

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(14.dp),
        color = container,
        border = if (active) null else androidx.compose.foundation.BorderStroke(
            1.dp,
            scheme.outlineVariant.copy(alpha = 0.7f),
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = content.copy(alpha = if (enabled) 1f else 0.35f),
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = content.copy(alpha = if (enabled) 1f else 0.35f),
            )
        }
    }
}
