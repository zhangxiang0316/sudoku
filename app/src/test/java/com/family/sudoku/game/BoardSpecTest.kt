package com.family.sudoku.game

import com.family.sudoku.data.BoardSize
import com.family.sudoku.data.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The box rules differ per board size (2x2, 2x3, 3x3), so they get their own focused test
 * instead of being assumed correct.
 */
class BoardSpecTest {

    @Test
    fun geometryMatchesTheClassicLayouts() {
        val four = BoardSpec.FOUR
        assertEquals(4, four.size)
        assertEquals(16, four.cellCount)
        assertEquals(4, four.boxCount)
        assertEquals(2, four.boxRows)
        assertEquals(2, four.boxCols)

        val six = BoardSpec.SIX
        assertEquals(6, six.size)
        assertEquals(36, six.cellCount)
        assertEquals(6, six.boxCount)
        assertEquals(2, six.boxRows)
        assertEquals(3, six.boxCols)

        val nine = BoardSpec.NINE
        assertEquals(9, nine.size)
        assertEquals(81, nine.cellCount)
        assertEquals(9, nine.boxCount)
        assertEquals(3, nine.boxRows)
        assertEquals(3, nine.boxCols)
    }

    @Test
    fun sixBySixUsesTwoByThreeBoxes() {
        val spec = BoardSpec.SIX
        // Cells (0,0) and (1,2) share the top-left 2x3 box; (0,3) starts the next box.
        assertTrue(spec.arePeers(spec.indexOf(0, 0), spec.indexOf(1, 2)))
        // (2,1) differs in row, column and box, so it must not be a peer.
        assertFalse(spec.arePeers(spec.indexOf(0, 0), spec.indexOf(2, 1)))
        assertEquals(spec.boxOf(spec.indexOf(0, 0)), spec.boxOf(spec.indexOf(1, 2)))
        assertFalse(spec.boxOf(spec.indexOf(0, 0)) == spec.boxOf(spec.indexOf(0, 3)))
        // (0,0) and (0,3) share a row but not a box.
        assertTrue(spec.arePeers(spec.indexOf(0, 0), spec.indexOf(0, 3)))
        assertEquals(2, spec.boxesPerRow)
        assertEquals(3, spec.boxesPerColumn)
    }

    @Test
    fun peersMatchABruteForceScanForEveryBoardSize() {
        for (spec in listOf(BoardSpec.FOUR, BoardSpec.SIX, BoardSpec.NINE)) {
            for (index in 0 until spec.cellCount) {
                val expected = (0 until spec.cellCount).filter { other ->
                    other != index &&
                        (
                            spec.rowOf(other) == spec.rowOf(index) ||
                                spec.colOf(other) == spec.colOf(index) ||
                                spec.boxOf(other) == spec.boxOf(index)
                            )
                }.toSet()
                val actual = spec.peersOf(index).toSet()
                assertEquals(
                    "${spec.size}x${spec.size} peers of cell $index",
                    expected,
                    actual,
                )
                assertEquals(actual.size, spec.peersOf(index).size)
                assertFalse(spec.peersOf(index).contains(index))
            }
        }
    }

    @Test
    fun everyBoxHoldsExactlySizeCells() {
        for (spec in listOf(BoardSpec.FOUR, BoardSpec.SIX, BoardSpec.NINE)) {
            val counters = IntArray(spec.boxCount)
            for (index in 0 until spec.cellCount) {
                counters[spec.boxOf(index)]++
            }
            counters.forEach { count ->
                assertEquals("${spec.size}x${spec.size} box size", spec.size, count)
            }
        }
    }

    @Test
    fun eachDifficultyGetsFewerCluesAsTheBoardGetsHarder() {
        for (size in BoardSize.entries) {
            val targets = Difficulty.entries.map { it.clueTargetFor(size) }
            for (index in 1 until targets.size) {
                assertTrue(
                    "${size.title} clue targets must decrease: $targets",
                    targets[index] < targets[index - 1],
                )
            }
            assertTrue(
                "${size.title} easy target must leave enough clues",
                targets.first() >= size.cellCount / 2,
            )
            assertTrue(
                "${size.title} expert target must stay solvable",
                targets.last() >= 6,
            )
        }
    }

    @Test
    fun noteGridFitsEveryDigit() {
        for (spec in listOf(BoardSpec.FOUR, BoardSpec.SIX, BoardSpec.NINE)) {
            assertTrue(
                "${spec.size}x${spec.size} note grid is too small",
                spec.noteColumns * spec.noteRows >= spec.size,
            )
        }
    }
}
