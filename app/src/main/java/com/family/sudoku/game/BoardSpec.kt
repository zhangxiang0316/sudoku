package com.family.sudoku.game

/**
 * Everything that varies between a 4x4, 6x6 and 9x9 Sudoku: the grid size and the size of
 * the sub-boxes. Keeping these together means the solver, generator, view model and board
 * renderer can all be written once instead of being duplicated per board size.
 *
 * @param size number of rows and columns (4, 6 or 9)
 * @param boxRows height of one sub-box in cells
 * @param boxCols width of one sub-box in cells
 */
data class BoardSpec(
    val size: Int,
    val boxRows: Int,
    val boxCols: Int,
) {
    init {
        require(size > 0) { "size must be positive" }
        require(boxRows > 0 && boxCols > 0) { "box dimensions must be positive" }
        require(size % boxRows == 0) { "size ($size) must divide by boxRows ($boxRows)" }
        require(size % boxCols == 0) { "size ($size) must divide by boxCols ($boxCols)" }
        require(boxRows * boxCols == size) {
            "boxRows * boxCols (${boxRows * boxCols}) must equal size ($size)"
        }
    }

    val cellCount: Int get() = size * size
    val boxesPerRow: Int get() = size / boxCols
    val boxesPerColumn: Int get() = size / boxRows
    val boxCount: Int get() = boxesPerRow * boxesPerColumn
    val maxDigit: Int get() = size

    fun rowOf(index: Int): Int = index / size

    fun colOf(index: Int): Int = index % size

    fun boxOf(index: Int): Int {
        val row = rowOf(index)
        val col = colOf(index)
        return (row / boxRows) * boxesPerRow + col / boxCols
    }

    fun indexOf(row: Int, col: Int): Int = row * size + col

    /** All other cells sharing a row, column or box with [index]. */
    fun peersOf(index: Int): List<Int> {
        val row = rowOf(index)
        val col = colOf(index)
        val result = LinkedHashSet<Int>(size * 3)
        for (i in 0 until size) {
            result += indexOf(row, i)
            result += indexOf(i, col)
        }
        val boxRowStart = (row / boxRows) * boxRows
        val boxColStart = (col / boxCols) * boxCols
        for (r in boxRowStart until boxRowStart + boxRows) {
            for (c in boxColStart until boxColStart + boxCols) {
                result += indexOf(r, c)
            }
        }
        result.remove(index)
        return result.toList()
    }

    fun arePeers(first: Int, second: Int): Boolean {
        if (first == second) return false
        return rowOf(first) == rowOf(second) ||
            colOf(first) == colOf(second) ||
            boxOf(first) == boxOf(second)
    }

    /** Number of columns used when drawing the small candidate grid inside a cell. */
    val noteColumns: Int
        get() = when {
            size <= 4 -> 2
            size <= 6 -> 3
            else -> 3
        }

    val noteRows: Int get() = (size + noteColumns - 1) / noteColumns

    companion object {
        val FOUR: BoardSpec = BoardSpec(size = 4, boxRows = 2, boxCols = 2)
        val SIX: BoardSpec = BoardSpec(size = 6, boxRows = 2, boxCols = 3)
        val NINE: BoardSpec = BoardSpec(size = 9, boxRows = 3, boxCols = 3)
    }
}
