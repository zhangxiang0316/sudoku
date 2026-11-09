package com.family.sudoku.ui.game

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.family.sudoku.ui.ConflictMessage
import com.family.sudoku.ui.GameUiState
import com.family.sudoku.ui.icons.SudokuIcons
import com.family.sudoku.ui.formatDuration
import com.family.sudoku.ui.theme.HintInk
import com.family.sudoku.ui.theme.HintInkDark
import com.family.sudoku.ui.theme.HintSurface
import com.family.sudoku.ui.theme.HintSurfaceDark
import com.family.sudoku.ui.theme.isDarkTheme

@Composable
fun GameScreen(
    state: GameUiState,
    onBack: () -> Unit,
    onPause: () -> Unit,
    onCellClick: (Int) -> Unit,
    onDigit: (Int) -> Unit,
    onErase: () -> Unit,
    onUndo: () -> Unit,
    onToggleNotes: () -> Unit,
    onHint: () -> Unit,
    onRetry: () -> Unit,
    onToggleQuiet: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val remainingCounts = rememberRemainingCounts(state.board, state.size.size)
    val interactionEnabled = !state.isGenerating && !state.isPaused && !state.isComplete

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "返回",
                    tint = scheme.onBackground,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = "${state.size.title} · ${state.difficulty.emoji} ${state.difficulty.title}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onBackground,
                )
                Text(
                    text = "已完成 ${state.filledCount}/${state.cellCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            QuietToggle(quiet = state.quietMode, onToggle = onToggleQuiet)
            Spacer(Modifier.width(4.dp))
            TimerChip(
                seconds = state.seconds,
                onClick = onPause,
                enabled = interactionEnabled,
            )
        }

        Spacer(Modifier.height(10.dp))

        // The board must stay square, so it is centred inside whatever space is left
        // between the header and the fixed controls at the bottom of the screen.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            val boardModifier = if (maxWidth <= maxHeight) {
                Modifier.fillMaxWidth()
            } else {
                Modifier.fillMaxHeight()
            }
            Box(modifier = boardModifier.aspectRatio(1f)) {
                SudokuBoard(
                    spec = state.spec,
                    board = state.board,
                    puzzle = state.puzzle,
                    solution = state.solution,
                    notes = state.notes,
                    selectedCell = state.selectedCell,
                    enabled = interactionEnabled,
                    onCellClick = onCellClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (state.isGenerating) 0.35f else 1f),
                    conflictCells = (state.conflict?.cells.orEmpty() + listOfNotNull(state.hintFocus)),
                )

                if (state.isGenerating) {
                    GeneratingOverlay(onRetry = onRetry)
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // The slot is always laid out at the same height, so showing a hint never resizes the
        // board underneath the child's finger.
        ConflictBanner(
            message = state.conflict,
            hintText = state.hintText,
            modifier = Modifier
                .fillMaxWidth()
                .height(CONFLICT_SLOT_HEIGHT),
        )

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusPill(
                label = "失误",
                value = state.mistakes.toString(),
                accent = if (state.mistakes > 0) scheme.error else scheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(8.dp))
            StatusPill(
                label = "提示",
                value = "${state.hints}/${state.hintAllowance}",
                accent = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = if (state.noteMode) "笔记模式" else "填数模式",
                style = MaterialTheme.typography.labelMedium,
                color = if (state.noteMode) scheme.secondary else scheme.onSurfaceVariant,
                fontWeight = if (state.noteMode) FontWeight.Bold else FontWeight.Normal,
            )
        }

        Spacer(Modifier.height(10.dp))

        ToolRow(
            noteMode = state.noteMode,
            canUndo = state.canUndo,
            hintsUsed = state.hints,
            maxHints = state.hintAllowance,
            onUndo = onUndo,
            onToggleNotes = onToggleNotes,
            onHint = onHint,
        )

        Spacer(Modifier.height(10.dp))

        NumberPad(
            spec = state.spec,
            remainingCounts = remainingCounts,
            enabled = interactionEnabled && state.selectedCell != null,
            noteMode = state.noteMode,
            onDigit = onDigit,
            onErase = onErase,
        )

        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun TimerChip(
    seconds: Int,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = scheme.surfaceVariant.copy(alpha = 0.7f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = SudokuIcons.Pause,
                contentDescription = "暂停",
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = formatDuration(seconds),
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun StatusPill(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = accent,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun GeneratingOverlay(onRetry: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = scheme.primary,
                strokeWidth = 4.dp,
                modifier = Modifier.size(44.dp),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "正在出题…",
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "每一关都保证只有一个答案哦",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Surface(
                onClick = onRetry,
                shape = RoundedCornerShape(10.dp),
                color = scheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.alpha(0.9f),
            ) {
                Text(
                    text = "等得有点久？点这里重试",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun rememberRemainingCounts(board: List<Int>, size: Int): Map<Int, Int> {
    return androidx.compose.runtime.remember(board, size) {
        val counts = HashMap<Int, Int>(size)
        for (digit in 1..size) {
            counts[digit] = size - board.count { it == digit }
        }
        counts
    }
}

/** Height of the always-present hint slot; keeping it fixed stops the board from jumping. */
private val CONFLICT_SLOT_HEIGHT = 40.dp

/**
 * Shows why a digit was rejected. The sentence is written the way an adult would explain it
 * to a child, and the cells that caused the clash are highlighted on the board at the same
 * time.
 */
@Composable
private fun ConflictBanner(
    message: ConflictMessage?,
    hintText: String? = null,
    modifier: Modifier = Modifier,
) {
    val dark = isDarkTheme
    val shown = hintText ?: message?.text
    // AnimatedContent keeps the outgoing sentence on screen while it fades, which is exactly
    // what the fading banner needs. Storing "the last message" in a remembered MutableState
    // would have meant writing to state during composition, so it is avoided here.
    AnimatedContent(
        targetState = shown,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        contentAlignment = Alignment.Center,
        label = "conflict",
        modifier = modifier,
    ) { current ->
        if (current != null) {
            Surface(
                color = if (dark) HintSurfaceDark else HintSurface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("conflict_banner"),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "💡", fontSize = 14.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = current,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (dark) HintInkDark else HintInk,
                        maxLines = 1,
                        modifier = Modifier
                            .semantics { liveRegion = LiveRegionMode.Polite }
                            .testTag("conflict_text"),
                    )
                }
            }
        }
    }
}

/** One switch for sound and vibration: "the baby is asleep" should silence everything. */
@Composable
private fun QuietToggle(quiet: Boolean, onToggle: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onToggle,
        color = if (quiet) scheme.surfaceVariant else scheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.testTag("quiet_toggle"),
    ) {
        Text(
            text = if (quiet) "🔇" else "🔊",
            fontSize = 15.sp,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
        )
    }
}
