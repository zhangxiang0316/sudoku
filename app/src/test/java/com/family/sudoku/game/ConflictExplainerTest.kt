package com.family.sudoku.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The explanation shown after a wrong entry is what turns a red cell into a lesson, so the
 * reason detection is covered directly rather than only through the ViewModel.
 */
class ConflictExplainerTest {

    private val four = BoardSpec.FOUR

    // 4x4 board, all rows empty except what each test places.
    private fun board(vararg pairs: Pair<Int, Int>): List<Int> {
        val cells = MutableList(four.cellCount) { 0 }
        pairs.forEach { (index, value) -> cells[index] = value }
        return cells
    }

    @Test
    fun `digit already in the same row is reported as a row conflict`() {
        // Row 0: put a 2 at column 1, then try 2 at column 3.
        val cells = board(four.indexOf(0, 1) to 2)
        val target = four.indexOf(0, 3)

        val conflict = ConflictExplainer.explain(cells, four, target, 2)

        assertEquals(ConflictReason.ROW, conflict.reason)
        assertEquals(listOf(four.indexOf(0, 1)), conflict.cells)
        assertEquals("这一行已经有 2 了哦", ConflictExplainer.messageFor(conflict, 2))
    }

    @Test
    fun `digit already in the same column is reported as a column conflict`() {
        // Column 2: put a 3 at row 1, then try 3 at row 3.
        val cells = board(four.indexOf(1, 2) to 3)
        val target = four.indexOf(3, 2)

        val conflict = ConflictExplainer.explain(cells, four, target, 3)

        assertEquals(ConflictReason.COLUMN, conflict.reason)
        assertEquals("这一列已经有 3 了哦", ConflictExplainer.messageFor(conflict, 3))
    }

    @Test
    fun `digit inside the same box but not the same row or column is a box conflict`() {
        // 4x4 uses 2x2 boxes. Box of (0,0) covers rows 0..1 and cols 0..1.
        // Place a 4 at (1,0) and try 4 at (0,1): same box, different row and column.
        val cells = board(four.indexOf(1, 0) to 4)
        val target = four.indexOf(0, 1)

        val conflict = ConflictExplainer.explain(cells, four, target, 4)

        assertEquals(ConflictReason.BOX, conflict.reason)
        assertTrue(
            "the message must name the box, got: ${ConflictExplainer.messageFor(conflict, 4)}",
            ConflictExplainer.messageFor(conflict, 4).contains("这一宫"),
        )
    }

    @Test
    fun `a digit that clashes nowhere falls back to a gentle message`() {
        val cells = board(four.indexOf(0, 0) to 1)
        val target = four.indexOf(3, 3)

        val conflict = ConflictExplainer.explain(cells, four, target, 2)

        assertEquals(ConflictReason.NO_CONFLICT, conflict.reason)
        assertTrue(conflict.cells.isEmpty())
        assertEquals("这一格还不能填 2，再看看别的线索～", ConflictExplainer.messageFor(conflict, 2))
    }

    @Test
    fun `row conflict wins over box conflict so the nearest clue is highlighted`() {
        // A 2 sits in the same row AND the same box as the target.
        val cells = board(four.indexOf(0, 0) to 2)
        val target = four.indexOf(0, 1)

        val conflict = ConflictExplainer.explain(cells, four, target, 2)

        assertEquals(ConflictReason.ROW, conflict.reason)
    }

    @Test
    fun `the target cell itself never counts as its own conflict`() {
        val cells = board(four.indexOf(0, 2) to 3)
        val target = four.indexOf(0, 2)

        val conflict = ConflictExplainer.explain(cells, four, target, 3)

        assertEquals(ConflictReason.NO_CONFLICT, conflict.reason)
    }

    @Test
    fun `explaining works on the 6x6 layout too`() {
        val six = BoardSpec.SIX
        val cells = MutableList(six.cellCount) { 0 }
        // 6x6 uses 2x3 boxes: rows 0..1, cols 0..2 form one box.
        cells[six.indexOf(1, 2)] = 5
        val target = six.indexOf(0, 1)

        val conflict = ConflictExplainer.explain(cells, six, target, 5)

        assertEquals(ConflictReason.BOX, conflict.reason)
        assertEquals(listOf(six.indexOf(1, 2)), conflict.cells)
    }
}
