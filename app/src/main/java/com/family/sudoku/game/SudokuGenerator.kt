package com.family.sudoku.game

import kotlin.random.Random

data class GeneratedPuzzle(
    val puzzle: IntArray,
    val solution: IntArray,
    val spec: BoardSpec,
) {
    val clueCount: Int get() = puzzle.count { it != 0 }
}

/**
 * Creates a complete solution first, then removes symmetrical pairs of cells while keeping
 * the puzzle uniquely solvable. Symmetry makes the board visually pleasant; the uniqueness
 * check guarantees the puzzle can be solved by reasoning instead of guessing.
 *
 * Works for any [BoardSpec], which is what makes 4x4 / 6x6 / 9x9 share one implementation.
 */
object SudokuGenerator {
    /**
     * A puzzle can only be made harder while it keeps exactly one solution, so the requested
     * clue count is a target rather than a guarantee. Several boards are generated and the
     * one closest to the target wins; without this the occasional board would land far from
     * the target and a "master" level could end up easier than "hard".
     */
    private const val MAX_ATTEMPTS = 8

    fun generate(
        spec: BoardSpec,
        clueTarget: Int,
        random: Random = Random.Default,
    ): GeneratedPuzzle {
        require(clueTarget in 0..spec.cellCount) {
            "clueTarget ($clueTarget) must be within 0..${spec.cellCount}"
        }

        var best: GeneratedPuzzle? = null
        var bestDistance = Int.MAX_VALUE

        repeat(MAX_ATTEMPTS) {
            val candidate = generateOnce(spec, clueTarget, random) ?: return@repeat
            val distance = kotlin.math.abs(candidate.clueCount - clueTarget)
            if (distance < bestDistance) {
                bestDistance = distance
                best = candidate
            }
            // An exact hit cannot be improved on, so stop early and keep generation snappy.
            if (distance == 0) return candidate
        }

        return requireNotNull(best) {
            "Could not generate a ${spec.size}x${spec.size} puzzle"
        }
    }

    private fun generateOnce(
        spec: BoardSpec,
        clueTarget: Int,
        random: Random,
    ): GeneratedPuzzle? {
        val solution = IntArray(spec.cellCount)
        if (!fillCompleteBoard(solution, spec, random)) return null

        val puzzle = solution.copyOf()
        var clueCount = spec.cellCount

        // Remove mirrored pairs first. Odd-sized boards have one centre cell without a
        // partner, which is handled on its own.
        val pairStarters = (0..spec.cellCount / 2).shuffled(random)
        for (start in pairStarters) {
            val mirror = spec.cellCount - 1 - start
            if (start == mirror) {
                // Odd-sized board: the centre cell has no partner.
                if (clueCount - 1 >= clueTarget) {
                    clueCount = removeIfUnique(puzzle, spec, intArrayOf(start), clueCount)
                }
            } else if (clueCount - 2 >= clueTarget) {
                clueCount = removeIfUnique(puzzle, spec, intArrayOf(start, mirror), clueCount)
            }
        }

        // Symmetry is nice but not at the cost of leaving the board too easy. Fill the
        // remaining gap with individual removals while unique solutions still hold.
        if (clueCount > clueTarget) {
            val remaining = puzzle.indices.filter { puzzle[it] != 0 }.shuffled(random)
            for (index in remaining) {
                if (clueCount <= clueTarget) break
                clueCount = removeIfUnique(puzzle, spec, intArrayOf(index), clueCount)
            }
        }

        return GeneratedPuzzle(puzzle = puzzle, solution = solution, spec = spec)
    }

    private fun removeIfUnique(
        puzzle: IntArray,
        spec: BoardSpec,
        indices: IntArray,
        clueCount: Int,
    ): Int {
        val backup = indices.map { puzzle[it] }
        indices.forEach { puzzle[it] = 0 }
        if (SudokuSolver.countSolutions(puzzle, spec, limit = 2) == 1) {
            return clueCount - indices.size
        }
        indices.forEachIndexed { position, index -> puzzle[index] = backup[position] }
        return clueCount
    }

    private fun fillCompleteBoard(
        board: IntArray,
        spec: BoardSpec,
        random: Random,
    ): Boolean {
        var bestIndex = -1
        var bestCandidates = IntArray(0)

        for (index in board.indices) {
            if (board[index] != 0) continue
            val candidates = candidatesFor(board, spec, index)
            if (candidates.isEmpty()) return false
            if (bestIndex == -1 || candidates.size < bestCandidates.size) {
                bestIndex = index
                bestCandidates = candidates
                if (candidates.size == 1) break
            }
        }

        if (bestIndex == -1) return true

        bestCandidates.shuffle(random)
        for (value in bestCandidates) {
            board[bestIndex] = value
            if (fillCompleteBoard(board, spec, random)) return true
        }
        board[bestIndex] = 0
        return false
    }

    private fun candidatesFor(board: IntArray, spec: BoardSpec, index: Int): IntArray {
        val row = spec.rowOf(index)
        val col = spec.colOf(index)
        val used = BooleanArray(spec.size + 1)
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
        return (1..spec.size).filter { !used[it] }.toIntArray()
    }
}
