package com.family.sudoku.game

import com.family.sudoku.data.BoardSize
import com.family.sudoku.data.Difficulty

enum class AppScreen {
    HOME,
    GAME,
    LEADERBOARD,
}

/** Identifies one specific combination of board size and difficulty. */
data class GameKey(
    val size: BoardSize,
    val difficulty: Difficulty,
)

data class GameResult(
    val seconds: Int,
    val mistakes: Int,
    val hints: Int,
    val stars: Int,
    val bestSeconds: Int,
    val isNewBest: Boolean,
    val rank: Int? = null,
)

data class SavedGame(
    val size: BoardSize,
    val difficulty: Difficulty,
    val puzzle: List<Int>,
    val solution: List<Int>,
    val board: List<Int>,
    val notes: List<Int>,
    val seconds: Int,
    val mistakes: Int,
    val hints: Int,
)

data class SavedGamePreview(
    val size: BoardSize,
    val difficulty: Difficulty,
    val filledCount: Int,
    val totalCount: Int,
    val seconds: Int,
)

/**
 * One finished game as it appears on the leaderboard. Only the fields needed to explain the
 * result are kept, so the stored payload stays small.
 */
data class ScoreEntry(
    val seconds: Int,
    val stars: Int,
    val mistakes: Int,
    val hints: Int,
    val playedAt: Long,
)

data class WinRecord(
    val bestSeconds: Int,
    val isNewBest: Boolean,
    /** 1-based position on the leaderboard, or null when the result did not make the top list. */
    val rank: Int? = null,
)

data class PlayerStats(
    val totalWins: Int = 0,
    val totalStars: Int = 0,
    val bestTimes: Map<GameKey, Int> = emptyMap(),
) {
    fun bestTimeFor(size: BoardSize, difficulty: Difficulty): Int? = bestTimes[GameKey(size, difficulty)]
}
