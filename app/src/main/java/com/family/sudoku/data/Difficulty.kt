package com.family.sudoku.data

/**
 * Difficulty is expressed by how many clues are left on the board. Every generated puzzle
 * still has exactly one solution, so more clues means an easier game.
 *
 * The number of clues that feels "easy" depends heavily on the board size: 9 clues on a
 * 4x4 grid is already half the board, while 9 clues on a 9x9 grid would be brutal. The
 * targets therefore live in a table keyed by board size.
 */
enum class Difficulty(
    val key: String,
    val title: String,
    val subtitle: String,
    val emoji: String,
) {
    EASY(key = "easy", title = "轻松", subtitle = "慢慢来，先热身", emoji = "🌱"),
    MEDIUM(key = "medium", title = "进阶", subtitle = "动动小脑筋", emoji = "🌿"),
    HARD(key = "hard", title = "挑战", subtitle = "需要多想一步", emoji = "🔥"),
    EXPERT(key = "expert", title = "大师", subtitle = "留给最勇敢的你", emoji = "🏆"),
    ;

    /**
     * How many clues to leave for this difficulty on the given board size. These values are
     * verified by unit tests: the generator must land within a couple of clues of them.
     */
    fun clueTargetFor(size: BoardSize): Int = when (size) {
        BoardSize.FOUR -> when (this) {
            EASY -> 9
            MEDIUM -> 8
            HARD -> 7
            EXPERT -> 6
        }

        BoardSize.SIX -> when (this) {
            EASY -> 20
            MEDIUM -> 17
            HARD -> 14
            EXPERT -> 12
        }

        BoardSize.NINE -> when (this) {
            EASY -> 46
            MEDIUM -> 38
            HARD -> 31
            EXPERT -> 26
        }
    }

    companion object {
        fun fromKey(key: String?): Difficulty =
            entries.firstOrNull { it.key == key } ?: EASY
    }
}
