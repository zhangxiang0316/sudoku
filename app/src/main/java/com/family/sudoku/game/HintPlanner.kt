package com.family.sudoku.game

/**
 * A hint in three steps, so a stuck child gets guidance instead of the answer.
 *
 * The first press only points at the technique, the second narrows the search to one cell,
 * and only the third reveals the digit. One hint therefore still teaches something, instead
 * of turning the game into "press hint until it finishes itself".
 */
enum class HintStage {
    /** Which technique to think about, without pointing at a cell. */
    GUIDE,

    /** Which cell to look at, without giving the digit. */
    FOCUS,

    /** The digit itself. */
    REVEAL,
}

/** Which unit the hint refers to, so the wording can say 行 / 列 / 宫. */
enum class HintUnit { CELL, ROW, COLUMN, BOX }

data class HintStep(
    val stage: HintStage,
    /** Cell the player should look at. Null for [HintStage.GUIDE]. */
    val cell: Int?,
    /** Digit to place. Only set for [HintStage.REVEAL]. */
    val digit: Int?,
    val unit: HintUnit,
    val message: String,
)

/**
 * Finds the easiest technique that still applies to the current board: a cell with a single
 * candidate (naked single) first, then a digit with only one possible cell inside a unit
 * (hidden single). Both are the techniques a beginner is expected to learn first.
 */
object HintPlanner {

    fun next(
        board: List<Int>,
        solution: List<Int>,
        spec: BoardSpec,
        preferredCell: Int? = null,
        stage: HintStage = HintStage.GUIDE,
    ): HintStep? {
        if (board.size != spec.cellCount || solution.size != spec.cellCount) return null
        val target = pickTarget(board, spec, preferredCell) ?: return null
        val digit = solution.getOrNull(target.cell) ?: return null
        if (digit !in 1..spec.size) return null

        return when (stage) {
            HintStage.GUIDE -> HintStep(stage, null, null, target.unit, guideText(target.unit))
            HintStage.FOCUS -> HintStep(
                stage = stage,
                cell = target.cell,
                digit = null,
                unit = target.unit,
                message = "看这一${unitWord(target.unit)}，有哪个数字只能填在这里？",
            )

            HintStage.REVEAL -> HintStep(
                stage = stage,
                cell = target.cell,
                digit = digit,
                unit = target.unit,
                message = "这一${unitWord(target.unit)}只能填 $digit 了",
            )
        }
    }

    private data class Target(val cell: Int, val unit: HintUnit)

    private fun unitWord(unit: HintUnit): String = when (unit) {
        HintUnit.CELL -> "格"
        HintUnit.ROW -> "行"
        HintUnit.COLUMN -> "列"
        HintUnit.BOX -> "宫"
    }

    private fun guideText(unit: HintUnit): String = when (unit) {
        HintUnit.CELL -> "试试看：有没有哪一格，只剩一个数字能填？"
        HintUnit.ROW -> "试试看：这一行里，哪个数字只剩一个位置了？"
        HintUnit.COLUMN -> "试试看：这一列里，哪个数字只剩一个位置了？"
        HintUnit.BOX -> "试试看：这一宫里，哪个数字只剩一个位置了？"
    }

    /**
     * Order of preference:
     * 1. the cell the player already selected, if it can be explained,
     * 2. any naked single,
     * 3. any hidden single.
     */
    private fun pickTarget(board: List<Int>, spec: BoardSpec, preferred: Int?): Target? {
        val emptyCells = board.indices.filter { board[it] == 0 }

        preferred?.takeIf { board.getOrNull(it) == 0 }
            ?.let { explainable(board, spec, it) }
            ?.let { return it }

        for (index in emptyCells) {
            if (candidates(board, spec, index).size == 1) {
                return Target(index, HintUnit.CELL)
            }
        }
        for (index in emptyCells) {
            hiddenSingle(board, spec, index)?.let { return it }
        }
        return null
    }

    /** A cell is explainable when it is a naked or hidden single. */
    private fun explainable(board: List<Int>, spec: BoardSpec, index: Int): Target? {
        if (board.getOrNull(index) != 0) return null
        if (candidates(board, spec, index).size == 1) return Target(index, HintUnit.CELL)
        return hiddenSingle(board, spec, index)
    }

    /** Returns the unit in which [index] is the only home for some digit, if any. */
    private fun hiddenSingle(board: List<Int>, spec: BoardSpec, index: Int): Target? {
        val row = spec.rowOf(index)
        val col = spec.colOf(index)
        val box = spec.boxOf(index)
        val mine = candidates(board, spec, index)

        for (digit in mine) {
            val rowPlaces = (0 until spec.size).count { offset ->
                val cell = spec.indexOf(row, offset)
                board[cell] == 0 && digit in candidates(board, spec, cell)
            }
            if (rowPlaces == 1) return Target(index, HintUnit.ROW)

            val colPlaces = (0 until spec.size).count { offset ->
                val cell = spec.indexOf(offset, col)
                board[cell] == 0 && digit in candidates(board, spec, cell)
            }
            if (colPlaces == 1) return Target(index, HintUnit.COLUMN)

            val boxPlaces = spec.peersOf(index).count { cell ->
                spec.boxOf(cell) == box && board[cell] == 0 && digit in candidates(board, spec, cell)
            }
            if (boxPlaces == 1) return Target(index, HintUnit.BOX)
        }
        return null
    }

    /** Candidate digits for one cell, following the same rules as the solver. */
    fun candidates(board: List<Int>, spec: BoardSpec, index: Int): List<Int> {
        if (index !in board.indices) return emptyList()
        val used = BooleanArray(spec.size + 1)
        val row = spec.rowOf(index)
        val col = spec.colOf(index)
        for (offset in 0 until spec.size) {
            used[board[spec.indexOf(row, offset)]] = true
            used[board[spec.indexOf(offset, col)]] = true
        }
        val boxRowStart = (row / spec.boxRows) * spec.boxRows
        val boxColStart = (col / spec.boxCols) * spec.boxCols
        for (r in boxRowStart until boxRowStart + spec.boxRows) {
            for (c in boxColStart until boxColStart + spec.boxCols) {
                used[board[spec.indexOf(r, c)]] = true
            }
        }
        return (1..spec.size).filter { !used[it] }
    }
}
