package com.family.sudoku.game

/**
 * Bitmask based Sudoku solver. It is fast enough to run repeatedly while generating a
 * puzzle on a phone, and it is also used to prove that a puzzle has exactly one solution.
 *
 * The implementation is size agnostic: it works for 4x4, 6x6 and 9x9 boards because every
 * rule (row, column, box) is expressed through [BoardSpec].
 */
object SudokuSolver {

    /**
     * Counts solutions without modifying [board]. Stops as soon as [limit] solutions have
     * been found, which is what the uniqueness check needs.
     */
    fun countSolutions(board: IntArray, spec: BoardSpec, limit: Int = 2): Int {
        if (limit <= 0) return 0
        if (board.size != spec.cellCount) return 0

        val rowMasks = IntArray(spec.size)
        val colMasks = IntArray(spec.size)
        val boxMasks = IntArray(spec.boxCount)
        val work = board.copyOf()

        for (index in work.indices) {
            val value = work[index]
            if (value < 0 || value > spec.size) return 0
            if (value == 0) continue

            val row = spec.rowOf(index)
            val col = spec.colOf(index)
            val box = spec.boxOf(index)
            val bit = 1 shl (value - 1)
            if (rowMasks[row] and bit != 0 ||
                colMasks[col] and bit != 0 ||
                boxMasks[box] and bit != 0
            ) {
                return 0
            }
            rowMasks[row] = rowMasks[row] or bit
            colMasks[col] = colMasks[col] or bit
            boxMasks[box] = boxMasks[box] or bit
        }

        return search(work, spec, rowMasks, colMasks, boxMasks, limit)
    }

    fun isSolved(board: IntArray, spec: BoardSpec): Boolean {
        if (board.size != spec.cellCount || board.any { it < 1 || it > spec.size }) return false
        val rowMasks = IntArray(spec.size)
        val colMasks = IntArray(spec.size)
        val boxMasks = IntArray(spec.boxCount)
        for (index in board.indices) {
            val row = spec.rowOf(index)
            val col = spec.colOf(index)
            val box = spec.boxOf(index)
            val bit = 1 shl (board[index] - 1)
            if (rowMasks[row] and bit != 0 ||
                colMasks[col] and bit != 0 ||
                boxMasks[box] and bit != 0
            ) {
                return false
            }
            rowMasks[row] = rowMasks[row] or bit
            colMasks[col] = colMasks[col] or bit
            boxMasks[box] = boxMasks[box] or bit
        }
        return true
    }

    /** Candidate digits for one empty cell, encoded as a bitmask (bit 0 means digit 1). */
    fun candidatesMask(
        board: IntArray,
        spec: BoardSpec,
        index: Int,
        rowMasks: IntArray,
        colMasks: IntArray,
        boxMasks: IntArray,
    ): Int {
        val row = spec.rowOf(index)
        val col = spec.colOf(index)
        val box = spec.boxOf(index)
        val fullMask = (1 shl spec.size) - 1
        return fullMask and (rowMasks[row] or colMasks[col] or boxMasks[box]).inv()
    }

    private fun search(
        board: IntArray,
        spec: BoardSpec,
        rowMasks: IntArray,
        colMasks: IntArray,
        boxMasks: IntArray,
        limit: Int,
    ): Int {
        var bestIndex = -1
        var bestMask = 0
        var bestCandidateCount = spec.size + 1

        for (index in board.indices) {
            if (board[index] != 0) continue
            val candidates = candidatesMask(board, spec, index, rowMasks, colMasks, boxMasks)
            val candidateCount = Integer.bitCount(candidates)
            if (candidateCount == 0) return 0
            if (candidateCount < bestCandidateCount) {
                bestCandidateCount = candidateCount
                bestIndex = index
                bestMask = candidates
                if (candidateCount == 1) break
            }
        }

        if (bestIndex == -1) return 1

        val row = spec.rowOf(bestIndex)
        val col = spec.colOf(bestIndex)
        val box = spec.boxOf(bestIndex)
        var total = 0
        var mask = bestMask
        while (mask != 0) {
            val bit = mask and -mask
            val value = Integer.numberOfTrailingZeros(bit) + 1
            board[bestIndex] = value
            rowMasks[row] = rowMasks[row] or bit
            colMasks[col] = colMasks[col] or bit
            boxMasks[box] = boxMasks[box] or bit

            total += search(board, spec, rowMasks, colMasks, boxMasks, limit)
            if (total >= limit) {
                board[bestIndex] = 0
                return total
            }

            rowMasks[row] = rowMasks[row] xor bit
            colMasks[col] = colMasks[col] xor bit
            boxMasks[box] = boxMasks[box] xor bit
            board[bestIndex] = 0
            mask = mask xor bit
        }
        return total
    }
}
