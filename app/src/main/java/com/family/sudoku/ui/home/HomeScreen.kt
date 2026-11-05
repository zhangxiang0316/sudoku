package com.family.sudoku.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.family.sudoku.data.BoardSize
import com.family.sudoku.data.Difficulty
import com.family.sudoku.game.PlayerStats
import com.family.sudoku.game.SavedGamePreview
import com.family.sudoku.ui.formatDuration
import com.family.sudoku.ui.game.Badge
import com.family.sudoku.ui.game.StarRow

@Composable
fun HomeScreen(
    selectedSize: BoardSize,
    selectedDifficulty: Difficulty,
    stats: PlayerStats,
    savedPreview: SavedGamePreview?,
    onSelectSize: (BoardSize) -> Unit,
    onSelectDifficulty: (Difficulty) -> Unit,
    onStart: () -> Unit,
    onContinue: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(12.dp))

            Header(onOpenLeaderboard = onOpenLeaderboard)

            Spacer(Modifier.height(16.dp))

            StatsCard(stats = stats)

            Spacer(Modifier.height(20.dp))

            Text(
                text = "① 选择盘面",
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "第一次玩建议从 4×4 开始，熟练了再换大的",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            SizeRow(
                selected = selectedSize,
                stats = stats,
                difficulty = selectedDifficulty,
                onSelect = onSelectSize,
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "② 选择难度",
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "同一盘面下，留的提示越少越难",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            DifficultyGrid(
                selected = selectedDifficulty,
                size = selectedSize,
                stats = stats,
                onSelect = onSelectDifficulty,
            )

            if (savedPreview != null) {
                Spacer(Modifier.height(18.dp))
                ContinueCard(preview = savedPreview, onContinue = onContinue)
            }

            Spacer(Modifier.height(16.dp))
        }

        // The call to action is pinned to the bottom so it stays reachable on small phones.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("start_button"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = scheme.primary,
                    contentColor = scheme.onPrimary,
                ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "开始 · ${selectedSize.title} ${selectedDifficulty.title}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "小提示：点格子，再点数字就能填啦",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun Header(onOpenLeaderboard: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "数独乐园",
                style = MaterialTheme.typography.displaySmall,
                color = scheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "每天动动脑，越玩越聪明",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
        }
        Surface(
            onClick = onOpenLeaderboard,
            shape = RoundedCornerShape(18.dp),
            color = scheme.secondaryContainer.copy(alpha = 0.6f),
            modifier = Modifier.testTag("leaderboard_entry"),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🏆", fontSize = 16.sp)
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "排行榜",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun StatsCard(stats: PlayerStats) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = scheme.secondaryContainer.copy(alpha = 0.5f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "通关 ${stats.totalWins} 局",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSecondaryContainer,
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StarRow(
                        stars = (stats.totalStars % 4).coerceIn(0, 3),
                        size = 15,
                        modifier = Modifier,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "共收集 ${stats.totalStars} 颗星",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSecondaryContainer.copy(alpha = 0.75f),
                    )
                }
            }
            Text(
                text = when {
                    stats.totalWins == 0 -> "🌱"
                    stats.totalWins < 5 -> "🌿"
                    stats.totalWins < 15 -> "🌳"
                    else -> "🏆"
                },
                fontSize = 34.sp,
            )
        }
    }
}

@Composable
private fun SizeRow(
    selected: BoardSize,
    stats: PlayerStats,
    difficulty: Difficulty,
    onSelect: (BoardSize) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        BoardSize.entries.forEach { size ->
            SizeCard(
                size = size,
                selected = size == selected,
                bestSeconds = stats.bestTimeFor(size, difficulty),
                onClick = { onSelect(size) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SizeCard(
    size: BoardSize,
    selected: Boolean,
    bestSeconds: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val accent = when (size) {
        BoardSize.FOUR -> scheme.secondary
        BoardSize.SIX -> Color(0xFF4C8DD9)
        BoardSize.NINE -> Color(0xFF8E6BC8)
    }
    val shape = RoundedCornerShape(18.dp)

    Column(
        modifier = modifier
            .clip(shape)
            .background(if (selected) accent.copy(alpha = 0.14f) else scheme.surface)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accent else scheme.outlineVariant.copy(alpha = 0.8f),
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp)
            .testTag("size_${size.key}"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = size.emoji, fontSize = 24.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            text = size.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (selected) accent else scheme.onSurface,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = size.subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
            maxLines = 1,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = bestSeconds?.let { "最佳 ${formatDuration(it)}" } ?: "未通关",
            style = MaterialTheme.typography.labelSmall,
            color = if (bestSeconds != null) {
                accent.copy(alpha = 0.9f)
            } else {
                scheme.onSurfaceVariant.copy(alpha = 0.55f)
            },
            maxLines = 1,
        )
    }
}

@Composable
private fun DifficultyGrid(
    selected: Difficulty,
    size: BoardSize,
    stats: PlayerStats,
    onSelect: (Difficulty) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Difficulty.entries.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowItems.forEach { difficulty ->
                    DifficultyCard(
                        difficulty = difficulty,
                        size = size,
                        selected = difficulty == selected,
                        bestSeconds = stats.bestTimeFor(size, difficulty),
                        onClick = { onSelect(difficulty) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DifficultyCard(
    difficulty: Difficulty,
    size: BoardSize,
    selected: Boolean,
    bestSeconds: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val accent = when (difficulty) {
        Difficulty.EASY -> scheme.secondary
        Difficulty.MEDIUM -> Color(0xFF4C8DD9)
        Difficulty.HARD -> scheme.primary
        Difficulty.EXPERT -> Color(0xFF8E6BC8)
    }
    val container = if (selected) accent.copy(alpha = 0.14f) else scheme.surface
    val clues = difficulty.clueTargetFor(size)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(container)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accent else scheme.outlineVariant.copy(alpha = 0.8f),
                shape = RoundedCornerShape(18.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp)
            .testTag("difficulty_${difficulty.key}"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = difficulty.emoji, fontSize = 20.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = difficulty.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (selected) accent else scheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            if (selected) {
                Badge(text = "已选", background = accent, content = Color.White)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "留存 $clues 个数字",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            maxLines = 1,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (bestSeconds != null) {
                "最佳 ${formatDuration(bestSeconds)}"
            } else {
                "还没通关过"
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (bestSeconds != null) {
                accent.copy(alpha = 0.9f)
            } else {
                scheme.onSurfaceVariant.copy(alpha = 0.6f)
            },
        )
    }
}

@Composable
private fun ContinueCard(
    preview: SavedGamePreview,
    onContinue: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(scheme.tertiaryContainer.copy(alpha = 0.7f))
            .border(1.dp, scheme.tertiary.copy(alpha = 0.5f), shape)
            .clickable(onClick = onContinue)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("⏳", fontSize = 24.sp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "继续上一局 · ${preview.size.title} ${preview.difficulty.title}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = scheme.onTertiaryContainer,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "已完成 ${preview.filledCount}/${preview.totalCount} · " +
                    "用时 ${formatDuration(preview.seconds)}",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onTertiaryContainer.copy(alpha = 0.8f),
            )
        }
        Text(
            text = "继续",
            style = MaterialTheme.typography.labelLarge,
            color = scheme.onTertiaryContainer,
            fontWeight = FontWeight.Bold,
        )
    }
}
