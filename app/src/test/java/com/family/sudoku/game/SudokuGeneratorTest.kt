package com.family.sudoku.game

import com.family.sudoku.data.BoardSize
import com.family.sudoku.data.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random
import kotlin.system.measureTimeMillis

class SudokuGeneratorTest {

    @Test
    fun everyBoardSizeProducesAConsistentPuzzle() {
        for (size in BoardSize.entries) {
            for (difficulty in Difficulty.entries) {
                val spec = size.spec
                val generated = SudokuGenerator.generate(
                    spec = spec,
                    clueTarget = difficulty.clueTargetFor(size),
                    random = Random(size.ordinal * 100 + difficulty.ordinal),
                )
                assertEquals(size.cellCount, generated.puzzle.size)
                assertEquals(size.cellCount, generated.solution.size)
                assertTrue(
                    "${size.title} solution must be a valid completed grid",
                    SudokuSolver.isSolved(generated.solution, spec),
                )
                for (index in generated.puzzle.indices) {
                    val clue = generated.puzzle[index]
                    if (clue != 0) {
                        assertEquals(
                            "clue $index must match the solution",
                            generated.solution[index],
                            clue,
                        )
                    }
                }
            }
        }
    }

    @Test
    fun everyGeneratedPuzzleHasExactlyOneSolution() {
        for (size in BoardSize.entries) {
            for (difficulty in Difficulty.entries) {
                repeat(2) { attempt ->
                    val generated = SudokuGenerator.generate(
                        spec = size.spec,
                        clueTarget = difficulty.clueTargetFor(size),
                        random = Random(size.ordinal * 1000 + difficulty.ordinal * 10 + attempt),
                    )
                    assertEquals(
                        "${size.title} $difficulty attempt $attempt must be unique",
                        1,
                        SudokuSolver.countSolutions(generated.puzzle, size.spec, limit = 3),
                    )
                }
            }
        }
    }

    @Test
    fun clueCountStaysCloseToTheDifficultyTarget() {
        for (size in BoardSize.entries) {
            for (difficulty in Difficulty.entries) {
                val target = difficulty.clueTargetFor(size)
                val clues = (0 until 3).map { attempt ->
                    SudokuGenerator.generate(
                        spec = size.spec,
                        clueTarget = target,
                        random = Random(size.ordinal * 777 + difficulty.ordinal * 13 + attempt),
                    ).clueCount
                }
                println("${size.title} $difficulty -> clues=$clues target=$target")
                for (actual in clues) {
                    assertTrue(
                        "${size.title} $difficulty produced $actual clues but targeted $target",
                        actual in (target - 2)..(target + 4),
                    )
                }
            }
        }
    }

    @Test
    fun harderLevelsNeverLeaveMoreCluesThanEasierOnes() {
        for (size in BoardSize.entries) {
            val averages = Difficulty.entries.associateWith { difficulty ->
                (0 until 4).map { attempt ->
                    SudokuGenerator.generate(
                        spec = size.spec,
                        clueTarget = difficulty.clueTargetFor(size),
                        random = Random(size.ordinal * 331 + difficulty.ordinal * 7 + attempt),
                    ).clueCount
                }.average()
            }
            val ladder = Difficulty.entries.map { averages.getValue(it) }
            for (index in 1 until ladder.size) {
                assertTrue(
                    "${size.title} ladder must not increase: $ladder",
                    ladder[index] <= ladder[index - 1] + 2.0,
                )
            }
        }
    }

    @Test
    fun puzzleIsSolvableBackToTheStoredSolution() {
        for (size in BoardSize.entries) {
            val generated = SudokuGenerator.generate(
                spec = size.spec,
                clueTarget = Difficulty.MEDIUM.clueTargetFor(size),
                random = Random(7),
            )
            val solved = generated.puzzle.copyOf()
            val restored = solveCopy(solved, size.spec)
            assertTrue("${size.title} puzzle must be solvable", restored)
            assertEquals(generated.solution.toList(), solved.toList())
        }
    }

    @Test
    fun generationIsFastEnoughForAPhone() {
        for (size in BoardSize.entries) {
            val elapsed = measureTimeMillis {
                repeat(3) {
                    SudokuGenerator.generate(
                        spec = size.spec,
                        clueTarget = Difficulty.EXPERT.clueTargetFor(size),
                        random = Random(it),
                    )
                }
            }
            println("${size.title} expert generation x3 took ${elapsed}ms")
            assertTrue("${size.title} generation took ${elapsed}ms", elapsed < 8_000)
        }
    }

    @Test
    fun nonSquareOrInconsistentBoardsAreRejected() {
        assertFalse(SudokuSolver.isSolved(IntArray(80) { 1 }, BoardSpec.NINE))
        assertFalse(SudokuSolver.isSolved(IntArray(81) { 0 }, BoardSpec.NINE))
        assertEquals(0, SudokuSolver.countSolutions(IntArray(80) { 0 }, BoardSpec.NINE))
    }

    /**
     * Tiny helper solver used by the test to prove the puzzle can be completed, independent
     * of the generator's own backtracking code.
     */
    private fun solveCopy(board: IntArray, spec: BoardSpec): Boolean {
        val index = board.indexOfFirst { it == 0 }
        if (index == -1) return true
        for (value in 1..spec.size) {
            if (isValid(board, spec, index, value)) {
                board[index] = value
                if (solveCopy(board, spec)) return true
                board[index] = 0
            }
        }
        return false
    }

    private fun isValid(board: IntArray, spec: BoardSpec, index: Int, value: Int): Boolean {
        val row = spec.rowOf(index)
        val col = spec.colOf(index)
        for (offset in 0 until spec.size) {
            if (board[spec.indexOf(row, offset)] == value) return false
            if (board[spec.indexOf(offset, col)] == value) return false
        }
        val boxRowStart = (row / spec.boxRows) * spec.boxRows
        val boxColStart = (col / spec.boxCols) * spec.boxCols
        for (r in boxRowStart until boxRowStart + spec.boxRows) {
            for (c in boxColStart until boxColStart + spec.boxCols) {
                if (board[spec.indexOf(r, c)] == value) return false
            }
        }
        return true
    }
}
