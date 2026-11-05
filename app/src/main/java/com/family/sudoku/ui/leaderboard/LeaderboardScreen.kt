package com.family.sudoku.ui.leaderboard

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.family.sudoku.data.BoardSize
import com.family.sudoku.data.Difficulty
import com.family.sudoku.game.ScoreEntry
import com.family.sudoku.ui.formatDuration
import com.family.sudoku.ui.game.StarRow
import com.family.sudoku.ui.formatPlayedAt

/**
 * Top-10 board for one board size and difficulty. Every combination keeps its own list,
 * because a 4x4 time can never be compared with a 9x9 time.
 */
@Composable
fun LeaderboardScreen(
    selectedSize: BoardSize,
    selectedDifficulty: Difficulty,
    entries: List<ScoreEntry>,
    onBack: () -> Unit,
    onSelectSize: (BoardSize) -> Unit,
    onSelectDifficulty: (Difficulty) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
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
                    text = "🏆 排行榜",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onBackground,
                )
                Text(
                    text = "各难度前十名，用时越短越靠前",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Column(Modifier.padding(horizontal = 16.dp)) {
            SizeTabs(selected = selectedSize, onSelect = onSelectSize)
            Spacer(Modifier.height(8.dp))
            DifficultyTabs(selected = selectedDifficulty, onSelect = onSelectDifficulty)
        }

        Spacer(Modifier.height(12.dp))

        if (entries.isEmpty()) {
            EmptyLeaderboard(
                selectedSize = selectedSize,
                selectedDifficulty = selectedDifficulty,
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("leaderboard_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(entries, key = { index, entry -> "$index-${entry.playedAt}" }) { index, entry ->
                    ScoreRow(rank = index + 1, entry = entry)
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun SizeTabs(selected: BoardSize, onSelect: (BoardSize) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BoardSize.entries.forEach { size ->
            Tab(
                label = size.title,
                selected = size == selected,
                onClick = { onSelect(size) },
                modifier = Modifier.weight(1f).testTag("lb_size_${size.key}"),
            )
        }
    }
}

@Composable
private fun DifficultyTabs(selected: Difficulty, onSelect: (Difficulty) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Difficulty.entries.forEach { difficulty ->
            Tab(
                label = "${difficulty.emoji} ${difficulty.title}",
                selected = difficulty == selected,
                onClick = { onSelect(difficulty) },
                modifier = Modifier.weight(1f).testTag("lb_difficulty_${difficulty.key}"),
            )
        }
    }
}

@Composable
private fun Tab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) scheme.primary else scheme.surface)
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = scheme.outlineVariant.copy(alpha = 0.8f),
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun ScoreRow(rank: Int, entry: ScoreEntry) {
    val scheme = MaterialTheme.colorScheme
    val medal = when (rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> null
    }
    val isTopThree = medal != null
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (isTopThree) scheme.secondaryContainer.copy(alpha = 0.45f) else scheme.surface,
            )
            .border(
                1.dp,
                if (isTopThree) scheme.secondary.copy(alpha = 0.45f)
                else scheme.outlineVariant.copy(alpha = 0.7f),
                shape,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("score_row_$rank"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(38.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (medal != null) {
                Text(medal, fontSize = 22.sp)
            } else {
                Text(
                    text = rank.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.width(6.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = formatDuration(entry.seconds),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = buildString {
                    append("失误 ${entry.mistakes} · 提示 ${entry.hints}")
                    if (entry.playedAt > 0) {
                        append(" · ")
                        append(formatPlayedAt(entry.playedAt))
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
        }

        StarRow(stars = entry.stars, size = 13, modifier = Modifier)
    }
}

@Composable
private fun EmptyLeaderboard(
    selectedSize: BoardSize,
    selectedDifficulty: Difficulty,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🌱", fontSize = 44.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "${selectedSize.title} ${selectedDifficulty.title} 还没有记录",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "通关一局就会出现在这里",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
