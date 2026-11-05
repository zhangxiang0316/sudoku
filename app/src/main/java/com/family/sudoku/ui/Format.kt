package com.family.sudoku.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatDuration(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    val seconds = safe % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

fun formatDurationShort(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val minutes = safe / 60
    val seconds = safe % 60
    return if (minutes > 0) "${minutes}分${seconds}秒" else "${seconds}秒"
}

/**
 * Short, kid friendly date for leaderboard rows. Older entries show the year so a record set
 * last year is not mistaken for a recent one.
 */
fun formatPlayedAt(epochMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    if (epochMillis <= 0L) return ""
    val zone = ZoneId.systemDefault()
    val played = Instant.ofEpochMilli(epochMillis).atZone(zone)
    val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
    val pattern = if (played.year == now.year) "M月d日" else "yyyy年M月d日"
    return DateTimeFormatter.ofPattern(pattern, Locale.CHINA).format(played)
}
