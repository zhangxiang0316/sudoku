package com.family.sudoku.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.family.sudoku.data.BoardSize
import com.family.sudoku.data.Difficulty
import com.family.sudoku.game.GameResult
import com.family.sudoku.ui.formatDuration
import kotlinx.coroutines.delay

@Composable
fun WinDialog(
    result: GameResult,
    size: BoardSize,
    difficulty: Difficulty,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit,
    onViewLeaderboard: () -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = scheme.surface,
            tonalElevation = 2.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(scheme.tertiaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🎉", fontSize = 32.sp)
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "${size.title} · ${difficulty.emoji} ${difficulty.title}",
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (result.stars == 3) "太棒了，全对通关！" else "恭喜通关！",
                    style = MaterialTheme.typography.headlineSmall,
                    color = scheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = when (result.stars) {
                        3 -> "没有失误也没有用提示，真厉害！"
                        2 -> "表现不错，再细心一点就满星啦"
                        else -> "完成了就是胜利，下一局会更顺"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                if (result.rank != null) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = scheme.secondaryContainer.copy(alpha = 0.7f),
                    ) {
                        Text(
                            text = if (result.rank == 1) {
                                "🏆 新纪录！登上了第 1 名"
                            } else {
                                "🎖️ 进入排行榜第 ${result.rank} 名"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                AnimatedStars(stars = result.stars)

                Spacer(Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ResultTile(
                        label = "用时",
                        value = formatDuration(result.seconds),
                        modifier = Modifier.weight(1f),
                    )
                    ResultTile(
                        label = "失误",
                        value = result.mistakes.toString(),
                        modifier = Modifier.weight(1f),
                    )
                    ResultTile(
                        label = "提示",
                        value = result.hints.toString(),
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = if (result.isNewBest) {
                        "🏅 新纪录！最佳成绩 ${formatDuration(result.bestSeconds)}"
                    } else {
                        "最佳成绩 ${formatDuration(result.bestSeconds)}"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (result.isNewBest) scheme.tertiary else scheme.onSurfaceVariant,
                    fontWeight = if (result.isNewBest) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(22.dp))

                Button(
                    onClick = onPlayAgain,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("再来一局", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onViewLeaderboard,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("win_leaderboard"),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("🏆 看看排行榜", style = MaterialTheme.typography.titleSmall)
                }
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onHome,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                ) {
                    Text(
                        text = "回到首页",
                        style = MaterialTheme.typography.titleSmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedStars(stars: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) { index ->
            val scale = remember { Animatable(0.6f) }
            LaunchedEffect(index, stars) {
                delay(index * 160L)
                if (index < stars) {
                    scale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 420f))
                }
            }
            Text(
                text = "★",
                fontSize = 40.sp,
                lineHeight = 44.sp,
                color = if (index < stars) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                modifier = Modifier.scale(if (index < stars) scale.value else 1f),
            )
        }
    }
}

@Composable
private fun ResultTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun PauseDialog(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = scheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("⏸️", fontSize = 34.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "休息一下",
                    style = MaterialTheme.typography.headlineSmall,
                    color = scheme.onSurface,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "进度已经帮你保存好了",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onResume,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("继续游戏", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("重玩本局")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onExit,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("回到首页")
                }
            }
        }
    }
}

@Composable
fun ExitConfirmDialog(
    onStay: () -> Unit,
    onLeave: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Dialog(onDismissRequest = onStay) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = scheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
            ) {
                Text(
                    text = "要离开这一局吗？",
                    style = MaterialTheme.typography.titleLarge,
                    color = scheme.onSurface,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "离开后进度会保存，下次可以接着玩。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onStay,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text("继续玩")
                    }
                    Button(
                        onClick = onLeave,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text("离开")
                    }
                }
            }
        }
    }
}

@Composable
fun RestartConfirmDialog(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = scheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
            ) {
                Text(
                    text = "重新开始本局？",
                    style = MaterialTheme.typography.titleLarge,
                    color = scheme.onSurface,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "已经填的数字和用时都会清空。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text("取消")
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text("重新开始")
                    }
                }
            }
        }
    }
}
