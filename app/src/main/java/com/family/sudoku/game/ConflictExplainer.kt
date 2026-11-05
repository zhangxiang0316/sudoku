package com.family.sudoku.game

/**
 * Why a filled digit is wrong. Telling the player *why* turns a red cell from a punishment
 * into a teachable moment, which matters a lot for a child learning the rules.
 */
enum class ConflictReason { ROW, COLUMN, BOX, NO_CONFLICT }

data class Conflict(
    val reason: ConflictReason,
    val cells: List<Int>,
)

/**
 * Explains a wrong entry by finding the digit that is already present in the same row,
 * column or box. When the digit does not clash directly, the answer is simply "not this
 * one", because the mistake only shows up later in the solution.
 */
object ConflictExplainer {

    fun explain(
        board: List<Int>,
        spec: BoardSpec,
        index: Int,
        digit: Int,
    ): Conflict {
        if (index !in board.indices) {
            return Conflict(ConflictReason.NO_CONFLICT, emptyList())
        }
        val row = spec.rowOf(index)
        val col = spec.colOf(index)

        val inRow = (0 until spec.size)
            .map { spec.indexOf(row, it) }
            .filter { it != index && board[it] == digit }
        if (inRow.isNotEmpty()) {
            return Conflict(ConflictReason.ROW, inRow)
        }

        val inColumn = (0 until spec.size)
            .map { spec.indexOf(it, col) }
            .filter { it != index && board[it] == digit }
        if (inColumn.isNotEmpty()) {
            return Conflict(ConflictReason.COLUMN, inColumn)
        }

        val inBox = spec.peersOf(index).filter { spec.boxOf(it) == spec.boxOf(index) && board[it] == digit }
        if (inBox.isNotEmpty()) {
            return Conflict(ConflictReason.BOX, inBox)
        }

        return Conflict(ConflictReason.NO_CONFLICT, emptyList())
    }

    /** Kid friendly sentence explaining the conflict. */
    fun messageFor(conflict: Conflict, digit: Int): String = when (conflict.reason) {
        ConflictReason.ROW -> "这一行已经有 $digit 了哦"
        ConflictReason.COLUMN -> "这一列已经有 $digit 了哦"
        ConflictReason.BOX -> "这一宫已经有 $digit 了哦"
        ConflictReason.NO_CONFLICT -> "这一格还不能填 $digit，再看看别的线索～"
    }
}
