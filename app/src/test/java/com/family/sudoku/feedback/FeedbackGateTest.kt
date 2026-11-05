package com.family.sudoku.feedback

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.family.sudoku.data.BoardSize
import com.family.sudoku.data.Difficulty
import com.family.sudoku.ui.GameViewModel
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Records which feedback events the game actually asks for. Robolectric has no real speaker,
 * so this covers the half that matters most: the right cue is requested at the right moment,
 * and nothing is requested when the player did not really do anything.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FeedbackGateTest {

    private class Recorder : GameFeedback {
        val events = mutableListOf<String>()
        override fun correct() {
            events += "correct"
        }

        override fun wrong() {
            events += "wrong"
        }

        override fun win() {
            events += "win"
        }

        override fun tap() {
            events += "tap"
        }
    }

    private val computeDispatcher = Dispatchers.Unconfined
    private lateinit var application: Application
    private lateinit var recorder: Recorder
    private lateinit var viewModel: GameViewModel

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        application.getSharedPreferences("sudoku_kids_prefs", 0).edit().clear().commit()
        recorder = Recorder()
        viewModel = GameViewModel(application, computeDispatcher, recorder)
        viewModel.startNewGame(BoardSize.FOUR, Difficulty.EASY)
        shadowOf(Looper.getMainLooper()).idle()
    }

    @After
    fun tearDown() {
        viewModel.stopTimers()
    }

    @Test
    fun `a wrong digit asks for the error cue`() {
        val state = viewModel.uiState.value
        val index = state.puzzle.indexOfFirst { it == 0 }
        val wrong = (1..state.size.size).first { it != state.solution[index] }

        viewModel.selectCell(index)
        viewModel.inputDigit(wrong)

        assertEquals(listOf("wrong"), recorder.events)
    }

    @Test
    fun `a correct digit asks for the success cue`() {
        val state = viewModel.uiState.value
        val index = state.puzzle.indexOfFirst { it == 0 }

        viewModel.selectCell(index)
        viewModel.inputDigit(state.solution[index])

        assertEquals(listOf("correct"), recorder.events)
    }

    @Test
    fun `completing the puzzle ends with the win cue`() {
        val state = viewModel.uiState.value
        for (index in state.board.indices) {
            if (state.puzzle[index] == 0) {
                viewModel.selectCell(index)
                viewModel.inputDigit(state.solution[index])
            }
        }

        assertTrue("the game must be complete", viewModel.uiState.value.isComplete)
        assertEquals("win", recorder.events.last())
        assertFalse(
            "the win cue must not fire twice: ${recorder.events}",
            recorder.events.count { it == "win" } > 1,
        )
    }

    @Test
    fun `erasing asks for a light tap`() {
        val state = viewModel.uiState.value
        val index = state.puzzle.indexOfFirst { it == 0 }
        viewModel.selectCell(index)
        viewModel.inputDigit(state.solution[index])
        recorder.events.clear()

        viewModel.eraseSelected()

        assertEquals(listOf("tap"), recorder.events)
    }

    @Test
    fun `a rejected tap on a given cell stays silent`() {
        val given = viewModel.uiState.value.puzzle.indexOfFirst { it != 0 }
        viewModel.selectCell(given)
        recorder.events.clear()

        // Givens are not editable, so nothing should be played.
        viewModel.inputDigit(1)

        assertTrue("a no-op must not make a sound: ${recorder.events}", recorder.events.isEmpty())
    }

    @Test
    fun `tapping the sound switch is silent while muting`() {
        // Muting must not beep: the child pressed the button precisely to stop the noise.
        recorder.events.clear()
        viewModel.toggleQuietMode()

        assertTrue("muting must stay silent: ${recorder.events}", recorder.events.isEmpty())
    }

    @Test
    fun `tapping the sound switch plays a confirmation when unmuting`() {
        viewModel.toggleQuietMode()
        recorder.events.clear()

        viewModel.toggleQuietMode()

        assertEquals(listOf("tap"), recorder.events)
    }
}
