package com.family.sudoku.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The layered hint is the main teaching feature, so the three steps and their targets are
 * covered directly rather than only through the ViewModel.
 */
class HintPlannerTest {

    private val four = BoardSpec.FOUR

    /** A solvable 4x4 board with a single empty cell, which must be a naked single. */
    private fun almostSolved(): Pair<IntArray, IntArray> {
        val solution = intArrayOf(
            1, 2, 3, 4,
            3, 4, 1, 2,
            2, 1, 4, 3,
            4, 3, 2, 1,
        )
        val board = solution.copyOf().also { it[8] = 0 }
        return board to solution
    }

    @Test
    fun `guide step names the technique without revealing a cell or digit`() {
        val (board, solution) = almostSolved()
        val step = HintPlanner.next(board.toList(), solution.toList(), four, stage = HintStage.GUIDE)

        assertNotNull(step)
        assertEquals(HintStage.GUIDE, step!!.stage)
        assertNull("guide must not point at a cell", step.cell)
        assertNull("guide must not reveal the digit", step.digit)
        assertTrue("guide should be phrased as a question", step.message.contains("试试看"))
    }

    @Test
    fun `focus step points at a cell but still withholds the digit`() {
        val (board, solution) = almostSolved()
        val step = HintPlanner.next(board.toList(), solution.toList(), four, stage = HintStage.FOCUS)

        assertNotNull(step)
        assertEquals(HintStage.FOCUS, step!!.stage)
        assertEquals("the only empty cell is the target", 8, step.cell)
        assertNull("focus must not reveal the digit", step.digit)
    }

    @Test
    fun `reveal step gives the digit and the cell`() {
        val (board, solution) = almostSolved()
        val step = HintPlanner.next(board.toList(), solution.toList(), four, stage = HintStage.REVEAL)

        assertNotNull(step)
        assertEquals(HintStage.REVEAL, step!!.stage)
        assertEquals(8, step.cell)
        assertEquals(solution[8], step.digit)
        assertEquals("这一格只能填 ${solution[8]} 了", step.message)
    }

    @Test
    fun `the three steps always describe the same cell`() {
        val (board, solution) = almostSolved()
        val cells = listOf(HintStage.GUIDE, HintStage.FOCUS, HintStage.REVEAL)
            .map { HintPlanner.next(board.toList(), solution.toList(), four, stage = it) }
        // Guide has no cell, but focus and reveal must agree.
        assertEquals(cells[1]!!.cell, cells[2]!!.cell)
    }

    @Test
    fun `a full board has no hint left`() {
        val (_, solution) = almostSolved()
        assertNull(HintPlanner.next(solution.toList(), solution.toList(), four))
    }

    @Test
    fun `the selected cell is preferred when it can be explained`() {
        val solution = intArrayOf(
            1, 2, 3, 4,
            3, 4, 1, 2,
            2, 1, 4, 3,
            4, 3, 2, 1,
        )
        // Two holes, both trivially solvable.
        val board = solution.copyOf().also {
            it[8] = 0
            it[15] = 0
        }
        val step = HintPlanner.next(
            board.toList(), solution.toList(), four,
            preferredCell = 15, stage = HintStage.FOCUS,
        )
        assertEquals("should help with the cell the child is looking at", 15, step!!.cell)
    }

    @Test
    fun `hints work on the 6x6 layout`() {
        val six = BoardSpec.SIX
        val solution = IntArray(six.cellCount)
        // Build a valid 6x6 solution by brute force through the generator.
        val generated = SudokuGenerator.generate(six, clueTarget = 20)
        solution.indices.forEach { solution[it] = generated.solution[it] }

        val board = solution.copyOf().also { it[0] = 0 }
        val step = HintPlanner.next(board.toList(), solution.toList(), six, stage = HintStage.REVEAL)
        assertNotNull("6x6 must produce a hint", step)
        assertEquals(generated.solution[0], step!!.digit)
    }

    @Test
    fun `candidate computation matches a hand checked 4x4 case`() {
        val board = listOf(
            1, 0, 0, 4,
            0, 0, 0, 0,
            0, 0, 0, 0,
            4, 0, 0, 1,
        )
        // Cell 1 (row 0, col 1): row has 1 and 4, column has nothing, box has 1 and 4.
        val candidates = HintPlanner.candidates(board, four, 1)
        assertEquals(listOf(2, 3), candidates)
    }
}
