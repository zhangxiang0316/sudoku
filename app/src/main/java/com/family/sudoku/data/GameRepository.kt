package com.family.sudoku.data

import android.content.Context
import com.family.sudoku.game.GameKey
import com.family.sudoku.game.PlayerStats
import com.family.sudoku.game.ScoreEntry
import com.family.sudoku.game.SavedGame
import com.family.sudoku.game.SavedGamePreview
import com.family.sudoku.game.SudokuSolver
import com.family.sudoku.game.WinRecord
import org.json.JSONArray
import org.json.JSONObject

/**
 * Offline storage for the in-progress game and small player statistics. The payload is
 * deliberately tiny and human-readable; no database or network is needed for this app.
 */
class GameRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveGame(game: SavedGame) {
        val json = JSONObject().apply {
            put("version", SAVE_VERSION)
            put("size", game.size.key)
            put("difficulty", game.difficulty.key)
            put("puzzle", game.puzzle.toJsonArray())
            put("solution", game.solution.toJsonArray())
            put("board", game.board.toJsonArray())
            put("notes", game.notes.toJsonArray())
            put("seconds", game.seconds)
            put("mistakes", game.mistakes)
            put("hints", game.hints)
        }
        prefs.edit()
            .putString(KEY_ACTIVE_GAME, json.toString())
            .putString(KEY_LAST_DIFFICULTY, game.difficulty.key)
            .putString(KEY_LAST_SIZE, game.size.key)
            .apply()
    }

    fun loadGame(): SavedGame? {
        val raw = prefs.getString(KEY_ACTIVE_GAME, null) ?: return null
        return try {
            val json = JSONObject(raw)
            val puzzle = json.getJSONArray("puzzle").toIntList() ?: return null
            val solution = json.getJSONArray("solution").toIntList() ?: return null
            val board = json.getJSONArray("board").toIntList() ?: return null
            val notes = json.getJSONArray("notes").toIntList() ?: return null

            // Older saves (version 1) only ever contained 9x9 boards, so a missing size
            // falls back to whatever board length the puzzle has.
            val size = BoardSize.fromKey(json.optString("size").takeIf { it.isNotBlank() })
                ?: BoardSize.entries.firstOrNull { it.cellCount == puzzle.size }
                ?: return null

            if (puzzle.size != size.cellCount ||
                solution.size != size.cellCount ||
                board.size != size.cellCount ||
                notes.size != size.cellCount
            ) {
                return null
            }
            if (solution.any { it < 1 || it > size.size } ||
                !SudokuSolver.isSolved(solution.toIntArray(), size.spec)
            ) {
                return null
            }
            val emptyMask = (1 shl size.size) - 1
            if (puzzle.any { it < 0 || it > size.size } ||
                puzzle.indices.any { puzzle[it] != 0 && puzzle[it] != solution[it] } ||
                puzzle.none { it != 0 }
            ) {
                return null
            }
            if (board.any { it < 0 || it > size.size } || notes.any { it < 0 || it and emptyMask.inv() != 0 }) {
                return null
            }
            if (puzzle.indices.any { puzzle[it] != 0 && board[it] != puzzle[it] }) {
                return null
            }
            if (board == solution) return null

            val difficulty = Difficulty.fromKey(json.optString("difficulty"))
            val seconds = json.optInt("seconds", 0)
            val mistakes = json.optInt("mistakes", 0)
            val hints = json.optInt("hints", 0)
            if (seconds < 0 || mistakes < 0 || hints < 0) return null

            SavedGame(
                size = size,
                difficulty = difficulty,
                puzzle = puzzle,
                solution = solution,
                board = board,
                notes = notes,
                seconds = seconds,
                mistakes = mistakes,
                hints = hints,
            )
        } catch (_: Exception) {
            null
        }
    }

    fun clearGame() {
        prefs.edit().remove(KEY_ACTIVE_GAME).apply()
    }

    fun lastDifficulty(): Difficulty =
        Difficulty.fromKey(prefs.getString(KEY_LAST_DIFFICULTY, null))

    /** Returns null on a fresh install so the home screen can suggest the 4x4 board first. */
    fun lastSize(): BoardSize? = BoardSize.fromKey(prefs.getString(KEY_LAST_SIZE, null))

    fun saveLastSelection(size: BoardSize, difficulty: Difficulty) {
        prefs.edit()
            .putString(KEY_LAST_SIZE, size.key)
            .putString(KEY_LAST_DIFFICULTY, difficulty.key)
            .apply()
    }

    /**
     * Quiet mode silences both the tones and the vibration. Defaults to off so a first time
     * player gets the full feedback, which is what makes the game feel alive.
     */
    fun isQuietMode(): Boolean = prefs.getBoolean(KEY_QUIET_MODE, false)

    fun saveQuietMode(quiet: Boolean) {
        prefs.edit().putBoolean(KEY_QUIET_MODE, quiet).apply()
    }

    fun loadStats(): PlayerStats {
        val bestTimes = buildMap {
            BoardSize.entries.forEach { size ->
                Difficulty.entries.forEach { difficulty ->
                    bestSeconds(size, difficulty)?.let { put(GameKey(size, difficulty), it) }
                }
            }
        }
        return PlayerStats(
            totalWins = prefs.getInt(KEY_TOTAL_WINS, 0),
            totalStars = prefs.getInt(KEY_TOTAL_STARS, 0),
            bestTimes = bestTimes,
        )
    }

    /**
     * Records a finished game, updates the personal best and keeps a top-10 leaderboard for
     * this exact board size and difficulty combination.
     */
    fun recordWin(
        size: BoardSize,
        difficulty: Difficulty,
        seconds: Int,
        stars: Int,
        mistakes: Int = 0,
        hints: Int = 0,
        playedAt: Long = System.currentTimeMillis(),
    ): WinRecord {
        val previousBest = bestSeconds(size, difficulty)
        val isNewBest = previousBest == null || seconds < previousBest
        val best = if (isNewBest) seconds else previousBest

        val entry = ScoreEntry(
            seconds = seconds,
            stars = stars.coerceIn(1, 3),
            mistakes = mistakes,
            hints = hints,
            playedAt = playedAt,
        )
        val updated = (loadLeaderboard(size, difficulty) + entry).ranked()
        // indexOf returns -1 when the result was too slow for a full leaderboard, which must
        // mean "not ranked" rather than rank 0.
        val position = updated.indexOfFirst { it == entry }

        prefs.edit()
            .putInt(bestKey(size, difficulty), best)
            .putInt(KEY_TOTAL_WINS, prefs.getInt(KEY_TOTAL_WINS, 0) + 1)
            .putInt(KEY_TOTAL_STARS, prefs.getInt(KEY_TOTAL_STARS, 0) + entry.stars)
            .putString(leaderboardKey(size, difficulty), updated.toJson().toString())
            .apply()

        return WinRecord(
            bestSeconds = best,
            isNewBest = isNewBest,
            rank = position.takeIf { it >= 0 }?.plus(1),
        )
    }

    /** Best 10 results for one board size and difficulty, fastest first. */
    fun loadLeaderboard(size: BoardSize, difficulty: Difficulty): List<ScoreEntry> {
        val raw = prefs.getString(leaderboardKey(size, difficulty), null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val seconds = item.optInt("seconds", -1)
                    if (seconds < 0) continue
                    add(
                        ScoreEntry(
                            seconds = seconds,
                            stars = item.optInt("stars", 1).coerceIn(1, 3),
                            mistakes = item.optInt("mistakes", 0).coerceAtLeast(0),
                            hints = item.optInt("hints", 0).coerceAtLeast(0),
                            playedAt = item.optLong("playedAt", 0L),
                        ),
                    )
                }
            }.ranked()
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Fastest first. Two equal times are ordered by when they were played, so an older result
     * keeps the better rank instead of the list reshuffling between launches.
     */
    private fun List<ScoreEntry>.ranked(): List<ScoreEntry> =
        sortedWith(compareBy({ it.seconds }, { it.playedAt })).take(MAX_LEADERBOARD_ENTRIES)

    private fun List<ScoreEntry>.toJson(): JSONArray = JSONArray().apply {
        this@toJson.forEach { entry ->
            put(
                JSONObject().apply {
                    put("seconds", entry.seconds)
                    put("stars", entry.stars)
                    put("mistakes", entry.mistakes)
                    put("hints", entry.hints)
                    put("playedAt", entry.playedAt)
                },
            )
        }
    }

    private fun leaderboardKey(size: BoardSize, difficulty: Difficulty): String =
        "leaderboard_${size.key}_${difficulty.key}"

    /**
     * A best time can legitimately be zero seconds, so "no record" is expressed as null
     * instead of a 0 sentinel.
     */
    private fun bestSeconds(size: BoardSize, difficulty: Difficulty): Int? {
        val key = bestKey(size, difficulty)
        if (prefs.contains(key)) return prefs.getInt(key, 0)
        // Save files written before the 4x4/6x6 boards existed stored 9x9 records under
        // "best_<difficulty>". Reading them keeps old best times visible.
        if (size == BoardSize.NINE) {
            val legacyKey = "best_${difficulty.key}"
            if (prefs.contains(legacyKey)) return prefs.getInt(legacyKey, 0)
        }
        return null
    }

    private fun List<Int>.toJsonArray(): JSONArray = JSONArray().apply {
        this@toJsonArray.forEach { put(it) }
    }

    private fun JSONArray.toIntList(): List<Int>? {
        if (length() == 0) return null
        return buildList {
            for (index in 0 until length()) {
                add(optInt(index, -1))
            }
        }
    }

    private fun bestKey(size: BoardSize, difficulty: Difficulty): String =
        "best_${size.key}_${difficulty.key}"

    companion object {
        private const val PREFS_NAME = "sudoku_kids_prefs"
        private const val SAVE_VERSION = 2
        private const val KEY_ACTIVE_GAME = "active_game"
        private const val KEY_LAST_DIFFICULTY = "last_difficulty"
        private const val KEY_LAST_SIZE = "last_size"
        private const val KEY_TOTAL_WINS = "total_wins"
        private const val KEY_TOTAL_STARS = "total_stars"
        private const val KEY_QUIET_MODE = "quiet_mode"
        const val MAX_LEADERBOARD_ENTRIES = 10
    }
}

fun SavedGame.toPreview(): SavedGamePreview = SavedGamePreview(
    size = size,
    difficulty = difficulty,
    filledCount = board.count { it != 0 },
    totalCount = board.size,
    seconds = seconds,
)
